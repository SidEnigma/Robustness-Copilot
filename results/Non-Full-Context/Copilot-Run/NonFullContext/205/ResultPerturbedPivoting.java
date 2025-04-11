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
 
 import org.openscience.cdk.interfaces.IAtomContainer;
 
 import java.util.Arrays;
 
 import static org.openscience.cdk.graph.GraphUtil.EdgeToBondMap;
 
 /**
  * A mutable state for matching graphs using the Ullmann algorithm {@cdk.cite
  * Ullmann76}. There are a couple of modifications in this implementation.
  * Firstly the mappings are stored in two vectors m1 and m2 and simply allows us
  * to return {@link #mapping()} without searching the compatibility matrix.
  * Secondly the compatibility matrix is non-binary and instead of removing
  * entries they are <i>marked</i>. The backtracking then resets these entries
  * rather and avoids storing/copying the matrix between states.
  *
  * @author John May
  * @cdk.module isomorphism
  */
 final class UllmannState extends State {
 
     /** Adjacency list representations. */
     final int[][]               g1, g2;
 
     /** Query and target bond maps. */
     private final EdgeToBondMap bond1, bonds2;
 
     /** The compatibility matrix. */
     final CompatibilityMatrix   matrix;
 
     /** Current mapped values. */
     final int[]                 m1, m2;
 
     /** Size of the current mapping. */
     int                         size     = 0;
 
     /** How bond semantics are matched. */
     private final BondMatcher   bondMatcher;
 
     /** Indicates a vertex is unmapped. */
     private static int          UNMAPPED = -1;
 
     /**
      * Create a state for matching subgraphs using the Ullmann refinement
      * procedure.
      *
      * @param container1  query container
      * @param container2  target container
      * @param g1          query container adjacency list
      * @param g2          target container adjacency list
      * @param bonds1      query container bond map
      * @param bonds2      target container bond map
      * @param atomMatcher method of matching atom semantics
      * @param bondMatcher method of matching bond semantics
      */
     public UllmannState(IAtomContainer container1, IAtomContainer container2, int[][] g1, int[][] g2,
             EdgeToBondMap bonds1, EdgeToBondMap bonds2, AtomMatcher atomMatcher, BondMatcher bondMatcher) {
         this.bondMatcher = bondMatcher;
         this.g1 = g1;
         this.g2 = g2;
         this.bond1 = bonds1;
         this.bonds2 = bonds2;
         this.m1 = new int[g1.length];
         this.m2 = new int[g2.length];
         Arrays.fill(m1, UNMAPPED);
         Arrays.fill(m2, UNMAPPED);
 
         // build up compatibility matrix
         matrix = new CompatibilityMatrix(g1.length, g2.length);
         for (int i = 0; i < g1.length; i++) {
             for (int j = 0; j < g2.length; j++) {
                 if (g1[i].length <= g2[j].length && atomMatcher.matches(container1.getAtom(i), container2.getAtom(j))) {
                     matrix.set(i, j);
                 }
             }
         }
     }
 
     /**{@inheritDoc} */
     @Override
     int nextN(int n) {
         return size; // we progress down the rows of the matrix
     }
 
     /**{@inheritDoc} */
     @Override
     int nextM(int n, int m) {
         for (int i = m + 1; i < g2.length; i++)
             if (m2[i] == UNMAPPED) return i;
         return g2.length;
     }
 
     /**{@inheritDoc} */
     @Override
     int nMax() {
         return g1.length;
     }
 
     /**{@inheritDoc} */
     @Override
     int mMax() {
         return g2.length;
     }
 
     /**{@inheritDoc} */
     @Override
     boolean add(int n, int m) {
 
         if (!matrix.get(n, m)) return false;
 
         // fix the mapping
         matrix.markRow(n, -(n + 1));
         matrix.set(n, m);
 
         // attempt to refine the mapping
         if (refine(n)) {
             size = size + 1;
             m1[n] = m;
             m2[m] = n;
             return true;
         } else {
             // mapping became invalid - unfix mapping
             matrix.resetRows(n, -(n + 1));
             return false;
         }
     }
 
     /**{@inheritDoc} */
     @Override
     void remove(int n, int m) {
         size--;
         m1[n] = m2[m] = UNMAPPED;
         matrix.resetRows(n, -(n + 1));
     }
 
     /**
      * Refines the compatibility removing any mappings which have now become
      * invalid (since the last mapping). The matrix is refined from the row
      * after the current {@code row} - all previous rows are fixed. If when
      * refined we find a query vertex has no more candidates left in the target
      * we can never reach a feasible matching and refinement is aborted (false
      * is returned).
      *
      * @param row refine from here
      * @return match is still feasible
      */
     private boolean refine(int row) {
         int marking = -(row + 1);
         boolean changed;
         do {
             changed = false;
             // for every feasible mapping verify if it is still valid
             for (int n = row + 1; n < matrix.nRows; n++) {
                 for (int m = 0; m < matrix.mCols; m++) {
 
                     if (matrix.get(n, m) && !verify(n, m)) {
 
                         // remove the now invalid mapping
                         matrix.mark(n, m, marking);
                         changed = true;
 
                         // no more mappings for n in the feasibility matrix
                         if (!hasCandidate(n)) return false;
                     }
                 }
             }
         } while (changed);
         return true;
     }
 
 
/** Verify that for each vertex adjacent to n, there must be at least one feasible adjacent candidate that can be mapped. */
 private boolean verify(int n, int m){
        for (int i = 0; i < g1[n].length; i++) {
            int n1 = g1[n][i];
            if (m1[n1] == UNMAPPED) {
                for (int j = 0; j < g2[m].length; j++) {
                    int m2 = g2[m][j];
                    if (m2 != UNMAPPED && matrix.get(n1, m2)) return true;
                }
            }
        }
        return false;
    }
    
        /**
        * Returns true if there are any candidates for the given vertex in the
        * feasibility matrix.
        *
        * @param n vertex to check
        * @return true if there are any candidates
        */
        private boolean hasCandidate(int n) {
            for (int m = 0; m < matrix.mCols; m++) {
                if (matrix.get(n, m)) return true;
            }
            return false;
        }
    
        /**{@inheritDoc} */
        @Override
        int[] mapping() {
            int[] mapping = new int[m1.length];
            for (int i = 0; i < mapping.length; i++) {
                mapping[i] = m1[i];
            }
            return mapping;
        }
    
        /**{@inheritDoc} */
        @Override
        boolean isFeasible() {
            return size == m1.length;
        }
    
        /**{@inheritDoc} */
        @Override
        boolean isComplete() {
            return size == m1.length;
        }
    
        /**{@inheritDoc} */
        @Override
        boolean isEmpty() {
            return size == 0;
        }
    
        /**{@inheritDoc} */
        @Override
        int size() {
            return size;
        }
    
        /**{@inheritDoc} */
        @Override
        int[] getMapping() {
            return mapping();
        }
    
        /**{@in     
 }

 

}