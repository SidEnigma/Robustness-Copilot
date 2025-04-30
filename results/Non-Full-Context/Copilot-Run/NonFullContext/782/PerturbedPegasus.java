/* Copyright (C) 2004-2008  Egon Willighagen <egonw@users.sf.net>
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
 
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.graph.rebond.RebondTool;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemFile;
 import org.openscience.cdk.interfaces.IChemModel;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.interfaces.IChemSequence;
 import org.openscience.cdk.interfaces.ICrystal;
 import org.openscience.cdk.io.formats.IResourceFormat;
 import org.openscience.cdk.io.formats.PMPFormat;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 
 import javax.vecmath.Point3d;
 import javax.vecmath.Vector3d;
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.Reader;
 import java.io.StringReader;
 import java.util.Hashtable;
 import java.util.Map;
 import java.util.StringTokenizer;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 /**
  * Reads an frames from a PMP formated input.
  * Both compilation and use of this class requires Java 1.4.
  *
  * @cdk.module  io
  * @cdk.githash
  * @cdk.iooptions
  *
  * @cdk.keyword file format, Polymorph Predictor (tm)
  *
  * @author E.L. Willighagen
  * @cdk.require java1.4+
  */
 public class PMPReader extends DefaultChemObjectReader {
 
     private static final String   PMP_ZORDER   = "ZOrder";
     private static final String   PMP_ID       = "Id";
 
     private BufferedReader        input;
 
     private static ILoggingTool   logger       = LoggingToolFactory.createLoggingTool(PMPReader.class);
 
     /* Keep a copy of the PMP model */
     private IAtomContainer        modelStructure;
     private IChemObject           chemObject;
     /* Keep an index of PMP id -> AtomCountainer id */
     private Map<Integer, Integer> atomids      = new Hashtable<>();
     private Map<Integer, Integer> atomGivenIds = new Hashtable<>();
     private Map<Integer, Integer> bondids      = new Hashtable<>();
     private Map<Integer, Integer> bondAtomOnes = new Hashtable<>();
     private Map<Integer, Integer> bondAtomTwos = new Hashtable<>();
     private Map<Integer, Double>  bondOrders   = new Hashtable<>();
 
     /* Often used patterns */
     Pattern                       objHeader;
     Pattern                       objCommand;
     Pattern                       atomTypePattern;
 
     int                           lineNumber;
     int                           bondCounter  = 0;
     private RebondTool            rebonder;
 
     /*
      * construct a new reader from a Reader type object
      * @param input reader from which input is read
      */
     public PMPReader(Reader input) {
         this.input = new BufferedReader(input);
         this.lineNumber = 0;
 
         /* compile patterns */
         objHeader = Pattern.compile(".*\\((\\d+)\\s(\\w+)$");
         objCommand = Pattern.compile(".*\\(A\\s([CFDIO])\\s(\\w+)\\s+\"?(.*?)\"?\\)$");
         atomTypePattern = Pattern.compile("^(\\d+)\\s+(\\w+)$");
 
         rebonder = new RebondTool(2.0, 0.5, 0.5);
     }
 
     public PMPReader(InputStream input) {
         this(new InputStreamReader(input));
     }
 
     public PMPReader() {
         this(new StringReader(""));
     }
 
     @Override
     public IResourceFormat getFormat() {
         return PMPFormat.getInstance();
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
     @SuppressWarnings("unchecked")
     public boolean accepts(Class<? extends IChemObject> classObject) {
         if (IChemFile.class.equals(classObject)) return true;
         Class<?>[] interfaces = classObject.getInterfaces();
         for (Class<?> anInterface : interfaces) {
             if (IChemFile.class.equals(anInterface)) return true;
         }
         Class superClass = classObject.getSuperclass();
         if (superClass != null) return this.accepts(superClass);
         return false;
     }
 
     /**
      * reads the content from a PMP input. It can only return a
      * IChemObject of type ChemFile
      *
      * @param object class must be of type ChemFile
      *
      * @see IChemFile
      */
     @Override
     @SuppressWarnings("unchecked")
     public <T extends IChemObject> T read(T object) throws CDKException {
         if (object instanceof IChemFile) {
             return (T) readChemFile((IChemFile) object);
         } else {
             throw new CDKException("Only supported is reading of ChemFile objects.");
         }
     }
 
     // private procedures
 
     private String readLine() throws IOException {
         String line = input.readLine();
         lineNumber = lineNumber + 1;
         logger.debug("LINE (" + lineNumber + "): ", line);
         return line;
     }
 
 
/** Private method that reads the input to the ChemFile object. */
 private IChemFile readChemFile(IChemFile chemFile){}

 

}