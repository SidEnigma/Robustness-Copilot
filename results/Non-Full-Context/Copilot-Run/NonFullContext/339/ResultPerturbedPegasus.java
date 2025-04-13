/* Copyright (C) 2005-2007  The Chemistry Development Kit (CDK) Project
  *                    2014  Egon Willighagen <egonw@users.sf.net>
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
 
 import org.openscience.cdk.DefaultChemObjectBuilder;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAminoAcid;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IBioPolymer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.interfaces.IStrand;
 import org.openscience.cdk.templates.AminoAcids;
 import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
 
 import java.util.Map;
 
 /**
  * Class that facilitates building protein structures. Building DNA and RNA
  * is done by a complementary class <code>NucleicAcidBuilderTool</code> (to be
  * written).
  *
  * @cdk.module pdb
  * @cdk.githash
  */
 public class ProteinBuilderTool {
 
     private static ILoggingTool logger = LoggingToolFactory.createLoggingTool(ProteinBuilderTool.class);
 
     /**
      * Builds a protein by connecting a new amino acid at the N-terminus of the
      * given strand.
      *
      * @param protein protein to which the strand belongs
      * @param aaToAdd amino acid to add to the strand of the protein
      * @param strand  strand to which the protein is added
      */
     public static IBioPolymer addAminoAcidAtNTerminus(IBioPolymer protein, IAminoAcid aaToAdd, IStrand strand,
             IAminoAcid aaToAddTo) {
         // then add the amino acid
         addAminoAcid(protein, aaToAdd, strand);
         // Now think about the protein back bone connection
         if (protein.getMonomerCount() == 0) {
             // make the connection between that aminoAcid's C-terminus and the
             // protein's N-terminus
             protein.addBond(aaToAdd.getBuilder().newInstance(IBond.class, aaToAddTo.getNTerminus(),
                     aaToAdd.getCTerminus(), IBond.Order.SINGLE));
         } // else : no current N-terminus, so nothing special to do
         return protein;
     }
 
 
/** A new amino acid is connected to the C-terminus of the strand. */
 public static IBioPolymer addAminoAcidAtCTerminus(IBioPolymer protein, IAminoAcid aaToAdd, IStrand strand, IAminoAcid aaToAddTo){
        // then add the amino acid
        addAminoAcid(protein, aaToAdd, strand);
        // Now think about the protein back bone connection
        if (protein.getMonomerCount() == 0) {
            // make the connection between that aminoAcid's C-terminus and the
            // protein's N-terminus
            protein.addBond(aaToAdd.getBuilder().newInstance(IBond.class, aaToAddTo.getCTerminus(),
                    aaToAdd.getNTerminus(), IBond.Order.SINGLE));
        } // else : no current N-terminus, so nothing special to do
        return protein;
    }
    
        /**
        * Builds a protein by connecting a new amino acid at the C-terminus of the
        * given strand.
        *
        * @param protein protein to which the strand belongs
        * @param aaToAdd amino acid to add to the strand of the protein
        * @param strand  strand to which the protein is added
        */
        public static IBioPolymer addAminoAcidAtCTerminus(IBioPolymer protein, IAminoAcid aaToAdd, IStrand strand) {
            // then add the amino acid
            addAminoAcid(protein, aaToAdd, strand);
            // Now think about the protein back bone connection
            if (protein.getMonomerCount() == 0) {
                // make the connection between that aminoAcid's C-terminus and the
                // protein's N-terminus
                protein.addBond(aaToAdd.getBuilder().newInstance(IBond.class, aaToAdd.getCTerminus(),
                        aaToAdd.getNTerminus(), IBond.Order.SINGLE));
            } // else : no current N-terminus, so nothing special to do
            return protein;
        }
    
        /**
        * Builds a protein by connecting a new amino acid at the N-terminus of the
        * given strand.
        *
        * @param protein protein to which the strand belongs
        * @param aaToAdd        
 }

 

}