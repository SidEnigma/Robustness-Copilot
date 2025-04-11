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
 
 
/** Randomly generates a single, connected, and properly linked structure from a number of fragments. */
 public IAtomContainer generate(IAtomContainerSet atomContainers) throws CDKException{
        IAtomContainer molecule = atomContainers.getAtomContainer(0);
        IAtomContainerSet fragmentSet = atomContainers.getConnectedComponents();
        int fragmentCount = fragmentSet.getAtomContainerCount();
        int[] fragmentSizes = new int[fragmentCount];
        for (int i = 0; i < fragmentCount; i++) {
            fragmentSizes[i] = fragmentSet.getAtomContainer(i).getAtomCount();
        }
        int[] fragmentHydrogens = new int[fragmentCount];
        for (int i = 0; i < fragmentCount; i++) {
            fragmentHydrogens[i] = 0;
        }
        int[] fragmentBonds = new int[fragmentCount];
        for (int i = 0; i < fragmentCount; i++) {
            fragmentBonds[i] = 0;
        }
        int[] fragmentAtoms = new int[fragmentCount];
        for (int i = 0; i < fragmentCount; i++) {
            fragmentAtoms[i] = 0;
        }
        int[] fragmentHydrogens2 = new int[fragmentCount];
        for (int i = 0; i < fragmentCount; i++) {
            fragmentHydrogens2[i] = 0;
        }
        int[] fragmentBonds2 = new int[fragmentCount];
        for (int i = 0; i < fragmentCount; i++) {
            fragmentBonds2[i] = 0;
        }
        int[] fragmentAtoms2 = new int[fragmentCount];
        for (int i = 0; i < fragmentCount; i++) {
            fragmentAtoms2[i] = 0;
        }
        int[] fragmentHydrogens3 = new int[fragmentCount];
        for (int i = 0; i < fragmentCount; i++) {
            fragmentHydrogens3[i] = 0;
        }
        int[] fragmentBonds3 = new int[fragmentCount];
        for (int i = 0; i < fragmentCount; i++) {
            fragmentBonds3[i] = 0;
        }       
 }

 

}