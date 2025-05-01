/* Copyright (C) 2007  Miguel Rojasch <miguelrojasch@users.sf.net>
  *
  *  Contact: cdk-devel@lists.sourceforge.net
  *
  *  This program is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU Lesser General Public License
  *  as published by the Free Software Foundation; either version 2.1
  *  of the License, or (at your option) any later version.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU Lesser General Public License for more details.
  *
  *  You should have received a copy of the GNU Lesser General Public License
  *  along with this program; if not, write to the Free Software
  *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 package org.openscience.cdk.formula;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Objects;
 
 import org.openscience.cdk.DefaultChemObjectBuilder;
 import org.openscience.cdk.interfaces.IAdductFormula;
 import org.openscience.cdk.interfaces.IIsotope;
 import org.openscience.cdk.interfaces.IMolecularFormula;
 import org.openscience.cdk.interfaces.IMolecularFormulaSet;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 
 /**
  *  Class defining an adduct object in a MolecularFormula. It maintains
  *   a list of list IMolecularFormula.<p>
  *
  *  Examples:
  * <ul>
  *   <li><code>[C2H4O2+Na]+</code></li>
  * </ul>
  *
  * @cdk.module  data
  * @author      miguelrojasch
  * @cdk.created 2007-11-20
  * @cdk.keyword molecular formula
  * @cdk.githash
  */
 public class AdductFormula implements Iterable<IMolecularFormula>, IAdductFormula, Cloneable {
 
     /**
      * Determines if a de-serialized object is compatible with this class.
      *
      * This value must only be changed if and only if the new version
      * of this class is incompatible with the old version. See Sun docs
      * for <a href=http://java.sun.com/products/jdk/1.1/docs/guide
      * /serialization/spec/version.doc.html>details</a>.
      */
     private static final long       serialVersionUID = -811384981700039389L;
 
     /**  Internal List of IMolecularFormula. */
     private List<IMolecularFormula> components;
 
     /**
      *  Constructs an empty AdductFormula.
      *
      *  @see #AdductFormula(IMolecularFormula)
      */
     public AdductFormula() {
         components = new ArrayList<IMolecularFormula>();
     }
 
     /**
      * Constructs an AdductFormula with a copy AdductFormula of another
      * AdductFormula (A shallow copy, i.e., with the same objects as in
      * the original AdductFormula).
      *
      *  @param  formula  An MolecularFormula to copy from
      *  @see             #AdductFormula()
      */
     public AdductFormula(IMolecularFormula formula) {
         components = new ArrayList<IMolecularFormula>();
         components.add(0, formula);
     }
 
     /**
      * Adds an molecularFormula to this chemObject.
      *
      * @param  formula  The molecularFormula to be added to this chemObject
      */
     @Override
     public void addMolecularFormula(IMolecularFormula formula) {
         components.add(formula);
     }
 
     /**
      *  Adds all molecularFormulas in the AdductFormula to this chemObject.
      *
      * @param  formulaSet  The MolecularFormulaSet
      */
     @Override
     public void add(IMolecularFormulaSet formulaSet) {
 
         for (IMolecularFormula mf : formulaSet.molecularFormulas()) {
             addMolecularFormula(mf);
         }
         /*
          * notifyChanged() is called by addAtomContainer()
          */
     }
 
     /**
      *  True, if the AdductFormula contains the given IIsotope object and not
      *  the instance. The method looks for other isotopes which has the same
      *  symbol, natural abundance and exact mass.
      *
      * @param  isotope  The IIsotope this AdductFormula is searched for
      * @return          True, if the AdductFormula contains the given isotope object
      */
     @Override
     public boolean contains(IIsotope isotope) {
         for (Iterator<IIsotope> it = isotopes().iterator(); it.hasNext();) {
             IIsotope thisIsotope = it.next();
             if (isTheSame(thisIsotope, isotope)) {
                 return true;
             }
         }
         return false;
     }
 
     /**
      *  Returns the partial charge of this Adduct. If the charge
      *  has not been set the return value is Double.NaN.
      *
      * @return the charge of this Adduct
      *
      * @see    #setCharge
      */
     @Override
     public Integer getCharge() {
         Integer charge = 0;
         Iterator<IMolecularFormula> componentIterator = components.iterator();
         while (componentIterator.hasNext()) {
             charge += componentIterator.next().getCharge();
         }
         return charge;
     }
 
     /**
      *  Checks a set of Nodes for the occurrence of the isotope in the
      *  adduct formula from a particular isotope. It returns 0 if the does not exist.
      *
      * @param   isotope          The IIsotope to look for
      * @return                   The occurrence of this isotope in this adduct
      * @see                      #getIsotopeCount()
      */
     @Override
     public int getIsotopeCount(IIsotope isotope) {
         int count = 0;
         Iterator<IMolecularFormula> componentIterator = components.iterator();
         while (componentIterator.hasNext()) {
             count += componentIterator.next().getIsotopeCount(isotope);
         }
         return count;
     }
 
     /**
      *  Checks a set of Nodes for the number of different isotopes in the
      *  adduct formula.
      *
      * @return        The the number of different isotopes in this adduct formula
      * @see           #getIsotopeCount(IIsotope)
      */
     @Override
     public int getIsotopeCount() {
         return isotopesList().size();
     }
 
     /**
      *  Returns an Iterator for looping over all isotopes in this adduct formula.
      *
      * @return    An Iterator with the isotopes in this adduct formula
      */
     @Override
     public Iterable<IIsotope> isotopes() {
         return new Iterable<IIsotope>() {
 
             @Override
             public Iterator<IIsotope> iterator() {
                 return isotopesList().iterator();
             }
         };
     }
 
 
/** Returns a List for looping over all isotopes in this adduct formula. */
 private List<IIsotope> isotopesList(){
        List<IIsotope> isotopes = new ArrayList<IIsotope>();
        for (IMolecularFormula component : components) {
            for (IIsotope isotope : component.isotopes()) {
                if (!isotopes.contains(isotope)) {
                    isotopes.add(isotope);
                }
            }
        }
        return isotopes;
    }
    
        /**
        *  Returns an Iterator for looping over all molecularFormulas in this adduct formula.
        *
        * @return    An Iterator with the molecularFormulas in this adduct formula
        */
        @Override
        public Iterable<IMolecularFormula> molecularFormulas() {
            return new Iterable<IMolecularFormula>() {
    
                @Override
                public Iterator<IMolecularFormula> iterator() {
                    return components.iterator();
                }
            };
        }
    
        /**
        *  Returns the molecularFormula of this AdductFormula.
        *
        * @return    The molecularFormula of this AdductFormula
        */
        @Override
        public IMolecularFormula molecularFormula() {
            IMolecularFormula formula = new MolecularFormula();
            for (IMolecularFormula component : components) {
                formula.add(component);
            }
            return formula;
        }
    
        /**
        *  Returns the molecularFormula of this AdductFormula.
        *
        * @return    The molecularFormula of this AdductFormula
        */
        @Override
        public IMolecularFormula molecularFormula(IChemObjectBuilder builder) {
            IMolecularFormula formula = new MolecularFormula(builder);
            for (IMolecularFormula component : components) {
                formula.add(component);
            }
            return formula;
        }
    
        /**
        *  Returns the molecularFormula of this AdductFormula.
        *
        * @return    The molecularFormula of this AdductFormula
        */
        @Override
        public IMolecularFormula        
 }

 

}