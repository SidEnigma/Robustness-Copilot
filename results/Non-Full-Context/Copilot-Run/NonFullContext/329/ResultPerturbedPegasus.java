/* Copyright (C) 2004-2008  Rajarshi Guha <rajarshi.guha@gmail.com>
  *
  *  Contact: cdk-devel@lists.sourceforge.net
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
 package org.openscience.cdk.pharmacophore;
 
 import org.openscience.cdk.Bond;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.isomorphism.matchers.IQueryBond;
 import org.openscience.cdk.BondRef;
 
 /**
  * Represents a pharmacophore query angle constraint.
  *
  * @author Rajarshi Guha
  * @cdk.module pcore
  * @cdk.githash
  * @cdk.keyword pharmacophore
  * @cdk.keyword 3D isomorphism
  * @see PharmacophoreQueryAtom
  * @see PharmacophoreMatcher
  * @see org.openscience.cdk.isomorphism.matchers.QueryAtomContainer
  */
 public class PharmacophoreQueryAngleBond extends Bond implements IQueryBond {
 
     private double upper;
     private double lower;
 
     public PharmacophoreQueryAngleBond() {}
 
     /**
      * Create a query angle constraint between three query groups.
      * 
      * Note that the angle is only considered upto 2 decimal places.
      *
      * @param atom1 The first pharmacophore group
      * @param atom2 The second pharmacophore group
      * @param atom3 The third pharmacophore group
      * @param lower The lower bound of the angle between the three groups
      * @param upper The upper bound of the angle between the three groups
      */
     public PharmacophoreQueryAngleBond(PharmacophoreQueryAtom atom1, PharmacophoreQueryAtom atom2,
             PharmacophoreQueryAtom atom3, double lower, double upper) {
         super(new IAtom[]{atom1, atom2, atom3});
         this.upper = round(upper, 2);
         this.lower = round(lower, 2);
     }
 
     /**
      * Create a query angle constraint between three query groups.
      * 
      * This constructor allows you to define a query angle constraint
      * such that the angle between the three query groups is exact
      * (i.e., not a range).
      * 
      * Note that the angle is only considered upto 2 decimal places.
      *
      * @param atom1 The first pharmacophore group
      * @param atom2 The second pharmacophore group
      * @param atom3 The third pharmacophore group
      * @param angle The exact angle between the two groups
      */
     public PharmacophoreQueryAngleBond(PharmacophoreQueryAtom atom1, PharmacophoreQueryAtom atom2,
             PharmacophoreQueryAtom atom3, double angle) {
         super(new PharmacophoreQueryAtom[]{atom1, atom2, atom3});
         this.upper = round(angle, 2);
         this.lower = round(angle, 2);
     }
 
 
/** The query angle constraint is checked to see if it matches the target distance. */
 public boolean matches(IBond bond){
        if(!(bond instanceof Bond)) return false;
        IBond b = (IBond)bond;
        if(b.getOrder() != IBond.Order.SINGLE) return false;
        if(b.getAtom(0) != getAtom(0) && b.getAtom(0) != getAtom(1) && b.getAtom(0) != getAtom(2)) return false;
        if(b.getAtom(1) != getAtom(0) && b.getAtom(1) != getAtom(1) && b.getAtom(1) != getAtom(2)) return false;
        if(b.getAtom(2) != getAtom(0) && b.getAtom(2) != getAtom(1) && b.getAtom(2) != getAtom(2)) return false;
        double angle = getAngle(b);
        return angle >= lower && angle <= upper;
    }
    
        /**
        * Get the angle between the three atoms in this bond.
        *
        * @param bond The bond to get the angle between
        * @return The angle between the three atoms in this bond
        */
        public double getAngle(IBond bond) {
            double angle = 0.0;
            IAtom atom1 = bond.getAtom(0);
            IAtom atom2 = bond.getAtom(1);
            IAtom atom3 = bond.getAtom(2);
            if(atom1 == getAtom(0)) {
                if(atom2 == getAtom(1)) {
                    angle = getAngle(atom1, atom2, atom3);
                } else if(atom2 == getAtom(2)) {
                    angle = getAngle(atom1, atom3, atom2);
                }
            } else if(atom1 == getAtom(1)) {
                if(atom2 == getAtom(0)) {
                    angle = getAngle(atom2, atom1, atom3);
                } else if(atom2 == getAtom(2        
 }

 

}