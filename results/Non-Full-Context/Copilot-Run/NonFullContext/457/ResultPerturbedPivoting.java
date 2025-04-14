/*
  * Copyright (c) 2013 European Bioinformatics Institute (EMBL-EBI)
  *                    John May <jwmay@users.sf.net>
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as published by
  * the Free Software Foundation; either version 2.1 of the License, or (at
  * your option) any later version. All we ask is that proper credit is given
  * for our work, which includes - but is not limited to - adding the above
  * copyright notice to the beginning of your source code files, and to any
  * copyright notice that you may distribute with programs based on this work.
  *
  * This program is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  * License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 U
  */
 
 package org.openscience.cdk.hash.stereo;
 
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IDoubleBondStereochemistry;
 import org.openscience.cdk.interfaces.IStereoElement;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import static org.openscience.cdk.interfaces.IDoubleBondStereochemistry.Conformation.OPPOSITE;
 
 /**
  * Defines a stereo encoder factory for the hash code. The factory allows the
  * generation of stereo hash codes for molecules with predefined {@link
  * IDoubleBondStereochemistry} stereo elements.
  *
  * @author John May
  * @cdk.module hash
  */
 public final class DoubleBondElementEncoderFactory implements StereoEncoderFactory {
 
     /**
      *{@inheritDoc}
      */
     @Override
     public StereoEncoder create(IAtomContainer container, int[][] graph) {
 
         // index atoms for quick lookup - wish we didn't have to do this
         // but the it's better than calling getAtomNumber every time - we use
         // a lazy creation so it's only created if there was a need for it
         Map<IAtom, Integer> atomToIndex = null;
 
         List<StereoEncoder> encoders = new ArrayList<StereoEncoder>();
 
         // for each double-bond element - create a new encoder
         for (IStereoElement se : container.stereoElements()) {
             if (se instanceof IDoubleBondStereochemistry) {
                 encoders.add(encoder((IDoubleBondStereochemistry) se, atomToIndex = indexMap(atomToIndex, container),
                         graph));
             }
         }
 
         return encoders.isEmpty() ? StereoEncoder.EMPTY : new MultiStereoEncoder(encoders);
     }
 
 
/** Create an encoder for the {@link IDoubleBondStereochemistry} element. */
 private static GeometryEncoder encoder(IDoubleBondStereochemistry dbs, Map<IAtom, Integer> atomToIndex, int[][] graph){
     
      // the atom indices for the double bond
      int a1 = atomToIndex.get(dbs.getFirstAtom());
      int a2 = atomToIndex.get(dbs.getSecondAtom());
      int a3 = atomToIndex.get(dbs.getThirdAtom());
     
      // the bond indices for the double bond
      int b1 = graph[a1][a2];
      int b2 = graph[a2][a3];
      int b3 = graph[a3][a1];
     
      // the bond indices for the opposite bond
      int b4 = graph[a1][a3];
      int b5 = graph[a2][a1];
      int b6 = graph[a3][a2];
     
      // the bond indices for the bonds between the double bond and the opposite bond
      int b7 = graph[a1][a2];
      int b8 = graph[a2][a3];
      int b9 = graph[a3][a1];
     
      // the bond indices for the bonds between the opposite bond and the double bond
      int b10 = graph[a1][a3];
      int b11 = graph[a2][a1];
      int b12 = graph[a3][a2];
     
      // the bond indices for the bonds between the double bond and the opposite bond
      int b13 = graph[a1][a2];
      int b14 = graph[a2][a3];
      int b15 = graph[a3][a1];
     
      // the bond indices for the bonds between the opposite bond and the double bond
      int b16 = graph[a1][a3];
      int b17 = graph[a2][a1];
      int b18 = graph[a3][a2];
     
      // the bond indices for the bonds between the double bond and the opposite bond
      int b19 = graph[a1][a2];
      int b20 = graph[a2][a3];
      int b21 = graph[a3][a1];
     
      // the bond indices for the bonds between the opposite bond           
 }

 

}