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
 
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.ITetrahedralChirality;
 import org.openscience.cdk.interfaces.ITetrahedralChirality.Stereo;
 
 import java.util.Objects;
 
 /**
  * Stereochemistry specification for quadrivalent atoms to be used for the CIP algorithm only.
  *
  * <p>The data model defines the central, chiral {@link IAtom},
  * and its four {@link ILigand}s, each of which has an ligand {@link IAtom}, directly bonded to the chiral atom via
  * an {@link IBond}. The ordering of the four ligands is important, and defines together with the {@link STEREO}
  * to spatial geometry around the chiral atom. The first ligand points towards to observer, and the three other
  * ligands point away from the observer; the {@link STEREO} then defines the order of the second, third, and
  * fourth ligand to be clockwise or anti-clockwise.
  *
  * @cdk.module cip
  * @cdk.githash
  */
 class LigancyFourChirality {
 
     private IAtom                        chiralAtom;
     private ILigand[]                    ligands;
     private ITetrahedralChirality.Stereo stereo;
 
     /**
      * Creates a new data model for chirality for the CIP rules.
      *
      * @param chiralAtom The {@link IAtom} that is actually chiral.
      * @param ligands    An array with exactly four {@link ILigand}s.
      * @param stereo     A indication of clockwise or anticlockwise orientation of the atoms.
      *
      * @see ITetrahedralChirality.Stereo
      */
     public LigancyFourChirality(IAtom chiralAtom, ILigand[] ligands, ITetrahedralChirality.Stereo stereo) {
         this.chiralAtom = chiralAtom;
         this.ligands = ligands;
         this.stereo = stereo;
     }
 
     /**
      * Creates a new data model for chirality for the CIP rules based on a chirality definition
      * in the CDK data model with {@link ITetrahedralChirality}.
      *
      * @param container    {@link IAtomContainer} to which the chiral atom belongs.
      * @param cdkChirality {@link ITetrahedralChirality} object specifying the chirality.
      */
     public LigancyFourChirality(IAtomContainer container, ITetrahedralChirality cdkChirality) {
         this.chiralAtom = cdkChirality.getChiralAtom();
         IAtom[] ligandAtoms = cdkChirality.getLigands();
         this.ligands = new ILigand[ligandAtoms.length];
         VisitedAtoms visitedAtoms = new VisitedAtoms();
         for (int i = 0; i < ligandAtoms.length; i++) {
             // ITetrahedralChirality stores a impl hydrogen as the central atom
             if (ligandAtoms[i].equals(chiralAtom)) {
                 this.ligands[i] = new ImplicitHydrogenLigand(container, visitedAtoms, chiralAtom);
             } else {
                 this.ligands[i] = new Ligand(container, visitedAtoms, chiralAtom, ligandAtoms[i]);
             }
         }
         this.stereo = cdkChirality.getStereo();
     }
 
     /**
      * Returns the four ligands for this chirality.
      *
      * @return An array of four {@link ILigand}s.
      */
     public ILigand[] getLigands() {
         return ligands;
     }
 
     /**
      * Returns the chiral {@link IAtom} to which the four ligands are connected..
      *
      * @return The chiral {@link IAtom}.
      */
     public IAtom getChiralAtom() {
         return chiralAtom;
     }
 
     /**
      * Returns the chirality value for this stereochemistry object.
      *
      * @return A {@link ITetrahedralChirality.Stereo} value.
      */
     public ITetrahedralChirality.Stereo getStereo() {
         return stereo;
     }
 
 
/** Recalculates the {@link LigancyFourChirality} based on the new, given atom ordering. */
 public LigancyFourChirality project(ILigand[] newOrder){
        if (newOrder.length != 4) {
            throw new IllegalArgumentException("Expected four ligands, got " + newOrder.length);
        }
        ILigand[] newLigands = new ILigand[4];
        for (int i = 0; i < 4; i++) {
            newLigands[i] = ligands[newOrder[i].getIndex()];
        }
        return new LigancyFourChirality(chiralAtom, newLigands, stereo);
    }
    
        /**
        * Returns a string representation of this stereochemistry object.
        *
        * @return A string representation of this stereochemistry object.
        */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(chiralAtom.getSymbol());
            sb.append("(");
            sb.append(ligands[0].getLigandAtom().getSymbol());
            sb.append(",");
            sb.append(ligands[1].getLigandAtom().getSymbol());
            sb.append(",");
            sb.append(ligands[2].getLigandAtom().getSymbol());
            sb.append(",");
            sb.append(ligands[3].getLigandAtom().getSymbol());
            sb.append(")");
            sb.append(stereo);
            return sb.toString();
        }
    
        /**
        * Returns a hash code value for this stereochemistry object.
        *
        * @return A hash code value for this stereochemistry object.
        */
        @Override
        public int hashCode() {
            return Objects.hash(chiralAtom, ligands, stereo);
        }
    
        /**
        * Compares this stereochemistry object with the specified object for equality.
        *
        * @param obj The object to be compared to this stereochemistry object.
        * @return {@code true} if the given object is equal to this stereochemistry object, {@code false} otherwise     
 }

 

}