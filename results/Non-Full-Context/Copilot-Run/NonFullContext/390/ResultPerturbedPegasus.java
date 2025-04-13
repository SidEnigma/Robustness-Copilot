/* Copyright (c) 2014 Collaborative Drug Discovery, Inc. <alex@collaborativedrug.com>
  *
  * Implemented by Alex M. Clark, produced by Collaborative Drug Discovery, Inc.
  * Made available to the CDK community under the terms of the GNU LGPL.
  *
  *    http://collaborativedrug.com
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
 
 import java.util.AbstractMap;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.BitSet;
 import java.util.Collections;
 import java.util.List;
 import java.util.Map;
 import java.util.TreeMap;
 import java.util.zip.CRC32;
 
 import javax.vecmath.Point2d;
 import javax.vecmath.Point3d;
 
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IStereoElement;
 import org.openscience.cdk.interfaces.IElement;
 
 /**
  *  <p>Circular fingerprints: for generating fingerprints that are functionally equivalent to ECFP-2/4/6 and FCFP-2/4/6
  *  fingerprints, which are partially described by Rogers et al. {@cdk.cite Rogers2010}.
  *
  *  <p>While the literature describes the method in detail, it does not disclose either the hashing technique for converting
  *  lists of integers into 32-bit codes, nor does it describe the scheme used to classify the atom types for creating
  *  the FCFP-class of descriptors. For this reason, the fingerprints that are created are not binary compatible with
  *  the reference implementation. They do, however, achieve effectively equal performance for modelling purposes.</p>
  *
  *  <p>The resulting fingerprint bits are presented as a list of unique bits, each with a 32-bit hashcode; typically there
  *  are no more than a hundred or so unique bit hashcodes per molecule. These identifiers can be folded into a smaller
  *  array of bits, such that they can be represented as a single long binary number, which is often more convenient.</p>
  *
  *	<p>The  integer hashing is done using the CRC32 algorithm, using the Java CRC32 class, which is the same
  *	formula/parameters as used by PNG files, and described in:</p>
  *
  *		<a href="http://www.w3.org/TR/PNG/#D-CRCAppendix">http://www.w3.org/TR/PNG/#D-CRCAppendix</a>
  *
  *	<p>Implicit vs. explicit hydrogens are handled, i.e. it doesn't matter whether the incoming molecule is hydrogen
  *	suppressed or not.</p>
  *
  *  <p>Implementation note: many of the algorithms involved in the generation of fingerprints (e.g. aromaticity, atom
  *  typing) have been coded up explicitly for use by this class, rather than making use of comparable functionality
  *  elsewhere in the CDK. This is to ensure that the CDK implementation of the algorithm is strictly equal to other
  *  implementations: dependencies on CDK functionality that could be modified or improved in the future would break
  *  binary compatibility with formerly identical implementations on other platforms.</p>
  *
  *  <p>For the FCFP class of fingerprints, atom typing is done using a scheme similar to that described by
  *  Green et al {@cdk.cite Green1994}.</p>
  *  
  *  <p>The fingerprints and their uses have been described in Clark et al. {@cdk.cite Clark2014}.
  *
  * <br/>
  * <b>
  * Important! this fingerprint can not be used for substructure screening.
  * </b>
  *
  * @author         am.clark
  * @cdk.created    2014-01-01
  * @cdk.keyword    fingerprint
  * @cdk.keyword    similarity
  * @cdk.module     standard
  * @cdk.githash
  */
 public class CircularFingerprinter extends AbstractFingerprinter implements IFingerprinter {
 
     // ------------ constants ------------
 
     // identity by literal atom environment
     public static final int CLASS_ECFP0 = 1;
     public static final int CLASS_ECFP2 = 2;
     public static final int CLASS_ECFP4 = 3;
     public static final int CLASS_ECFP6 = 4;
     // identity by functional character of the atom
     public static final int CLASS_FCFP0 = 5;
     public static final int CLASS_FCFP2 = 6;
     public static final int CLASS_FCFP4 = 7;
     public static final int CLASS_FCFP6 = 8;
 
     public static final class FP {
 
         public int   hashCode;
         public int   iteration;
         public int[] atoms;
 
         public FP(int hashCode, int iteration, int[] atoms) {
             this.hashCode = hashCode;
             this.iteration = iteration;
             this.atoms = atoms;
         }
     }
     
 
     // ------------ private members ------------
 
     private final int      ATOMCLASS_ECFP = 1;
     private final int      ATOMCLASS_FCFP = 2;
 
     private IAtomContainer mol;
     private final int      length;
 
     private int[]          identity;
     private boolean[]      resolvedChiral;
     private int[][]        atomGroup;
     private CRC32          crc            = new CRC32();        // recycled for each CRC calculation
     private ArrayList<FP>  fplist         = new ArrayList<FP>();
 
     // summary information about the molecule, for quick access
     private boolean[]      amask;                               // true for all heavy atoms, i.e. hydrogens and non-elements are excluded
     private int[]          hcount;                              // total hydrogen count, including explicit and implicit hydrogens
     private int[][]        atomAdj, bondAdj;                    // precalculated adjacencies, including only those qualifying with 'amask'
     private int[]          ringBlock;                           // ring block identifier; 0=not in a ring
     private int[][]        smallRings;                          // all rings of size 3 through 7
     private int[]          bondOrder;                           // numeric bond order for easy reference
     private boolean[]      atomArom, bondArom;                  // aromaticity precalculated
     private int[][]        tetra;                               // tetrahedral rubric, a precursor to chirality
 
     // stored information for bio-typing; only defined for FCFP-class fingerprints
     private boolean[]      maskDon, maskAcc, maskPos, maskNeg, maskAro, maskHal; // functional property flags
     private int[]          bondSum;                                             // sum of bond orders for each atom (including explicit H's)
     private boolean[]      hasDouble;                                           // true if an atom has any double bonds
     private boolean[]      aliphatic;                                           // true for carbon atoms with only sigma bonds
     private boolean[]      isOxide;                                             // true if the atom has a double bond to oxygen
     private boolean[]      lonePair;                                            // true if the atom is N,O,S with octet valence and at least one lone pair
     private boolean[]      tetrazole;                                           // special flag for being in a tetrazole (C1=NN=NN1) ring
 
     // ------------ options -------------------
     private int     classType, atomClass;
     private boolean optPerceiveStereo = false;
 
     // ------------ public methods ------------
 
     /**
      * Default constructor: uses the ECFP6 type.
      */
     public CircularFingerprinter() {
         this(CLASS_ECFP6);
     }
 
     /**
      * Specific constructor: initializes with descriptor class type, one of ECFP_{p} or FCFP_{p}, where ECFP is
      * for the extended-connectivity fingerprints, FCFP is for the functional class version, and {p} is the
      * path diameter, and may be 0, 2, 4 or 6.
      *
      * @param classType one of CLASS_ECFP{n} or CLASS_FCFP{n}
      */
     public CircularFingerprinter(int classType) {
         this(classType, 1024);
     }
 
     /**
      * Specific constructor: initializes with descriptor class type, one of ECFP_{p} or FCFP_{p}, where ECFP is
      * for the extended-connectivity fingerprints, FCFP is for the functional class version, and {p} is the
      * path diameter, and may be 0, 2, 4 or 6.
      *
      * @param classType one of CLASS_ECFP{n} or CLASS_FCFP{n}
      * @param len size of folded (binary) fingerprint                  
      */
     public CircularFingerprinter(int classType, int len) {
         if (classType < 1 || classType > 8)
             throw new IllegalArgumentException("Invalid classType specified: " + classType);
         this.classType = classType;
         this.length = len;
     }
 
     /**
      * Sets whether stereochemistry should be re-perceived from 2D/3D
      * coordinates. By default stereochemistry encoded as {@link IStereoElement}s
      * are used.
      *
      * @param val perceived from 2D
      */
     public void setPerceiveStereo(boolean val) {
         this.optPerceiveStereo = val;
     }
 
     @Override
     protected List<Map.Entry<String, String>> getParameters() {
         String type = null;
         switch (classType) {
             case CLASS_ECFP0: type = "ECFP0"; break;
             case CLASS_ECFP2: type = "ECFP2"; break;
             case CLASS_ECFP4: type = "ECFP4"; break;
             case CLASS_ECFP6: type = "ECFP6"; break;
             case CLASS_FCFP0: type = "FCFP0"; break;
             case CLASS_FCFP2: type = "FCFP2"; break;
             case CLASS_FCFP4: type = "FCFP4"; break;
             case CLASS_FCFP6: type = "FCFP6"; break;
         }
         return Arrays.<Map.Entry<String, String>>asList(
             new AbstractMap.SimpleImmutableEntry<>("classType", type),
             new AbstractMap.SimpleImmutableEntry<>("perceiveStereochemistry",
                                                    Boolean.toString(optPerceiveStereo))
         );
     }
 
 
/** The fingerprints are calculated and stored for later use. */
 public void calculate(IAtomContainer mol) throws CDKException{
        this.mol = mol;
        // initialize the fingerprint list
        fplist.clear();
    
        // initialize the molecule
        initMolecule();
    
        // calculate the fingerprints
        switch (classType) {
            case CLASS_ECFP0: calculateECFP0(); break;
            case CLASS_ECFP2: calculateECFP2(); break;
            case CLASS_ECFP4: calculateECFP4(); break;
            case CLASS_ECFP6: calculateECFP6(); break;
            case CLASS_FCFP0: calculateFCFP0(); break;
            case CLASS_FCFP2: calculateFCFP2(); break;
            case CLASS_FCFP4: calculateFCFP4(); break;
            case CLASS_FCFP6: calculateFCFP6(); break;
        }
    }
    
        /**
        * Returns the calculated fingerprint.
        *
        * @return the fingerprint
        */
        public List<FP> getFingerprints() {
            return fplist;
        }
    
        /**
        * Returns the calculated fingerprint.
        *
        * @return the fingerprint
        */
        public FP getFingerprint() {
            return fplist.get(0);
        }
    
        /**
        * Returns the calculated fingerprint.
        *
        * @return the fingerprint
        */
        public FP getFingerprint(int index) {
            return fplist.get(index);
        }
    
        /**
        * Returns the calculated fingerprint.
        *
        * @return the fingerprint
        */
        public FP getFingerprint(String fingerprint) {
            for (FP fp : fplist) {
                if (fp.hashCode == fingerprint.hashCode())
                    return fp;
            }
            return null;
        }
    
        /**
        * Returns the calculated fingerprint.
        *
        * @return the fingerprint
        */
        public FP getFingerprint(int index, int iteration) {
            for (FP fp : fplist) {
                if (fp.iteration == iteration && fp.hashCode == index)
                    return f        
 }

 

}