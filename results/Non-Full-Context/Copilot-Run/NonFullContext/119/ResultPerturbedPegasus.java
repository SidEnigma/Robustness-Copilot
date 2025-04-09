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
 
 
/** The kekuliseAromaticRings function adds double/ single bond order information for the molecule with all the atoms marked Planar3 hybridisation. */
 public IAtomContainer kekuliseAromaticRings(IAtomContainer molecule) throws CDKException{
     
      //Check if molecule is aromatic
      if (!isAromatic(molecule)) {
            throw new CDKException("Molecule is not aromatic");
      }
     
      //Check if molecule is marked as SP2 hybridisation
      if (!isSP2(molecule)) {
            throw new CDKException("Molecule is not marked as SP2");
      }
     
      //Check if molecule is marked as Planar3 hybridisation
      if (!isPlanar3(molecule)) {
            throw new CDKException("Molecule is not marked as Planar3");
      }
     
      //Check if molecule is marked as Planar3 hybridisation
      if (!isPlanar3(molecule)) {
            throw new CDKException("Molecule is not marked as Planar3");
      }
     
      //Check if molecule is marked as Planar3 hybridisation
      if (!isPlanar3(molecule)) {
            throw new CDKException("Molecule is not marked as Planar3");
      }
     
      //Check if molecule is marked as Planar3 hybridisation
      if (!isPlanar3(molecule)) {
            throw new CDKException("Molecule is not marked as Planar3");
      }
     
      //Check if molecule is marked as Planar3 hybridisation
      if (!isPlanar3(molecule)) {
            throw new CDKException("Molecule is not marked as Planar3");
      }
     
      //Check if molecule is marked as Planar3 hybridisation
      if (!isPlanar3(molecule)) {
            throw new CDKException("Molecule is not marked as Planar3");
      }
     
      //Check if molecule is marked as Planar3 hybridisation
      if (!isPlanar3(molecule)) {
            throw new CDKException("Molecule is not marked as Planar3");
      }
     
      //Check if molecule is marked as Planar3 hybridisation
      if (!isPlanar3(molecule)) {
            throw new CDKException("Molecule is not marked as           
 }

 

}