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
 public static int[][] aaBondInfo(){
      int[][] bondInfo = new int[20][20];
      bondInfo[0][0] = 0;
      bondInfo[0][1] = 0;
      bondInfo[0][2] = 0;
      bondInfo[0][3] = 0;
      bondInfo[0][4] = 0;
      bondInfo[0][5] = 0;
      bondInfo[0][6] = 0;
      bondInfo[0][7] = 0;
      bondInfo[0][8] = 0;
      bondInfo[0][9] = 0;
      bondInfo[0][10] = 0;
      bondInfo[0][11] = 0;
      bondInfo[0][12] = 0;
      bondInfo[0][13] = 0;
      bondInfo[0][14] = 0;
      bondInfo[0][15] = 0;
      bondInfo[0][16] = 0;
      bondInfo[0][17] = 0;
      bondInfo[0][18] = 0;
      bondInfo[0][19] = 0;
      bondInfo[1][0] = 0;
      bondInfo[1][1] = 0;
      bondInfo[1][2] = 0;
      bondInfo[1][3] = 0;
      bondInfo[1][4] = 0;
      bondInfo[1][5] = 0;
      bondInfo[1][6] = 0;
      bondInfo[1][7] = 0;
      bondInfo[1][8] = 0;
      bondInfo[1][9] = 0;
      bondInfo[1][10] = 0;
      bondInfo[1][11] = 0;
      bondInfo[1][12] = 0;
      bondInfo[1][13] = 0;
      bondInfo[1][14] = 0;
      bondInfo[1][15] = 0;
      bondInfo[1][16] = 0;
      bondInfo[1][17] = 0;
      bondInfo[1][18] = 0;
      bondInfo[1][19] = 0;
      bondInfo  
 }

 

}