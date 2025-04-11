package org.dcache.util;
 
 import static com.google.common.base.Preconditions.checkArgument;
 import static com.google.common.base.Strings.padStart;
 import static java.util.Objects.requireNonNull;
 import static org.dcache.util.ChecksumType.ADLER32;
 
 import com.google.common.base.CharMatcher;
 import com.google.common.io.BaseEncoding;
 import java.io.Serializable;
 import java.security.MessageDigest;
 
 public class Checksum implements Serializable {
 
     private static final long serialVersionUID = 7338775749513974986L;
 
     private static final CharMatcher HEXADECIMAL = CharMatcher.anyOf("0123456789abcdef");
 
     private static final char DELIMITER = ':';
 
     private final ChecksumType type;
     private final String value;
 
     /**
      * Creates a new instance of Checksum.
      *
      * @param type  The checksum algorithm.
      * @param value The checksum value.
      * @throws NullPointerException     if either argument is null
      * @throws IllegalArgumentException if the value has the wrong length for the checksum
      *                                  algorithm.
      */
     public Checksum(ChecksumType type, byte[] value) {
         this(type, BaseEncoding.base16().lowerCase().encode(value));
     }
 
     public Checksum(MessageDigest digest) {
         this(ChecksumType.getChecksumType(digest.getAlgorithm()), digest.digest());
     }
 
     /**
      * Creates a new instance of Checksum based on supplied type and a string of the checksum value
      * in hexadecimal.  If the type is ADLER32 then the value may omit any leading zeros.
      *
      * @param type  The checksum algorithm.
      * @param value The hexadecimal representation of the checksum value.
      * @throws NullPointerException     if either argument is null
      * @throws IllegalArgumentException if the value contains non-hexadecimal characters or has the
      *                                  wrong length for the checksum type.
      */
     public Checksum(ChecksumType type, String value) {
         requireNonNull(type, "type may not be null");
         requireNonNull(value, "value may not be null");
 
         this.type = type;
         this.value = normalise(type, value);
 
         checkArgument(HEXADECIMAL.matchesAllOf(this.value),
               "checksum value \"%s\" contains non-hexadecimal digits", value);
 
         checkArgument(this.value.length() == type.getNibbles(),
               "%s requires %s hexadecimal digits but \"%s\" has %s",
               type.getName(), type.getNibbles(), value, this.value.length());
     }
 
     /**
      * Check whether the supplied value is consistent with the given ChecksumType.
      *
      * @param type  The checksum algorithm.
      * @param value The checksum value to verify.
      * @return true if value contains only hexadecimal characters and has the correct length for the
      * supplied algorithm.
      */
     public static boolean isValid(ChecksumType type, String value) {
         String normalised = normalise(type, value);
         return HEXADECIMAL.matchesAllOf(normalised) &&
               normalised.length() == type.getNibbles();
     }
 
     private static String normalise(ChecksumType type, String value) {
         String normalised = value.trim().toLowerCase();
         /**
          * Due to bug in checksum calculation module, some ADLER32
          * sums are stored without leading zeros.
          */
 
         if (type == ADLER32) {
             normalised = padStart(normalised, type.getNibbles(), '0');
         }
 
         return normalised;
     }
 
     public ChecksumType getType() {
         return type;
     }
 
     public String getValue() {
         return value;
     }
 
     @Override
     public boolean equals(Object other) {
         if (other == null) {
             return false;
         }
 
         if (other == this) {
             return true;
         }
 
         if (other.getClass() != this.getClass()) {
             return false;
         }
 
         Checksum that = (Checksum) other;
         return ((this.type == that.type) && this.value.equals(that.value));
     }
 
     @Override
     public int hashCode() {
         return value.hashCode() ^ type.hashCode();
     }
 
     @Override
     public String toString() {
         return type.getType() + ":" + value;
     }
 
 
/** Create a new checksum instance for an already calculated summary of a particular type. */

public static Checksum parseChecksum(String digest) {
    requireNonNull(digest, "digest may not be null");

    int delimiterIndex = digest.indexOf(DELIMITER);
    checkArgument(delimiterIndex != -1, "Invalid checksum format: %s", digest);

    String typeString = digest.substring(0, delimiterIndex);
    String value = digest.substring(delimiterIndex + 1);

    ChecksumType type = ChecksumType.getChecksumType(typeString);
    checkArgument(type != null, "Invalid checksum type: %s", typeString);

    return new Checksum(type, value);
}
 

}