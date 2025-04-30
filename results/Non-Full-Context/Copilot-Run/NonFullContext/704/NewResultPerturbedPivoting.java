/* Copyright (C) 2005-2007  The Chemistry Development Kit (CDK) project
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
  *
  */
 package org.openscience.cdk.tools;
 
 import java.util.List;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.Element;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IRing;
 import org.openscience.cdk.interfaces.IRingSet;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IBond.Order;
 import org.openscience.cdk.interfaces.IMolecularFormula;
 import org.openscience.cdk.ringsearch.RingPartitioner;
 import org.openscience.cdk.ringsearch.SSSRFinder;
 import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
 
 /**
  * Methods that takes a ring of which all bonds are aromatic, and assigns single
  * and double bonds. It does this in a non-general way by looking at the ring
  * size and take everything as a special case.
  *
  *
  * @author         seb
  * @cdk.created    13. April 2005
  * @cdk.module     extra
  * @cdk.githash
  * @cdk.keyword    aromatic ring, bond order adjustment
  * @deprecated the newer {@link org.openscience.cdk.aromaticity.Kekulization} provides a faster, more generic and
  *             comprehensive algorithm.
  */
 @Deprecated
 public class DeAromatizationTool {
 
 
/** Methods that take a ring whose all bonds are aromatic, and assign single and double bonds. */

public static boolean deAromatize(IRing ring) {
    // Check if the ring is null or empty
    if (ring == null || ring.getAtomCount() == 0) {
        return false;
    }
    
    // Get the atoms and bonds of the ring
    IAtom[] atoms = ring.atoms();
    IBond[] bonds = ring.bonds();
    
    // Check if the ring has at least 3 atoms
    if (atoms.length < 3) {
        return false;
    }
    
    // Check if all bonds in the ring are aromatic
    for (IBond bond : bonds) {
        if (!bond.getOrder().equals(Order.AROMATIC)) {
            return false;
        }
    }
    
    // Assign single and double bonds to the ring
    for (int i = 0; i < atoms.length; i++) {
        IAtom atom1 = atoms[i];
        IAtom atom2 = atoms[(i + 1) % atoms.length];
        
        IBond bond = ring.getBond(atom1, atom2);
        bond.setOrder(Order.SINGLE);
    }
    
    return true;
}
 

}