/* Copyright (C) 2006-2007  Sam Adams <sea36@users.sf.net>
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public License
  * as published by the Free Software Foundation; either version 2.1
  * of the License, or (at your option) any later version.
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
 package org.openscience.cdk.inchi;
 
 import io.github.dan2097.jnainchi.InchiAtom;
 import io.github.dan2097.jnainchi.InchiBond;
 import io.github.dan2097.jnainchi.InchiBondStereo;
 import io.github.dan2097.jnainchi.InchiBondType;
 import io.github.dan2097.jnainchi.InchiInput;
 import io.github.dan2097.jnainchi.InchiInputFromInchiOutput;
 import io.github.dan2097.jnainchi.InchiOptions;
 import io.github.dan2097.jnainchi.InchiStatus;
 import io.github.dan2097.jnainchi.InchiStereo;
 import io.github.dan2097.jnainchi.InchiStereoParity;
 import io.github.dan2097.jnainchi.InchiStereoType;
 import io.github.dan2097.jnainchi.JnaInchi;
 import net.sf.jniinchi.INCHI_RET;
 import org.openscience.cdk.config.Elements;
 import org.openscience.cdk.config.Isotopes;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.interfaces.IStereoElement;
 import org.openscience.cdk.interfaces.ITetrahedralChirality;
 import org.openscience.cdk.stereo.DoubleBondStereochemistry;
 import org.openscience.cdk.stereo.ExtendedCisTrans;
 import org.openscience.cdk.stereo.ExtendedTetrahedral;
 
 import java.io.IOException;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 /**
  * <p>This class generates a CDK IAtomContainer from an InChI string.  It places
  * calls to a JNI wrapper for the InChI C++ library.
  *
  * <p>The generated IAtomContainer will have all 2D and 3D coordinates set to 0.0,
  * but may have atom parities set.  Double bond and allene stereochemistry are
  * not currently recorded.
  *
  * <br>
  * <b>Example usage</b>
  *
  * <code>// Generate factory - throws CDKException if native code does not load</code><br>
  * <code>InChIGeneratorFactory factory = new InChIGeneratorFactory();</code><br>
  * <code>// Get InChIToStructure</code><br>
  * <code>InChIToStructure intostruct = factory.getInChIToStructure(</code><br>
  * <code>  inchi, DefaultChemObjectBuilder.getInstance()</code><br>
  * <code>);</code><br>
  * <code></code><br>
  * <code>INCHI_RET ret = intostruct.getReturnStatus();</code><br>
  * <code>if (ret == INCHI_RET.WARNING) {</code><br>
  * <code>  // Structure generated, but with warning message</code><br>
  * <code>  System.out.println("InChI warning: " + intostruct.getMessage());</code><br>
  * <code>} else if (ret != INCHI_RET.OKAY) {</code><br>
  * <code>  // Structure generation failed</code><br>
  * <code>  throw new CDKException("Structure generation failed failed: " + ret.toString()</code><br>
  * <code>    + " [" + intostruct.getMessage() + "]");</code><br>
  * <code>}</code><br>
  * <code></code><br>
  * <code>IAtomContainer container = intostruct.getAtomContainer();</code><br>
  * <p><br>
  *
  * @author Sam Adams
  *
  * @cdk.module inchi
  * @cdk.githash
  */
 public class InChIToStructure {
 
     protected InchiInputFromInchiOutput output;
 
     protected InchiOptions options;
 
     protected IAtomContainer          molecule;
 
     // magic number - indicates isotope mass is relative
     private static final int          ISOTOPIC_SHIFT_FLAG = 10000;
     /**
      * JNI-Inchi uses the magic number {#link ISOTOPIC_SHIFT_FLAG} plus the
      * (possibly negative) relative mass. So any isotope value
      * coming back from jni-inchi greater than this threshold value
      * should be treated as a relative mass.
      */
     private static final int ISOTOPIC_SHIFT_THRESHOLD = ISOTOPIC_SHIFT_FLAG - 100;
 
     /**
      * Constructor. Generates CDK AtomContainer from InChI.
      * @param inchi
      * @throws CDKException
      */
     protected InChIToStructure(String inchi, IChemObjectBuilder builder, InchiOptions options) throws CDKException {
         if (inchi == null)
             throw new IllegalArgumentException("Null InChI string provided");
         if (options == null)
             throw new IllegalArgumentException("Null options provided");
         this.output = JnaInchi.getInchiInputFromInchi(inchi);
         this.options = options;
         generateAtomContainerFromInchi(builder);
     }
 
     /**
      * Constructor. Generates CDK AtomContainer from InChI.
      * @param inchi
      * @throws CDKException
      */
     protected InChIToStructure(String inchi, IChemObjectBuilder builder) throws CDKException {
         this(inchi, builder, new InchiOptions.InchiOptionsBuilder().build());
     }
 
     /**
      * Constructor. Generates CMLMolecule from InChI.
      * @param inchi
      * @param options
      * @throws CDKException
      */
     protected InChIToStructure(String inchi, IChemObjectBuilder builder, String options) throws CDKException {
         this(inchi, builder, InChIOptionParser.parseString(options));
     }
 
     /**
      * Constructor. Generates CMLMolecule from InChI.
      * @param inchi
      * @param options
      * @throws CDKException
      */
     protected InChIToStructure(String inchi, IChemObjectBuilder builder, List<String> options) throws CDKException {
         this(inchi, builder, InChIOptionParser.parseStrings(options));
     }
 
 
/** The storage order of atoms should be changed. */

private void flip(IBond bond) {
    // The storage order of atoms should be changed.
    IAtom[] atoms = bond.getAtoms();
    IAtom temp = atoms[0];
    atoms[0] = atoms[1];
    atoms[1] = temp;
    bond.setAtoms(atoms[0], atoms[1]);
}
 

}