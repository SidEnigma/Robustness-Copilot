/*  Copyright (C) 2004-2007  The Chemistry Development Kit (CDK) project
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
 package org.openscience.cdk.qsar.descriptors.molecular;
 
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.qsar.AbstractMolecularDescriptor;
 import org.openscience.cdk.qsar.DescriptorSpecification;
 import org.openscience.cdk.qsar.DescriptorValue;
 import org.openscience.cdk.qsar.IMolecularDescriptor;
 import org.openscience.cdk.qsar.result.DoubleResult;
 import org.openscience.cdk.qsar.result.IDescriptorResult;
 import org.openscience.cdk.qsar.result.IntegerResult;
 
 /**
  * This Class contains a method that returns the number failures of the
  * Lipinski's Rule Of 5.
  * See <a href="http://en.wikipedia.org/wiki/Lipinski%27s_Rule_of_Five">http://en.wikipedia.org/wiki/Lipinski%27s_Rule_of_Five</a>.
  *
   * <table border="1"><caption>Parameters for this descriptor:</caption>
  *   <tr>
  *     <td>Name</td>
  *     <td>Default</td>
  *     <td>Description</td>
  *   </tr>
  *   <tr>
  *     <td>checkAromaticity</td>
  *     <td>false</td>
  *     <td>True is the aromaticity has to be checked</td>
  *   </tr>
  * </table>
  *
  * Returns a single value named <i>LipinskiFailures</i>.
  *
  * @author      mfe4
  * @cdk.created 2004-11-03
  * @cdk.module  qsarmolecular
  * @cdk.githash
  * @cdk.dictref qsar-descriptors:lipinskifailures
  *
  * @cdk.keyword Lipinski
  * @cdk.keyword rule-of-five
  * @cdk.keyword descriptor
  */
 public class RuleOfFiveDescriptor extends AbstractMolecularDescriptor implements IMolecularDescriptor {
 
     private boolean               checkAromaticity = false;
 
     private static final String[] NAMES            = {"LipinskiFailures"};
 
     /**
      *  Constructor for the RuleOfFiveDescriptor object.
      */
     public RuleOfFiveDescriptor() {}
 
     /**
      * Returns a <code>Map</code> which specifies which descriptor
      * is implemented by this class.
      *
      * These fields are used in the map:
      * <ul>
      * <li>Specification-Reference: refers to an entry in a unique dictionary
      * <li>Implementation-Title: anything
      * <li>Implementation-Identifier: a unique identifier for this version of
      *  this class
      * <li>Implementation-Vendor: CDK, JOELib, or anything else
      * </ul>
      *
      * @return An object containing the descriptor specification
      */
     @Override
     public DescriptorSpecification getSpecification() {
         return new DescriptorSpecification(
                 "http://www.blueobelisk.org/ontologies/chemoinformatics-algorithms/#lipinskifailures", this.getClass()
                         .getName(), "The Chemistry Development Kit");
     }
 
     /**
      *  Sets the parameters attribute of the RuleOfFiveDescriptor object.
          *
          *  There is only one parameter, which should be a Boolean indicating whether
          *  aromaticity should be checked or has already been checked. The name of the paramete
          *  is checkAromaticity.
      *
      *@param  params            Parameter is only one: a boolean.
      *@throws  CDKException  if more than 1 parameter or a non-Boolean parameter is specified
          *@see #getParameters
      */
     @Override
     public void setParameters(Object[] params) throws CDKException {
         if (params.length != 1) {
             throw new CDKException("RuleOfFiveDescriptor expects one parameter");
         }
         if (!(params[0] instanceof Boolean)) {
             throw new CDKException("The first parameter must be of type Boolean");
         }
         // ok, all should be fine
         checkAromaticity = (Boolean) params[0];
     }
 
     /**
      *  Gets the parameters attribute of the RuleOfFiveDescriptor object.
      *
      *@return    The parameters value
          *@see #setParameters
      */
     @Override
     public Object[] getParameters() {
         // return the parameters as used for the descriptor calculation
         Object[] params = new Object[1];
         params[0] = checkAromaticity;
         return params;
     }
 
     @Override
     public String[] getDescriptorNames() {
         return NAMES;
     }
 
 
/** Check if the aromaticity of the input has been previously checked */
 public DescriptorValue calculate(IAtomContainer mol){}

 

}