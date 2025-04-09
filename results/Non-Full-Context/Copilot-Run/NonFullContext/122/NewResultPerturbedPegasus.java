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
 
     /**
      *  Generate coordinates for all atoms which are singly bonded and have no
      *  coordinates. This is useful when hydrogens are present but have no coordinates.
      *  It knows about C, O, N, S only and will give tetrahedral or trigonal
      *  geometry elsewhere. Bond lengths are computed from covalent radii or taken
      *  out of a parameter set if available. Angles are tetrahedral or trigonal
      *
      * @param  atomContainer  the set of atoms involved
      * @throws CDKException
      * @cdk.keyword           coordinate calculation
      * @cdk.keyword           3D model
      */
     public void add3DCoordinatesForSinglyBondedLigands(IAtomContainer atomContainer) throws CDKException {
         IAtom refAtom = null;
         IAtom atomC = null;
         int nwanted = 0;
         for (int i = 0; i < atomContainer.getAtomCount(); i++) {
             refAtom = atomContainer.getAtom(i);
             if (refAtom.getAtomicNumber() != IElement.H && hasUnsetNeighbour(refAtom, atomContainer)) {
                 IAtomContainer noCoords = getUnsetAtomsInAtomContainer(refAtom, atomContainer);
                 IAtomContainer withCoords = getPlacedAtomsInAtomContainer(refAtom, atomContainer);
                 if (withCoords.getAtomCount() > 0) {
                     atomC = getPlacedHeavyAtomInAtomContainer(withCoords.getAtom(0), refAtom, atomContainer);
                 }
                 if (refAtom.getFormalNeighbourCount() == 0 && refAtom.getAtomicNumber() == IElement.C) {
                     nwanted = noCoords.getAtomCount();
                 } else if (refAtom.getFormalNeighbourCount() == 0 && refAtom.getAtomicNumber() != IElement.C) {
                     nwanted = 4;
                 } else {
                     nwanted = refAtom.getFormalNeighbourCount() - withCoords.getAtomCount();
                 }
                 Point3d[] newPoints = get3DCoordinatesForLigands(refAtom, noCoords, withCoords, atomC, nwanted,
                         DEFAULT_BOND_LENGTH_H, -1);
                 for (int j = 0; j < noCoords.getAtomCount(); j++) {
                     IAtom ligand = noCoords.getAtom(j);
                     Point3d newPoint = rescaleBondLength(refAtom, ligand, newPoints[j]);
                     ligand.setPoint3d(newPoint);
                     ligand.setFlag(CDKConstants.ISPLACED, true);
                 }
 
                 noCoords.removeAllElements();
                 withCoords.removeAllElements();
             }
         }
     }
 
     /**
      *  Rescales Point2 so that length 1-2 is sum of covalent radii.
      *  If covalent radii cannot be found, use bond length of 1.0
      *
      *@param  atom1          stationary atom
      *@param  atom2          movable atom
      *@param  point2         coordinates for atom 2
      *@return                new coordinates for atom 2
      */
     public Point3d rescaleBondLength(IAtom atom1, IAtom atom2, Point3d point2) {
         Point3d point1 = atom1.getPoint3d();
         Double d1 = atom1.getCovalentRadius();
         Double d2 = atom2.getCovalentRadius();
         // in case we have no covalent radii, set to 1.0
         double distance = (d1 == null || d2 == null) ? 1.0 : d1 + d2;
         if (pSet != null) {
             distance = getDistanceValue(atom1.getAtomTypeName(), atom2.getAtomTypeName());
         }
         Vector3d vect = new Vector3d(point2);
         vect.sub(point1);
         vect.normalize();
         vect.scale(distance);
         Point3d newPoint = new Point3d(point1);
         newPoint.add(vect);
         return newPoint;
     }
 
     /**
      *  Adds 3D coordinates for singly-bonded ligands of a reference atom (A).
      *  Initially designed for hydrogens. The ligands of refAtom are identified and
      *  those with 3D coordinates used to generate the new points. (This allows
      *  structures with partially known 3D coordinates to be used, as when groups
      *  are added.) "Bent" and "non-planar" groups can be formed by taking a subset
      *  of the calculated points. Thus R-NH2 could use 2 of the 3 points calculated
      *  from (1,iii) nomenclature: A is point to which new ones are "attached". A
      *  may have ligands B, C... B may have ligands J, K.. points X1, X2... are
      *  returned The cases (see individual routines, which use idealised geometry
      *  by default): (0) zero ligands of refAtom. The resultant points are randomly
      *  oriented: (i) 1 points required; +x,0,0 (ii) 2 points: use +x,0,0 and
      *  -x,0,0 (iii) 3 points: equilateral triangle in xy plane (iv) 4 points
      *  x,x,x, x,-x,-x, -x,x,-x, -x,-x,x (1a) 1 ligand(B) of refAtom which itself
      *  has a ligand (J) (i) 1 points required; vector along AB vector (ii) 2
      *  points: 2 vectors in ABJ plane, staggered and eclipsed wrt J (iii) 3
      *  points: 1 staggered wrt J, the others +- gauche wrt J (1b) 1 ligand(B) of
      *  refAtom which has no other ligands. A random J is generated and (1a)
      *  applied (2) 2 ligands(B, C) of refAtom A (i) 1 points required; vector in
      *  ABC plane bisecting AB, AC. If ABC is linear, no points (ii) 2 points: 2
      *  vectors at angle ang, whose resultant is 2i (3) 3 ligands(B, C, D) of
      *  refAtom A (i) 1 points required; if A, B, C, D coplanar, no points. else
      *  vector is resultant of BA, CA, DA fails if atom itself has no coordinates
      *  or &gt;4 ligands
      *
      * @param  refAtom        (A) to which new ligands coordinates could be added
      * @param  length         A-X length
      * @param  angle          B-A-X angle (used in certain cases)
      * @param  nwanted        Description of the Parameter
      * @param  noCoords       Description of the Parameter
      * @param  withCoords     Description of the Parameter
      * @param  atomC          Description of the Parameter
      * @return                Point3D[] points calculated. If request could not be
      *      fulfilled (e.g. too many atoms, or strange geometry, returns empty
      *      array (zero length, not null)
      * @throws CDKException
      * @cdk.keyword           coordinate generation
      */
 
     public Point3d[] get3DCoordinatesForLigands(IAtom refAtom, IAtomContainer noCoords, IAtomContainer withCoords,
             IAtom atomC, int nwanted, double length, double angle) throws CDKException {
         Point3d newPoints[] = new Point3d[1];
 
         if (noCoords.getAtomCount() == 0 && withCoords.getAtomCount() == 0) {
             return newPoints;
         }
 
         // too many ligands at present
         if (withCoords.getAtomCount() > 3) {
             return newPoints;
         }
 
         IBond.Order refMaxBondOrder = refAtom.getMaxBondOrder();
         if (refAtom.getFormalNeighbourCount() == 1) {
             //        	WTF???
         } else if (refAtom.getFormalNeighbourCount() == 2 || refMaxBondOrder == IBond.Order.TRIPLE) {
             //sp
             if (angle == -1) {
                 angle = SP_ANGLE;
             }
             newPoints[0] = get3DCoordinatesForSPLigands(refAtom, withCoords, length, angle);
         } else if (refAtom.getFormalNeighbourCount() == 3 || (refMaxBondOrder == IBond.Order.DOUBLE)) {
             //sp2
             if (angle == -1) {
                 angle = SP2_ANGLE;
             }
             try {
                 newPoints = get3DCoordinatesForSP2Ligands(refAtom, noCoords, withCoords, atomC, length, angle);
             } catch (Exception ex1) {
                 //				logger.debug("Get3DCoordinatesForLigandsERROR: Cannot place SP2 Ligands due to:" + ex1.toString());
                 throw new CDKException("Cannot place sp2 substituents\n" + ex1.getMessage(), ex1);
             }
 
         } else {
             //sp3
             try {
                 newPoints = get3DCoordinatesForSP3Ligands(refAtom, noCoords, withCoords, atomC, nwanted, length, angle);
             } catch (Exception ex1) {
                 //				logger.debug("Get3DCoordinatesForLigandsERROR: Cannot place SP3 Ligands due to:" + ex1.toString());
                 throw new CDKException("Cannot place sp3 substituents\n" + ex1.getMessage(), ex1);
             }
         }
         //logger.debug("...Ready "+newPoints.length+" "+newPoints[0].toString());
         return newPoints;
     }
 
     public Point3d get3DCoordinatesForSPLigands(IAtom refAtom, IAtomContainer withCoords, double length, double angle) {
         //logger.debug(" SP Ligands start "+refAtom.getPoint3d()+" "+(withCoords.getAtomAt(0)).getPoint3d());
         Vector3d ca = new Vector3d(refAtom.getPoint3d());
         ca.sub((withCoords.getAtom(0)).getPoint3d());
         ca.normalize();
         ca.scale(length);
         Point3d newPoint = new Point3d(refAtom.getPoint3d());
         newPoint.add(ca);
         return newPoint;
     }
 
     /**
      *  Main method for the calculation of the ligand coordinates for sp2 atoms.
      *  Decides if one or two coordinates should be created
      *
      *@param  refAtom            central atom (Atom)
      *@param  noCoords           Description of the Parameter
      *@param  withCoords         Description of the Parameter
      *@param  atomC              Description of the Parameter
      *@param  length             Description of the Parameter
      *@param  angle              Description of the Parameter
      *@return                    coordinates as Points3d []
      */
     public Point3d[] get3DCoordinatesForSP2Ligands(IAtom refAtom, IAtomContainer noCoords, IAtomContainer withCoords,
             IAtom atomC, double length, double angle) {
         //logger.debug(" SP2 Ligands start");
         Point3d newPoints[] = new Point3d[1];
         if (angle < 0) {
             angle = SP2_ANGLE;
         }
         if (withCoords.getAtomCount() >= 2) {
             //logger.debug("Wanted:1 "+noCoords.getAtomCount());
             newPoints[0] = calculate3DCoordinatesSP2_1(refAtom.getPoint3d(), (withCoords.getAtom(0)).getPoint3d(),
                     (withCoords.getAtom(1)).getPoint3d(), length, -1 * angle);
 
         } else if (withCoords.getAtomCount() <= 1) {
             //logger.debug("NoCoords 2:"+noCoords.getAtomCount());
             newPoints = calculate3DCoordinatesSP2_2(refAtom.getPoint3d(), (withCoords.getAtom(0)).getPoint3d(),
                     (atomC != null) ? atomC.getPoint3d() : null, length, angle);
         }
         //logger.debug("Ready SP2");
         return newPoints;
     }
 
     /**
      *  Main method for the calculation of the ligand coordinates for sp3 atoms.
      *  Decides how many coordinates should be created
      *
      *@param  refAtom            central atom (Atom)
      *@param  nwanted            how many ligands should be created
      *@param  length             bond length
      *@param  angle              angle in a B-A-(X) system; a=central atom;
      *      x=ligand with unknown coordinates
      *@param  noCoords           Description of the Parameter
      *@param  withCoords         Description of the Parameter
      *@param  atomC              Description of the Parameter
      *@return                    Description of the Return Value
      */
     public Point3d[] get3DCoordinatesForSP3Ligands(IAtom refAtom, IAtomContainer noCoords, IAtomContainer withCoords,
             IAtom atomC, int nwanted, double length, double angle) {
         //logger.debug("SP3 Ligands start ");
         Point3d newPoints[] = new Point3d[0];
         Point3d aPoint = refAtom.getPoint3d();
         int nwithCoords = withCoords.getAtomCount();
         if (angle < 0) {
             angle = TETRAHEDRAL_ANGLE;
         }
         if (nwithCoords == 0) {
             newPoints = calculate3DCoordinates0(refAtom.getPoint3d(), nwanted, length);
         } else if (nwithCoords == 1) {
             newPoints = calculate3DCoordinates1(aPoint, (withCoords.getAtom(0)).getPoint3d(),
                     (atomC != null) ? atomC.getPoint3d() : null, nwanted, length, angle);
         } else if (nwithCoords == 2) {
             Point3d bPoint = withCoords.getAtom(0).getPoint3d();
             Point3d cPoint = withCoords.getAtom(1).getPoint3d();
             newPoints = calculate3DCoordinates2(aPoint, bPoint, cPoint, nwanted, length, angle);
         } else if (nwithCoords == 3) {
             Point3d bPoint = withCoords.getAtom(0).getPoint3d();
             Point3d cPoint = withCoords.getAtom(1).getPoint3d();
             newPoints = new Point3d[1];
             Point3d dPoint = withCoords.getAtom(2).getPoint3d();
             newPoints[0] = calculate3DCoordinates3(aPoint, bPoint, cPoint, dPoint, length);
         }
         //logger.debug("...Ready");
         return newPoints;
     }
 
     /**
      *  Calculates substituent points. Calculate substituent points for (0) zero
      *  ligands of aPoint. The resultant points are randomly oriented: (i) 1 points
      *  required; +x,0,0 (ii) 2 points: use +x,0,0 and -x,0,0 (iii) 3 points:
      *  equilateral triangle in the xy plane (iv) 4 points x,x,x, x,-x,-x, -x,x,-x,
      *  -x,-x,x where 3x**2 = bond length
      *
      *@param  aPoint   to which substituents are added
      *@param  nwanted  number of points to calculate (1-4)
      *@param  length   from aPoint
      *@return          Point3d[] nwanted points (or zero if failed)
      */
     public Point3d[] calculate3DCoordinates0(Point3d aPoint, int nwanted, double length) {
         Point3d points[] = new Point3d[0];
         if (nwanted == 1) {
             points = new Point3d[1];
             points[0] = new Point3d(aPoint);
             points[0].add(new Vector3d(length, 0.0, 0.0));
         } else if (nwanted == 2) {
             points = new Point3d[2];
             points[0] = new Point3d(aPoint);
             points[0].add(new Vector3d(length, 0.0, 0.0));
             points[1] = new Point3d(aPoint);
             points[1].add(new Vector3d(-length, 0.0, 0.0));
         } else if (nwanted == 3) {
             points = new Point3d[3];
             points[0] = new Point3d(aPoint);
             points[0].add(new Vector3d(length, 0.0, 0.0));
             points[1] = new Point3d(aPoint);
             points[1].add(new Vector3d(-length * 0.5, -length * 0.5 * Math.sqrt(3.0), 0.0f));
             points[2] = new Point3d(aPoint);
             points[2].add(new Vector3d(-length * 0.5, length * 0.5 * Math.sqrt(3.0), 0.0f));
         } else if (nwanted == 4) {
             points = new Point3d[4];
             double dx = length / Math.sqrt(3.0);
             points[0] = new Point3d(aPoint);
             points[0].add(new Vector3d(dx, dx, dx));
             points[1] = new Point3d(aPoint);
             points[1].add(new Vector3d(dx, -dx, -dx));
             points[2] = new Point3d(aPoint);
             points[2].add(new Vector3d(-dx, -dx, dx));
             points[3] = new Point3d(aPoint);
             points[3].add(new Vector3d(-dx, dx, -dx));
         }
         return points;
     }
 
     /**
      *  Calculate new point(s) X in a B-A system to form B-A-X. Use C as reference
      *  for * staggering about the B-A bond (1a) 1 ligand(B) of refAtom (A) which
      *  itself has a ligand (C) (i) 1 points required; vector along AB vector (ii)
      *  2 points: 2 vectors in ABC plane, staggered and eclipsed wrt C (iii) 3
      *  points: 1 staggered wrt C, the others +- gauche wrt C If C is null, a
      *  random non-colinear C is generated
      *
      *@param  aPoint   to which substituents are added
      *@param  nwanted  number of points to calculate (1-3)
      *@param  length   A-X length
      *@param  angle    B-A-X angle
      *@param  bPoint   Description of the Parameter
      *@param  cPoint   Description of the Parameter
      *@return          Point3d[] nwanted points (or zero if failed)
      */
     public Point3d[] calculate3DCoordinates1(Point3d aPoint, Point3d bPoint, Point3d cPoint, int nwanted,
             double length, double angle) {
         Point3d points[] = new Point3d[nwanted];
         // BA vector
         Vector3d ba = new Vector3d(aPoint);
         ba.sub(bPoint);
         ba.normalize();
         // if no cPoint, generate a random reference
         if (cPoint == null) {
             Vector3d cVector = getNonColinearVector(ba);
             cPoint = new Point3d(cVector);
         }
         // CB vector
         Vector3d cb = new Vector3d(bPoint);
         cb.sub(cPoint);
         cb.normalize();
         // if A, B, C colinear, replace C by random point
         double cbdotba = cb.dot(ba);
         if (cbdotba > 0.999999) {
             Vector3d cVector = getNonColinearVector(ba);
             cPoint = new Point3d(cVector);
             cb = new Vector3d(bPoint);
             cb.sub(cPoint);
         }
         // cbxba = c x b
         Vector3d cbxba = new Vector3d();
         cbxba.cross(cb, ba);
         cbxba.normalize();
         // create three perp axes ba, cbxba, and ax
         Vector3d ax = new Vector3d();
         ax.cross(cbxba, ba);
         ax.normalize();
         double drot = Math.PI * 2.0 / (double) nwanted;
         for (int i = 0; i < nwanted; i++) {
             double rot = (double) i * drot;
             points[i] = new Point3d(aPoint);
             Vector3d vx = new Vector3d(ba);
             vx.scale(-Math.cos(angle) * length);
             Vector3d vy = new Vector3d(ax);
             vy.scale(Math.cos(rot) * length);
             Vector3d vz = new Vector3d(cbxba);
             vz.scale(Math.sin(rot) * length);
             points[i].add(vx);
             points[i].add(vy);
             points[i].add(vz);
         }
         /*
          * ax = null; cbxba = null; ba = null; cb = null;
          */
         return points;
     }
 
 
