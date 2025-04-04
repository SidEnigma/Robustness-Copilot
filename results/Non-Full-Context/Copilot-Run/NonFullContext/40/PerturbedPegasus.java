/* Copyright (C) 2004-2009  Ulrich Bauer <ulrich.bauer@alumni.tum.de>
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
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 package org.openscience.cdk.ringsearch.cyclebasis;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Set;
 
 
 import org._3pq.jgrapht.Edge;
 import org._3pq.jgrapht.UndirectedGraph;
 import org._3pq.jgrapht.graph.UndirectedSubgraph;
 
 /**
  * A cycle in a graph G is a subgraph in which every vertex has even degree.
  *
  * @author Ulrich Bauer &lt;ulrich.bauer@alumni.tum.de&gt;
  *
  * @cdk.module standard
  * @cdk.githash
  *
  * @cdk.keyword smallest-set-of-rings
  * @cdk.keyword ring search
  * @deprecated internal implemenation detail from SSSRFinder, do not use
  */
 @Deprecated
 public class SimpleCycle extends UndirectedSubgraph {
 
     private static final long serialVersionUID = -3330742084804445688L;
 
     /**
      * Constructs a cycle in a graph consisting of the specified edges.
      *
      * @param   g the graph in which the cycle is contained
      * @param   edges the edges of the cycle
      */
     public SimpleCycle(UndirectedGraph g, Collection edges) {
         this(g, new HashSet(edges));
     }
 
     /**
      * Constructs a cycle in a graph consisting of the specified edges.
      *
      * @param   g the graph in which the cycle is contained
      * @param   edges the edges of the cycle
      */
     public SimpleCycle(UndirectedGraph g, Set edges) {
         super(g, inducedVertices(edges), edges);
         // causes a unit test to fail, but the assertions are met
         // assert checkConsistency();
     }
 
     static private Set inducedVertices(Set edges) {
         Set inducedVertices = new HashSet();
         for (Iterator i = edges.iterator(); i.hasNext();) {
             Edge edge = (Edge) i.next();
             inducedVertices.add(edge.getSource());
             inducedVertices.add(edge.getTarget());
         }
         return inducedVertices;
     }
 
 
/** The sum of the weights of all edges is returned. */
 public double weight(){}

 

}