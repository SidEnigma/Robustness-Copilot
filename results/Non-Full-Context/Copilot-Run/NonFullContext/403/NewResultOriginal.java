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
 
 import org.openscience.cdk.graph.GraphUtil;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IDoubleBondStereochemistry;
 import org.openscience.cdk.interfaces.IStereoElement;
 import org.openscience.cdk.ringsearch.RingSearch;
 import org.openscience.cdk.stereo.ExtendedCisTrans;
 
 import javax.vecmath.Point2d;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.Map;
 
 /**
  * Correct double-bond configuration depiction in 2D to be correct for it's
  * specified {@link org.openscience.cdk.interfaces.IDoubleBondStereochemistry}. Ideally double-bond adjustment
  * should be done in when generating a structure diagram (and consider
  * overlaps). This method finds double bonds with incorrect depicted
  * configuration and reflects one side to correct the configuration.
  * <b>IMPORTANT: should be invoked before labelling up/down bonds. Cyclic
  * double-bonds with a configuration can not be corrected (error logged).</b>
  *
  * @author John May
  * @cdk.module sdg
  */
 final class CorrectGeometricConfiguration {
 
     /** The structure we are assigning labels to. */
     private final IAtomContainer      container;
 
     /** Adjacency list graph representation of the structure. */
     private final int[][]             graph;
 
     /** Lookup atom index (avoid IAtomContainer). */
     private final Map<IAtom, Integer> atomToIndex;
 
     /** Test if a bond is cyclic. */
     private final RingSearch          ringSearch;
 
     /** Visited flags when atoms are being reflected. */
     private final boolean[]           visited;
 
 
/** Adjust all double bond elements in the provided structure. */

public static IAtomContainer correct(IAtomContainer container) {
    int[][] graph = GraphUtil.toAdjList(container);
    Map<IAtom, Integer> atomToIndex = new HashMap<>();
    for (int i = 0; i < container.getAtomCount(); i++) {
        atomToIndex.put(container.getAtom(i), i);
    }
    RingSearch ringSearch = new RingSearch(container);
    boolean[] visited = new boolean[container.getAtomCount()];

    // Adjust all double bond elements in the provided structure
    for (IStereoElement stereoElement : container.stereoElements()) {
        if (stereoElement instanceof IDoubleBondStereochemistry) {
            IDoubleBondStereochemistry doubleBondStereo = (IDoubleBondStereochemistry) stereoElement;
            IBond doubleBond = doubleBondStereo.getStereoBond();
            IAtom[] doubleBondAtoms = doubleBondStereo.getStereoAtoms();
            Point2d[] doubleBondCoords = new Point2d[2];
            for (int i = 0; i < 2; i++) {
                doubleBondCoords[i] = container.getAtomPoint2d(doubleBondAtoms[i]);
            }
            ExtendedCisTrans extendedCisTrans = new ExtendedCisTrans(doubleBondAtoms, doubleBondCoords);
            extendedCisTrans.setContainer(container);
            extendedCisTrans.setGraph(graph);
            extendedCisTrans.setAtomToIndex(atomToIndex);
            extendedCisTrans.setRingSearch(ringSearch);
            extendedCisTrans.setVisited(visited);
            extendedCisTrans.correct();
        }
    }

    return container;
}
 

}