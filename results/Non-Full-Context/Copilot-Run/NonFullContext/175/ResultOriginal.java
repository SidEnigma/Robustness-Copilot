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
 import org.openscience.cdk.interfaces.IStereoElement;
 import org.openscience.cdk.interfaces.ITetrahedralChirality;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import static org.openscience.cdk.interfaces.ITetrahedralChirality.Stereo.CLOCKWISE;
 
 /**
  * Defines a stereo encoder factory for the hash code. The factory allows the
  * generation of stereo hash codes for molecules with predefined
  * {@link ITetrahedralChirality} stereo elements.
  *
  * @author John May
  * @cdk.module hash
  */
 public final class TetrahedralElementEncoderFactory implements StereoEncoderFactory {
 
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
 
         // for each tetrahedral element - create a new encoder
         for (IStereoElement se : container.stereoElements()) {
             if (se instanceof ITetrahedralChirality) {
                 encoders.add(encoder((ITetrahedralChirality) se, atomToIndex = indexMap(atomToIndex, container)));
             }
         }
 
         return encoders.isEmpty() ? StereoEncoder.EMPTY : new MultiStereoEncoder(encoders);
     }
 
 
/** Create an encoder for the {@link ITetrahedralChirality} element. */
 private static GeometryEncoder encoder(ITetrahedralChirality tc, Map<IAtom, Integer> atomToIndex){
     
      // get the atoms and their indices
      IAtom[] atoms = tc.getAtoms();
      int[] indices = new int[atoms.length];
      for (int i = 0; i < atoms.length; i++) {
            indices[i] = atomToIndex.get(atoms[i]);
      }
     
      // get the chirality
      int chirality = tc.getStereo() == CLOCKWISE ? 1 : 0;
     
      // get the parity
      int parity = tc.getParity() == ITetrahedralChirality.Parity.ODD ? 1 : 0;
     
      // get the configuration
      int[] config = new int[atoms.length];
      for (int i = 0; i < atoms.length; i++) {
            config[i] = tc.getConfiguration(atoms[i]);
      }
     
      // create the encoder
      return new GeometryEncoder(indices, chirality, parity, config);           
 }

 

}