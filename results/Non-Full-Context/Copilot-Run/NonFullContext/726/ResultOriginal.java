/* Copyright (C) 1997-2007  The Chemistry Development Kit (CDK) project
  *                    2009  Egon Willighagen <egonw@users.sf.net>
  *                    2010  Mark Rijnbeek <mark_rynbeek@users.sf.net>
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
 
 import org.openscience.cdk.AtomRef;
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.config.Elements;
 import org.openscience.cdk.config.Isotopes;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemFile;
 import org.openscience.cdk.interfaces.IChemModel;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.interfaces.IChemSequence;
 import org.openscience.cdk.interfaces.IIsotope;
 import org.openscience.cdk.interfaces.IPseudoAtom;
 import org.openscience.cdk.interfaces.IStereoElement;
 import org.openscience.cdk.interfaces.ITetrahedralChirality;
 import org.openscience.cdk.io.formats.IResourceFormat;
 import org.openscience.cdk.io.formats.MDLFormat;
 import org.openscience.cdk.io.setting.BooleanIOSetting;
 import org.openscience.cdk.io.setting.IOSetting;
 import org.openscience.cdk.io.setting.StringIOSetting;
 import org.openscience.cdk.isomorphism.matchers.Expr;
 import org.openscience.cdk.isomorphism.matchers.IQueryAtom;
 import org.openscience.cdk.isomorphism.matchers.QueryAtom;
 import org.openscience.cdk.isomorphism.matchers.QueryBond;
 import org.openscience.cdk.sgroup.Sgroup;
 import org.openscience.cdk.sgroup.SgroupBracket;
 import org.openscience.cdk.sgroup.SgroupKey;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
 import org.openscience.cdk.tools.manipulator.ChemFileManipulator;
 
 import java.io.BufferedWriter;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.io.StringWriter;
 import java.io.Writer;
 import java.nio.charset.StandardCharsets;
 import java.text.NumberFormat;
 import java.text.SimpleDateFormat;
 import java.util.AbstractMap;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.EnumSet;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.Set;
 import java.util.TreeMap;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 import java.util.stream.Collectors;
 
 /**
  * Writes MDL molfiles, which contains a single molecule (see {@cdk.cite DAL92}).
  * For writing a MDL molfile you can this code:
  * <pre>
  * MDLV2000Writer writer = new MDLV2000Writer(
  *   new FileWriter(new File("output.mol"))
  * );
  * writer.write((IAtomContainer)molecule);
  * writer.close();
  * </pre>
  * 
  * <p>The writer has two IO settings: one for writing 2D coordinates, even if
  * 3D coordinates are given for the written data; the second writes aromatic
  * bonds as bond type 4, which is, strictly speaking, a query bond type, but
  * my many tools used to reflect aromaticity. The full IO setting API is
  * explained in CDK News {@cdk.cite WILLIGHAGEN2004}. One programmatic option
  * to set the option for writing 2D coordinates looks like:
  * <pre>
  * Properties customSettings = new Properties();
  * customSettings.setProperty(
  *  "ForceWriteAs2DCoordinates", "true"
  * );
  * PropertiesListener listener =
  *   new PropertiesListener(customSettings);
  * writer.addChemObjectIOListener(listener);
  * </pre>
  *
  * @cdk.module io
  * @cdk.githash
  * @cdk.iooptions
  * @cdk.keyword file format, MDL molfile
  */
 public class MDLV2000Writer extends DefaultChemObjectWriter {
 
     public static final String OptForceWriteAs2DCoordinates = "ForceWriteAs2DCoordinates";
     public static final String OptWriteMajorIsotopes        = "WriteMajorIsotopes";
     public static final String OptWriteAromaticBondTypes    = "WriteAromaticBondTypes";
     public static final String OptWriteQueryFormatValencies = "WriteQueryFormatValencies";
     public static final String OptWriteDefaultProperties    = "WriteDefaultProperties";
     public static final String OptProgramName               = "ProgramName";
 
     private final static ILoggingTool logger = LoggingToolFactory.createLoggingTool(MDLV2000Writer.class);
 
     // regular expression to capture R groups with attached numbers
     private Pattern NUMERED_R_GROUP = Pattern.compile("R(\\d+)");
 
     /**
      * Enumeration of all valid radical values.
      */
     public enum SPIN_MULTIPLICITY {
 
         None(0, 0),
         Monovalent(2, 1),
         DivalentSinglet(1, 2),
         DivalentTriplet(3, 2);
 
         // the radical SDF value
         private final int value;
         // the corresponding number of single electrons
         private final int singleElectrons;
 
         private SPIN_MULTIPLICITY(int value, int singleElectrons) {
             this.value = value;
             this.singleElectrons = singleElectrons;
         }
 
         /**
          * Radical value for the spin multiplicity in the properties block.
          *
          * @return the radical value
          */
         public int getValue() {
             return value;
         }
 
         /**
          * The number of single electrons that correspond to the spin multiplicity.
          *
          * @return the number of single electrons
          */
         public int getSingleElectrons() {
             return singleElectrons;
         }
 
         /**
          * Create a SPIN_MULTIPLICITY instance for the specified value.
          *
          * @param value input value (in the property block)
          * @return instance
          * @throws CDKException unknown spin multiplicity value
          */
         public static SPIN_MULTIPLICITY ofValue(int value) throws CDKException {
             switch (value) {
                 case 0:
                     return None;
                 case 1:
                     return DivalentSinglet;
                 case 2:
                     return Monovalent;
                 case 3:
                     return DivalentTriplet;
                 default:
                     throw new CDKException("unknown spin multiplicity: " + value);
             }
         }
     }
 
     // number of entries on line; value = 1 to 8
     private static final int NN8   = 8;
     // spacing between entries on line
     private static final int WIDTH = 3;
 
     private BooleanIOSetting forceWriteAs2DCoords;
 
     private BooleanIOSetting writeMajorIsotopes;
 
     // The next two options are MDL Query format options, not really
     // belonging to the MDLV2000 format, and will be removed when
     // a MDLV2000QueryWriter is written.
 
     /*
      * Should aromatic bonds be written as bond type 4? If true, this makes the
      * output a query file.
      */
     private BooleanIOSetting writeAromaticBondTypes;
 
     /* Should atomic valencies be written in the Query format. */
     @Deprecated
     private BooleanIOSetting writeQueryFormatValencies;
 
     private BooleanIOSetting writeDefaultProps;
 
     private StringIOSetting programNameOpt;
 
     private BufferedWriter writer;
 
     /**
      * Constructs a new MDLWriter that can write an {@link IAtomContainer}
      * to the MDL molfile format.
      *
      * @param out The Writer to write to
      */
     public MDLV2000Writer(Writer out) {
         if (out instanceof BufferedWriter) {
             writer = (BufferedWriter) out;
         } else {
             writer = new BufferedWriter(out);
         }
         initIOSettings();
     }
 
     /**
      * Constructs a new MDLWriter that can write an {@link IAtomContainer}
      * to a given OutputStream.
      *
      * @param output The OutputStream to write to
      */
     public MDLV2000Writer(OutputStream output) {
         this(new OutputStreamWriter(output, StandardCharsets.UTF_8));
     }
 
     public MDLV2000Writer() {
         this(new StringWriter());
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
      * Flushes the output and closes this object.
      */
     @Override
     public void close() throws IOException {
         writer.close();
     }
 
     @Override
     public boolean accepts(Class<? extends IChemObject> classObject) {
         Class<?>[] interfaces = classObject.getInterfaces();
         for (int i = 0; i < interfaces.length; i++) {
             if (IAtomContainer.class.equals(interfaces[i])) return true;
             if (IChemFile.class.equals(interfaces[i])) return true;
             if (IChemModel.class.equals(interfaces[i])) return true;
         }
         if (IAtomContainer.class.equals(classObject)) return true;
         if (IChemFile.class.equals(classObject)) return true;
         if (IChemModel.class.equals(classObject)) return true;
         Class superClass = classObject.getSuperclass();
         if (superClass != null) return this.accepts(superClass);
         return false;
     }
 
 
/** Writes a {@link IChemObject} to the MDL molfile formated output. */
 public void write(IChemObject object) throws CDKException{
        if (object instanceof IChemFile) {
            writeChemFile((IChemFile) object);
        } else if (object instanceof IChemModel) {
            writeChemModel((IChemModel) object);
        } else if (object instanceof IAtomContainer) {
            writeMolecule((IAtomContainer) object);
        } else {
            throw new CDKException("Only supported is writing of ChemFile, ChemModel and AtomContainer objects.");
        }
    }
    
        /**
        * Writes a {@link IChemFile} to the MDL molfile formated output.
        *
        * @param file The ChemFile to write
        * @throws CDKException if writing fails
        */
        public void writeChemFile(IChemFile file) throws CDKException {
            for (int i = 0; i < file.getChemSequenceCount(); i++) {
                IChemSequence sequence = file.getChemSequence(i);
                if (sequence instanceof IChemModel) {
                    writeChemModel((IChemModel) sequence);
                } else {
                    throw new CDKException("Only supported is writing of ChemModel objects.");
                }
            }
        }
    
        /**
        * Writes a {@link IChemModel} to the MDL molfile formated output.
        *
        * @param model The ChemModel to write
        * @throws CDKException if writing fails
        */
        public void writeChemModel(IChemModel model) throws CDKException {
            for (int i = 0; i < model.getChemSequenceCount(); i++) {
                IChemSequence sequence = model.getChemSequence(i);
                if (sequence instanceof IAtomContainer) {
                    writeMolecule((IAtomContainer) sequence);
                } else {
                    throw new CDKException("Only supported is writing of AtomContainer objects.");
                }
            }
        }
    
        /**
        * Writes a {@link IAtomContainer} to the MDL molfile formated output.
        *
        * @param molecule The AtomContainer     
 }

 

}