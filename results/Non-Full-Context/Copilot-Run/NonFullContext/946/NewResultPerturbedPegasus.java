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
 
 
/** A molecule is split into two parts. */

    protected static List<IAtomContainer> splitMolecule(IAtomContainer atomContainer, IBond bond) {
        List<IAtomContainer> fragments = new ArrayList<>();

        // Create two new atom containers for the fragments
        IAtomContainer fragment1 = atomContainer.getBuilder().newInstance(IAtomContainer.class);
        IAtomContainer fragment2 = atomContainer.getBuilder().newInstance(IAtomContainer.class);

        // Get the atoms and bonds from the original molecule
        List<IAtom> atoms = atomContainer.atoms();
        List<IBond> bonds = atomContainer.bonds();

        // Add atoms and bonds to the fragments based on the split bond
        for (IAtom atom : atoms) {
            if (atomContainer.getConnectedBondsCount(atom) > 0) {
                if (atomContainer.getConnectedBonds(atom).contains(bond)) {
                    fragment1.addAtom(atom);
                } else {
                    fragment2.addAtom(atom);
                }
            }
        }

        for (IBond b : bonds) {
            if (b.equals(bond)) {
                fragment1.addBond(b);
            } else {
                fragment2.addBond(b);
            }
        }

        // Add the fragments to the list
        fragments.add(fragment1);
        fragments.add(fragment2);

        return fragments;
    }
 

}