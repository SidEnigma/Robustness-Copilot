/*  Copyright (C) 2005-2007  Christian Hoppe <chhoppe@users.sf.net>
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
 import org.openscience.cdk.aromaticity.Aromaticity;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IElement;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.qsar.AbstractMolecularDescriptor;
 import org.openscience.cdk.qsar.DescriptorSpecification;
 import org.openscience.cdk.qsar.DescriptorValue;
 import org.openscience.cdk.qsar.IMolecularDescriptor;
 import org.openscience.cdk.qsar.result.IDescriptorResult;
 import org.openscience.cdk.qsar.result.IntegerResult;
 import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
 
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * Class that returns the number of atoms in the largest pi system.
  * 
  * <table border="1"><caption>Parameters for this descriptor:</caption>
  * <tr>
  * <td>Name</td>
  * <td>Default</td>
  * <td>Description</td>
  * </tr>
  * <tr>
  * <td>checkAromaticity</td>
  * <td>false</td>
  * <td>True is the aromaticity has to be checked</td>
  * </tr>
  * </table>
  * 
  * Returns a single value named <i>nAtomPi</i>
  *
  * @author chhoppe from EUROSCREEN
  * @cdk.created 2006-1-03
  * @cdk.module qsarmolecular
  * @cdk.githash
  * @cdk.dictref qsar-descriptors:largestPiSystem
  */
 public class LargestPiSystemDescriptor extends AbstractMolecularDescriptor implements IMolecularDescriptor {
 
     private boolean               checkAromaticity = false;
     private static final String[] NAMES            = {"nAtomP"};
 
     /**
      * Constructor for the LargestPiSystemDescriptor object.
      */
     public LargestPiSystemDescriptor() {}
 
     /**
      * Returns a <code>Map</code> which specifies which descriptor
      * is implemented by this class.
      * 
      * These fields are used in the map:
      * <ul>
      * <li>Specification-Reference: refers to an entry in a unique dictionary
      * <li>Implementation-Title: anything
      * <li>Implementation-Identifier: a unique identifier for this version of
      * this class
      * <li>Implementation-Vendor: CDK, JOELib, or anything else
      * </ul>
      *
      * @return An object containing the descriptor specification
      */
     @Override
     public DescriptorSpecification getSpecification() {
         return new DescriptorSpecification(
                 "http://www.blueobelisk.org/ontologies/chemoinformatics-algorithms/#largestPiSystem", this.getClass()
                         .getName(), "The Chemistry Development Kit");
     }
 
     /**
      * Sets the parameters attribute of the LargestPiSystemDescriptor object.
      * 
      * This descriptor takes one parameter, which should be Boolean to indicate whether
      * aromaticity has been checked (TRUE) or not (FALSE).
      *
      * @param params The new parameters value
      * @throws CDKException if more than one parameter or a non-Boolean parameter is specified
      * @see #getParameters
      */
     @Override
     public void setParameters(Object[] params) throws CDKException {
         if (params.length > 1) {
             throw new CDKException("LargestPiSystemDescriptor only expects one parameter");
         }
         if (!(params[0] instanceof Boolean)) {
             throw new CDKException("The first parameter must be of type Boolean");
         }
         // ok, all should be fine
         checkAromaticity = (Boolean) params[0];
     }
 
     /**
      * Gets the parameters attribute of the LargestPiSystemDescriptor object.
      *
      * @return The parameters value
      * @see #setParameters
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
 
     private DescriptorValue getDummyDescriptorValue(Exception e) {
         return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), new IntegerResult(
                 (int) Double.NaN), getDescriptorNames(), e);
     }
 
 
/** Find the greatest pi system's atom count in the provided IAtomContainer. If "checkAromaticity" is true, the method checks the aromaticity. If "checkAromaticity" is false, it means that the aromaticity has already been verified. The method requires only one parameter. */

public DescriptorValue calculate(IAtomContainer container) {
    try {
        if (checkAromaticity) {
            Aromaticity aromaticity = new Aromaticity();
            aromaticity.apply(container);
        }
        
        List<List<IAtom>> piSystems = new ArrayList<>();
        List<IAtom> currentPiSystem = new ArrayList<>();
        
        for (IAtom atom : container.atoms()) {
            if (atom.getFlag(CDKConstants.ISAROMATIC)) {
                currentPiSystem.add(atom);
            } else {
                if (!currentPiSystem.isEmpty()) {
                    piSystems.add(currentPiSystem);
                    currentPiSystem = new ArrayList<>();
                }
            }
        }
        
        if (!currentPiSystem.isEmpty()) {
            piSystems.add(currentPiSystem);
        }
        
        int largestPiSystemSize = 0;
        for (List<IAtom> piSystem : piSystems) {
            if (piSystem.size() > largestPiSystemSize) {
                largestPiSystemSize = piSystem.size();
            }
        }
        
        return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(),
                new IntegerResult(largestPiSystemSize), getDescriptorNames());
    } catch (Exception e) {
        return getDummyDescriptorValue(e);
    }
}
 

}