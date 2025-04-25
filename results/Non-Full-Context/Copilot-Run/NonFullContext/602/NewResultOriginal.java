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
 
     /**
      *  Count and find first heavy atom(s) (non Hydrogens) in a chain.
      *
      * @param  molecule the reference molecule for searching the chain
      * @param  chain  chain to be searched
      * @return        the atom number of the first heavy atom the number of heavy atoms in the chain
      */
     public int[] findHeavyAtomsInChain(IAtomContainer molecule, IAtomContainer chain) {
         int[] heavy = {-1, -1};
         int hc = 0;
         for (int i = 0; i < chain.getAtomCount(); i++) {
             if (isHeavyAtom(chain.getAtom(i))) {
                 if (heavy[0] < 0) {
                     heavy[0] = molecule.indexOf(chain.getAtom(i));
                 }
                 hc++;
             }
         }
         heavy[1] = hc;
         return heavy;
     }
 
     /**
      *  Mark all atoms in chain as placed. (CDKConstant ISPLACED)
      *
      * @param  ac  chain
      * @return     chain all atoms marked as placed
      */
     public IAtomContainer markPlaced(IAtomContainer ac) {
         for (int i = 0; i < ac.getAtomCount(); i++) {
             ac.getAtom(i).setFlag(CDKConstants.ISPLACED, true);
         }
         return ac;
     }
 
 
/** Method assigns 3D coordinates to the heavy atoms in an aliphatic chain. */

public void placeAliphaticHeavyChain(IAtomContainer molecule, IAtomContainer chain) throws CDKException {
    int[] heavyAtoms = findHeavyAtomsInChain(molecule, chain);
    int startIndex = heavyAtoms[0];
    int numHeavyAtoms = heavyAtoms[1];
    
    if (numHeavyAtoms < 2) {
        throw new CDKException("Chain must contain at least two heavy atoms");
    }
    
    IAtom startAtom = molecule.getAtom(startIndex);
    IAtom secondAtom = molecule.getAtom(startIndex + 1);
    
    Point3d startPoint = new Point3d(startAtom.getPoint3d());
    Point3d secondPoint = new Point3d(secondAtom.getPoint3d());
    
    Vector3d bondVector = new Vector3d();
    bondVector.sub(secondPoint, startPoint);
    
    double bondLength = DEFAULT_BOND_LENGTH;
    if (pSet != null && pSet.containsKey("bondLength")) {
        bondLength = ((Double) pSet.get("bondLength")).doubleValue();
    }
    
    bondVector.normalize();
    bondVector.scale(bondLength);
    
    Point3d thirdPoint = new Point3d();
    thirdPoint.add(startPoint, bondVector);
    
    Vector3d normalVector = new Vector3d();
    normalVector.cross(bondVector, new Vector3d(0, 0, 1));
    normalVector.normalize();
    
    double sp3Angle = DEFAULT_SP3_ANGLE;
    if (pSet != null && pSet.containsKey("sp3Angle")) {
        sp3Angle = ((Double) pSet.get("sp3Angle")).doubleValue();
    }
    
    double sp2Angle = DEFAULT_SP2_ANGLE;
    if (pSet != null && pSet.containsKey("sp2Angle")) {
        sp2Angle = ((Double) pSet.get("sp2Angle")).doubleValue();
    }
    
    double spAngle = DEFAULT_SP_ANGLE;
    if (pSet != null && pSet.containsKey("spAngle")) {
        spAngle = ((Double) pSet.get("spAngle")).doubleValue();
    }
    
    double angle = sp3Angle;
    if (numHeavyAtoms == 2) {
        angle = sp2Angle;
    } else if (numHeavyAtoms == 1) {
        angle = spAngle;
    }
    
    GeometryUtil.setAngle(startAtom, secondAtom, thirdPoint, angle, normalVector);
    
    for (int i = startIndex + 2; i < startIndex + numHeavyAtoms; i++) {
        IAtom currentAtom = molecule.getAtom(i);
        Point3d currentPoint = new Point3d();
        currentPoint.add(thirdPoint, bondVector);
        
        if (numHeavyAtoms > 2) {
            Vector3d dihedralVector = new Vector3d();
            dihedralVector.cross(bondVector, normalVector);
            dihedralVector.normalize();
            
            double dihedralAngle = DIHEDRAL_EXTENDED_CHAIN;
            if (pSet != null && pSet.containsKey("dihedralAngle")) {
                dihedralAngle = ((Double) pSet.get("dihedralAngle")).doubleValue();
            }
            
            GeometryUtil.setDihedralAngle(startAtom, secondAtom, thirdPoint, currentAtom, dihedralAngle, dihedralVector);
        }
        
        currentAtom.setPoint3d(currentPoint);
        thirdPoint = currentPoint;
    }
    
    markPlaced(chain);
}
 

}