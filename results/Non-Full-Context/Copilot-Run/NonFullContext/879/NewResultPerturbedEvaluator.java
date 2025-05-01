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
 
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomType;
 import org.openscience.cdk.interfaces.IBond;
 
 import javax.vecmath.Point2d;
 import javax.vecmath.Point3d;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 
 import static org.openscience.cdk.interfaces.IBond.Order.DOUBLE;
 import static org.openscience.cdk.interfaces.IBond.Stereo.E_OR_Z;
 
 /**
  * A stereo encoder factory encoding double bond configurations by 2D and 3D
  * coordinates. This factory will attempt to encode all double bonds that meet
  * the following conditions. Are not {@literal -N=N-} bonds, non-cumulated,
  * non-query and have each double bonded atom has at least one substituent. In
  * future the encoding rules may be more strict or even configurable but
  * currently they may be over zealous when encoding configurations with 3D
  * coordinates. <br> This class is intended to be used with a the hash
  * encoding classes and is easier used via the {@link org.openscience.cdk.hash.HashGeneratorMaker}.
  *
  * @author John May
  * @cdk.module hash
  * @cdk.githash
  * @see org.openscience.cdk.hash.HashGeneratorMaker
  */
 public final class GeometricDoubleBondEncoderFactory implements StereoEncoderFactory {
 
     /**
      * Create a stereo encoder for all potential 2D and 3D double bond stereo
      * configurations.
      *
      * @param container an atom container
      * @param graph     adjacency list representation of the container
      * @return a new encoder for tetrahedral elements
      */
     @Override
     public StereoEncoder create(IAtomContainer container, int[][] graph) {
 
         List<StereoEncoder> encoders = new ArrayList<StereoEncoder>(5);
 
         for (IBond bond : container.bonds()) {
 
             // if double bond and not E or Z query bond
             if (DOUBLE.equals(bond.getOrder()) && !E_OR_Z.equals(bond.getStereo())) {
 
                 IAtom left = bond.getBegin();
                 IAtom right = bond.getEnd();
 
                 // skip -N=N- double bonds which exhibit inversion
                 if (Integer.valueOf(7).equals(left.getAtomicNumber())
                         && Integer.valueOf(7).equals(right.getAtomicNumber())) continue;
 
                 StereoEncoder encoder = newEncoder(container, left, right, right, left, graph);
 
                 if (encoder != null) {
                     encoders.add(encoder);
                 }
             }
         }
 
         return encoders.isEmpty() ? StereoEncoder.EMPTY : new MultiStereoEncoder(encoders);
     }
 
     /**
      * Create a new encoder for the specified left and right atoms. The parent
      * is the atom which is connected by a double bond to the left and right
      * atom. For simple double bonds the parent of each is the other atom, in
      * cumulenes the parents are not the same.
      *
      * @param container   the molecule
      * @param left        the left atom
      * @param leftParent  the left atoms parent (usually {@literal right})
      * @param right       the right atom
      * @param rightParent the right atoms parent (usually {@literal left})
      * @param graph       adjacency list representation of the molecule
      * @return a stereo encoder (or null)
      */
     static StereoEncoder newEncoder(IAtomContainer container, IAtom left, IAtom leftParent, IAtom right,
             IAtom rightParent, int[][] graph) {
 
         List<IBond> leftBonds = container.getConnectedBondsList(left);
         List<IBond> rightBonds = container.getConnectedBondsList(right);
 
         // check the left and right bonds are acceptable
         if (accept(left, leftBonds) && accept(right, rightBonds)) {
 
             int leftIndex = container.indexOf(left);
             int rightIndex = container.indexOf(right);
 
             int leftParentIndex = container.indexOf(leftParent);
             int rightParentIndex = container.indexOf(rightParent);
 
             // neighbors of u/v with the bonded atoms (left,right) moved
             // to the back of each array. this is important as we can
             // drop it when we build the permutation parity
             int[] leftNeighbors = moveToBack(graph[leftIndex], leftParentIndex);
             int[] rightNeighbors = moveToBack(graph[rightIndex], rightParentIndex);
 
             int l1 = leftNeighbors[0];
             int l2 = leftNeighbors[1] == leftParentIndex ? leftIndex : leftNeighbors[1];
             int r1 = rightNeighbors[0];
             int r2 = rightNeighbors[1] == rightParentIndex ? rightIndex : rightNeighbors[1];
 
             // make 2D/3D geometry
             GeometricParity geometric = geometric(container, leftIndex, rightIndex, l1, l2, r1, r2);
 
             // geometric is null if there were no coordinates
             if (geometric != null) {
                 return new GeometryEncoder(new int[]{leftIndex, rightIndex}, new CombinedPermutationParity(
                         permutation(leftNeighbors), permutation(rightNeighbors)), geometric);
             }
 
         }
 
         return null;
     }
 
 
/** This method generates a new geometric parity for the given molecule and atom indices */

static GeometricParity geometric(IAtomContainer mol, int l, int r, int l1, int l2, int r1, int r2) {
    // Implementation logic goes here
    
    // Example implementation:
    // Calculate the coordinates of the atoms
    Point3d l1Coord = mol.getAtom(l1).getPoint3d();
    Point3d l2Coord = mol.getAtom(l2).getPoint3d();
    Point3d r1Coord = mol.getAtom(r1).getPoint3d();
    Point3d r2Coord = mol.getAtom(r2).getPoint3d();
    
    // Calculate the dihedral angle between the two planes formed by the atoms
    double dihedralAngle = calculateDihedralAngle(l1Coord, l2Coord, r1Coord, r2Coord);
    
    // Determine the geometric parity based on the dihedral angle
    GeometricParity parity;
    if (dihedralAngle > 0) {
        parity = GeometricParity.CIS;
    } else {
        parity = GeometricParity.TRANS;
    }
    
    return parity;
}
 

}