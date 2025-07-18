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
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.isomorphism.matchers.IQueryBond;
 import org.openscience.cdk.BondRef;
 
 /**
  * Represents a pharmacophore query distance constraint.
  *
  * @author Rajarshi Guha
  * @cdk.module pcore
  * @cdk.githash
  * @cdk.keyword pharmacophore
  * @cdk.keyword 3D isomorphism
  * @see org.openscience.cdk.pharmacophore.PharmacophoreQueryAtom
  * @see org.openscience.cdk.pharmacophore.PharmacophoreMatcher
  * @see org.openscience.cdk.isomorphism.matchers.QueryAtomContainer
  */
 public class PharmacophoreQueryBond extends Bond implements IQueryBond {
 
     private double upper;
     private double lower;
 
     public PharmacophoreQueryBond() {}
 
     /**
      * Create a query distance constraint between two query groups.
      *
      * Note that the distance is only considered upto 2 decimal places.
      *
      * @param atom1 The first pharmacophore group
      * @param atom2 The second pharmacophore group
      * @param lower The lower bound of the distance between the two groups
      * @param upper The upper bound of the distance between the two groups
      * @see #PharmacophoreQueryBond(PharmacophoreQueryAtom,PharmacophoreQueryAtom,double)
      */
     public PharmacophoreQueryBond(PharmacophoreQueryAtom atom1, PharmacophoreQueryAtom atom2, double lower, double upper) {
         super(atom1, atom2);
         this.upper = round(upper, 2);
         this.lower = round(lower, 2);
     }
 
     /**
      * Create a query distance constraint between two query groups.
      * 
      * This constructor allows you to define a query distance constraint
      * such that the distance between the two query groups is exact
      * (i.e., not a range).
      *
      * Note that the distance is only considered upto 2 decimal places.
      *
      * @param atom1    The first pharmacophore group
      * @param atom2    The second pharmacophore group
      * @param distance The exact distance between the two groups
      * @see #PharmacophoreQueryBond(PharmacophoreQueryAtom, PharmacophoreQueryAtom, double, double)
      */
     public PharmacophoreQueryBond(PharmacophoreQueryAtom atom1, PharmacophoreQueryAtom atom2, double distance) {
         super(atom1, atom2);
         this.upper = round(distance, 2);
         this.lower = round(distance, 2);
     }
 
 
/** The query distance constraint is checked to see if it matches the target distance. */
 public boolean matches(IBond bond){}

 

}