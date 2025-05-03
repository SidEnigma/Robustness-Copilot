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
 
 
/** At the designated bond, nondestructively divide a molecule into two pieces.   It should be noted that the opened ring will appear twice in the resultant list if a ring bond is supplied. */
 protected static List<IAtomContainer> splitMolecule(IAtomContainer atomContainer, IBond bond){
      List<IAtomContainer> result = new ArrayList<IAtomContainer>();
      IAtomContainer mol1 = atomContainer.getBuilder().newInstance(IAtomContainer.class, atomContainer);
      IAtomContainer mol2 = atomContainer.getBuilder().newInstance(IAtomContainer.class, atomContainer);
      IAtom atom1 = bond.getAtom(0);
      IAtom atom2 = bond.getAtom(1);
      mol1.removeAtom(atom2);
      mol2.removeAtom(atom1);
      mol1.removeBond(bond);
      mol2.removeBond(bond);
      result.add(mol1);
      result.add(mol2);
      return result;  
 }

 

}