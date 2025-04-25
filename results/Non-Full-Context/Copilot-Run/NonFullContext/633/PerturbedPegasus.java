/*
  * Copyright (c) 2013 John May <jwmay@users.sf.net>
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public License
  * as published by the Free Software Foundation; either version 2.1
  * of the License, or (at your option) any later version.
  * All we ask is that proper credit is given for our work, which includes
  * - but is not limited to - adding the above copyright notice to the beginning
  * of your source code files, and to any copyright notice that you may distribute
  * with programs based on this work.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 U
  */
 
 package org.openscience.cdk.hash.stereo;
 
 
 import javax.vecmath.Point3d;
 
 /**
  * Calculate the geometric configuration of a double bond. The configuration is
  * provided as a parity (+1,-1) where +1 indicates the substituents are on
  * <i>opposite</i> sides (E or trans) and -1 indicates they are <i>together</i>
  * on the same side (Z or cis).
  *
  * @author John May
  * @cdk.module hash
  * @cdk.githash
  */
 final class DoubleBond3DParity extends GeometricParity {
 
     // coordinates of the double bond atoms:
     // x       w
     //  \     /
     //   u = v
     private Point3d u, v, x, w;
 
     /**
      * Create a new double bond parity for the 2D coordinates of the atoms.
      *
      * @param left             one atom of the double bond
      * @param right            the other atom of a double bond
      * @param leftSubstituent  the substituent atom connected to the left atom
      * @param rightSubstituent the substituent atom connected to the right atom
      */
     public DoubleBond3DParity(Point3d left, Point3d right, Point3d leftSubstituent, Point3d rightSubstituent) {
         this.u = left;
         this.v = right;
         this.x = leftSubstituent;
         this.w = rightSubstituent;
     }
 
 
/** The double bond has a configuration. */
 public int parity(){}

 

}