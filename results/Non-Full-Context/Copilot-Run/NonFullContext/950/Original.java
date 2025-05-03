/* 
  *  Copyright (C) 2002-2007  The Chemistry Development Kit (CDK) project
  *                     2014  Mark B Vine (orcid:0000-0002-7794-0426)
  *
  *  Contact: cdk-devel@lists.sourceforge.net
  *
  *  This program is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU Lesser General Public License
  *  as published by the Free Software Foundation; either version 2.1
  *  of the License, or (at your option) any later version.
  *  All I ask is that proper credit is given for my work, which includes
  *  - but is not limited to - adding the above copyright notice to the beginning
  *  of your source code files, and to any copyright notice that you may distribute
  *  with programs based on this work.
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
 package org.openscience.cdk.smiles;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.aromaticity.Aromaticity;
 import org.openscience.cdk.atomtype.CDKAtomTypeMatcher;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.graph.Cycles;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomContainerSet;
 import org.openscience.cdk.interfaces.IAtomType;
 import org.openscience.cdk.interfaces.IAtomType.Hybridization;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IRing;
 import org.openscience.cdk.interfaces.IRingSet;
 import org.openscience.cdk.ringsearch.AllRingsFinder;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.RingManipulator;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 
 /**
  * Tool that tries to deduce bond orders based on connectivity and hybridization
  * for a number of common ring systems of up to seven-membered rings. It assumes
  * that atom types have been perceived before that class is used.
  *
  * <p>The calculation can be interrupted with {@link #setInterrupted(boolean)},
  * but assumes that this class is not used in a threaded fashion. When a calculation
  * is interrupted, the boolean is reset to false.
  *
  * @author Todd Martin
  * @cdk.module smiles
  * @cdk.githash
  * @cdk.keyword bond order
  *
  * @cdk.bug 1895805
  * @cdk.bug 1931262
  *
  * @cdk.threadnonsafe
  * @deprecated Use the newer {@link org.openscience.cdk.aromaticity.Kekulization}
  */
 @Deprecated
 public class DeduceBondSystemTool {
 
     private AllRingsFinder      allRingsFinder;
     private static ILoggingTool logger      = LoggingToolFactory.createLoggingTool(DeduceBondSystemTool.class);
 
     private List<Integer[]>     listOfRings = null;
 
     private boolean             interrupted;
 
     /**
      * Constructor for the DeduceBondSystemTool object.
      */
     public DeduceBondSystemTool() {
         allRingsFinder = new AllRingsFinder();
     }
 
     /**
      * Constructor for the DeduceBondSystemTool object accepting a custom {@link AllRingsFinder}.
      *
      * @param ringFinder a custom {@link AllRingsFinder}.
      */
     public DeduceBondSystemTool(AllRingsFinder ringFinder) {
         allRingsFinder = ringFinder;
     }
 
 
/** Determines if, according to the algorithms implemented in this class, the given  AtomContainer has properly distributed double bonds. */
 public boolean isOK(IAtomContainer m) throws CDKException{}

 

}