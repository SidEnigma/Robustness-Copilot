/* Copyright (C) 2006-2009  Syed Asad Rahman <asad@ebi.ac.uk>
  *                    2010  Egon Willighagen <egonw@users.sf.net>
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
  * You should have received atom copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 package org.openscience.cdk.normalize;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import javax.vecmath.Point2d;
 import javax.vecmath.Point3d;
 
 import org.openscience.cdk.Atom;
 import org.openscience.cdk.Bond;
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.DefaultChemObjectBuilder;
 import org.openscience.cdk.PseudoAtom;
 import org.openscience.cdk.aromaticity.Aromaticity;
 import org.openscience.cdk.atomtype.CDKAtomTypeMatcher;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomType;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IBond.Order;
 import org.openscience.cdk.interfaces.ILonePair;
 import org.openscience.cdk.interfaces.IPseudoAtom;
 import org.openscience.cdk.interfaces.IRing;
 import org.openscience.cdk.interfaces.IRingSet;
 import org.openscience.cdk.interfaces.ISingleElectron;
 import org.openscience.cdk.interfaces.IStereoElement;
 import org.openscience.cdk.ringsearch.AllRingsFinder;
 import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
 import org.openscience.cdk.tools.manipulator.AtomTypeManipulator;
 import org.openscience.cdk.tools.manipulator.RingSetManipulator;
 
 /**
  * This class containes set of modules required to clean a molecule
  * before subjecting it for MCS search. eg. aromatizeMolecule
  * @cdk.module smsd
  * @cdk.githash
  * @author Syed Asad Rahman &lt;asad@ebi.ac.uk&gt;
  * @deprecated This class is part of SMSD and either duplicates functionality elsewhere in the CDK or provides public
  *             access to internal implementation details. SMSD has been deprecated from the CDK with a newer, more recent
  *             version of SMSD is available at <a href="http://github.com/asad/smsd">http://github.com/asad/smsd</a>.
  */
 @Deprecated
 public class SMSDNormalizer extends AtomContainerManipulator {
 
     /**
      * Returns deep copy of the molecule
      * @param container
      * @return deep copy of the mol
      */
     public static IAtomContainer makeDeepCopy(IAtomContainer container) {
 
         IAtomContainer newAtomContainer = DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainer.class);
 
         int lonePairCount = container.getLonePairCount();
         int singleElectronCount = container.getSingleElectronCount();
 
         ILonePair[] lonePairs = new ILonePair[lonePairCount];
         ISingleElectron[] singleElectrons = new ISingleElectron[singleElectronCount];
 
         //      Deep copy of the Atoms
         IAtom[] atoms = copyAtoms(container, newAtomContainer);
 
         //      Deep copy of the bonds
         copyBonds(atoms, container, newAtomContainer);
 
         //      Deep copy of the LonePairs
         for (int index = 0; index < container.getLonePairCount(); index++) {
 
             if (container.getAtom(index).getSymbol().equalsIgnoreCase("R")) {
                 lonePairs[index] = DefaultChemObjectBuilder.getInstance().newInstance(ILonePair.class,
                         container.getAtom(index));
             }
             newAtomContainer.addLonePair(lonePairs[index]);
         }
 
         for (int index = 0; index < container.getSingleElectronCount(); index++) {
             singleElectrons[index] = DefaultChemObjectBuilder.getInstance().newInstance(ISingleElectron.class,
                     container.getAtom(index));
             newAtomContainer.addSingleElectron(singleElectrons[index]);
 
         }
         newAtomContainer.addProperties(container.getProperties());
         newAtomContainer.setFlags(container.getFlags());
 
         newAtomContainer.setID(container.getID());
 
         newAtomContainer.notifyChanged();
         return newAtomContainer;
 
     }
 
 
/** This function finds rings and uses aromaticity detection code to  aromatize the molecule. */
 public static void aromatizeMolecule(IAtomContainer mol){}

 

}