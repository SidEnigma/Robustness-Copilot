/* Copyright (C) 1997-2007  The Chemistry Development Kit (CDK) project
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
 package org.openscience.cdk.structgen;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.openscience.cdk.graph.ConnectivityChecker;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.BondManipulator;
 
 /**
  * The VicinitySampler is a generator of constitutional isomers. It needs to be
  * provided with a starting constitution and it makes random moves in
  * constitutional space from there. This generator was first suggested by
  * Faulon {@cdk.cite FAU96}.
  *
  * @cdk.keyword  structure generator
  * @cdk.module   structgen
  * @cdk.githash
  * @cdk.bug      1632610
  */
 public class VicinitySampler {
 
     private final static ILoggingTool LOGGER     = LoggingToolFactory.createLoggingTool(VicinitySampler.class);
 
     int                               molCounter = 0;
 
 
/** Sample any possible quadruple of the set of atoms in ac and establish all of the possible bonding schemes according to Faulon's equations */
 public static List<IAtomContainer> sample(IAtomContainer ac){
      List<IAtomContainer> result = new ArrayList<IAtomContainer>();
      int nAtoms = ac.getAtomCount();
      int nBonds = ac.getBondCount();
      int nBondsMax = nAtoms*(nAtoms-1)/2;
      int nBondsMin = nBonds;
      int nBondsMax2 = nBondsMax;
      int nBondsMin2 = nBondsMin;
      int nBondsMax3 = nBondsMax;
      int nBondsMin3 = nBondsMin;
      int nBondsMax4 = nBondsMax;
      int nBondsMin4 = nBondsMin;
      int nBondsMax5 = nBondsMax;
      int nBondsMin5 = nBondsMin;
      int nBondsMax6 = nBondsMax;
      int nBondsMin6 = nBondsMin;
      int nBondsMax7 = nBondsMax;
      int nBondsMin7 = nBondsMin;
      int nBondsMax8 = nBondsMax;
      int nBondsMin8 = nBondsMin;
      int nBondsMax9 = nBondsMax;
      int nBondsMin9 = nBondsMin;
      int nBondsMax10 = nBondsMax;
      int nBondsMin10 = nBondsMin;
      int nBondsMax11 = nBondsMax;
      int nBondsMin11 = nBondsMin;
      int nBondsMax12 = nBondsMax;
      int nBondsMin12 = nBondsMin;
      int nBondsMax13 = nBondsMax;
      int nBondsMin13 = nBondsMin;
      int nBondsMax14 = nBondsMax;
      int nBondsMin14 = nBondsMin;
      int nBondsMax15 = nBondsMax;
      int nBondsMin15 = nBondsMin;
      int nBondsMax16 = nBondsMax;
      int nBondsMin16 = nBonds  
 }

 

}