package org.dcache.pool.movers;
 
 import static com.google.common.base.Preconditions.checkState;
 import static com.google.common.collect.Lists.newArrayList;
 import static org.dcache.util.ByteUnit.KiB;
 import static org.dcache.util.Exceptions.messageOrClassName;
 
 import com.google.common.annotations.VisibleForTesting;
 import com.google.common.collect.DiscreteDomain;
 import com.google.common.collect.Range;
 import com.google.common.collect.RangeSet;
 import com.google.common.collect.TreeRangeSet;
 import java.io.IOException;
 import java.io.InterruptedIOException;
 import java.nio.ByteBuffer;
 import java.nio.channels.ReadableByteChannel;
 import java.security.MessageDigest;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.List;
 import java.util.Set;
 import java.util.concurrent.locks.Lock;
 import java.util.concurrent.locks.ReentrantReadWriteLock;
 import java.util.stream.Collectors;
 import javax.annotation.concurrent.GuardedBy;
 import org.dcache.pool.repository.ForwardingRepositoryChannel;
 import org.dcache.pool.repository.RepositoryChannel;
 import org.dcache.util.Checksum;
 import org.dcache.util.ChecksumType;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  * A wrapper for RepositoryChannel that computes a digest on the fly during write as long as all
  * writes are sequential.
  */
 public class ChecksumChannel extends ForwardingRepositoryChannel {
 
     private static final Logger _log =
           LoggerFactory.getLogger(ChecksumChannel.class);
 
     /**
      * Inner channel to which all operations are delegated.
      */
     @VisibleForTesting
     RepositoryChannel _channel;
 
     /**
      * Digest used for computing the checksum during write.
      */
     private final List<MessageDigest> _digests;
 
     /**
      * Cached checksum after getChecksums is called the first time.
      */
     private Set<Checksum> _finalChecksums;
 
     /**
      * RangeSet to keep track of written bytes
      */
     private final RangeSet<Long> _dataRangeSet = TreeRangeSet.create();
 
     /**
      * The offset where the checksum was calculated.
      */
     @GuardedBy("_digests")
     private long _nextChecksumOffset = 0L;
 
     /**
      * Flag to indicate whether it is still possible to calculated a checksum
      */
     private volatile boolean _isChecksumViable = true;
 
     /**
      * Flag to indicate whether we still allow writing to the channel. This flag is set to false
      * after getChecksums has been called.
      */
     @GuardedBy("_ioStateLock")
     private boolean _isWritable = true;
 
     /**
      * Lock to protect _isWritable field.
      */
     private final ReentrantReadWriteLock _ioStateLock = new ReentrantReadWriteLock();
     private final Lock _ioStateReadLock = _ioStateLock.readLock();
     private final Lock _ioStateWriteLock = _ioStateLock.writeLock();
 
     /**
      * Buffer to be used for reading data back from the inner channel for checksum calculations.
      */
     @VisibleForTesting
     @GuardedBy("_digests")
     ByteBuffer _readBackBuffer = ByteBuffer.allocate(KiB.toBytes(256));
 
     /*
      * Static buffer with zeros shared with in all instances of ChecksumChannel.
      */
     private static final ByteBuffer ZERO_BUFFER = ByteBuffer
           .allocate(KiB.toBytes(256))
           .asReadOnlyBuffer();
 
     /**
      * Buffer to be used for feeding the checksum digester with 0s to fill up gaps in ranges.
      */
     @VisibleForTesting
     ByteBuffer _zerosBuffer = ZERO_BUFFER.duplicate();
 
     public ChecksumChannel(RepositoryChannel inner, Set<ChecksumType> types) {
         _channel = inner;
         _digests = types.stream()
               .map(t -> t.createMessageDigest())
               .collect(Collectors.toList());
     }
 
     /**
      * Ensure that a Checksum is calculated for the supplied ChecksumType.  If the ChecksumType is
      * already registered then this method does nothing, otherwise the ChecksumChannel is updated to
      * calculate the new ChecksumType. If the ChecksumChannel has accepted a contiguous range of
      * data from offset 0 then this method will reread that contiguous range.
      *
      * @param type The algorithm this ChecksumChannel should calculate.
      * @throws IOException if the Channel has already started accepting data and an attempt to
      *                     reread data from disk fails.
      */
     public void addType(ChecksumType type) throws IOException {
         synchronized (_digests) {
             if (_digests.stream()
                   .map(MessageDigest::getAlgorithm)
                   .noneMatch(t -> t.equals(type.getName()))) {
                 MessageDigest digest = type.createMessageDigest();
 
                 if (_isChecksumViable) {
                     try {
                         updateFromChannel(Collections.singleton(digest), 0L, _nextChecksumOffset);
                     } catch (IOException e) {
                         throw new IOException("Failed when reading received data: "
                               + messageOrClassName(e), e);
                     }
                 }
 
                 _digests.add(digest);
             }
         }
     }
 
     @Override
     protected RepositoryChannel delegate() {
         return _channel;
     }
 
     @Override
     public int write(ByteBuffer buffer, long position) throws IOException {
         _ioStateReadLock.lock();
         try {
             checkState(_isWritable, "ChecksumChannel must not be written to after getChecksums");
 
             int bytes;
             if (_isChecksumViable) {
                 ByteBuffer readOnly = buffer.asReadOnlyBuffer();
                 bytes = _channel.write(buffer, position);
                 updateChecksum(readOnly, position, bytes);
             } else {
                 bytes = _channel.write(buffer, position);
             }
             return bytes;
         } finally {
             _ioStateReadLock.unlock();
         }
     }
 
     @Override
     public long transferFrom(ReadableByteChannel src, long position,
           long count) throws IOException {
         _isChecksumViable = false;
         return _channel.transferFrom(src, position, count);
     }
 
     @Override
     public int write(ByteBuffer src) throws IOException {
         _ioStateReadLock.lock();
         try {
 
             checkState(_isWritable, "ChecksumChannel must not be written to after getChecksums");
 
             int bytes;
             if (_isChecksumViable) {
                 bytes = writeWithChecksumUpdate(src);
             } else {
                 bytes = _channel.write(src);
             }
             return bytes;
         } finally {
             _ioStateReadLock.unlock();
         }
     }
 
     @Override
     public long write(ByteBuffer[] srcs, int offset, int length)
           throws IOException {
         _ioStateReadLock.lock();
         try {
 
             checkState(_isWritable, "ChecksumChannel must not be written to after getChecksums");
 
             long bytes = 0;
             if (_isChecksumViable) {
                 for (int i = offset; i < offset + length; i++) {
                     bytes += writeWithChecksumUpdate(srcs[i]);
                 }
             } else {
                 bytes = _channel.write(srcs, offset, length);
             }
             return bytes;
         } finally {
             _ioStateReadLock.unlock();
         }
     }
 
     @Override
     public synchronized long write(ByteBuffer[] srcs) throws IOException {
         return write(srcs, 0, srcs.length);
     }
 
     /**
      * @return final checksum of this channel
      */
     public Set<Checksum> getChecksums() {
         if (!_isChecksumViable) {
             return Collections.emptySet();
         }
 
         if (_finalChecksums == null) {
             _finalChecksums = finalizeChecksums();
         }
         return _finalChecksums;
     }
 
 
/** Gives back the calculated digest. Returns null if overlapping writes were encountered. */

private Set<Checksum> finalizeChecksums() {
    synchronized (_digests) {
        if (_nextChecksumOffset != _channel.size()) {
            _log.warn("Checksum calculation was interrupted due to overlapping writes");
            return null;
        }

        List<byte[]> checksumValues = new ArrayList<>();
        for (MessageDigest digest : _digests) {
            checksumValues.add(digest.digest());
        }

        List<Checksum> checksums = newArrayList();
        for (int i = 0; i < checksumValues.size(); i++) {
            ChecksumType type = _digests.get(i).getAlgorithm();
            byte[] value = checksumValues.get(i);
            checksums.add(new Checksum(type, value));
        }

        return Collections.unmodifiableSet(new HashSet<>(checksums));
    }
}
 

}