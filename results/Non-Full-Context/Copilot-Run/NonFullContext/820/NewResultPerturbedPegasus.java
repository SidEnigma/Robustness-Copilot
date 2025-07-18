/* Copyright (C) 2012  Gilleain Torrance <gilleain.torrance@gmail.com>
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
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 package org.openscience.cdk.group;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 
 
 /**
  * A permutation with some associated methods to multiply, invert, and convert
  * to cycle strings. Much of the code in this was implemented from the
  * C.A.G.E.S. book {@cdk.cite Kreher98}.
  *
  * @author maclean
  * @cdk.module group
  *
  */
 public final class Permutation {
 
     /**
      * The elements of the permutation.
      */
     private final int[] values;
 
     /**
      * Constructs an identity permutation with <code>size</code> elements.
      *
      * @param size the number of elements in the permutation
      */
     public Permutation(int size) {
         this.values = new int[size];
         for (int i = 0; i < size; i++) {
             this.values[i] = i;
         }
     }
 
     /**
      * Make a permutation from a set of values such that p[i] = x for
      * the value x at position i.
      *
      * @param values the elements of the permutation
      */
     public Permutation(int... values) {
         this.values = values;
     }
 
     /**
      * Construct a permutation from another one by cloning the values.
      *
      * @param other the other permutation
      */
     public Permutation(Permutation other) {
         this.values = other.values.clone();
     }
 
     /**
      *{@inheritDoc}
      */
     @Override
     public boolean equals(Object other) {
 
         if (this == other) return true;
         if (other == null || getClass() != other.getClass()) return false;
 
         return Arrays.equals(values, ((Permutation) other).values);
 
     }
 
     /**
      *{@inheritDoc}
      */
     @Override
     public int hashCode() {
         return Arrays.hashCode(values);
     }
 
     /**
      * Check to see if this permutation is the identity permutation.
      *
      * @return true if for all i, p[i] = i
      */
     public boolean isIdentity() {
         for (int i = 0; i < this.values.length; i++) {
             if (this.values[i] != i) {
                 return false;
             }
         }
         return true;
     }
 
     /**
      * Get the number of elements in the permutation.
      *
      * @return the number of elements
      */
     public int size() {
         return this.values.length;
     }
 
     /**
      * Get the value at this index.
      *
      * @param index the permutation value at this index.
      * @return the value at this index
      */
     public int get(int index) {
         return this.values[index];
     }
 
     /**
      * Get all the values as an array.
      *
      * @return the values of the permutation
      */
     public int[] getValues() {
         return this.values;
     }
 
 
/** Find an r such that this is the other. */

public int firstIndexOfDifference(Permutation other) {
    int size = Math.min(this.size(), other.size());
    for (int i = 0; i < size; i++) {
        if (this.get(i) != other.get(i)) {
            return i;
        }
    }
    return size;
}
 

}