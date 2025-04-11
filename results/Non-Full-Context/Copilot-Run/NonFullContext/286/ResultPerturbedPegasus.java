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
 
     /**
      *  This method calculates the ionization potential of an atom.
      *
      *@param  atom          The IAtom to ionize.
      *@param  container         Parameter is the IAtomContainer.
      *@return                   The ionization potential. Not possible the ionization.
      */
     @Override
     public DescriptorValue calculate(IAtom atom, IAtomContainer container) {
         double value;
         // FIXME: for now I'll cache a few modified atomic properties, and restore them at the end of this method
         String originalAtomtypeName = atom.getAtomTypeName();
         Integer originalNeighborCount = atom.getFormalNeighbourCount();
         Integer originalValency = atom.getValency();
         Double originalBondOrderSum = atom.getBondOrderSum();
         Order originalMaxBondOrder = atom.getMaxBondOrder();
         IAtomType.Hybridization originalHybridization = atom.getHybridization();
 
         if (!isCachedAtomContainer(container)) {
             try {
                 AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(container);
                 LonePairElectronChecker lpcheck = new LonePairElectronChecker();
                 lpcheck.saturate(container);
             } catch (CDKException e) {
                 return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), new DoubleResult(
                         Double.NaN), NAMES, e);
             }
 
         }
         value = db.extractIP(container, atom);
         // restore original props
         atom.setAtomTypeName(originalAtomtypeName);
         atom.setFormalNeighbourCount(originalNeighborCount);
         atom.setValency(originalValency);
         atom.setHybridization(originalHybridization);
         atom.setMaxBondOrder(originalMaxBondOrder);
         atom.setBondOrderSum(originalBondOrderSum);
 
         return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), new DoubleResult(value),
                                    NAMES);
 
     }
 
 
/** The atom is being looked at to see if it belongs to the halogen family. */
 private boolean familyHalogen(IAtom atom){
        String symbol = atom.getSymbol();
        if (symbol.equals("F") || symbol.equals("Cl") || symbol.equals("Br") || symbol.equals("I"))
            return true;
        else
            return false;       
 }

 

}