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
 
 
/** Restore the standardized isotopic pattern to the highest abundance. */

    public static IsotopePattern normalize(IsotopePattern isotopeP) {
        List<Isotope> isotopes = isotopeP.getIsotopes();

        // Sort the isotopes in descending order of abundance
        Collections.sort(isotopes, new Comparator<Isotope>() {
            @Override
            public int compare(Isotope isotope1, Isotope isotope2) {
                return Double.compare(isotope2.getAbundance(), isotope1.getAbundance());
            }
        });

        // Set the highest abundance isotope as the first element
        Isotope highestAbundanceIsotope = isotopes.get(0);
        isotopes.remove(0);
        isotopes.add(0, highestAbundanceIsotope);

        // Create a new IsotopePattern with the normalized isotopes
        IsotopePattern normalizedPattern = new IsotopePattern(isotopes);

        return normalizedPattern;
    }
 

}