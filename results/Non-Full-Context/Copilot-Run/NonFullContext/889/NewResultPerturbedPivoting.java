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
 
 package org.openscience.cdk.layout;
 
 import org.openscience.cdk.BondRef;
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.geometry.GeometryUtil;
 import org.openscience.cdk.graph.GraphUtil;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IDoubleBondStereochemistry;
 import org.openscience.cdk.interfaces.IStereoElement;
 import org.openscience.cdk.interfaces.ITetrahedralChirality;
 import org.openscience.cdk.ringsearch.RingSearch;
 import org.openscience.cdk.stereo.Atropisomeric;
 import org.openscience.cdk.stereo.ExtendedTetrahedral;
 import org.openscience.cdk.stereo.Octahedral;
 import org.openscience.cdk.stereo.SquarePlanar;
 import org.openscience.cdk.stereo.TrigonalBipyramidal;
 import org.openscience.cdk.tools.LoggingToolFactory;
 
 import javax.vecmath.Point2d;
 import javax.vecmath.Vector2d;
 import java.util.ArrayDeque;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Comparator;
 import java.util.Deque;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Objects;
 import java.util.Set;
 
 import static org.openscience.cdk.interfaces.IBond.Order.DOUBLE;
 import static org.openscience.cdk.interfaces.IBond.Order.SINGLE;
 import static org.openscience.cdk.interfaces.IBond.Stereo.DOWN;
 import static org.openscience.cdk.interfaces.IBond.Stereo.DOWN_INVERTED;
 import static org.openscience.cdk.interfaces.IBond.Stereo.E_OR_Z;
 import static org.openscience.cdk.interfaces.IBond.Stereo.NONE;
 import static org.openscience.cdk.interfaces.IBond.Stereo.UP;
 import static org.openscience.cdk.interfaces.IBond.Stereo.UP_INVERTED;
 import static org.openscience.cdk.interfaces.IBond.Stereo.UP_OR_DOWN;
 import static org.openscience.cdk.interfaces.IBond.Stereo.UP_OR_DOWN_INVERTED;
 
 /**
  * Assigns non-planar labels (wedge/hatch) to the tetrahedral and extended tetrahedral
  * stereocentres in a 2D depiction. Labels are assigned to atoms using the following priority. <ol> <li>bond to non-stereo atoms</li> <li>acyclic
  * bonds</li> <li>bonds to atoms with lower degree (i.e. terminal)</li> <li>lower atomic number</li>
  * </ol>
  *
  * Unspecified bonds are also marked.
  *
  * @author John May
  * @cdk.module sdg
  */
 final class NonplanarBonds {
 
     /** The structure we are assigning labels to. */
     private final IAtomContainer container;
 
     /** Adjacency list graph representation of the structure. */
     private final int[][] graph;
 
     /** Search for cyclic atoms. */
     private final RingSearch ringSearch;
 
     /** Tetrahedral elements indexed by central atom. */
     private final ITetrahedralChirality[] tetrahedralElements;
 
     /** Double-bond elements indexed by end atoms. */
     private final IDoubleBondStereochemistry[] doubleBondElements;
 
     /** Lookup atom index (avoid IAtomContainer). */
     private final Map<IAtom, Integer> atomToIndex;
 
     /** Quick lookup of a bond give the atom index of it's atoms. */
     private final GraphUtil.EdgeToBondMap edgeToBond;
 
     /**
      * Assign non-planar, up and down labels to indicate tetrahedral configuration. Currently all
      * existing directional labels are removed before assigning new labels.
      *
      * @param container the structure to assign labels to
      * @return a container with assigned labels (currently the same as the input)
      * @throws IllegalArgumentException an atom had no 2D coordinates or labels could not be
      *                                  assigned to a tetrahedral centre
      */
     public static IAtomContainer assign(final IAtomContainer container) {
         GraphUtil.EdgeToBondMap edgeToBond = GraphUtil.EdgeToBondMap.withSpaceFor(container);
         new NonplanarBonds(container, GraphUtil.toAdjList(container, edgeToBond), edgeToBond);
         return container;
     }
 
     /**
      * Assign non-planar bonds to the tetrahedral stereocenters in the {@code container}.
      *
      * @param container structure
      * @param g         graph adjacency list representation
      * @throws IllegalArgumentException an atom had no 2D coordinates or labels could not be
      *                                  assigned to a tetrahedral centre
      */
     NonplanarBonds(final IAtomContainer container, final int[][] g, final GraphUtil.EdgeToBondMap edgeToBond) {
 
         this.container = container;
         this.tetrahedralElements = new ITetrahedralChirality[container.getAtomCount()];
         this.doubleBondElements = new IDoubleBondStereochemistry[container.getAtomCount()];
         this.graph = g;
         this.atomToIndex = new HashMap<>(2*container.getAtomCount());
         this.edgeToBond = edgeToBond;
         this.ringSearch = new RingSearch(container, graph);
 
         // clear existing up/down labels to avoid collision, this isn't strictly
         // needed if the atom positions weren't adjusted but we can't guarantee
         // that so it's safe to clear them
         for (IBond bond : container.bonds()) {
             switch (bond.getStereo()) {
                 case UP:
                 case UP_INVERTED:
                 case DOWN:
                 case DOWN_INVERTED:
                     bond.setStereo(NONE);
             }
         }
 
         for (int i = 0; i < container.getAtomCount(); i++) {
             IAtom atom = container.getAtom(i);
             atomToIndex.put(atom, i);
             if (atom.getPoint2d() == null)
                 throw new IllegalArgumentException("atom " + i + " had unset coordinates");
         }
 
         // index the tetrahedral elements by their focus
         Integer[] foci = new Integer[container.getAtomCount()];
         int n = 0;
         for (IStereoElement element : container.stereoElements()) {
             if (element instanceof ITetrahedralChirality) {
                 ITetrahedralChirality tc = (ITetrahedralChirality) element;
                 int focus = atomToIndex.get(tc.getChiralAtom());
                 tetrahedralElements[focus] = tc;
                 foci[n++] = focus;
             }
             else if (element instanceof IDoubleBondStereochemistry) {
                 IBond doubleBond = ((IDoubleBondStereochemistry) element).getStereoBond();
                 doubleBondElements[atomToIndex.get(doubleBond.getBegin())] =
                         doubleBondElements[atomToIndex.get(doubleBond.getEnd())] = (IDoubleBondStereochemistry) element;
             }
         }
 
         // prioritise to highly-congested tetrahedral centres first
         Arrays.sort(foci, 0, n, new Comparator<Integer>() {
 
             @Override
             public int compare(Integer i, Integer j) {
                 return -Integer.compare(nAdjacentCentres(i), nAdjacentCentres(j));
             }
         });
 
         // Tetrahedral labels
         for (int i = 0; i < n; i++) {
             label(tetrahedralElements[foci[i]]);
         }
 
         // Rarer types of stereo
         for (IStereoElement se : container.stereoElements()) {
             if (se instanceof ExtendedTetrahedral) {
                 label((ExtendedTetrahedral) se);
             } else if (se instanceof Atropisomeric) {
                 label((Atropisomeric) se);
             } else if (se instanceof SquarePlanar) {
                 modifyAndLabel((SquarePlanar) se);
             } else if (se instanceof TrigonalBipyramidal) {
                 modifyAndLabel((TrigonalBipyramidal) se);
             } else if (se instanceof Octahedral) {
                 modifyAndLabel((Octahedral) se);
             }
         }
 
         // Unspecified double bond, indicated with an up/down wavy bond
         for (IBond bond : findUnspecifiedDoubleBonds(g)) {
             labelUnspecified(bond);
         }
     }
 
     private void rotate(Point2d p, Point2d pivot, double cos, double sin) {
         double x = p.x - pivot.x;
         double y = p.y - pivot.y;
         double nx = x * cos + y * sin;
         double ny = -x * sin + y * cos;
         p.x = nx + pivot.x;
         p.y = ny + pivot.y;
     }
 
     private Point2d getRotated(Point2d org, Point2d piviot, double theta) {
         Point2d cpy = new Point2d(org);
         rotate(cpy, piviot, Math.cos(theta), Math.sin(theta));
         return cpy;
     }
 
     // moves multiple bonds into angles
     private boolean snapBondsToPosition(IAtom beg, List<IBond> bonds, double ... angles) {
         Point2d p       = beg.getPoint2d();
         Point2d ref     = new Point2d(p.x, p.y+1);
         if (angles.length != bonds.size())
             throw new IllegalArgumentException();
         boolean res = true;
         for (int i = 0; i < bonds.size(); i++) {
             if (!snapBondToPosition(beg, bonds.get(i), getRotated(ref, p, Math.toRadians(angles[i]))))
                 res = false;
         }
         return res;
     }
 
     // tP=target point
     private boolean snapBondToPosition(IAtom beg, IBond bond, Point2d tP) {
         IAtom    end = bond.getOther(beg);
         Point2d  bP = beg.getPoint2d();
         Point2d  eP = end.getPoint2d();
         Vector2d curr = new Vector2d(eP.x-bP.x, eP.y-bP.y);
         Vector2d dest = new Vector2d(tP.x-bP.x, tP.y-bP.y);
 
         double theta = Math.atan2(curr.y, curr.x) - Math.atan2(dest.y, dest.x);
 
         double sin = Math.sin(theta);
         double cos = Math.cos(theta);
 
         // if the bond is already visited it is in a ring with another atom, if it's
         // spiro with the central atom (beg) we may be able to flip into position
         if (bond.getFlag(CDKConstants.VISITED)) {
 
             curr.normalize();
             dest.normalize();
 
             // close enough, give it a little bump to be perfect
             double dot = curr.dot(dest);
             if (dot >= 0.97) {
                 rotate(end.getPoint2d(), bP, cos, sin);
                 return true;
             }
 
             Map<IAtom,Integer> visit = new HashMap<>();
             visit.put(beg, 1);
             floodFill(visit, end, 1);
             IBond reflectBond = null;
             for (IAtom atom : visit.keySet()) {
                 IBond tmp = atom.getBond(beg);
                 if (tmp != null && BondRef.deref(tmp) != BondRef.deref(bond)) {
                     if (reflectBond != null)
                         return false; // not spiro...
                     reflectBond = tmp;
                 }
             }
 
             // should not be possible but if so just indicate we should rollback
             if (reflectBond == null)
                 return false;
 
             // reflect the atoms we collected around the other bond
             GeometryUtil.reflect(visit.keySet(),
                                  reflectBond.getBegin().getPoint2d(),
                                  reflectBond.getEnd().getPoint2d());
 
             curr = new Vector2d(eP.x-bP.x, eP.y-bP.y);
             curr.normalize();
 
             // did we get close?
             double newdot = curr.dot(dest);
             boolean okay = newdot >= 0.97;
 
             // hard snap to expected position
             if (newdot > dot) {
                 theta = Math.atan2(curr.y, curr.x) - Math.atan2(dest.y, dest.x);
                 rotate(end.getPoint2d(), bP, Math.cos(theta), Math.sin(theta));
             } else if (newdot < dot) {
                 // reflect it back then snap
                 GeometryUtil.reflect(visit.keySet(),
                                      reflectBond.getBegin().getPoint2d(),
                                      reflectBond.getEnd().getPoint2d());
                 rotate(end.getPoint2d(), bP, Math.cos(theta), Math.sin(theta));
             }
 
             return okay;
         }
 
         beg.setFlag(CDKConstants.VISITED, true);
         bond.setFlag(CDKConstants.VISITED, true);
         Deque<IAtom> queue = new ArrayDeque<>();
         queue.add(end);
         while (!queue.isEmpty()) {
             IAtom atom = queue.poll();
             if (!atom.getFlag(CDKConstants.VISITED)) {
                 rotate(atom.getPoint2d(), bP, cos, sin);
                 atom.setFlag(CDKConstants.VISITED, true);
                 for (IBond b : container.getConnectedBondsList(atom))
                     if (!b.getFlag(CDKConstants.VISITED)) {
                         queue.add(b.getOther(atom));
                         b.setFlag(CDKConstants.VISITED, true);
                     }
             }
         }
         return true;
     }
 
     private void floodFill(Map<IAtom,Integer> visit, IAtom beg, int num) {
         Deque<IAtom> queue = new ArrayDeque<>();
         visit.put(beg, num);
         queue.add(beg);
         while (!queue.isEmpty()) {
             IAtom atm = queue.poll();
             visit.put(atm, num);
             for (IBond bnd : atm.bonds()) {
                 IAtom nbr = bnd.getOther(atm);
                 if (visit.get(nbr) == null)
                     queue.add(nbr);
             }
         }
     }
 
     private void modifyAndLabel(SquarePlanar se) {
         IAtom       focus = se.getFocus();
         List<IAtom> atoms = se.normalize().getCarriers();
         List<IBond> bonds = new ArrayList<>(4);
 
         int                 rcount = 0;
         Map<IAtom, Integer> rmap   = new HashMap<>();
         List<Integer>       rnums  = new ArrayList<>(4);
         rmap.put(focus, 0);
 
         for (IAtom atom : atoms) {
             IBond bond = container.getBond(se.getFocus(), atom);
             if (bond.isInRing()) {
                 if (!rmap.containsKey(atom))
                     floodFill(rmap, atom, ++rcount);
                 rnums.add(rmap.get(atom));
             } else
                 rnums.add(0);
             bonds.add(bond);
         }
 
         if (rcount > 0 &&
                 checkAndHandleRingSystems(bonds, rnums) == SPIRO_REJECT)
             return;
 
         for (IAtom atom : container.atoms())
             atom.setFlag(CDKConstants.VISITED, false);
         for (IBond bond : container.bonds())
             bond.setFlag(CDKConstants.VISITED, false);
 
         snapBondsToPosition(focus, bonds, -60, 60, 120, -120);
         setBondDisplay(bonds.get(0), focus, DOWN);
         setBondDisplay(bonds.get(1), focus, DOWN);
         setBondDisplay(bonds.get(2), focus, UP);
         setBondDisplay(bonds.get(3), focus, UP);
     }
 
     private boolean doMirror(List<IAtom> atoms) {
         int p = 1;
         for (int i = 0; i < atoms.size(); i++) {
             IAtom a = atoms.get(i);
             for (int j = i+1; j < atoms.size(); j++) {
                 IAtom b = atoms.get(j);
                 if (a.getAtomicNumber() > b.getAtomicNumber())
                     p *= -1;
             }
         }
         return p < 0;
     }
 
     private void modifyAndLabel(TrigonalBipyramidal se) {
 
         IAtom       focus = se.getFocus();
         List<IAtom> atoms = se.normalize().getCarriers();
         List<IBond> bonds = new ArrayList<>(5);
 
         int                 rcount = 0;
         Map<IAtom, Integer> rmap   = new HashMap<>();
         List<Integer>       rnums  = new ArrayList<>(4);
         rmap.put(focus, 0);
 
         for (IAtom atom : atoms) {
             IBond bond = container.getBond(se.getFocus(), atom);
             if (bond.isInRing()) {
                 if (!rmap.containsKey(atom))
                     floodFill(rmap, atom, ++rcount);
                 rnums.add(rmap.get(atom));
             } else
                 rnums.add(0);
             bonds.add(bond);
         }
 
         int res = SPIRO_ACCEPT;
         if (rcount > 0 && (res = checkAndHandleRingSystems(bonds, rnums)) == SPIRO_REJECT)
             return;
 
         for (IAtom atom : container.atoms())
             atom.setFlag(CDKConstants.VISITED, false);
         for (IBond bond : container.bonds())
             bond.setFlag(CDKConstants.VISITED, false);
 
         // Optional but have a look at the equatorial ligands
         // and maybe invert the image based on the permutation
         // parity of their atomic numbers.
         boolean mirror = res == SPIRO_MIRROR || rcount == 0 && doMirror(atoms.subList(1,4));
 
         if (mirror) {
             snapBondsToPosition(focus, bonds, 0, -60, 90, -120, 180);
         } else {
             snapBondsToPosition(focus, bonds, 0, 60, -90, 120, 180);
         }
         setBondDisplay(bonds.get(1), focus, DOWN);
         setBondDisplay(bonds.get(3), focus, UP);
     }
 
     private void modifyAndLabel(Octahedral oc) {
         IAtom       focus = oc.getFocus();
         List<IAtom> atoms = oc.normalize().getCarriers();
         List<IBond> bonds = new ArrayList<>(6);
 
         // determine which ring sets our bonds are in, if they are in spiro
         // we can shuffle around, if more complex we can't
         int                 rcount = 0;
         Map<IAtom, Integer> rmap   = new HashMap<>();
         rmap.put(focus, 0);
         List<Integer>       rnums  = new ArrayList<>(6);
 
         double blen = 0;
         for (IAtom atom : atoms) {
             IBond bond = container.getBond(oc.getFocus(), atom);
             if (bond.isInRing()) {
                 if (!rmap.containsKey(atom))
                     floodFill(rmap, atom, ++rcount);
                 rnums.add(rmap.get(atom));
             } else {
                 rnums.add(0);
             }
             bonds.add(bond);
             blen += GeometryUtil.getLength2D(bond);
         }
 
         int res = SPIRO_ACCEPT;
         if (rcount > 0 &&
             (res = checkAndHandleRingSystems(bonds, rnums)) == SPIRO_REJECT)
             return;
 
         for (IAtom atom : container.atoms())
             atom.setFlag(CDKConstants.VISITED, false);
         for (IBond bond : container.bonds())
             bond.setFlag(CDKConstants.VISITED, false);
 
         if (res == SPIRO_MIRROR) {
             snapBondsToPosition(focus, bonds, 0, -60, 60, 120, -120, 180);
         } else {
             snapBondsToPosition(focus, bonds, 0, 60, -60, -120, 120, 180);
         }
 
         setBondDisplay(bonds.get(1), focus, DOWN);
         setBondDisplay(bonds.get(2), focus, DOWN);
         setBondDisplay(bonds.get(3), focus, UP);
         setBondDisplay(bonds.get(4), focus, UP);
     }
 
 
     private static int SPIRO_REJECT = 0;
     private static int SPIRO_ACCEPT = 1;
     private static int SPIRO_MIRROR = 2;
 
     /**
      * This is complicated, we have a set of bonds and the rings they belong to. We move the bonds around such that
      * we can depict them in a nice way given the constraints of the geometry.
      *
      * @param bonds the bonds
      * @param rnums the ring membership
      * @return the status
      */
     private int checkAndHandleRingSystems(List<IBond> bonds, List<Integer> rnums) {
 
         if (!isSpiro(rnums))
             return SPIRO_REJECT;
 
         // square planar
         if (bonds.size() == 4) {
 
             // check for trans- pairings which we can't lay out at the moment
             if (rnums.get(0).equals(rnums.get(2)) || rnums.get(1).equals(rnums.get(3)))
                 return SPIRO_REJECT;
 
             // rotate such that there is a spiro (or no rings) in position 1/2 in the plane, these are laid out
             // adjacent so is the only place we can nicely place the spiro
             int rotate;
             if (rnums.get(1).equals(rnums.get(2)))
                 rotate = 0; // don't rotate
             else if (rnums.get(2).equals(rnums.get(3)))
                 rotate = 1;
             else if (rnums.get(3).equals(rnums.get(0)))
                 rotate = 2;
             else
                 rotate = 0;
             for (int i = 0; i < rotate; i++) {
                 rotate(bonds, 0, 4);
                 rotate(rnums, 0, 4);
             }
         }
 
         // TBPY
         if (bonds.size() == 5) {
 
             // check for trans- pairing which we can't lay out at the moment
             if (rnums.get(0) != 0 && rnums.get(0).equals(rnums.get(4)))
                 return SPIRO_REJECT;
 
             // rotate such that there is a spiro (or no rings) in position 1/2 in the plane, these are laid out
             // adjacent so is the only place we can nicely place the spiro
             int rotate;
             if (rnums.get(1) != 0 && rnums.get(1).equals(rnums.get(3)) ||
                 rnums.get(2) == 0 && !rnums.get(1).equals(rnums.get(3)))
                 rotate = 0; // don't rotate
             else if (rnums.get(2) != 0 && rnums.get(2).equals(rnums.get(1)) ||
                      rnums.get(3) == 0 && !rnums.get(2).equals(rnums.get(1)))
                 rotate = 1;
             else if (rnums.get(3) != 0 && rnums.get(3).equals(rnums.get(2)) ||
                      rnums.get(1) == 0 && !rnums.get(3).equals(rnums.get(2)))
                 rotate = 2;
             else
                 rotate = 0;
             for (int i = 0; i < rotate; i++) {
                 rotate(bonds, 1, 3);
                 rotate(rnums, 1, 3);
             }
 
             if ((!rnums.get(0).equals(0) && rnums.get(0).equals(rnums.get(3)) ||
                     (!rnums.get(1).equals(0) && rnums.get(1).equals(rnums.get(4))))) {
                 swap(bonds, 1, 3);
                 swap(rnums, 1, 3);
                 return SPIRO_MIRROR;
             }
         }
 
         // octahedral
         if (bonds.size() == 6) {
 
             // check for trans- pairings which we can't lay out at the moment
             if (rnums.get(0) != 0 && rnums.get(0).equals(rnums.get(5)) ||
                 rnums.get(1) != 0 && rnums.get(1).equals(rnums.get(3)) ||
                 rnums.get(2) != 0 && rnums.get(2).equals(rnums.get(4)))
                 return SPIRO_REJECT;
 
             // rotate such that there is a spiro (or no rings) in position 2/3 in the plane, these are laid out
             // adjacent so is the only place we can nicely place the spiro
             int rotate;
             if (rnums.get(2).equals(rnums.get(3)))
                 rotate = 0; // don't rotate
             else if (rnums.get(3).equals(rnums.get(4)))
                 rotate = 1;
             else if (rnums.get(4).equals(rnums.get(1)))
                 rotate = 2;
             else if (rnums.get(1).equals(rnums.get(2)))
                 rotate = 3;
             else
                 return SPIRO_REJECT;
 
             for (int i = 0; i < rotate; i++) {
                 rotate(bonds, 1, 4);
                 rotate(rnums, 1, 4);
             }
 
             // now check the vertical axis, they should be pair 0,1 and 4,5 since again
             // those are adjacent and allow us to depict nicely
             if ((!rnums.get(0).equals(0) && rnums.get(0).equals(rnums.get(4))) ||
                 (!rnums.get(1).equals(0) && rnums.get(1).equals(rnums.get(5)))) {
                 swap(bonds, 1, 4);
                 swap(rnums, 1, 4);
                 return SPIRO_MIRROR;
             }
 
             return SPIRO_ACCEPT;
         }
 
         return SPIRO_ACCEPT;
     }
 
     /**
      * Ensures out rings are only spiro by inspecting the ring set numbers. This is the case if we have &ge; 3 of a
      * given number in the list.
      * <pre>
      *     [0,0,1,2,1,2] => yes
      *     [0,0,1,0,1,0] => yes - 0 is not in a ring
      *     [0,0,1,1,1,0] => no
      * </pre>
      * @param rnums rnums
      * @return only spiro
      */
     private boolean isSpiro(List<Integer> rnums) {
         // invariant the max number is < the total in the list
         int[] counts = new int[1+rnums.size()];
         for (Integer rnum : rnums) {
             if (rnum != 0 && ++counts[rnum] > 2)
                 return false;
         }
         return true;
     }
 
     private <T> void rotate(List<T> l, int off, int len) {
         for (int i = 0; i < (len-1); i++) {
             swap(l, off + i, off + ((i + 1) % 4));
         }
     }
 
     private <T> void swap(List<T> l, int i, int j) {
          T tmp = l.get(i);
          l.set(i, l.get(j));
          l.set(j, tmp);
     }
 
     private IBond.Stereo flip(IBond.Stereo disp) {
         switch (disp) {
             case UP: return UP_INVERTED;
             case UP_INVERTED: return UP;
             case DOWN: return DOWN_INVERTED;
             case DOWN_INVERTED: return DOWN;
             case UP_OR_DOWN: return UP_OR_DOWN_INVERTED;
             case UP_OR_DOWN_INVERTED: return UP_OR_DOWN;
             default: return disp;
         }
     }
 
     private void setBondDisplay(IBond bond, IAtom focus, IBond.Stereo display) {
         if (bond.getBegin().equals(focus))
             bond.setStereo(display);
         else
             bond.setStereo(flip(display));
     }
 
     /**
      * Find a bond between two possible atoms. For example beg1 - end or
      * beg2 - end.
      * @param beg1 begin 1
      * @param beg2 begin 2
      * @param end end
      * @return the bond (or null if none)
      */
     private IBond findBond(IAtom beg1, IAtom beg2, IAtom end) {
         IBond bond = container.getBond(beg1, end);
         if (bond != null)
             return bond;
         return container.getBond(beg2, end);
     }
 
     /**
      * Sets a wedge bond, because wedges are relative we may need to flip
      * the storage order on the bond.
      *
      * @param bond the bond
      * @param end the expected end atom (fat end of wedge)
      * @param style the wedge style
      */
     private void setWedge(IBond bond, IAtom end, IBond.Stereo style) {
         if (!bond.getEnd().equals(end))
             bond.setAtoms(new IAtom[]{bond.getEnd(), bond.getBegin()});
         bond.setStereo(style);
     }
 
     /**
      * Assign non-planar labels (wedge/hatch) to the bonds of extended
      * tetrahedral elements to correctly represent its stereochemistry.
      *
      * @param element a extended tetrahedral element
      */
     private void label(final ExtendedTetrahedral element) {
 
         final IAtom focus = element.focus();
         final IAtom[] atoms = element.peripherals();
         final IBond[] bonds = new IBond[4];
 
         int p = parity(element.winding());
 
         List<IBond> focusBonds = container.getConnectedBondsList(focus);
 
         if (focusBonds.size() != 2) {
             LoggingToolFactory.createLoggingTool(getClass()).warn(
                     "Non-cumulated carbon presented as the focus of extended tetrahedral stereo configuration");
             return;
         }
 
         IAtom[] terminals = element.findTerminalAtoms(container);
 
         IAtom left  = terminals[0];
         IAtom right = terminals[1];
 
         // some bonds may be null if, this happens when an implicit atom
         // is present and one or more 'atoms' is a terminal atom
         for (int i = 0; i < 4; i++)
             bonds[i] = findBond(left, right, atoms[i]);
 
 
         // find the clockwise ordering (in the plane of the page) by sorting by
         // polar coordinates
         int[] rank = new int[4];
         for (int i = 0; i < 4; i++)
             rank[i] = i;
         p *= sortClockwise(rank, focus, atoms, 4);
 
         // assign all up/down labels to an auxiliary array
         IBond.Stereo[] labels = new IBond.Stereo[4];
         for (int i = 0; i < 4; i++) {
             int v = rank[i];
             p *= -1;
             labels[v] = p > 0 ? UP : DOWN;
         }
 
         int[] priority = new int[]{5, 5, 5, 5};
 
         // set the label for the highest priority and available bonds on one side
         // of the cumulated system, setting both sides doesn't make sense
         int i = 0;
         for (int v : priority(atomToIndex.get(focus), atoms, 4)) {
             IBond bond = bonds[v];
             if (bond == null) continue;
             if (bond.getStereo() == NONE && bond.getOrder() == SINGLE) priority[v] = i++;
         }
 
         // we now check which side was more favourable and assign two labels
         // to that side only
         if (priority[0] + priority[1] < priority[2] + priority[3]) {
             if (priority[0] < 5)
                 setWedge(bonds[0], atoms[0], labels[0]);
             if (priority[1] < 5)
                 setWedge(bonds[1], atoms[1], labels[1]);
         } else {
             if (priority[2] < 5)
                 setWedge(bonds[2], atoms[2], labels[2]);
             if (priority[3] < 5)
                 setWedge(bonds[3], atoms[3], labels[3]);
         }
 
     }
 
     /**
      * Assign non-planar labels (wedge/hatch) to the bonds to
      * atropisomers
      *
      * @param element a extended tetrahedral element
      */
     private void label(final Atropisomeric element) {
 
         final IBond   focus = element.getFocus();
         final IAtom   beg   = focus.getBegin();
         final IAtom   end   = focus.getEnd();
         final IAtom[] atoms = element.getCarriers().toArray(new IAtom[0]);
         final IBond[] bonds = new IBond[4];
 
         int p = 0;
         switch (element.getConfigOrder()) {
             case IStereoElement.LEFT:
                 p = +1;
                 break;
             case IStereoElement.RIGHT:
                 p = -1;
                 break;
         }
 
         // some bonds may be null if, this happens when an implicit atom
         // is present and one or more 'atoms' is a terminal atom
         bonds[0] = container.getBond(beg, atoms[0]);
         bonds[1] = container.getBond(beg, atoms[1]);
         bonds[2] = container.getBond(end, atoms[2]);
         bonds[3] = container.getBond(end, atoms[3]);
 
         // may be back to front?
         if (bonds[0] == null || bonds[1] == null ||
             bonds[2] == null || bonds[3] == null)
             throw new IllegalStateException("Unexpected configuration ordering, beg/end bonds should be in that order.");
 
         // find the clockwise ordering (in the plane of the page) by sorting by
         // polar corodinates
         int[] rank = new int[4];
         for (int i = 0; i < 4; i++)
             rank[i] = i;
 
         IAtom phantom = beg.getBuilder().newAtom();
         phantom.setPoint2d(new Point2d((beg.getPoint2d().x + end.getPoint2d().x) / 2,
                                    (beg.getPoint2d().y + end.getPoint2d().y) / 2));
         p *= sortClockwise(rank, phantom, atoms, 4);
 
         // assign all up/down labels to an auxiliary array
         IBond.Stereo[] labels = new IBond.Stereo[4];
         for (int i = 0; i < 4; i++) {
             int v = rank[i];
             p *= -1;
             labels[v] = p > 0 ? UP : DOWN;
         }
 
         int[] priority = new int[]{5, 5, 5, 5};
 
         // set the label for the highest priority and available bonds on one side
         // of the cumulated system, setting both sides doesn't make sense
         int i = 0;
         for (int v : new int[]{0,1,2,3}) {
             IBond bond = bonds[v];
             if (bond == null) continue;
             if (bond.getStereo() == NONE && bond.getOrder() == SINGLE) priority[v] = i++;
         }
 
         // we now check which side was more favourable and assign two labels
         // to that side only
         if (priority[0] + priority[1] < priority[2] + priority[3]) {
             if (priority[0] < 5) {
                 bonds[0].setAtoms(new IAtom[]{beg, atoms[0]});
                 bonds[0].setStereo(labels[0]);
             }
             if (priority[1] < 5) {
                 bonds[1].setAtoms(new IAtom[]{beg, atoms[1]});
                 bonds[1].setStereo(labels[1]);
             }
         } else {
             if (priority[2] < 5) {
                 bonds[2].setAtoms(new IAtom[]{end, atoms[2]});
                 bonds[2].setStereo(labels[2]);
             }
             if (priority[3] < 5) {
                 bonds[3].setAtoms(new IAtom[]{end, atoms[3]});
                 bonds[3].setStereo(labels[3]);
             }
         }
 
     }
 
     /**
      * Assign labels to the bonds of tetrahedral element to correctly represent
      * its stereo configuration.
      *
      * @param element a tetrahedral element
      * @throws IllegalArgumentException the labels could not be assigned
      */
     private void label(final ITetrahedralChirality element) {
 
         final IAtom focus = element.getChiralAtom();
         final IAtom[] atoms = element.getLigands();
         final IBond[] bonds = new IBond[4];
 
         int p = parity(element.getStereo());
         int n = 0;
 
         // unspecified centre, no need to assign labels
         if (p == 0) return;
 
         for (int i = 0; i < 4; i++) {
             if (atoms[i].equals(focus)) {
                 p *= indexParity(i); // implicit H, adjust parity
             } else {
                 bonds[n] = container.getBond(focus, atoms[i]);
                 if (bonds[n] == null)
                     throw new IllegalArgumentException("Inconsistent stereo,"
                                                        + " tetrahedral centre"
                                                        + " contained atom not"
                                                        + " stored in molecule");
                 atoms[n] = atoms[i];
                 n++;
             }
         }
 
         // sort coordinates and adjust parity (rank gives us the sorted order)
         int[] rank = new int[n];
         for (int i = 0; i < n; i++)
             rank[i] = i;
         p *= sortClockwise(rank, focus, atoms, n);
 
         // special case when there are three neighbors are acute and an implicit
         // hydrogen is opposite all three neighbors. The central label needs to
         // be inverted, atoms could be laid out like this automatically, consider
         // CC1C[C@H]2CC[C@@H]1C2
         int invert = -1;
         if (n == 3) {
             // find a triangle of non-sequential neighbors (sorted clockwise)
             // which has anti-clockwise winding
             for (int i = 0; i < n; i++) {
                 Point2d a = atoms[rank[i]].getPoint2d();
                 Point2d b = focus.getPoint2d();
                 Point2d c = atoms[rank[(i + 2) % n]].getPoint2d();
                 double det = (a.x - c.x) * (b.y - c.y) - (a.y - c.y) * (b.x - c.x);
                 if (det > 0) {
                     invert = rank[(i + 1) % n];
                     break;
                 }
             }
         }
 
         // assign all up/down labels to an auxiliary array
         IBond.Stereo[] labels = new IBond.Stereo[n];
         for (int i = 0; i < n; i++) {
             int v = rank[i];
 
             // 4 neighbors (invert every other one)
             if (n == 4) p *= -1;
 
             labels[v] = invert == v ? p > 0 ? DOWN : UP : p > 0 ? UP : DOWN;
         }
 
         // set the label for the highest priority and available bond
         IBond.Stereo firstlabel      = null;
         boolean      assignTwoLabels = assignTwoLabels(bonds, labels);
         for (int v : priority(atomToIndex.get(focus), atoms, n)) {
             IBond bond = bonds[v];
             if (bond.getStereo() != NONE || bond.getOrder() != SINGLE)
                 continue;
             // first label
             if (firstlabel == null) {
                 bond.setAtoms(new IAtom[]{focus, atoms[v]}); // avoids UP_INVERTED/DOWN_INVERTED
                 bond.setStereo(labels[v]);
                 firstlabel = labels[v];
                 // don't assign a second label when there are only three ligands
                 if (!assignTwoLabels)
                     break;
             }
             // second label
             else if (labels[v] != firstlabel) {
                 // don't add if it's possibly a stereo-centre
                 if (isSp3Carbon(atoms[v], graph[container.indexOf(atoms[v])].length))
                     break;
                 bond.setAtoms(new IAtom[]{focus, atoms[v]}); // avoids UP_INVERTED/DOWN_INVERTED
                 bond.setStereo(labels[v]);
                 break;
             }
         }
 
         // it should be possible to always assign labels somewhere -> unchecked exception
         if (firstlabel == null)
             throw new IllegalArgumentException("could not assign non-planar (up/down) labels");
     }
 
     private boolean assignTwoLabels(IBond[] bonds, IBond.Stereo[] labels) {
         return labels.length == 4 && countRingBonds(bonds) != 3;
     }
 
     private int countRingBonds(IBond[] bonds) {
         int rbonds = 0;
         for (IBond bond : bonds) {
             if (bond != null && bond.isInRing())
                 rbonds++;
         }
         return rbonds;
     }
 
     /**
      * Obtain the parity of a value x. The parity is -1 if the value is odd or
      * +1 if the value is even.
      *
      * @param x a value
      * @return the parity
      */
     private int indexParity(int x) {
         return (x & 0x1) == 1 ? -1 : +1;
     }
 
     /**
      * Obtain the parity (winding) of a tetrahedral element. The parity is -1
      * for clockwise (odd), +1 for anticlockwise (even) and 0 for unspecified.
      *
      * @param stereo configuration
      * @return the parity
      */
     private int parity(ITetrahedralChirality.Stereo stereo) {
         switch (stereo) {
             case CLOCKWISE:
                 return -1;
             case ANTI_CLOCKWISE:
                 return +1;
             default:
                 return 0;
         }
     }
 
     /**
      * Obtain the number of centres adjacent to the atom at the index, i.
      *
      * @param i atom index
      * @return number of adjacent centres
      */
     private int nAdjacentCentres(int i) {
         int n = 0;
         for (IAtom atom : tetrahedralElements[i].getLigands())
             if (tetrahedralElements[atomToIndex.get(atom)] != null) n++;
         return n;
     }
 
     /**
      * Obtain a prioritised array where the indices 0 to n which correspond to
      * the provided {@code atoms}.
      *
      * @param focus focus of the tetrahedral atom
      * @param atoms the atom
      * @param n     number of atoms
      * @return prioritised indices
      */
     private int[] priority(int focus, IAtom[] atoms, int n) {
         int[] rank = new int[n];
         for (int i = 0; i < n; i++)
             rank[i] = i;
         for (int j = 1; j < n; j++) {
             int v = rank[j];
             int i = j - 1;
             while ((i >= 0) && hasPriority(focus, atomToIndex.get(atoms[v]), atomToIndex.get(atoms[rank[i]]))) {
                 rank[i + 1] = rank[i--];
             }
             rank[i + 1] = v;
         }
         return rank;
     }
 
     // indicates where an atom is a Sp3 carbon and is possibly a stereo-centre
     private boolean isSp3Carbon(IAtom atom, int deg) {
         Integer elem = atom.getAtomicNumber();
         Integer hcnt = atom.getImplicitHydrogenCount();
         if (elem == null || hcnt == null) return false;
         if (elem == 6 && hcnt <= 1 && deg + hcnt == 4) {
             // more expensive check, look one out and see if we have any
             // duplicate terminal neighbors
             List<IAtom> terminals = new ArrayList<>();
             for (IBond bond : container.getConnectedBondsList(atom)) {
                 IAtom nbr = bond.getOther(atom);
                 if (container.getConnectedBondsCount(nbr) == 1) {
                     for (IAtom terminal : terminals) {
                         if (Objects.equals(terminal.getAtomicNumber(),
                                            nbr.getAtomicNumber()) &&
                             Objects.equals(terminal.getMassNumber(),
                                            nbr.getMassNumber()) &&
                             Objects.equals(terminal.getFormalCharge(),
                                            nbr.getFormalCharge()) &&
                             Objects.equals(terminal.getImplicitHydrogenCount(),
                                            nbr.getImplicitHydrogenCount())) {
                             return false;
                         }
                     }
                     terminals.add(nbr);
                 }
             }
             return true;
         }
         return false;
     }
 
 
/** Does the atom with the index {@code i} have priority over the atom at the index {@code j} for the tetrahedral atom {@code focus}. */

private boolean hasPriority(int focus, int i, int j) {
    // Implementation logic goes here
    // Return true if atom i has priority over atom j for the tetrahedral atom focus
    // Return false otherwise
}
 

}