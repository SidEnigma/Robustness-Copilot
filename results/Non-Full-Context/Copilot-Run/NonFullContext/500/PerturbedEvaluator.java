/**
  *
  * Copyright (C) 2006-2010  Syed Asad Rahman <asad@ebi.ac.uk>
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
  * You should have received eAtom copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 package org.openscience.cdk.smsd.filters;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.LinkedHashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Objects;
 import java.util.TreeMap;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.DefaultChemObjectBuilder;
 import org.openscience.cdk.aromaticity.Aromaticity;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.graph.ConnectivityChecker;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomContainerSet;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IBond.Order;
 import org.openscience.cdk.interfaces.IRingSet;
 import org.openscience.cdk.isomorphism.matchers.IQueryAtom;
 import org.openscience.cdk.isomorphism.matchers.IQueryBond;
 import org.openscience.cdk.smsd.ring.HanserRingFinder;
 import org.openscience.cdk.smsd.tools.BondEnergies;
 import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
 import org.openscience.cdk.tools.manipulator.RingSetManipulator;
 
 /**
  * Class that ranks MCS final solution according to the chemical rules.
  *
  * @cdk.module smsd
  * @cdk.githash
  * @author Syed Asad Rahman &lt;asad@ebi.ac.uk&gt;
  * @deprecated SMSD has been deprecated from the CDK with a newer, more recent
  *             version of SMSD is available at <a href="http://github.com/asad/smsd">http://github.com/asad/smsd</a>.
  */
 @Deprecated
 public class ChemicalFilters {
 
     private List<Map<Integer, Integer>> allMCS        = null;
     private Map<Integer, Integer>       firstSolution = null;
     private List<Map<IAtom, IAtom>>     allAtomMCS    = null;
     private Map<IAtom, IAtom>           firstAtomMCS  = null;
     private List<Double>                stereoScore   = null;
     private List<Integer>               fragmentSize  = null;
     private List<Double>                bEnergies     = null;
     private IAtomContainer              rMol          = null;
     private IAtomContainer              pMol          = null;
 
     /**
      * This class has all the three chemical filters supported by the SMSD.
      * i.e ring matches, bond energy etc
      *
      * <OL>
      * <lI>a: Bond energy,
      * <lI>b: Fragment count,
      * <lI>c: Stereo matches
      * </OL>
      *
      * @param allMCS
      * @param allAtomMCS
      * @param firstSolution
      * @param firstAtomMCS
      * @param sourceMol
      * @param targetMol
      */
     public ChemicalFilters(List<Map<Integer, Integer>> allMCS, List<Map<IAtom, IAtom>> allAtomMCS,
             Map<Integer, Integer> firstSolution, Map<IAtom, IAtom> firstAtomMCS, IAtomContainer sourceMol,
             IAtomContainer targetMol) {
         this.allAtomMCS = allAtomMCS;
         this.allMCS = allMCS;
         this.firstAtomMCS = firstAtomMCS;
         this.firstSolution = firstSolution;
         this.pMol = targetMol;
         this.rMol = sourceMol;
 
         stereoScore = new ArrayList<Double>();
         fragmentSize = new ArrayList<Integer>();
         bEnergies = new ArrayList<Double>();
 
     }
 
     private void clear() {
 
         firstSolution.clear();
         allMCS.clear();
         allAtomMCS.clear();
         firstAtomMCS.clear();
         stereoScore.clear();
         fragmentSize.clear();
         bEnergies.clear();
 
     }
 
     private void clear(Map<Integer, Map<Integer, Integer>> sortedAllMCS,
             Map<Integer, Map<IAtom, IAtom>> sortedAllAtomMCS, Map<Integer, Double> stereoScoreMap,
             Map<Integer, Integer> fragmentScoreMap, Map<Integer, Double> energySelectionMap) {
 
         sortedAllMCS.clear();
         sortedAllAtomMCS.clear();
         stereoScoreMap.clear();
         fragmentScoreMap.clear();
         energySelectionMap.clear();
 
     }
 
     private void addSolution(int counter, int key, Map<Integer, Map<IAtom, IAtom>> allFragmentAtomMCS,
             Map<Integer, Map<Integer, Integer>> allFragmentMCS, Map<Integer, Double> stereoScoreMap,
             Map<Integer, Double> energyScoreMap, Map<Integer, Integer> fragmentScoreMap) {
 
         allAtomMCS.add(counter, allFragmentAtomMCS.get(key));
         allMCS.add(counter, allFragmentMCS.get(key));
         stereoScore.add(counter, stereoScoreMap.get(key));
         fragmentSize.add(counter, fragmentScoreMap.get(key));
         bEnergies.add(counter, energyScoreMap.get(key));
 
     }
 
     private void initializeMaps(Map<Integer, Map<Integer, Integer>> sortedAllMCS,
             Map<Integer, Map<IAtom, IAtom>> sortedAllAtomMCS, Map<Integer, Double> stereoScoreMap,
             Map<Integer, Integer> fragmentScoreMap, Map<Integer, Double> energySelectionMap) {
 
         Integer index = 0;
         for (Map<IAtom, IAtom> atomsMCS : allAtomMCS) {
             sortedAllAtomMCS.put(index, atomsMCS);
             fragmentScoreMap.put(index, 0);
             energySelectionMap.put(index, 0.0);
             stereoScoreMap.put(index, 0.0);
             index++;
         }
 
         index = 0;
         for (Map<Integer, Integer> mcs : allMCS) {
             sortedAllMCS.put(index, mcs);
             index++;
         }
 
         index = 0;
         for (Double score : bEnergies) {
             energySelectionMap.put(index, score);
             index++;
         }
 
         index = 0;
         for (Integer score : fragmentSize) {
             fragmentScoreMap.put(index, score);
             index++;
         }
 
         index = 0;
         for (Double score : stereoScore) {
             stereoScoreMap.put(index, score);
             index++;
         }
 
     }
 
     /**
      * Sort MCS solution by stereo and bond type matches.
      * @throws CDKException
      */
     public synchronized void sortResultsByStereoAndBondMatch() throws CDKException {
 
         //        System.out.println("\n\n\n\nSort By ResultsByStereoAndBondMatch");
 
         Map<Integer, Map<Integer, Integer>> allStereoMCS = new HashMap<Integer, Map<Integer, Integer>>();
         Map<Integer, Map<IAtom, IAtom>> allStereoAtomMCS = new HashMap<Integer, Map<IAtom, IAtom>>();
 
         Map<Integer, Integer> fragmentScoreMap = new TreeMap<Integer, Integer>();
         Map<Integer, Double> energyScoreMap = new TreeMap<Integer, Double>();
         Map<Integer, Double> stereoScoreMap = new HashMap<Integer, Double>();
 
         initializeMaps(allStereoMCS, allStereoAtomMCS, stereoScoreMap, fragmentScoreMap, energyScoreMap);
 
         boolean stereoMatchFlag = getStereoBondChargeMatch(stereoScoreMap, allStereoMCS, allStereoAtomMCS);
 
         boolean flag = false;
         if (stereoMatchFlag) {
 
             //Higher Score is mapped preferred over lower
             stereoScoreMap = sortMapByValueInDecendingOrder(stereoScoreMap);
             double higestStereoScore = stereoScoreMap.isEmpty() ? 0 : stereoScoreMap.values().iterator().next();
             double secondhigestStereoScore = higestStereoScore;
             for (Integer key : stereoScoreMap.keySet()) {
                 if (secondhigestStereoScore < higestStereoScore && stereoScoreMap.get(key) > secondhigestStereoScore) {
                     secondhigestStereoScore = stereoScoreMap.get(key);
                 } else if (secondhigestStereoScore == higestStereoScore
                         && stereoScoreMap.get(key) < secondhigestStereoScore) {
                     secondhigestStereoScore = stereoScoreMap.get(key);
                 }
             }
 
             if (!stereoScoreMap.isEmpty()) {
                 flag = true;
                 clear();
             }
 
             /* Put back the sorted solutions */
 
             int counter = 0;
             for (Integer i : stereoScoreMap.keySet()) {
                 //                System.out.println("Sorted Map key " + I + " Sorted Value: " + stereoScoreMap.get(I));
                 //                System.out.println("Stereo MCS " + allStereoMCS.get(I) + " Stereo Value: "
                 //                        + stereoScoreMap.get(I));
                 if (higestStereoScore == stereoScoreMap.get(i).doubleValue()) {
                     //|| secondhigestStereoScore == stereoScoreMap.get(I).doubleValue()) {
                     addSolution(counter, i, allStereoAtomMCS, allStereoMCS, stereoScoreMap, energyScoreMap,
                             fragmentScoreMap);
                     counter++;
 
                     //                    System.out.println("Sorted Map key " + I + " Sorted Value: " + stereoScoreMap.get(I));
                     //                    System.out.println("Stereo MCS " + allStereoMCS.get(I) + " Stereo Value: "
                     //                            + stereoScoreMap.get(I));
                 }
             }
             if (flag) {
                 firstSolution.putAll(allMCS.get(0));
                 firstAtomMCS.putAll(allAtomMCS.get(0));
                 clear(allStereoMCS, allStereoAtomMCS, stereoScoreMap, fragmentScoreMap, energyScoreMap);
             }
         }
 
     }
 
 
/** The solution is sorted by ascending order of the fragment count. */
 public synchronized void sortResultsByFragments(){}

 

}