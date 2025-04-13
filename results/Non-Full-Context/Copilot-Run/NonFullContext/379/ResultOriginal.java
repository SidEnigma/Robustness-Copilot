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
 
 package org.openscience.cdk.layout;
 
 import org.openscience.cdk.BondRef;
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.geometry.GeometryUtil;
 import org.openscience.cdk.graph.GraphUtil;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IDoubleBondStereochemistry;
 import org.openscience.cdk.interfaces.IStereoElement;
 import org.openscience.cdk.interfaces.ITetrahedralChirality;
 import org.openscience.cdk.ringsearch.RingSearch;
 import org.openscience.cdk.stereo.Atropisomeric;
 import org.openscience.cdk.stereo.ExtendedTetrahedral;
 import org.openscience.cdk.stereo.Octahedral;
 import org.openscience.cdk.stereo.SquarePlanar;
 import org.openscience.cdk.stereo.TrigonalBipyramidal;
 import org.openscience.cdk.tools.LoggingToolFactory;
 
 import javax.vecmath.Point2d;
 import javax.vecmath.Vector2d;
 import java.util.ArrayDeque;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Comparator;
 import java.util.Deque;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Objects;
 import java.util.Set;
 
 import static org.openscience.cdk.interfaces.IBond.Order.DOUBLE;
 import static org.openscience.cdk.interfaces.IBond.Order.SINGLE;
 import static org.openscience.cdk.interfaces.IBond.Stereo.DOWN;
 import static org.openscience.cdk.interfaces.IBond.Stereo.DOWN_INVERTED;
 import static org.openscience.cdk.interfaces.IBond.Stereo.E_OR_Z;
 import static org.openscience.cdk.interfaces.IBond.Stereo.NONE;
 import static org.openscience.cdk.interfaces.IBond.Stereo.UP;
 import static org.openscience.cdk.interfaces.IBond.Stereo.UP_INVERTED;
 import static org.openscience.cdk.interfaces.IBond.Stereo.UP_OR_DOWN;
 import static org.openscience.cdk.interfaces.IBond.Stereo.UP_OR_DOWN_INVERTED;
 
 /**
  * Assigns non-planar labels (wedge/hatch) to the tetrahedral and extended tetrahedral
  * stereocentres in a 2D depiction. Labels are assigned to atoms using the following priority. <ol> <li>bond to non-stereo atoms</li> <li>acyclic
  * bonds</li> <li>bonds to atoms with lower degree (i.e. terminal)</li> <li>lower atomic number</li>
  * </ol>
  *
  * Unspecified bonds are also marked.
  *
  * @author John May
  * @cdk.module sdg
  */
 final class NonplanarBonds {
 
     /** The structure we are assigning labels to. */
     private final IAtomContainer container;
 
     /** Adjacency list graph representation of the structure. */
     private final int[][] graph;
 
     /** Search for cyclic atoms. */
     private final RingSearch ringSearch;
 
     /** Tetrahedral elements indexed by central atom. */
     private final ITetrahedralChirality[] tetrahedralElements;
 
     /** Double-bond elements indexed by end atoms. */
     private final IDoubleBondStereochemistry[] doubleBondElements;
 
     /** Lookup atom index (avoid IAtomContainer). */
     private final Map<IAtom, Integer> atomToIndex;
 
     /** Quick lookup of a bond give the atom index of it's atoms. */
     private final GraphUtil.EdgeToBondMap edgeToBond;
 
 
/** Assign non-planar, up and down labels to indicate tetrahedral configuration. */
 public static IAtomContainer assign(final IAtomContainer container){
      return new NonplanarBonds(container).assign();
  }
  
      /**
        * Constructor.
        *
        * @param container the structure to assign labels to
        */
      private NonplanarBonds(final IAtomContainer container) {
          this.container = container;
          this.ringSearch = new RingSearch(container);
          this.tetrahedralElements = container.getStereoElements().stream()
                  .filter(e -> e instanceof ITetrahedralChirality)
                  .map(e -> (ITetrahedralChirality) e)
                  .toArray(ITetrahedralChirality[]::new);
          this.doubleBondElements = container.getStereoElements().stream()
                  .filter(e -> e instanceof IDoubleBondStereochemistry)
                  .map(e -> (IDoubleBondStereochemistry) e)
                  .toArray(IDoubleBondStereochemistry[]::new);
          this.atomToIndex = new HashMap<>(container.getAtomCount());
          for (int i = 0; i < container.getAtomCount(); i++) {
              atomToIndex.put(container.getAtom(i), i);
          }
          this.edgeToBond = GraphUtil.EdgeToBondMap.withAtomContainer(container);
          this.graph = GraphUtil.toAdjacencyMatrix(container);
      }
  
      /**
        * Assigns non-planar labels to the structure.
        *
        * @return the structure with non-planar labels assigned
        */
      private IAtomContainer assign() {
          for (int i = 0; i < container.getAtomCount(); i++) {
              assign(i);
          }
          return container;
      }
  
      /**
        * Assigns non-planar labels to the specified atom.
        *
        * @param index the index of the atom to assign labels to
        */
      private void assign(final int index) {
          final IAtom atom = container.getAtom(index);
          final 
 }

 

}