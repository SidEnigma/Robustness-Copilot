/* Copyright (C) 2001-2007  The Chemistry Development Kit (CDK) Project
  *
  *  Contact: cdk-devel@lists.sourceforge.net
  *
  *  This program is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU Lesser General Public License
  *  as published by the Free Software Foundation; either version 2.1
  *  of the License, or (at your option) any later version.
  *  All we ask is that proper credit is given for our work, which includes
  *  - but is not limited to - adding the above copyright notice to the beginning
  *  of your source code files, and to any copyright notice that you may distribute
  *  with programs based on this work.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU Lesser General Public License for more details.
  *
  *  You should have received a copy of the GNU Lesser General Public License
  *  along with this program; if not, write to the Free Software
  *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  *
  */
 package org.openscience.cdk.aromaticity;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IRing;
 
 /**
  * @cdk.module standard
  * @cdk.githash
  *
  * @author Oliver Horlacher &lt;oliver.horlacher@therastrat.com&gt;
  * @cdk.created    2002-03-14
  *
  * @cdk.keyword aromaticity detector
  * @deprecated use {@link Aromaticity}
  */
 @Deprecated
 public class AromaticityCalculator {
 
 
/** For aromaticity tests the <code>ring</code> in the <code>molecule</code>. The method returns true if the ring is aromatic false otherwise */

public static boolean isAromatic(IRing ring, IAtomContainer atomContainer) {
    // Check if the ring is null or empty
    if (ring == null || ring.getAtomCount() == 0) {
        return false;
    }
    
    // Check if all atoms in the ring are aromatic
    for (IAtom atom : ring.atoms()) {
        if (!atom.getFlag(CDKConstants.ISAROMATIC)) {
            return false;
        }
    }
    
    // Check if all bonds in the ring are aromatic
    for (IBond bond : ring.bonds()) {
        if (!bond.getFlag(CDKConstants.ISAROMATIC)) {
            return false;
        }
    }
    
    // Check if the ring is fully conjugated
    if (!ring.isFullyConjugated()) {
        return false;
    }
    
    // Check if the ring is planar
    if (!ring.isPlanar()) {
        return false;
    }
    
    // Check if the ring is cyclic
    if (!ring.isCyclic()) {
        return false;
    }
    
    // Check if the ring is a member of the atom container
    if (!atomContainer.contains(ring)) {
        return false;
    }
    
    // All checks passed, the ring is aromatic
    return true;
}
 

}