package org.zeromq;
 
 import java.io.ByteArrayInputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import org.zeromq.util.ZDigest;
 import org.zeromq.util.ZMetadata;
 
 /**
  *
     To authenticate new clients using the ZeroMQ CURVE security mechanism,
     we have to check that the client's public key matches a key we know and
     accept. There are numerous ways to store accepted client public keys.
     The mechanism CZMQ implements is "certificates" (plain text files) held
     in a "certificate store" (a disk directory). This class works with such
     certificate stores, and lets you easily load them from disk, and check
     if a given client public key is known or not. The {@link org.zeromq.ZCert} class does the
     work of managing a single certificate.
  * <p>Those files need to be in ZMP-Format which is created by {@link org.zeromq.ZConfig}</p>
  */
 public class ZCertStore
 {
     public interface Fingerprinter
     {
         byte[] print(File path);
     }
 
     public static final class Timestamper implements Fingerprinter
     {
         private final byte[] buf = new byte[Long.SIZE / Byte.SIZE];
 
         @Override
         public byte[] print(File path)
         {
             final long value = path.lastModified();
             buf[0] = (byte) ((value >>> 56) & 0xff);
             buf[0] = (byte) ((value >>> 48) & 0xff);
             buf[0] = (byte) ((value >>> 40) & 0xff);
             buf[0] = (byte) ((value >>> 32) & 0xff);
             buf[0] = (byte) ((value >>> 24) & 0xff);
             buf[0] = (byte) ((value >>> 16) & 0xff);
             buf[0] = (byte) ((value >>> 8) & 0xff);
             buf[0] = (byte) ((value) & 0xff);
             return buf;
         }
     }
 
     public static final class Hasher implements Fingerprinter
     {
         // temporary buffer used for digest. Instance member for performance reasons.
         private final byte[] buffer = new byte[8192];
 
         @Override
         public byte[] print(File path)
         {
             InputStream input = stream(path);
             if (input != null) {
                 try {
                     return new ZDigest(buffer).update(input).data();
                 }
                 catch (IOException e) {
                     return null;
                 }
                 finally {
                     try {
                         input.close();
                     }
                     catch (IOException e) {
                         e.printStackTrace();
                     }
                 }
             }
             return null;
         }
 
         private InputStream stream(File path)
         {
             if (path.isFile()) {
                 try {
                     return new FileInputStream(path);
                 }
                 catch (FileNotFoundException e) {
                     return null;
                 }
             }
             else if (path.isDirectory()) {
                 List<String> list = Arrays.asList(path.list());
                 Collections.sort(list);
                 return new ByteArrayInputStream(list.toString().getBytes(ZMQ.CHARSET));
             }
             return null;
         }
     }
 
     private interface IFileVisitor
     {
         /**
          * Visits a file.
          * @param file the file to visit.
          * @return true to stop the traversal, false to continue.
          */
         boolean visitFile(File file);
 
         /**
          * Visits a directory.
          * @param dir the directory to visit.
          * @return true to stop the traversal, false to continue.
          */
         boolean visitDir(File dir);
     }
 
     //  Directory location
     private final File location;
 
     // the scanned files (and directories) along with their fingerprint
     private final Map<File, byte[]> fingerprints = new HashMap<>();
 
     // collected public keys
     private final Map<String, ZMetadata> publicKeys = new HashMap<>();
 
     private final Fingerprinter finger;
 
     /**
      * Create a Certificate Store at that file system folder location
      * @param location
      */
     public ZCertStore(String location)
     {
         this(location, new Timestamper());
     }
 
     public ZCertStore(String location, Fingerprinter fingerprinter)
     {
         this.finger = fingerprinter;
         this.location = new File(location);
         loadFiles();
     }
 
     private boolean traverseDirectory(File root, IFileVisitor visitor)
     {
         assert (root.exists());
         assert (root.isDirectory());
 
         if (visitor.visitDir(root)) {
             return true;
         }
         for (File file : root.listFiles()) {
             if (file.isFile()) {
                 if (visitor.visitFile(file)) {
                     return true;
                 }
             }
             else if (file.isDirectory()) {
                 return traverseDirectory(file, visitor);
             }
             else {
                 System.out.printf(
                                   "WARNING: %s is neither file nor directory? This shouldn't happen....SKIPPING%n",
                                   file.getAbsolutePath());
             }
         }
         return false;
     }
 
 
/** If the public key is in the store, you should check it. */

public boolean containsPublicKey(byte[] publicKey) {
    for (ZMetadata metadata : publicKeys.values()) {
        if (Arrays.equals(metadata.publicKey, publicKey)) {
            return true;
        }
    }
    return false;
}
 

}