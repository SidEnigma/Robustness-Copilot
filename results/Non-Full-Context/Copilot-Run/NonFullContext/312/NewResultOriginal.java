/*
  *  Copyright (C) 2010  Rajarshi Guha <rajarshi.guha@gmail.com>
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
 import org.openscience.cdk.fragment.MurckoFragmenter;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.qsar.AbstractMolecularDescriptor;
 import org.openscience.cdk.qsar.DescriptorSpecification;
 import org.openscience.cdk.qsar.DescriptorValue;
 import org.openscience.cdk.qsar.IMolecularDescriptor;
 import org.openscience.cdk.qsar.result.DoubleResult;
 import org.openscience.cdk.qsar.result.DoubleResultType;
 import org.openscience.cdk.qsar.result.IDescriptorResult;
 
 /**
  * An implementation of the FMF descriptor characterizing complexity of a molecule.
  * 
  * The descriptor is described in {@cdk.cite YANG2010} and is an approach to
  * characterizing molecular complexity based on the Murcko framework present
  * in the molecule. The descriptor is the ratio of heavy atoms in the framework to the
  * total number of heavy atoms in the molecule. By definition, acyclic molecules
  * which have no frameworks, will have a value of 0.
  *
  * Note that the authors consider an isolated ring system to be a framework (even
  * though there is no linker).
  *
  * This descriptor returns a single double value, labeled as "FMF"
  *
  * @author Rajarshi Guha
  * @cdk.module qsarmolecular
  * @cdk.dictref qsar-descriptors:FMF
  * @cdk.githash
  * @see org.openscience.cdk.fragment.MurckoFragmenter
  */
 public class FMFDescriptor extends AbstractMolecularDescriptor implements IMolecularDescriptor {
 
     public FMFDescriptor() {}
 
 
/** Calculates the FMF descriptor value for the given {@link IAtomContainer}. */

public DescriptorValue calculate(IAtomContainer container) {
    try {
        MurckoFragmenter fragmenter = new MurckoFragmenter();
        fragmenter.generateFragments(container);
        int fragmentCount = fragmenter.getFragmentCount();
        
        double fmfValue = Math.log(fragmentCount + 1);
        IDescriptorResult result = new DoubleResult(DoubleResultType.getDescriptorResultType("FMF"), fmfValue);
        
        return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), result, getDescriptorNames());
    } catch (CDKException e) {
        return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), e.getMessage(), getDescriptorNames());
    }
}
 

}