/* Copyright (C) 2012   Syed Asad Rahman <asad@ebi.ac.uk>
  *
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
 package org.openscience.cdk.fingerprint;
 
 import java.io.Serializable;
 import java.util.ArrayList;
 import java.util.BitSet;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 import org.openscience.cdk.aromaticity.Aromaticity;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.graph.ConnectivityChecker;
 import org.openscience.cdk.graph.Cycles;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomContainerSet;
 import org.openscience.cdk.interfaces.IRingSet;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.RingSetManipulator;
 import org.openscience.cdk.tools.periodictable.PeriodicTable;
 
 /**
  * Generates a fingerprint for a given {@link IAtomContainer}. Fingerprints are one-dimensional bit arrays, where bits
  * are set according to a the occurrence of a particular structural feature (See for example the Daylight inc. theory
  * manual for more information). Fingerprints are a means for determining the similarity of chemical structures,
  * some fingerprints (not this one) allow database pre-screening for substructure searches.
 
  * <pre>
  *
  * A fingerprint is generated for an AtomContainer with this code:
  * It is recommended to use atomtyped container before generating the fingerprints.
  *
  * For example: AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(atomContainer);
  *
  *   AtomContainer molecule = new AtomContainer();
  *   AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(atomContainer);
  *   IFingerprinter fingerprinter = new ShortestPathFingerprinter();
  *   IBitFingerprint fingerprint = fingerprinter.getFingerprint(molecule);
  *   fingerprint.fingerprintLength(); // returns 1024 by default
  *   fingerprint.length(); // returns the highest set bit
  * </pre>
  *
  * <P>The FingerPrinter calculates fingerprint based on the Shortest Paths between two atoms. It also takes into account
  * ring system, charges etc while generating a fingerprint. </P>
  *
  * <p>The FingerPrinter assumes that hydrogens are explicitly given! Furthermore, if pseudo atoms or atoms with
  * malformed symbols are present, their atomic number is taken as one more than the last element currently supported in {@link PeriodicTable}.
  * </P>
  *
  * <br/>
  * <b>
  * Important! this fingerprint can not be used for substructure screening.
  * </b>
  *
  * @author Syed Asad Rahman (2012)
  * @cdk.keyword fingerprint
  * @cdk.keyword similarity
  * @cdk.module fingerprint
  * @cdk.githash
  *
  */
 public class ShortestPathFingerprinter extends AbstractFingerprinter implements IFingerprinter, Serializable {
 
     /**
      * The default length of created fingerprints.
      */
     public final static int     DEFAULT_SIZE     = 1024;
     private static final long   serialVersionUID = 7867864332244557861L;
     /**
      * The default length of created fingerprints.
      */
     private int                 fingerprintLength;
     private static ILoggingTool logger           = LoggingToolFactory
                                                          .createLoggingTool(ShortestPathFingerprinter.class);
 
     /**
      * Creates a fingerprint generator of length
      * <code>DEFAULT_SIZE</code>
      */
     public ShortestPathFingerprinter() {
         this(DEFAULT_SIZE);
     }
 
     /**
      * Constructs a fingerprint generator that creates fingerprints of the given fingerprintLength, using a generation
      * algorithm with shortest paths.
      *
      * @param fingerprintLength The desired fingerprintLength of the fingerprint
      */
     public ShortestPathFingerprinter(int fingerprintLength) {
         this.fingerprintLength = fingerprintLength;
     }
 
     /**
      * Generates a shortest path based BitSet fingerprint for the given AtomContainer.
      *
      * @param ac The AtomContainer for which a fingerprint is generated
      * @exception CDKException if there error in aromaticity perception or other CDK functions
      * @return A {@link BitSet} representing the fingerprint
      */
     @Override
     public IBitFingerprint getBitFingerprint(IAtomContainer ac) throws CDKException {
 
         IAtomContainer atomContainer = null;
         try {
             atomContainer = (IAtomContainer) ac.clone();
         } catch (CloneNotSupportedException ex) {
             logger.error("Failed to clone the molecule:", ex);
         }
         Aromaticity.cdkLegacy().apply(atomContainer);
         BitSet bitSet = new BitSet(fingerprintLength);
         if (!ConnectivityChecker.isConnected(atomContainer)) {
             IAtomContainerSet partitionedMolecules = ConnectivityChecker.partitionIntoMolecules(atomContainer);
             for (IAtomContainer container : partitionedMolecules.atomContainers()) {
                 addUniquePath(container, bitSet);
             }
         } else {
             addUniquePath(atomContainer, bitSet);
         }
         return new BitSetFingerprint(bitSet);
     }
 
     /**
      * {@inheritDoc}
      *
      * @param ac The AtomContainer for which a fingerprint is generated
      * @return Map of raw fingerprint paths/features
      * @throws UnsupportedOperationException method is not supported
      */
     @Override
     public Map<String, Integer> getRawFingerprint(IAtomContainer ac) throws CDKException {
         throw new UnsupportedOperationException();
     }
 
     private void addUniquePath(IAtomContainer container, BitSet bitSet) {
         Integer[] hashes = findPaths(container);
         for (Integer hash : hashes) {
             int position = getRandomNumber(hash);
             bitSet.set(position);
         }
     }
 
     private void addUniquePath(IAtomContainer atomContainer, Map<String, Integer> uniquePaths) {
         Integer[] hashes;
         hashes = findPaths(atomContainer);
         for (Integer hash : hashes) {
             int position = getRandomNumber(hash);
             uniquePaths.put(String.valueOf(position), hash);
         }
     }
 
 
/** Get all paths of lengths 0 to the specified length. */
 private Integer[] findPaths(IAtomContainer container){}

 

}