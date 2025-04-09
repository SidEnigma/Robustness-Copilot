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
 
     /**
      * This function finds rings and uses aromaticity detection code to
      * aromatize the molecule.
      * @param mol input molecule
      */
     public static void aromatizeMolecule(IAtomContainer mol) {
 
         // need to find rings and aromaticity again since added H's
 
         IRingSet ringSet = null;
         try {
             AllRingsFinder arf = new AllRingsFinder();
             ringSet = arf.findAllRings(mol);
 
             // SSSRFinder s = new SSSRFinder(atomContainer);
             // srs = s.findEssentialRings();
 
         } catch (Exception e) {
             e.printStackTrace();
         }
 
         try {
             // figure out which atoms are in aromatic rings:
             //            printAtoms(atomContainer);
             SMSDNormalizer.percieveAtomTypesAndConfigureAtoms(mol);
             //            printAtoms(atomContainer);
             Aromaticity.cdkLegacy().apply(mol);
             //            printAtoms(atomContainer);
             // figure out which rings are aromatic:
             RingSetManipulator.markAromaticRings(ringSet);
             //            printAtoms(atomContainer);
             // figure out which simple (non cycles) rings are aromatic:
             // HueckelAromaticityDetector.detectAromaticity(atomContainer, srs);
         } catch (Exception e) {
             e.printStackTrace();
         }
 
         // only atoms in 6 membered rings are aromatic
         // determine largest ring that each atom is atom part of
 
         for (int i = 0; i <= mol.getAtomCount() - 1; i++) {
 
             mol.getAtom(i).setFlag(CDKConstants.ISAROMATIC, false);
 
             jloop: for (int j = 0; j <= ringSet.getAtomContainerCount() - 1; j++) {
                 //logger.debug(i+"\t"+j);
                 IRing ring = (IRing) ringSet.getAtomContainer(j);
                 if (!ring.getFlag(CDKConstants.ISAROMATIC)) {
                     continue jloop;
                 }
 
                 boolean haveatom = ring.contains(mol.getAtom(i));
 
                 //logger.debug("haveatom="+haveatom);
 
                 if (haveatom && ring.getAtomCount() == 6) {
                     mol.getAtom(i).setFlag(CDKConstants.ISAROMATIC, true);
                 }
             }
         }
     }
 
     /**
      * Returns The number of explicit hydrogens for a given IAtom.
      * @param atomContainer
      * @param atom
      * @return The number of explicit hydrogens on the given IAtom.
      */
     public static int getExplicitHydrogenCount(IAtomContainer atomContainer, IAtom atom) {
         int hCount = 0;
         for (IAtom iAtom : atomContainer.getConnectedAtomsList(atom)) {
             IAtom connectedAtom = iAtom;
             if (connectedAtom.getSymbol().equals("H")) {
                 hCount++;
             }
         }
         return hCount;
     }
 
     /**
      * Returns The number of Implicit Hydrogen Count for a given IAtom.
      * @param atomContainer
      * @param atom
      * @return Implicit Hydrogen Count
      */
     public static int getImplicitHydrogenCount(IAtomContainer atomContainer, IAtom atom) {
         return atom.getImplicitHydrogenCount() == CDKConstants.UNSET ? 0 : atom.getImplicitHydrogenCount();
     }
 
     /**
      * The summed implicit + explicit hydrogens of the given IAtom.
      * @param atomContainer
      * @param atom
      * @return The summed implicit + explicit hydrogens of the given IAtom.
      */
     public static int getHydrogenCount(IAtomContainer atomContainer, IAtom atom) {
         return getExplicitHydrogenCount(atomContainer, atom) + getImplicitHydrogenCount(atomContainer, atom);
     }
 
     /**
      * Returns IAtomContainer without Hydrogen. If an AtomContainer has atom single atom which
      * is atom Hydrogen then its not removed.
      * @param atomContainer
      * @return IAtomContainer without Hydrogen. If an AtomContainer has atom single atom which
      * is atom Hydrogen then its not removed.
      */
     public static IAtomContainer removeHydrogensAndPreserveAtomID(IAtomContainer atomContainer) {
         Map<IAtom, IAtom> map = new HashMap<IAtom, IAtom>(); // maps original atoms to clones.
         List<IAtom> remove = new ArrayList<IAtom>(); // lists removed Hs.
         IAtomContainer mol = null;
         if (atomContainer.getBondCount() > 0) {
             // Clone atoms except those to be removed.
             mol = atomContainer.getBuilder().newInstance(IAtomContainer.class);
             int count = atomContainer.getAtomCount();
             for (int i = 0; i < count; i++) {
                 // Clone/remove this atom?
                 IAtom atom = atomContainer.getAtom(i);
                 if (!atom.getSymbol().equals("H")) {
                     IAtom clonedAtom = null;
                     try {
                         clonedAtom = (IAtom) atom.clone();
                     } catch (CloneNotSupportedException e) {
                         // TODO Auto-generated catch block
                         e.printStackTrace();
                     }
                     //added by Asad to preserve the Atom ID for atom mapping without Hydrogen
                     clonedAtom.setID(atom.getID());
                     clonedAtom.setFlags(atom.getFlags());
                     int countH = 0;
                     if (atom.getImplicitHydrogenCount() != null) {
                         countH = atom.getImplicitHydrogenCount();
                     }
                     clonedAtom.setImplicitHydrogenCount(countH);
                     mol.addAtom(clonedAtom);
                     map.put(atom, clonedAtom);
 
                 } else {
                     remove.add(atom); // maintain list of removed H.
                 }
             }
             //            Clone bonds except those involving removed atoms.
             mol = cloneAndMarkNonHBonds(mol, atomContainer, remove, map);
             //            Recompute hydrogen counts of neighbours of removed Hydrogens.
             mol = reComputeHydrogens(mol, atomContainer, remove, map);
 
         } else {
             mol = atomContainer.getBuilder().newInstance(IAtomContainer.class, atomContainer);
             mol.addProperties(atomContainer.getProperties());
             mol.setFlags(atomContainer.getFlags());
             if (atomContainer.getID() != null) {
                 mol.setID(atomContainer.getID());
             }
             if (atomContainer.getAtom(0).getSymbol().equalsIgnoreCase("H")) {
                 System.err.println("WARNING: single hydrogen atom removal not supported!");
             }
 
         }
 
         return mol;
     }
 
     /**
      * Returns IAtomContainer without Hydrogen. If an AtomContainer has atom single atom which
      * is atom Hydrogen then its not removed.
      * @param atomContainer
      * @return IAtomContainer without Hydrogen. If an AtomContainer has atom single atom which
      * is atom Hydrogen then its not removed.
      */
     public static IAtomContainer convertExplicitToImplicitHydrogens(IAtomContainer atomContainer) {
         IAtomContainer mol = atomContainer.getBuilder().newInstance(IAtomContainer.class, atomContainer);
         convertImplicitToExplicitHydrogens(mol);
         if (mol.getAtomCount() > 1) {
             mol = removeHydrogens(mol);
         } else if (atomContainer.atoms().iterator().next().getSymbol().equalsIgnoreCase("H")) {
             System.err.println("WARNING: single hydrogen atom removal not supported!");
         }
         mol.addProperties(atomContainer.getProperties());
         mol.setFlags(atomContainer.getFlags());
         if (atomContainer.getID() != null) {
             mol.setID(atomContainer.getID());
         }
         return mol;
     }
 
 
/** The convenience method to perceive atom types is used. */
 public static void percieveAtomTypesAndConfigureAtoms(IAtomContainer container) throws CDKException{}

 

}