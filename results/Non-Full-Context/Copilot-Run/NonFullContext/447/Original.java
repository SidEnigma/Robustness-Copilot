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
 package org.openscience.cdk.qsar.descriptors.atomic;
 
 import org.openscience.cdk.charges.GasteigerMarsiliPartialCharges;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IElement;
 import org.openscience.cdk.qsar.AbstractAtomicDescriptor;
 import org.openscience.cdk.qsar.DescriptorSpecification;
 import org.openscience.cdk.qsar.DescriptorValue;
 import org.openscience.cdk.qsar.IAtomicDescriptor;
 import org.openscience.cdk.qsar.result.DoubleArrayResult;
 
 import java.util.List;
 
 /**
  * The calculation of partial charges of an heavy atom and its protons is based on Gasteiger Marsili (PEOE).
  *
  * This descriptor has no parameters. The result of this descriptor is a vector of 5 values, corresponding
  * to a maximum of four protons for any given atom. If an atom has fewer than four protons, the remaining values
  * are set to Double.NaN. Also note that the values for the neighbors are not returned in a particular order
  * (though the order is fixed for multiple runs for the same atom).
  *
  * @author mfe4
  * @cdk.created 2004-11-03
  * @cdk.module qsaratomic
  * @cdk.githash
  * @cdk.dictref qsar-descriptors:protonPartialCharge
  */
 public class ProtonTotalPartialChargeDescriptor extends AbstractAtomicDescriptor implements IAtomicDescriptor {
 
     private GasteigerMarsiliPartialCharges peoe             = null;
     private List<IAtom>                    neighboors;
     private final int                      MAX_PROTON_COUNT = 5;
 
     /**
      *  Constructor for the ProtonTotalPartialChargeDescriptor object
      */
     public ProtonTotalPartialChargeDescriptor() {}
 
     /**
      *  Gets the specification attribute of the ProtonTotalPartialChargeDescriptor
      *  object
      *
      *@return    The specification value
      */
     @Override
     public DescriptorSpecification getSpecification() {
         return new DescriptorSpecification(
                 "http://www.blueobelisk.org/ontologies/chemoinformatics-algorithms/#protonPartialCharge", this
                         .getClass().getName(), "The Chemistry Development Kit");
     }
 
     /**
      * This descriptor does not have any parameter to be set.
      */
     @Override
     public void setParameters(Object[] params) throws CDKException {
         // no parameters
     }
 
     /**
      *  Gets the parameters attribute of the ProtonTotalPartialChargeDescriptor
      *  object
      *
      *@return    The parameters value
      *@see #setParameters
      */
     @Override
     public Object[] getParameters() {
         return null;
     }
 
     @Override
     public String[] getDescriptorNames() {
         String[] labels = new String[MAX_PROTON_COUNT];
         for (int i = 0; i < MAX_PROTON_COUNT; i++) {
             labels[i] = "protonTotalPartialCharge" + (i + 1);
         }
         return labels;
     }
 
     private DescriptorValue getDummyDescriptorValue(Exception e) {
         DoubleArrayResult result = new DoubleArrayResult(MAX_PROTON_COUNT);
         for (int i = 0; i < neighboors.size() + 1; i++)
             result.add(Double.NaN);
         return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), result,
                 getDescriptorNames(), e);
     }
 
 
/** The method returns partial charges assigned to an heavy atom and its protons through Gasteiger Marsili  It is needed to call the addExplicitHydrogensToSatisfyValency method from the class tools. */
 public DescriptorValue calculate(IAtom atom, IAtomContainer ac){}

 

}