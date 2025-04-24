/* Copyright (C) 2005-2007  Martin Eklund <martin.eklund@farmbio.uu.se>
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
 package org.openscience.cdk.templates;
 
 import java.io.IOException;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 import org.openscience.cdk.AminoAcid;
 import org.openscience.cdk.ChemFile;
 import org.openscience.cdk.aromaticity.Aromaticity;
 import org.openscience.cdk.aromaticity.ElectronDonation;
 import org.openscience.cdk.dict.DictRef;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.graph.Cycles;
 import org.openscience.cdk.interfaces.IAminoAcid;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemFile;
 import org.openscience.cdk.io.CMLReader;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.AminoAcidManipulator;
 import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
 import org.openscience.cdk.tools.manipulator.ChemFileManipulator;
 
 /**
  * Tool that provides templates for the (natural) amino acids.
  *
  * @author Martin Eklund &lt;martin.eklund@farmbio.uu.se&gt;
  * @cdk.module  pdb
  * @cdk.githash
  * @cdk.keyword templates
  * @cdk.keyword amino acids, stuctures
  * @cdk.created 2005-02-08
  */
 public class AminoAcids {
 
     private static final ILoggingTool LOGGER = LoggingToolFactory.createLoggingTool(AminoAcids.class);
 
 
/** Using the information conveyed in the bonds creates and returns a matrix. */

public static int[][] aaBondInfo() {
    // Your implementation logic goes here
    
    // Create a matrix to store the bond information
    int[][] bondMatrix = null;
    
    try {
        // Read the chemical file
        IChemFile chemFile = new ChemFile();
        CMLReader reader = new CMLReader(chemFile);
        reader.read("path/to/chemical/file.cml");
        
        // Get the atom container from the chemical file
        IAtomContainer atomContainer = ChemFileManipulator.getAllAtomContainers(chemFile).get(0);
        
        // Get the bonds from the atom container
        List<IBond> bonds = atomContainer.bonds();
        
        // Create a map to store the bond indices
        Map<IBond, Integer> bondIndices = new HashMap<>();
        
        // Assign indices to the bonds
        int index = 0;
        for (IBond bond : bonds) {
            bondIndices.put(bond, index);
            index++;
        }
        
        // Initialize the bond matrix
        bondMatrix = new int[bonds.size()][bonds.size()];
        
        // Populate the bond matrix
        for (IBond bond : bonds) {
            int bondIndex = bondIndices.get(bond);
            
            // Get the atoms involved in the bond
            IAtom atom1 = bond.getAtom(0);
            IAtom atom2 = bond.getAtom(1);
            
            // Get the indices of the atoms
            int atomIndex1 = atomContainer.indexOf(atom1);
            int atomIndex2 = atomContainer.indexOf(atom2);
            
            // Set the bond information in the matrix
            bondMatrix[atomIndex1][atomIndex2] = bondIndex;
            bondMatrix[atomIndex2][atomIndex1] = bondIndex;
        }
    } catch (IOException | CDKException e) {
        LOGGER.error("Error reading chemical file: " + e.getMessage());
    }
    
    return bondMatrix;
}
 

}