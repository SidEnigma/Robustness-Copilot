/* Copyright (C) 2005-2007  Christian Hoppe <chhoppe@users.sf.net>
  *                    2014  Mark B Vine (orcid:0000-0002-7794-0426)
  *
  *  Contact: cdk-devel@list.sourceforge.net
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
  *  You should have received a copy of the GNU Lesser General Public License
  *  along with this program; if not, write to the Free Software
  *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 package org.openscience.cdk.modeling.builder3d;
 
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 import javax.vecmath.Point2d;
 import javax.vecmath.Point3d;
 import javax.vecmath.Vector3d;
 
 import org.openscience.cdk.AtomContainer;
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.geometry.GeometryUtil;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IElement;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 
 /**
  * Place aliphatic <b>chains</b> with Z matrix method. Please use {@link
  * ModelBuilder3D} to place general molecules.
  *
  * @author         chhoppe
  * @cdk.keyword    AtomPlacer3D
  * @cdk.created    2004-10-8
  * @cdk.module     builder3d
  * @cdk.githash
  * @see ModelBuilder3D
  */
 public class AtomPlacer3D {
 
     private Map<Object, List>   pSet                    = null;
     private double[]            distances;
     private int[]               firstAtoms              = null;
     private double[]            angles                  = null;
     private int[]               secondAtoms             = null;
     private double[]            dihedrals               = null;
     private int[]               thirdAtoms              = null;
     private final static double DIHEDRAL_EXTENDED_CHAIN = (180.0 / 180) * Math.PI;
     private final static double DIHEDRAL_BRANCHED_CHAIN = 0.0;
     private final static double DEFAULT_BOND_LENGTH     = 1.5;
     private final static double DEFAULT_SP3_ANGLE       = 109.471;
     private final static double DEFAULT_SP2_ANGLE       = 120.000;
     private final static double DEFAULT_SP_ANGLE        = 180.000;
 
     private final ILoggingTool logger = LoggingToolFactory.createLoggingTool(AtomPlacer3D.class);
 
     AtomPlacer3D() {}
 
     /**
      *  Initialize the atomPlacer class.
      *
      * @param  parameterSet  Force Field parameter as Hashtable
      */
     public void initilize(Map parameterSet) {
         pSet = parameterSet;
     }
 
 
/** Count and find the first heavy atom(s) (non-hydrogen) in a chain. */
 public int[] findHeavyAtomsInChain(IAtomContainer molecule, IAtomContainer chain){
        int[] firstAtoms = new int[chain.getAtomCount()];
        int counter = 0;
        for (int i = 0; i < chain.getAtomCount(); i++) {
            IAtom atom = chain.getAtom(i);
            if (!atom.getSymbol().equals("H")) {
                firstAtoms[counter] = i;
                counter++;
            }
        }
        int[] firstAtoms2 = new int[counter];
        System.arraycopy(firstAtoms, 0, firstAtoms2, 0, counter);
        return firstAtoms2;     
 }

 

}