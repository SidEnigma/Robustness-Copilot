/* Copyright (c) 2014 Collaborative Drug Discovery, Inc. <alex@collaborativedrug.com>
  *
  * Implemented by Alex M. Clark, produced by Collaborative Drug Discovery, Inc.
  * Made available to the CDK community under the terms of the GNU LGPL.
  *
  *    http://collaborativedrug.com
  *
  * Contact: cdk-devel@lists.sourceforge.net
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
 
 import java.io.IOException;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.atomtype.CDKAtomTypeMatcher;
 import org.openscience.cdk.config.Isotopes;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomType;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.qsar.DescriptorSpecification;
 import org.openscience.cdk.qsar.DescriptorValue;
 import org.openscience.cdk.qsar.IMolecularDescriptor;
 import org.openscience.cdk.qsar.result.DoubleResult;
 import org.openscience.cdk.qsar.result.DoubleResultType;
 import org.openscience.cdk.qsar.result.IDescriptorResult;
 import org.openscience.cdk.tools.CDKHydrogenAdder;
 import org.openscience.cdk.tools.manipulator.AtomTypeManipulator;
 
 /**
  * Polar surface area expressed as a ratio to molecular size. Calculates <b>tpsaEfficiency</b>, which is
  * to {@link TPSADescriptor} / <b>molecular weight</b>, in units of square Angstroms per Dalton.
  *
  * Other related descriptors may also be useful to add, e.g. ratio of polar to hydrophobic surface area.
  *
  * @cdk.module qsarmolecular
  * @cdk.githash
  *
  * @cdk.dictref qsar-descriptors:fractionalPSA
  * @cdk.keyword volume
  * @cdk.keyword descriptor
  */
 public class FractionalPSADescriptor implements IMolecularDescriptor {
 
     public FractionalPSADescriptor() {}
 
     @Override
     public void initialise(IChemObjectBuilder builder) {}
 
     /**
      * {@inheritDoc}
      */
     @Override
     public DescriptorSpecification getSpecification() {
         return new DescriptorSpecification(
                 "http://www.blueobelisk.org/ontologies/chemoinformatics-algorithms/#fractionalPSA", this.getClass()
                         .getName(), "The Chemistry Development Kit");
     }
 
     /** {@inheritDoc} */
     @Override
     public void setParameters(Object[] params) throws CDKException {
         if (params.length != 0) {
             throw new CDKException("The FractionalPSADescriptor expects zero parameters");
         }
     }
 
     /** {@inheritDoc} */
     @Override
     public Object[] getParameters() {
         return new Object[0];
     }
 
     @Override
     public String[] getDescriptorNames() {
         return new String[]{"tpsaEfficiency"};
     }
 
     private DescriptorValue getDummyDescriptorValue(Exception e) {
         return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), new DoubleResult(
                 Double.NaN), getDescriptorNames(), e);
     }
 
 
/** The ratio to molecule size is calculated by summing the topological polar surface area. */
 public DescriptorValue calculate(IAtomContainer mol){
        try {
            double tpsa = 0.0;
            double molecularWeight = 0.0;
            double molecularSize = 0.0;
            double tpsaEfficiency = 0.0;
            for (IAtom atom : mol.atoms()) {
                IAtomType type = CDKAtomTypeMatcher.findAtomType(mol, atom);
                if (type == null) {
                    type = AtomTypeManipulator.getAtomType(atom);
                }
                if (type == null) {
                    throw new CDKException("Could not determine atom type for atom " + atom);
                }
                double mass = type.getMass();
                if (mass == 0.0) {
                    throw new CDKException("Could not determine mass for atom " + atom);
                }
                molecularWeight += mass;
                double charge = atom.getFormalCharge();
                if (charge == 0.0) {
                    charge = AtomTypeManipulator.getFormalCharge(type);
                }
                if (charge == 0.0) {
                    throw new CDKException("Could not determine formal charge for atom " + atom);
                }
                double isotope = atom.getMassNumber();
                if (isotope == 0.0) {
                    isotope = AtomTypeManipulator.getMassNumber(type);
                }
                if (isotope == 0.0) {
                    throw new CDKException("Could not determine isotope for atom " + atom);
                }
                double isotopeMass = Isotopes.getIsotopeMass(isotope);
                if (isotopeMass == 0.0) {
                    throw new CDKException("Could not determine isotope mass for atom " + atom);
                }
                double isotopeMassDifference = isotopeMass - mass;
                if (isotopeMassDifference == 0.0) {
                    throw new CDKException("Could not determine isotope mass difference for atom " + atom);
                }
                double isotopeMassDifferenceMass = isotopeMassDifference * charge;
                if (isotopeMassDifferenceMass == 0.0)       
 }

 

}