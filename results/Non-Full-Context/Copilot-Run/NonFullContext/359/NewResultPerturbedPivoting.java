/* Copyright (C) 2008  Egon Willighagen <egonw@users.sf.net>
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public License
  * as published by the Free Software Foundation; either version 2.1
  * of the License, or (at your option) any later version.
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
 package org.openscience.cdk.tools.diff;
 
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.tools.diff.tree.ChemObjectDifference;
 import org.openscience.cdk.tools.diff.tree.DoubleDifference;
 import org.openscience.cdk.tools.diff.tree.IDifference;
 import org.openscience.cdk.tools.diff.tree.IntegerDifference;
 import org.openscience.cdk.tools.diff.tree.Point2dDifference;
 import org.openscience.cdk.tools.diff.tree.Point3dDifference;
 
 /**
  * Compares two {@link IAtom} classes.
  *
  * @author     egonw
  * @cdk.module diff
  * @cdk.githash
  */
 public class AtomDiff {
 
     /**
      * Overwrite the default public constructor because this class is not
      * supposed to be instantiated.
      */
     private AtomDiff() {}
 
 
/** Compare two {@link IChemObject} classes and return the difference as a {@link String}. */

public static String diff(IChemObject first, IChemObject second) {
    if (first == null || second == null) {
        throw new IllegalArgumentException("Arguments cannot be null");
    }

    if (!(first instanceof IAtom) || !(second instanceof IAtom)) {
        throw new IllegalArgumentException("Arguments must be of type IAtom");
    }

    IAtom atom1 = (IAtom) first;
    IAtom atom2 = (IAtom) second;

    StringBuilder diffBuilder = new StringBuilder();

    // Compare atom properties
    diffBuilder.append(compareProperty("Atomic Number", atom1.getAtomicNumber(), atom2.getAtomicNumber()));
    diffBuilder.append(compareProperty("Symbol", atom1.getSymbol(), atom2.getSymbol()));
    diffBuilder.append(compareProperty("Mass Number", atom1.getMassNumber(), atom2.getMassNumber()));
    // Add more properties to compare as needed

    return diffBuilder.toString();
}
 

}