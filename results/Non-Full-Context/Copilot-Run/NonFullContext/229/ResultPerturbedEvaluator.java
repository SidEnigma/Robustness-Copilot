/* Copyright (C) 2004-2007  Miguel Rojas <miguel.rojas@uni-koeln.de>
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
 package org.openscience.cdk.qsar.descriptors.bond;
 
 import org.openscience.cdk.charges.GasteigerMarsiliPartialCharges;
 import org.openscience.cdk.charges.GasteigerPEPEPartialCharges;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomType;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IBond.Order;
 import org.openscience.cdk.qsar.AbstractBondDescriptor;
 import org.openscience.cdk.qsar.DescriptorSpecification;
 import org.openscience.cdk.qsar.DescriptorValue;
 import org.openscience.cdk.qsar.result.DoubleResult;
 import org.openscience.cdk.tools.LonePairElectronChecker;
 import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 
 /**
  *  The calculation of bond total Partial charge is calculated
  *  determining the difference the Partial Total Charge on atoms
  *  A and B of a bond. Based in Gasteiger Charge.
  *  <table border="1"><caption>Parameters for this descriptor:</caption>
  *   <tr>
  *     <td>Name</td>
  *     <td>Default</td>
  *     <td>Description</td>
  *   </tr>
  *   <tr>
  *     <td>bondPosition</td>
  *     <td>0</td>
  *     <td>The position of the target bond</td>
  *   </tr>
  * </table>
  *
  * @author      Miguel Rojas
  * @cdk.created 2006-05-18
  * @cdk.module  qsarbond
  * @cdk.githash
  * @cdk.dictref qsar-descriptors:bondPartialTCharge
  *
  * @see org.openscience.cdk.qsar.descriptors.atomic.PartialPiChargeDescriptor
  * @see org.openscience.cdk.qsar.descriptors.atomic.PartialSigmaChargeDescriptor
  */
 public class BondPartialTChargeDescriptor extends AbstractBondDescriptor {
 
     private GasteigerMarsiliPartialCharges peoe = null;
     private GasteigerPEPEPartialCharges    pepe = null;
 
     /**Number of maximum iterations*/
     private int     maxIterations = -1;
     /**Number of maximum resonance structures*/
     private int     maxResonStruc = -1;
     /** make a lone pair electron checker. Default true*/
     private boolean lpeChecker    = true;
 
     private static final String[] NAMES = {"pCB"};
 
     /**
      *  Constructor for the BondPartialTChargeDescriptor object.
      */
     public BondPartialTChargeDescriptor() {
         peoe = new GasteigerMarsiliPartialCharges();
         pepe = new GasteigerPEPEPartialCharges();
     }
 
     /**
      *  Gets the specification attribute of the BondPartialTChargeDescriptor
      *  object.
      *
      *@return The specification value
      */
     @Override
     public DescriptorSpecification getSpecification() {
         return new DescriptorSpecification(
                 "http://www.blueobelisk.org/ontologies/chemoinformatics-algorithms/#bondPartialTCharge", this
                 .getClass().getName(), "The Chemistry Development Kit");
     }
 
     /**
      * This descriptor does have any parameter.
      */
     @Override
     public void setParameters(Object[] params) throws CDKException {
         if (params.length > 3)
             throw new CDKException("PartialPiChargeDescriptor only expects three parameter");
 
         if (!(params[0] instanceof Integer))
             throw new CDKException("The parameter must be of type Integer");
         maxIterations = (Integer) params[0];
 
         if (params.length > 1 && params[1] != null) {
             if (!(params[1] instanceof Boolean))
                 throw new CDKException("The parameter must be of type Boolean");
             lpeChecker = (Boolean) params[1];
         }
 
         if (params.length > 2 && params[2] != null) {
             if (!(params[2] instanceof Integer))
                 throw new CDKException("The parameter must be of type Integer");
             maxResonStruc = (Integer) params[2];
         }
     }
 
     /**
      *  Gets the parameters attribute of the BondPartialTChargeDescriptor object.
      *
      *@return The parameters value
      * @see #setParameters
      */
     @Override
     public Object[] getParameters() {
         // return the parameters as used for the descriptor calculation
         Object[] params = new Object[3];
         params[0] = maxIterations;
         params[1] = lpeChecker;
         params[2] = maxResonStruc;
         return params;
     }
 
     @Override
     public String[] getDescriptorNames() {
         return NAMES;
     }
 
     private DescriptorValue getDummyDescriptorValue(Exception e) {
         return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), new DoubleResult(
                 Double.NaN), NAMES, e);
     }
 
 
/** The total partial charge of a given bond is calculated. The method addExplicitHydrogensToSatisfyValency must be called from the tools.HydrogenAdder class. */
 public DescriptorValue calculate(IBond bond, IAtomContainer ac){
        try {
            if (bond.getOrder() == Order.SINGLE) {
                return getDummyDescriptorValue(new CDKException("The bond order is SINGLE"));
            }
    
            IAtom atomA = bond.getAtom(0);
            IAtom atomB = bond.getAtom(1);
    
            double pA = 0.0;
            double pB = 0.0;
    
            if (atomA.getFlag(CDKConstants.ISAROMATIC)) {
                pA = pepe.calculate(atomA, ac);
            } else {
                pA = peoe.calculate(atomA, ac);
            }
    
            if (atomB.getFlag(CDKConstants.ISAROMATIC)) {
                pB = pepe.calculate(atomB, ac);
            } else {
                pB = peoe.calculate(atomB, ac);
            }
    
            double pT = pA - pB;
    
            return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), new DoubleResult(pT),
                    NAMES);
        } catch (Exception e) {
            return getDummyDescriptorValue(e);
        }
    }
    
        @Override
        public DescriptorValue calculate(IAtomContainer container) {
            try {
                if (container.getBondCount() == 0) {
                    return getDummyDescriptorValue(new CDKException("The container has no bonds"));
                }
    
                IBond bond = container.getBond(bondPosition);
                if (bond == null) {
                    return getDummyDescriptorValue(new CDKException("The bond position is out of range"));
                }
    
                return calculate(bond, container);
            } catch (Exception e) {
                return getDummyDescriptorValue(e);
            }
        }
    
        @Override
        public DescriptorValue calculate(IAtomContainer container, IBond bond       
 }

 

}