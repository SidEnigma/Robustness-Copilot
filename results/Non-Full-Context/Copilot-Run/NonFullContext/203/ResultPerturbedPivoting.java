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
 package org.openscience.cdk.qsar.descriptors.atomic;
 
 import org.openscience.cdk.charges.GasteigerMarsiliPartialCharges;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.qsar.AbstractAtomicDescriptor;
 import org.openscience.cdk.qsar.DescriptorSpecification;
 import org.openscience.cdk.qsar.DescriptorValue;
 import org.openscience.cdk.qsar.result.DoubleResult;
 
 /**
  *  The calculation of sigma partial charges in sigma-bonded systems of an heavy atom
  *  was made by Marsilli-Gasteiger. It is implemented with the Partial Equalization
  *  of Orbital Electronegativity (PEOE).
  *
  * <table border="1"><caption>Parameters for this descriptor:</caption>
  *   <tr>
  *     <td>Name</td>
  *     <td>Default</td>
  *     <td>Description</td>
  *   </tr>
  *   <tr>
  *     <td>maxIterations</td>
  *     <td>0</td>
  *     <td>Number of maximum iterations</td>
  *   </tr>
  * </table>
  *
  *
  * @author      Miguel Rojas
  * @cdk.created 2006-04-15
  * @cdk.module  qsaratomic
  * @cdk.githash
  * @cdk.dictref qsar-descriptors:partialSigmaCharge
  * @see GasteigerMarsiliPartialCharges
  */
 public class PartialSigmaChargeDescriptor extends AbstractAtomicDescriptor {
 
     private static final String[]          NAMES = {"partialSigmaCharge"};
 
     private GasteigerMarsiliPartialCharges peoe  = null;
     /**Number of maximum iterations*/
     private int                            maxIterations;
 
     /**
      *  Constructor for the PartialSigmaChargeDescriptor object
      */
     public PartialSigmaChargeDescriptor() {
         peoe = new GasteigerMarsiliPartialCharges();
     }
 
     /**
      *  Gets the specification attribute of the PartialSigmaChargeDescriptor
      *  object
      *
      *@return    The specification value
      */
     @Override
     public DescriptorSpecification getSpecification() {
         return new DescriptorSpecification(
                 "http://www.blueobelisk.org/ontologies/chemoinformatics-algorithms/#partialSigmaCharge", this
                         .getClass().getName(), "The Chemistry Development Kit");
     }
 
     /**
      *  Sets the parameters attribute of the PartialSigmaChargeDescriptor
      *  object
      *
      *@param  params            Number of maximum iterations
      *@exception  CDKException  Description of the Exception
      */
     @Override
     public void setParameters(Object[] params) throws CDKException {
         if (params.length > 1) {
             throw new CDKException("PartialSigmaChargeDescriptor only expects one parameter");
         }
         if (!(params[0] instanceof Integer)) {
             throw new CDKException("The parameter 1 must be of type Integer");
         }
         maxIterations = (Integer) params[0];
     }
 
     /**
      *  Gets the parameters attribute of the PartialSigmaChargeDescriptor object
      *
      *@return    The parameters value
      */
     @Override
     public Object[] getParameters() {
         // return the parameters as used for the descriptor calculation
         Object[] params = new Object[1];
         params[0] = maxIterations;
         return params;
     }
 
     @Override
     public String[] getDescriptorNames() {
         return NAMES;
     }
 
 
/** The method returns partial charges apha assigned to a heavy atom via Gasteiger Marsili It is necessary to call the method addExplicitHydrogensToSatisfyValency from the class tools. */
 public DescriptorValue calculate(IAtom atom, IAtomContainer ac){
        try {
            peoe.setMaxIterations(maxIterations);
            peoe.calculate(ac);
            DoubleResult result = new DoubleResult(peoe.getPartialCharge(atom));
            return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), result,
                    atom.getSymbol());
        } catch (Exception e) {
            e.printStackTrace();
            return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(),
                    new DoubleResult(Double.NaN), atom.getSymbol());
        }
    }
    
        @Override
        public DescriptorValue calculate(IAtomContainer container) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    
        @Override
        public String[] getParameterNames() {
            return new String[]{"maxIterations"};
        }
    
        @Override
        public Object getParameter(String name) {
            if (name.equals("maxIterations")) {
                return maxIterations;
            }
            throw new IllegalArgumentException("Unknown parameter: " + name);
        }
    
        @Override
        public void setParameter(String name, Object value) {
            if (name.equals("maxIterations")) {
                maxIterations = ((Integer) value).intValue();
            } else {
                throw new IllegalArgumentException("Unknown parameter: " + name);
            }
        }
    
        @Override
        public Object clone() {
            PartialSigmaChargeDescriptor clone = null;
            try {
                clone = (PartialSigmaChargeDescriptor) super.clone();
                clone.peoe = (GasteigerMarsiliPartialCharges) peoe.clone();
            } catch (CloneNotSupportedException e) {
                // this should never happen
                throw new InternalError();
            }
            return clone;
        }
    
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + (this.peoe != null ? this.peoe.hashCode() : 0);      
 }

 

}