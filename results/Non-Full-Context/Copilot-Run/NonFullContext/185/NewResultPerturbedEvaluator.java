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
 
 
/** The {@code} mapping of the stereochemistry in the query is stored in the destination */

public boolean test(final int[] mapping) {
    // The mapping of the stereochemistry in the query is stored in the destination
    Map<IAtom, IAtom> queryToTargetMap = new HashMap<>();
    
    // Create a mapping of query atoms to target atoms based on the given mapping
    for (int i = 0; i < queryStereoIndices.length; i++) {
        int queryIndex = queryStereoIndices[i];
        int targetIndex = mapping[queryIndex];
        IAtom queryAtom = queryElements[queryIndex].getAtom();
        IAtom targetAtom = targetElements[targetIndex].getAtom();
        queryToTargetMap.put(queryAtom, targetAtom);
    }
    
    // Check if the stereochemistry in the query matches the stereochemistry in the target
    for (int i = 0; i < queryStereoIndices.length; i++) {
        int queryIndex = queryStereoIndices[i];
        int targetIndex = mapping[queryIndex];
        IStereoElement queryElement = queryElements[queryIndex];
        IStereoElement targetElement = targetElements[targetIndex];
        
        if (queryElement instanceof ITetrahedralChirality && targetElement instanceof ITetrahedralChirality) {
            ITetrahedralChirality queryChirality = (ITetrahedralChirality) queryElement;
            ITetrahedralChirality targetChirality = (ITetrahedralChirality) targetElement;
            
            IAtom[] queryLigands = queryChirality.getLigands();
            IAtom[] targetLigands = targetChirality.getLigands();
            
            IAtom queryCentralAtom = queryChirality.getChiralAtom();
            IAtom targetCentralAtom = queryToTargetMap.get(queryCentralAtom);
            
            if (!Arrays.equals(queryLigands, targetLigands) || !queryCentralAtom.equals(targetCentralAtom)) {
                return false;
            }
        } else if (queryElement instanceof IDoubleBondStereochemistry && targetElement instanceof IDoubleBondStereochemistry) {
            IDoubleBondStereochemistry queryStereo = (IDoubleBondStereochemistry) queryElement;
            IDoubleBondStereochemistry targetStereo = (IDoubleBondStereochemistry) targetElement;
            
            IBond queryBond = queryStereo.getStereoBond();
            IBond targetBond = targetStereo.getStereoBond();
            
            IAtom[] queryAtoms = queryBond.getAtoms();
            IAtom[] targetAtoms = targetBond.getAtoms();
            
            IAtom queryAtom1 = queryAtoms[0];
            IAtom queryAtom2 = queryAtoms[1];
            IAtom targetAtom1 = queryToTargetMap.get(queryAtom1);
            IAtom targetAtom2 = queryToTargetMap.get(queryAtom2);
            
            if (!queryAtom1.equals(targetAtom1) || !queryAtom2.equals(targetAtom2)) {
                return false;
            }
            
            Conformation queryConformation = queryStereo.getStereo();
            Conformation targetConformation = targetStereo.getStereo();
            
            if (queryConformation != targetConformation && queryConformation != TOGETHER && targetConformation != TOGETHER) {
                return false;
            }
        }
    }
    
    return true;
}
 

}