/*  Copyright (C)  2012  Kevin Lawson <kevin.lawson@syngenta.com>
  *                       Lucy Entwistle <lucy.entwistle@syngenta.com>
  *
  *  Contact: cdk-devel@lists.sourceforge.net
  *
  *  This program is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU Lesser General Public License
  *  as published by the Free Software Foundation; either version 2.1
  *  of the License, or (at your option) any later version.
  *  All I ask is that proper credit is given for my work, which includes
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
 package org.openscience.cdk.smiles;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Objects;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.graph.Cycles;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomType.Hybridization;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IRing;
 import org.openscience.cdk.interfaces.IRingSet;
 import org.openscience.cdk.tools.CDKHydrogenAdder;
 
 /**
  * Class to Fix bond orders at present for Aromatic Rings only.
  *
  * Contains one public function: kekuliseAromaticRings(IAtomContainer molecule)
  * <ul>
  * <li>Analyses which rings are marked aromatic/SP2/Planar3
  * <li>Splits rings into groups containing independent sets of single/fused rings
  * <li>Loops over each ring group
  * <li>Uses an adjacency matrix of bonds (rows) and atoms (columns) to represent
  * each fused ring system
  * <li>Scans the adjacency matrix for bonds for which there
  * is no order choice (eg - both bonds to the NH of pyrrole must be single)
  * <li>All choices made to match valency against bonds used (including implicit H atoms)
  * <li>Solves other bonds as possible - dependent on previous choices - makes free
  * (random) choices only where necessary and possible
  * <li>Makes assumption that where there is a choice in bond order
  * (not forced by previous choices) - either choice is consistent with correct solution
  *
  * <li>Requires molecule with all rings to be solved being marked aromatic
  * (SP2/Planar3 atoms). All bonds to non-ring atoms need to be fully defined
  * (including implicit H atoms)
  * </ul>
  *
  * @author Kevin Lawson
  * @author Lucy Entwistle
  * @cdk.module smiles
  * @cdk.githash
  * @deprecated use {@link org.openscience.cdk.aromaticity.Kekulization}
  */
 @Deprecated
 public class FixBondOrdersTool {
 
     private boolean interrupted;
 
     private static class Matrix {
 
         private int[] mArray;
         private int   rowCount;
         private int   columnCount;
 
         public Matrix(Integer rows, Integer cols) {
 
             //Single array of size rows * cols in matrix
             mArray = new int[rows * cols];
 
             //Record no of rows and number of columns
             rowCount = rows;
             columnCount = cols;
         }
 
         public void set(Integer rIndex, Integer cIndex, Integer val) {
             mArray[rIndex * columnCount + cIndex] = val;
         }
 
         public Integer get(Integer rIndex, Integer cIndex) {
             return mArray[rIndex * columnCount + cIndex];
         }
 
         public Integer colIndexOf(Integer colIndex, Integer val) {
             for (int i = 0; i < rowCount; i++) {
                 if (mArray[i * columnCount + colIndex] == val) {
                     return i;
                 }
             }
             return -1;
         }
 
         public Integer rowIndexOf(Integer rowIndex, Integer val) {
             for (int i = 0; i < columnCount; i++) {
                 if (mArray[rowIndex * getCols() + i] == val) {
                     return i;
                 }
             }
             return -1;
         }
 
         public Integer sumOfRow(Integer rowIndex) {
             Integer sumOfRow = 0;
             for (int i = 0; i < columnCount; i++) {
                 sumOfRow += mArray[rowIndex * columnCount + i];
             }
             return sumOfRow;
         }
 
         public Integer getRows() {
             return rowCount;
         }
 
         public Integer getCols() {
             return columnCount;
         }
     }
 
     /**
      * Constructor for the FixBondOrdersTool object.
      */
     public FixBondOrdersTool() {}
 
     /**
      * kekuliseAromaticRings - function to add double/single bond order information for molecules having rings containing all atoms marked SP2 or Planar3 hybridisation.
      * @param molecule The {@link IAtomContainer} to kekulise
      * @return The {@link IAtomContainer} with kekule structure
      * @throws CDKException
      */
     public IAtomContainer kekuliseAromaticRings(IAtomContainer molecule) throws CDKException {
         IAtomContainer mNew = null;
         try {
             mNew = (IAtomContainer) molecule.clone();
         } catch (Exception e) {
             throw new CDKException("Failed to clone source molecule");
         }
 
         IRingSet ringSet;
 
         try {
             ringSet = removeExtraRings(mNew);
         } catch (CDKException x) {
             throw x;
         } catch (Exception x) {
             throw new CDKException("failure in SSSRFinder.findAllRings", x);
         }
 
         if (ringSet == null) {
             throw new CDKException("failure in SSSRFinder.findAllRings");
         }
 
         //We need to establish which rings share bonds and set up sets of such interdependant rings
         List<Integer[]> rBondsArray = null;
         List<List<Integer>> ringGroups = null;
 
         //Start by getting a list (same dimensions and ordering as ringset) of all the ring bond numbers in the reduced ring set
         rBondsArray = getRingSystem(mNew, ringSet);
         //Now find out which share a bond and assign them accordingly to groups
         ringGroups = assignRingGroups(rBondsArray);
 
         //Loop through each group of rings checking all choices of double bond combis and seeing if you can get a
         //proper molecule.
         for (int i = 0; i < ringGroups.size(); i++) {
 
             //Set all ring bonds with single order to allow Matrix solving to work
             setAllRingBondsSingleOrder(ringGroups.get(i), ringSet);
 
             //Set up  lists of atoms, bonds and atom pairs for this ringGroup
             List<Integer> atomNos = null;
             atomNos = getAtomNosForRingGroup(mNew, ringGroups.get(i), ringSet);
 
             List<Integer> bondNos = null;
             bondNos = getBondNosForRingGroup(mNew, ringGroups.get(i), ringSet);
 
             //Array of same dimensions as bondNos (cols in Matrix)
             List<Integer[]> atomNoPairs = null;
             atomNoPairs = getAtomNoPairsForRingGroup(mNew, bondNos);
 
             //Set up ajacency Matrix
             Matrix M = new Matrix(atomNos.size(), bondNos.size());
             for (int x = 0; x < M.getRows(); x++) {
                 for (int y = 0; y < M.getCols(); y++) {
                     if (Objects.equals(atomNos.get(x), atomNoPairs.get(y)[0])) {
                         M.set(x, y, 1);
                     } else {
                         if (Objects.equals(atomNos.get(x), atomNoPairs.get(y)[1])) {
                             M.set(x, y, 1);
                         } else {
                             M.set(x, y, 0);
                         }
                     }
                 }
             }
 
             //Array of same dimensions as atomNos (rows in Matrix)
             List<Integer> freeValencies = null;
             freeValencies = getFreeValenciesForRingGroup(mNew, atomNos, M, ringSet);
 
             //Array of "answers"
             List<Integer> bondOrders = new ArrayList<Integer>();
             for (int j = 0; j < bondNos.size(); j++) {
                 bondOrders.add(0);
             }
 
             if (solveMatrix(M, atomNos, bondNos, freeValencies, atomNoPairs, bondOrders)) {
                 for (int j = 0; j < bondOrders.size(); j++) {
                     mNew.getBond(bondNos.get(j)).setOrder(
                             bondOrders.get(j) == 1 ? IBond.Order.SINGLE : IBond.Order.DOUBLE);
                 }
             } else {
                 //                TODO Put any failure code here
             }
         }
         return mNew;
     }
 
     /**
      * Removes rings which do not have all sp2/planar3 aromatic atoms.
      * and also gets rid of rings that have more than 8 atoms in them.
      *
      * @param m The {@link IAtomContainer} from which we want to remove rings
      * @return The set of reduced rings
      */
     private IRingSet removeExtraRings(IAtomContainer m) throws Exception {
 
         IRingSet rs = Cycles.sssr(m).toRingSet();
 
         //remove rings which dont have all aromatic atoms (according to hybridization set by lower case symbols in smiles):
         Iterator<IAtomContainer> i = rs.atomContainers().iterator();
         while (i.hasNext()) {
             IRing r = (IRing) i.next();
             if (r.getAtomCount() > 8) {
                 i.remove();
             } else {
                 for (IAtom a : r.atoms()) {
                     Hybridization h = a.getHybridization();
                     if (h == CDKConstants.UNSET || !(h == Hybridization.SP2 || h == Hybridization.PLANAR3)) {
                         i.remove();
                         break;
                     }
                 }
             }
         }
         return rs;
     }
 
     /**
      * Stores an {@link IRingSet} corresponding to a molecule using the bond numbers.
      *
      * @param mol The IAtomContainer for which to store the IRingSet.
      * @param ringSet The IRingSet to store
      * @return The List of Integer arrays for the bond numbers of each ringSet
      */
 
     private List<Integer[]> getRingSystem(IAtomContainer mol, IRingSet ringSet) {
         List<Integer[]> bondsArray;
         bondsArray = new ArrayList<Integer[]>();
         for (int r = 0; r < ringSet.getAtomContainerCount(); ++r) {
             IRing ring = (IRing) ringSet.getAtomContainer(r);
             Integer[] bondNumbers = new Integer[ring.getBondCount()];
             for (int i = 0; i < ring.getBondCount(); ++i) {
                 bondNumbers[i] = mol.indexOf(ring.getBond(i));
             }
             bondsArray.add(bondNumbers);
         }
         return bondsArray;
     }
 
 
/** Assigns a set of rings to groups each sharing a bond. */

private List<List<Integer>> assignRingGroups(List<Integer[]> rBondsArray) {
    List<List<Integer>> ringGroups = new ArrayList<>();
    for (int i = 0; i < rBondsArray.size(); i++) {
        Integer[] bondNumbers = rBondsArray.get(i);
        boolean assigned = false;
        for (int j = 0; j < ringGroups.size(); j++) {
            List<Integer> ringGroup = ringGroups.get(j);
            for (int k = 0; k < bondNumbers.length; k++) {
                if (ringGroup.contains(bondNumbers[k])) {
                    ringGroup.addAll(Arrays.asList(bondNumbers));
                    assigned = true;
                    break;
                }
            }
            if (assigned) {
                break;
            }
        }
        if (!assigned) {
            List<Integer> newRingGroup = new ArrayList<>(Arrays.asList(bondNumbers));
            ringGroups.add(newRingGroup);
        }
    }
    return ringGroups;
}
 

}