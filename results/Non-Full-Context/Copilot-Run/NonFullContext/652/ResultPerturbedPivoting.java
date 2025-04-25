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
 public static IsotopePattern normalize(IsotopePattern isotopeP){
     	IsotopePattern isotopeP2 = new IsotopePattern();
     	isotopeP2.setAtom(isotopeP.getAtom());
     	isotopeP2.setCharge(isotopeP.getCharge());
     	isotopeP2.setMass(isotopeP.getMass());
     	isotopeP2.setMostAbundantIsotope(isotopeP.getMostAbundantIsotope());
     	isotopeP2.setMostAbundantIsotopeNumber(isotopeP.getMostAbundantIsotopeNumber());
     	isotopeP2.setMostAbundantIsotopePercentage(isotopeP.getMostAbundantIsotopePercentage());
     	isotopeP2.setMostAbundantIsotopeRelativeAbundance(isotopeP.getMostAbundantIsotopeRelativeAbundance());
     	isotopeP2.setMostAbundantIsotopeTheoreticalMass(isotopeP.getMostAbundantIsotopeTheoreticalMass());
     	isotopeP2.setMostAbundantIsotopeTheoreticalMassNumber(isotopeP.getMostAbundantIsotopeTheoreticalMassNumber());
     	isotopeP2.setMostAbundantIsotopeTheoreticalMassPercentage(isotopeP.getMostAbundantIsotopeTheoreticalMassPercentage());
     	isotopeP2.setMostAbundantIsotopeTheoreticalMassRelativeAbundance(isotopeP.getMostAbundantIsotopeTheoreticalMassRelativeAbundance());
     	isotopeP2.setMostAbundantIsotopeTheoreticalMassTheoreticalMass(isotopeP.getMostAbundantIsotopeTheoreticalMassTheoreticalMass());
     	isotopeP2.setMostAbundantIsotopeTheoreticalMassTheoreticalMassNumber(isotopeP.getMostAbundantIsotopeTheoreticalMassTheoreticalMassNumber());        
 }

 

}