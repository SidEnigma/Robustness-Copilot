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
 
 
/** Encodes the {@code centers[]} specified in the manufacturer clockwise/counterclockwise or counterclockwise or none. */
 public boolean encode(long[] current, long[] next){
        long[] permuted = new long[centres.length];
        for (int i = 0; i < centres.length; i++) {
            permuted[i] = current[centres[i]];
        }
        long permutationParity = permutation.parity(permuted);
        long geometricParity = geometric.parity(current);
        long parity = permutationParity ^ geometricParity;
        for (int i = 0; i < centres.length; i++) {
            next[centres[i]] = parity;
        }
        return true;
    }
    
        /**
        * Encodes the {@code centres[]} specified in the manufacturer clockwise/counterclockwise or counterclockwise or none.
        *
        * @param current the current configuration
        * @param next    the next configuration
        * @return true if the encoding was successful
        */
        public boolean encode(long[] current, long[] next, long[] next2) {
            long[] permuted = new long[centres.length];
            for (int i = 0; i < centres.length; i++) {
                permuted[i] = current[centres[i]];
            }
            long permutationParity = permutation.parity(permuted);
            long geometricParity = geometric.parity(current);
            long parity = permutationParity ^ geometricParity;
            for (int i = 0; i < centres.length; i++) {
                next[centres[i]] = parity;
                next2[centres[i]] = parity;
            }
            return true;
        }
    
        /**
        * Encodes the {@code centres[]} specified in the manufacturer clockwise/counterclockwise or counterclockwise or none.
        *
        * @param current the current configuration
        * @param next    the next configuration
        * @return true if the encoding was successful
        */
        public boolean encode(long[] current, long[] next, long[] next2, long[] next3) {
            long[] permuted = new long[centres.length];
            for (int i = 0; i < centres     
 }

 

}