/* Copyright (C) 2011  Egon Willighagen <egonw@users.sf.net>
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public License
  * as published by the Free Software Foundation; either version 2.1
  * of the License, or (at your option) any later version.
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
 package org.openscience.cdk.graph.invariant;
 
 import net.sf.jniinchi.INCHI_OPTION;
 import net.sf.jniinchi.INCHI_RET;
 
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.inchi.InChIGenerator;
 import org.openscience.cdk.inchi.InChIGeneratorFactory;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 
 import java.util.Arrays;
 import java.util.List;
 
 /**
  * Tool for calculating atom numbers using the InChI algorithm.
  *
  * @cdk.module  inchi
  * @cdk.githash
  */
 public class InChINumbersTools {
 
     /**
      * Makes an array containing the InChI atom numbers of the non-hydrogen
      * atoms in the atomContainer. It returns zero for all hydrogens.
      *
      * @param  atomContainer  The {@link IAtomContainer} to analyze.
      * @return                The number from 1 to the number of heavy atoms.
      * @throws CDKException   When the InChI could not be generated
      */
     public static long[] getNumbers(IAtomContainer atomContainer) throws CDKException {
         String aux = auxInfo(atomContainer);
         long[] numbers = new long[atomContainer.getAtomCount()];
         parseAuxInfo(aux, numbers);
         return numbers;
     }
 
     /**
      * Parse the atom numbering from the auxinfo.
      *
      * @param aux InChI AuxInfo
      * @param numbers the atom numbers
      */
     public static void parseAuxInfo(String aux, long[] numbers) {
         aux = aux.substring(aux.indexOf("/N:") + 3);
         String numberStringAux = aux.substring(0, aux.indexOf('/'));
         int i = 1;
         for (String numberString : numberStringAux.split("[,;]"))
             numbers[Integer.valueOf(numberString) - 1] = i++;
     }
 
     /**
      * Obtain the InChI numbers for the input container to be used to order
      * atoms in Universal SMILES {@cdk.cite OBoyle12}. The numbers are obtained
      * using the fixedH and RecMet options of the InChI. All non-bridged
      * hydrogens are labelled as 0.
      *
      * @param container the structure to obtain the numbers of
      * @return the atom numbers
      * @throws CDKException
      */
     public static long[] getUSmilesNumbers(IAtomContainer container) throws CDKException {
         String aux = auxInfo(container, INCHI_OPTION.RecMet, INCHI_OPTION.FixedH);
         return parseUSmilesNumbers(aux, container);
     }
 
 
/** To use inUniversal SMILES analyze the canonical atom numbers of InChI (from AuxInfo).  The analysis follows: "Rule A: From the reconnected metal layer (/ R :) it should be possible to obtain the correspondence between the order of the incoming atoms and the canonical InChI labels, rather than from the initial layer, and therefore from the fixed labels of hydrogen (/ F :) in preference to standard labels (/ N :). "  It is also possible to adjust the labels for Rule E: in the case of the start atom is a negatively charged oxygen atom, start instead at any carbonyl oxygen attached to the same neighbour."   The same label is assigned to all unlabeled atoms (eg Hydrogens) which is different but larger than all other labels. Universal SMILES suggests that hydrogens should be visited first therefore hydrogen labeling must therefore be externally regulated. */
 static long[] parseUSmilesNumbers(String aux, IAtomContainer container){
        long[] numbers = new long[container.getAtomCount()];
        aux = aux.substring(aux.indexOf("/N:") + 3);
        String numberStringAux = aux.substring(0, aux.indexOf('/'));
        int i = 1;
        for (String numberString : numberStringAux.split("[,;]"))
            numbers[Integer.valueOf(numberString) - 1] = i++;
        return numbers;
    }
    
        /**
        * Obtain the InChI numbers for the input container to be used to order
        * atoms in Universal SMILES {@cdk.cite OBoyle12}. The numbers are obtained
        * using the fixedH and RecMet options of the InChI. All non-bridged
        * hydrogens are labelled as 0.
        *
        * @param container the structure to obtain the numbers of
        * @return the atom numbers
        * @throws CDKException
        */
        public static long[] getUSmilesNumbers(IAtomContainer container, INCHI_OPTION fixedH, INCHI_OPTION recMet) throws CDKException {
            String aux = auxInfo(container, recMet, fixedH);
            return parseUSmilesNumbers(aux, container);
        }
    
        /**
        * Obtain the InChI numbers for the input container to be used to order
        * atoms in Universal SMILES {@cdk.cite OBoyle12}. The numbers are obtained
        * using the fixedH and RecMet options of the InChI. All non-bridged
        * hydrogens are labelled as 0.
        *
        * @param container the structure to obtain the numbers of
        * @return the atom numbers
        * @throws CDKException
        */
        public static long[] getUSmilesNumbers(IAtomContainer container, INCHI_OPTION fixedH, INCHI_OPTION recMet, INCHI_OPTION hydrogen) throws CDKException {
            String aux = auxInfo(container, recMet, fixedH, hydrogen);
            return parseUSmilesNumbers(aux, container);     
 }

 

}