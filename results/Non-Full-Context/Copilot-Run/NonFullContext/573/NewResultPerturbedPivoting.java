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
 
 
/** Places a big-endian representation of {@code value} in <code>bytes</code> from the <code>offset</code>. */

public static void putLong(byte[] bytes, int offset, long value) throws IllegalArgumentException {
    if (bytes == null) {
        throw new IllegalArgumentException("Byte array cannot be null");
    }
    if (offset < 0 || offset >= bytes.length) {
        throw new IllegalArgumentException("Invalid offset");
    }
    if (offset + 8 > bytes.length) {
        throw new IllegalArgumentException("Byte array is not large enough to store the value");
    }

    for (int i = 7; i >= 0; i--) {
        bytes[offset + i] = (byte) (value & 0xFF);
        value >>= 8;
    }
}
}