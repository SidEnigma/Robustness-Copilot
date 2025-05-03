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
  *
  */
 package org.openscience.cdk.io;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomContainerSet;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.interfaces.IMapping;
 import org.openscience.cdk.interfaces.IReaction;
 import org.openscience.cdk.interfaces.IReactionSet;
 import org.openscience.cdk.io.formats.IResourceFormat;
 import org.openscience.cdk.io.formats.MDLFormat;
 import org.openscience.cdk.io.setting.BooleanIOSetting;
 import org.openscience.cdk.io.setting.IOSetting;
 import org.openscience.cdk.io.setting.StringIOSetting;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 
 import java.io.BufferedWriter;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.io.StringWriter;
 import java.io.Writer;
 import java.text.NumberFormat;
 import java.util.Iterator;
 import java.util.Locale;
 import java.util.Map;
 import java.util.Set;
 
 /**
  * Writes a reaction to a MDL rxn or SDF file. Attention: Stoichiometric
  * coefficients have to be natural numbers.
  *
  * <pre>
  * MDLRXNWriter writer = new MDLRXNWriter(new FileWriter(new File("output.mol")));
  * writer.write((Molecule)molecule);
  * writer.close();
  * </pre>
  *
  * See {@cdk.cite DAL92}.
  *
  * @cdk.module io
  * @cdk.githash
  * @cdk.iooptions
  *
  * @cdk.keyword file format, MDL RXN file
  */
 public class MDLRXNWriter extends DefaultChemObjectWriter {
 
     public static final String OptWriteAgents = "WriteAgents";
 
     private BooleanIOSetting writeAgents;
 
     private BufferedWriter      writer;
     private static ILoggingTool logger   = LoggingToolFactory.createLoggingTool(MDLRXNWriter.class);
     private int                 reactionNumber;
     public Map<String, Object>  rdFields = null;
 
     /**
      * Constructs a new MDLWriter that can write an array of
      * Molecules to a Writer.
      *
      * @param   out  The Writer to write to
      */
     public MDLRXNWriter(Writer out) {
         try {
             if (out instanceof BufferedWriter) {
                 writer = (BufferedWriter) out;
             } else {
                 writer = new BufferedWriter(out);
             }
         } catch (Exception exc) {
         }
         this.reactionNumber = 1;
         initIOSettings();
     }
 
     /**
      * Constructs a new MDLWriter that can write an array of
      * Molecules to a given OutputStream.
      *
      * @param   output  The OutputStream to write to
      */
     public MDLRXNWriter(OutputStream output) {
         this(new OutputStreamWriter(output));
     }
 
     public MDLRXNWriter() {
         this(new StringWriter());
     }
 
     private void initIOSettings() {
         writeAgents = addSetting(new BooleanIOSetting(OptWriteAgents,
                                                       IOSetting.Importance.LOW,
                                                       "Output agents in the RXN file",
                                                       "true"));
     }
 
     @Override
     public IResourceFormat getFormat() {
         return MDLFormat.getInstance();
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
      * Here you can set a map which will be used to build rd fields in the file.
      * The entries will be translated to rd fields like this:<br>
      * &gt; &lt;key&gt;<br>
      * &gt; value<br>
      * empty line<br>
      *
      * @param  map The map to be used, map of String-String pairs
      */
     public void setRdFields(Map<String, Object> map) {
         rdFields = map;
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
         if (IReaction.class.equals(classObject)) return true;
         if (IReactionSet.class.equals(classObject)) return true;
         Class<?>[] interfaces = classObject.getInterfaces();
         for (Class<?> anInterface : interfaces) {
             if (IReaction.class.equals(anInterface)) return true;
             if (IReactionSet.class.equals(anInterface)) return true;
         }
         Class superClass = classObject.getSuperclass();
         if (superClass != null) return this.accepts(superClass);
         return false;
     }
 
 
/** Writes an IChemObject in the formatted output of the RXN MDL file. */
 public void write(IChemObject object) throws CDKException{}

 

}