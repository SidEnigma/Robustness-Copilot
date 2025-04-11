/* Copyright (C) 1997-2009  Christoph Steinbeck, Stefan Kuhn <shk3@users.sf.net>
  *
  *  Contact: cdk-devel@lists.sourceforge.net
  *
  *  This program is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU Lesser General Public License
  *  as published by the Free Software Foundation; either version 2.1
  *  of the License, or (at your option) any later version.
  *  All we ask is that proper credit is given for our work, which includes
  *  - but is not limited to - adding the above copyright notice to the beginning
  *  of your source code files, and to any copyright notice that you may distribute
  *  with programs based on this work.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU Lesser General Public License for more details.
  *
  *  You should have received a copy of the GNU Lesser General Public License
  *  along with this program; if not, write to the Free Software
  *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 package org.openscience.cdk.structgen.stochastic;
 
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomContainerSet;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.SaturationChecker;
 import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
 import org.openscience.cdk.tools.manipulator.AtomContainerSetManipulator;
 import org.openscience.cdk.tools.manipulator.BondManipulator;
 
 /**
  * Randomly generates a single, connected, correctly bonded structure from
  * a number of fragments.
  * <p>Assign hydrogen counts to each heavy atom. The hydrogens should not be
  * in the atom pool but should be assigned implicitly to the heavy atoms in
  * order to reduce computational cost.
  *
  * @author     steinbeck
  * @cdk.created    2001-09-04
  * @cdk.module     structgen
  * @cdk.githash
  */
 public class PartialFilledStructureMerger {
 
     private ILoggingTool logger = LoggingToolFactory.createLoggingTool(PartialFilledStructureMerger.class);
 
     SaturationChecker    satCheck;
 
     /**
      * Constructor for the PartialFilledStructureMerger object.
      */
     public PartialFilledStructureMerger() {
         satCheck = new SaturationChecker();
     }
 
 
/** A single, connected and correctly bound structure from a set of fragments is generated randomly. IMPORTANT: The AtomContainers of the collection must be connected. No valid result will be formed if an AtomContainer is disconnected. */
 public IAtomContainer generate(IAtomContainerSet atomContainers) throws CDKException{
        IAtomContainer molecule = atomContainers.getAtomContainer(0);
        IAtomContainerSet fragments = atomContainers.getConnectedComponents();
        int nrOfFragments = fragments.getAtomContainerCount();
        int nrOfAtoms = molecule.getAtomCount();
        int nrOfBonds = molecule.getBondCount();
        int nrOfHydrogens = 0;
        int nrOfHeavyAtoms = 0;
        int nrOfHeavyBonds = 0;
        int nrOfHeavyHydrogens = 0;
        int nrOfHeavyHydrogenBonds = 0;
        int nrOfHeavyHydrogenAtoms = 0;
        int nrOfHeavyBondAtoms = 0;
        int nrOfHeavyBondBonds = 0;
        int nrOfHeavyBondHydrogens = 0;
        int nrOfHeavyBondHydrogenBonds = 0;
        int nrOfHeavyBondHydrogenAtoms = 0;
        int nrOfHeavyBondHeavyAtoms = 0;
        int nrOfHeavyBondHeavyBonds = 0;
        int nrOfHeavyBondHeavyHydrogens = 0;
        int nrOfHeavyBondHeavyHydrogenBonds = 0;
        int nrOfHeavyBondHeavyHydrogenAtoms = 0;
        int nrOfHeavyBondHeavyBondAtoms = 0;
        int nrOfHeavyBondHeavyBondBonds = 0;
        int nrOfHeavyBondHeavyBondHydrogens = 0;
        int nrOfHeavyBondHeavyBondHydrogenBonds = 0;
        int nrOfHeavyBondHeavyBondHydrogenAtoms = 0;
        int nrOfHeavyBondHeavyBondHeavyAtoms = 0;
        int nrOfHeavyBondHeavyBondHeavyBonds = 0;
        int nrOfHeavyBondHeavyBondHeavyHydrogens = 0;
        int nrOfHeavyBondHeavyBondHeavyHydrogenBonds = 0;
        int nrOfHeavyBondHeavyBondHeavyHydrogenAtoms = 0;       
 }

 

}