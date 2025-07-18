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
 
 
 import java.util.Arrays;
 
 /**
  * Given a geometric parity and a permutation parity encode the parity of the
  * combination at the specified stereo centre indices.
  *
  * @author John May
  * @cdk.module hash
  */
 final class GeometryEncoder implements StereoEncoder {
 
     /* value for a clockwise configuration */
     private static final long       CLOCKWISE     = 15543053;
 
     /* value for a anticlockwise configuration */
     private static final long       ANTICLOCKWISE = 15521419;
 
     /* for calculation the permutation parity */
     private final PermutationParity permutation;
 
     /* for calculating the geometric parity */
     private final GeometricParity   geometric;
 
     /* index to encode */
     private final int[]             centres;
 
     /**
      * Create a new encoder for multiple stereo centres (specified as an
      * array).
      *
      * @param centres     the stereo centres which will be configured
      * @param permutation calculator for permutation parity
      * @param geometric   geometric calculator
      * @throws IllegalArgumentException if the centres[] were empty
      */
     public GeometryEncoder(int[] centres, PermutationParity permutation, GeometricParity geometric) {
         if (centres.length == 0) throw new IllegalArgumentException("no centres[] provided");
         this.permutation = permutation;
         this.geometric = geometric;
         this.centres = Arrays.copyOf(centres, centres.length);
     }
 
     /**
      * Convenience method to create a new encoder for a single stereo centre.
      *
      * @param centre      a stereo centre which will be configured
      * @param permutation calculator for permutation parity
      * @param geometric   geometric calculator
      * @throws IllegalArgumentException if the centres[] were empty
      */
     public GeometryEncoder(int centre, PermutationParity permutation, GeometricParity geometric) {
         this(new int[]{centre}, permutation, geometric);
     }
 
 
/** Returns true if the encoding of the centres[] as clockwise/anticlockwise has been successfull, false otherwise. */

public boolean encode(long[] current, long[] next) {
    if (current.length != centres.length || next.length != centres.length) {
        throw new IllegalArgumentException("Invalid input arrays");
    }
    
    long currentEncoding = 0;
    long nextEncoding = 0;
    
    for (int i = 0; i < centres.length; i++) {
        int centre = centres[i];
        
        if (current[i] == centre) {
            currentEncoding |= CLOCKWISE << (i * 2);
        } else if (current[i] == -centre) {
            currentEncoding |= ANTICLOCKWISE << (i * 2);
        } else {
            return false;
        }
        
        if (next[i] == centre) {
            nextEncoding |= CLOCKWISE << (i * 2);
        } else if (next[i] == -centre) {
            nextEncoding |= ANTICLOCKWISE << (i * 2);
        } else {
            return false;
        }
    }
    
    return permutation.encode(currentEncoding, nextEncoding) && geometric.encode(currentEncoding, nextEncoding);
}
 

}