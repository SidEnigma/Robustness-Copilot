/*
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package tech.tablesaw.util;
 
 import com.google.common.base.Strings;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.regex.Pattern;
 
 /**
  * Operations on {@link java.lang.String} that are {@code null} safe.
  *
  * <p>{@code StringUtils} handles {@code null} input Strings quietly. That is to say that a {@code
  * null} input will return {@code null}. Where a {@code boolean} or {@code int} is being returned
  * details vary by method.
  *
  * <p>A side effect of the {@code null} handling is that a {@code NullPointerException} should be
  * considered a bug in {@code StringUtils}.
  *
  * <p>Methods in this class give sample code to explain their operation. The symbol {@code *} is
  * used to indicate any input including {@code null}.
  *
  * <p>#ThreadSafe#
  *
  * @see java.lang.String
  * @since 1.0
  */
 // @Immutable
 public class StringUtils {
   // Performance testing notes (JDK 1.4, Jul03, scolebourne)
   // Whitespace:
   // Character.isWhitespace() is faster than WHITESPACE.indexOf()
   // where WHITESPACE is a string of all whitespace characters
   //
   // Character access:
   // String.charAt(n) versus toCharArray(), then array[n]
   // String.charAt(n) is about 15% worse for a 10K string
   // They are about equal for a length 50 string
   // String.charAt(n) is about 4 times better for a length 3 string
   // String.charAt(n) is best bet overall
   //
   // Append:
   // String.concat about twice as fast as StringBuffer.append
   // (not sure who tested this)
 
   /**
    * The empty String {@code ""}.
    *
    * @since 2.0
    */
   private static final String EMPTY = "";
 
   /** The maximum size to which the padding constant(s) can expand. */
   private static final int PAD_LIMIT = 8192;
 
   private static final Pattern ZERO_DECIMAL_PATTERN = Pattern.compile("\\.0+$");
 
   private StringUtils() {}
 
   // Empty checks
   // -----------------------------------------------------------------------
 
   /**
    * Splits a String by Character type as returned by {@code java.lang.Character.getType(char)}.
    * Groups of contiguous characters of the same type are returned as complete tokens, with the
    * following exception: if {@code camelCase} is {@code true}, the character of type {@code
    * Character.UPPERCASE_LETTER}, if any, immediately preceding a token of type {@code
    * Character.LOWERCASE_LETTER} will belong to the following token rather than to the preceding, if
    * any, {@code Character.UPPERCASE_LETTER} token.
    *
    * @param str the String to split, may be {@code null}
    * @return an array of parsed Strings, {@code null} if null String input
    * @since 2.4
    */
   public static String[] splitByCharacterTypeCamelCase(final String str) {
     if (str == null) {
       return null;
     }
     if (str.isEmpty()) {
       return new String[0];
     }
     final char[] c = str.toCharArray();
     final List<String> list = new ArrayList<>();
     int tokenStart = 0;
     int currentType = Character.getType(c[tokenStart]);
     for (int pos = tokenStart + 1; pos < c.length; pos++) {
       final int type = Character.getType(c[pos]);
       if (type == currentType) {
         continue;
       }
       if (type == Character.LOWERCASE_LETTER && currentType == Character.UPPERCASE_LETTER) {
         final int newTokenStart = pos - 1;
         if (newTokenStart != tokenStart) {
           list.add(new String(c, tokenStart, newTokenStart - tokenStart));
           tokenStart = newTokenStart;
         }
       } else {
         list.add(new String(c, tokenStart, pos - tokenStart));
         tokenStart = pos;
       }
       currentType = type;
     }
     list.add(new String(c, tokenStart, c.length - tokenStart));
     return list.toArray(new String[0]);
   }
 
   // Joining
   // -----------------------------------------------------------------------
 
   /**
    * Joins the elements of the provided array into a single String containing the provided list of
    * elements.
    *
    * <p>No delimiter is added before or after the list. Null objects or empty strings within the
    * array are represented by empty strings.
    *
    * <pre>
    * StringUtils.join(null, *)               = null
    * StringUtils.join([], *)                 = ""
    * StringUtils.join([null], *)             = ""
    * StringUtils.join(["a", "b", "c"], ';')  = "a;b;c"
    * StringUtils.join(["a", "b", "c"], null) = "abc"
    * StringUtils.join([null, "", "a"], ';')  = ";;a"
    * </pre>
    *
    * @param array the array of values to join together, may be null
    * @param separator the separator character to use
    * @return the joined String, {@code null} if null array input
    * @since 2.0
    */
   public static String join(final Object[] array, final char separator) {
     if (array == null) {
       return null;
     }
     return join(array, separator, 0, array.length);
   }
 
   /**
    * Joins the elements of the provided array into a single String containing the provided list of
    * elements.
    *
    * <p>No delimiter is added before or after the list. Null objects or empty strings within the
    * array are represented by empty strings.
    *
    * <pre>
    * StringUtils.join(null, *)               = null
    * StringUtils.join([], *)                 = ""
    * StringUtils.join([null], *)             = ""
    * StringUtils.join(["a", "b", "c"], ';')  = "a;b;c"
    * StringUtils.join(["a", "b", "c"], null) = "abc"
    * StringUtils.join([null, "", "a"], ';')  = ";;a"
    * </pre>
    *
    * @param array the array of values to join together, may be null
    * @param separator the separator character to use
    * @param startIndex the first index to start joining from. It is an error to pass in an end index
    *     past the end of the array
    * @param endIndex the index to stop joining from (exclusive). It is an error to pass in an end
    *     index past the end of the array
    * @return the joined String, {@code null} if null array input
    * @since 2.0
    */
   private static String join(
       final Object[] array, final char separator, final int startIndex, final int endIndex) {
     if (array == null) {
       return null;
     }
     final int noOfItems = endIndex - startIndex;
     if (noOfItems <= 0) {
       return EMPTY;
     }
     final StringBuilder buf = new StringBuilder(noOfItems * 16);
     for (int i = startIndex; i < endIndex; i++) {
       if (i > startIndex) {
         buf.append(separator);
       }
       if (array[i] != null) {
         buf.append(array[i]);
       }
     }
     return buf.toString();
   }
 
   // Padding
   // -----------------------------------------------------------------------
   /**
    * Repeat a String {@code repeat} times to form a new String.
    *
    * <pre>
    * StringUtils.repeat(null, 2) = null
    * StringUtils.repeat("", 0)   = ""
    * StringUtils.repeat("", 2)   = ""
    * StringUtils.repeat("a", 3)  = "aaa"
    * StringUtils.repeat("ab", 2) = "abab"
    * StringUtils.repeat("a", -2) = ""
    * </pre>
    *
    * @param str the String to repeat, may be null
    * @param repeat number of times to repeat str, negative treated as zero
    * @return a new String consisting of the original String repeated, {@code null} if null String
    *     input
    */
   public static String repeat(final String str, final int repeat) {
     // Performance tuned for 2.0 (JDK1.4)
 
     if (str == null) {
       return null;
     }
     if (repeat <= 0) {
       return EMPTY;
     }
     final int inputLength = str.length();
     if (repeat == 1 || inputLength == 0) {
       return str;
     }
     if (inputLength == 1 && repeat <= PAD_LIMIT) {
       return repeat(str.charAt(0), repeat);
     }
 
     final int outputLength = inputLength * repeat;
     switch (inputLength) {
       case 1:
         return repeat(str.charAt(0), repeat);
       case 2:
         final char ch0 = str.charAt(0);
         final char ch1 = str.charAt(1);
         final char[] output2 = new char[outputLength];
         for (int i = repeat * 2 - 2; i >= 0; i--, i--) {
           output2[i] = ch0;
           output2[i + 1] = ch1;
         }
         return new String(output2);
       default:
         final StringBuilder buf = new StringBuilder(outputLength);
         for (int i = 0; i < repeat; i++) {
           buf.append(str);
         }
         return buf.toString();
     }
   }
 
   /**
    * Returns padding using the specified delimiter repeated to a given length.
    *
    * <pre>
    * StringUtils.repeat('e', 0)  = ""
    * StringUtils.repeat('e', 3)  = "eee"
    * StringUtils.repeat('e', -2) = ""
    * </pre>
    *
    * <p>Note: this method does not support padding with <a
    * href="http://www.unicode.org/glossary/#supplementary_character">Unicode Supplementary
    * Characters</a> as they require a pair of {@code char}s to be represented. If you are needing to
    * support full I18N of your applications consider using {@link #repeat(String, int)} instead.
    *
    * @param ch character to repeat
    * @param repeat number of times to repeat char, negative treated as zero
    * @return String with repeated character
    * @see #repeat(String, int)
    */
   private static String repeat(final char ch, final int repeat) {
     if (repeat <= 0) {
       return EMPTY;
     }
     final char[] buf = new char[repeat];
     for (int i = repeat - 1; i >= 0; i--) {
       buf[i] = ch;
     }
     return new String(buf);
   }
 
   /**
    * Gets a CharSequence length or {@code 0} if the CharSequence is {@code null}.
    *
    * @param cs a CharSequence or {@code null}
    * @return CharSequence length or {@code 0} if the CharSequence is {@code null}.
    * @since 2.4
    * @since 3.0 Changed signature from length(String) to length(CharSequence)
    */
   public static int length(final CharSequence cs) {
     return cs == null ? 0 : cs.length();
   }
 
   // Case conversion
   // -----------------------------------------------------------------------
 
 
/** Capitalizes a String changing the first character to title case as per Character#toTitleCase(int). No other characters are changed.  A null input String returns null. */

public static String capitalize(final String str) {
  if (str == null) {
    return null;
  }
  if (str.isEmpty()) {
    return str;
  }
  final char[] c = str.toCharArray();
  c[0] = Character.toTitleCase(c[0]);
  return new String(c);
}
 

}