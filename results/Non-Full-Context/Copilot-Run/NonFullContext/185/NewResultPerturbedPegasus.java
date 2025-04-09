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
 
 
/** Is the query preserved in the target? */

public boolean test(final int[] mapping) {
    // Check if the mapping array is null or its length is not equal to the number of atoms in the query
    if (mapping == null || mapping.length != query.getAtomCount()) {
        return false;
    }
    
    // Create a new array to store the adjusted group configuration
    groupConfigAdjust = new int[query.getAtomCount()];
    
    // Iterate over the query stereo indices
    for (int i = 0; i < queryStereoIndices.length; i++) {
        int queryIndex = queryStereoIndices[i];
        int targetIndex = mapping[queryIndex];
        
        // Check if the target index is out of bounds or the query and target atoms are not equal
        if (targetIndex < 0 || targetIndex >= target.getAtomCount() || !queryMap.get(queryElements[queryIndex]).equals(targetMap.get(targetElements[targetIndex]))) {
            return false;
        }
        
        // Check if the query and target stereo types are not equal
        if (!queryTypes[queryIndex].equals(targetTypes[targetIndex])) {
            return false;
        }
        
        // Check if the query stereo type is TetrahedralChirality
        if (queryTypes[queryIndex] instanceof ITetrahedralChirality) {
            ITetrahedralChirality queryChirality = (ITetrahedralChirality) queryTypes[queryIndex];
            ITetrahedralChirality targetChirality = (ITetrahedralChirality) targetTypes[targetIndex];
            
            // Check if the query and target chirality are not equal
            if (!queryChirality.getStereo().equals(targetChirality.getStereo())) {
                return false;
            }
        }
        
        // Check if the query stereo type is DoubleBondStereochemistry
        if (queryTypes[queryIndex] instanceof IDoubleBondStereochemistry) {
            IDoubleBondStereochemistry queryStereo = (IDoubleBondStereochemistry) queryTypes[queryIndex];
            IDoubleBondStereochemistry targetStereo = (IDoubleBondStereochemistry) targetTypes[targetIndex];
            
            // Check if the query and target stereo conformations are not equal
            if (!queryStereo.getConformation().equals(targetStereo.getConformation())) {
                return false;
            }
            
            // Adjust the group configuration based on the stereo conformation
            if (queryStereo.getConformation() == TOGETHER) {
                groupConfigAdjust[i] = 1;
            }
        }
    }
    
    // Check if the adjusted group configuration is not equal to the target group configuration
    if (!Arrays.equals(groupConfigAdjust, targetStereoIndices)) {
        return false;
    }
    
    // All checks passed, the query is preserved in the target
    return true;
}
 

}