/* Copyright (C) 2006-2010  Syed Asad Rahman <asad@ebi.ac.uk>
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
  * You should have received sourceAtom copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 package org.openscience.cdk.smsd.algorithm.rgraph;
 
 import java.util.ArrayList;
 import java.util.BitSet;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Stack;
 import java.util.TreeMap;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.smsd.helper.FinalMappings;
 import org.openscience.cdk.tools.manipulator.BondManipulator;
 
 /**
  * This algorithm derives from the algorithm described in
  * [Tonnelier, C. and Jauffret, Ph. and Hanser, Th. and Jauffret, Ph. and Kaufmann, G.,
  * Machine Learning of generic reactions:
  * 3. An efficient algorithm for maximal common substructure determination,
  * Tetrahedron Comput. Methodol., 1990, 3:351-358] and modified in the thesis of
  * T. Hanser [Unknown BibTeXML type: HAN93].
  *
  * @cdk.module smsd
  * @cdk.githash
  * @author Syed Asad Rahman &lt;asad@ebi.ac.uk&gt;
  * @deprecated This class is part of SMSD and either duplicates functionality elsewhere in the CDK or provides public
  *             access to internal implementation details. SMSD has been deprecated from the CDK with a newer, more recent
  *             version of SMSD is available at <a href="http://github.com/asad/smsd">http://github.com/asad/smsd</a>.
  */
 @Deprecated
 public class CDKRMapHandler {
 
     public CDKRMapHandler() {
 
     }
 
     /**
      * Returns source molecule
      * @return the source
      */
     public IAtomContainer getSource() {
         return source;
     }
 
     /**
      * Set source molecule
      * @param aSource the source to set
      */
     public void setSource(IAtomContainer aSource) {
         source = aSource;
     }
 
     /**
      * Returns target molecule
      * @return the target
      */
     public IAtomContainer getTarget() {
         return target;
     }
 
     /**
      * Set target molecule
      * @param aTarget the target to set
      */
     public void setTarget(IAtomContainer aTarget) {
         target = aTarget;
     }
 
     private List<Map<Integer, Integer>> mappings;
     private IAtomContainer       source;
     private IAtomContainer       target;
     private boolean                     timeoutFlag = false;
 
     /**
      * This function calculates all the possible combinations of MCS
      * @param molecule1
      * @param molecule2
      * @param shouldMatchBonds
      * @throws CDKException
      */
     public void calculateOverlapsAndReduce(IAtomContainer molecule1, IAtomContainer molecule2, boolean shouldMatchBonds)
             throws CDKException {
 
         setSource(molecule1);
         setTarget(molecule2);
 
         setMappings(new ArrayList<Map<Integer, Integer>>());
 
         if ((getSource().getAtomCount() == 1) || (getTarget().getAtomCount() == 1)) {
             List<CDKRMap> overlaps = CDKMCS.checkSingleAtomCases(getSource(), getTarget());
             int nAtomsMatched = overlaps.size();
             nAtomsMatched = (nAtomsMatched > 0) ? 1 : 0;
             if (nAtomsMatched > 0) {
                 /* UnComment this to get one Unique Mapping */
                 //List reducedList = removeRedundantMappingsForSingleAtomCase(overlaps);
                 //int counter = 0;
                 identifySingleAtomsMatchedParts(overlaps, getSource(), getTarget());
 
             }
 
         } else {
             List<List<CDKRMap>> overlaps = CDKMCS.search(getSource(), getTarget(), new BitSet(), new BitSet(), true,
                     true, shouldMatchBonds);
 
             List<List<CDKRMap>> reducedList = removeSubGraph(overlaps);
             Stack<List<CDKRMap>> allMaxOverlaps = getAllMaximum(reducedList);
             while (!allMaxOverlaps.empty()) {
                 //                System.out.println("source: " + source.getAtomCount() + ", target: " + target.getAtomCount() + ", overl: " + allMaxOverlaps.peek().size());
                 List<List<CDKRMap>> maxOverlapsAtoms = makeAtomsMapOfBondsMap(allMaxOverlaps.peek(), getSource(),
                         getTarget());
                 //                System.out.println("size of maxOverlaps: " + maxOverlapsAtoms.size());
                 identifyMatchedParts(maxOverlapsAtoms, getSource(), getTarget());
                 //                identifyMatchedParts(allMaxOverlaps.peek(), source, target);
                 allMaxOverlaps.pop();
             }
         }
 
         FinalMappings.getInstance().set(getMappings());
 
     }
 
     /**
      * This function calculates only one solution (exact) because we are looking at the
      * molecules which are exactly same in terms of the bonds and atoms determined by the
      * Fingerprint
      * @param molecule1
      * @param molecule2
      * @param shouldMatchBonds
      * @throws CDKException
      */
     public void calculateOverlapsAndReduceExactMatch(IAtomContainer molecule1, IAtomContainer molecule2,
             boolean shouldMatchBonds) throws CDKException {
 
         setSource(molecule1);
         setTarget(molecule2);
 
         setMappings(new ArrayList<Map<Integer, Integer>>());
 
         //System.out.println("Searching: ");
         //List overlaps = UniversalIsomorphismTesterBondTypeInSensitive.getSubgraphAtomsMap(source, target);
 
         if ((getSource().getAtomCount() == 1) || (getTarget().getAtomCount() == 1)) {
 
             List<CDKRMap> overlaps = CDKMCS.checkSingleAtomCases(getSource(), getTarget());
             int nAtomsMatched = overlaps.size();
             nAtomsMatched = (nAtomsMatched > 0) ? 1 : 0;
             if (nAtomsMatched > 0) {
                 identifySingleAtomsMatchedParts(overlaps, getSource(), getTarget());
             }
 
         } else {
 
             List<List<CDKRMap>> overlaps = CDKMCS.search(getSource(), getTarget(), new BitSet(), new BitSet(), true,
                     true, shouldMatchBonds);
 
             List<List<CDKRMap>> reducedList = removeSubGraph(overlaps);
             Stack<List<CDKRMap>> allMaxOverlaps = getAllMaximum(reducedList);
 
             while (!allMaxOverlaps.empty()) {
                 List<List<CDKRMap>> maxOverlapsAtoms = makeAtomsMapOfBondsMap(allMaxOverlaps.peek(), getSource(),
                         getTarget());
                 identifyMatchedParts(maxOverlapsAtoms, getSource(), getTarget());
                 allMaxOverlaps.pop();
             }
         }
         FinalMappings.getInstance().set(getMappings());
     }
 
     /**
      * This function calculates only one solution (exact) because we are looking at the
      * molecules which are exactly same in terms of the bonds and atoms determined by the
      * Fingerprint
      * @param molecule1
      * @param molecule2
      * @param shouldMatchBonds
      * @throws CDKException
      */
     public void calculateSubGraphs(IAtomContainer molecule1, IAtomContainer molecule2, boolean shouldMatchBonds)
             throws CDKException {
 
         setSource(molecule1);
         setTarget(molecule2);
 
         setMappings(new ArrayList<Map<Integer, Integer>>());
 
         //System.out.println("Searching: ");
         //List overlaps = UniversalIsomorphismTesterBondTypeInSensitive.getSubgraphAtomsMap(source, target);
 
         if ((getSource().getAtomCount() == 1) || (getTarget().getAtomCount() == 1)) {
 
             List<CDKRMap> overlaps = CDKMCS.checkSingleAtomCases(getSource(), getTarget());
             int nAtomsMatched = overlaps.size();
             nAtomsMatched = (nAtomsMatched > 0) ? 1 : 0;
             if (nAtomsMatched > 0) {
                 identifySingleAtomsMatchedParts(overlaps, getSource(), getTarget());
             }
 
         } else {
 
             List<List<CDKRMap>> overlaps = CDKMCS.getSubgraphMaps(getSource(), getTarget(), shouldMatchBonds);
 
             List<List<CDKRMap>> reducedList = removeSubGraph(overlaps);
             Stack<List<CDKRMap>> allMaxOverlaps = getAllMaximum(reducedList);
 
             while (!allMaxOverlaps.empty()) {
                 List<List<CDKRMap>> maxOverlapsAtoms = makeAtomsMapOfBondsMap(allMaxOverlaps.peek(), getSource(),
                         getTarget());
                 identifyMatchedParts(maxOverlapsAtoms, getSource(), getTarget());
                 allMaxOverlaps.pop();
             }
         }
         FinalMappings.getInstance().set(getMappings());
     }
 
     /**
      * This function calculates only one solution (exact) because we are looking at the
      * molecules which are exactly same in terms of the bonds and atoms determined by the
      * Fingerprint
      * @param molecule1
      * @param molecule2
      * @param shouldMatchBonds
      * @throws CDKException
      */
     public void calculateIsomorphs(IAtomContainer molecule1, IAtomContainer molecule2, boolean shouldMatchBonds)
             throws CDKException {
 
         setSource(molecule1);
         setTarget(molecule2);
 
         setMappings(new ArrayList<Map<Integer, Integer>>());
 
         //System.out.println("Searching: ");
         //List overlaps = UniversalIsomorphismTesterBondTypeInSensitive.getSubgraphAtomsMap(source, target);
 
         if ((getSource().getAtomCount() == 1) || (getTarget().getAtomCount() == 1)) {
 
             List<CDKRMap> overlaps = CDKMCS.checkSingleAtomCases(getSource(), getTarget());
             int nAtomsMatched = overlaps.size();
             nAtomsMatched = (nAtomsMatched > 0) ? 1 : 0;
             if (nAtomsMatched > 0) {
                 identifySingleAtomsMatchedParts(overlaps, getSource(), getTarget());
             }
 
         } else {
 
             List<List<CDKRMap>> overlaps = CDKMCS.getIsomorphMaps(getSource(), getTarget(), shouldMatchBonds);
 
             List<List<CDKRMap>> reducedList = removeSubGraph(overlaps);
             Stack<List<CDKRMap>> allMaxOverlaps = getAllMaximum(reducedList);
 
             while (!allMaxOverlaps.empty()) {
                 List<List<CDKRMap>> maxOverlapsAtoms = makeAtomsMapOfBondsMap(allMaxOverlaps.peek(), getSource(),
                         getTarget());
                 identifyMatchedParts(maxOverlapsAtoms, getSource(), getTarget());
                 allMaxOverlaps.pop();
             }
         }
         FinalMappings.getInstance().set(getMappings());
     }
 
     /**
      *
      * @param overlaps
      * @return
      */
     protected List<List<CDKRMap>> removeSubGraph(List<List<CDKRMap>> overlaps) {
 
         List<List<CDKRMap>> reducedList = new ArrayList<List<CDKRMap>>(overlaps);
 
         for (int i = 0; i < overlaps.size(); i++) {
             List<CDKRMap> graphI = overlaps.get(i);
 
             for (int j = i + 1; j < overlaps.size(); j++) {
                 List<CDKRMap> graphJ = overlaps.get(j);
 
                 // Gi included in Gj or Gj included in Gi then
                 // reduce the irrelevant solution
                 if (graphI.size() != graphJ.size()) {
                     if (isSubgraph(graphJ, graphI)) {
                         reducedList.remove(graphI);
                     } else if (isSubgraph(graphI, graphJ)) {
                         reducedList.remove(graphJ);
                     }
                 }
 
             }
         }
         return reducedList;
     }
 
     /**
      *
      * @param overlaps
      * @return
      */
     protected List<CDKRMap> removeRedundantMappingsForSingleAtomCase(List<CDKRMap> overlaps) {
         List<CDKRMap> reducedList = new ArrayList<CDKRMap>();
         reducedList.add(overlaps.get(0));
         //reducedList.add(overlaps.get(1));
         return reducedList;
     }
 
     /**
      *  This makes sourceAtom map of matching atoms out of sourceAtom map of matching bonds as produced by the get(Subgraph|Ismorphism)Map methods.
      *
      * @param  rMapList   The list produced by the getMap method.
      * @param  graph1  first molecule. Must not be an IQueryAtomContainer.
      * @param  graph2  second molecule. May be an IQueryAtomContainer.
      * @return     The mapping found projected on graph1. This is sourceAtom List of CDKRMap objects containing Ids of matching atoms.
      */
     private static List<List<CDKRMap>> makeAtomsMapOfBondsMap(List<CDKRMap> rMapList, IAtomContainer graph1,
             IAtomContainer graph2) {
         if (rMapList == null) {
             return (null);
         }
         List<List<CDKRMap>> result = null;
         if (rMapList.size() == 1) {
             result = makeAtomsMapOfBondsMapSingleBond(rMapList, graph1, graph2);
         } else {
             List<CDKRMap> resultLocal = new ArrayList<CDKRMap>();
             for (int i = 0; i < rMapList.size(); i++) {
                 IBond qBond = graph1.getBond(rMapList.get(i).getId1());
                 IBond tBond = graph2.getBond(rMapList.get(i).getId2());
                 IAtom[] qAtoms = BondManipulator.getAtomArray(qBond);
                 IAtom[] tAtoms = BondManipulator.getAtomArray(tBond);
                 for (int j = 0; j < 2; j++) {
                     List<IBond> bondsConnectedToAtom1j = graph1.getConnectedBondsList(qAtoms[j]);
                     for (int k = 0; k < bondsConnectedToAtom1j.size(); k++) {
                         if (!bondsConnectedToAtom1j.get(k).equals(qBond)) {
                             IBond testBond = bondsConnectedToAtom1j.get(k);
                             for (int m = 0; m < rMapList.size(); m++) {
                                 IBond testBond2;
                                 if ((rMapList.get(m)).getId1() == graph1.indexOf(testBond)) {
                                     testBond2 = graph2.getBond((rMapList.get(m)).getId2());
                                     for (int n = 0; n < 2; n++) {
                                         List<IBond> bondsToTest = graph2.getConnectedBondsList(tAtoms[n]);
                                         if (bondsToTest.contains(testBond2)) {
                                             CDKRMap map;
                                             if (j == n) {
                                                 map = new CDKRMap(graph1.indexOf(qAtoms[0]),
                                                         graph2.indexOf(tAtoms[0]));
                                             } else {
                                                 map = new CDKRMap(graph1.indexOf(qAtoms[1]),
                                                         graph2.indexOf(tAtoms[0]));
                                             }
                                             if (!resultLocal.contains(map)) {
                                                 resultLocal.add(map);
                                             }
                                             CDKRMap map2;
                                             if (j == n) {
                                                 map2 = new CDKRMap(graph1.indexOf(qAtoms[1]),
                                                         graph2.indexOf(tAtoms[1]));
                                             } else {
                                                 map2 = new CDKRMap(graph1.indexOf(qAtoms[0]),
                                                         graph2.indexOf(tAtoms[1]));
                                             }
                                             if (!resultLocal.contains(map2)) {
                                                 resultLocal.add(map2);
                                             }
                                         }
                                     }
                                 }
                             }
                         }
                     }
                 }
             }
             result = new ArrayList<List<CDKRMap>>();
             result.add(resultLocal);
         }
         return result;
     }
 
 
/** This makes the map of the atoms of corresponding atoms from the atom map of the corresponding bonds as produced by the get(Subgraph| Ismorphism)Cartographic methods. */
 private static List<List<CDKRMap>> makeAtomsMapOfBondsMapSingleBond(List<CDKRMap> list, IAtomContainer sourceGraph, IAtomContainer targetGraph){
        List<List<CDKRMap>> result = new ArrayList<List<CDKRMap>>();
        List<CDKRMap> resultLocal = new ArrayList<CDKRMap>();
        for (int i = 0; i < list.size(); i++) {
            IBond qBond = sourceGraph.getBond(list.get(i).getId1());
            IBond tBond = targetGraph.getBond(list.get(i).getId2());
            IAtom[] qAtoms = BondManipulator.getAtomArray(qBond);
            IAtom[] tAtoms = BondManipulator.getAtomArray(tBond);
            for (int j = 0; j < 2; j++) {
                List<IBond> bondsConnectedToAtom1j = sourceGraph.getConnectedBondsList(qAtoms[j]);
                for (int k = 0; k < bondsConnectedToAtom1j.size(); k++) {
                    if (!bondsConnectedToAtom1j.get(k).equals(qBond)) {
                        IBond testBond = bondsConnectedToAtom1j.get(k);
                        for (int m = 0; m < list.size(); m++) {
                            IBond testBond2;
                            if ((list.get(m)).getId1() == sourceGraph.indexOf(testBond)) {
                                testBond2 = targetGraph.getBond((list.get(m)).getId2());
                                for (int n = 0; n < 2; n++) {
                                    List<IBond> bondsToTest = targetGraph.getConnectedBondsList(tAtoms[n]);
                                    if (bondsToTest.contains(testBond2)) {
                                        CDKRMap map;
                                        if (j == n) {
                                            map = new CDKRMap(sourceGraph.indexOf(qAtoms[0]),
                                                    targetGraph.indexOf(tAtoms[0]));
                                        } else {
                                            map = new CDKRMap(sourceGraph.indexOf(      
 }

 

}