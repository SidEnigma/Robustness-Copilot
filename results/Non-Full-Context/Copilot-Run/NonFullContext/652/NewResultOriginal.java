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
 
 
/** Return the isotope pattern normalized to the highest abundance. */

    public static IsotopePattern normalize(IsotopePattern isotopeP) {
        // Get the list of isotopes from the isotope pattern
        List<Isotope> isotopes = isotopeP.getIsotopes();

        // Sort the isotopes based on their abundance in descending order
        Collections.sort(isotopes, new Comparator<Isotope>() {
            @Override
            public int compare(Isotope isotope1, Isotope isotope2) {
                return Double.compare(isotope2.getAbundance(), isotope1.getAbundance());
            }
        });

        // Get the highest abundance from the first isotope
        double highestAbundance = isotopes.get(0).getAbundance();

        // Normalize the abundances of all isotopes to the highest abundance
        for (Isotope isotope : isotopes) {
            double normalizedAbundance = isotope.getAbundance() / highestAbundance;
            isotope.setAbundance(normalizedAbundance);
        }

        // Update the isotope pattern with the normalized abundances
        isotopeP.setIsotopes(isotopes);

        return isotopeP;
    }
 

}