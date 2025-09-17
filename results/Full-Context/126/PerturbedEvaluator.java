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
 import org.openscience.cdk.interfaces.IBond;
 
 import static org.openscience.cdk.graph.GraphUtil.EdgeToBondMap;
 
 /**
  * Vento-Foggia (VF) state for matching isomorphisms (identity) {@cdk.cite
  * Cordella04}.  Note: no heuristics or sorting are performed as all and
  * should be checked externally
  *
  * @author John May
  * @cdk.module isomorphism
  */
 final class VFState extends AbstractVFState {
 
     /**
      * The query (container1) and target (container2) of the isomorphism
      * matching.
      */
     private final IAtomContainer container1, container2;
 
     /**
      * Lookup for the query bonds (bonds1) and target bonds (bonds2) of the
      * isomorphism matching.
      */
     private final EdgeToBondMap  bonds1, bonds2;
 
     /** Defines how atoms are matched. */
     private final AtomMatcher    atomMatcher;
 
     /** Defines how bonds are matched. */
     private final BondMatcher    bondMatcher;
 
     /**
      * Create a VF state for matching isomorphisms. The query is passed first
      * and should read as, find container1 in container2.
      *
      * @param container1  the molecule to search for (query)
      * @param container2  the molecule to search in (target)
      * @param g1          adjacency list of the query
      * @param g2          adjacency list of the target
      * @param bonds1      bond lookup of the query
      * @param bonds2      bond lookup of the target
      * @param atomMatcher what semantic attributes (symbol, charge, query)
      *                    determines atoms to be compatible
      * @param bondMatcher what semantic attributes (order/aromatic, query)
      *                    determines bonds to be compatible
      */
     VFState(IAtomContainer container1, IAtomContainer container2, int[][] g1, int[][] g2, EdgeToBondMap bonds1,
             EdgeToBondMap bonds2, AtomMatcher atomMatcher, BondMatcher bondMatcher) {
         super(g1, g2);
         this.container1 = container1;
         this.container2 = container2;
         this.bonds1 = bonds1;
         this.bonds2 = bonds2;
         this.atomMatcher = atomMatcher;
         this.bondMatcher = bondMatcher;
     }
 
 
/** Checks the feasibility of the candidate pair {n, m}. If all k-look-ahead rules are valid a candidate pair is syntactically feasible. The adjacency relation of the mapping is checked by the look-ahead rules. */
 boolean feasible(int n, int m){}

                                                                          }
