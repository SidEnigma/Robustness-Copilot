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
 
 
/** Pick any possible set of atoms in ac and then use the equations of Faulon to create bonding schemes. */

    public static List<IAtomContainer> sample(IAtomContainer ac) {
        List<IAtomContainer> samples = new ArrayList<>();

        // Get all atoms from the atom container
        List<IAtom> atoms = ac.atoms();

        // Generate all possible combinations of atoms
        for (int i = 0; i < atoms.size(); i++) {
            for (int j = i + 1; j < atoms.size(); j++) {
                // Create a new atom container for each combination
                IAtomContainer sample = ac.getBuilder().newInstance(IAtomContainer.class);

                // Add the selected atoms to the sample
                sample.addAtom(atoms.get(i));
                sample.addAtom(atoms.get(j));

                // Check if the sample is connected
                if (ConnectivityChecker.isConnected(sample)) {
                    // Add the sample to the list of samples
                    samples.add(sample);
                }
            }
        }

        return samples;
    }
 

}