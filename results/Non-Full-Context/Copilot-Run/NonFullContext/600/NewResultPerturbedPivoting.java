/* Copyright (C) 2004-2007  The Chemistry Development Kit (CDK) project
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
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.exception.NoSuchAtomException;
 import org.openscience.cdk.graph.SpanningTree;
 import org.openscience.cdk.interfaces.IElement;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IBond.Order;
 import org.openscience.cdk.interfaces.IRingSet;
 import org.openscience.cdk.qsar.AbstractMolecularDescriptor;
 import org.openscience.cdk.qsar.DescriptorSpecification;
 import org.openscience.cdk.qsar.DescriptorValue;
 import org.openscience.cdk.qsar.IMolecularDescriptor;
 import org.openscience.cdk.qsar.result.IDescriptorResult;
 import org.openscience.cdk.qsar.result.IntegerResult;
 import org.openscience.cdk.tools.manipulator.BondManipulator;
 
 import java.util.List;
 
 /**
  *  The number of rotatable bonds is given by the SMARTS specified by Daylight on
  *  <a href="http://www.daylight.com/dayhtml_tutorials/languages/smarts/smarts_examples.html#EXMPL">SMARTS tutorial</a><p>
  *
  * <table border="1"><caption>Parameters for this descriptor:</caption>
  *   <tr>
  *     <td>Name</td>
  *     <td>Default</td>
  *     <td>Description</td>
  *   </tr>
  *   <tr>
  *     <td>includeTerminals</td>
  *     <td>false</td>
  *     <td>True if terminal bonds are included</td>
  *   </tr>
  *   <tr>
  *     <td>excludeAmides</td>
  *     <td>false</td>
  *     <td>True if amide C-N bonds should be excluded</td>
  *   </tr>
  * </table>
  *
  * Returns a single value named <i>nRotB</i>
  *
  * @author      mfe4
  * @cdk.created 2004-11-03
  * @cdk.module  qsarmolecular
  * @cdk.githash
  * @cdk.dictref qsar-descriptors:rotatableBondsCount
  *
  * @cdk.keyword bond count, rotatable
  * @cdk.keyword descriptor
  */
 public class RotatableBondsCountDescriptor extends AbstractMolecularDescriptor implements IMolecularDescriptor {
 
     private boolean includeTerminals = false;
     private boolean excludeAmides    = false;
 
     /**
      *  Constructor for the RotatableBondsCountDescriptor object
      */
     public RotatableBondsCountDescriptor() {}
 
     /**
      *  Gets the specification attribute of the RotatableBondsCountDescriptor
      *  object
      *
      *@return    The specification value
      */
     @Override
     public DescriptorSpecification getSpecification() {
         return new DescriptorSpecification(
                 "http://www.blueobelisk.org/ontologies/chemoinformatics-algorithms/#rotatableBondsCount", this
                         .getClass().getName(), "The Chemistry Development Kit");
     }
 
     /**
      *  Sets the parameters attribute of the RotatableBondsCountDescriptor object
      *
      *@param  params            a boolean true means that terminal atoms must be included in the count
      *@exception  CDKException  Description of the Exception
      */
     @Override
     public void setParameters(Object[] params) throws CDKException {
         if (params.length != 2) {
             throw new CDKException("RotatableBondsCount expects two parameters");
         }
         if (!(params[0] instanceof Boolean) || !(params[1] instanceof Boolean)) {
             throw new CDKException("The parameters must be of type Boolean");
         }
         // ok, all should be fine
         includeTerminals = (Boolean) params[0];
         excludeAmides = (Boolean) params[1];
     }
 
     /**
      *  Gets the parameters attribute of the RotatableBondsCountDescriptor object
      *
      *@return    The parameters value
      */
     @Override
     public Object[] getParameters() {
         // return the parameters as used for the descriptor calculation
         Object[] params = new Object[2];
         params[0] = includeTerminals;
         params[1] = excludeAmides;
         return params;
     }
 
     @Override
     public String[] getDescriptorNames() {
         return new String[]{includeTerminals ? "nRotBt" : "nRotB"};
     }
 
 
/** The method calculates the number of rotating bonds of an atom container. */

public DescriptorValue calculate(IAtomContainer ac) {
    int rotatableBondsCount = 0;
    
    // Iterate through all the bonds in the atom container
    for (IBond bond : ac.bonds()) {
        // Check if the bond is rotatable
        if (isRotatableBond(bond, ac)) {
            rotatableBondsCount++;
        }
    }
    
    // Create the descriptor value with the calculated count
    IDescriptorResult result = new IntegerResult(rotatableBondsCount);
    DescriptorValue descriptorValue = new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), result, getDescriptorNames());
    
    return descriptorValue;
}
 

}