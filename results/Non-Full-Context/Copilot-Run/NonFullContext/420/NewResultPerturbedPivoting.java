/*
  * Copyright (C) 2004-2007  The Chemistry Development Kit (CDK) project
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public License
  * as published by the Free Software Foundation; either version 2.1
  * of the License, or (at your option) any later version.
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
 
 package org.openscience.cdk.geometry.surface;
 
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
 import org.openscience.cdk.tools.periodictable.PeriodicTable;
 
 import javax.vecmath.Point3d;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 /**
  * A class representing the solvent accessible surface area surface of a molecule.
  *
  * <p>This class is based on the Python implementation of the DCLM method
  * ({@cdk.cite EIS95}) by Peter McCluskey, which is a non-analytical method to generate a set of points
  * representing the solvent accessible surface area of a molecule.
  *
  * <p>The neighbor list is a simplified version of that
  * described in {@cdk.cite EIS95} and as a result, the surface areas of the atoms may not be exact
  * (compared to analytical calculations). The tessellation is slightly different from
  * that described by McCluskey and uses recursive subdivision starting from an icosahedral
  * representation.
  *
  * <p>The default solvent radius used is 1.4A and setting this to 0 will give the
  * Van der Waals surface. The accuracy can be increased by increasing the tessellation
  * level, though the default of 4 is a good balance between accuracy and speed.
  *
  * @author      Rajarshi Guha
  * @cdk.created 2005-05-08
  * @cdk.module  qsarmolecular
  * @cdk.githash
  * @cdk.bug     1846421
  */
 public class NumericalSurface {
 
     private static ILoggingTool logger         = LoggingToolFactory.createLoggingTool(NumericalSurface.class);
     double          solventRadius  = 1.4;
     int             tesslevel      = 4;
     IAtom[]         atoms;
     List<Point3d>[] surfPoints;
     double[]        areas;
     double[]        volumes;
 
     /**
      * Constructor to initialize the surface calculation with default values.
      *
      * This constructor use the Van der Waals radii as defined in <i>org/openscience/cdk/config/data/jmol_atomtypes.txt</i>
      * of the source distribution. Also uses a tesselation level of 4 and solvent radius of 1.4A.
      *
      * @param atomContainer The {@link IAtomContainer} for which the surface is to be calculated
      */
     public NumericalSurface(IAtomContainer atomContainer) {
         this.atoms = AtomContainerManipulator.getAtomArray(atomContainer);
         init();
     }
 
     /**
      * Constructor to initialize the surface calculation with user specified values.
      *
      * This constructor use the Van der Waals radii as defined in <i>org/openscience/cdk/config/data/jmol_atomtypes.txt</i>
      * of the source distribution
      *
      * @param atomContainer The {@link IAtomContainer} for which the surface is to be calculated
      * @param solventRadius The radius of a solvent molecule that is used to extend
      * the radius of each atom. Setting to 0 gives the Van der Waals surface
      * @param tesslevel The number of levels that the subdivision algorithm for tessellation
      * should use
      */
     public NumericalSurface(IAtomContainer atomContainer, double solventRadius, int tesslevel) {
         this.solventRadius = solventRadius;
         this.atoms = AtomContainerManipulator.getAtomArray(atomContainer);
         this.tesslevel = tesslevel;
         init();
     }
 
     /**
      * Evaluate the surface.
      *
      * This method generates the points on the accessible surface area of each atom
      * as well as calculating the surface area of each atom.
      * @deprecated
      */
     @Deprecated
     public void calculateSurface() {
         // NO-OP
     }
 
 
/** Initialize the surface, generate the points on the accessible surface of each atom as well as the calculation of the surface of each atom. */
private void init() {
    // Initialize the surfPoints and areas arrays
    surfPoints = new List[atoms.length];
    areas = new double[atoms.length];
    volumes = new double[atoms.length];

    // Generate the points on the accessible surface of each atom
    for (int i = 0; i < atoms.length; i++) {
        IAtom atom = atoms[i];
        surfPoints[i] = generateSurfacePoints(atom);
    }

    // Calculate the surface area and volume of each atom
    for (int i = 0; i < atoms.length; i++) {
        IAtom atom = atoms[i];
        areas[i] = calculateSurfaceArea(atom);
        volumes[i] = calculateVolume(atom);
    }
}
 

}