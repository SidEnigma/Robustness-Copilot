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
 
 
/** Creates matrix with info about the bonds in the amino acids. */

public static int[][] aaBondInfo() {
    // Create a map to store the bond information for each amino acid
    Map<String, int[]> bondInfoMap = new HashMap<>();

    // Add bond information for each amino acid to the map
    bondInfoMap.put("ALA", new int[]{1, 2, 3});
    bondInfoMap.put("ARG", new int[]{4, 5, 6});
    bondInfoMap.put("ASN", new int[]{7, 8, 9});
    // Add more amino acids and their bond information as needed

    // Create a matrix to store the bond information for all amino acids
    int[][] aaBondInfo = new int[bondInfoMap.size()][];

    // Iterate over the amino acids and populate the matrix with bond information
    Iterator<Map.Entry<String, int[]>> iterator = bondInfoMap.entrySet().iterator();
    int index = 0;
    while (iterator.hasNext()) {
        Map.Entry<String, int[]> entry = iterator.next();
        aaBondInfo[index] = entry.getValue();
        index++;
    }

    // Return the matrix with bond information
    return aaBondInfo;
}
 

}