/** A B-A-C system is formed by calculating new point(s) X. */

public Point3d[] calculate3DCoordinates2(Point3d aPoint, Point3d bPoint, Point3d cPoint, int nwanted, double length, double angle) {
    Point3d points[] = new Point3d[nwanted];
    Vector3d ba = new Vector3d(aPoint);
    ba.sub(bPoint);
    ba.normalize();
    Vector3d cb = new Vector3d(bPoint);
    cb.sub(cPoint);
    cb.normalize();
    double cbdotba = cb.dot(ba);
    if (cbdotba > 0.999999) {
        Vector3d cVector = getNonColinearVector(ba);
        cPoint = new Point3d(cVector);
        cb = new Vector3d(bPoint);
        cb.sub(cPoint);
    }
    Vector3d cbxba = new Vector3d();
    cbxba.cross(cb, ba);
    cbxba.normalize();
    Vector3d ax = new Vector3d();
    ax.cross(cbxba, ba);
    ax.normalize();
    double drot = Math.PI * 2.0 / (double) nwanted;
    for (int i = 0; i < nwanted; i++) {
        double rot = (double) i * drot;
        points[i] = new Point3d(aPoint);
        Vector3d vx = new Vector3d(ba);
        vx.scale(-Math.cos(angle) * length);
        Vector3d vy = new Vector3d(ax);
        vy.scale(Math.cos(rot) * length);
        Vector3d vz = new Vector3d(cbxba);
        vz.scale(Math.sin(rot) * length);
        points[i].add(vx);
        points[i].add(vy);
        points[i].add(vz);
    }
    return points;
}
 

}