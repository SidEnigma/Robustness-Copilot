/*  Copyright (C) 2004-2007  Rajarshi Guha <rajarshi@users.sourceforge.net>
  *
  *  Contact: cdk-devel@lists.sourceforge.net
  *
  *  This program is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU Lesser General Public License
  *  as published by the Free Software Foundation; either version 2.1
  *  of the License, or (at your option) any later version.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU Lesser General Public License for more details.
  *
  *  You should have received a copy of the GNU Lesser General Public License
  *  along with this program; if not, write to the Free Software
  *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 package org.openscience.cdk.qsar.descriptors.molecular;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.List;
 
 import org.openscience.cdk.config.Isotopes;
 import org.openscience.cdk.config.IsotopeFactory;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IElement;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;
 import org.openscience.cdk.isomorphism.matchers.QueryAtomContainer;
 import org.openscience.cdk.isomorphism.mcss.RMap;
 import org.openscience.cdk.qsar.AtomValenceTool;
 
 /**
  * Utility methods for chi index calculations.
  * 
  * These methods are common to all the types of chi index calculations and can
  * be used to evaluate path, path-cluster, cluster and chain chi indices.
  *
  * @author     Rajarshi Guha
  * @cdk.module qsarmolecular
  * @cdk.githash
  */
 class ChiIndexUtils {
 
     /**
      * Gets the fragments from a target <code>AtomContainer</code> matching a set of query fragments.
      * 
      * This method returns a list of lists. Each list contains the atoms of the target <code>AtomContainer</code>
      * that arise in the mapping of bonds in the target molecule to the bonds in the query fragment.
      * The query fragments should be constructed
      * using the <code>createAnyAtomAnyBondContainer</code> method of the <code>QueryAtomContainerCreator</code>
      * CDK class, since we are only interested in connectivity and not actual atom or bond type information.
      *
      * @param atomContainer The target <code>AtomContainer</code>
      * @param queries       An array of query fragments
      * @return A list of lists, each list being the atoms that match the query fragments
      */
     public static List<List<Integer>> getFragments(IAtomContainer atomContainer, QueryAtomContainer[] queries) {
         UniversalIsomorphismTester universalIsomorphismTester = new UniversalIsomorphismTester();
         List<List<Integer>> uniqueSubgraphs = new ArrayList<List<Integer>>();
         for (QueryAtomContainer query : queries) {
             List<List<RMap>> subgraphMaps = null;
             try {
                 // we get the list of bond mappings
                 subgraphMaps = universalIsomorphismTester.getSubgraphMaps(atomContainer, query);
             } catch (CDKException e) {
                 e.printStackTrace();
             }
             if (subgraphMaps == null) continue;
             if (subgraphMaps.size() == 0) continue;
 
             // get the atom paths in the unique set of bond maps
             uniqueSubgraphs.addAll(getUniqueBondSubgraphs(subgraphMaps, atomContainer));
         }
 
         // lets run a check on the length of each returned fragment and delete
         // any that don't match the length of out query fragments. Note that since
         // sometimes a fragment might be a ring, it will have number of atoms
         // equal to the number of bonds, where as a fragment with no rings
         // will have number of atoms equal to the number of bonds+1. So we need to check
         // fragment size against all unique query sizes - I get lazy and don't check
         // unique query sizes, but the size of each query
         List<List<Integer>> retValue = new ArrayList<List<Integer>>();
         for (List<Integer> fragment : uniqueSubgraphs) {
             for (QueryAtomContainer query : queries) {
                 if (fragment.size() == query.getAtomCount()) {
                     retValue.add(fragment);
                     break;
                 }
             }
         }
         return retValue;
     }
 
     /**
      * Evaluates the simple chi index for a set of fragments.
      *
      * @param atomContainer The target <code>AtomContainer</code>
      * @param fragLists      A list of fragments
      * @return The simple chi index
      */
     public static double evalSimpleIndex(IAtomContainer atomContainer, List<List<Integer>> fragLists) {
         double sum = 0;
         for (List<Integer> fragList : fragLists) {
             double prod = 1.0;
             for (Integer atomSerial : fragList) {
                 IAtom atom = atomContainer.getAtom(atomSerial);
                 int nconnected = atomContainer.getConnectedBondsCount(atom);
                 prod = prod * nconnected;
             }
             if (prod != 0) sum += 1.0 / Math.sqrt(prod);
         }
         return sum;
     }
 
 
/** Evaluates the valence corrected chi index for a set of fragments. */
 public static double evalValenceIndex(IAtomContainer atomContainer, List<List<Integer>> fragList) throws CDKException{}

 

}