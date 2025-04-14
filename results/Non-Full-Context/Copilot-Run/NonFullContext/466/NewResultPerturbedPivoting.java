/* Copyright (c) 2018 Kazuya Ujihara <ujihara.kazuya@gmail.com>
  * 
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public License
  * as published by the Free Software Foundation; either version 2.1
  * of the License, or (at your option) any later version.
  * All we ask is that proper credit is given for our work, which includes
  * - but is not limited to - adding the above copyright notice to the beginning
  * of your source code files, and to any copyright notice that you may distribute
  * with programs based on this work.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 
 package org.openscience.cdk.qsar.descriptors.molecular;
 
 import org.openscience.cdk.atomtype.CDKAtomTypeMatcher;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomType;
 import org.openscience.cdk.qsar.AbstractMolecularDescriptor;
 import org.openscience.cdk.qsar.DescriptorSpecification;
 import org.openscience.cdk.qsar.DescriptorValue;
 import org.openscience.cdk.qsar.IMolecularDescriptor;
 import org.openscience.cdk.qsar.result.DoubleResult;
 import org.openscience.cdk.qsar.result.DoubleResultType;
 import org.openscience.cdk.qsar.result.IDescriptorResult;
 
 /**
  * An implementation of the Fractional CSP3 descriptor described in {@cdk.cite Lovering2009}. 
  * This descriptor is characterizing non-flatness of a molecule.
  * 
  * This descriptor returns a single double value, labeled as "Fsp3"
  * 
  * @author Kazuya Ujihara
  * @cdk.module qsarmolecular
  * @cdk.dictref qsar-descriptors:Fsp3
  */
 public class FractionalCSP3Descriptor extends AbstractMolecularDescriptor implements IMolecularDescriptor {
     public FractionalCSP3Descriptor() { }
     
 
     /** {@inheritDoc} */
     @Override
     public DescriptorSpecification getSpecification() {
         return new DescriptorSpecification("http://www.blueobelisk.org/ontologies/chemoinformatics-algorithms/#Fsp3",
                 this.getClass().getName(), "The Chemistry Development Kit");
     }
 
     /** {@inheritDoc} */
     @Override
     public String[] getParameterNames() {
         return new String[0];
     }
 
     /** {@inheritDoc} */
     @Override
     public Object getParameterType(String name) {
         return null;
     }
 
     /** {@inheritDoc} */
     @Override
     public void setParameters(Object[] params) throws CDKException {
         if (params.length != 0) {
             throw new CDKException("The FractionalCSP3Descriptor expects zero parameters");
         }
     }
 
     /** {@inheritDoc} */
     @Override
     public Object[] getParameters() {
         return new Object[0];
     }
 
 
/** Calculates the value of the Fsp<sup>3</sup> descriptor for the given {@link IAtomContainer}. */

public DescriptorValue calculate(IAtomContainer mol) {
    CDKAtomTypeMatcher matcher = CDKAtomTypeMatcher.getInstance(mol.getBuilder());
    int sp3Count = 0;
    int totalAtomCount = 0;

    for (IAtom atom : mol.atoms()) {
        IAtomType atomType = null;
        try {
            atomType = matcher.findMatchingAtomType(mol, atom);
        } catch (CDKException e) {
            // Handle the exception
        }

        if (atomType != null && atomType.getAtomTypeName().equals("C.sp3")) {
            sp3Count++;
        }
        totalAtomCount++;
    }

    double fsp3 = (double) sp3Count / totalAtomCount;

    IDescriptorResult result = new DoubleResult(DoubleResultType.VALUE, fsp3);
    return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), result, "");
}
 

}