/* Copyright (C) 2006-2007  Miguel Rojas <miguel.rojas@uni-koeln.de>
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License along
  * with this program; if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 package org.openscience.cdk.qsar.descriptors.atomic;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.StringTokenizer;
 
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomType;
 import org.openscience.cdk.interfaces.IBond.Order;
 import org.openscience.cdk.qsar.AbstractAtomicDescriptor;
 import org.openscience.cdk.qsar.DescriptorSpecification;
 import org.openscience.cdk.qsar.DescriptorValue;
 import org.openscience.cdk.qsar.result.DoubleResult;
 import org.openscience.cdk.tools.HOSECodeGenerator;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.LonePairElectronChecker;
 import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
 
 /**
  *  This class returns the ionization potential of an atom containing lone
  *  pair electrons. It is
  *  based on a decision tree which is extracted from Weka(J48) from
  *  experimental values. Up to now is only possible predict for
  *  Cl,Br,I,N,P,O,S Atoms and they are not belong to
  *  conjugated system or not adjacent to an double bond.
  *
  * <table border="1"><caption>Parameters for this descriptor:</caption>
  *   <tr>
  *     <td>Name</td>
  *     <td>Default</td>
  *     <td>Description</td>
  *   </tr>
  *   <tr>
  *     <td></td>
  *     <td></td>
  *     <td>no parameters</td>
  *   </tr>
  * </table>
  *
  * @author       Miguel Rojas
  * @cdk.created  2006-05-26
  * @cdk.module   qsaratomic
  * @cdk.githash
  * @cdk.dictref  qsar-descriptors:ionizationPotential
  */
 public class IPAtomicHOSEDescriptor extends AbstractAtomicDescriptor {
 
     private static final String[] NAMES = {"ipAtomicHOSE"};
 
     /** Maximum spheres to use by the HoseCode model.*/
     int                           maxSpheresToUse = 10;
 
     private IPdb                  db              = new IPdb();
 
     /**
      *  Constructor for the IPAtomicHOSEDescriptor object.
      */
     public IPAtomicHOSEDescriptor() {}
 
     /**
      *  Gets the specification attribute of the IPAtomicHOSEDescriptor object
      *
      *@return    The specification value
      */
     @Override
     public DescriptorSpecification getSpecification() {
         return new DescriptorSpecification(
                 "http://www.blueobelisk.org/ontologies/chemoinformatics-algorithms/#ionizationPotential", this
                         .getClass().getName(), "The Chemistry Development Kit");
     }
 
     /**
      * This descriptor does have any parameter.
      */
     @Override
     public void setParameters(Object[] params) throws CDKException {}
 
     /**
      *  Gets the parameters attribute of the IPAtomicHOSEDescriptor object.
      *
      *@return    The parameters value
      * @see #setParameters
      */
     @Override
     public Object[] getParameters() {
         return null;
     }
 
     @Override
     public String[] getDescriptorNames() {
         return NAMES;
     }
 
 
/** The method calculates the potential of an atom. */
 public DescriptorValue calculate(IAtom atom, IAtomContainer container){
        DescriptorValue result = new DescriptorValue();
        result.setName(getSpecification().getName());
        result.setSpecification(getSpecification());
        result.setValue(calculateValue(atom, container));
        return result;
    }
    
        /**
        *  Calculates the value of the descriptor for the given atom.
        *
        *@param  atom  The IAtom for which the descriptor is calculated
        *@param  container  The IAtomContainer in which the IAtom is contained
        *@return         The value of the descriptor
        */
        @Override
        public DescriptorValue calculate(IAtom atom, IAtomContainer container,
                DescriptorValue descriptorValue) {
            descriptorValue.setName(getSpecification().getName());
            descriptorValue.setSpecification(getSpecification());
            descriptorValue.setValue(calculateValue(atom, container));
            return descriptorValue;
        }
    
        /**
        *  Calculates the value of the descriptor for the given atom.
        *
        *@param  atom  The IAtom for which the descriptor is calculated
        *@param  container  The IAtomContainer in which the IAtom is contained
        *@return         The value of the descriptor
        */
        @Override
        public double calculateValue(IAtom atom, IAtomContainer container) {
            double result = 0.0;
            if (atom.getSymbol().equals("C")) {
                result = calculateC(atom, container);
            } else if (atom.getSymbol().equals("N")) {
                result = calculateN(atom, container);
            } else if (atom.getSymbol().equals("O")) {
                result = calculateO(atom, container);
            } else if (atom.getSymbol().equals("S")) {
                result = calculateS(atom, container);
            } else if (atom.getSymbol().equals("P")) {
                result = calculateP(atom, container);
            } else if (atom.getSymbol().equals("F")) {      
 }

 

}