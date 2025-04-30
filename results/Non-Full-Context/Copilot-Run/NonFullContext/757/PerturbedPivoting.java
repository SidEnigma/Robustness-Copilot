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
 
     /**
      * Create a new chem model for a single {@link IAtomContainer}.
      *
      * @param container the container to create the model for
      * @return a new {@link IChemModel}
      */
     private static IChemModel newModel(final IAtomContainer container) {
 
         if (container == null) throw new NullPointerException("cannot create chem model for a null container");
 
         final IChemObjectBuilder builder = container.getBuilder();
         final IChemModel model = builder.newInstance(IChemModel.class);
         final IAtomContainerSet containers = builder.newInstance(IAtomContainerSet.class);
 
         containers.addAtomContainer(container);
         model.setMoleculeSet(containers);
 
         return model;
     }
 
     /**
      * Read an IAtomContainer from a file in MDL sd format
      *
      * @return The Molecule that was read from the MDL file.
      */
     private IAtomContainer readAtomContainer(IAtomContainer molecule) throws CDKException {
 
         boolean isQuery = molecule instanceof IQueryAtomContainer;
         IAtomContainer outputContainer = null;
         Map<IAtom,Integer> parities = new HashMap<>();
 
         int linecount = 0;
         String title = null;
         String program = null;
         String remark = null;
         String line = "";
 
         try {
 
             line = input.readLine();
             linecount++;
             if (line == null) {
                 return null;
             }
 
             if (line.startsWith("$$$$")) {
                 return molecule;
             }
             if (line.length() > 0) {
                 title = line;
             }
             line = input.readLine();
             linecount++;
             program = line;
             line = input.readLine();
             linecount++;
             if (line.length() > 0) {
                 remark = line;
             }
 
             line = input.readLine();
             linecount++;
 
             // if the line is empty we have a problem - either a malformed
             // molecule entry or just extra new lines at the end of the file
             if (line.length() == 0) {
                 handleError("Unexpected empty line", linecount, 0, 0);
                 // read till the next $$$$ or EOF
                 while (true) {
                     line = input.readLine();
                     linecount++;
                     if (line == null) {
                         return null;
                     }
                     if (line.startsWith("$$$$")) {
                         return molecule; // an empty molecule
                     }
                 }
             }
 
             final CTabVersion version = CTabVersion.ofHeader(line);
 
             // check the CT block version
             if (version == CTabVersion.V3000) {
                 handleError("This file must be read with the MDLV3000Reader.");
                 // even if relaxed we can't read V3000 using the V2000 parser
                 throw new CDKException("This file must be read with the MDLV3000Reader.");
             } else if (version == CTabVersion.UNSPECIFIED) {
                 handleError("This file must be read with the MDLReader.");
                 // okay to read in relaxed mode
             }
 
             int nAtoms = readMolfileInt(line, 0);
             int nBonds = readMolfileInt(line, 3);
             int chiral = readMolfileInt(line, 13);
 
             final IAtom[] atoms = new IAtom[nAtoms];
             final IBond[] bonds = new IBond[nBonds];
 
             // used for applying the MDL valence model
             int[] explicitValence = new int[nAtoms];
 
             boolean hasX = false, hasY = false, hasZ = false;
 
             for (int i = 0; i < nAtoms; i++) {
                 line = input.readLine();
                 linecount++;
 
                 final IAtom atom = readAtomFast(line, molecule.getBuilder(), parities, linecount, isQuery);
 
                 atoms[i] = atom;
 
                 Point3d p = atom.getPoint3d();
                 hasX = hasX || p.x != 0d;
                 hasY = hasY || p.y != 0d;
                 hasZ = hasZ || p.z != 0d;
             }
 
             // convert to 2D, if totalZ == 0
             if (!hasX && !hasY && !hasZ) {
                 if (nAtoms == 1) {
                     atoms[0].setPoint2d(new Point2d(0, 0));
                 } else {
                     for (IAtom atomToUpdate : atoms) {
                         atomToUpdate.setPoint3d(null);
                     }
                 }
             } else if (!hasZ) {
                 //'  CDK     09251712073D'
                 // 0123456789012345678901
                 if (is3Dfile(program)) {
                     hasZ = true;
                 } else if (!forceReadAs3DCoords.isSet()) {
                     for (IAtom atomToUpdate : atoms) {
                         Point3d p3d = atomToUpdate.getPoint3d();
                         if (p3d != null) {
                             atomToUpdate.setPoint2d(new Point2d(p3d.x, p3d.y));
                             atomToUpdate.setPoint3d(null);
                         }
                     }
                 }
             }
 
             for (int i = 0; i < nBonds; i++) {
                 line = input.readLine();
                 linecount++;
                 bonds[i] = readBondFast(line, molecule.getBuilder(), atoms, explicitValence, linecount, isQuery);
                 isQuery = isQuery ||
                                 bonds[i] instanceof IQueryBond ||
                                 (bonds[i].getOrder() == IBond.Order.UNSET && !bonds[i].isAromatic());
             }
 
             if (!isQuery)
                 outputContainer = molecule;
             else
                 outputContainer = new QueryAtomContainer(molecule.getBuilder());
 
             if (title != null)
                 outputContainer.setTitle(title);
             if (remark != null)
                 outputContainer.setProperty(CDKConstants.REMARK, remark);
 
             // if the container is empty we can simply set the atoms/bonds
             // otherwise we add them to the end
             if (outputContainer.isEmpty()) {
                 outputContainer.setAtoms(atoms);
                 outputContainer.setBonds(bonds);
             } else {
                 for (IAtom atom : atoms)
                     outputContainer.addAtom(atom);
                 for (IBond bond : bonds)
                     outputContainer.addBond(bond);
             }
 
             // create 0D stereochemistry
             if (addStereoElements.isSet()) {
                 Parities:
                 for (Map.Entry<IAtom, Integer> e : parities.entrySet()) {
                     int parity = e.getValue();
                     if (parity != 1 && parity != 2)
                         continue; // 3=unspec
                     int idx = 0;
                     IAtom focus = e.getKey();
                     IAtom[] carriers = new IAtom[4];
                     int hidx = -1;
                     for (IAtom nbr : outputContainer.getConnectedAtomsList(focus)) {
                         if (idx == 4)
                             continue Parities; // too many neighbors
                         if (nbr.getAtomicNumber() == 1) {
                             if (hidx >= 0)
                                 continue Parities;
                             hidx = idx;
                         }
                         carriers[idx++] = nbr;
                     }
                     // to few neighbors, or already have a hydrogen defined
                     if (idx < 3 || idx < 4 && hidx >= 0)
                         continue;
                     if (idx == 3)
                         carriers[idx++] = focus;
 
                     if (idx == 4) {
                         Stereo winding = parity == 1 ? Stereo.CLOCKWISE : Stereo.ANTI_CLOCKWISE;
                         // H is always at back, even if explicit! At least this seems to be the case.
                         // we adjust the winding as needed
                         if (hidx == 0 || hidx == 2)
                             winding = winding.invert();
                         outputContainer.addStereoElement(new TetrahedralChirality(focus, carriers, winding));
                     }
                 }
             }
 
             // read PROPERTY block
             readPropertiesFast(input, outputContainer, nAtoms);
 
             // read potential SD file data between M  END and $$$$
             readNonStructuralData(input, outputContainer);
 
             // note: apply the valence model last so that all fixes (i.e. hydrogen
             // isotopes) are in place we need to use a offset as this atoms
             // could be added to a molecule which already had atoms present
             int offset = outputContainer.getAtomCount() - nAtoms;
             for (int i = offset; i < outputContainer.getAtomCount(); i++) {
                 int valence = explicitValence[i - offset];
                 if (valence < 0) {
                     isQuery = true; // also counts aromatic bond as query
                 } else {
                     int unpaired = outputContainer.getConnectedSingleElectronsCount(outputContainer.getAtom(i));
                     applyMDLValenceModel(outputContainer.getAtom(i), valence + unpaired, unpaired);
                 }
             }
 
             // sanity check that we have a decent molecule, query bonds or query atoms mean we
             // don't have a hydrogen count for atoms and stereo perception isn't
             // currently possible
             if (!(outputContainer instanceof IQueryAtomContainer) && !isQuery &&
                 addStereoElements.isSet() && hasX && hasY) {
                 //ALS property could have changed an atom into a QueryAtom
                 for(IAtom atom : outputContainer.atoms()){
                     if (AtomRef.deref(atom) instanceof QueryAtom) {
                         isQuery=true;
                         break;
                     }
                 }
                 if(!isQuery) {
                     if (hasZ) { // has 3D coordinates
                         outputContainer.setStereoElements(StereoElementFactory.using3DCoordinates(outputContainer)
                                 .createAll());
                     } else if (!forceReadAs3DCoords.isSet()) { // has 2D coordinates (set as 2D coordinates)
                         outputContainer.setStereoElements(StereoElementFactory.using2DCoordinates(outputContainer)
                                 .createAll());
                     }
                 }
             }
 
             // chiral flag not set which means this molecule is this stereoisomer "and" the enantiomer, mark all
             // Tetrahedral stereo as AND1 (&1)
             if (chiral == 0) {
                 for (IStereoElement<?,?> se : outputContainer.stereoElements()) {
                     if (se.getConfigClass() == IStereoElement.TH) {
                         se.setGroupInfo(IStereoElement.GRP_RAC1);
                     }
                 }
             }
 
         } catch (CDKException exception) {
             String error = "Error while parsing line " + linecount + ": " + line + " -> " + exception.getMessage();
             logger.error(error);
             throw exception;
         } catch (IOException exception) {
             exception.printStackTrace();
             String error = "Error while parsing line " + linecount + ": " + line + " -> " + exception.getMessage();
             logger.error(error);
             handleError("Error while parsing line: " + line, linecount, 0, 0, exception);
         }
 
         return outputContainer;
     }
 
     private boolean is3Dfile(String program) {
         return program.length() >= 22 && program.substring(20, 22).equals("3D");
     }
 
 
/** Applies the MDL valence model to atoms using explicit valence (sum of bond order) and charge to determine the correct number of implicit hydrogens. */
 private void applyMDLValenceModel(IAtom atom, int explicitValence, int unpaired){}

 

}