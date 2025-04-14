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
 
 
/** Given a heavy atom, it returns the partial charges assigned and the protons through Gasteiger Marsili. You have to call addExplicitHydrogensToSatisfyValency method, defined in the class tools.HydrogenAdder. */
 public DescriptorValue calculate(IAtom atom, IAtomContainer ac){
        try {
            neighboors = ac.getConnectedAtomsList(atom);
            if (neighboors.size() == 0)
                return getDummyDescriptorValue(new CDKException("No neighbors found"));
            if (atom.getSymbol().equals("H"))
                return getDummyDescriptorValue(new CDKException("The atom is a hydrogen"));
            if (atom.getSymbol().equals("C"))
                return getDummyDescriptorValue(new CDKException("The atom is a carbon"));
            if (atom.getSymbol().equals("N"))
                return getDummyDescriptorValue(new CDKException("The atom is a nitrogen"));
            if (atom.getSymbol().equals("O"))
                return getDummyDescriptorValue(new CDKException("The atom is an oxygen"));
            if (atom.getSymbol().equals("S"))
                return getDummyDescriptorValue(new CDKException("The atom is a sulfur"));
            if (atom.getSymbol().equals("P"))
                return getDummyDescriptorValue(new CDKException("The atom is a phosphorus"));
            if (atom.getSymbol().equals("F"))
                return getDummyDescriptorValue(new CDKException("The atom is a fluorine"));
            if (atom.getSymbol().equals("Cl"))
                return getDummyDescriptorValue(new CDKException("The atom is a chlorine"));
            if (atom.getSymbol().equals("Br"))
                return getDummyDescriptorValue(new CDKException("The atom is a bromine"));
            if (atom.getSymbol().equals("I"))
                return getDummyDescriptorValue(new CDKException("The atom is a iodine"));
            if (atom.getSymbol().equals("B"))
                return getDummyDescriptorValue(new CDKException("The atom is a boron"));
            if (atom.getSymbol().equals("Al"))
                return getDummy     
 }

 

}