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
 
     /**
      *  This makes maps of matching atoms out of atom maps of matching bonds as produced by the get(Subgraph|Ismorphism)Maps methods.
      *
      * @param  list   The list produced by the getMap method.
      * @param  sourceGraph  The first atom container. Must not be atom IQueryAtomContainer.
      * @param  targetGraph  The second one (first and second as in getMap). May be an QueryAtomContaienr.
      * @return     A Vector of Vectors of CDKRMap objects of matching Atoms.
      */
     public static List<List<CDKRMap>> makeAtomsMapsOfBondsMaps(List<List<CDKRMap>> list, IAtomContainer sourceGraph,
             IAtomContainer targetGraph) {
         if (list == null) {
             return list;
         }
         if (targetGraph.getAtomCount() == 1) {
             return list; // since the RMap is already an atom-atom mapping
         }
         List<List<CDKRMap>> result = new ArrayList<List<CDKRMap>>();
         for (List<CDKRMap> l2 : list) {
             result.add(makeAtomsMapOfBondsMap(l2, sourceGraph, targetGraph));
         }
         return result;
     }
 
     /**
      *  This makes atom map of matching atoms out of atom map of matching bonds as produced by the get(Subgraph|Ismorphism)Map methods.
      *
      * @param  list   The list produced by the getMap method.
      * @param  sourceGraph  first molecule. Must not be an IQueryAtomContainer.
      * @param  targetGraph  second molecule. May be an IQueryAtomContainer.
      * @return     The mapping found projected on sourceGraph. This is atom List of CDKRMap objects containing Ids of matching atoms.
      */
     public static List<CDKRMap> makeAtomsMapOfBondsMap(List<CDKRMap> list, IAtomContainer sourceGraph,
             IAtomContainer targetGraph) {
         if (list == null) {
             return (list);
         }
         List<CDKRMap> result = new ArrayList<CDKRMap>();
         for (int i = 0; i < list.size(); i++) {
             IBond bond1 = sourceGraph.getBond(list.get(i).getId1());
             IBond bond2 = targetGraph.getBond(list.get(i).getId2());
             IAtom[] atom1 = BondManipulator.getAtomArray(bond1);
             IAtom[] atom2 = BondManipulator.getAtomArray(bond2);
             for (int j = 0; j < 2; j++) {
                 List<IBond> bondsConnectedToAtom1j = sourceGraph.getConnectedBondsList(atom1[j]);
                 for (int k = 0; k < bondsConnectedToAtom1j.size(); k++) {
                     if (!bondsConnectedToAtom1j.get(k).equals(bond1)) {
                         IBond testBond = bondsConnectedToAtom1j.get(k);
                         for (int m = 0; m < list.size(); m++) {
                             IBond testBond2;
                             if ((list.get(m)).getId1() == sourceGraph.indexOf(testBond)) {
                                 testBond2 = targetGraph.getBond((list.get(m)).getId2());
                                 for (int n = 0; n < 2; n++) {
                                     List<IBond> bondsToTest = targetGraph.getConnectedBondsList(atom2[n]);
                                     if (bondsToTest.contains(testBond2)) {
                                         CDKRMap map;
                                         if (j == n) {
                                             map = new CDKRMap(sourceGraph.indexOf(atom1[0]),
                                                     targetGraph.indexOf(atom2[0]));
                                         } else {
                                             map = new CDKRMap(sourceGraph.indexOf(atom1[1]),
                                                     targetGraph.indexOf(atom2[0]));
                                         }
                                         if (!result.contains(map)) {
                                             result.add(map);
                                         }
                                         CDKRMap map2;
                                         if (j == n) {
                                             map2 = new CDKRMap(sourceGraph.indexOf(atom1[1]),
                                                     targetGraph.indexOf(atom2[1]));
                                         } else {
                                             map2 = new CDKRMap(sourceGraph.indexOf(atom1[0]),
                                                     targetGraph.indexOf(atom2[1]));
                                         }
                                         if (!result.contains(map2)) {
                                             result.add(map2);
                                         }
                                     }
                                 }
                             }
                         }
                     }
                 }
             }
         }
         return result;
     }
 
     /**
      *  Builds  the nodes of the CDKRGraph ( resolution graph ), from
      *  two atom containers (description of the two molecules to compare)
      *
      * @param  graph   the target CDKRGraph
      * @param  ac1   first molecule. Must not be an IQueryAtomContainer.
      * @param  ac2   second molecule. May be an IQueryAtomContainer.
      * @throws org.openscience.cdk.exception.CDKException if it takes too long to identify overlaps
      */
     private static void nodeConstructor(CDKRGraph graph, IAtomContainer ac1, IAtomContainer ac2,
             boolean shouldMatchBonds) throws CDKException {
         if (ac1 instanceof IQueryAtomContainer) {
             throw new CDKException("The first IAtomContainer must not be an IQueryAtomContainer");
         }
 
         // resets the target graph.
         graph.clear();
 
         // compares each bondA1 of G1 to each bondA1 of G2
         for (int i = 0; i < ac1.getBondCount(); i++) {
             IBond bondA1 = ac1.getBond(i);
             for (int j = 0; j < ac2.getBondCount(); j++) {
                 IBond bondA2 = ac2.getBond(j);
                 if (bondA2 instanceof IQueryBond) {
                     IQueryBond queryBond = (IQueryBond) bondA2;
                     IQueryAtom atom1 = (IQueryAtom) (bondA2.getBegin());
                     IQueryAtom atom2 = (IQueryAtom) (bondA2.getEnd());
                     if (queryBond.matches(bondA1)) {
                         // ok, bonds match
                         if (atom1.matches(bondA1.getBegin()) && atom2.matches(bondA1.getEnd())
                                 || atom1.matches(bondA1.getEnd()) && atom2.matches(bondA1.getBegin())) {
                             // ok, atoms match in either order
                             graph.addNode(new CDKRNode(i, j));
                         }
                     }
                 } else {
                     // if both bonds are compatible then create an association node
                     // in the resolution graph
                     if (isMatchFeasible(ac1, bondA1, ac2, bondA2, shouldMatchBonds)) {
                         graph.addNode(new CDKRNode(i, j));
                     }
 
                 }
             }
         }
     }
 
     private static boolean isMatchFeasible(IAtomContainer ac1, IBond bondA1, IAtomContainer ac2, IBond bondA2,
             boolean shouldMatchBonds) {
 
         //Bond Matcher
         BondMatcher bondMatcher = new DefaultBondMatcher(ac1, bondA1, shouldMatchBonds);
         //Atom Matcher
         AtomMatcher atomMatcher1 = new DefaultRGraphAtomMatcher(ac1, bondA1.getBegin(), shouldMatchBonds);
         //Atom Matcher
         AtomMatcher atomMatcher2 = new DefaultRGraphAtomMatcher(ac1, bondA1.getEnd(), shouldMatchBonds);
 
         if (DefaultMatcher.isBondMatch(bondMatcher, ac2, bondA2, shouldMatchBonds)
                 && DefaultMatcher.isAtomMatch(atomMatcher1, atomMatcher2, ac2, bondA2, shouldMatchBonds)) {
             return true;
         }
         return false;
     }
 
     /**
      *  Build edges of the RGraphs
      *  This method create the edge of the CDKRGraph and
      *  calculates the incompatibility and neighbourhood
      *  relationships between CDKRGraph nodes.
      *
      * @param  graph   the rGraph
      * @param  ac1   first molecule. Must not be an IQueryAtomContainer.
      * @param  ac2   second molecule. May be an IQueryAtomContainer.
      * @throws org.openscience.cdk.exception.CDKException if it takes too long to get the overlaps
      */
     private static void arcConstructor(CDKRGraph graph, IAtomContainer ac1, IAtomContainer ac2) throws CDKException {
         // each node is incompatible with itself
         for (int i = 0; i < graph.getGraph().size(); i++) {
             CDKRNode rNodeX = graph.getGraph().get(i);
             rNodeX.getForbidden().set(i);
         }
 
         IBond bondA1;
         IBond bondA2;
         IBond bondB1;
         IBond bondB2;
 
         graph.setFirstGraphSize(ac1.getBondCount());
         graph.setSecondGraphSize(ac2.getBondCount());
 
         for (int i = 0; i < graph.getGraph().size(); i++) {
             CDKRNode rNodeX = graph.getGraph().get(i);
 
             // two nodes are neighbours if their adjacency
             // relationship in are equivalent in G1 and G2
             // else they are incompatible.
             for (int j = i + 1; j < graph.getGraph().size(); j++) {
                 CDKRNode rNodeY = graph.getGraph().get(j);
 
                 bondA1 = ac1.getBond(graph.getGraph().get(i).getRMap().getId1());
                 bondA2 = ac2.getBond(graph.getGraph().get(i).getRMap().getId2());
                 bondB1 = ac1.getBond(graph.getGraph().get(j).getRMap().getId1());
                 bondB2 = ac2.getBond(graph.getGraph().get(j).getRMap().getId2());
 
                 if (bondA2 instanceof IQueryBond) {
                     if (bondA1.equals(bondB1) || bondA2.equals(bondB2)
                             || !queryAdjacencyAndOrder(bondA1, bondB1, bondA2, bondB2)) {
                         rNodeX.getForbidden().set(j);
                         rNodeY.getForbidden().set(i);
                     } else if (hasCommonAtom(bondA1, bondB1)) {
                         rNodeX.getExtension().set(j);
                         rNodeY.getExtension().set(i);
                     }
                 } else {
                     if (bondA1.equals(bondB1) || bondA2.equals(bondB2)
                             || (!getCommonSymbol(bondA1, bondB1).equals(getCommonSymbol(bondA2, bondB2)))) {
                         rNodeX.getForbidden().set(j);
                         rNodeY.getForbidden().set(i);
                     } else if (hasCommonAtom(bondA1, bondB1)) {
                         rNodeX.getExtension().set(j);
                         rNodeY.getExtension().set(i);
                     }
                 }
             }
         }
     }
 
     /**
      * Determines if two bonds have at least one atom in common.
      *
      * @param  atom  first bondA1
      * @param  bondB  second bondA1
      * @return    the symbol of the common atom or "" if
      *            the 2 bonds have no common atom
      */
     private static boolean hasCommonAtom(IBond bondA, IBond bondB) {
         return bondA.contains(bondB.getBegin()) || bondA.contains(bondB.getEnd());
     }
 
     /**
      *  Determines if 2 bondA1 have 1 atom in common and returns the common symbol
      *
      * @param  atom  first bondA1
      * @param  bondB  second bondA1
      * @return    the symbol of the common atom or "" if
      *            the 2 bonds have no common atom
      */
     private static String getCommonSymbol(IBond bondA, IBond bondB) {
         String symbol = "";
 
         if (bondA.contains(bondB.getBegin())) {
             symbol = bondB.getBegin().getSymbol();
         } else if (bondA.contains(bondB.getEnd())) {
             symbol = bondB.getEnd().getSymbol();
         }
 
         return symbol;
     }
 
     /**
      *  Determines if 2 bondA1 have 1 atom in common if second is atom query AtomContainer
      *
      * @param  atom1  first bondA1
      * @param  bondB1  second bondA1
      * @return    the symbol of the common atom or "" if
      *            the 2 bonds have no common atom
      */
     private static boolean queryAdjacency(IBond bondA1, IBond bondB1, IBond bondA2, IBond bondB2) {
 
         IAtom atom1 = null;
         IAtom atom2 = null;
 
         if (bondA1.contains(bondB1.getBegin())) {
             atom1 = bondB1.getBegin();
         } else if (bondA1.contains(bondB1.getEnd())) {
             atom1 = bondB1.getEnd();
         }
 
         if (bondA2.contains(bondB2.getBegin())) {
             atom2 = bondB2.getBegin();
         } else if (bondA2.contains(bondB2.getEnd())) {
             atom2 = bondB2.getEnd();
         }
 
         if (atom1 != null && atom2 != null) {
             // well, this looks fishy: the atom2 is not always atom IQueryAtom !
             return ((IQueryAtom) atom2).matches(atom1);
         } else {
             return atom1 == null && atom2 == null;
         }
 
     }
 
     /**
      *  Determines if 2 bondA1 have 1 atom in common if second is atom query AtomContainer
      *  and wheter the order of the atoms is correct (atoms match).
      *
      * @param  bondA1  first bondA1
      * @param  bond2  second bondA1
      * @param queryBond1 first query bondA1
      * @param queryBond2 second query bondA1
      * @return    the symbol of the common atom or "" if the 2 bonds have no common atom
      */
     private static boolean queryAdjacencyAndOrder(IBond bond1, IBond bond2, IBond queryBond1, IBond queryBond2) {
 
         IAtom centralAtom = null;
         IAtom centralQueryAtom = null;
 
         if (bond1.contains(bond2.getBegin())) {
             centralAtom = bond2.getBegin();
         } else if (bond1.contains(bond2.getEnd())) {
             centralAtom = bond2.getEnd();
         }
 
         if (queryBond1.contains(queryBond2.getBegin())) {
             centralQueryAtom = queryBond2.getBegin();
         } else if (queryBond1.contains(queryBond2.getEnd())) {
             centralQueryAtom = queryBond2.getEnd();
         }
 
         if (centralAtom != null && centralQueryAtom != null && ((IQueryAtom) centralQueryAtom).matches(centralAtom)) {
             IQueryAtom queryAtom1 = (IQueryAtom) queryBond1.getOther(centralQueryAtom);
             IQueryAtom queryAtom2 = (IQueryAtom) queryBond2.getOther(centralQueryAtom);
             IAtom atom1 = bond1.getOther(centralAtom);
             IAtom atom2 = bond2.getOther(centralAtom);
             if (queryAtom1.matches(atom1) && queryAtom2.matches(atom2) || queryAtom1.matches(atom2)
                     && queryAtom2.matches(atom1)) {
                 return true;
             } else {
                 return false;
             }
         } else {
             return centralAtom == null && centralQueryAtom == null;
         }
 
     }
 
 
