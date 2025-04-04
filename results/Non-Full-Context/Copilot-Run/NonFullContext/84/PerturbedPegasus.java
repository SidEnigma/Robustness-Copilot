/* Copyright (C) 2010  Rajarshi Guha <rajarshi.guha@gmail.com>
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
 package org.openscience.cdk.fragment;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.aromaticity.Aromaticity;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.graph.ConnectivityChecker;
 import org.openscience.cdk.graph.PathTools;
 import org.openscience.cdk.hash.HashGeneratorMaker;
 import org.openscience.cdk.hash.MoleculeHashGenerator;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomContainerSet;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IRingSet;
 import org.openscience.cdk.interfaces.IStereoElement;
 import org.openscience.cdk.ringsearch.AllRingsFinder;
 import org.openscience.cdk.ringsearch.RingSearch;
 import org.openscience.cdk.smiles.SmilesGenerator;
 import org.openscience.cdk.tools.CDKHydrogenAdder;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
 
 import java.util.ArrayDeque;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Deque;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 /**
  * An implementation of the Murcko fragmenation method {@cdk.cite MURCKO96}.
  * 
  * As an implementation of {@link IFragmenter} this class will return
  * the Murcko frameworks (i.e., ring systems + linkers) along with
  * the ring systems ia getFragments. The
  * class also provides methods to extract the ring systems and frameworks
  * separately. For all these methods, the user can retrieve the substructures
  * as canonical SMILES strings or as {@link IAtomContainer} objects.
  * 
  * Note that in contrast to the original paper which implies that a single molecule
  * has a single framework, this class returns multiple frameworks consisting of all
  * combinations of ring systems and linkers. The "true" Murcko framework is simply
  * the largest framework.
  *
  * @author Rajarshi Guha
  * @cdk.module fragment
  * @cdk.githash
  * @cdk.keyword fragment
  * @cdk.keyword framework
  * @see org.openscience.cdk.fragment.ExhaustiveFragmenter
  */
 public class MurckoFragmenter implements IFragmenter {
 
     private static final String IS_SIDECHAIN_ATOM    = "sidechain";
     private static final String IS_LINKER_ATOM       = "linker";
     private static final String IS_CONNECTED_TO_RING = "rcon";
 
     MoleculeHashGenerator       generator;
     SmilesGenerator             smigen;
 
     Map<Long, IAtomContainer>   frameMap             = new HashMap<Long, IAtomContainer>();
     Map<Long, IAtomContainer>   ringMap              = new HashMap<Long, IAtomContainer>();
 
     boolean                     singleFrameworkOnly  = false;
     boolean                     ringFragments        = true;
     int                         minimumFragmentSize  = 5;
 
     /**
      * Instantiate Murcko fragmenter.
      * 
      * Considers fragments with 5 or more atoms and generates multiple
      * frameworks if available.
      */
     public MurckoFragmenter() {
         this(true, 5, null);
     }
 
     /**
      * Instantiate Murcko fragmenter.
      *
      * @param singleFrameworkOnly if <code>true</code>, only the true Murcko framework is generated.
      * @param minimumFragmentSize the smallest size of fragment to consider
      */
     public MurckoFragmenter(boolean singleFrameworkOnly, int minimumFragmentSize) {
         this(singleFrameworkOnly, minimumFragmentSize, null);
     }
 
     /**
      * Instantiate Murcko fragmenter.
      *
      * @param singleFrameworkOnly if <code>true</code>, only the true Murcko framework is generated.
      * @param minimumFragmentSize the smallest size of fragment to consider
      * @param generator           An instance of a {@link MoleculeHashGenerator} to be used to check for
      *                            duplicate fragments
      */
     public MurckoFragmenter(boolean singleFrameworkOnly, int minimumFragmentSize, MoleculeHashGenerator generator) {
         this.singleFrameworkOnly = singleFrameworkOnly;
         this.minimumFragmentSize = minimumFragmentSize;
 
         if (generator == null)
             this.generator = new HashGeneratorMaker().depth(8).elemental().isotopic().charged().orbital().molecular();
         else
             this.generator = generator;
 
         smigen = SmilesGenerator.unique().aromatic();
     }
 
     /**
      * Sets whether to calculate ring fragments (true by default).
      *
      * @param val true/false
      */
     public void setComputeRingFragments(boolean val) {
         this.ringFragments = val;
     }
 
     /**
      * Perform the fragmentation procedure.
      *
      * @param atomContainer The input molecule
      * @throws CDKException
      */
     @Override
     public void generateFragments(IAtomContainer atomContainer) throws CDKException {
         Set<Long> fragmentSet = new HashSet<Long>();
         frameMap.clear();
         ringMap.clear();
         run(atomContainer, fragmentSet);
     }
 
 
/** The Murcko Scaffold is computed for a molecule in a linear time. */
 public static IAtomContainer scaffold(final IAtomContainer mol){}

 

}