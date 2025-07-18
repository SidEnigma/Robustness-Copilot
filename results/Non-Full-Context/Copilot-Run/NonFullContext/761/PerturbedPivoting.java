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
 
     /**
      * Given the assigned preliminary MMFF atom types (symbs[]) update these to the aromatic types.
      * To begin, all the 5 and 6 member aromatic cycles are discovered. The symbolic types of five
      * and six member cycles are then update with {@link #updateAromaticTypesInFiveMemberRing(int[],
      * String[])} and {@link #updateAromaticTypesInSixMemberRing(int[], String[])}.
      *
      * @param container structure representation
      * @param symbs     vector of symbolic types for the whole structure
      * @param bonds     edge to bond map lookup
      * @param graph     adjacency list graph representation of structure
      * @param mmffArom  set of bonds that are aromatic
      */
     void assign(IAtomContainer container, String[] symbs, EdgeToBondMap bonds, int[][] graph, Set<IBond> mmffArom) {
 
         int[] contribution = new int[graph.length];
         int[] doubleBonds = new int[graph.length];
         Arrays.fill(doubleBonds, -1);
         setupContributionAndDoubleBonds(container, bonds, graph, contribution, doubleBonds);
 
         int[][] cycles = findAromaticRings(cyclesOfSizeFiveOrSix(container, graph), contribution, doubleBonds);
 
         for (int[] cycle : cycles) {
             int len = cycle.length - 1;
             if (len == 6) {
                 updateAromaticTypesInSixMemberRing(cycle, symbs);
             }
             if (len == 5 && normaliseCycle(cycle, contribution)) {
                 updateAromaticTypesInFiveMemberRing(cycle, symbs);
             }
             // mark aromatic bonds
             for (int i = 1; i < cycle.length; i++)
                 mmffArom.add(bonds.get(cycle[i], cycle[i - 1]));
         }
     }
 
     /**
      * From a provided set of cycles find the 5/6 member cycles that fit the MMFF aromaticity
      * definition - {@link #isAromaticRing(int[], int[], int[], boolean[])}. The cycles of size 6
      * are listed first.
      *
      * @param cycles       initial set of cycles from
      * @param contribution vector of p electron contributions from each vertex
      * @param dbs          vector of double-bond pairs, index stored double-bonded index
      * @return the cycles that are aromatic
      */
     private static int[][] findAromaticRings(int[][] cycles, int[] contribution, int[] dbs) {
 
         // loop control variables, the while loop continual checks all cycles
         // until no changes are found
         boolean found;
         boolean[] checked = new boolean[cycles.length];
 
         // stores the aromatic atoms as a bit set and the aromatic bonds as
         // a hash set. the aromatic bonds are the result of this method but the
         // aromatic atoms are needed for checking each ring
         final boolean[] aromaticAtoms = new boolean[contribution.length];
 
         final List<int[]> ringsOfSize6 = new ArrayList<int[]>();
         final List<int[]> ringsOfSize5 = new ArrayList<int[]>();
 
         do {
             found = false;
             for (int i = 0; i < cycles.length; i++) {
 
                 // note paths are closed walks and repeat first/last vertex so
                 // the true length is one less
                 int[] cycle = cycles[i];
                 int len = cycle.length - 1;
 
                 if (checked[i]) continue;
 
                 if (isAromaticRing(cycle, contribution, dbs, aromaticAtoms)) {
                     checked[i] = true;
                     found |= true;
                     for (int j = 0; j < len; j++) {
                         aromaticAtoms[cycle[j]] = true;
                     }
                     if (len == 6)
                         ringsOfSize6.add(cycle);
                     else if (len == 5) ringsOfSize5.add(cycle);
 
                 }
             }
         } while (found);
 
         List<int[]> rings = new ArrayList<int[]>();
         rings.addAll(ringsOfSize6);
         rings.addAll(ringsOfSize5);
 
         return rings.toArray(new int[rings.size()][]);
     }
 
     /**
      * Check if a cycle/ring is aromatic. A cycle is aromatic if the sum of its p electrons is equal
      * to 4n+2. Double bonds can only contribute if they are in the cycle being tested or are
      * already delocalised.
      *
      * @param cycle        closed walk of vertices in the cycle
      * @param contribution vector of p electron contributions from each vertex
      * @param dbs          vector of double-bond pairs, index stored double-bonded index
      * @param aromatic     binary set of aromatic atoms
      * @return whether the ring is aromatic
      */
     static boolean isAromaticRing(int[] cycle, int[] contribution, int[] dbs, boolean[] aromatic) {
 
         int len = cycle.length - 1;
         int sum = 0;
 
         int i = 0;
         int iPrev = len - 1;
         int iNext = 1;
 
         while (i < len) {
 
             int prev = cycle[iPrev];
             int curr = cycle[i];
             int next = cycle[iNext];
 
             int pElectrons = contribution[curr];
 
             if (pElectrons < 0) return false;
 
             // single p electrons are only donated from double bonds, these are
             // only counted if the bonds are either in this ring or the bond
             // is aromatic
             if (pElectrons == 1) {
                 final int other = dbs[curr];
                 if (other < 0) return false;
                 if (other != prev && other != next && !aromatic[other]) return false;
             }
 
             iPrev = i;
             i = iNext;
             iNext = iNext + 1;
             sum += pElectrons;
         }
 
         // the sum of electrons 4n+2?
         return (sum - 2) % 4 == 0;
     }
 
     /**
      * Update aromatic atom types in a six member ring. The aromatic types here are hard coded from
      * the 'MMFFAROM.PAR' file.
      *
      * @param cycle 6-member aromatic cycle / ring
      * @param symbs vector of symbolic types for the whole structure
      */
     static void updateAromaticTypesInSixMemberRing(int[] cycle, String[] symbs) {
         for (final int v : cycle) {
             if (NCN_PLUS.equals(symbs[v]) || "N+=C".equals(symbs[v]) || "N=+C".equals(symbs[v]))
                 symbs[v] = "NPD+";
             else if ("N2OX".equals(symbs[v]))
                 symbs[v] = "NPOX";
             else if ("N=C".equals(symbs[v]) || "N=N".equals(symbs[v]))
                 symbs[v] = "NPYD";
             else if (symbs[v].startsWith("C")) symbs[v] = "CB";
         }
     }
 
     /**
      * Update the symbolic for a 5-member cycle/ring. The cycle should first be normalised with
      * {@link #normaliseCycle(int[], int[])} to put the unique 'pi-lone-pair' in position 1 (index
      * 0). Using predefined mappings the symbolic atom types are updated in the 'symbs[]' vector.
      *
      * @param cycle normalised 5-member cycle (6 indices)
      * @param symbs vector of symbolic types for the whole structure
      */
     private void updateAromaticTypesInFiveMemberRing(int[] cycle, String[] symbs) {
 
         final String hetro = symbs[cycle[0]];
 
         // simple conditions tell is the 'IM' and 'AN' flags
         final boolean imidazolium = NCN_PLUS.equals(hetro) || NGD_PLUS.equals(hetro);
         final boolean anion = "NM".equals(hetro);
 
         symbs[cycle[0]] = hetroTypes.get(hetro);
 
         symbs[cycle[1]] = getAlphaAromaticType(symbs[cycle[1]], imidazolium, anion);
         symbs[cycle[4]] = getAlphaAromaticType(symbs[cycle[4]], imidazolium, anion);
         symbs[cycle[2]] = getBetaAromaticType(symbs[cycle[2]], imidazolium, anion);
         symbs[cycle[3]] = getBetaAromaticType(symbs[cycle[3]], imidazolium, anion);
 
     }
 
     /**
      * Convenience method to obtain the aromatic type of a symbolic (SYMB) type in the alpha
      * position of a 5-member ring. This method delegates to {@link #getAromaticType(java.util.Map,
      * char, String, boolean, boolean)} setup for alpha atoms.
      *
      * @param symb        symbolic atom type
      * @param imidazolium imidazolium flag (IM naming from MMFFAROM.PAR)
      * @param anion       anion flag (AN naming from MMFFAROM.PAR)
      * @return the aromatic type
      */
     private String getAlphaAromaticType(String symb, boolean imidazolium, boolean anion) {
         return getAromaticType(alphaTypes, 'A', symb, imidazolium, anion);
     }
 
     /**
      * Convenience method to obtain the aromatic type of a symbolic (SYMB) type in the beta position
      * of a 5-member ring. This method delegates to {@link #getAromaticType(java.util.Map, char,
      * String, boolean, boolean)} setup for beta atoms.
      *
      * @param symb        symbolic atom type
      * @param imidazolium imidazolium flag (IM naming from MMFFAROM.PAR)
      * @param anion       anion flag (AN naming from MMFFAROM.PAR)
      * @return the aromatic type
      */
     private String getBetaAromaticType(String symb, boolean imidazolium, boolean anion) {
         return getAromaticType(betaTypes, 'B', symb, imidazolium, anion);
     }
 
     /**
      * Obtain the aromatic atom type for an atom in the alpha or beta position of a 5-member
      * aromatic ring. The method primarily uses an HashMap to lookup up the aromatic type. The two
      * maps are, {@link #alphaTypes} and {@link #betaTypes}. Depending on the position (alpha or
      * beta), one map is passed to the method. The exceptions to using the HashMap directly are as
      * follows: 1) if AN flag is raised and the symbolic type is a nitrogen, the type is 'N5M'. 2)
      * If the IM or AN flag is raised, the atom is 'C5' or 'N5 instead of 'C5A', 'C5B', 'N5A', or
      * 'N5B'. This is because the hetroatom in these rings can resonate and so the atom is both
      * alpha and beta.
      *
      * @param map         mapping of alpha or beta types
      * @param suffix      'A' or 'B'
      * @param symb        input symbolic type
      * @param imidazolium imidazolium flag (IM naming from MMFFAROM.PAR)
      * @param anion       anion flag (AN naming from MMFFAROM.PAR)
      * @return the aromatic type
      */
     static String getAromaticType(Map<String, String> map, char suffix, String symb, boolean imidazolium, boolean anion) {
         if (anion && symb.startsWith("N")) symb = "N5M";
         if (map.containsKey(symb)) symb = map.get(symb);
         if ((imidazolium || anion) && symb.charAt(symb.length() - 1) == suffix)
             symb = symb.substring(0, symb.length() - 1);
         return symb;
     }
 
 
/** Find the index of a hetroatoma in a cycle. */
 static int indexOfHetro(int[] cycle, int[] contribution){}

 

}