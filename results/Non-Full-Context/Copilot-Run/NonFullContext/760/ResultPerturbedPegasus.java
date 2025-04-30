package edu.harvard.iq.dataverse.util;
 
 import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2LoginBackingBean;
 import java.io.UnsupportedEncodingException;
 import java.security.InvalidKeyException;
 import java.security.MessageDigest;
 import java.security.NoSuchAlgorithmException;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Base64;
 import java.util.Collections;
 import java.util.List;
 import java.util.Optional;
 import java.util.Set;
 import java.util.TreeSet;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import javax.crypto.BadPaddingException;
 import javax.crypto.Cipher;
 import javax.crypto.IllegalBlockSizeException;
 import javax.crypto.NoSuchPaddingException;
 import javax.crypto.spec.SecretKeySpec;
 import org.jsoup.Jsoup;
 
 /**
  *
  * @author skraffmiller
  */
 public class StringUtil {
        
     private static final Logger logger = Logger.getLogger(StringUtil.class.getCanonicalName());
     public static final Set<String> TRUE_VALUES = Collections.unmodifiableSet(new TreeSet<>( Arrays.asList("1","yes", "true","allow")));
     
     public static final boolean nonEmpty( String str ) {
         return ! isEmpty(str);
     }
     
     public static final boolean isEmpty(String str) {
         return str==null || str.trim().equals("");        
     }
     
     public static  String nullToEmpty(String inString) {
         return inString == null ? "" : inString;
     }
 
     public static final boolean isAlphaNumeric(String str) {
       final char[] chars = str.toCharArray();
       for (int x = 0; x < chars.length; x++) {      
         final char c = chars[x];
         if(! isAlphaNumericChar(c)) {
             return false;
         }
       }  
       return true;
     }
     
     public static String substringIncludingLast(String str, String separator) {
       if (isEmpty(str)) {
           return str;
       }
       if (isEmpty(separator)) {
           return "";
       }
       int pos = str.lastIndexOf(separator);
       if (pos == -1 || pos == (str.length() - separator.length())) {
           return "";
       }
       return str.substring(pos);
   }
     
     public static Optional<String> toOption(String s) {
         if ( isEmpty(s) ) {
             return Optional.empty();
         } else {
             return Optional.of(s.trim());
         }
     }
     
     
 
/** If @code s contains a "truthy" value, it's checked. */
 public static boolean isTrue(String s){
        return TRUE_VALUES.contains(s);     
 }

 

}