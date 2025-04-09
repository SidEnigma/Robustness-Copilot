/*
  * Copyright (c) 2013 John May <jwmay@users.sf.net>
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
 
 import javax.vecmath.Point2d;
 import javax.vecmath.Point3d;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 /**
  * Stereo encoder factory for 2D and 3D cumulative double bonds.
  *
  * @author John May
  * @cdk.module hash
  */
 public class GeometricCumulativeDoubleBondFactory implements StereoEncoderFactory {
 
     /**
      * Create a stereo encoder for cumulative double bonds.
      *
      * @param container the container
      * @param graph     adjacency list representation of the container
      * @return a stereo encoder
      */
     @Override
     public StereoEncoder create(IAtomContainer container, int[][] graph) {
 
         int n = container.getAtomCount();
         BondMap map = new BondMap(n);
 
         List<StereoEncoder> encoders = new ArrayList<StereoEncoder>(1);
 
         // index double bonds by their atoms
         for (IBond bond : container.bonds()) {
             if (isDoubleBond(bond)) map.add(bond);
         }
 
         Set<IAtom> visited = new HashSet<IAtom>(n);
 
         // find atoms which are connected between two double bonds
         for (IAtom a : map.atoms()) {
 
             List<IBond> bonds = map.bonds(a);
             if (bonds.size() == 2) {
 
                 // (s)tart/(e)nd of cumulated system: -s=a=e-
                 IAtom s = bonds.get(0).getOther(a);
                 IAtom e = bonds.get(1).getOther(a);
                 // need the parents to re-use the double bond encoder
                 IAtom sParent = a;
                 IAtom eParent = a;
 
                 visited.add(a);
                 visited.add(s);
                 visited.add(e);
 
                 int size = 2;
 
                 // expand out from 'l'
                 while (s != null && map.cumulated(s)) {
                     IAtom p = map.bonds(s).get(0).getOther(s);
                     IAtom q = map.bonds(s).get(1).getOther(s);
                     sParent = s;
                     s = visited.add(p) ? p : visited.add(q) ? q : null;
                     size++;
                 }
 
                 // expand from 'r'
                 while (e != null && map.cumulated(e)) {
                     IAtom p = map.bonds(e).get(0).getOther(e);
                     IAtom q = map.bonds(e).get(1).getOther(e);
                     eParent = e;
                     e = visited.add(p) ? p : visited.add(q) ? q : null;
                     size++;
                 }
 
                 // s and e are null if we had a cumulative cycle...
                 if (s != null && e != null) {
 
                     // system has now be expanded, size is the number of double
                     // bonds. For odd numbers we use E/Z whilst for even are
                     // axial M/P.
                     //  \           /
                     //   s = = = = e
                     //  /           \
                     if (isOdd(size)) {
                         StereoEncoder encoder = GeometricDoubleBondEncoderFactory.newEncoder(container, s, sParent, e,
                                 eParent, graph);
                         if (encoder != null) {
                             encoders.add(encoder);
                         }
                     } else {
                         StereoEncoder encoder = axialEncoder(container, s, e);
                         if (encoder != null) {
                             encoders.add(encoder);
                         }
                     }
                 }
             }
         }
 
         return encoders.isEmpty() ? StereoEncoder.EMPTY : new MultiStereoEncoder(encoders);
     }
 
     /**
      * Create an encoder for axial 2D stereochemistry for the given start and
      * end atoms.
      *
      * @param container the molecule
      * @param start     start of the cumulated system
      * @param end       end of the cumulated system
      * @return an encoder or null if there are no coordinated
      */
     static StereoEncoder axialEncoder(IAtomContainer container, IAtom start, IAtom end) {
 
         List<IBond> startBonds = container.getConnectedBondsList(start);
         List<IBond> endBonds = container.getConnectedBondsList(end);
 
         if (startBonds.size() < 2 || endBonds.size() < 2) return null;
 
         if (has2DCoordinates(startBonds) && has2DCoordinates(endBonds)) {
             return axial2DEncoder(container, start, startBonds, end, endBonds);
         } else if (has3DCoordinates(startBonds) && has3DCoordinates(endBonds)) {
             return axial3DEncoder(container, start, startBonds, end, endBonds);
         }
 
         return null;
     }
 
     /**
      * Create an encoder for axial 2D stereochemistry for the given start and
      * end atoms.
      *
      * @param container  the molecule
      * @param start      start of the cumulated system
      * @param startBonds bonds connected to the start
      * @param end        end of the cumulated system
      * @param endBonds   bonds connected to the end
      * @return an encoder
      */
     private static StereoEncoder axial2DEncoder(IAtomContainer container, IAtom start, List<IBond> startBonds,
             IAtom end, List<IBond> endBonds) {
 
         Point2d[] ps = new Point2d[4];
         int[] es = new int[4];
 
         PermutationParity perm = new CombinedPermutationParity(fill2DCoordinates(container, start, startBonds, ps, es,
                 0), fill2DCoordinates(container, end, endBonds, ps, es, 2));
 
         GeometricParity geom = new Tetrahedral2DParity(ps, es);
 
         int u = container.indexOf(start);
         int v = container.indexOf(end);
 
         return new GeometryEncoder(new int[]{u, v}, perm, geom);
     }
 
 
/** For the indicated start and target atoms, create an encoder for axial 3D stereochemistry */
 private static StereoEncoder axial3DEncoder(IAtomContainer container, IAtom start, List<IBond> startBonds, IAtom end, List<IBond> endBonds){
     
      Point3d[] ps = new Point3d[4];
      int[] es = new int[4];
     
      PermutationParity perm = new CombinedPermutationParity(fill3DCoordinates(container, start, startBonds, ps, es,
                 0), fill3DCoordinates(container, end, endBonds, ps, es, 2));
     
      GeometricParity geom = new Tetrahedral3DParity(ps, es);
     
      int u = container.indexOf(start);
      int v = container.indexOf(end);
     
      return new GeometryEncoder(new int[]{u, v}, perm, geom);
     }
     
      /**
        * Create an encoder for axial 2D stereochemistry for the given start and
        * end atoms.
        *
        * @param container  the molecule
        * @param start      start of the cumulated system
        * @param startBonds bonds connected to the start
        * @param end        end of the cumulated system
        * @param endBonds   bonds connected to the end
        * @return an encoder
        */
      private static StereoEncoder axial3DEncoder(IAtomContainer container, IAtom start, List<IBond> startBonds,
                 IAtom end, List<IBond> endBonds) {
     
            Point3d[] ps = new Point3d[4];
            int[] es = new int[4];
     
            PermutationParity perm = new CombinedPermutationParity(fill3DCoordinates(container, start, startBonds, ps, es,
                  0), fill3DCoordinates(container, end, endBonds, ps, es, 2));
     
            GeometricParity geom = new Tetrahedral3DParity(ps, es);
     
            int u = container.indexOf(start);
            int v = container.indexOf(end);
     
            return new GeometryEncoder(new int[]{u, v}, perm, geom);
      }
     
      /**
        * Create            
 }

 

}