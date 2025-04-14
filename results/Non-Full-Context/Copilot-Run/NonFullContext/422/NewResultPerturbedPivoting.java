package org.openscience.cdk.formula;
 
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.List;
 
 /**
  * Class to manipulate IsotopePattern objects.
  *
  * @author Miguel Rojas Cherto
  *
  * @cdk.module  formula
  * @cdk.githash
  */
 public class IsotopePatternManipulator {
 
     /**
      * Return the isotope pattern normalized to the highest abundance.
      *
      * @param isotopeP  The IsotopePattern object to normalize
      * @return          The IsotopePattern normalized
      */
     public static IsotopePattern normalize(IsotopePattern isotopeP) {
         IsotopeContainer isoHighest = null;
 
         double biggestAbundance = 0;
         /* Extraction of the isoContainer with the highest abundance */
         for (IsotopeContainer isoContainer : isotopeP.getIsotopes()) {
             double abundance = isoContainer.getIntensity();
             if (biggestAbundance < abundance) {
                 biggestAbundance = abundance;
                 isoHighest = isoContainer;
             }
         }
         /* Normalize */
         IsotopePattern isoNormalized = new IsotopePattern();
         for (IsotopeContainer isoContainer : isotopeP.getIsotopes()) {
             double inten = isoContainer.getIntensity() / isoHighest.getIntensity();
             IsotopeContainer icClone;
             try {
                 icClone = (IsotopeContainer) isoContainer.clone();
                 icClone.setIntensity(inten);
                 if (isoHighest.equals(isoContainer))
                     isoNormalized.setMonoIsotope(icClone);
                 else
                     isoNormalized.addIsotope(icClone);
 
             } catch (CloneNotSupportedException e) {
                 e.printStackTrace();
             }
 
         }
         isoNormalized.setCharge(isotopeP.getCharge());
         return isoNormalized;
     }
 
     /**
      * Return the isotope pattern sorted and normalized by intensity
      * to the highest abundance.
      *
      * @param isotopeP  The IsotopePattern object to sort
      * @return          The IsotopePattern sorted
      */
     public static IsotopePattern sortAndNormalizedByIntensity(IsotopePattern isotopeP) {
         IsotopePattern isoNorma = normalize(isotopeP);
         return sortByIntensity(isoNorma);
     }
 
 
/** Bring the intensity-sorted isotopic pattern back to the greatest abundance. */

public static IsotopePattern sortByIntensity(IsotopePattern isotopeP) {
    List<IsotopeContainer> isotopes = isotopeP.getIsotopes();
    
    // Sort the isotopes based on intensity in descending order
    Collections.sort(isotopes, new Comparator<IsotopeContainer>() {
        @Override
        public int compare(IsotopeContainer iso1, IsotopeContainer iso2) {
            return Double.compare(iso2.getIntensity(), iso1.getIntensity());
        }
    });
    
    // Create a new IsotopePattern object to store the sorted isotopes
    IsotopePattern sortedPattern = new IsotopePattern();
    
    // Add the isotopes to the sorted pattern
    for (IsotopeContainer isoContainer : isotopes) {
        sortedPattern.addIsotope(isoContainer);
    }
    
    // Set the charge of the sorted pattern
    sortedPattern.setCharge(isotopeP.getCharge());
    
    return sortedPattern;
}
 

}