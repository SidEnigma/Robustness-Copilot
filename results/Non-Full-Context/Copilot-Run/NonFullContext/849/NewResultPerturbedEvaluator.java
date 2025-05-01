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
 
 
/** Add a Checksum to the supplied ChecksumType */

public void addType(ChecksumType type) throws IOException {
    _ioStateReadLock.lock();
    try {
        checkState(_isWritable, "Channel is not writable");
        _ioStateWriteLock.lock();
    } finally {
        _ioStateReadLock.unlock();
    }

    try {
        MessageDigest digest = type.createMessageDigest();
        _digests.add(digest);
    } catch (Exception e) {
        throw new IOException("Failed to create message digest for checksum type: " + type, e);
    } finally {
        _ioStateWriteLock.unlock();
    }
}
 

}