/* Copyright (C) 1997-2008  The Chemistry Development Kit (CDK) project
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
  */
 package org.openscience.cdk.geometry;
 
 import java.awt.Dimension;
 import java.awt.geom.Rectangle2D;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Objects;
 import java.util.Set;
 import java.util.TreeMap;
 
 import javax.vecmath.Point2d;
 import javax.vecmath.Point3d;
 import javax.vecmath.Vector2d;
 import javax.vecmath.Vector3d;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemModel;
 import org.openscience.cdk.interfaces.IReaction;
 import org.openscience.cdk.interfaces.IRing;
 import org.openscience.cdk.interfaces.IRingSet;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.ReactionManipulator;
 import org.openscience.cdk.tools.manipulator.ChemModelManipulator;
 
 /**
  * A set of static utility classes for geometric calculations and operations.
  * This class is extensively used, for example, by JChemPaint to edit molecule.
  * All methods in this class change the coordinates of the atoms. Use GeometryTools if you use an external set of coordinates (e. g. renderingCoordinates from RendererModel)
  *
  * @author        seb
  * @author        Stefan Kuhn
  * @author        Egon Willighagen
  * @author        Ludovic Petain
  * @author        Christian Hoppe
  * @author        Niels Out
  * @author        John May
  *
  * @cdk.module    standard
  * @cdk.githash
  * @deprecated use {@link GeometryUtil} moved for dependency reorganisation
  */
 @Deprecated
 public class GeometryTools {
 
     private static ILoggingTool logger = LoggingToolFactory.createLoggingTool(GeometryTools.class);
 
     /**
      * Provides the coverage of coordinates for this molecule.
      *
      * @see GeometryTools#get2DCoordinateCoverage(org.openscience.cdk.interfaces.IAtomContainer)
      * @see GeometryTools#get3DCoordinateCoverage(org.openscience.cdk.interfaces.IAtomContainer)
      */
     public static enum CoordinateCoverage {
 
         /**
          * All atoms have coordinates.
          */
         FULL,
 
         /**
          * At least one atom has coordinates but not all.
          */
         PARTIAL,
 
         /**
          * No atoms have coordinates.
          */
         NONE
 
     };
 
 
/** Adds an automatically calculated offset to the coordinates of all atoms so that all coordinates are positive and the smallest x or y coordinate is exactly zero. */

public static void translateAllPositive(IAtomContainer atomCon) {
    // Find the smallest x and y coordinates
    double minX = Double.MAX_VALUE;
    double minY = Double.MAX_VALUE;
    for (IAtom atom : atomCon.atoms()) {
        Point2d point = atom.getPoint2d();
        if (point.x < minX) {
            minX = point.x;
        }
        if (point.y < minY) {
            minY = point.y;
        }
    }
    
    // Calculate the offset
    double offsetX = -minX;
    double offsetY = -minY;
    
    // Translate all atoms by the offset
    for (IAtom atom : atomCon.atoms()) {
        Point2d point = atom.getPoint2d();
        point.x += offsetX;
        point.y += offsetY;
        atom.setPoint2d(point);
    }
}
 

}