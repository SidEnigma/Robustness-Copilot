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
 
 
/** Sort the MCS solution by stereo correspondence and link type. */
 public synchronized void sortResultsByStereoAndBondMatch() throws CDKException{
     
      Map<Integer, Map<Integer, Integer>> sortedAllMCS = new HashMap<Integer, Map<Integer, Integer>>();
      Map<Integer, Map<IAtom, IAtom>> sortedAllAtomMCS = new HashMap<Integer, Map<IAtom, IAtom>>();
      Map<Integer, Double> stereoScoreMap = new HashMap<Integer, Double>();
      Map<Integer, Integer> fragmentScoreMap = new HashMap<Integer, Integer>();
      Map<Integer, Double> energySelectionMap = new HashMap<Integer, Double>();
     
      initializeMaps(sortedAllMCS, sortedAllAtomMCS, stereoScoreMap, fragmentScoreMap, energySelectionMap);
     
      int counter = 0;
      for (Map<Integer, Integer> mcs : allMCS) {
            double stereoScore = 0.0;
            double energyScore = 0.0;
            int fragmentSize = 0;
            int bondMatch = 0;
            int stereoMatch = 0;
            int ringMatch = 0;
            int ringSize = 0;
            int ringSize2 = 0;
            int ringSize3 = 0;
            int ringSize4 = 0;
            int ringSize5 = 0;
            int ringSize6 = 0;
            int ringSize7 = 0;
            int ringSize8 = 0;
            int ringSize9 = 0;
            int ringSize10 = 0;
            int ringSize11 = 0;
            int ringSize12 = 0;
            int ringSize13 = 0;
            int ringSize14 = 0;
            int ringSize15 = 0;
            int ringSize16 = 0;
            int ringSize17 = 0;
            int ringSize18 = 0;
            int ringSize19 = 0;
            int ringSize20 = 0;
            int ringSize21 = 0;
            int ringSize22 = 0;
            int ringSize23 = 0;
            int ringSize24 = 0;
            int ringSize25 = 0;
            int ringSize26 = 0;
            int ringSize27 = 0;
            int ringSize28 = 0;         
 }

 

}