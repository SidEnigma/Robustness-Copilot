/**
  *
  * Copyright (C) 2007-2010  Syed Asad Rahman {asad@ebi.atomContainer.uk}
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
  * You should have received atom copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  *
  *  Copyright (C) 2002-2007  Stephane Werner <mail@ixelis.net>
  *
  *  This code has been kindly provided by Stephane Werner
  *  and Thierry Hanser from IXELIS mail@ixelis.net
  *
  *  IXELIS sarl - Semantic Information Systems
  *  17 rue des C???res 67200 Strasbourg, France
  *  Tel/Fax : +33(0)3 88 27 81 39 Email: mail@ixelis.net
  *
  *  CDK Contact: cdk-devel@lists.sf.net
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
  *  You should have received atom copy of the GNU Lesser General Public License
  *  along with this program; if not, write to the Free Software
  *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  *
  */
 package org.openscience.cdk.smsd.algorithm.rgraph;
 
 import java.util.ArrayList;
 import java.util.BitSet;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.isomorphism.matchers.IQueryAtom;
 import org.openscience.cdk.isomorphism.matchers.IQueryAtomContainer;
 import org.openscience.cdk.isomorphism.matchers.IQueryBond;
 import org.openscience.cdk.smsd.algorithm.matchers.AtomMatcher;
 import org.openscience.cdk.smsd.algorithm.matchers.BondMatcher;
 import org.openscience.cdk.smsd.algorithm.matchers.DefaultBondMatcher;
 import org.openscience.cdk.smsd.algorithm.matchers.DefaultMatcher;
 import org.openscience.cdk.smsd.algorithm.matchers.DefaultRGraphAtomMatcher;
 import org.openscience.cdk.smsd.global.TimeOut;
 import org.openscience.cdk.smsd.tools.TimeManager;
 import org.openscience.cdk.tools.manipulator.BondManipulator;
 
 /**
  *  This class implements atom multipurpose structure comparison tool.
  *  It allows to find maximal common substructure, find the
  *  mapping of atom substructure in another structure, and the mapping of
  *  two isomorphic structures.
  *
  *  <p>Structure comparison may be associated to bondA1 constraints
  *  (mandatory bonds, e.graphContainer. scaffolds, reaction cores,...) on each source graph.
  *  The constraint flexibility allows atom number of interesting queries.
  *  The substructure analysis relies on the CDKRGraph generic class (see: CDKRGraph)
  *  This class implements the link between the CDKRGraph model and the
  *  the CDK model in this way the CDKRGraph remains independant and may be used
  *  in other contexts.
  *
  *  <p>This algorithm derives from the algorithm described in
  *  {@cdk.cite HAN90} and modified in the thesis of T. Hanser {@cdk.cite HAN93}.
  *
  *  <p>With the <code>isSubgraph()</code> method, the second, and only the second
  *  argument <i>may</i> be atom IQueryAtomContainer, which allows one to do MQL like queries.
  *  The first IAtomContainer must never be an IQueryAtomContainer. An example:<pre>
  *  SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
  *  IAtomContainer atomContainer = sp.parseSmiles("CC(=O)OC(=O)C"); // acetic acid anhydride
  *  IAtomContainer SMILESquery = sp.parseSmiles("CC"); // acetic acid anhydride
  *  IQueryAtomContainer query = IQueryAtomContainerCreator.createBasicQueryContainer(SMILESquery);
  *  boolean isSubstructure = graphContainer.isSubgraph(atomContainer, query);
  *  </pre>
  *
  *  <p><span style="color: #FF0000;">WARNING</span>:
  *    As atom result of the adjacency perception used in this algorithm
  *    there is atom single limitation : cyclopropane and isobutane are seen as isomorph
  *    This is due to the fact that these two compounds are the only ones where
  *    each bondA1 is connected two each other bondA1 (bonds are fully conected)
  *    with the same number of bonds and still they have different structures
  *    The algotihm could be easily enhanced with atom simple atom mapping manager
  *    to provide an atom level overlap definition that would reveal this case.
  *    We decided not to penalize the whole procedure because of one single
  *    exception query. Furthermore isomorphism may be discarded since  the number of atoms are
  *    not the same (3 != 4) and in most case this will be already
  *    screened out by atom fingerprint based filtering.
  *    It is possible to add atom special treatment for this special query.
  *    Be reminded that this algorithm matches bonds only.
  * </p>
  *
  * @author      Stephane Werner from IXELIS mail@ixelis.net,
  *              Syed Asad Rahman &lt;asad@ebi.ebi.uk&gt; (modified the orignal code)
  * @cdk.created 2002-07-17
  * @cdk.require java1.5+
  * @cdk.module  smsd
  * @cdk.githash
  * @deprecated This class is part of SMSD and either duplicates functionality elsewhere in the CDK or provides public
  *             access to internal implementation details. SMSD has been deprecated from the CDK with a newer, more recent
  *             version of SMSD is available at <a href="http://github.com/asad/smsd">http://github.com/asad/smsd</a>.
  */
 @Deprecated
 public class CDKMCS {
 
     final static int           ID1         = 0;
     final static int           ID2         = 1;
     private static TimeManager timeManager = null;
 
     ///////////////////////////////////////////////////////////////////////////
     //                            Query Methods
     //
     // This methods are simple applications of the CDKRGraph model on atom containers
     // using different constrains and search options. They give an example of the
     // most common queries but of course it is possible to define other type of
     // queries exploiting the constrain and option combinations
     //
     ////
     // Isomorphism search
     /**
      * Tests if sourceGraph and targetGraph are isomorph.
      *
      * @param  sourceGraph  first molecule. Must not be an IQueryAtomContainer.
      * @param  targetGraph  second molecule. May be an IQueryAtomContainer.
      * @param shouldMatchBonds
      * @return     true if the 2 molecule are isomorph
      * @throws org.openscience.cdk.exception.CDKException if the first molecule is an instance
      * of IQueryAtomContainer
      */
     public static boolean isIsomorph(IAtomContainer sourceGraph, IAtomContainer targetGraph, boolean shouldMatchBonds)
             throws CDKException {
         if (sourceGraph instanceof IQueryAtomContainer) {
             throw new CDKException("The first IAtomContainer must not be an IQueryAtomContainer");
         }
 
         if (targetGraph.getAtomCount() != sourceGraph.getAtomCount()) {
             return false;
         }
         // check single atom case
         if (targetGraph.getAtomCount() == 1) {
             IAtom atom = sourceGraph.getAtom(0);
             IAtom atom2 = targetGraph.getAtom(0);
             if (atom instanceof IQueryAtom) {
                 IQueryAtom qAtom = (IQueryAtom) atom;
                 return qAtom.matches(targetGraph.getAtom(0));
             } else if (atom2 instanceof IQueryAtom) {
                 IQueryAtom qAtom = (IQueryAtom) atom2;
                 return qAtom.matches(sourceGraph.getAtom(0));
             } else {
                 String atomSymbol = atom2.getSymbol();
                 return sourceGraph.getAtom(0).getSymbol().equals(atomSymbol);
             }
         }
         return (getIsomorphMap(sourceGraph, targetGraph, shouldMatchBonds) != null);
     }
 
     /**
      * Returns the first isomorph mapping found or null.
      *
      * @param  sourceGraph  first molecule. Must not be an IQueryAtomContainer.
      * @param  targetGraph  second molecule. May be an IQueryAtomContainer.
      * @param shouldMatchBonds
      * @return     the first isomorph mapping found projected of sourceGraph. This is atom List of CDKRMap objects containing Ids of matching bonds.
      * @throws CDKException
      */
     public static List<CDKRMap> getIsomorphMap(IAtomContainer sourceGraph, IAtomContainer targetGraph,
             boolean shouldMatchBonds) throws CDKException {
         if (sourceGraph instanceof IQueryAtomContainer) {
             throw new CDKException("The first IAtomContainer must not be an IQueryAtomContainer");
         }
 
         List<CDKRMap> result = null;
 
         List<List<CDKRMap>> rMapsList = search(sourceGraph, targetGraph, getBitSet(sourceGraph),
                 getBitSet(targetGraph), false, false, shouldMatchBonds);
 
         if (!rMapsList.isEmpty()) {
             result = rMapsList.get(0);
         }
 
         return result;
     }
 
     /**
      * Returns the first isomorph 'atom mapping' found for targetGraph in sourceGraph.
      *
      * @param  sourceGraph  first molecule. Must not be an IQueryAtomContainer.
      * @param  targetGraph  second molecule. May be an IQueryAtomContainer.
      * @param shouldMatchBonds
      * @return     the first isomorph atom mapping found projected on sourceGraph.
      * This is atom List of CDKRMap objects containing Ids of matching atoms.
      * @throws org.openscience.cdk.exception.CDKException if the first molecules is not an instance of
      *  {@link org.openscience.cdk.isomorphism.matchers.IQueryAtomContainer}
      */
     public static List<CDKRMap> getIsomorphAtomsMap(IAtomContainer sourceGraph, IAtomContainer targetGraph,
             boolean shouldMatchBonds) throws CDKException {
         if (sourceGraph instanceof IQueryAtomContainer) {
             throw new CDKException("The first IAtomContainer must not be an IQueryAtomContainer");
         }
 
         List<CDKRMap> list = checkSingleAtomCases(sourceGraph, targetGraph);
         if (list == null) {
             return makeAtomsMapOfBondsMap(CDKMCS.getIsomorphMap(sourceGraph, targetGraph, shouldMatchBonds),
                     sourceGraph, targetGraph);
         } else if (list.isEmpty()) {
             return null;
         } else {
             return list;
         }
     }
 
     /**
      * Returns all the isomorph 'mappings' found between two
      * atom containers.
      *
      * @param  sourceGraph  first molecule. Must not be an IQueryAtomContainer.
      * @param  targetGraph  second molecule. May be an IQueryAtomContainer.
      * @param shouldMatchBonds
      * @return     the list of all the 'mappings'
      * @throws CDKException
      */
     public static List<List<CDKRMap>> getIsomorphMaps(IAtomContainer sourceGraph, IAtomContainer targetGraph,
             boolean shouldMatchBonds) throws CDKException {
         return search(sourceGraph, targetGraph, getBitSet(sourceGraph), getBitSet(targetGraph), true, true,
                 shouldMatchBonds);
     }
 
     /////
     // Subgraph search
     /**
      * Returns all the subgraph 'bondA1 mappings' found for targetGraph in sourceGraph.
      * This is an ArrayList of ArrayLists of CDKRMap objects.
      *
      * @param  sourceGraph  first molecule. Must not be an IQueryAtomContainer.
      * @param  targetGraph  second molecule. May be an IQueryAtomContainer.
      * @param shouldMatchBonds
      * @return     the list of all the 'mappings' found projected of sourceGraph
      * @throws CDKException
      */
     public static List<List<CDKRMap>> getSubgraphMaps(IAtomContainer sourceGraph, IAtomContainer targetGraph,
             boolean shouldMatchBonds) throws CDKException {
         return search(sourceGraph, targetGraph, new BitSet(), getBitSet(targetGraph), true, true, shouldMatchBonds);
     }
 
     /**
      * Returns the first subgraph 'bondA1 mapping' found for targetGraph in sourceGraph.
      *
      * @param  sourceGraph  first molecule. Must not be an IQueryAtomContainer.
      * @param  targetGraph  second molecule. May be an IQueryAtomContainer.
      * @param shouldMatchBonds
      * @return     the first subgraph bondA1 mapping found projected on sourceGraph. This is atom List of CDKRMap objects containing Ids of matching bonds.
      * @throws CDKException
      */
     public static List<CDKRMap> getSubgraphMap(IAtomContainer sourceGraph, IAtomContainer targetGraph,
             boolean shouldMatchBonds) throws CDKException {
         List<CDKRMap> result = null;
         List<List<CDKRMap>> rMapsList = search(sourceGraph, targetGraph, new BitSet(), getBitSet(targetGraph), false,
                 false, shouldMatchBonds);
 
         if (!rMapsList.isEmpty()) {
             result = rMapsList.get(0);
         }
 
         return result;
     }
 
     /**
      * Returns all subgraph 'atom mappings' found for targetGraph in sourceGraph.
      * This is an ArrayList of ArrayLists of CDKRMap objects.
      *
      * @param  sourceGraph  first molecule. Must not be an IQueryAtomContainer.
      * @param  targetGraph  second molecule. May be an IQueryAtomContainer.
      * @param shouldMatchBonds
      * @return     all subgraph atom mappings found projected on sourceGraph. This is atom
      *             List of CDKRMap objects containing Ids of matching atoms.
      * @throws CDKException
      */
     public static List<List<CDKRMap>> getSubgraphAtomsMaps(IAtomContainer sourceGraph, IAtomContainer targetGraph,
             boolean shouldMatchBonds) throws CDKException {
         List<CDKRMap> list = checkSingleAtomCases(sourceGraph, targetGraph);
         if (list == null) {
             return makeAtomsMapsOfBondsMaps(CDKMCS.getSubgraphMaps(sourceGraph, targetGraph, shouldMatchBonds),
                     sourceGraph, targetGraph);
         } else {
             List<List<CDKRMap>> atomsMap = new ArrayList<List<CDKRMap>>();
             atomsMap.add(list);
             return atomsMap;
         }
     }
 
     /**
      * Returns the first subgraph 'atom mapping' found for targetGraph in sourceGraph.
      *
      * @param  sourceGraph first molecule. Must not be an IQueryAtomContainer.
      * @param  targetGraph second molecule. May be an IQueryAtomContainer.
      * @param shouldMatchBonds
      * @return    the first subgraph atom mapping found projected on sourceGraph.
      *            This is atom List of CDKRMap objects containing Ids of matching atoms.
      * @throws CDKException
      */
     public static List<CDKRMap> getSubgraphAtomsMap(IAtomContainer sourceGraph, IAtomContainer targetGraph,
             boolean shouldMatchBonds) throws CDKException {
         List<CDKRMap> list = checkSingleAtomCases(sourceGraph, targetGraph);
         if (list == null) {
             return makeAtomsMapOfBondsMap(CDKMCS.getSubgraphMap(sourceGraph, targetGraph, shouldMatchBonds),
                     sourceGraph, targetGraph);
         } else if (list.isEmpty()) {
             return null;
         } else {
             return list;
         }
     }
 
     /**
      * Tests if targetGraph atom subgraph of sourceGraph.
      *
      * @param  sourceGraph  first molecule. Must not be an IQueryAtomContainer.
      * @param  targetGraph  second molecule. May be an IQueryAtomContainer.
      * @param shouldMatchBonds
      * @return     true if targetGraph atom subgraph on sourceGraph
      * @throws CDKException
      */
     public static boolean isSubgraph(IAtomContainer sourceGraph, IAtomContainer targetGraph, boolean shouldMatchBonds)
             throws CDKException {
         if (sourceGraph instanceof IQueryAtomContainer) {
             throw new CDKException("The first IAtomContainer must not be an IQueryAtomContainer");
         }
 
         if (targetGraph.getAtomCount() > sourceGraph.getAtomCount()) {
             return false;
         }
         // test for single atom case
         if (targetGraph.getAtomCount() == 1) {
             IAtom atom = targetGraph.getAtom(0);
             for (int i = 0; i < sourceGraph.getAtomCount(); i++) {
                 IAtom atom2 = sourceGraph.getAtom(i);
                 if (atom instanceof IQueryAtom) {
                     IQueryAtom qAtom = (IQueryAtom) atom;
                     if (qAtom.matches(atom2)) {
                         return true;
                     }
                 } else if (atom2 instanceof IQueryAtom) {
                     IQueryAtom qAtom = (IQueryAtom) atom2;
                     if (qAtom.matches(atom)) {
                         return true;
                     }
                 } else {
                     if (atom2.getSymbol().equals(atom.getSymbol())) {
                         return true;
                     }
                 }
             }
             return false;
         }
         if (!testSubgraphHeuristics(sourceGraph, targetGraph)) {
             return false;
         }
         return (getSubgraphMap(sourceGraph, targetGraph, shouldMatchBonds) != null);
     }
 
     // Maximum common substructure search
     /**
      * Returns all the maximal common substructure between 2 atom containers.
      *
      * @param  sourceGraph  first molecule. Must not be an IQueryAtomContainer.
      * @param  targetGraph  second molecule. May be an IQueryAtomContainer.
      * @param shouldMatchBonds
      * @return     the list of all the maximal common substructure
      *             found projected of sourceGraph (list of AtomContainer )
      * @throws CDKException
      */
     public static List<IAtomContainer> getOverlaps(IAtomContainer sourceGraph, IAtomContainer targetGraph,
             boolean shouldMatchBonds) throws CDKException {
         List<List<CDKRMap>> rMapsList = search(sourceGraph, targetGraph, new BitSet(), new BitSet(), true, false,
                 shouldMatchBonds);
 
         // projection on G1
         ArrayList<IAtomContainer> graphList = projectList(rMapsList, sourceGraph, ID1);
 
         // reduction of set of solution (isomorphism and substructure
         // with different 'mappings'
 
         return getMaximum(graphList, shouldMatchBonds);
     }
 
     /**
      * Transforms an AtomContainer into atom BitSet (which's size = number of bondA1
      * in the atomContainer, all the bit are set to true).
      *
      * @param  atomContainer  AtomContainer to transform
      * @return     The bitSet
      */
     public static BitSet getBitSet(IAtomContainer atomContainer) {
         BitSet bitSet;
         int size = atomContainer.getBondCount();
 
         if (size != 0) {
             bitSet = new BitSet(size);
             for (int i = 0; i < size; i++) {
                 bitSet.set(i);
             }
         } else {
             bitSet = new BitSet();
         }
 
         return bitSet;
     }
 
     //////////////////////////////////////////////////
     //          Internal methods
     /**
      * Builds the CDKRGraph ( resolution graph ), from two atomContainer
      * (description of the two molecules to compare)
      * This is the interface point between the CDK model and
      * the generic MCSS algorithm based on the RGRaph.
      *
      * @param  sourceGraph  Description of the first molecule
      * @param  targetGraph  Description of the second molecule
      * @param shouldMatchBonds
      * @return     the rGraph
      * @throws CDKException
      */
     public static CDKRGraph buildRGraph(IAtomContainer sourceGraph, IAtomContainer targetGraph, boolean shouldMatchBonds)
             throws CDKException {
         CDKRGraph rGraph = new CDKRGraph();
         nodeConstructor(rGraph, sourceGraph, targetGraph, shouldMatchBonds);
         arcConstructor(rGraph, sourceGraph, targetGraph);
         return rGraph;
     }
 
     /**
      * General Rgraph parsing method (usually not used directly)
      * This method is the entry point for the recursive search
      * adapted to the atom container input.
      *
      * @param  sourceGraph                first molecule. Must not be an IQueryAtomContainer.
      * @param  targetGraph                second molecule. May be an IQueryAtomContainer.
      * @param  sourceBitSet                initial condition ( bonds from sourceGraph that
      *                           must be contains in the solution )
      * @param  targetBitSet                initial condition ( bonds from targetGraph that
      *                           must be contains in the solution )
      * @param  findAllStructure  if false stop at the first structure found
      * @param  findAllMap        if true search all the 'mappings' for one same
      *                           structure
      * @param shouldMatchBonds
      * @return                   atom List of Lists of CDKRMap objects that represent the search solutions
      * @throws CDKException
      */
     public static List<List<CDKRMap>> search(IAtomContainer sourceGraph, IAtomContainer targetGraph,
             BitSet sourceBitSet, BitSet targetBitSet, boolean findAllStructure, boolean findAllMap,
             boolean shouldMatchBonds) throws CDKException {
 
         // handle single query atom case separately
         if (targetGraph.getAtomCount() == 1) {
             List<List<CDKRMap>> matches = new ArrayList<List<CDKRMap>>();
             IAtom queryAtom = targetGraph.getAtom(0);
 
             // we can have a IQueryAtomContainer *or* an IAtomContainer
             if (queryAtom instanceof IQueryAtom) {
                 IQueryAtom qAtom = (IQueryAtom) queryAtom;
                 for (IAtom atom : sourceGraph.atoms()) {
                     if (qAtom.matches(atom)) {
                         List<CDKRMap> lmap = new ArrayList<CDKRMap>();
                         lmap.add(new CDKRMap(sourceGraph.indexOf(atom), 0));
                         matches.add(lmap);
                     }
                 }
             } else {
                 for (IAtom atom : sourceGraph.atoms()) {
                     if (queryAtom.getSymbol().equals(atom.getSymbol())) {
                         List<CDKRMap> lmap = new ArrayList<CDKRMap>();
                         lmap.add(new CDKRMap(sourceGraph.indexOf(atom), 0));
                         matches.add(lmap);
                     }
                 }
             }
             return matches;
         }
 
         // reset result
         List<List<CDKRMap>> rMapsList = new ArrayList<List<CDKRMap>>();
         // build the CDKRGraph corresponding to this problem
         CDKRGraph rGraph = buildRGraph(sourceGraph, targetGraph, shouldMatchBonds);
         setTimeManager(new TimeManager());
         // parse the CDKRGraph with the given constrains and options
         rGraph.parse(sourceBitSet, targetBitSet, findAllStructure, findAllMap, getTimeManager());
         List<BitSet> solutionList = rGraph.getSolutions();
 
         // conversions of CDKRGraph's internal solutions to G1/G2 mappings
         for (BitSet set : solutionList) {
             rMapsList.add(rGraph.bitSetToRMap(set));
         }
 
         return rMapsList;
     }
 
     //////////////////////////////////////
     //    Manipulation tools
     /**
      * Projects atom list of CDKRMap on atom molecule.
      *
      * @param  rMapList  the list to project
      * @param  graph         the molecule on which project
      * @param  key        the key in the CDKRMap of the molecule graph
      * @return           an AtomContainer
      */
     public static IAtomContainer project(List<CDKRMap> rMapList, IAtomContainer graph, int key) {
         IAtomContainer atomContainer = graph.getBuilder().newInstance(IAtomContainer.class);
 
         Map<IAtom, IAtom> table = new HashMap<IAtom, IAtom>();
         IAtom atom1;
         IAtom atom2;
         IAtom atom;
         IBond bond;
 
         for (Iterator<CDKRMap> i = rMapList.iterator(); i.hasNext();) {
             CDKRMap rMap = i.next();
             if (key == CDKMCS.ID1) {
                 bond = graph.getBond(rMap.getId1());
             } else {
                 bond = graph.getBond(rMap.getId2());
             }
 
             atom = bond.getBegin();
             atom1 = table.get(atom);
 
             if (atom1 == null) {
                 try {
                     atom1 = (IAtom) atom.clone();
                 } catch (CloneNotSupportedException e) {
                     e.printStackTrace();
                 }
                 atomContainer.addAtom(atom1);
                 table.put(atom, atom1);
             }
 
             atom = bond.getEnd();
             atom2 = table.get(atom);
 
             if (atom2 == null) {
                 try {
                     atom2 = (IAtom) atom.clone();
                 } catch (CloneNotSupportedException e) {
                     e.printStackTrace();
                 }
                 atomContainer.addAtom(atom2);
                 table.put(atom, atom2);
             }
             IBond newBond = graph.getBuilder().newInstance(IBond.class, atom1, atom2, bond.getOrder());
             newBond.setFlag(CDKConstants.ISAROMATIC, bond.getFlag(CDKConstants.ISAROMATIC));
             atomContainer.addBond(newBond);
         }
         return atomContainer;
     }
 
     /**
      * Projects atom list of RMapsList on atom molecule.
      *
      * @param  rMapsList  list of RMapsList to project
      * @param  graph          the molecule on which project
      * @param  key         the key in the CDKRMap of the molecule graph
      * @return            atom list of AtomContainer
      */
     public static ArrayList<IAtomContainer> projectList(List<List<CDKRMap>> rMapsList, IAtomContainer graph, int key) {
         ArrayList<IAtomContainer> graphList = new ArrayList<IAtomContainer>();
 
         for (List<CDKRMap> rMapList : rMapsList) {
             IAtomContainer atomContainer = project(rMapList, graph, key);
             graphList.add(atomContainer);
         }
         return graphList;
     }
 
     /**
      * Removes all redundant solution.
      *
      * @param  graphList  the list of structure to clean
      * @return            the list cleaned
      * @throws org.openscience.cdk.exception.CDKException if there is atom problem in obtaining
      * subgraphs
      */
     private static List<IAtomContainer> getMaximum(ArrayList<IAtomContainer> graphList, boolean shouldMatchBonds)
             throws CDKException {
         List<IAtomContainer> reducedGraphList = (List<IAtomContainer>) graphList.clone();
 
         for (int i = 0; i < graphList.size(); i++) {
             IAtomContainer graphI = graphList.get(i);
 
             for (int j = i + 1; j < graphList.size(); j++) {
                 IAtomContainer graphJ = graphList.get(j);
 
                 // Gi included in Gj or Gj included in Gi then
                 // reduce the irrelevant solution
                 if (isSubgraph(graphJ, graphI, shouldMatchBonds)) {
                     reducedGraphList.remove(graphI);
                 } else if (isSubgraph(graphI, graphJ, shouldMatchBonds)) {
                     reducedGraphList.remove(graphJ);
                 }
             }
         }
         return reducedGraphList;
     }
 
     /**
      *  Checks for single atom cases before doing subgraph/isomorphism search
      *
      * @param  sourceGraph  AtomContainer to match on. Must not be an IQueryAtomContainer.
      * @param  targetGraph  AtomContainer as query. May be an IQueryAtomContainer.
      * @return     List of List of CDKRMap objects for the Atoms (not Bonds!), null if no single atom case
      * @throws org.openscience.cdk.exception.CDKException if the first molecule is an instance
      * of IQueryAtomContainer
      */
     public static List<CDKRMap> checkSingleAtomCases(IAtomContainer sourceGraph, IAtomContainer targetGraph)
             throws CDKException {
         if (sourceGraph instanceof IQueryAtomContainer) {
             throw new CDKException("The first IAtomContainer must not be an IQueryAtomContainer");
         }
 
         if (targetGraph.getAtomCount() == 1) {
             List<CDKRMap> arrayList = new ArrayList<CDKRMap>();
             IAtom atom = targetGraph.getAtom(0);
             if (atom instanceof IQueryAtom) {
                 IQueryAtom qAtom = (IQueryAtom) atom;
                 for (int i = 0; i < sourceGraph.getAtomCount(); i++) {
                     if (qAtom.matches(sourceGraph.getAtom(i))) {
                         arrayList.add(new CDKRMap(i, 0));
                     }
                 }
             } else {
                 String atomSymbol = atom.getSymbol();
                 for (int i = 0; i < sourceGraph.getAtomCount(); i++) {
                     if (sourceGraph.getAtom(i).getSymbol().equals(atomSymbol)) {
                         arrayList.add(new CDKRMap(i, 0));
                     }
                 }
             }
             return arrayList;
         } else if (sourceGraph.getAtomCount() == 1) {
             List<CDKRMap> arrayList = new ArrayList<CDKRMap>();
             IAtom atom = sourceGraph.getAtom(0);
             for (int i = 0; i < targetGraph.getAtomCount(); i++) {
                 IAtom atom2 = targetGraph.getAtom(i);
                 if (atom2 instanceof IQueryAtom) {
                     IQueryAtom qAtom = (IQueryAtom) atom2;
                     if (qAtom.matches(atom)) {
                         arrayList.add(new CDKRMap(0, i));
                     }
                 } else {
                     if (atom2.getSymbol().equals(atom.getSymbol())) {
                         arrayList.add(new CDKRMap(0, i));
                     }
                 }
             }
             return arrayList;
         } else {
             return null;
         }
     }
 
 
/** Maps of matching atoms are created by using the get(Subgraph)IsmorphismMaps methods. */

public static List<List<CDKRMap>> makeAtomsMapsOfBondsMaps(List<List<CDKRMap>> list, IAtomContainer sourceGraph, IAtomContainer targetGraph) {
    List<List<CDKRMap>> atomsMaps = new ArrayList<>();
    for (List<CDKRMap> bondsMap : list) {
        List<CDKRMap> atomsMap = new ArrayList<>();
        for (CDKRMap bondMap : bondsMap) {
            int sourceAtomIndex = bondMap.getId1();
            int targetAtomIndex = bondMap.getId2();
            IAtom sourceAtom = sourceGraph.getAtom(sourceAtomIndex);
            IAtom targetAtom = targetGraph.getAtom(targetAtomIndex);
            CDKRMap atomMap = new CDKRMap(sourceAtomIndex, targetAtomIndex);
            atomsMap.add(atomMap);
        }
        atomsMaps.add(atomsMap);
    }
    return atomsMaps;
}
 

}