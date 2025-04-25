/* Copyright (C) 2005-2008  Egon Willighagen <egonw@users.sf.net>
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public License
  * as published by the Free Software Foundation, version 2.1.
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
 package org.openscience.cdk.atomtype;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.openscience.cdk.config.AtomTypeFactory;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomType;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.BondManipulator;
 
 /**
  * AtomTypeMatcher that finds an AtomType by matching the Atom's element symbol.
  * This atom type matcher takes into account formal charge and number of
  * implicit hydrogens, and requires bond orders to be given.
  *
  * <p>This class uses the <b>cdk/config/data/structgen_atomtypes.xml</b>
  * list. If there is not an atom type defined for the tested atom, then null
  * is returned.
  *
  * @author         egonw
  * @cdk.created    2006-09-22
  * @cdk.module     structgen
  * @cdk.githash
  */
 public class StructGenAtomTypeGuesser implements IAtomTypeGuesser {
 
     private static AtomTypeFactory factory = null;
     private static ILoggingTool    logger  = LoggingToolFactory.createLoggingTool(StructGenAtomTypeGuesser.class);
 
     /**
      * Constructor for the StructGenMatcher object.
      */
     public StructGenAtomTypeGuesser() {}
 
 
/** The Atom's element symbol, formal charge and hybridization state are found by this. */

public List<IAtomType> possibleAtomTypes(IAtomContainer atomContainer, IAtom atom) throws CDKException {
    List<IAtomType> possibleTypes = new ArrayList<>();
    
    // Get the Atom's element symbol
    String symbol = atom.getSymbol();
    
    // Get the Atom's formal charge
    int formalCharge = atom.getFormalCharge();
    
    // Get the Atom's hybridization state
    IAtomType.Hybridization hybridization = atom.getHybridization();
    
    // Get the Atom's neighbors
    List<IAtom> neighbors = atomContainer.getConnectedAtomsList(atom);
    
    // Iterate over all possible AtomTypes in the AtomTypeFactory
    for (IAtomType atomType : AtomTypeFactory.getInstance().getAllAtomTypes()) {
        // Check if the AtomType matches the Atom's properties
        if (atomType.getSymbol().equals(symbol) &&
            atomType.getFormalCharge() == formalCharge &&
            atomType.getHybridization() == hybridization) {
            
            // Check if the AtomType matches the Atom's neighbors
            boolean matchesNeighbors = true;
            for (IAtom neighbor : neighbors) {
                IBond bond = atomContainer.getBond(atom, neighbor);
                if (bond != null && !atomType.getBondTypes().contains(bond.getOrder())) {
                    matchesNeighbors = false;
                    break;
                }
            }
            
            // Add the AtomType to the list of possible types if it matches the Atom's neighbors
            if (matchesNeighbors) {
                possibleTypes.add(atomType);
            }
        }
    }
    
    return possibleTypes;
}
 

}