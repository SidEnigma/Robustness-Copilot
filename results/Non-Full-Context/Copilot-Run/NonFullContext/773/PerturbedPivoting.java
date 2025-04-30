/* Copyright (C) 2005-2007  Christian Hoppe <chhoppe@users.sf.net>
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
 package org.openscience.cdk.modeling.builder3d;
 
 import java.util.List;
 import java.util.Map;
 import java.util.Objects;
 
 import javax.vecmath.AxisAngle4d;
 import javax.vecmath.Matrix3d;
 import javax.vecmath.Point3d;
 import javax.vecmath.Vector3d;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IElement;
 
 /**
  *  A set of static utility classes for geometric calculations on Atoms.
  *
  *@author         Peter Murray-Rust,chhoppe,egonw
  *@cdk.created    2003-??-??
  * @cdk.module    builder3d
  * @cdk.githash
  */
 public class AtomTetrahedralLigandPlacer3D {
 
     private Map                 pSet                  = null;
     public final static double  DEFAULT_BOND_LENGTH_H = 1.0;
 
     public final static double  TETRAHEDRAL_ANGLE     = 2.0 * Math.acos(1.0 / Math.sqrt(3.0));
 
     private final static double SP2_ANGLE             = 120 * Math.PI / 180;
     private final static double SP_ANGLE              = Math.PI;
 
     final static Vector3d       XV                    = new Vector3d(1.0, 0.0, 0.0);
     final static Vector3d       YV                    = new Vector3d(0.0, 1.0, 0.0);
 
     /**
      *  Constructor for the AtomTetrahedralLigandPlacer3D object.
      */
     AtomTetrahedralLigandPlacer3D() {}
 
     /**
      *  Constructor for the setParameterSet object.
      *
      *@param  moleculeParameter  Description of the Parameter
      */
     public void setParameterSet(Map moleculeParameter) {
         pSet = moleculeParameter;
     }
 
 
/** Generate coordinates for all atoms that are individually bonded and do not have coordinates. */
 public void add3DCoordinatesForSinglyBondedLigands(IAtomContainer atomContainer) throws CDKException{}

 

}