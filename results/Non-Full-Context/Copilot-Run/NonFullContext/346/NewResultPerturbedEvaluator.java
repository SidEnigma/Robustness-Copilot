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
 import org.openscience.cdk.graph.Cycles;
 import org.openscience.cdk.graph.GraphUtil;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.qsar.AbstractMolecularDescriptor;
 import org.openscience.cdk.qsar.DescriptorSpecification;
 import org.openscience.cdk.qsar.DescriptorValue;
 import org.openscience.cdk.qsar.result.IDescriptorResult;
 import org.openscience.cdk.qsar.result.IntegerResult;
 
 /**
  * Counts the number of atoms in the longest aliphatic chain.
  * <p>
  * <table border="1"><caption>Parameters for this descriptor:</caption>
  * <tr>
  * <td>Name</td>
  * <td>Default</td>
  * <td>Description</td>
  * </tr>
  * <tr>
  * <td>checkRingSystem</td>
  * <td>false</td>
  * <td>True is the CDKConstant.ISINRING has to be set</td>
  * </tr>
  * </table>
  * <p>
  * Returns a single value named <i>nAtomLAC</i>
  *
  * @author chhoppe from EUROSCREEN
  * @author John Mayfield
  * @cdk.created 2006-1-03
  * @cdk.module qsarmolecular
  * @cdk.githash
  * @cdk.dictref qsar-descriptors:largestAliphaticChain
  */
 public class LongestAliphaticChainDescriptor
     extends AbstractMolecularDescriptor {
 
     public static final String CHECK_RING_SYSTEM = "checkRingSystem";
     private boolean checkRingSystem = false;
 
     private static final String[] NAMES = {"nAtomLAC"};
 
     /**
      * Constructor for the LongestAliphaticChainDescriptor object.
      */
     public LongestAliphaticChainDescriptor() {
     }
 
     /**
      * Returns a <code>Map</code> which specifies which descriptor
      * is implemented by this class.
      * <p>
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
             "http://www.blueobelisk.org/ontologies/chemoinformatics-algorithms/#longestAliphaticChain", this
             .getClass().getName(), "The Chemistry Development Kit");
     }
 
     /**
      * Sets the parameters attribute of the LongestAliphaticChainDescriptor object.
      * <p>
      * This descriptor takes one parameter, which should be Boolean to indicate whether
      * aromaticity has been checked (TRUE) or not (FALSE).
      *
      * @param params The new parameters value
      * @throws CDKException if more than one parameter or a non-Boolean parameter is specified
      * @see #getParameters
      */
     @Override
     public void setParameters(Object[] params) throws CDKException {
         if (params.length > 1)
             throw new CDKException("LongestAliphaticChainDescriptor only expects one parameter");
         if (!(params[0] instanceof Boolean))
             throw new CDKException("Expected parameter of type " + Boolean.class);
         checkRingSystem = (Boolean) params[0];
     }
 
     /**
      * Gets the parameters attribute of the LongestAliphaticChainDescriptor object.
      *
      * @return The parameters value
      * @see #setParameters
      */
     @Override
     public Object[] getParameters() {
         // return the parameters as used for the descriptor calculation
         Object[] params = new Object[1];
         params[0] = checkRingSystem;
         return params;
     }
 
     @Override
     public String[] getDescriptorNames() {
         return NAMES;
     }
 
     private DescriptorValue getDummyDescriptorValue(Exception e) {
         return new DescriptorValue(getSpecification(),
                                    getParameterNames(),
                                    getParameters(),
                                    new IntegerResult(0),
                                    getDescriptorNames(),
                                    e);
     }
 
     private static boolean isAcyclicCarbon(IAtom atom) {
         return atom.getAtomicNumber() == 6 && !atom.isInRing();
     }
 
     /**
      * Depth-First-Search on an acyclic graph. Since we have no cycles we
      * don't need the visit flags and only need to know which atom we came from.
      *
      * @param adjlist adjacency list representation of grah
      * @param v       the current atom index
      * @param prev    the previous atom index
      * @return the max length traversed
      */
     private static int getMaxDepth(int[][] adjlist, int v, int prev) {
         int longest = 0;
         for (int w : adjlist[v]) {
             if (w == prev) continue;
             // no cycles so don't need to check previous
             int length = getMaxDepth(adjlist, w, v);
             if (length > longest)
                 longest = length;
         }
         return 1 + longest;
     }
 
 
/** Given a IAtomContainer, it computes the highest number of atoms of the longest aliphatic chain. */

public DescriptorValue calculate(IAtomContainer mol) {
    try {
        int[][] adjlist = GraphUtil.toAdjList(mol);
        int maxChainLength = 0;
        
        for (IAtom atom : mol.atoms()) {
            if (isAcyclicCarbon(atom)) {
                int chainLength = getMaxDepth(adjlist, mol.indexOf(atom), -1);
                if (chainLength > maxChainLength) {
                    maxChainLength = chainLength;
                }
            }
        }
        
        IDescriptorResult result = new IntegerResult(maxChainLength);
        return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), result, getDescriptorNames(), null);
    } catch (Exception e) {
        return getDummyDescriptorValue(e);
    }
}
 

}