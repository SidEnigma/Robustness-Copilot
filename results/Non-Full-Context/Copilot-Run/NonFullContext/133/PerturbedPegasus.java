/* Copyright (C) 2009  Rajarshi Guha <rajarshi.guha@gmail.com>
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
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.graph.Cycles;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.interfaces.IRingSet;
 import org.openscience.cdk.interfaces.IElement;
 import org.openscience.cdk.smarts.SmartsPattern;
 import org.openscience.cdk.tools.periodictable.PeriodicTable;
 
 
 import java.io.IOException;
 import java.util.BitSet;
 import java.util.HashMap;
 import java.util.Map;
 
 /**
  * Generates a Pubchem fingerprint for a molecule.
  * 
  * These fingerprints are described
  * <a href="ftp://ftp.ncbi.nlm.nih.gov/pubchem/specifications/pubchem_fingerprints.txt">
  * here</a> and are of the structural key type, of length 881. See
  * {@link org.openscience.cdk.fingerprint.Fingerprinter} for a
  * more detailed description of fingerprints in general. This implementation is
  * based on the public domain code made available by the NCGC
  * <a href="http://www.ncgc.nih.gov/pub/openhts/code/NCGC_PubChemFP.java.txt">
  * here</a>
  * 
  * 
  * A fingerprint is generated for an AtomContainer with this code: <pre>
  *   Molecule molecule = new Molecule();
  *   PubchemFingerprinter fprinter = new PubchemFingerprinter();
  *   BitSet fingerprint = fprinter.getBitFingerprint(molecule);
  *   fprinter.getSize(); // returns 881
  *   fingerprint.length(); // returns the highest set bit
  * </pre>
  * Note that the fingerprinter assumes that you have detected aromaticity and
  * atom types before evaluating the fingerprint. Also the fingerprinter
  * expects that explicit H's are present
  * 
  * Note that this fingerprint is not particularly fast, as it will perform
  * ring detection using {@link org.openscience.cdk.ringsearch.AllRingsFinder}
  * as well as multiple SMARTS queries.
  * 
  * Some SMARTS patterns have been modified from the original code, since they
  * were based on explicit H matching. As a result, we replace the explicit H's
  * with a query of the {@code #<N>&!H0} where {@code <N>} is the atomic number. Thus bit 344 was
  * originally {@code [#6](~[#6])([H])} but is written here as
  * {@code [#6&!H0]~[#6]}. In some cases, where the H count can be reduced
  * to single possibility we directly use that H count. An example is bit 35,
  * which was {@code [#6](~[#6])(~[#6])(~[#6])([H])} and is rewritten as
  * {@code [#6H1](~[#6])(~[#6])(~[#6])}.
  * 
  * <br/>
  * <b>Warning - this class is not thread-safe and uses stores intermediate steps
  * internally. Please use a separate instance of the class for each thread.</b>
  * <br/>
  * <b>
  * Important! this fingerprint can not be used for substructure screening.
  * </b>
  *
  * @author Rajarshi Guha
  * @cdk.keyword fingerprint
  * @cdk.keyword similarity
  * @cdk.module fingerprint
  * @cdk.githash
  * @cdk.threadnonsafe
  */
 public class PubchemFingerprinter extends AbstractFingerprinter implements IFingerprinter {
 
     /**
      * Number of bits in this fingerprint.
      */
     public static final int FP_SIZE = 881;
 
     private byte[]          m_bits;
 
     private Map<String,SmartsPattern> cache = new HashMap<>();
 
     public PubchemFingerprinter(IChemObjectBuilder builder) {
         m_bits = new byte[(FP_SIZE + 7) >> 3];
     }
 
     /**
      * Calculate 881 bit Pubchem fingerprint for a molecule.
      * 
      * See
      * <a href="ftp://ftp.ncbi.nlm.nih.gov/pubchem/specifications/pubchem_fingerprints.txt">here</a>
      * for a description of each bit position.
      *
      * @param atomContainer the molecule to consider
      * @return the fingerprint
      * @throws CDKException if there is an error during substructure
      * searching or atom typing
      * @see #getFingerprintAsBytes()
      */
     @Override
     public IBitFingerprint getBitFingerprint(IAtomContainer atomContainer) throws CDKException {
         generateFp(atomContainer);
         BitSet fp = new BitSet(FP_SIZE);
         for (int i = 0; i < FP_SIZE; i++) {
             if (isBitOn(i)) fp.set(i);
         }
         return new BitSetFingerprint(fp);
     }
 
     /** {@inheritDoc} */
     @Override
     public Map<String, Integer> getRawFingerprint(IAtomContainer iAtomContainer) throws CDKException {
         throw new UnsupportedOperationException();
     }
 
     /**
      * Get the size of the fingerprint.
      *
      * @return The bit length of the fingerprint
      */
     @Override
     public int getSize() {
         return FP_SIZE;
     }
 
     static class CountElements {
 
         int[] counts = new int[120];
 
         public CountElements(IAtomContainer m) {
             for (int i = 0; i < m.getAtomCount(); i++)
                 ++counts[m.getAtom(i).getAtomicNumber()];
         }
 
         public int getCount(int atno) {
             return counts[atno];
         }
 
         public int getCount(String symb) {
             return counts[PeriodicTable.getAtomicNumber(symb)];
         }
     }
 
     static class CountRings {
 
         int[][]  sssr = {};
         IRingSet ringSet;
 
         public CountRings(IAtomContainer m) {
             ringSet = Cycles.sssr(m).toRingSet();
         }
 
         public int countAnyRing(int size) {
             int c = 0;
             for (IAtomContainer ring : ringSet.atomContainers()) {
                 if (ring.getAtomCount() == size) c++;
             }
             return c;
         }
 
         private boolean isCarbonOnlyRing(IAtomContainer ring) {
             for (IAtom ringAtom : ring.atoms()) {
                 if (ringAtom.getAtomicNumber() != IElement.C) return false;
             }
             return true;
         }
 
         private boolean isRingSaturated(IAtomContainer ring) {
             for (IBond ringBond : ring.bonds()) {
                 if (ringBond.getOrder() != IBond.Order.SINGLE || ringBond.getFlag(CDKConstants.ISAROMATIC)
                         || ringBond.getFlag(CDKConstants.SINGLE_OR_DOUBLE)) return false;
             }
             return true;
         }
 
         private boolean isRingUnsaturated(IAtomContainer ring) {
             return !isRingSaturated(ring);
         }
 
         private int countNitrogenInRing(IAtomContainer ring) {
             int c = 0;
             for (IAtom ringAtom : ring.atoms()) {
                 if (ringAtom.getAtomicNumber() == IElement.N) c++;
             }
             return c;
         }
 
         private int countHeteroInRing(IAtomContainer ring) {
             int c = 0;
             for (IAtom ringAtom : ring.atoms()) {
                 if (ringAtom.getAtomicNumber() != IElement.C && ringAtom.getAtomicNumber() != IElement.H) c++;
             }
             return c;
         }
 
         private boolean isAromaticRing(IAtomContainer ring) {
             for (IBond bond : ring.bonds())
                 if (!bond.getFlag(CDKConstants.ISAROMATIC)) return false;
             return true;
         }
 
         public int countAromaticRing() {
             int c = 0;
             for (IAtomContainer ring : ringSet.atomContainers()) {
                 if (isAromaticRing(ring)) c++;
             }
             return c;
         }
 
         public int countHeteroAromaticRing() {
             int c = 0;
             for (IAtomContainer ring : ringSet.atomContainers()) {
                 if (!isCarbonOnlyRing(ring) && isAromaticRing(ring)) c++;
             }
             return c;
         }
 
         public int countSaturatedOrAromaticCarbonOnlyRing(int size) {
             int c = 0;
             for (IAtomContainer ring : ringSet.atomContainers()) {
                 if (ring.getAtomCount() == size && isCarbonOnlyRing(ring)
                         && (isRingSaturated(ring) || isAromaticRing(ring))) c++;
             }
             return c;
         }
 
         public int countSaturatedOrAromaticNitrogenContainingRing(int size) {
             int c = 0;
             for (IAtomContainer ring : ringSet.atomContainers()) {
                 if (ring.getAtomCount() == size && (isRingSaturated(ring) || isAromaticRing(ring))
                         && countNitrogenInRing(ring) > 0) ++c;
             }
             return c;
         }
 
         public int countSaturatedOrAromaticHeteroContainingRing(int size) {
             int c = 0;
             for (IAtomContainer ring : ringSet.atomContainers()) {
                 if (ring.getAtomCount() == size && (isRingSaturated(ring) || isAromaticRing(ring))
                         && countHeteroInRing(ring) > 0) ++c;
             }
             return c;
         }
 
         public int countUnsaturatedCarbonOnlyRing(int size) {
             int c = 0;
             for (IAtomContainer ring : ringSet.atomContainers()) {
                 if (ring.getAtomCount() == size && isRingUnsaturated(ring) && !isAromaticRing(ring)
                         && isCarbonOnlyRing(ring)) ++c;
             }
             return c;
         }
 
         public int countUnsaturatedNitrogenContainingRing(int size) {
             int c = 0;
             for (IAtomContainer ring : ringSet.atomContainers()) {
                 if (ring.getAtomCount() == size && isRingUnsaturated(ring) && !isAromaticRing(ring)
                         && countNitrogenInRing(ring) > 0) ++c;
             }
             return c;
         }
 
         public int countUnsaturatedHeteroContainingRing(int size) {
             int c = 0;
             for (IAtomContainer ring : ringSet.atomContainers()) {
                 if (ring.getAtomCount() == size && isRingUnsaturated(ring) && !isAromaticRing(ring)
                         && countHeteroInRing(ring) > 0) ++c;
             }
             return c;
         }
     }
 
     class CountSubstructures {
 
         private IAtomContainer mol;
 
         public CountSubstructures(IAtomContainer m) {
             mol = m;
         }
 
         public int countSubstructure(String smarts) throws CDKException {
             SmartsPattern ptrn = cache.get(smarts);
             if (ptrn == null) {
                 ptrn = SmartsPattern.create(smarts);
                 ptrn.setPrepare(false);
                 cache.put(smarts, ptrn);
             }
             return ptrn.matchAll(mol).countUnique();
         }
     }
 
     private void _generateFp(byte[] fp, IAtomContainer mol) throws CDKException {
         SmartsPattern.prepare(mol);
         countElements(fp, mol);
         countRings(fp, mol);
         countSubstructures(fp, mol);
     }
 
     private void generateFp(IAtomContainer mol) throws CDKException {
         for (int i = 0; i < m_bits.length; ++i) {
             m_bits[i] = 0;
         }
         _generateFp(m_bits, mol);
     }
 
     private boolean isBitOn(int bit) {
         return (m_bits[bit >> 3] & MASK[bit % 8]) != 0;
     }
 
     /**
      * Returns the fingerprint generated for a molecule as a byte[].
      * 
      * Note that this should be immediately called after calling
      * {@link #getBitFingerprint(org.openscience.cdk.interfaces.IAtomContainer)}
      *
      * @return The fingerprint as a byte array
      * @see #getBitFingerprint(org.openscience.cdk.interfaces.IAtomContainer)
      */
     public byte[] getFingerprintAsBytes() {
         return m_bits;
     }
 
 
/** A Base64 encoded Pubchem fingerprints is returned. */
 public static BitSet decode(String enc){}

 

}