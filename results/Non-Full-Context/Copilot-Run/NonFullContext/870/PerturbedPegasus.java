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
 
     /**
      * Removes all isotopes from a given element in the MolecularFormula.
      *
      * @param  formula   IMolecularFormula molecularFormula
      * @param  element   The IElement of the IIsotopes to be removed
      * @return           The molecularFormula with the isotopes removed
      */
     public static IMolecularFormula removeElement(IMolecularFormula formula, IElement element) {
         for (IIsotope isotope : getIsotopes(formula, element)) {
             formula.removeIsotope(isotope);
         }
         return formula;
     }
 
     /**
      * Returns the string representation of the molecular formula.
      *
      * @param formula       The IMolecularFormula Object
      * @param orderElements The order of Elements
      * @param setOne        True, when must be set the value 1 for elements with
      *                      one atom
      * @return A String containing the molecular formula
      * @see #getHTML(IMolecularFormula)
      * @see #generateOrderEle()
      * @see #generateOrderEle_Hill_NoCarbons()
      * @see #generateOrderEle_Hill_WithCarbons()
      */
     public static String getString(IMolecularFormula formula, String[] orderElements,
                                    boolean setOne) {
         return getString(formula, orderElements, setOne, true);
     }
 
     private static void appendElement(StringBuilder sb, Integer mass, int elem, int count) {
         String symbol = Elements.ofNumber(elem).symbol();
         if (symbol.isEmpty())
             symbol = "R";
         if (mass != null)
             sb.append('[')
               .append(mass)
               .append(']')
               .append(symbol);
         else
             sb.append(symbol);
         if (count != 0)
             sb.append(count);
     }
 
     /**
      * Returns the string representation of the molecular formula.
      *
      * @param formula       The IMolecularFormula Object
      * @param orderElements The order of Elements
      * @param setOne        True, when must be set the value 1 for elements with
      *                      one atom
      * @param setMassNumber If the formula contains an isotope of an element that is the
      *                      non-major isotope, the element is represented as <code>[XE]</code> where
      *                      <code>X</code> is the mass number and <code>E</code> is the element symbol
      * @return A String containing the molecular formula
      * @see #getHTML(IMolecularFormula)
      * @see #generateOrderEle()
      * @see #generateOrderEle_Hill_NoCarbons()
      * @see #generateOrderEle_Hill_WithCarbons()
      */
     public static String getString(IMolecularFormula formula, String[] orderElements,
                                    boolean setOne, boolean setMassNumber) {
         StringBuilder  stringMF     = new StringBuilder();
         List<IIsotope> isotopesList = putInOrder(orderElements, formula);
         Integer q = formula.getCharge();
 
         if (q != null && q != 0)
             stringMF.append('[');
 
         if (!setMassNumber) {
             int count = 0;
             int prev  = -1;
             for (IIsotope isotope : isotopesList) {
                 if (!Objects.equals(isotope.getAtomicNumber(), prev)) {
                     if (count != 0)
                         appendElement(stringMF,
                                       null, prev,
                                       setOne || count != 1 ? count : 0);
                     prev   = isotope.getAtomicNumber();
                     count  = formula.getIsotopeCount(isotope);
                 } else
                     count += formula.getIsotopeCount(isotope);
             }
             if (count != 0)
                 appendElement(stringMF,
                               null, prev,
                               setOne || count != 1 ? count : 0);
         } else {
             for (IIsotope isotope : isotopesList) {
                 int count = formula.getIsotopeCount(isotope);
                 appendElement(stringMF,
                               isotope.getMassNumber(), isotope.getAtomicNumber(),
                               setOne || count != 1 ? count : 0);
             }
         }
 
 
         if (q != null && q != 0) {
             stringMF.append(']');
             if (q > 0) {
                 if (q > 1)
                     stringMF.append(q);
                 stringMF.append('+');
             } else {
                 if (q < -1)
                     stringMF.append(-q);
                 stringMF.append('-');
             }
         }
 
         return stringMF.toString();
     }
 
     /**
      * Returns the string representation of the molecular formula.
      * Based on Hill System. The Hill system is a system of writing
      * chemical formulas such that the number of carbon atoms in a
      * molecule is indicated first, the number of hydrogen atoms next,
      * and then the number of all other chemical elements subsequently,
      * in alphabetical order. When the formula contains no carbon, all
      * the elements, including hydrogen, are listed alphabetically.
      *
      * @param  formula  The IMolecularFormula Object
      * @return          A String containing the molecular formula
      *
      * @see #getHTML(IMolecularFormula)
      */
     public static String getString(IMolecularFormula formula) {
 
         return getString(formula, false);
     }
 
     /**
      * Returns the string representation of the molecular formula.
      * Based on Hill System. The Hill system is a system of writing
      * chemical formulas such that the number of carbon atoms in a
      * molecule is indicated first, the number of hydrogen atoms next,
      * and then the number of all other chemical elements subsequently,
      * in alphabetical order. When the formula contains no carbon, all
      * the elements, including hydrogen, are listed alphabetically.
      *
      * @param  formula  The IMolecularFormula Object
      * @param  setOne   True, when must be set the value 1 for elements with
      * 					one atom
      * @return          A String containing the molecular formula
      *
      * @see #getHTML(IMolecularFormula)
      */
     public static String getString(IMolecularFormula formula, boolean setOne) {
 
         if (containsElement(formula, formula.getBuilder().newInstance(IElement.class, "C")))
             return getString(formula, generateOrderEle_Hill_WithCarbons(), setOne, false);
         else
             return getString(formula, generateOrderEle_Hill_NoCarbons(), setOne, false);
     }
 
 
     /**
      * Returns the string representation of the molecular formula.
      * Based on Hill System. The Hill system is a system of writing
      * chemical formulas such that the number of carbon atoms in a
      * molecule is indicated first, the number of hydrogen atoms next,
      * and then the number of all other chemical elements subsequently,
      * in alphabetical order. When the formula contains no carbon, all
      * the elements, including hydrogen, are listed alphabetically.
      *
      * @param  formula  The IMolecularFormula Object
      * @param  setOne   True, when must be set the value 1 for elements with
      * 					one atom
      * @param setMassNumber If the formula contains an isotope of an element that is the
      *                      non-major isotope, the element is represented as <code>[XE]</code> where
      *                      <code>X</code> is the mass number and <code>E</code> is the element symbol
      * @return          A String containing the molecular formula
      *
      * @see #getHTML(IMolecularFormula)
      */
     public static String getString(IMolecularFormula formula, boolean setOne, boolean setMassNumber) {
 
         if (containsElement(formula, formula.getBuilder().newInstance(IElement.class, "C")))
             return getString(formula, generateOrderEle_Hill_WithCarbons(), setOne, setMassNumber);
         else
             return getString(formula, generateOrderEle_Hill_NoCarbons(), setOne, setMassNumber);
     }
 
     public static List<IIsotope> putInOrder(String[] orderElements, IMolecularFormula formula) {
         List<IIsotope> isotopesList = new ArrayList<IIsotope>();
         for (String orderElement : orderElements) {
             IElement element = formula.getBuilder().newInstance(IElement.class, orderElement);
             if (containsElement(formula, element)) {
                 List<IIsotope> isotopes = getIsotopes(formula, element);
                 Collections.sort(isotopes,
                                  new Comparator<IIsotope>() {
                                      @Override
                                      public int compare(IIsotope a,
                                                         IIsotope b) {
                                          Integer aMass = a.getMassNumber();
                                          Integer bMass = b.getMassNumber();
                                          if (aMass == null)
                                              return -1;
                                          if (bMass == null)
                                              return +1;
                                          return aMass.compareTo(bMass);
                                      }
                                  });
                 isotopesList.addAll(isotopes);
             }
         }
         return isotopesList;
     }
 
     /**
      * @deprecated  Use {@link #getString(org.openscience.cdk.interfaces.IMolecularFormula)}
      */
     @Deprecated
     public static String getHillString(IMolecularFormula formula) {
         StringBuffer hillString = new StringBuffer();
 
         Map<String, Integer> hillMap = new TreeMap<String, Integer>();
         for (IIsotope isotope : formula.isotopes()) {
             String symbol = isotope.getSymbol();
             if (hillMap.containsKey(symbol))
                 hillMap.put(symbol, hillMap.get(symbol) + formula.getIsotopeCount(isotope));
             else
                 hillMap.put(symbol, formula.getIsotopeCount(isotope));
         }
 
         // if we have a C append it and also add in the H
         // and then remove these elements
         int count;
         if (hillMap.containsKey("C")) {
             hillString.append('C');
             count = hillMap.get("C");
             if (count > 1) hillString.append(count);
             hillMap.remove("C");
             if (hillMap.containsKey("H")) {
                 hillString.append('H');
                 count = hillMap.get("H");
                 if (count > 1) hillString.append(count);
                 hillMap.remove("H");
             }
         }
 
         // now take all the rest in alphabetical order
         for (String key : hillMap.keySet()) {
             hillString.append(key);
             count = hillMap.get(key);
             if (count > 1) hillString.append(count);
         }
         return hillString.toString();
     }
 
     /**
      * Returns the string representation of the molecular formula based on Hill
      * System with numbers wrapped in &lt;sub&gt;&lt;/sub&gt; tags. Useful for
      * displaying formulae in Swing components or on the web.
      *
      *
      * @param   formula  The IMolecularFormula object
      * @return           A HTML representation of the molecular formula
      * @see              #getHTML(IMolecularFormula, boolean, boolean)
      *
      */
     public static String getHTML(IMolecularFormula formula) {
         return getHTML(formula, true, true);
     }
 
     /**
      * Returns the string representation of the molecular formula based on Hill
      * System with numbers wrapped in &lt;sub&gt;&lt;/sub&gt; tags and the
      * isotope of each Element in &lt;sup&gt;&lt;/sup&gt; tags and the total
      * charge of IMolecularFormula in &lt;sup&gt;&lt;/sup&gt; tags. Useful for
      * displaying formulae in Swing components or on the web.
      *
      *
      * @param   formula  The IMolecularFormula object
      * @param   chargeB  True, If it has to show the charge
      * @param   isotopeB True, If it has to show the Isotope mass
      * @return           A HTML representation of the molecular formula
      * @see              #getHTML(IMolecularFormula)
      *
      */
     public static String getHTML(IMolecularFormula formula, boolean chargeB, boolean isotopeB) {
         String[] orderElements;
         if (containsElement(formula, formula.getBuilder().newInstance(IElement.class, "C")))
             orderElements = generateOrderEle_Hill_WithCarbons();
         else
             orderElements = generateOrderEle_Hill_NoCarbons();
         return getHTML(formula, orderElements, chargeB, isotopeB);
     }
 
     /**
      * Returns the string representation of the molecular formula with numbers
      * wrapped in &lt;sub&gt;&lt;/sub&gt; tags and the isotope of each Element
      * in &lt;sup&gt;&lt;/sup&gt; tags and the total showCharge of IMolecularFormula
      * in &lt;sup&gt;&lt;/sup&gt; tags. Useful for displaying formulae in Swing
      * components or on the web.
      *
      *
      * @param   formula  The IMolecularFormula object
      * @param   orderElements The order of Elements
      * @param   showCharge  True, If it has to show the showCharge
      * @param   showIsotopes True, If it has to show the Isotope mass
      * @return           A HTML representation of the molecular formula
      * @see              #getHTML(IMolecularFormula)
      *
      */
     public static String getHTML(IMolecularFormula formula, String[] orderElements, boolean showCharge, boolean showIsotopes) {
         StringBuilder sb = new StringBuilder();
         for (String orderElement : orderElements) {
             IElement element = formula.getBuilder().newInstance(IElement.class, orderElement);
             if (containsElement(formula, element)) {
                 if (!showIsotopes) {
                     sb.append(element.getSymbol());
                     int n = getElementCount(formula, element);
                     if (n > 1) {
                         sb.append("<sub>").append(n).append("</sub>");
                     }
                 } else {
                     for (IIsotope isotope : getIsotopes(formula, element)) {
                         Integer massNumber = isotope.getMassNumber();
                         if (massNumber != null)
                             sb.append("<sup>").append(massNumber).append("</sup>");
                         sb.append(isotope.getSymbol());
                         int n = formula.getIsotopeCount(isotope);
                         if (n > 1) {
                             sb.append("<sub>").append(n).append("</sub>");
                         }
                     }
                 }
             }
         }
 
         if (showCharge) {
             Integer charge = formula.getCharge();
             if (charge == CDKConstants.UNSET || charge == 0) {
                 return sb.toString();
             } else {
                 sb.append("<sup>");
                 if (charge > 1 || charge < -1)
                     sb.append(Math.abs(charge));
                 if (charge > 0)
                     sb.append('+');
                 else
                     sb.append(MINUS); // note, not a hyphen!
                 sb.append("</sup>");
             }
         }
         return sb.toString();
     }
 
     /**
      * Construct an instance of IMolecularFormula, initialized with a molecular
      * formula string. The string is immediately analyzed and a set of Nodes
      * is built based on this analysis
      * <p> The hydrogens must be implicit.
      *
      * @param  stringMF   The molecularFormula string
      * @param builder a IChemObjectBuilder which is used to construct atoms
      * @return            The filled IMolecularFormula
      * @see               #getMolecularFormula(String,IMolecularFormula)
      */
     public static IMolecularFormula getMolecularFormula(String stringMF, IChemObjectBuilder builder) {
         return getMolecularFormula(stringMF, false, builder);
     }
 
     /**
      * Construct an instance of IMolecularFormula, initialized with a molecular
      * formula string. The string is immediately analyzed and a set of Nodes
      * is built based on this analysis. The hydrogens must be implicit. Major
      * isotopes are being used.
      *
      * @param  stringMF   The molecularFormula string
      * @param builder a IChemObjectBuilder which is used to construct atoms
      * @return The filled IMolecularFormula
      * @see               #getMolecularFormula(String,IMolecularFormula)
      */
     public static IMolecularFormula getMajorIsotopeMolecularFormula(String stringMF, IChemObjectBuilder builder) {
         return getMolecularFormula(stringMF, true, builder);
     }
 
     private static IMolecularFormula getMolecularFormula(String stringMF, boolean assumeMajorIsotope,
             IChemObjectBuilder builder) {
         IMolecularFormula formula = builder.newInstance(IMolecularFormula.class);
 
         return getMolecularFormula(stringMF, formula, assumeMajorIsotope);
     }
 
     private static final char HYPHEN = '-';
     private static final char MINUS  = '–';
     private static final String HYPHEN_STR = "-";
     private static final String MINUS_STR  = "–";
 
     /**
      * add in a instance of IMolecularFormula the elements extracts form
      * molecular formula string. The string is immediately analyzed and a set of Nodes
      * is built based on this analysis
      * <p> The hydrogens must be implicit.
      *
      * @param  stringMF   The molecularFormula string
      * @return            The filled IMolecularFormula
      * @see               #getMolecularFormula(String, IChemObjectBuilder)
      */
     public static IMolecularFormula getMolecularFormula(String stringMF, IMolecularFormula formula) {
         return getMolecularFormula(stringMF, formula, false);
     }
 
     private static boolean isUpper(char c) {
         return c >= 'A' && c <= 'Z';
     }
 
     private static boolean isLower(char c) {
         return c >= 'a' && c <= 'z';
     }
 
     private static boolean isDigit(char c) {
         return c >= '0' && c <= '9';
     }
 
     private static boolean isSign(char c) {
         return c == '+' || c == '-' || c == MINUS;
     }
 
     // helper class for parsing MFs
     private static final class CharIter {
         int pos;
         String str;
 
 
         public CharIter(int pos, String str) {
             this.pos = pos;
             this.str = str;
         }
 
 
         char next() {
             return pos == str.length() ? '\0' : str.charAt(pos++);
         }
 
         int nextUInt() {
             char c = next();
             if (!isDigit(c)) {
                 if (c != '\0')
                     pos--;
                 return -1;
             }
             int res = c - '0';
             while (isDigit(c = next()))
                 res = (10 * res) + (c - '0');
             if (c != '\0')
                 pos--;
             return res;
         }
 
         Elements nextElement() {
             char c1 = next();
             if (!isUpper(c1)) {
                 if (c1 != '\0') pos--;
                 return null;
             }
             char c2 = next();
             if (!isLower(c2)) {
                 if (c2 != '\0') pos--;
                 return Elements.ofString("" + c1);
             }
             return Elements.ofString("" + c1 + c2);
         }
 
         boolean nextIf(char c) {
             if (str.charAt(pos) == c) {
                 pos++;
                 return true;
             }
             return false;
         }
     }
 
     // parses an isotope from a symbol in the form:
      // ('[' <DIGIT> ']')? <UPPER> <LOWER>? <DIGIT>+?
     private static boolean parseIsotope(CharIter iter,
                                         IMolecularFormula mf,
                                         boolean setMajor) {
         Elements elem = null;
         int mass = 0;
         int count = 0;
         if (iter.nextIf('[')) {
             mass = iter.nextUInt();
             if (mass < 0)
                 return false;
             elem = iter.nextElement(); // optional
             if (!iter.nextIf(']'))
                 return false;
         }
         if (elem == null) {
             elem = iter.nextElement();
             if (elem == null)
                 return false;
         }
         count = iter.nextUInt();
         if (count < 0)
             count = 1;
         IIsotope isotope = mf.getBuilder().newInstance(IIsotope.class, elem.symbol());
         isotope.setAtomicNumber(elem.number());
         if (mass != 0)
             isotope.setMassNumber(mass);
         else if (setMajor) {
             try {
                 IIsotope major = Isotopes.getInstance().getMajorIsotope(elem.number());
                 if (major != null)
                     isotope.setMassNumber(major.getMassNumber());
             } catch (IOException ex) {
                 // ignored
             }
         }
         mf.addIsotope(isotope, count);
         return true;
     }
 
     /**
      * Add to an instance of IMolecularFormula the elements extracts form
      * molecular formula string. The string is immediately analyzed and a set of Nodes
      * is built based on this analysis. The hydrogens are assumed to be implicit.
      * The boolean indicates if the major isotope is to be assumed, or if no
      * assumption is to be made.
      *
      * @param  stringMF           The molecularFormula string
      * @param  assumeMajorIsotope If true, it will take the major isotope for each element
      * @return                    The filled IMolecularFormula
      * @see                       #getMolecularFormula(String, org.openscience.cdk.interfaces.IChemObjectBuilder)
      * @see #getMolecularFormula(String, boolean, org.openscience.cdk.interfaces.IChemObjectBuilder)
      */
     private static IMolecularFormula getMolecularFormula(String stringMF, IMolecularFormula formula,
             boolean assumeMajorIsotope) {
 
         if (stringMF.contains(".") || stringMF.contains("(") || stringMF.length() > 0 && stringMF.charAt(0) >= '0' && stringMF.charAt(0) <= '9')
             stringMF = simplifyMolecularFormula(stringMF);
 
         // Extract charge from String when contains []X- format
         Integer charge = null;
         if ((stringMF.contains("+") || stringMF.contains(HYPHEN_STR) || stringMF.contains(MINUS_STR))) {
             int pos = findChargePosition(stringMF);
             if (pos >= 0 && pos != stringMF.length()) {
                 charge = parseCharge(new CharIter(pos, stringMF));
                 stringMF = stringMF.substring(0, pos);
                 if (stringMF.charAt(0) == '[' &&
                     stringMF.charAt(stringMF.length()-1) == ']')
                     stringMF = stringMF.substring(1, stringMF.length()-1);
             }
         }
         if (stringMF.isEmpty())
             return null;
         int len = stringMF.length();
         CharIter iter = new CharIter(0, stringMF);
         iter.str = stringMF;
         while (iter.pos < len) {
             if (!parseIsotope(iter, formula, assumeMajorIsotope)) {
                 LoggingToolFactory.createLoggingTool(MolecularFormulaManipulator.class)
                     .error("Could not parse MF: " + iter.str);
                 return null;
             }
         }
 
         if (charge != null) formula.setCharge(charge);
         return formula;
     }
 
 
     private static int parseCharge(CharIter iter) {
         int sign = 0;
         int number = iter.nextUInt();
         switch (iter.next()) {
             case '+':
                 sign = +1;
                 break;
             case HYPHEN:
             case MINUS:
                 sign = -1;
                 break;
         }
         if (number < 0)
             number = iter.nextUInt();
         if (number < 0)
             number = 1;
         if (sign == 0) {
             switch (iter.next()) {
                 case '+':
                     sign = +1;
                     break;
                 case HYPHEN:
                 case MINUS:
                     sign = -1;
                     break;
             }
         }
         return sign * number;
     }
 
 
/** The charge position is given in a formula format. */
 private static int findChargePosition(String formula){}

 

}