/** Check some simple heuristics to see if the subgraph query can realistically be an atom subgraph of the supergraph. */
private static boolean testSubgraphHeuristics(IAtomContainer ac1, IAtomContainer ac2) throws CDKException {
    // Check if ac1 is an IQueryAtomContainer
    if (ac1 instanceof IQueryAtomContainer) {
        throw new CDKException("The first IAtomContainer must not be an IQueryAtomContainer");
    }
    
    // Check if ac2 has only one atom
    if (ac2.getAtomCount() == 1) {
        return true;
    }
    
    // Check if ac1 has only one atom
    if (ac1.getAtomCount() == 1) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of atoms
    if (ac1.getAtomCount() != ac2.getAtomCount()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of bonds
    if (ac1.getBondCount() != ac2.getBondCount()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of rings
    if (ac1.getRingCount() != ac2.getRingCount()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of aromatic bonds
    if (ac1.getAromaticBondCount() != ac2.getAromaticBondCount()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of explicit hydrogen atoms
    if (ac1.getExplicitHydrogenCount() != ac2.getExplicitHydrogenCount()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of implicit hydrogen atoms
    if (ac1.getImplicitHydrogenCount() != ac2.getImplicitHydrogenCount()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of total hydrogen atoms
    if (ac1.getTotalHydrogenCount() != ac2.getTotalHydrogenCount()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of valence electrons
    if (ac1.getValencyElectronCount() != ac2.getValencyElectronCount()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of lone pairs
    if (ac1.getLonePairCount() != ac2.getLonePairCount()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of radical electrons
    if (ac1.getRadicalElectronCount() != ac2.getRadicalElectronCount()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of positive formal charges
    if (ac1.getFormalChargeCount(IAtomChargeFormal.POSITIVE) != ac2.getFormalChargeCount(IAtomChargeFormal.POSITIVE)) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of negative formal charges
    if (ac1.getFormalChargeCount(IAtomChargeFormal.NEGATIVE) != ac2.getFormalChargeCount(IAtomChargeFormal.NEGATIVE)) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of zero formal charges
    if (ac1.getFormalChargeCount(IAtomChargeFormal.ZERO) != ac2.getFormalChargeCount(IAtomChargeFormal.ZERO)) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of single bonds
    if (ac1.getBondCount(IBond.Order.SINGLE) != ac2.getBondCount(IBond.Order.SINGLE)) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of double bonds
    if (ac1.getBondCount(IBond.Order.DOUBLE) != ac2.getBondCount(IBond.Order.DOUBLE)) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of triple bonds
    if (ac1.getBondCount(IBond.Order.TRIPLE) != ac2.getBondCount(IBond.Order.TRIPLE)) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of aromatic bonds
    if (ac1.getBondCount(IBond.Order.AROMATIC) != ac2.getBondCount(IBond.Order.AROMATIC)) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of up/down bonds
    if (ac1.getBondCount(IBond.Stereo.UP) != ac2.getBondCount(IBond.Stereo.UP) ||
        ac1.getBondCount(IBond.Stereo.DOWN) != ac2.getBondCount(IBond.Stereo.DOWN)) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of cis/trans bonds
    if (ac1.getBondCount(IBond.Stereo.CIS_TRANS) != ac2.getBondCount(IBond.Stereo.CIS_TRANS)) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of ring bonds
    if (ac1.getRingBondCount() != ac2.getRingBondCount()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of ring atoms
    if (ac1.getRingAtomCount() != ac2.getRingAtomCount()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of ring systems
    if (ac1.getRingSystemCount() != ac2.getRingSystemCount()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of ring sizes
    if (ac1.getRingSizeCount() != ac2.getRingSizeCount()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of smallest rings
    if (ac1.getSmallestRingSize() != ac2.getSmallestRingSize()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of largest rings
    if (ac1.getLargestRingSize() != ac2.getLargestRingSize()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of smallest rings with heteroatoms
    if (ac1.getSmallestRingSizeWithHeteroatoms() != ac2.getSmallestRingSizeWithHeteroatoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of largest rings with heteroatoms
    if (ac1.getLargestRingSizeWithHeteroatoms() != ac2.getLargestRingSizeWithHeteroatoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of smallest rings with non-hydrogen atoms
    if (ac1.getSmallestRingSizeWithNonHydrogenAtoms() != ac2.getSmallestRingSizeWithNonHydrogenAtoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of largest rings with non-hydrogen atoms
    if (ac1.getLargestRingSizeWithNonHydrogenAtoms() != ac2.getLargestRingSizeWithNonHydrogenAtoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of smallest rings with heteroatoms and non-hydrogen atoms
    if (ac1.getSmallestRingSizeWithHeteroatomsAndNonHydrogenAtoms() != ac2.getSmallestRingSizeWithHeteroatomsAndNonHydrogenAtoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of largest rings with heteroatoms and non-hydrogen atoms
    if (ac1.getLargestRingSizeWithHeteroatomsAndNonHydrogenAtoms() != ac2.getLargestRingSizeWithHeteroatomsAndNonHydrogenAtoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of smallest rings with aromatic atoms
    if (ac1.getSmallestRingSizeWithAromaticAtoms() != ac2.getSmallestRingSizeWithAromaticAtoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of largest rings with aromatic atoms
    if (ac1.getLargestRingSizeWithAromaticAtoms() != ac2.getLargestRingSizeWithAromaticAtoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of smallest rings with non-aromatic atoms
    if (ac1.getSmallestRingSizeWithNonAromaticAtoms() != ac2.getSmallestRingSizeWithNonAromaticAtoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of largest rings with non-aromatic atoms
    if (ac1.getLargestRingSizeWithNonAromaticAtoms() != ac2.getLargestRingSizeWithNonAromaticAtoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of smallest rings with aromatic atoms and non-hydrogen atoms
    if (ac1.getSmallestRingSizeWithAromaticAtomsAndNonHydrogenAtoms() != ac2.getSmallestRingSizeWithAromaticAtomsAndNonHydrogenAtoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of largest rings with aromatic atoms and non-hydrogen atoms
    if (ac1.getLargestRingSizeWithAromaticAtomsAndNonHydrogenAtoms() != ac2.getLargestRingSizeWithAromaticAtomsAndNonHydrogenAtoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of smallest rings with non-aromatic atoms and non-hydrogen atoms
    if (ac1.getSmallestRingSizeWithNonAromaticAtomsAndNonHydrogenAtoms() != ac2.getSmallestRingSizeWithNonAromaticAtomsAndNonHydrogenAtoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of largest rings with non-aromatic atoms and non-hydrogen atoms
    if (ac1.getLargestRingSizeWithNonAromaticAtomsAndNonHydrogenAtoms() != ac2.getLargestRingSizeWithNonAromaticAtomsAndNonHydrogenAtoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of smallest rings with aromatic atoms and heteroatoms
    if (ac1.getSmallestRingSizeWithAromaticAtomsAndHeteroatoms() != ac2.getSmallestRingSizeWithAromaticAtomsAndHeteroatoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of largest rings with aromatic atoms and heteroatoms
    if (ac1.getLargestRingSizeWithAromaticAtomsAndHeteroatoms() != ac2.getLargestRingSizeWithAromaticAtomsAndHeteroatoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of smallest rings with non-aromatic atoms and heteroatoms
    if (ac1.getSmallestRingSizeWithNonAromaticAtomsAndHeteroatoms() != ac2.getSmallestRingSizeWithNonAromaticAtomsAndHeteroatoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of largest rings with non-aromatic atoms and heteroatoms
    if (ac1.getLargestRingSizeWithNonAromaticAtomsAndHeteroatoms() != ac2.getLargestRingSizeWithNonAromaticAtomsAndHeteroatoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of smallest rings with aromatic atoms and non-hydrogen atoms and heteroatoms
    if (ac1.getSmallestRingSizeWithAromaticAtomsAndNonHydrogenAtomsAndHeteroatoms() != ac2.getSmallestRingSizeWithAromaticAtomsAndNonHydrogenAtomsAndHeteroatoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of largest rings with aromatic atoms and non-hydrogen atoms and heteroatoms
    if (ac1.getLargestRingSizeWithAromaticAtomsAndNonHydrogenAtomsAndHeteroatoms() != ac2.getLargestRingSizeWithAromaticAtomsAndNonHydrogenAtomsAndHeteroatoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of smallest rings with non-aromatic atoms and non-hydrogen atoms and heteroatoms
    if (ac1.getSmallestRingSizeWithNonAromaticAtomsAndNonHydrogenAtomsAndHeteroatoms() != ac2.getSmallestRingSizeWithNonAromaticAtomsAndNonHydrogenAtomsAndHeteroatoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of largest rings with non-aromatic atoms and non-hydrogen atoms and heteroatoms
    if (ac1.getLargestRingSizeWithNonAromaticAtomsAndNonHydrogenAtomsAndHeteroatoms() != ac2.getLargestRingSizeWithNonAromaticAtomsAndNonHydrogenAtomsAndHeteroatoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of smallest rings with aromatic atoms and non-hydrogen atoms and non-heteroatoms
    if (ac1.getSmallestRingSizeWithAromaticAtomsAndNonHydrogenAtomsAndNonHeteroatoms() != ac2.getSmallestRingSizeWithAromaticAtomsAndNonHydrogenAtomsAndNonHeteroatoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of largest rings with aromatic atoms and non-hydrogen atoms and non-heteroatoms
    if (ac1.getLargestRingSizeWithAromaticAtomsAndNonHydrogenAtomsAndNonHeteroatoms() != ac2.getLargestRingSizeWithAromaticAtomsAndNonHydrogenAtomsAndNonHeteroatoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of smallest rings with non-aromatic atoms and non-hydrogen atoms and non-heteroatoms
    if (ac1.getSmallestRingSizeWithNonAromaticAtomsAndNonHydrogenAtomsAndNonHeteroatoms() != ac2.getSmallestRingSizeWithNonAromaticAtomsAndNonHydrogenAtomsAndNonHeteroatoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of largest rings with non-aromatic atoms and non-hydrogen atoms and non-heteroatoms
    if (ac1.getLargestRingSizeWithNonAromaticAtomsAndNonHydrogenAtomsAndNonHeteroatoms() != ac2.getLargestRingSizeWithNonAromaticAtomsAndNonHydrogenAtomsAndNonHeteroatoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of smallest rings with aromatic atoms and heteroatoms and non-hydrogen atoms
    if (ac1.getSmallestRingSizeWithAromaticAtomsAndHeteroatomsAndNonHydrogenAtoms() != ac2.getSmallestRingSizeWithAromaticAtomsAndHeteroatomsAndNonHydrogenAtoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of largest rings with aromatic atoms and heteroatoms and non-hydrogen atoms
    if (ac1.getLargestRingSizeWithAromaticAtomsAndHeteroatomsAndNonHydrogenAtoms() != ac2.getLargestRingSizeWithAromaticAtomsAndHeteroatomsAndNonHydrogenAtoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of smallest rings with non-aromatic atoms and heteroatoms and non-hydrogen atoms
    if (ac1.getSmallestRingSizeWithNonAromaticAtomsAndHeteroatomsAndNonHydrogenAtoms() != ac2.getSmallestRingSizeWithNonAromaticAtomsAndHeteroatomsAndNonHydrogenAtoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of largest rings with non-aromatic atoms and heteroatoms and non-hydrogen atoms
    if (ac1.getLargestRingSizeWithNonAromaticAtomsAndHeteroatomsAndNonHydrogenAtoms() != ac2.getLargestRingSizeWithNonAromaticAtomsAndHeteroatomsAndNonHydrogenAtoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of smallest rings with aromatic atoms and non-heteroatoms and non-hydrogen atoms
    if (ac1.getSmallestRingSizeWithAromaticAtomsAndNonHeteroatomsAndNonHydrogenAtoms() != ac2.getSmallestRingSizeWithAromaticAtomsAndNonHeteroatomsAndNonHydrogenAtoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of largest rings with aromatic atoms and non-heteroatoms and non-hydrogen atoms
    if (ac1.getLargestRingSizeWithAromaticAtomsAndNonHeteroatomsAndNonHydrogenAtoms() != ac2.getLargestRingSizeWithAromaticAtomsAndNonHeteroatomsAndNonHydrogenAtoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of smallest rings with non-aromatic atoms and non-heteroatoms and non-hydrogen atoms
    if (ac1.getSmallestRingSizeWithNonAromaticAtomsAndNonHeteroatomsAndNonHydrogenAtoms() != ac2.getSmallestRingSizeWithNonAromaticAtomsAndNonHeteroatomsAndNonHydrogenAtoms()) {
        return false;
    }
    
    // Check if ac1 and ac2 have the same number of largest rings with non-aromatic atoms and non-heteroatoms and non-hydrogen atoms
    if (ac1.getLargestRingSizeWithNonAromaticAtomsAndNonHeteroatomsAndNonHydrogenAtoms() != ac2.getLargestRingSizeWithNonAromaticAtomsAndNonHeteroatomsAndNonHydrogenAtoms()) {
        return false;
    }
    
    return true;
}
 

}