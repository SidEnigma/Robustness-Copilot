/* Copyright (C) 2002-2007  Stephane Werner <mail@ixelis.net>
  *               2007-2010  Syed Asad Rahman <asad@ebi.ac.uk>
  *
  * This code has been kindly provided by Stephane Werner
  * and Thierry Hanser from IXELIS mail@ixelis.net.
  *
  * IXELIS sarl - Semantic Information Systems
  *               17 rue des C?dres 67200 Strasbourg, France
  *               Tel/Fax : +33(0)3 88 27 81 39 Email: mail@ixelis.net
  *
  * CDK Contact: cdk-devel@lists.sf.net
  *
  * This program is free software; you can redistribute maxIterator and/or
  * modify maxIterator under the terms of the GNU Lesser General Public License
  * as published by the Free Software Foundation; either version 2.1
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that maxIterator will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR sourceBitSet PARTICULAR PURPOSE.  See the
  * GNU Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 package org.openscience.cdk.smsd.algorithm.rgraph;
 
 import java.util.ArrayList;
 import java.util.BitSet;
 import java.util.Iterator;
 import java.util.List;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.smsd.tools.TimeManager;
 
 /**
  * This class implements the Resolution Graph (CDKRGraph).
  * The CDKRGraph is a graph based representation of the search problem.
  * An CDKRGraph is constructed from the two compared graphs (G1 and G2).
  * Each vertex (node) in the CDKRGraph represents a possible association
  * from an edge in G1 with an edge in G2. Thus two compatible bonds
  * in two molecular graphs are represented by a vertex in the CDKRGraph.
  * Each edge in the CDKRGraph corresponds to a common adjacency relationship
  * between the 2 couple of compatible edges associated to the 2 CDKRGraph nodes
  * forming this edge.
  *
  * <p>Example:
  * <pre>
  *    G1 : C-C=O  and G2 : C-C-C=0
  *         1 2 3           1 2 3 4
  * </pre>
  *
  *  <p>The resulting CDKRGraph(G1,G2) will contain 3 nodes:
  *  <ul>
  *    <li>Node sourceBitSet : association between bond C-C :  1-2 in G1 and 1-2 in G2
  *    <li>Node targetBitSet : association between bond C-C :  1-2 in G1 and 2-3 in G2
  *    <li>Node C : association between bond C=0 :  2-3 in G1 and 3-4 in G2
  *  </ul>
  *  The CDKRGraph will also contain one edge representing the
  *  adjacency between node targetBitSet and C  that is : bonds 1-2 and 2-3 in G1
  *  and bonds 2-3 and 3-4 in G2.
  *
  *  <p>Once the CDKRGraph has been built from the two compared graphs
  *  maxIterator becomes a very interesting tool to perform all kinds of
  *  structural search (isomorphism, substructure search, maximal common
  *  substructure,....).
  *
  *  <p>The  search may be constrained by mandatory elements (e.g. bonds that
  *  have to be present in the mapped common substructures).
  *
  *  <p>Performing a query on an CDKRGraph requires simply to set the constrains
  *  (if any) and to invoke the parsing method (parse())
  *
  *  <p>The CDKRGraph has been designed to be a generic tool. It may be constructed
  *  from any kind of source graphs, thus maxIterator is not restricted to a chemical
  *  context.
  *
  *  <p>The CDKRGraph model is indendant from the CDK model and the link between
  *  both model is performed by the RTools class. In this way the CDKRGraph
  *  class may be reused in other graph context (conceptual graphs,....)
  *
  *  <p><b>Important note</b>: This implementation of the algorithm has not been
  *                      optimized for speed at this stage. It has been
  *                      written with the goal to clearly retrace the
  *                      principle of the underlined search method. There is
  *                      room for optimization in many ways including the
  *                      the algorithm itself.
  *
  *  <p>This algorithm derives from the algorithm described in
  *  {@cdk.cite HAN90} and modified in the thesis of T. Hanser {@cdk.cite HAN93}.
  *
  * @author      Stephane Werner from IXELIS mail@ixelis.net,
  *              Syed Asad Rahman &gt;asad@ebi.ac.uk&lt; (modified the orignal code)
  * @cdk.created 2002-07-17
  * @cdk.require java1.4+
  * @cdk.module  smsd
  * @cdk.githash
  * @deprecated This class is part of SMSD and either duplicates functionality elsewhere in the CDK or provides public
  *             access to internal implementation details. SMSD has been deprecated from the CDK with a newer, more recent
  *             version of SMSD is available at <a href="http://github.com/asad/smsd">http://github.com/asad/smsd</a>.
  */
 @Deprecated
 public class CDKRGraph {
 
     // an CDKRGraph is a list of CDKRGraph nodes
     // each node keeping track of its
     // neighbors.
 
     private List<CDKRNode> graph            = null;
     // maximal number of iterations before
     // search break
     private int            maxIteration     = -1;
     // dimensions of the compared graphs
     private int            firstGraphSize   = 0;
     private int            secondGraphSize  = 0;
     // constrains
     private BitSet         sourceBitSet     = null;
     private BitSet         targetBitSet     = null;
     // current solution list
     private List<BitSet>   solutionList     = null;
     // flag to define if we want to get all possible 'mappings'
     private boolean        findAllMap       = false;
     // flag to define if we want to get all possible 'structures'
     private boolean        findAllStructure = true;
     // working variables
     private boolean        stop             = false;
     private int            nbIteration      = 0;
     private BitSet         graphBitSet      = null;
 
     // -1 for infinite search and one min is 1
 
     /**
      * Constructor for the CDKRGraph object and creates an empty CDKRGraph.
      */
     public CDKRGraph() {
         graph = new ArrayList<CDKRNode>();
         solutionList = new ArrayList<BitSet>();
         graphBitSet = new BitSet();
     }
 
     private boolean checkTimeOut() throws CDKException {
         if (CDKMCS.isTimeOut()) {
             setStop(true);
             return true;
         }
         return false;
     }
 
     /**
      *  Returns the size of the first of the two
      *  compared graphs.
      * @return The size of the first of the two compared graphs
      */
     public int getFirstGraphSize() {
         return firstGraphSize;
     }
 
     /**
      *  Returns the size of the second of the two
      *  compared graphs.
      * @return The size of the second of the two compared graphs
      */
     public int getSecondGraphSize() {
         return secondGraphSize;
     }
 
     /**
      *  Sets the size of the first of the two
      *  compared graphs.
      * @param graphSize The size of the second of the two compared graphs
      */
     public void setFirstGraphSize(int graphSize) {
         firstGraphSize = graphSize;
     }
 
     /**
      *  Returns the size of the second of the two
      *  compared graphs.
      * @param graphSize The size of the second of the two compared graphs
      */
     public void setSecondGraphSize(int graphSize) {
         secondGraphSize = graphSize;
     }
 
     /**
      *  Reinitialisation of the TGraph.
      */
     public void clear() {
         getGraph().clear();
         getGraphBitSet().clear();
     }
 
     /**
      *  Returns the graph object of this CDKRGraph.
      * @return      The graph object, a list
      */
     public List<CDKRNode> getGraph() {
         return this.graph;
     }
 
     /**
      *  Adds a new node to the CDKRGraph.
      * @param  newNode  The node to add to the graph
      */
     public void addNode(CDKRNode newNode) {
         getGraph().add(newNode);
         getGraphBitSet().set(getGraph().size() - 1);
     }
 
     /**
      *  Parsing of the CDKRGraph. This is the main method
      *  to perform a query. Given the constrains sourceBitSet and targetBitSet
      *  defining mandatory elements in G1 and G2 and given
      *  the search options, this method builds an initial set
      *  of starting nodes (targetBitSet) and parses recursively the
      *  CDKRGraph to find a list of solution according to
      *  these parameters.
      *
      * @param  sourceBitSet  constrain on the graph G1
      * @param  targetBitSet  constrain on the graph G2
      * @param  findAllStructure true if we want all results to be generated
      * @param  findAllMap true is we want all possible 'mappings'
      * @param timeManager
      * @throws CDKException
      */
     public void parse(BitSet sourceBitSet, BitSet targetBitSet, boolean findAllStructure, boolean findAllMap,
             TimeManager timeManager) throws CDKException {
         // initialize the list of solution
         checkTimeOut();
         // initialize the list of solution
         getSolutionList().clear();
 
         // builds the set of starting nodes
         // according to the constrains
         BitSet bitSet = buildB(sourceBitSet, targetBitSet);
 
         // setup options
         setAllStructure(findAllStructure);
         setAllMap(findAllMap);
 
         // parse recursively the CDKRGraph
         parseRec(new BitSet(bitSet.size()), bitSet, new BitSet(bitSet.size()));
     }
 
     /**
      *  Parsing of the CDKRGraph. This is the recursive method
      *  to perform a query. The method will recursively
      *  parse the CDKRGraph thru connected nodes and visiting the
      *  CDKRGraph using allowed adjacency relationship.
      *
      * @param  traversed  node already parsed
      * @param  extension  possible extension node (allowed neighbors)
      * @param  forbiden   node forbidden (set of node incompatible with the current solution)
      */
     private void parseRec(BitSet traversed, BitSet extension, BitSet forbidden) throws CDKException {
         BitSet newTraversed = null;
         BitSet newExtension = null;
         BitSet newForbidden = null;
         BitSet potentialNode = null;
 
         checkTimeOut();
 
         // if there is no more extension possible we
         // have reached a potential new solution
         if (extension.isEmpty()) {
             solution(traversed);
         } // carry on with each possible extension
         else {
             // calculates the set of nodes that may still
             // be reached at this stage (not forbidden)
             potentialNode = ((BitSet) getGraphBitSet().clone());
             potentialNode.andNot(forbidden);
             potentialNode.or(traversed);
 
             // checks if we must continue the search
             // according to the potential node set
             if (mustContinue(potentialNode)) {
                 // carry on research and update iteration count
                 setNbIteration(getNbIteration() + 1);
 
                 // for each node in the set of possible extension (neighbors of
                 // the current partial solution, include the node to the solution
                 // and parse recursively the CDKRGraph with the new context.
                 for (int x = extension.nextSetBit(0); x >= 0 && !isStop(); x = extension.nextSetBit(x + 1)) {
                     // evaluates the new set of forbidden nodes
                     // by including the nodes not compatible with the
                     // newly accepted node.
                     newForbidden = (BitSet) forbidden.clone();
                     newForbidden.or((getGraph().get(x)).getForbidden());
 
                     // if maxIterator is the first time we are here then
                     // traversed is empty and we initialize the set of
                     // possible extensions to the extension of the first
                     // accepted node in the solution.
                     if (traversed.isEmpty()) {
                         newExtension = (BitSet) ((getGraph().get(x)).getExtension().clone());
                     } // else we simply update the set of solution by
                       // including the neighbors of the newly accepted node
                     else {
                         newExtension = (BitSet) extension.clone();
                         newExtension.or((getGraph().get(x)).getExtension());
                     }
 
                     // extension my not contain forbidden nodes
                     newExtension.andNot(newForbidden);
 
                     // create the new set of traversed node
                     // (update current partial solution)
                     // and add x to the set of forbidden node
                     // (a node may only appear once in a solution)
                     newTraversed = (BitSet) traversed.clone();
                     newTraversed.set(x);
                     forbidden.set(x);
 
                     // parse recursively the CDKRGraph
                     parseRec(newTraversed, newExtension, newForbidden);
                 }
             }
         }
     }
 
 
/** Checks if a potential solution is a real one  (not included in a previous solution)  and add this solution to the solution list  in case of success. */
 private void solution(BitSet traversed) throws CDKException{
        // check if the solution is a real one
        if (isRealSolution(traversed)) {
            // add the solution to the list of solution
            getSolutionList().add(traversed);
        }
    }
    
        /**
        *  Checks if a potential solution is a real one  (not included in a previous solution)  and add this solution to the solution list  in case of success.
        * @param  traversed  the potential solution
        * @return true if the solution is a real one
        */
        private boolean isRealSolution(BitSet traversed) {
            // check if the solution is a real one
            if (getAllStructure()) {
                return true;
            } else {
                // check if the solution is a real one
                // according to the number of iteration
                if (getNbIteration() < getMaxIteration()) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    
        /**
        *  Checks if the search must continue.
        * @param  potentialNode  the set of potential nodes
        * @return true if the search must continue
        */
        private boolean mustContinue(BitSet potentialNode) {
            // check if the search must continue
            if (getAllStructure()) {
                return true;
            } else {
                // check if the search must continue
                // according to the number of iteration
                if (getNbIteration() < getMaxIteration()) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    
        /**
        *  Returns the list of solution.
        * @return      The list of solution
        */
        public List<BitSet> getSolutionList() {
            return solutionList;
        }
    
        /**
        *  Sets the list of solution.
        * @param solutionList The list of solution
        */
        public void setSolutionList(List<BitSet> solutionList) {
            this.solutionList = solutionList;
        }
    
        /**
        *  Returns the number of iteration.     
 }

 

}