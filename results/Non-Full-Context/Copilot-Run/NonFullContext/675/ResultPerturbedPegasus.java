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
 
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.graph.AllPairsShortestPaths;
 import org.openscience.cdk.graph.Cycles;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.qsar.AbstractMolecularDescriptor;
 import org.openscience.cdk.qsar.DescriptorSpecification;
 import org.openscience.cdk.qsar.DescriptorValue;
 import org.openscience.cdk.qsar.IMolecularDescriptor;
 import org.openscience.cdk.qsar.result.IDescriptorResult;
 import org.openscience.cdk.qsar.result.IntegerResult;
 
 import java.util.HashSet;
 import java.util.Set;
 
 /**
  * Class that returns the number of atoms in the largest chain.
  * 
  * <table border="1"><caption>Parameters for this descriptor:</caption>
  * <tr>
  * <td>Name</td>
  * <td>Default</td>
  * <td>Description</td>
  * </tr>
  * <tr>
  * <td>checkAromaticity (deprecated)</td>
  * <td>false</td>
  * <td>Old parameter is now ignored</td>
  * </tr>
  * <tr>
  * <td>checkRingSystem</td>
  * <td>false</td>
  * <td>True is the CDKConstant.ISINRING has to be set</td>
  * </tr>
  * </table>
  * 
  * Returns a single value named <i>nAtomLAC</i>. Note that a chain exists if there
  * are two or more atoms. Thus single atom molecules will return 0
  *
  * @author chhoppe from EUROSCREEN
  * @cdk.created 2006-1-03
  * @cdk.module qsarmolecular
  * @cdk.githash
  * @cdk.dictref qsar-descriptors:largestChain
  */
 public class LargestChainDescriptor extends AbstractMolecularDescriptor implements IMolecularDescriptor {
 
     private boolean               checkAromaticity = false;
     private boolean               checkRingSystem  = false;
     private static final String[] NAMES            = {"nAtomLC"};
 
     /**
      * Constructor for the LargestChain object.
      */
     public LargestChainDescriptor() {}
 
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
                 "http://www.blueobelisk.org/ontologies/chemoinformatics-algorithms/#largestChain", this.getClass()
                         .getName(), "The Chemistry Development Kit");
     }
 
     /**
      * Sets the parameters attribute of the LargestChain object.
      * 
      * This descriptor takes two parameters, which should be Booleans to indicate whether
      * aromaticity and ring member ship needs been checked (TRUE) or not (FALSE). The first
      * parameter (aromaticity) is deprecated and ignored.
      *
      * @param params The new parameters value
      * @throws CDKException if more than one parameter or a non-Boolean parameter is specified
      * @see #getParameters
      */
     @Override
     public void setParameters(Object[] params) throws CDKException {
         if (params.length > 2) {
             throw new CDKException("LargestChainDescriptor only expects two parameter");
         }
         if (!(params[0] instanceof Boolean) || !(params[1] instanceof Boolean)) {
             throw new CDKException("Both parameters must be of type Boolean");
         }
         // ok, all should be fine
         checkAromaticity = (Boolean) params[0];
         checkRingSystem = (Boolean) params[1];
     }
 
     /**
      * Gets the parameters attribute of the LargestChainDescriptor object.
      *
      * @return The parameters value
      * @see #setParameters
      */
     @Override
     public Object[] getParameters() {
         // return the parameters as used for the descriptor calculation
         Object[] params = new Object[2];
         params[0] = checkAromaticity;
         params[1] = checkRingSystem;
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
 
 
/** The supplied IAtomContainer has a count of the largest chain. */
 public DescriptorValue calculate(IAtomContainer atomContainer){
        try {
            int nAtomLAC = 0;
            int nAtom = atomContainer.getAtomCount();
            int nBond = atomContainer.getBondCount();
            int[][] adjacencyMatrix = new int[nAtom][nAtom];
            for (int i = 0; i < nAtom; i++) {
                for (int j = 0; j < nAtom; j++) {
                    adjacencyMatrix[i][j] = 0;
                }
            }
            for (IBond bond : atomContainer.bonds()) {
                int i = atomContainer.getAtomNumber(bond.getBegin());
                int j = atomContainer.getAtomNumber(bond.getEnd());
                adjacencyMatrix[i][j] = 1;
                adjacencyMatrix[j][i] = 1;
            }
            AllPairsShortestPaths allPairsShortestPaths = new AllPairsShortestPaths(adjacencyMatrix);
            int[][] distanceMatrix = allPairsShortestPaths.getDistanceMatrix();
            for (int i = 0; i < nAtom; i++) {
                for (int j = 0; j < nAtom; j++) {
                    if (distanceMatrix[i][j] > 1) {
                        nAtomLAC++;
                    }
                }
            }
            return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), new IntegerResult(
                    nAtomLAC), getDescriptorNames());
        } catch (Exception e) {
            return getDummyDescriptorValue(e);
        }
    }
    
        @Override
        public DescriptorValue calculate(IAtomContainer container, String descriptorName) {
            return calculate(container);
        }
    
        @Override
        public DescriptorValue calculate(IAtomContainer container, String descriptorName, Object parameter) {
            return calculate(container);
        }
    
        @Override
        public DescriptorValue calculate(IAtomContainer container, String descriptorName, Object[] parameters) {
            return      
 }

 

}