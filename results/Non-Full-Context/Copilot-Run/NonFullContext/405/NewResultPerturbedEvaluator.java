/*
  * Copyright (c) 2013 European Bioinformatics Institute (EMBL-EBI)
  *                    John May <jwmay@users.sf.net>
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as published by
  * the Free Software Foundation; either version 2.1 of the License, or (at
  * your option) any later version. All we ask is that proper credit is given
  * for our work, which includes - but is not limited to - adding the above
  * copyright notice to the beginning of your source code files, and to any
  * copyright notice that you may distribute with programs based on this work.
  *
  * This program is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  * License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 U
  */
 
 package org.openscience.cdk.isomorphism;
 
 
 import java.util.Arrays;
 
 /**
  * A state for the Vento-Foggia (VF) algorithm. The state allows adding and
  * removing of mappings as well as generating the new candidate mappings {@link
  * #nextN(int)} and {@link #nextM(int, int)}. The feasibility check is left for
  * subclasses to implement.
  *
  * @author John May
  * @cdk.module isomorphism
  */
 abstract class AbstractVFState extends State {
 
     /** Value indicates a vertex is unmapped. */
     protected static final int UNMAPPED = -1;
 
     /** Adjacency list representation of the containers. */
     protected final int[][]    g1, g2;
 
     /** Mapping - m1 is the the mapping from g1 to g1, m2 is from g2 to g1. */
     protected final int[]      m1, m2;
 
     /** The (terminal) vertices which are adjacent to each mapped pair. */
     protected final int[]      t1, t2;
 
     /** Size of current solution - the number of vertices matched. */
     protected int              size;
 
     /**
      * Create a state which will be used to match g1 in g2.
      *
      * @param g1 find this graph
      * @param g2 search this graph
      */
     public AbstractVFState(final int[][] g1, final int[][] g2) {
         this.g1 = g1;
         this.g2 = g2;
         this.m1 = new int[g1.length];
         this.m2 = new int[g2.length];
         this.t1 = new int[g1.length];
         this.t2 = new int[g2.length];
         size = 0;
         Arrays.fill(m1, UNMAPPED);
         Arrays.fill(m2, UNMAPPED);
     }
 
     /**
      * Given the current query candidate (n), find the next candidate. The next
      * candidate is the next vertex > n (in some ordering) that is unmapped and
      * is adjacent to a mapped vertex (terminal). If there is no such vertex
      * (disconnected) the next unmapped vertex is returned. If there are no more
      * candidates m == |V| of G1.
      *
      * @param n previous candidate n
      * @return the next value of n
      */
     @Override
     final int nextN(int n) {
         if (size == 0) return 0;
         for (int i = n + 1; i < g1.length; i++)
             if (m1[i] == UNMAPPED && t1[i] > 0) return i;
         for (int i = n + 1; i < g1.length; i++)
             if (m1[i] == UNMAPPED) return i;
         return nMax();
     }
 
 
/** Given the current target candidate, it returns the next candidate, the verted that is unmpapped and adjacent to a terminal mapped vertex. If it can not find the vertex, it returns the next unmapped vertex or the cardinality of the vertex if there are no more candidates. */

final int nextM(int n, int m) {
    if (size == 0) return 0;
    for (int i = n + 1; i < g2.length; i++) {
        if (m2[i] == UNMAPPED && t2[i] > 0 && isAdjacentMapped(i, m)) {
            return i;
        }
    }
    for (int i = n + 1; i < g2.length; i++) {
        if (m2[i] == UNMAPPED) {
            return i;
        }
    }
    return mMax();
}
 

}