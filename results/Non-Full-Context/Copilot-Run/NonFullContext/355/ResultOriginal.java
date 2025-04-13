/*
  * Copyright (c) 2014 European Bioinformatics Institute (EMBL-EBI)
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
 
 package org.openscience.cdk.forcefield.mmff;
 
 import org.openscience.cdk.exception.Intractable;
 import org.openscience.cdk.graph.Cycles;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import static org.openscience.cdk.graph.GraphUtil.EdgeToBondMap;
 
 /**
  * Assign MMFF aromatic atom types from the preliminary symbolic type. The assignment is described
  * in the appendix of {@cdk.cite Halgren96a}:
  *
  * For non-hydrogen atoms, the assignment of symbolic MMFF atom types takes place in two stages. In
  * the first, a provisional atom type is assigned based on local connectivity. In the second,
  * aromatic systems are perceived, and properly qualified aromatic atom types are assigned based on
  * ring size and, for five-membered rings, on the position within the ring. Information in this file
  * (MMFFAROM.PAR) is used to make the proper correspondence between provisional and final (aromatic)
  * atom types. 
  *
  * The column labeled "L5" refers, in the case of 5-ring systems, to the position of the atom in
  * question relative to the unique pi-lone-pair containing heteroatom (which itself occupies
  * position "1"); a "4" is an artificial entry that is assigned when no such unique heteroatom
  * exists, as for example occurs in imidazolium cations and in tetrazole anions. An entry of "1" in
  * the "IM CAT" or "N5 ANION" column must also be matched for such ionic species to convert the
  * "OLD" (preliminary) to "AROM" (aromatic) symbolic atom type. Note: in matching the "OLD" symbolic
  * atom types, an "exact" match is first attempted. If this match fails, a wild-carded match, using
  * for example "C*" is then employed. 
  *
  * This class implements this in three stages. Firstly, the aromatic rings are found with {@link
  * #findAromaticRings(int[][], int[], int[])}. These rings are then parsed to {@link
  * #updateAromaticTypesInSixMemberRing(int[], String[])} and {@link #updateAromaticTypesInFiveMemberRing(int[],
  * String[])}. The more complex of the two is the five member rings that normalises the ring to put
  * the 'pi-lone-pair' hetroatom in position 1. The alpha and beta positions are then fixed and the
  * {@link #alphaTypes} and {@link #betaTypes} mappings are used to obtain the correct assignment.
  *
  * @author John May
  */
 final class MmffAromaticTypeMapping {
 
     /**
      * Create an instance to map from preliminary MMFF symbolic types to their aromatic equivalent.
      */
     MmffAromaticTypeMapping() {}
 
 
/** Given the assigned preliminary MMFF atom types (symbs[]) update these to the aromatic types. */
 void assign(IAtomContainer container, String[] symbs, EdgeToBondMap bonds, int[][] graph, Set<IBond> mmffArom){
      // find the aromatic rings
      int[] ringSizes = findAromaticRings(graph, bonds, mmffArom);
  
      // update the aromatic types in the rings
      updateAromaticTypesInSixMemberRing(ringSizes, symbs);
      updateAromaticTypesInFiveMemberRing(ringSizes, symbs);
  
      // update the atom types
      for (int i = 0; i < container.getAtomCount(); i++) {
          container.getAtom(i).setSymbol(symbs[i]);
      }
  }
  
      /**
        * Find the aromatic rings in the graph.
        *
        * @param graph the graph to find the rings in
        * @param bonds the bond map
        * @param mmffArom the aromatic bonds
        * @return the ring sizes
        */
      private int[] findAromaticRings(int[][] graph, EdgeToBondMap bonds, Set<IBond> mmffArom) {
          // find the aromatic rings
          int[] ringSizes = Cycles.findCycles(graph, bonds, mmffArom);
  
          // sort the rings by size
          Arrays.sort(ringSizes);
          return ringSizes;
      }
  
      /**
        * Update the aromatic types in the six member rings.
        *
        * @param ringSizes the ring sizes
        * @param symbs the preliminary MMFF atom types
        */
      private void updateAromaticTypesInSixMemberRing(int[] ringSizes, String[] symbs) {
          // update the aromatic types in the six member rings
          for (int i = 0; i < ringSizes.length; i++) {
              if (ringSizes[i] == 6) {
                  updateAromaticTypesInSixMemberRing(i, symbs);
              }
          }
      }
  
      /**
        * Update the aromatic types in the six member ring.
        *
        * @param ringSize the ring size
        * @param symbs the preliminary MMFF atom types
        */
      private void updateAromaticTypesIn  
 }

 

}