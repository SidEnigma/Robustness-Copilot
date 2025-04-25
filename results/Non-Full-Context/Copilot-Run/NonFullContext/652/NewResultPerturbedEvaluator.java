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
 
 
/** Given an IsotopePattern as input, it returns its normalized version to the highest abundance. */

    public static IsotopePattern normalize(IsotopePattern isotopeP) {
        List<Isotope> isotopes = isotopeP.getIsotopes();
        
        // Sort the isotopes in descending order based on abundance
        Collections.sort(isotopes, new Comparator<Isotope>() {
            @Override
            public int compare(Isotope isotope1, Isotope isotope2) {
                return Double.compare(isotope2.getAbundance(), isotope1.getAbundance());
            }
        });
        
        // Get the highest abundance
        double highestAbundance = isotopes.get(0).getAbundance();
        
        // Normalize the abundances to the highest abundance
        for (Isotope isotope : isotopes) {
            double normalizedAbundance = isotope.getAbundance() / highestAbundance;
            isotope.setAbundance(normalizedAbundance);
        }
        
        return isotopeP;
    }
 

}