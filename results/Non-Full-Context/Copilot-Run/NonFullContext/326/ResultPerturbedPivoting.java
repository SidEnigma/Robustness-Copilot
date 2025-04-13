/* $Revision$ $Author$ $Date$
  *
  * Copyright (c) 2015 Collaborative Drug Discovery, Inc. <alex@collaborativedrug.com>
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
 
 package org.openscience.cdk.fingerprint.model;
 
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.fingerprint.CircularFingerprinter;
 import org.openscience.cdk.interfaces.IAtomContainer;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.StringReader;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Comparator;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Set;
 import java.util.TreeSet;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 /**
  *  <ul>
  *	<li>Bayesian models using fingerprints: provides model creation, analysis, prediction and
  *  serialisation.</li>
  *  
  *  <li>Uses a variation of the classic Bayesian model, using a Laplacian correction, which sums log
  *  values of ratios rather than multiplying them together. This is an effective way to work with large
  *  numbers of fingerprints without running into extreme numerical precision issues, but it also means
  *  that the outgoing predictor is an arbitrary value rather than a probability, which introduces
  *  the need for an additional calibration step prior to interpretation.</li>
  *  
  *  <li>For more information about the method, see:
  *		J. Chem. Inf. Model, v.46, pp.1124-1133 (2006)
  *		J. Biomol. Screen., v.10, pp.682-686 (2005)
  *		Molec. Divers., v.10, pp.283-299 (2006)</li>
  *
  *  <li>Currently only the CircularFingerprinter fingerprints are supported (i.e. ECFP_n and FCFP_n).</li>
  *
  *  <li>Model building is done by selecting the fingerprinting method and folding size, then providing
  *  a series of molecules &amp; responses. Individual model contributions are kept around in order to
  *  produce the analysis data (e.g. the ROC curve), but is discarded during serialise/deserialise
  *  cycles.
  *  
  *  <li>Fingerprint "folding" is optional, but recommended, because it places an upper limit on the model
  *  size. If folding is not used (folding=0) then the entire 32-bits are used, which means that in the
  *  diabolical case, the number of Bayesian contributions that needs to be stored is 4 billion. In practice
  *  the improvement in predictivity tends to plateaux out at around 1024 bits, so values of 2048 or 4096
  *  are generally safe. Folding values must be integer powers of 2.</li>
  *
  *  </ul>
  *  
  * @author         am.clark
  * @cdk.created    2015-01-05
  * @cdk.keyword    fingerprint
  * @cdk.keyword    bayesian
  * @cdk.keyword    model
  * @cdk.module     standard
  * @cdk.githash
  */
 
 public class Bayesian {
 
     private int                    classType;
     private int                    folding      = 0;
 
     // incoming hash codes: actual values, and subsumed values are {#active,#total}
     private int                    numActive    = 0;
     protected Map<Integer, int[]>  inHash       = new HashMap<Integer, int[]>();
     protected ArrayList<int[]>     training     = new ArrayList<int[]>();
     protected ArrayList<Boolean>   activity     = new ArrayList<Boolean>();
 
     // built model: contributions for each hash code
     protected Map<Integer, Double> contribs     = new HashMap<Integer, Double>();
     protected double               lowThresh    = 0, highThresh = 0;
     protected double               range        = 0, invRange = 0;                            // cached to speed up scaling calibration
 
     // self-validation metrics: can optionally be calculated after a build
     protected double[]             estimates    = null;
     protected float[]              rocX         = null, rocY = null;                          // between 0..1, and rounded to modest precision
     protected String               rocType      = null;
     protected double               rocAUC       = Double.NaN;
     protected int                  trainingSize = 0, trainingActives = 0;                     // this is serialised, while the actual training set is not
 
     // optional text attributes (serialisable)
     private String                 noteTitle    = null, noteOrigin = null;
     private String[]               noteComments = null;
     private boolean optPerceiveStereo = false;
 
     private static final Pattern   PTN_HASHLINE = Pattern.compile("^(-?\\d+)=([\\d\\.Ee-]+)");
 
     // ----------------- public methods -----------------
 
     /**
      * Instantiate a Bayesian model with no data.
      * 
      * @param classType one of the CircularFingerprinter.CLASS_* constants
      */
     public Bayesian(int classType) {
         this.classType = classType;
     }
 
     /**
      * Instantiate a Bayesian model with no data.
      * 	 * @param classType one of the CircularFingerprinter.CLASS_* constants
      * @param folding the maximum number of fingerprint bits, which must be a power of 2 (e.g. 1024, 2048) or 0 for no folding
      */
     public Bayesian(int classType, int folding) {
         this.classType = classType;
         this.folding = folding;
 
         // make sure the folding is valid
         boolean bad = false;
         if (folding > 0) for (int f = folding; f > 0; f = f >> 1)
             if ((f & 1) == 1 && f != 1) {
                 bad = true;
                 break;
             }
         if (folding < 0 || bad)
             throw new ArithmeticException("Fingerprint folding " + folding + " invalid: must be 0 or power of 2.");
     }
 
     /**
      * Sets whether stereochemistry should be re-perceived from 2D/3D
      * coordinates. By default stereochemistry encoded as {@link org.openscience.cdk.interfaces.IStereoElement}s
      * are used.
      *
      * @param val perceived from 2D
      */
     public void setPerceiveStereo(boolean val) {
         this.optPerceiveStereo = val;
     }
     
     /**
      * Access to the fingerprint type.
      * 
      * @return fingerprint class, one of CircularFingerprinter.CLASS_*
      */
     public int getClassType() {
         return classType;
     }
 
     /**
      * Access to the fingerprint folding extent.
      * 
      * @return folding extent, either 0 (for none) or a power of 2
      */
     public int getFolding() {
         return folding;
     }
 
     /**
      * Appends a new row to the model source data, which consists of a molecule and whether or not it
      * is considered active.
      * 
      * @param mol molecular structure, which must be non-blank
      * @param active whether active or not
      */
     public void addMolecule(IAtomContainer mol, boolean active) throws CDKException {
         if (mol == null || mol.getAtomCount() == 0) throw new CDKException("Molecule cannot be blank or null.");
 
         CircularFingerprinter circ = new CircularFingerprinter(classType);
         circ.setPerceiveStereo(optPerceiveStereo);
         circ.calculate(mol);
 
         // gather all of the (folded) fingerprints into a sorted set
         final int AND_BITS = folding - 1; // e.g. 1024/0x400 -> 1023/0x3FF: chop off higher order bits
         Set<Integer> hashset = new TreeSet<Integer>();
         for (int n = circ.getFPCount() - 1; n >= 0; n--) {
             int code = circ.getFP(n).hashCode;
             if (folding > 0) code &= AND_BITS;
             hashset.add(code);
         }
 
         // convert the set into a sorted primitive array
         int[] hashes = new int[hashset.size()];
         int p = 0;
         for (Integer h : hashset)
             hashes[p++] = h;
 
         // record the processed information for model building purposes		
         if (active) numActive++;
         training.add(hashes);
         activity.add(active);
         for (int h : hashes) {
             int[] stash = inHash.get(h);
             if (stash == null) stash = new int[]{0, 0};
             if (active) stash[0]++;
             stash[1]++;
             inHash.put(h, stash);
         }
     }
 
 
/** Perform this Bayesian model generation, using the {molecule:activity} pairs that have been submitted so far. */
 public void build() throws CDKException{
        // build the model
        buildModel();
    
        // calculate self-validation metrics
        calculateEstimates();
    }
    
        /**
        * Access to the model's contribution estimates.
        * 
        * @return an array of contribution estimates, one for each hash code
        */
        public double[] getEstimates() {
            return estimates;
        }
    
        /**
        * Access to the model's ROC curve.
        * 
        * @return an array of ROC X values, one for each hash code
        */
        public float[] getROCX() {
            return rocX;
        }
    
        /**
        * Access to the model's ROC curve.
        * 
        * @return an array of ROC Y values, one for each hash code
        */
        public float[] getROCY() {
            return rocY;
        }
    
        /**
        * Access to the model's ROC type.
        * 
        * @return the ROC type, one of "AUC", "ROC", "PRC", or null
        */
        public String getROCType() {
            return rocType;
        }
    
        /**
        * Access to the model's ROC AUC.
        * 
        * @return the ROC AUC, or NaN if not available
        */
        public double getROCAUC() {
            return rocAUC;
        }
    
        /**
        * Access to the model's training set size.
        * 
        * @return the training set size
        */
        public int getTrainingSize() {
            return trainingSize;
        }
    
        /**
        * Access to the model's training set active size.
        * 
        * @return the training set active size
        */
        public int getTrainingActives() {
            return trainingActives;
        }
    
        /**
        * Access to the model's note title.
        * 
        * @return the note title, or null if not available
        */
        public String getNoteTitle() {      
 }

 

}