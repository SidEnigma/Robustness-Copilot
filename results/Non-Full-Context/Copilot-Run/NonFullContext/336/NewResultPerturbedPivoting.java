/*
  * Copyright (c) 2013 John May <jwmay@users.sf.net>
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public License
  * as published by the Free Software Foundation; either version 2.1
  * of the License, or (at your option) any later version.
  * All we ask is that proper credit is given for our work, which includes
  * - but is not limited to - adding the above copyright notice to the beginning
  * of your source code files, and to any copyright notice that you may distribute
  * with programs based on this work.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 U
  */
 
 package org.openscience.cdk.hash.stereo;
 
 
 /**
  * A basic implementation suitable for determining the parity of the indicates a
  * provided sub-array.
  *
  * @author John May
  * @cdk.module hash
  * @cdk.githash
  */
 final class BasicPermutationParity extends PermutationParity {
 
     private final int[] indices;
 
     /**
      * Create a permutation parity for the provided indices.
      *
      * @param indices sub-array of indices
      * @throws NullPointerException     the provided indices were null
      * @throws IllegalArgumentException less then two indices provided
      */
     public BasicPermutationParity(int[] indices) {
         if (indices == null) throw new NullPointerException("no indices[] provided");
         if (indices.length < 2)
             throw new IllegalArgumentException("at least 2 incides required,"
                     + "use PermutationParity.IDENTITY for single neighbors");
         this.indices = indices;
     }
 
 
/** The numerical values to be checked are usually small ({@literal < 5}) and so we use brute force to count the number of inversions. */
public int parity(long[] current) {
    int count = 0;
    for (int i = 0; i < current.length - 1; i++) {
        for (int j = i + 1; j < current.length; j++) {
            if (current[i] > current[j]) {
                count++;
            }
        }
    }
    return count % 2;
}
 

}