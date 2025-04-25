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
 public static IsotopePattern normalize(IsotopePattern isotopeP){
     	IsotopePattern isotopeP2 = new IsotopePattern();
     	for (Isotope isotope : isotopeP.getIsotopes()) {
     		isotopeP2.addIsotope(isotope);
     	}
     	isotopeP2.normalize();
     	return isotopeP2;       
 }

 

}