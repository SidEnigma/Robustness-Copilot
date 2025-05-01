package org.openscience.cdk.fingerprint;
 
 
 import java.io.BufferedReader;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * Default sets of atom containers aimed for use with the substructure.
  *
  * @author egonw
  *
  * @cdk.module fingerprint
  * @cdk.githash
  */
 public class StandardSubstructureSets {
     /**
      * The functional groups.
      *
      * @return A set of the functional groups.
      * @throws Exception if there is an error parsing SMILES for the functional groups
      */
     public static String[] getFunctionalGroupSMARTS() throws Exception {
         return readSMARTSPattern("org/openscience/cdk/fingerprint/data/SMARTS_InteLigand.txt");
     }
 
     /**
      * Subset of the MACCS fingerprint definitions. The subset encompasses the pattern
      * that are countable:
      * <ul>
      *     <li>Patterns have obvious counting nature, <i>e.g., 6-Ring, C=O, etc.</i></li>
      *     <li>Patterns like <i>"Is there at least 1 of this and that?", "Are there at least 2 ..."</i> etc. are merged</li>
      *     <li>Patterns clearly corresponding to binary properties, <i>e.g., actinide group ([Ac,Th,Pa,...]), isotope, etc.,</i> have been removed.</li>
      * </ul>
      *
      *
      * @return Countable subset of the MACCS fingerprint definition
      * @throws Exception if there is an error parsing SMILES patterns
      */
     public static String[] getCountableMACCSSMARTS() throws Exception {
         return readSMARTSPattern("org/openscience/cdk/fingerprint/data/SMARTS_countable_MACCS_keys.txt");
     }
 
 
/** Get a list of smart patterns from the specified file */
 private static String[] readSMARTSPattern(String filename) throws Exception{
                    
 }

 

}