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
 
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.Objects;
 
 import org.openscience.cdk.interfaces.IIsotope;
 
 /**
  *  Class defining a expanded molecular formula object. The Isotopes don't have
  *  a fix occurrence in the MolecularFormula but they have a range.<p>
  *  With this class man can define a MolecularFormula which contains certain IIsotope
  *  with a maximum and minimum occurrence.
  *
  *  Examples:
  * <ul>
  *   <li><code>[C(1-5)H(4-10)]-</code></li>
  * </ul>
  *
  * @cdk.module  formula
  * @author      miguelrojasch
  * @cdk.created 2007-11-20
  * @cdk.keyword molecular formula
  * @cdk.githash
  */
 public class MolecularFormulaRange implements Cloneable {
 
     private Map<IIsotope, Integer> isotopesMax;
     private Map<IIsotope, Integer> isotopesMin;
 
     /**
      *  Constructs an empty MolecularFormulaExpand.
      */
     public MolecularFormulaRange() {
         isotopesMax = new HashMap<IIsotope, Integer>();
         isotopesMin = new HashMap<IIsotope, Integer>();
     }
 
 
/** Adds to the MolecularFormulaExpand the isotope in a number that represents the maximum and minimum occurrences allowed. */
 public void addIsotope(IIsotope isotope, int countMin, int countMax){}

 

}