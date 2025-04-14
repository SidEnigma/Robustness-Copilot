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
 
 
/** Given a left and right atoms, return a stereo encoder for them. The parent is the atom connected by a double bond to the two atoms. In case of simple double bonds the parent is the other atom. */
 static StereoEncoder newEncoder(IAtomContainer container, IAtom left, IAtom leftParent, IAtom right, IAtom rightParent, int[][] graph){
     
      // if left and right are constitutionally constitutionally bonded then skip
      if (graph[left.getIndex()][right.getIndex()] == 0) return null;
     
      // if left and right are constitutionally constitutionally bonded then skip
      if (graph[right.getIndex()][left.getIndex()] == 0) return null;
     
      // if left and right are constitutionally constitutionally bonded then skip
      if (graph[leftParent.getIndex()][rightParent.getIndex()] == 0) return null;
     
      // if left and right are constitutionally constitutionally bonded then skip
      if (graph[rightParent.getIndex()][leftParent.getIndex()] == 0) return null;
     
      // if left and right are constitutionally constitutionally bonded then skip
      if (graph[left.getIndex()][leftParent.getIndex()] == 0) return null;
     
      // if left and right are constitutionally constitutionally bonded then skip
      if (graph[right.getIndex()][rightParent.getIndex()] == 0) return null;
     
      // if left and right are constitutionally constitutionally bonded then skip
      if (graph[leftParent.getIndex()][right.getIndex()] == 0) return null;
     
      // if left and right are constitutionally bonded then skip
      if (graph[rightParent.getIndex()][left.getIndex()] == 0) return null;
     
      // if left and right are constitutionally bonded then skip
      if (graph[left.getIndex()][rightParent.getIndex()] == 0) return null;
     
      // if left and right are constitutionally bonded then skip
      if (graph[rightParent.getIndex()][leftParent.getIndex()] == 0) return null;
     
      // if left and right are constitutionally bonded then skip
      if (graph[leftParent.getIndex()][rightParent.getIndex()] == 0) return null;
     
      // if left and right are constitutionally bonded then skip
      if (graph[right.getIndex()][leftParent.getIndex()] == 0) return null;
     
      // if left and right are constitutionally bonded then skip
      if (graph[leftParent.         
 }

 

}