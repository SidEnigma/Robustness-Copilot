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
 
 
/** Return the pattern normalized to the highest amount. */

    public static IsotopePattern normalize(IsotopePattern isotopeP) {
        // Get the list of isotopes from the input pattern
        List<Isotope> isotopes = isotopeP.getIsotopes();

        // Sort the isotopes based on their amount in descending order
        Collections.sort(isotopes, new Comparator<Isotope>() {
            @Override
            public int compare(Isotope o1, Isotope o2) {
                return Double.compare(o2.getAmount(), o1.getAmount());
            }
        });

        // Get the highest amount from the first isotope
        double highestAmount = isotopes.get(0).getAmount();

        // Normalize the amounts of all isotopes to the highest amount
        for (Isotope isotope : isotopes) {
            double normalizedAmount = isotope.getAmount() / highestAmount;
            isotope.setAmount(normalizedAmount);
        }

        // Create a new IsotopePattern with the normalized isotopes
        IsotopePattern normalizedPattern = new IsotopePattern();
        normalizedPattern.setIsotopes(isotopes);

        return normalizedPattern;
    }
 

}