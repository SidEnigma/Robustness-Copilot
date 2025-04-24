/*
  * This library is free software; you can redistribute it and/or modify
  * it under the terms of the GNU Library General Public License as
  * published by the Free Software Foundation; either version 2 of the
  * License, or (at your option) any later version.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Library General Public License for more details.
  *
  * You should have received a copy of the GNU Library General Public
  * License along with this program (see the file COPYING.LIB for more
  * details); if not, write to the Free Software Foundation, Inc.,
  * 675 Mass Ave, Cambridge, MA 02139, USA.
  */
 package org.dcache.util;
 
 /**
  * @since 0.0.3
  */
 public final class Bytes {
 
     private Bytes() {
     }
 
 
/** Add big-endian representation of value into bytes starting from offset. */
 public static void putLong(byte[] bytes, int offset, long value) throws IllegalArgumentException{
      if (bytes == null) {
          throw new IllegalArgumentException("bytes is null");
      }
      if (offset < 0) {
          throw new IllegalArgumentException("offset is negative");
      }
      if (offset + 8 > bytes.length) {
          throw new IllegalArgumentException("offset + 8 > bytes.length");
      }
      bytes[offset] = (byte) (value >> 56);
      bytes[offset + 1] = (byte) (value >> 48);
      bytes[offset + 2] = (byte) (value >> 40);
      bytes[offset + 3] = (byte) (value >> 32);
      bytes[offset + 4] = (byte) (value >> 24);
      bytes[offset + 5] = (byte) (value >> 16);
      bytes[offset + 6] = (byte) (value >> 8);
      bytes[offset + 7] = (byte) value; 
 }

 

}