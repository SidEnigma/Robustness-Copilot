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
 
 
/** Distribute the bonded atoms (neighbours) of an atom such that they fill the  remaining space around an atom in a geometrically nice way. */
 public void distributePartners(IAtom atom, IAtomContainer placedNeighbours, Point2d sharedAtomsCenter, IAtomContainer unplacedNeighbours, double bondLength){
        double angle = 0;
        double angleIncrement = Math.PI / 2;
        double angleIncrementStep = Math.PI / 8;
        double angleIncrementStep2 = Math.PI / 16;
        double angleIncrementStep3 = Math.PI / 32;
        double angleIncrementStep4 = Math.PI / 64;
        double angleIncrementStep5 = Math.PI / 128;
        double angleIncrementStep6 = Math.PI / 256;
        double angleIncrementStep7 = Math.PI / 512;
        double angleIncrementStep8 = Math.PI / 1024;
        double angleIncrementStep9 = Math.PI / 2048;
        double angleIncrementStep10 = Math.PI / 4096;
        double angleIncrementStep11 = Math.PI / 8192;
        double angleIncrementStep12 = Math.PI / 16384;
        double angleIncrementStep13 = Math.PI / 32768;
        double angleIncrementStep14 = Math.PI / 65536;
        double angleIncrementStep15 = Math.PI / 131072;
        double angleIncrementStep16 = Math.PI / 262144;
        double angleIncrementStep17 = Math.PI / 524288;
        double angleIncrementStep18 = Math.PI / 1048576;
        double angleIncrementStep19 = Math.PI / 2097152;
        double angleIncrementStep20 = Math.PI / 4194304;
        double angleIncrementStep21 = Math.PI / 8388608;
        double angleIncrementStep22 = Math.PI / 16777216;
        double angleIncrementStep23 = Math.PI / 33554432;
        double angleIncrementStep24 = Math.PI / 67108864;
        double angleIncrementStep25 = Math.PI / 134217728;
        double angleIncrementStep26 = Math.PI / 268435456;
        double angleIncrementStep27 = Math.PI / 536870912;
        double angleIncrementStep28 = Math.PI / 1073741824;
        double angleIncrementStep29 = Math.PI / 2147483648;
        double angle        
 }

 

}