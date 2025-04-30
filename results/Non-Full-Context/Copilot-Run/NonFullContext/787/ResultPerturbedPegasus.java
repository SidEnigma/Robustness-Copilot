/* Copyright (C) 2007  Miguel Rojasch <miguelrojasch@users.sf.net>
  *               2014  Mark B Vine (orcid:0000-0002-7794-0426)
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
  *  */
 package org.openscience.cdk.tools.manipulator;
 
 import org.openscience.cdk.CDK;
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.config.AtomTypeFactory;
 import org.openscience.cdk.config.Elements;
 import org.openscience.cdk.config.IsotopeFactory;
 import org.openscience.cdk.config.Isotopes;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.*;
 import org.openscience.cdk.sgroup.Sgroup;
 import org.openscience.cdk.sgroup.SgroupType;
 import org.openscience.cdk.tools.LoggingToolFactory;
 
 import java.io.IOException;
 import java.util.*;
 
 /**
  * Class with convenience methods that provide methods to manipulate
  * {@link IMolecularFormula}'s. For example:
  *
  *
  * @cdk.module  formula
  * @author      miguelrojasch
  * @cdk.created 2007-11-20
  * @cdk.githash
  */
 public class MolecularFormulaManipulator {
 
     /**
      * For use with {@link #getMass(IMolecularFormula)}. This option uses the mass
      * stored on atoms ({@link IAtom#getExactMass()}) or the average mass of the
      * element when unspecified.
      */
     public static final int MolWeight                = AtomContainerManipulator.MolWeight;
 
     /**
      * For use with {@link #getMass(IMolecularFormula)}. This option ignores the
      * mass stored on atoms ({@link IAtom#getExactMass()}) and uses the average
      * mass of each element. This option is primarily provided for backwards
      * compatibility.
      */
     public static final int MolWeightIgnoreSpecified = AtomContainerManipulator.MolWeightIgnoreSpecified;
 
     /**
      * For use with {@link #getMass(IMolecularFormula)}. This option uses the mass
      * stored on atoms {@link IAtom#getExactMass()} or the mass of the major
      * isotope when this is not specified.
      */
     public static final int MonoIsotopic             = AtomContainerManipulator.MonoIsotopic;
 
     /**
      * For use with {@link #getMass(IMolecularFormula)}. This option uses the mass
      * stored on atoms {@link IAtom#getExactMass()} and then calculates a
      * distribution for any unspecified atoms and uses the most abundant
      * distribution. For example C<sub>6</sub>Br<sub>6</sub> would have three
      * <sup>79</sup>Br and <sup>81</sup>Br because their abundance is 51 and
      * 49%.
      */
     public static final int MostAbundant             = AtomContainerManipulator.MostAbundant;
 
     public static final Comparator<IIsotope> NAT_ABUN_COMP = new Comparator<IIsotope>() {
         @Override
         public int compare(IIsotope o1, IIsotope o2) {
             return -Double.compare(o1.getNaturalAbundance(),
                                    o2.getNaturalAbundance());
         }
     };
 
     /**
      *  Checks a set of Nodes for the occurrence of each isotopes
      *  instance in the molecular formula. In short number of atoms.
      *
      * @param   formula  The MolecularFormula to check
      * @return           The occurrence total
      */
     public static int getAtomCount(IMolecularFormula formula) {
 
         int count = 0;
         for (IIsotope isotope : formula.isotopes()) {
             count += formula.getIsotopeCount(isotope);
         }
         return count;
     }
 
     /**
      * Checks a set of Nodes for the occurrence of the isotopes in the
      * molecular formula from a particular IElement. It returns 0 if the
      * element does not exist. The search is based only on the IElement.
      *
      * @param   formula The MolecularFormula to check
      * @param   element The IElement object
      * @return          The occurrence of this element in this molecular formula
      */
     public static int getElementCount(IMolecularFormula formula, IElement element) {
 
         int count = 0;
         for (IIsotope isotope : formula.isotopes()) {
             if (isotope.getSymbol().equals(element.getSymbol())) count += formula.getIsotopeCount(isotope);
         }
         return count;
     }
 
     /**
      * Occurrences of a given element from an isotope in a molecular formula.
      *
      * @param  formula the formula
      * @param  isotope isotope of an element
      * @return         number of the times the element occurs
      * @see #getElementCount(IMolecularFormula, IElement)
      */
     public static int getElementCount(IMolecularFormula formula, IIsotope isotope) {
         return getElementCount(formula, formula.getBuilder().newInstance(IElement.class, isotope));
     }
 
     /**
      * Occurrences of a given element in a molecular formula.
      *
      * @param  formula the formula
      * @param  symbol  element symbol (e.g. C for carbon)
      * @return         number of the times the element occurs
      * @see #getElementCount(IMolecularFormula, IElement)
      */
     public static int getElementCount(IMolecularFormula formula, String symbol) {
         return getElementCount(formula, formula.getBuilder().newInstance(IElement.class, symbol));
     }
 
     /**
      * Get a list of IIsotope from a given IElement which is contained
      * molecular. The search is based only on the IElement.
      *
      * @param   formula The MolecularFormula to check
      * @param   element The IElement object
      * @return          The list with the IIsotopes in this molecular formula
      */
     public static List<IIsotope> getIsotopes(IMolecularFormula formula, IElement element) {
 
         List<IIsotope> isotopeList = new ArrayList<IIsotope>();
         for (IIsotope isotope : formula.isotopes()) {
             if (isotope.getSymbol().equals(element.getSymbol())) isotopeList.add(isotope);
 
         }
         return isotopeList;
     }
 
     /**
      *  Get a list of all Elements which are contained
      *  molecular.
      *
      *@param   formula The MolecularFormula to check
      *@return          The list with the IElements in this molecular formula
      */
     public static List<IElement> elements(IMolecularFormula formula) {
 
         List<IElement> elementList = new ArrayList<IElement>();
         List<String> stringList = new ArrayList<String>();
         for (IIsotope isotope : formula.isotopes()) {
             if (!stringList.contains(isotope.getSymbol())) {
                 elementList.add(isotope);
                 stringList.add(isotope.getSymbol());
             }
 
         }
         return elementList;
     }
 
     /**
      * True, if the MolecularFormula contains the given element as IIsotope object.
      *
      * @param  formula   IMolecularFormula molecularFormula
      * @param  element   The element this MolecularFormula is searched for
      * @return           True, if the MolecularFormula contains the given element object
      */
     public static boolean containsElement(IMolecularFormula formula, IElement element) {
 
         for (IIsotope isotope : formula.isotopes()) {
             if (element.getSymbol().equals(isotope.getSymbol())) return true;
         }
 
         return false;
     }
 
 
/** All elements in the MolecularFormula are removed. */
 public static IMolecularFormula removeElement(IMolecularFormula formula, IElement element){
        return removeElement(formula, element.getSymbol());
    }
    
        /**
        * All elements with the given symbol are removed.
        *
        * @param  formula   IMolecularFormula molecularFormula
        * @param  symbol    The symbol this MolecularFormula is searched for
        * @return           The MolecularFormula without the given element
        */
        public static IMolecularFormula removeElement(IMolecularFormula formula, String symbol) {
    
            for (IIsotope isotope : formula.isotopes()) {
                if (isotope.getSymbol().equals(symbol)) {
                    formula.removeIsotope(isotope);
                }
            }
            return formula;
        }
    
        /**
        * All elements in the MolecularFormula are removed.
        *
        * @param  formula   IMolecularFormula molecularFormula
        * @param  isotope   The isotope this MolecularFormula is searched for
        * @return           The MolecularFormula without the given element
        */
        public static IMolecularFormula removeElement(IMolecularFormula formula, IIsotope isotope) {
    
            formula.removeIsotope(isotope);
            return formula;
        }
    
        /**
        * All elements in the MolecularFormula are removed.
        *
        * @param  formula   IMolecularFormula molecularFormula
        * @param  isotope   The isotope this MolecularFormula is searched for
        * @return           The MolecularFormula without the given element
        */
        public static IMolecularFormula removeElement(IMolecularFormula formula, int isotope) {
    
            formula.removeIsotope(isotope);
            return formula;
        }
    
        /**
        * All elements in the MolecularFormula are removed.
        *
        * @param  formula   IMolecularFormula molecularFormula
        * @param  isotope   The isotope this MolecularFormula is searched for
        * @return           The MolecularFormula without the given element
        */
        public static IMolecularForm        
 }

 

}