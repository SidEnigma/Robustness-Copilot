/* Copyright (C) 2003-2007  The Chemistry Development Kit (CDK) project
  *
  *  Contact: cdk-devel@lists.sourceforge.net
  *
  *  This program is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU Lesser General Public License
  *  as published by the Free Software Foundation; either version 2.1
  *  of the License, or (at your option) any later version.
  *  All we ask is that proper credit is given for our work, which includes
  *  - but is not limited to - adding the above copyright notice to the beginning
  *  of your source code files, and to any copyright notice that you may distribute
  *  with programs based on this work.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU Lesser General Public License for more details.
  *
  *  You should have received a copy of the GNU Lesser General Public License
  *  along with this program; if not, write to the Free Software
  *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  *
  */
 package org.openscience.cdk.layout;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.config.Elements;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.geometry.BondTools;
 import org.openscience.cdk.geometry.GeometryUtil;
 import org.openscience.cdk.graph.GraphUtil;
 import org.openscience.cdk.graph.PathTools;
 import org.openscience.cdk.graph.matrix.ConnectionMatrix;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IRing;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
 
 import javax.vecmath.Point2d;
 import javax.vecmath.Tuple2d;
 import javax.vecmath.Vector2d;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Comparator;
 import java.util.List;
 import java.util.stream.Collectors;
 import java.util.stream.StreamSupport;
 
 /**
  *  Methods for generating coordinates for atoms in various situations. They can
  *  be used for Automated Structure Diagram Generation or in the interactive
  *  buildup of molecules by the user.
  *
  *@author      steinbeck
  *@cdk.created 2003-08-29
  *@cdk.module  sdg
  * @cdk.githash
  */
 public class AtomPlacer {
 
     private static final double ANGLE_120 = Math.toRadians(120);
     public final static boolean debug = true;
     public static final String PRIORITY = "Weight";
     private static ILoggingTool logger = LoggingToolFactory.createLoggingTool(AtomPlacer.class);
 
     /**
      *  The molecule to be laid out. To be assigned from outside
      */
     IAtomContainer molecule = null;
 
     /**
      *  Constructor for the AtomPlacer object
      */
     public AtomPlacer() {}
 
     /**
      *  Return the molecule the AtomPlacer currently works with
      *
      *@return    the molecule the AtomPlacer currently works with
      */
     public IAtomContainer getMolecule() {
         return this.molecule;
     }
 
     /**
      *  Sets the molecule the AtomPlacer currently works with
      *
      *@param  molecule  the molecule the AtomPlacer currently works with
      */
     public void setMolecule(IAtomContainer molecule) {
         this.molecule = molecule;
     }
 
 
/** Distribute the bonded (neighboring) atoms of an atom so that they fill the remaining space around an atom in a geometrically pleasing way. */

public void distributePartners(IAtom atom, IAtomContainer placedNeighbours, Point2d sharedAtomsCenter, IAtomContainer unplacedNeighbours, double bondLength) {
    // Get the number of placed and unplaced neighbors
    int numPlacedNeighbours = placedNeighbours.getAtomCount();
    int numUnplacedNeighbours = unplacedNeighbours.getAtomCount();

    // Calculate the total number of neighbors
    int totalNeighbours = numPlacedNeighbours + numUnplacedNeighbours;

    // Calculate the angle between each neighbor
    double angleBetweenNeighbours = 2 * Math.PI / totalNeighbours;

    // Calculate the initial angle for the first neighbor
    double initialAngle = Math.atan2(sharedAtomsCenter.y - atom.getPoint2d().y, sharedAtomsCenter.x - atom.getPoint2d().x);

    // Create a list to store the angles for each neighbor
    List<Double> angles = new ArrayList<>();

    // Add the initial angle for the first neighbor
    angles.add(initialAngle);

    // Calculate the angles for the remaining neighbors
    for (int i = 1; i < totalNeighbours; i++) {
        double angle = initialAngle + i * angleBetweenNeighbours;
        angles.add(angle);
    }

    // Sort the angles in ascending order
    angles.sort(Comparator.naturalOrder());

    // Distribute the placed neighbors
    for (int i = 0; i < numPlacedNeighbours; i++) {
        IAtom neighbour = placedNeighbours.getAtom(i);
        double angle = angles.get(i);

        // Calculate the position of the neighbor
        double x = atom.getPoint2d().x + bondLength * Math.cos(angle);
        double y = atom.getPoint2d().y + bondLength * Math.sin(angle);

        // Set the position of the neighbor
        neighbour.setPoint2d(new Point2d(x, y));
    }

    // Distribute the unplaced neighbors
    for (int i = 0; i < numUnplacedNeighbours; i++) {
        IAtom neighbour = unplacedNeighbours.getAtom(i);
        double angle = angles.get(i + numPlacedNeighbours);

        // Calculate the position of the neighbor
        double x = atom.getPoint2d().x + bondLength * Math.cos(angle);
        double y = atom.getPoint2d().y + bondLength * Math.sin(angle);

        // Set the position of the neighbor
        neighbour.setPoint2d(new Point2d(x, y));
    }
}
 

}