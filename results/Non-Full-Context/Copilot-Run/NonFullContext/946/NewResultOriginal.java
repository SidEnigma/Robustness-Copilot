/* Copyright (C) 2010  Rajarshi Guha <rajarshi.guha@gmail.com>
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
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 package org.openscience.cdk.fragment;
 
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * Helper methods for fragmentation algorithms.
  * 
  * Most of these methods are specific to the fragmentation algorithms
  * in this package and so are protected. In general, these methods will
  * not be used by the rest of the API or by other users of the library.
  *
  * @author Rajarshi Guha
  * @cdk.module fragment
  */
 public class FragmentUtils {
 
 
/** Non destructively split a molecule into two parts at the specified bond. */

    protected static List<IAtomContainer> splitMolecule(IAtomContainer atomContainer, IBond bond) {
        List<IAtomContainer> fragments = new ArrayList<>();

        // Create two new atom containers for the fragments
        IAtomContainer fragment1 = atomContainer.getBuilder().newInstance(IAtomContainer.class);
        IAtomContainer fragment2 = atomContainer.getBuilder().newInstance(IAtomContainer.class);

        // Add atoms and bonds to the fragments based on the split bond
        for (IAtom atom : atomContainer.atoms()) {
            if (atomContainer.getConnectedBondsCount(atom) > 0) {
                if (atomContainer.getConnectedBonds(atom).contains(bond)) {
                    fragment1.addAtom(atom);
                } else {
                    fragment2.addAtom(atom);
                }
            }
        }

        for (IBond moleculeBond : atomContainer.bonds()) {
            if (moleculeBond.equals(bond)) {
                continue;
            }

            IAtom[] bondAtoms = moleculeBond.atoms();
            IAtom atom1 = bondAtoms[0];
            IAtom atom2 = bondAtoms[1];

            if (fragment1.contains(atom1) && fragment1.contains(atom2)) {
                fragment1.addBond(moleculeBond);
            } else if (fragment2.contains(atom1) && fragment2.contains(atom2)) {
                fragment2.addBond(moleculeBond);
            }
        }

        fragments.add(fragment1);
        fragments.add(fragment2);

        return fragments;
    }
 

}