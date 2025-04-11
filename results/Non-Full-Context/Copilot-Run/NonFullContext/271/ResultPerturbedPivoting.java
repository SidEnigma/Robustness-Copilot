/* Copyright (C) 2002  Bradley A. Smith <bradley@baysmith.com>
  *               2002  Miguel Howard
  *               2003-2007  Egon Willighagen <egonw@users.sf.net>
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
 import java.util.Iterator;
 
 import javax.vecmath.Point3d;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.io.formats.IResourceFormat;
 import org.openscience.cdk.io.formats.XYZFormat;
 import org.openscience.cdk.tools.FormatStringBuffer;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 
 /**
  * @cdk.module io
  * @cdk.githash
  * @cdk.iooptions
  *
  * @author Bradley A. Smith &lt;bradley@baysmith.com&gt;
  * @author  J. Daniel Gezelter
  * @author  Egon Willighagen
  */
 public class XYZWriter extends DefaultChemObjectWriter {
 
     private BufferedWriter      writer;
     private static ILoggingTool logger = LoggingToolFactory.createLoggingTool(XYZWriter.class);
     private FormatStringBuffer  fsb;
 
     /**
     * Constructor.
     *
     * @param out the stream to write the XYZ file to.
     */
     public XYZWriter(Writer out) {
         fsb = new FormatStringBuffer("%-8.6f");
         try {
             if (out instanceof BufferedWriter) {
                 writer = (BufferedWriter) out;
             } else {
                 writer = new BufferedWriter(out);
             }
         } catch (Exception exc) {
         }
     }
 
     public XYZWriter(OutputStream output) {
         this(new OutputStreamWriter(output));
     }
 
     public XYZWriter() {
         this(new StringWriter());
     }
 
     @Override
     public IResourceFormat getFormat() {
         return XYZFormat.getInstance();
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
         for (int i = 0; i < interfaces.length; i++) {
             if (IAtomContainer.class.equals(interfaces[i])) return true;
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
                 throw new CDKException("Error while writing XYZ file: " + ex.getMessage(), ex);
             }
         } else {
             throw new CDKException("XYZWriter only supports output of Molecule classes.");
         }
     }
 
 
/** writes a single image in XYZ format to the recorder. */
 public void writeMolecule(IAtomContainer mol) throws IOException{
        int atomCount = mol.getAtomCount();
        int bondCount = mol.getBondCount();
        writer.write(Integer.toString(atomCount));
        writer.write("\n");
        writer.write(Integer.toString(bondCount));
        writer.write("\n");
        for (int i = 0; i < atomCount; i++) {
            IAtom atom = mol.getAtom(i);
            writer.write(atom.getSymbol());
            writer.write(" ");
            Point3d p = atom.getPoint3d();
            if (p != null) {
                writer.write(fsb.format(p.x));
                writer.write(" ");
                writer.write(fsb.format(p.y));
                writer.write(" ");
                writer.write(fsb.format(p.z));
            } else {
                writer.write("0.0 0.0 0.0");
            }
            writer.write("\n");
        }
        for (int i = 0; i < bondCount; i++) {
            writer.write(Integer.toString(mol.getBond(i).getAtomNumber(0) + 1));
            writer.write(" ");
            writer.write(Integer.toString(mol.getBond(i).getAtomNumber(1) + 1));
            writer.write(" ");
            writer.write(Integer.toString(mol.getBond(i).getOrder().numeric()));
            writer.write("\n");
        }
    }
    
        @Override
        public void write(IChemObject object, String filename) throws CDKException {
            // not implemented
        }
    
        @Override
        public void write(IChemObject object, OutputStream output) throws CDKException {
            // not implemented
        }
    
        @Override
        public void write(IChemObject object, Writer output) throws CDKException {
            // not implemented
        }
    
        @Override
        public void setWriter(OutputStream output, String encoding) throws CDKException     
 }

 

}