/* Copyright (C) 2010  Egon Willighagen <egonw@users.sf.net>
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
 package org.openscience.cdk.geometry.cip;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.geometry.cip.rules.CIPLigandRule;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IDoubleBondStereochemistry;
 import org.openscience.cdk.interfaces.IStereoElement;
 import org.openscience.cdk.interfaces.ITetrahedralChirality;
 import org.openscience.cdk.interfaces.IBond.Order;
 import org.openscience.cdk.interfaces.ITetrahedralChirality.Stereo;
 import org.openscience.cdk.stereo.StereoTool;
 
 import static org.openscience.cdk.interfaces.IDoubleBondStereochemistry.Conformation;
 
 /**
  * Tool to help determine the R,S and stereochemistry definitions of a subset of the
  * CIP rules {@cdk.cite Cahn1966}. The used set up sub rules are specified in the
  * {@link CIPLigandRule} class.
  *
  * <p>Basic use starts from a {@link ITetrahedralChirality} and therefore
  * assumes atoms with four neighbours:
  * <pre>
  * IAtom[] ligandAtoms =
  *   mol.getConnectedAtomsList(centralAtom).toArray(new IAtom[4]);
  * ITetrahedralChirality tetraStereo = new TetrahedralChirality(
  *   centralAtom, ligandAtoms, Stereo.ANTI_CLOCKWISE
  * );
  * CIP_CHIRALITY cipChirality = CIPTool.getCIPChirality(mol, tetraStereo);
  * </pre>
  * The {@link org.openscience.cdk.interfaces.IBond.Stereo} value can be
  * reconstructed from 3D coordinates with the {@link StereoTool}.
  *
  * @cdk.module cip
  * @cdk.githash
  */
 public class CIPTool {
 
     /**
      * IAtom index to indicate an implicit hydrogen, not present in the chemical graph.
      */
     public static final int      HYDROGEN = -1;
 
     private static CIPLigandRule cipRule  = new CIPLigandRule();
 
     /**
      * Enumeration with the two tetrahedral chiralities defined by the CIP schema.
      *
      * @author egonw
      */
     public enum CIP_CHIRALITY {
         R, S, E, Z, NONE
     }
 
     /**
      * Returns the R or S chirality according to the CIP rules, based on the given
      * chirality information.
      *
      * @param  stereoCenter Chiral center for which the CIP chirality is to be
      *                      determined as {@link LigancyFourChirality} object.
      * @return A {@link CIP_CHIRALITY} value.
      */
     public static CIP_CHIRALITY getCIPChirality(LigancyFourChirality stereoCenter) {
         ILigand[] ligands = order(stereoCenter.getLigands());
         LigancyFourChirality rsChirality = stereoCenter.project(ligands);
 
         boolean allAreDifferent = checkIfAllLigandsAreDifferent(ligands);
         if (!allAreDifferent) return CIP_CHIRALITY.NONE;
 
         if (rsChirality.getStereo() == Stereo.CLOCKWISE) return CIP_CHIRALITY.R;
 
         return CIP_CHIRALITY.S;
     }
 
     /**
      * Convenience method for labelling all stereo elements. The {@link
      * CIP_CHIRALITY} is determined for each element and stored as as {@link
      * String} on the {@link CDKConstants#CIP_DESCRIPTOR} property key.
      * Atoms/bonds that are not stereocenters have no label assigned and the
      * property will be null.
      *
      * @param container structure to label
      */
     public static void label(IAtomContainer container) {
 
         for (IStereoElement stereoElement : container.stereoElements()) {
             if (stereoElement instanceof ITetrahedralChirality) {
                 ITetrahedralChirality tc = (ITetrahedralChirality) stereoElement;
                 tc.getChiralAtom().setProperty(CDKConstants.CIP_DESCRIPTOR, getCIPChirality(container, tc).toString());
             } else if (stereoElement instanceof IDoubleBondStereochemistry) {
                 IDoubleBondStereochemistry dbs = (IDoubleBondStereochemistry) stereoElement;
                 dbs.getStereoBond()
                         .setProperty(CDKConstants.CIP_DESCRIPTOR, getCIPChirality(container, dbs).toString());
             }
         }
 
     }
 
     /**
      * Returns the R or S chirality according to the CIP rules, based on the given
      * chirality information.
      *
      * @param  container    {@link IAtomContainer} to which the <code>stereoCenter</code>
      *                      belongs.
      * @param  stereoCenter Chiral center for which the CIP chirality is to be
      *                      determined as {@link ITetrahedralChirality} object.
      * @return A {@link CIP_CHIRALITY} value.
      */
     public static CIP_CHIRALITY getCIPChirality(IAtomContainer container, ITetrahedralChirality stereoCenter) {
 
         // the LigancyFourChirality is kind of redundant but we keep for an
         // easy way to get the ILigands array
         LigancyFourChirality tmp = new LigancyFourChirality(container, stereoCenter);
         Stereo stereo = stereoCenter.getStereo();
 
         int parity = permParity(tmp.getLigands());
 
         if (parity == 0) return CIP_CHIRALITY.NONE;
         if (parity < 0) stereo = stereo.invert();
 
         if (stereo == Stereo.CLOCKWISE) return CIP_CHIRALITY.R;
         if (stereo == Stereo.ANTI_CLOCKWISE) return CIP_CHIRALITY.S;
 
         return CIP_CHIRALITY.NONE;
     }
 
     public static CIP_CHIRALITY getCIPChirality(IAtomContainer container, IDoubleBondStereochemistry stereoCenter) {
 
         IBond stereoBond = stereoCenter.getStereoBond();
         IBond leftBond = stereoCenter.getBonds()[0];
         IBond rightBond = stereoCenter.getBonds()[1];
 
         // the following variables are usd to label the atoms - makes things
         // a little more concise
         //
         // x       y       x
         //  \     /         \
         //   u = v    or     u = v
         //                        \
         //                         y
         //
         IAtom u = stereoBond.getBegin();
         IAtom v = stereoBond.getEnd();
         IAtom x = leftBond.getOther(u);
         IAtom y = rightBond.getOther(v);
 
         Conformation conformation = stereoCenter.getStereo();
 
         ILigand[] leftLigands = getLigands(u, container, v);
         ILigand[] rightLigands = getLigands(v, container, u);
 
         if (leftLigands.length > 2 || rightLigands.length > 2) return CIP_CHIRALITY.NONE;
 
         // invert if x/y aren't in the first position
         if (!leftLigands[0].getLigandAtom().equals(x)) conformation = conformation.invert();
         if (!rightLigands[0].getLigandAtom().equals(y)) conformation = conformation.invert();
 
         int p = permParity(leftLigands) * permParity(rightLigands);
 
         if (p == 0) return CIP_CHIRALITY.NONE;
 
         if (p < 0) conformation = conformation.invert();
 
         if (conformation == Conformation.TOGETHER) return CIP_CHIRALITY.Z;
         if (conformation == Conformation.OPPOSITE) return CIP_CHIRALITY.E;
 
         return CIP_CHIRALITY.NONE;
     }
 
     /**
      * Obtain the ligands connected to the 'atom' excluding 'exclude'. This is
      * mainly meant as a utility for double-bond labelling.
      *
      * @param atom      an atom
      * @param container a structure to which 'atom' belongs
      * @param exclude   exclude this atom - can not be null
      * @return the ligands
      */
     private static ILigand[] getLigands(IAtom atom, IAtomContainer container, IAtom exclude) {
 
         List<IAtom> neighbors = container.getConnectedAtomsList(atom);
 
         ILigand[] ligands = new ILigand[neighbors.size() - 1];
 
         int i = 0;
         for (IAtom neighbor : neighbors) {
             if (!neighbor.equals(exclude)) ligands[i++] = new Ligand(container, new VisitedAtoms(), atom, neighbor);
         }
 
         return ligands;
     }
 
     /**
      * Checks if each next {@link ILigand} is different from the previous
      * one according to the {@link CIPLigandRule}. It assumes that the input
      * is sorted based on that rule.
      *
      * @param ligands array of {@link ILigand} to check
      * @return true, if all ligands are different
      */
     public static boolean checkIfAllLigandsAreDifferent(ILigand[] ligands) {
         for (int i = 0; i < (ligands.length - 1); i++) {
             if (cipRule.compare(ligands[i], ligands[i + 1]) == 0) return false;
         }
         return true;
     }
 
     /**
      * Reorders the {@link ILigand} objects in the array according to the CIP rules.
      *
      * @param ligands Array of {@link ILigand}s to be reordered.
      * @return        Reordered array of {@link ILigand}s.
      */
     public static ILigand[] order(ILigand[] ligands) {
         ILigand[] newLigands = new ILigand[ligands.length];
         System.arraycopy(ligands, 0, newLigands, 0, ligands.length);
 
         Arrays.sort(newLigands, cipRule);
         return newLigands;
     }
 
 
/** Get the permutation parity (-1.0,+1) to put the ligands in descending order (highest first). */
 private static int permParity(final ILigand[] ligands){
        int parity = 0;
        for (int i = 0; i < ligands.length; i++) {
            parity += ligands[i].getLigandAtom().getAtomicNumber();
        }
        return parity % 2 == 0 ? 1 : -1;
    }
    
        /**
        * Get the permutation parity (-1.0,+1) to put the ligands in descending order (highest first).
        *
        * @param ligands Array of {@link ILigand}s to be reordered.
        * @return        Reordered array of {@link ILigand}s.
        */
        public static int permParity(final ILigand[] ligands) {
            int parity = 0;
            for (int i = 0; i < ligands.length; i++) {
                parity += ligands[i].getLigandAtom().getAtomicNumber();
            }
            return parity % 2 == 0 ? 1 : -1;
        }
    
        /**
        * Get the permutation parity (-1.0,+1) to put the ligands in descending order (highest first).
        *
        * @param ligands Array of {@link ILigand}s to be reordered.
        * @return        Reordered array of {@link ILigand}s.
        */
        public static int permParity(final List<ILigand> ligands) {
            int parity = 0;
            for (int i = 0; i < ligands.size(); i++) {
                parity += ligands.get(i).getLigandAtom().getAtomicNumber();
            }
            return parity % 2 == 0 ? 1 : -1;
        }
    
        /**
        * Get the permutation parity (-1.0,+1) to put the ligands in descending order (highest first).
        *
        * @param ligands Array of {@link ILigand}s to be reordered.
        * @return        Reordered array of {@link ILigand}s.
        */
        public static int permParity(final ILigand ligands) {
            int     
 }

 

}