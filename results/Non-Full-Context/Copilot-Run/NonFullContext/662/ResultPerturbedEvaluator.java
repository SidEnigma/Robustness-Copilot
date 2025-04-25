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
 
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IDoubleBondStereochemistry;
 import org.openscience.cdk.interfaces.IStereoElement;
 import org.openscience.cdk.interfaces.ITetrahedralChirality;
 
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.function.Predicate;
 
 import static org.openscience.cdk.interfaces.IDoubleBondStereochemistry.Conformation;
 import static org.openscience.cdk.interfaces.IDoubleBondStereochemistry.Conformation.TOGETHER;
 import static org.openscience.cdk.interfaces.ITetrahedralChirality.Stereo.CLOCKWISE;
 
 /**
  * Filters out (sub)graph-isomorphism matches that have invalid stereochemistry
  * configuration. The class is not currently set up to handle partial mappings
  * (MCS) but could easily be extended to handle such cases.  The class
  * implements the Guava predicate and can be used easily filter the mappings.
  *
  * <blockquote><pre>{@code
  * Predicate<int[]> f              = new StereoMatch(query, target);
  * Iterable<int[]>  mappings       = ...; // from subgraph isomorphism etc.
  * Iterable<int[]>  stereoMappings = Iterables.filter(mappings, f);
  * }</pre></blockquote>
  *
  * @author John May
  * @cdk.module isomorphism
  */
 final class StereoMatch implements Predicate<int[]> {
 
     /** Query and target contains. */
     private final IAtomContainer query, target;
 
     /** Atom to atom index lookup. */
     private final Map<IAtom, Integer> queryMap, targetMap;
 
     /** Indexed array of stereo elements. */
     private final IStereoElement[]    queryElements, targetElements;
 
     /** Indexed array of stereo element types. */
     private final Type[]              queryTypes, targetTypes;
 
     /** Indices of focus atoms of stereo elements. */
     private final int[]               queryStereoIndices, targetStereoIndices;
 
     /**
      * Indicates the stereo group config for a given atom idx, 0=unsed, 1=stored, -1=inverted.
      * Initially all entries start as 0, if we hit a stereo-element in a group &1, &2, or1, or2
      * then we check if we have already "set" the group, if not then we "set" the group to make
      * the first element match, this means we may choose to flip the group to be the enantiomer.
      */
     private int[] groupConfigAdjust;
 
     /**
      * Create a predicate for checking mappings between a provided
      * {@code query} and {@code target}.
      *
      * @param query query container
      * @param target target container
      */
     StereoMatch(IAtomContainer query, IAtomContainer target) {
         this.query = query;
         this.target = target;
         this.queryMap = indexAtoms(query);
         this.targetMap = indexAtoms(target);
         this.queryElements = new IStereoElement[query.getAtomCount()];
         this.targetElements = new IStereoElement[target.getAtomCount()];
         this.queryTypes = new Type[query.getAtomCount()];
         this.targetTypes = new Type[target.getAtomCount()];
 
         queryStereoIndices = indexElements(queryMap, queryElements, queryTypes, query);
         targetStereoIndices = indexElements(targetMap, targetElements, targetTypes, target);
     }
 
     /**
      * Is the {@code mapping} of the stereochemistry in the query preserved in
      * the target.
      *
      * @param mapping permutation of the query vertices
      * @return the stereo chemistry is value
      */
     @Override
     public boolean test(final int[] mapping) {
 
         // n.b. not true for unspecified queries e.g. [C@?H](*)(*)*
         if (queryStereoIndices.length > targetStereoIndices.length) return false;
 
         // reset augment group config if it was initialised
         if (groupConfigAdjust != null)
             Arrays.fill(groupConfigAdjust, 0);
 
         for (final int u : queryStereoIndices) {
             switch (queryTypes[u]) {
                 case Tetrahedral:
                     if (!checkTetrahedral(u, mapping)) return false;
                     break;
                 case Geometric:
                     if (!checkGeometric(u, otherIndex(u), mapping)) return false;
                     break;
             }
         }
         return true;
     }
 
     /**
      * Backwards compatible method from when we used GUAVA predicates.
      * @param ints atom index bijection
      * @return true/false
      * @see #test(int[])
      */
     public boolean apply(int[] ints) {
         return test(ints);
     }
 
     /**
      * Verify the tetrahedral stereochemistry (clockwise/anticlockwise) of atom
      * {@code u} is preserved in the target when the {@code mapping} is used.
      *
      * @param u       tetrahedral index in the target
      * @param mapping mapping of vertices
      * @return the tetrahedral configuration is preserved
      */
     private boolean checkTetrahedral(int u, int[] mapping) {
         int v = mapping[u];
         if (targetTypes[v] != Type.Tetrahedral) return false;
 
         ITetrahedralChirality queryElement = (ITetrahedralChirality) queryElements[u];
         ITetrahedralChirality targetElement = (ITetrahedralChirality) targetElements[v];
 
         // access neighbors of each element, then map the query to the target
         int[] us = neighbors(queryElement, queryMap);
         int[] vs = neighbors(targetElement, targetMap);
         us = map(u, v, us, mapping);
 
         if (us == null) return false;
 
         int p = permutationParity(us) * parity(queryElement.getStereo());
         int q = permutationParity(vs) * parity(targetElement.getStereo());
 
         int groupInfo = targetElement.getGroupInfo();
         if (groupInfo != 0) {
             if (groupConfigAdjust == null)
                 groupConfigAdjust = new int[target.getAtomCount()];
 
             // 'set' the group either to be 'as stored' or 'flipped'
             if (groupConfigAdjust[v] == 0) {
                 int adjust = p == q ? +1 : -1;
                 for (int idx : targetStereoIndices) {
                     if (targetElements[idx].getGroupInfo() == groupInfo)
                         groupConfigAdjust[idx] = adjust;
                 }
             }
 
             // make the adjustment
             q *= groupConfigAdjust[v];
         }
 
         return p == q;
     }
 
     /**
      * Transforms the neighbors {@code us} adjacent to {@code u} into the target
      * indices using the mapping {@code mapping}. The transformation accounts
      * for an implicit hydrogen in the query being an explicit hydrogen in the
      * target.
      *
      * @param u       central atom of tetrahedral element
      * @param v       mapped central atom of the tetrahedral element
      * @param us      neighboring vertices of u (u plural)
      * @param mapping mapping from the query to the target
      * @return the neighbors us, transformed into the neighbors around v
      */
     private int[] map(int u, int v, int[] us, int[] mapping) {
 
         // implicit hydrogen in query but explicit in target, modify the mapping
         // such that the central atom, u, mapps to the hydrogen
         if (query.getAtom(u).getImplicitHydrogenCount() == 1 && target.getAtom(v).getImplicitHydrogenCount() == 0) {
             IAtom explicitHydrogen = findHydrogen(((ITetrahedralChirality) targetElements[v]).getLigands());
             // the substructure had a hydrogen but the superstructure did not
             // the matching is not possible - if we allowed the mapping then
             // we would have different results for implicit/explicit hydrogens
             if (explicitHydrogen == null) return null;
             mapping[u] = targetMap.get(explicitHydrogen);
         }
 
         for (int i = 0; i < us.length; i++)
             us[i] = mapping[us[i]];
 
         mapping[u] = v; // remove temporary mapping to hydrogen
         return us;
     }
 
 
/** Checks whether the geometric stereochemistry configuration is preserved. */
 private boolean checkGeometric(int u1, int u2, int[] mapping){
        int v1 = mapping[u1];
        int v2 = mapping[u2];
        if (targetTypes[v1] != Type.Geometric) return false;
        if (targetTypes[v2] != Type.Geometric) return false;
    
        IGeometricChirality queryElement = (IGeometricChirality) queryElements[u1];
        IGeometricChirality targetElement = (IGeometricChirality) targetElements[v1];
    
        // access neighbors of each element, then map the query to the target
        int[] us = neighbors(queryElement, queryMap);
        int[] vs = neighbors(targetElement, targetMap);
        us = map(u1, v1, us, mapping);
        vs = map(u2, v2, vs, mapping);
    
        if (us == null || vs == null) return false;
    
        int p = permutationParity(us) * parity(queryElement.getStereo());
        int q = permutationParity(vs) * parity(targetElement.getStereo());
    
        return p == q;
    }
    
        /**
        * Find the neighbors of {@code element} in the query.
        *
        * @param element element to find neighbors of
        * @param map     mapping from query to target
        * @return the neighbors of {@code element} in the query
        */
        private int[] neighbors(IStereoElement element, Map<IAtom, Integer> map) {
            int[] neighbors = new int[element.getAtoms().length];
            for (int i = 0; i < neighbors.length; i++) {
                IAtom atom = element.getAtoms()[i];
                neighbors[i] = map.get(atom);
            }
            return neighbors;
        }
    
        /**
        * Find the parity of the permutation {@code permutation}.
        *
        * @param permutation permutation to find parity of
        * @return the parity of the permutation
        */
        private int parity(int[] permutation) {
            int parity = 1      
 }

 

}