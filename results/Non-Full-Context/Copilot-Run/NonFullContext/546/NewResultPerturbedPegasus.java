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
 
 
/** The matrix has information about the bonds in the amino acids. */

public static int[][] aaBondInfo() {
    int[][] bondInfo = new int[20][20];
    
    try {
        // Read the amino acid data from a ChemFile
        IChemFile chemFile = ChemFileManipulator.readChemFileFromReader(new CMLReader(AminoAcids.class.getResourceAsStream("/data/aminoacids.cml")));
        
        // Get the list of amino acids from the ChemFile
        List<IAminoAcid> aminoAcids = ChemFileManipulator.getAllAminoAcids(chemFile);
        
        // Iterate over the amino acids
        for (IAminoAcid aminoAcid : aminoAcids) {
            // Get the atom container for the amino acid
            IAtomContainer atomContainer = AminoAcidManipulator.getAtomContainer(aminoAcid);
            
            // Get the bonds in the amino acid
            List<IBond> bonds = atomContainer.bonds();
            
            // Iterate over the bonds
            for (IBond bond : bonds) {
                // Get the atoms involved in the bond
                IAtom atom1 = bond.getAtom(0);
                IAtom atom2 = bond.getAtom(1);
                
                // Get the atom indices
                int index1 = atomContainer.indexOf(atom1);
                int index2 = atomContainer.indexOf(atom2);
                
                // Update the bondInfo matrix
                bondInfo[index1][index2]++;
                bondInfo[index2][index1]++;
            }
        }
    } catch (IOException | CDKException e) {
        LOGGER.error("Error reading amino acid data", e);
    }
    
    return bondInfo;
}
 

}