/* Copyright (C) 2005-2007  Egon Willighagen <egonw@users.sf.net>
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  *  This library is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU Lesser General Public
  *  License as published by the Free Software Foundation; either
  *  version 2.1 of the License, or (at your option) any later version.
  *
  *  This library is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  *  Lesser General Public License for more details.
  *
  *  You should have received a copy of the GNU Lesser General Public
  *  License along with this library; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 package org.openscience.cdk.io;
 
 import java.io.BufferedWriter;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.io.StringWriter;
 import java.io.Writer;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.atomtype.SybylAtomTypeMatcher;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomType;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.io.formats.IResourceFormat;
 import org.openscience.cdk.io.formats.Mol2Format;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 
 /**
  * An output Writer that writes molecular data into the
  * <a href="http://www.tripos.com/data/support/mol2.pdf">Tripos Mol2 format</a>.
  * Writes the atoms and the bonds only at this moment.
  *
  * @cdk.module io
  * @cdk.githash
  * @cdk.iooptions
  *
  * @author     Egon Willighagen
  */
 public class Mol2Writer extends DefaultChemObjectWriter {
 
     private BufferedWriter       writer;
     private static ILoggingTool  logger = LoggingToolFactory.createLoggingTool(Mol2Writer.class);
     private SybylAtomTypeMatcher matcher;
 
     public Mol2Writer() {
         this(new StringWriter());
     }
 
     /**
      * Constructs a new Mol2 writer.
      * @param out the stream to write the Mol2 file to.
      */
     public Mol2Writer(Writer out) {
         try {
             if (out instanceof BufferedWriter) {
                 writer = (BufferedWriter) out;
             } else {
                 writer = new BufferedWriter(out);
             }
         } catch (Exception exc) {
         }
     }
 
     public Mol2Writer(OutputStream output) {
         this(new OutputStreamWriter(output));
     }
 
     @Override
     public IResourceFormat getFormat() {
         return Mol2Format.getInstance();
     }
 
     @Override
     public void setWriter(Writer out) throws CDKException {
         if (out instanceof BufferedWriter) {
             writer = (BufferedWriter) out;
         } else {
             writer = new BufferedWriter(out);
         }
     }
 
     @Override
     public void setWriter(OutputStream output) throws CDKException {
         setWriter(new OutputStreamWriter(output));
     }
 
     /**
      * Flushes the output and closes this object.
      */
     @Override
     public void close() throws IOException {
         writer.close();
     }
 
     @Override
     public boolean accepts(Class<? extends IChemObject> classObject) {
         if (IAtomContainer.class.equals(classObject)) return true;
         Class<?>[] interfaces = classObject.getInterfaces();
         for (Class<?> anInterface : interfaces) {
             if (IAtomContainer.class.equals(anInterface)) return true;
         }
         Class superClass = classObject.getSuperclass();
         if (superClass != null) return this.accepts(superClass);
         return false;
     }
 
     @Override
     public void write(IChemObject object) throws CDKException {
         if (object instanceof IAtomContainer) {
             try {
                 writeMolecule((IAtomContainer) object);
             } catch (Exception ex) {
                 throw new CDKException("Error while writing Mol2 file: " + ex.getMessage(), ex);
             }
         } else {
             throw new CDKException("Mol2Writer only supports output of Molecule classes.");
         }
     }
 
 
/** A single frame is written in a format called the writer's format. */
 public void writeMolecule(IAtomContainer mol) throws IOException{}

                                                                                                                              }
