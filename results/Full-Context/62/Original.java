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
 
 
/** Parse the InChI canonical atom numbers (from the AuxInfo) to use in  Universal SMILES. */
 static long[] parseUSmilesNumbers(String aux, IAtomContainer container){}

                                                                                                         
     /**
      * Exchange the elements at index i with that at index j.
      *
      * @param values an array of values
      * @param i an index
      * @param j another index
      */
     private static void exch(long[] values, int i, int j) {
         long k = values[i];
         values[i] = values[j];
         values[j] = k;
     }
 
     /**
      * Find a neutral oxygen bonded to the {@code atom} with a pi bond.
      *
      * @param container the container
      * @param atom      an atom from the container
      * @return a pi bonded oxygen (or null if not found)
      */
     private static IAtom findPiBondedOxygen(IAtomContainer container, IAtom atom) {
         for (IBond bond : container.getConnectedBondsList(atom)) {
             if (bond.getOrder() == IBond.Order.DOUBLE) {
                 IAtom neighbor = bond.getOther(atom);
                 int charge = neighbor.getFormalCharge() == null ? 0 : neighbor.getFormalCharge();
                 if (neighbor.getAtomicNumber() == 8 && charge == 0) return neighbor;
             }
         }
         return null;
     }
 
     /**
      * Obtain the InChI auxiliary info for the provided structure using
      * using the specified InChI options.
      *
      * @param  container the structure to obtain the numbers of
      * @return auxiliary info
      * @throws CDKException the inchi could not be generated
      */
     static String auxInfo(IAtomContainer container, INCHI_OPTION... options) throws CDKException {
         InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
         boolean org = factory.getIgnoreAromaticBonds();
         factory.setIgnoreAromaticBonds(true);
         InChIGenerator gen = factory.getInChIGenerator(container, Arrays.asList(options));
         factory.setIgnoreAromaticBonds(org); // an option on the singleton so we should reset for others
         if (gen.getReturnStatus() != INCHI_RET.OKAY && gen.getReturnStatus() != INCHI_RET.WARNING)
             throw new CDKException("Could not generate InChI Numbers: " + gen.getMessage());
         return gen.getAuxInfo();
     }
 
 }
