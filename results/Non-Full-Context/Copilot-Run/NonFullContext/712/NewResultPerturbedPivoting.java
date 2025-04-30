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
 
 import org.openscience.cdk.interfaces.IAtomType;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.tools.diff.tree.AtomTypeHybridizationDifference;
 import org.openscience.cdk.tools.diff.tree.BondOrderDifference;
 import org.openscience.cdk.tools.diff.tree.ChemObjectDifference;
 import org.openscience.cdk.tools.diff.tree.DoubleDifference;
 import org.openscience.cdk.tools.diff.tree.IDifference;
 import org.openscience.cdk.tools.diff.tree.IntegerDifference;
 import org.openscience.cdk.tools.diff.tree.StringDifference;
 
 /**
  * Compares two {@link IAtomType} classes.
  *
  * @author     egonw
  * @cdk.module diff
  * @cdk.githash
  */
 public class AtomTypeDiff {
 
     /**
      * Overwrite the default public constructor because this class is not
      * supposed to be instantiated.
      */
     private AtomTypeDiff() {}
 
     /**
      * Compare two {@link IChemObject} classes and return the difference as a {@link String}.
      *
      * @param first  the first of the two classes to compare
      * @param second the second of the two classes to compare
      * @return a {@link String} representation of the difference between the first and second {@link IChemObject}.
      */
     public static String diff(IChemObject first, IChemObject second) {
         IDifference difference = difference(first, second);
         if (difference == null) {
             return "";
         } else {
             return difference.toString();
         }
     }
 
 
/** Compare two classes {@link IChemObject} and return the difference as a {@link IDifference}. */

public static IDifference difference(IChemObject first, IChemObject second) {
    if (first instanceof IAtomType && second instanceof IAtomType) {
        IAtomType firstAtomType = (IAtomType) first;
        IAtomType secondAtomType = (IAtomType) second;

        // Compare atom type hybridization
        if (firstAtomType.getHybridization() != secondAtomType.getHybridization()) {
            return new AtomTypeHybridizationDifference(firstAtomType.getHybridization(), secondAtomType.getHybridization());
        }

        // Compare bond order
        if (firstAtomType.getBondOrder() != secondAtomType.getBondOrder()) {
            return new BondOrderDifference(firstAtomType.getBondOrder(), secondAtomType.getBondOrder());
        }

        // Compare other properties of IAtomType if needed

        // If no differences found, return null
        return null;
    }

    // If the objects are not of type IAtomType, return a generic ChemObjectDifference
    return new ChemObjectDifference(first, second);
}

 

}