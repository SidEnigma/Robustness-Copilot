/* Copyright (C) 1997-2007  Christoph Steinbeck <steinbeck@users.sourceforge.net>
  *                    2010  Egon Willighagen <egonw@users.sourceforge.net>
  *                    2014  Mark B Vine (orcid:0000-0002-7794-0426)
  *
  *  Contact: cdk-devel@lists.sourceforge.net
  *
  *  This program is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU Lesser General Public License
  *  as published by the Free Software Foundation; either version 2.1
  *  of the License, or (at your option) any later version.
  *  All we ask is that proper credit is given for our work, which includes
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
 package org.openscience.cdk.io;
 
 import org.openscience.cdk.AtomRef;
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.config.Elements;
 import org.openscience.cdk.config.IsotopeFactory;
 import org.openscience.cdk.config.Isotopes;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomContainerSet;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemFile;
 import org.openscience.cdk.interfaces.IChemModel;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.interfaces.IChemSequence;
 import org.openscience.cdk.interfaces.IIsotope;
 import org.openscience.cdk.interfaces.IPseudoAtom;
 import org.openscience.cdk.interfaces.ISingleElectron;
 import org.openscience.cdk.interfaces.IStereoElement;
 import org.openscience.cdk.interfaces.ITetrahedralChirality.Stereo;
 import org.openscience.cdk.io.formats.IResourceFormat;
 import org.openscience.cdk.io.formats.MDLV2000Format;
 import org.openscience.cdk.io.setting.BooleanIOSetting;
 import org.openscience.cdk.io.setting.IOSetting;
 import org.openscience.cdk.isomorphism.matchers.Expr;
 import org.openscience.cdk.isomorphism.matchers.IQueryAtomContainer;
 import org.openscience.cdk.isomorphism.matchers.IQueryBond;
 import org.openscience.cdk.isomorphism.matchers.QueryAtom;
 import org.openscience.cdk.isomorphism.matchers.QueryAtomContainer;
 import org.openscience.cdk.isomorphism.matchers.QueryBond;
 import org.openscience.cdk.sgroup.Sgroup;
 import org.openscience.cdk.sgroup.SgroupBracket;
 import org.openscience.cdk.sgroup.SgroupKey;
 import org.openscience.cdk.sgroup.SgroupType;
 import org.openscience.cdk.stereo.StereoElementFactory;
 import org.openscience.cdk.stereo.TetrahedralChirality;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
 
 import javax.vecmath.Point2d;
 import javax.vecmath.Point3d;
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.Reader;
 import java.io.StringReader;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.StringTokenizer;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import static org.openscience.cdk.io.MDLV2000Writer.SPIN_MULTIPLICITY;
 
 /**
  * Reads content from MDL molfiles and SD files. It can read a {@link
  * IAtomContainer} or {@link IChemModel} from an MDL molfile, and a {@link
  * IChemFile} from a SD file, with a {@link IChemSequence} of {@link
  * IChemModel}'s, where each IChemModel will contain one {@link IAtomContainer}.
  *
  * <p>From the Atom block it reads atomic coordinates, element types and formal
  * charges. From the Bond block it reads the bonds and the orders. Additionally,
  * it reads 'M  CHG', 'G  ', 'M  RAD' and 'M  ISO' lines from the property
  * block.
  *
  * <p>If all z coordinates are 0.0, then the xy coordinates are taken as 2D,
  * otherwise the coordinates are read as 3D.
  *
  * <p>The title of the MOL file is read and can be retrieved with:
  * <pre>
  *   molecule.getProperty(CDKConstants.TITLE);
  * </pre>
  *
  * <p>RGroups which are saved in the MDL molfile as R#, are renamed according to
  * their appearance, e.g. the first R# is named R1. With PseudAtom.getLabel()
  * "R1" is returned (instead of R#). This is introduced due to the SAR table
  * generation procedure of Scitegics PipelinePilot.
  *
  * @author steinbeck
  * @author Egon Willighagen
  * @cdk.module io
  * @cdk.githash
  * @cdk.iooptions
  * @cdk.created 2000-10-02
  * @cdk.keyword file format, MDL molfile
  * @cdk.keyword file format, SDF
  * @cdk.bug 1587283
  */
 public class MDLV2000Reader extends DefaultChemObjectReader {
 
     BufferedReader                   input            = null;
     private static ILoggingTool      logger           = LoggingToolFactory.createLoggingTool(MDLV2000Reader.class);
 
     private BooleanIOSetting         forceReadAs3DCoords;
     private BooleanIOSetting         interpretHydrogenIsotopes;
     private BooleanIOSetting         addStereoElements;
 
     // Pattern to remove trailing space (String.trim() will remove leading space, which we don't want)
     private static final Pattern     TRAILING_SPACE   = Pattern.compile("\\s+$");
 
     /** Delimits Structure-Data (SD) Files. */
     private static final String      RECORD_DELIMITER = "$$$$";
 
     /** Valid pseudo labels. */
     private static final Set<String> PSEUDO_LABELS    = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("*","A","Q","L","LP","R","R#")));
     
     public MDLV2000Reader() {
         this(new StringReader(""));
     }
 
     /**
      * Constructs a new MDLReader that can read Molecule from a given
      * InputStream.
      *
      * @param in The InputStream to read from
      */
     public MDLV2000Reader(InputStream in) {
         this(new InputStreamReader(in));
     }
 
     public MDLV2000Reader(InputStream in, Mode mode) {
         this(new InputStreamReader(in), mode);
     }
 
     /**
      * Constructs a new MDLReader that can read Molecule from a given Reader.
      *
      * @param in The Reader to read from
      */
     public MDLV2000Reader(Reader in) {
         this(in, Mode.RELAXED);
     }
 
     public MDLV2000Reader(Reader in, Mode mode) {
         input = new BufferedReader(in);
         initIOSettings();
         super.mode = mode;
     }
 
     @Override
     public IResourceFormat getFormat() {
         return MDLV2000Format.getInstance();
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
 
     @SuppressWarnings("unchecked")
     @Override
     public boolean accepts(Class<? extends IChemObject> classObject) {
         Class<?>[] interfaces = classObject.getInterfaces();
         for (Class<?> anInterface : interfaces) {
             if (IChemFile.class.equals(anInterface)) return true;
             if (IChemModel.class.equals(anInterface)) return true;
             if (IAtomContainer.class.equals(anInterface)) return true;
         }
         if (IAtomContainer.class.equals(classObject)) return true;
         if (IChemFile.class.equals(classObject)) return true;
         if (IChemModel.class.equals(classObject)) return true;
         Class superClass = classObject.getSuperclass();
         return superClass != null && this.accepts(superClass);
     }
 
     /**
      * Takes an object which subclasses IChemObject, e.g. Molecule, and will
      * read this (from file, database, internet etc). If the specific
      * implementation does not support a specific IChemObject it will throw an
      * Exception.
      *
      * @param object The object that subclasses IChemObject
      * @return The IChemObject read
      * @throws CDKException
      */
     @SuppressWarnings("unchecked")
     @Override
     public <T extends IChemObject> T read(T object) throws CDKException {
         if (object instanceof IAtomContainer) {
             return (T) readAtomContainer((IAtomContainer) object);
         } else if (object instanceof IChemFile) {
             return (T) readChemFile((IChemFile) object);
         } else if (object instanceof IChemModel) {
             return (T) readChemModel((IChemModel) object);
         } else {
             throw new CDKException("Only supported are ChemFile and Molecule.");
         }
     }
 
     private IChemModel readChemModel(IChemModel chemModel) throws CDKException {
         IAtomContainerSet setOfMolecules = chemModel.getMoleculeSet();
         if (setOfMolecules == null) {
             setOfMolecules = chemModel.getBuilder().newInstance(IAtomContainerSet.class);
         }
         IAtomContainer m = readAtomContainer(chemModel.getBuilder().newInstance(IAtomContainer.class));
         if (m != null) {
             setOfMolecules.addAtomContainer(m);
         }
         chemModel.setMoleculeSet(setOfMolecules);
         return chemModel;
     }
 
     /**
      * Read a ChemFile from a file in MDL SDF format.
      *
      * @return The ChemFile that was read from the MDL file.
      */
     private IChemFile readChemFile(IChemFile chemFile) throws CDKException {
 
         IChemObjectBuilder builder = chemFile.getBuilder();
         IChemSequence sequence = builder.newInstance(IChemSequence.class);
 
         try {
             IAtomContainer m;
             while ((m = readAtomContainer(builder.newInstance(IAtomContainer.class))) != null) {
                 sequence.addChemModel(newModel(m));
             }
         } catch (CDKException e) {
             throw e;
         } catch (IllegalArgumentException exception) {
             String error = "Error while parsing SDF";
             logger.error(error);
             logger.debug(exception);
             throw new CDKException(error, exception);
         }
         try {
             input.close();
         } catch (Exception exc) {
             String error = "Error while closing file: " + exc.getMessage();
             logger.error(error);
             throw new CDKException(error, exc);
         }
 
         chemFile.addChemSequence(sequence);
         return chemFile;
     }
 
 
/** Create a new chem model for a single {@link IAtomContainer}. */
 private static IChemModel newModel(final IAtomContainer container){
        IChemModel model = new ChemModel();
        model.setMoleculeSet(new AtomContainerSet());
        model.getMoleculeSet().addAtomContainer(container);
        return model;
    }
    
        /**
        * Read an AtomContainer from a file in MDL SDF format.
        *
        * @return The AtomContainer that was read from the MDL file.
        */
        private IAtomContainer readAtomContainer(IAtomContainer atomContainer) throws CDKException {
    
            String line;
            StringBuilder sb = new StringBuilder();
            try {
                while ((line = input.readLine()) != null) {
                    if (line.startsWith("$$$$")) {
                        break;
                    }
                    sb.append(line);
                }
            } catch (IOException exception) {
                String error = "Error while reading SDF";
                logger.error(error);
                logger.debug(exception);
                throw new CDKException(error, exception);
            }
    
            if (line == null) {
                return null;
            }
    
            String sdf = sb.toString();
            if (sdf.length() == 0) {
                return null;
            }
    
            // remove trailing space
            sdf = TRAILING_SPACE.matcher(sdf).replaceAll("");
    
            // split into records
            String[] records = sdf.split(RECORD_DELIMITER);
    
            // parse records
            for (String record : records) {
                if (record.length() == 0) {
                    continue;
                }
                parseRecord(atomContainer, record);
            }
    
            return atomContainer;
        }
    
        /**
        * Parse a single record from an MDL SDF file.
        *
        * @param atomContainer The AtomContainer to add the data to
        * @param record The record to parse
        * @throws CDKException
        */
        private void parseRecord(IAtomContainer atomContainer, String record) throws CDKException {     
 }

 

}