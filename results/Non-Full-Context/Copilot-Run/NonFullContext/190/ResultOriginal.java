/* Copyright (c) 2014  Collaborative Drug Discovery, Inc. <alex@collaborativedrug.com>
  *               2014  Mark B Vine (orcid:0000-0002-7794-0426)
  *
  * Implemented by Alex M. Clark, produced by Collaborative Drug Discovery, Inc.
  * Made available to the CDK community under the terms of the GNU LGPL.
  *
  *    http://collaborativedrug.com
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
  */
 
 package org.openscience.cdk.qsar.descriptors.molecular;
 
 import java.util.ArrayList;
 
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.qsar.DescriptorSpecification;
 import org.openscience.cdk.qsar.DescriptorValue;
 import org.openscience.cdk.qsar.IMolecularDescriptor;
 import org.openscience.cdk.qsar.result.IDescriptorResult;
 import org.openscience.cdk.qsar.result.IntegerArrayResult;
 
 /**
  *
  * Small ring descriptors: these are based on enumeration of all the small rings (sizes 3 to 9) in a molecule,
  * which can be obtained quickly and deterministically.
  *
  * @cdk.module qsarmolecular
  * @cdk.githash
  *
  * @cdk.dictref qsar-descriptors:smallrings
  * @cdk.keyword smallrings
  * @cdk.keyword descriptor
 */
 public class SmallRingDescriptor implements IMolecularDescriptor {
 
     private static final String[] NAMES = {"nSmallRings", // total number of small rings (of size 3 through 9)
             "nAromRings", // total number of small aromatic rings
             "nRingBlocks", // total number of distinct ring blocks
             "nAromBlocks", // total number of "aromatically connected components"
             "nRings3", "nRings4", "nRings5", "nRings6", "nRings7", "nRings8", "nRings9" // individual breakdown of small rings
                                         };
 
     private IAtomContainer        mol;
     private int[][]               atomAdj, bondAdj; // precalculated adjacencies
     private int[]                 ringBlock;       // ring block identifier; 0=not in a ring
     private int[][]               smallRings;      // all rings of size 3 through 7
     private int[]                 bondOrder;       // numeric bond order for easy reference
     private boolean[]             bondArom; // aromaticity precalculated
     private boolean[]             piAtom;            // true for all atoms involved in a double bond
     private int[]                 implicitH;         // hydrogens in addition to those encoded
 
     public SmallRingDescriptor() {}
 
     @Override
     public void initialise(IChemObjectBuilder builder) {}
 
     /**
      * Fetch descriptor specification.
      */
     @Override
     public DescriptorSpecification getSpecification() {
         return new DescriptorSpecification(
                 "http://www.blueobelisk.org/ontologies/chemoinformatics-algorithms/#smallRings", this.getClass()
                         .getName(), "The Chemistry Development Kit");
     }
 
     /**
      * Set parameters: ignored, there are none.
      */
     @Override
     public void setParameters(Object[] params) throws CDKException {}
 
     /**
      * Get parameters: returns empty array, there are none.
      */
     @Override
     public Object[] getParameters() {
         return new Object[0];
     }
 
     /**
      * Returns the names of the descriptors made available by this class.
      */
     @Override
     public String[] getDescriptorNames() {
         return NAMES;
     }
 
     /**
      * Returns a placeholder with the descriptor size and type.
      */
     @Override
     public IDescriptorResult getDescriptorResultType() {
         return new IntegerArrayResult(NAMES.length);
     }
 
     /**
      * Get parameters: empty, there are none.
      */
     @Override
     public String[] getParameterNames() {
         return new String[0];
     }
 
     /**
      * Parameter types: there aren't any.
      */
     @Override
     public Object getParameterType(String name) {
         return true;
     }
 
 
/** Performs the calculation: the graph will be analyzed and ring information will be determined and wrapped  up into descriptors. */
 public DescriptorValue calculate(IAtomContainer mol){
        this.mol = mol;
        int nAtoms = mol.getAtomCount();
        int nBonds = mol.getBondCount();
        int nRings = 0;
        int nAromRings = 0;
        int nRingBlocks = 0;
        int nAromBlocks = 0;
        int nRings3 = 0;
        int nRings4 = 0;
        int nRings5 = 0;
        int nRings6 = 0;
        int nRings7 = 0;
        int nRings8 = 0;
        int nRings9 = 0;
    
        // precalculate aromaticity
        bondArom = new boolean[nBonds];
        for (IBond bond : mol.bonds()) {
            if (bond.getFlag(CDKConstants.ISAROMATIC)) {
                bondArom[bond.getAtom(0).getIndex()] = true;
                bondArom[bond.getAtom(1).getIndex()] = true;
            }
        }
    
        // precalculate bond order
        bondOrder = new int[nBonds];
        for (IBond bond : mol.bonds()) {
            bondOrder[bond.getIndex()] = bond.getOrder().numeric();
        }
    
        // precalculate pi-atoms
        piAtom = new boolean[nAtoms];
        for (IBond bond : mol.bonds()) {
            if (bond.getOrder().numeric() == 2) {
                piAtom[bond.getAtom(0).getIndex()] = true;
                piAtom[bond.getAtom(1).getIndex()] = true;
            }
        }
    
        // precalculate implicit hydrogens
        implicitH = new int[nAtoms];
        for (IAtom atom : mol.atoms()) {
            implicitH[atom.getIndex()] = atom.getImplicitHydrogenCount();
        }
    
        // precalculate adjacency information       
 }

 

}