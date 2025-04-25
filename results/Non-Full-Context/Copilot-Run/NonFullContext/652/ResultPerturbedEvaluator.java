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
 public static IsotopePattern normalize(IsotopePattern isotopeP){
     	IsotopePattern isotopeP_normalized = new IsotopePattern();
     	List<Isotope> isotopes = isotopeP.getIsotopes();
     	Collections.sort(isotopes, new Comparator<Isotope>() {
     		public int compare(Isotope o1, Isotope o2) {
     			return o2.getAbundance() - o1.getAbundance();
     		}
     	});
     	for (Isotope isotope : isotopes) {
     		isotopeP_normalized.addIsotope(isotope);
     	}
     	return isotopeP_normalized;     
 }

 

}