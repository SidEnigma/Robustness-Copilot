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
 
 import java.util.*;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.DefaultChemObjectBuilder;
 import org.openscience.cdk.interfaces.IIsotope;
 import org.openscience.cdk.interfaces.IMolecularFormula;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 
 /**
  * Class defining a molecular formula object. It maintains
  * a list of list {@link IIsotope}.
  *
  * <p>Examples:
  * <ul>
  *   <li><code>[C<sub>5</sub>H<sub>5</sub>]-</code></li>
  *   <li><code>C<sub>6</sub>H<sub>6</sub></code></li>
  *   <li><code><sup>12</sup>C<sub>5</sub><sup>13</sup>CH<sub>6</sub></code></li>
  * </ul>
  *
  * @cdk.module  data
  * @author      miguelrojasch
  * @cdk.created 2007-11-20
  * @cdk.keyword molecular formula
  * @cdk.githash
  */
 public class MolecularFormula implements IMolecularFormula {
 
     /**
      * Determines if a de-serialized object is compatible with this class.
      *
      * This value must only be changed if and only if the new version
      * of this class is incompatible with the old version. See Sun docs
      * for <a href=http://java.sun.com/products/jdk/1.1/docs/guide
      * /serialization/spec/version.doc.html>details</a>.
      */
     private static final long      serialVersionUID = -2011407700837295287L;
 
     private Map<IIsotope, Integer> isotopes;
     /**
      *  The partial charge of the molecularFormula. The default value is Double.NaN.
      */
     private Integer                charge           = null;
 
     /**
      *  A hashtable for the storage of any kind of properties of this IChemObject.
      */
     private Map<Object, Object>    properties;
 
     /**
      *  Constructs an empty MolecularFormula.
      */
     public MolecularFormula() {
         isotopes = new HashMap<IIsotope, Integer>();
     }
 
     /**
      * Adds an molecularFormula to this MolecularFormula.
      *
      * @param  formula  The molecularFormula to be added to this chemObject
      * @return          The IMolecularFormula
      */
     @Override
     public IMolecularFormula add(IMolecularFormula formula) {
         for (IIsotope newIsotope : formula.isotopes()) {
             addIsotope(newIsotope, formula.getIsotopeCount(newIsotope));
         }
         if (formula.getCharge() != null) {
             if (charge != null)
                 charge += formula.getCharge();
             else
                 charge = formula.getCharge();
         }
         return this;
     }
 
     /**
      *  Adds an Isotope to this MolecularFormula one time.
      *
      * @param  isotope  The isotope to be added to this MolecularFormula
      * @see             #addIsotope(IIsotope, int)
      */
     @Override
     public IMolecularFormula addIsotope(IIsotope isotope) {
         return this.addIsotope(isotope, 1);
     }
 
     /**
      *  Adds an Isotope to this MolecularFormula in a number of occurrences.
      *
      * @param  isotope  The isotope to be added to this MolecularFormula
      * @param  count    The number of occurrences to add
      * @see             #addIsotope(IIsotope)
      */
     @Override
     public IMolecularFormula addIsotope(IIsotope isotope, int count) {
         if (count == 0)
             return this;
         boolean flag = false;
         for (IIsotope thisIsotope : isotopes()) {
             if (isTheSame(thisIsotope, isotope)) {
                 isotopes.put(thisIsotope, isotopes.get(thisIsotope) + count);
                 flag = true;
                 break;
             }
         }
         if (!flag) {
             isotopes.put(isotope, count);
         }
 
         return this;
     }
 
 
/** This method returns true if the MolecularFormula contains the given IIsotope object and false otherwise */
 public boolean contains(IIsotope isotope){}

 

}