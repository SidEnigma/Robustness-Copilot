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
 
 
/** You can compare two classes and return a different result. */

public static String diff(IChemObject first, IChemObject second) {
    // Implementation logic goes here

    // Example implementation:
    if (first instanceof IAtom && second instanceof IAtom) {
        IAtom atom1 = (IAtom) first;
        IAtom atom2 = (IAtom) second;

        // Compare atom properties and return the difference as a string
        if (!atom1.getSymbol().equals(atom2.getSymbol())) {
            return "Symbol difference: " + atom1.getSymbol() + " vs " + atom2.getSymbol();
        } else if (atom1.getAtomicNumber() != atom2.getAtomicNumber()) {
            return "Atomic number difference: " + atom1.getAtomicNumber() + " vs " + atom2.getAtomicNumber();
        } else {
            return "No difference";
        }
    } else {
        return "Cannot compare different types of objects";
    }
}
}
