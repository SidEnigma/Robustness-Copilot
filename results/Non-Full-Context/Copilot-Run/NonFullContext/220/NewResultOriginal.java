/*
  *
  *
  *  Copyright (C) 2010 Rajarshi Guha <rajarshi.guha@gmail.com>
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
 import org.openscience.cdk.interfaces.IElement;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomType.Hybridization;
 import org.openscience.cdk.qsar.AbstractMolecularDescriptor;
 import org.openscience.cdk.qsar.DescriptorSpecification;
 import org.openscience.cdk.qsar.DescriptorValue;
 import org.openscience.cdk.qsar.IMolecularDescriptor;
 import org.openscience.cdk.qsar.result.DoubleResult;
 import org.openscience.cdk.qsar.result.IDescriptorResult;
 import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
 
 /**
  * {@link IMolecularDescriptor} that reports the fraction of sp3 carbons to sp2 carbons.
  * 
  * Note that it only considers carbon atoms and rather than use a simple ratio
  * it reports the value of N<sub>sp3</sub>/ (N<sub>sp3</sub> + N<sub>sp2</sub>).
  * The original form of the descriptor (i.e., simple ratio) has been used to
  * characterize molecular complexity, especially in the are of natural products
  * , which usually have a high value of the sp3 to sp2 ratio.
  *
  * @author Rajarshi Guha
  * @cdk.module qsarmolecular
  * @cdk.githash
  * @cdk.dictref qsar-descriptors:hybratio
  */
 public class HybridizationRatioDescriptor extends AbstractMolecularDescriptor implements IMolecularDescriptor {
 
     /**
      * Constructor for the HybridizationRatioDescriptor object.
      */
     public HybridizationRatioDescriptor() {}
 
     /**
      * Returns a {@link DescriptorSpecification} which specifies which descriptor is implemented by this class.
      *
      *{@inheritDoc}
      * @return An object containing the descriptor specification
      */
     @Override
     public DescriptorSpecification getSpecification() {
         return new DescriptorSpecification(
                 "http://www.blueobelisk.org/ontologies/chemoinformatics-algorithms/#hybratio", this.getClass()
                         .getName(), "The Chemistry Development Kit");
     }
 
     /**
      * Sets the parameters attribute of the HybridizationRatioDescriptor object.
      *
      * @param params The new parameters value
      * @throws org.openscience.cdk.exception.CDKException
      *          if more than 1 parameter is specified or if the parameter
      *          is not of type String
      * @see #getParameters
      */
     @Override
     public void setParameters(Object[] params) throws CDKException {}
 
     /**
      * Gets the parameters attribute of the HybridizationRatioDescriptor object.
      *
      * This descriptor takes no parameters
      *
      * @return The parameters value
      * @see #setParameters
      */
     @Override
     public Object[] getParameters() {
         return new Object[0];
     }
 
     @Override
     public String[] getDescriptorNames() {
         return new String[]{"HybRatio"};
     }
 
     /**
      *{@inheritDoc}
      *
      * @param e the exception
      * @return a dummy value
      */
     private DescriptorValue getDummyDescriptorValue(Exception e) {
         return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), new DoubleResult(
                 Double.NaN), getDescriptorNames(), e);
     }
 
 
/** Calculate sp3/sp2 hybridization ratio in the supplied {@link IAtomContainer}. */

public DescriptorValue calculate(IAtomContainer container) {
    try {
        int sp3Count = 0;
        int sp2Count = 0;
        
        for (IAtom atom : container.atoms()) {
            Hybridization hybridization = AtomContainerManipulator.getHybridization(atom);
            
            if (hybridization == Hybridization.SP3) {
                sp3Count++;
            } else if (hybridization == Hybridization.SP2) {
                sp2Count++;
            }
        }
        
        double hybridizationRatio = (double) sp3Count / sp2Count;
        IDescriptorResult result = new DoubleResult(hybridizationRatio);
        
        return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), result, getDescriptorNames(), null);
    } catch (Exception e) {
        return getDummyDescriptorValue(e);
    }
}
 

}