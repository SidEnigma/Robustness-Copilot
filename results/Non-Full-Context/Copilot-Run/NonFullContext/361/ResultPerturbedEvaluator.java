/* Copyright (C) 2003-2007  The Chemistry Development Kit (CDK) project
  *                    2014  Mark B Vine (orcid:0000-0002-7794-0426)
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
 package org.openscience.cdk.io;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IChemFile;
 import org.openscience.cdk.interfaces.IChemModel;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.interfaces.IChemSequence;
 import org.openscience.cdk.interfaces.IMapping;
 import org.openscience.cdk.interfaces.IReaction;
 import org.openscience.cdk.interfaces.IReactionSet;
 import org.openscience.cdk.io.formats.IResourceFormat;
 import org.openscience.cdk.io.formats.MDLRXNFormat;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.Reader;
 import java.io.StringReader;
 import java.util.Iterator;
 import java.util.StringTokenizer;
 
 /**
  * Reads a molecule from an MDL RXN file {@cdk.cite DAL92}.
  *
  * @cdk.module io
  * @cdk.githash
  * @cdk.iooptions
  *
  * @author     Egon Willighagen
  * @cdk.created    2003-07-24
  *
  * @cdk.keyword    file format, MDL RXN
  * @deprecated Use V2000 or V3000
  */
 @Deprecated
 public class MDLRXNReader extends DefaultChemObjectReader {
 
     BufferedReader              input  = null;
     private static ILoggingTool logger = LoggingToolFactory.createLoggingTool(MDLRXNReader.class);
 
     /**
      * Constructs a new MDLReader that can read Molecule from a given Reader.
      *
      * @param  in  The Reader to read from
      */
     public MDLRXNReader(Reader in) {
         this(in, Mode.RELAXED);
     }
 
     public MDLRXNReader(Reader in, Mode mode) {
         if (in instanceof BufferedReader) {
             input = (BufferedReader) in;
         } else {
             input = new BufferedReader(in);
         }
         super.mode = mode;
     }
 
     public MDLRXNReader(InputStream input) {
         this(input, Mode.RELAXED);
     }
 
     public MDLRXNReader(InputStream input, Mode mode) {
         this(new InputStreamReader(input), mode);
     }
 
     public MDLRXNReader() {
         this(new StringReader(""));
     }
 
     @Override
     public IResourceFormat getFormat() {
         return MDLRXNFormat.getInstance();
     }
 
     @Override
     public void setReader(Reader input) throws CDKException {
         if (input instanceof BufferedReader) {
             this.input = (BufferedReader) input;
         } else {
             this.input = new BufferedReader(input);
         }
     }
 
     @Override
     public void setReader(InputStream input) throws CDKException {
         setReader(new InputStreamReader(input));
     }
 
     @Override
     public boolean accepts(Class<? extends IChemObject> classObject) {
         if (IChemFile.class.equals(classObject)) return true;
         if (IChemModel.class.equals(classObject)) return true;
         if (IReaction.class.equals(classObject)) return true;
         if (IReactionSet.class.equals(classObject)) return true;
         Class<?>[] interfaces = classObject.getInterfaces();
         for (int i = 0; i < interfaces.length; i++) {
             if (IChemModel.class.equals(interfaces[i])) return true;
             if (IChemFile.class.equals(interfaces[i])) return true;
             if (IReaction.class.equals(interfaces[i])) return true;
             if (IReactionSet.class.equals(interfaces[i])) return true;
         }
         Class superClass = classObject.getSuperclass();
         if (superClass != null) return this.accepts(superClass);
         return false;
     }
 
     /**
       * Takes an object which subclasses IChemObject, e.g.Molecule, and will read
       * this (from file, database, internet etc). If the specific implementation
       * does not support a specific IChemObject it will throw an Exception.
       *
       * @param  object                              The object that subclasses
       *      IChemObject
       * @return                                     The IChemObject read
       * @exception  CDKException
       */
     @Override
     public <T extends IChemObject> T read(T object) throws CDKException {
         if (object instanceof IChemFile) {
             return (T) readChemFile((IChemFile) object);
         } else if (object instanceof IChemModel) {
             return (T) readChemModel((IChemModel) object);
         } else if (object instanceof IReactionSet) {
             return (T) readReactionSet((IReactionSet) object);
         } else if (object instanceof IReaction) {
             return (T) readReaction(object.getBuilder());
         } else {
             throw new CDKException("Only supported are Reaction, ReactionSet, ChemModel and ChemFile, and not "
                     + object.getClass().getName() + ".");
         }
     }
 
     public boolean accepts(IChemObject object) {
         if (object instanceof IReaction) {
             return true;
         } else if (object instanceof IChemModel) {
             return true;
         } else if (object instanceof IChemFile) {
             return true;
         } else if (object instanceof IReactionSet) {
             return true;
         }
         return false;
     }
 
 
/** Using a file in MDL RDF format, read a ChemFile */
 private IChemFile readChemFile(IChemFile chemFile) throws CDKException{
        IChemSequence chemSequence = chemFile.getBuilder().newInstance(IChemSequence.class);
        chemFile.setChemSequence(chemSequence);
        IChemSequence reactionSequence = chemFile.getBuilder().newInstance(IChemSequence.class);
        chemFile.setReactionSequence(reactionSequence);
        IChemSequence modelSequence = chemFile.getBuilder().newInstance(IChemSequence.class);
        chemFile.setModelSequence(modelSequence);
        String line;
        try {
            while ((line = input.readLine()) != null) {
                if (line.startsWith("$RXN")) {
                    IReaction reaction = readReaction(chemFile.getBuilder());
                    reactionSequence.addChemSequence(reaction);
                } else if (line.startsWith("$MOL")) {
                    IChemModel model = readChemModel(chemFile.getBuilder());
                    modelSequence.addChemSequence(model);
                } else if (line.startsWith("$MDL")) {
                    IChemModel model = readChemModel(chemFile.getBuilder());
                    modelSequence.addChemSequence(model);
                } else if (line.startsWith("$END")) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new CDKException("IOException while reading file: " + e.getMessage(), e);
        }
        return chemFile;        
 }

 

}