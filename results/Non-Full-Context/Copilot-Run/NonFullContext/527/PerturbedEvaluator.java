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
  */
 package org.openscience.cdk.formula;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 
 import org.openscience.cdk.config.AtomTypeFactory;
 import org.openscience.cdk.config.Isotopes;
 import org.openscience.cdk.config.IsotopeFactory;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.formula.rules.ChargeRule;
 import org.openscience.cdk.formula.rules.ElementRule;
 import org.openscience.cdk.formula.rules.IRule;
 import org.openscience.cdk.formula.rules.ToleranceRangeRule;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.interfaces.IElement;
 import org.openscience.cdk.interfaces.IIsotope;
 import org.openscience.cdk.interfaces.IMolecularFormula;
 import org.openscience.cdk.interfaces.IMolecularFormulaSet;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
 import org.openscience.cdk.tools.manipulator.MolecularFormulaRangeManipulator;
 
 /**
  * <p>Tool to determine molecular formula consistent with a given accurate mass. The
  * molecular formulas are not validate. It only consist in generate combination according
  * object (see MolecularFormulaChecker). The algorithm is published in Rojas-Cherto M. et.al.
  * {@cdk.cite RojasCherto2011}.
  *
  * <pre>
  *   MassToFormulaTool mf = new MassToFormulaTool();
  *   double myMass = 133.004242;
  *   IMolecularFormulaSet mfSet = mf.generate(myMass);
  * </pre>
  *
  * <p>The elements are listed according on difference with the proposed mass.
  *
  *
  * @cdk.module  formula
  * @author      miguelrojasch
  * @cdk.created 2007-03-01
  * @cdk.githash
  * @deprecated Please use MolecularFormulaGenerator
  */
 @Deprecated
 public class MassToFormulaTool {
 
     private ILoggingTool          logger = LoggingToolFactory.createLoggingTool(MassToFormulaTool.class);
 
     private IChemObjectBuilder    builder;
 
     /** */
     AtomTypeFactory               factory;
 
     /** matrix to follow for the permutations.*/
     private int[][]               matrix_Base;
 
     /** Array listing the order of the elements to be shown according probability occurrence.*/
     private String[]              orderElements;
 
     /** A List with all rules to be applied. see IRule.*/
     private List<IRule>           rules;
     private MolecularFormulaRange mfRange;
     private Double                charge;
     private Double                tolerance;
 
     /**
      * Construct an instance of MassToFormulaTool. It is necessary because different
      * matrix have to build. Furthermore the default restrictions are initiated.
      *
      * @see #setDefaultRestrictions()
      */
     public MassToFormulaTool(IChemObjectBuilder builder) {
         this.builder = builder;
         logger.info("Initiate MassToForumlaTool");
         factory = AtomTypeFactory.getInstance(builder);
         this.orderElements = generateOrderE();
 
         setDefaultRestrictions();
 
     }
 
     /**
      * Set the restrictions that must be presents in the molecular formula.
      *
      * @param rulesNew  The restrictions to impose
      *
      * @see #getRestrictions()
      * @see #setDefaultRestrictions()
      * @see IRule
      */
     public void setRestrictions(List<IRule> rulesNew) throws CDKException {
 
         Iterator<IRule> itRules = rulesNew.iterator();
         while (itRules.hasNext()) {
             IRule rule = itRules.next();
             if (rule instanceof ElementRule) {
                 mfRange = (MolecularFormulaRange) ((Object[]) rule.getParameters())[0];
 
                 //removing the rule
                 Iterator<IRule> oldRuleIt = rules.iterator();
                 while (oldRuleIt.hasNext()) {
                     IRule oldRule = oldRuleIt.next();
                     if (oldRule instanceof ElementRule) {
                         rules.remove(oldRule);
                         rules.add(rule);
                         break;
                     }
                 }
                 this.matrix_Base = getMatrix(mfRange.getIsotopeCount());
             } else if (rule instanceof ChargeRule) {
                 this.charge = (Double) ((Object[]) rule.getParameters())[0];
 
                 //removing the rule
                 Iterator<IRule> oldRuleIt = rules.iterator();
                 while (oldRuleIt.hasNext()) {
                     IRule oldRule = oldRuleIt.next();
                     if (oldRule instanceof ChargeRule) {
                         rules.remove(oldRule);
                         rules.add(rule);
                         break;
                     }
                 }
             } else if (rule instanceof ToleranceRangeRule) {
                 this.tolerance = (Double) ((Object[]) rule.getParameters())[1];
                 //removing the rule
                 Iterator<IRule> oldRuleIt = rules.iterator();
                 while (oldRuleIt.hasNext()) {
                     IRule oldRule = oldRuleIt.next();
                     if (oldRule instanceof ToleranceRangeRule) {
                         rules.remove(oldRule);
                         rules.add(rule);
                         break;
                     }
                 }
             } else {
                 rules.add(rule);
             }
 
         }
     }
 
     /**
      * Get the restrictions that must be presents in the molecular formula.
      *
      * @return The restrictions to be imposed
      *
      * @see #setDefaultRestrictions()
      */
     public List<IRule> getRestrictions() {
         return this.rules;
     }
 
     /**
      * Set the default restrictions that must be presents in the molecular formula.
      *
      * @see #getRestrictions()
      */
     public void setDefaultRestrictions() {
         try {
             callDefaultRestrictions();
         } catch (CDKException e) {
             e.printStackTrace();
         } catch (IOException e) {
             e.printStackTrace();
         }
     }
 
     /**
      * Create the default restrictions. They are:<p>
      *
      * The major isotopes = C, H, O and N<p>
      * Charge = 0.0, indicating neutral compound<p>
      * Tolerance = 0.05 amu<p>
      * @throws ClassNotFoundException
      * @throws IOException
      * @throws CDKException
      * @throws IOException
      *
      */
     private void callDefaultRestrictions() throws CDKException, IOException {
 
         List<IRule> rules1 = new ArrayList<IRule>();
         IsotopeFactory ifac = Isotopes.getInstance();
 
         // restriction for occurrence elements
         MolecularFormulaRange mfRange1 = new MolecularFormulaRange();
         mfRange1.addIsotope(ifac.getMajorIsotope("C"), 0, 15);
         mfRange1.addIsotope(ifac.getMajorIsotope("H"), 0, 15);
         mfRange1.addIsotope(ifac.getMajorIsotope("N"), 0, 15);
         mfRange1.addIsotope(ifac.getMajorIsotope("O"), 0, 15);
 
         IRule rule = new ElementRule();
         Object[] params = new Object[1];
         params[0] = mfRange1;
         rule.setParameters(params);
 
         rules1.add(rule);
 
         // occurrence for charge
         rule = new ChargeRule(); // default 0.0 neutral
         rules1.add(rule);
         charge = (Double) ((Object[]) rule.getParameters())[0];
 
         // occurrence for tolerance
         rule = new ToleranceRangeRule(); // default 0.05
         rules1.add(rule);
         this.tolerance = (Double) ((Object[]) rule.getParameters())[1];
 
         this.matrix_Base = getMatrix(mfRange1.getIsotopeCount());
 
         this.mfRange = mfRange1;
         this.rules = rules1;
 
     }
 
 
/** Extracts the molecur formula. */
 public IMolecularFormulaSet generate(double mass){}

 

}