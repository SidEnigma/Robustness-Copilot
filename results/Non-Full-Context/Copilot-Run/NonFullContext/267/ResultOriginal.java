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
 
     /**
      * Applies the MDL valence model to atoms using the explicit valence (bond
      * order sum) and charge to determine the correct number of implicit
      * hydrogens. The model is not applied if the explicit valence is less than
      * 0 - this is the case when a query bond was read for an atom.
      *
      * @param atom            the atom to apply the model to
      * @param unpaired        unpaired electron count
      * @param explicitValence the explicit valence (bond order sum)
      */
     private void applyMDLValenceModel(IAtom atom, int explicitValence, int unpaired) {
 
         if (atom.getValency() != null) {
             if (atom.getValency() >= explicitValence)
                 atom.setImplicitHydrogenCount(atom.getValency() - (explicitValence - unpaired));
             else
                 atom.setImplicitHydrogenCount(0);
         } else {
             Integer element = atom.getAtomicNumber();
             if (element == null) element = 0;
 
             Integer charge = atom.getFormalCharge();
             if (charge == null) charge = 0;
 
             int implicitValence = MDLValence.implicitValence(element, charge, explicitValence);
             if (implicitValence < explicitValence) {
                 atom.setValency(explicitValence);
                 atom.setImplicitHydrogenCount(0);
             } else {
                 atom.setValency(implicitValence);
                 atom.setImplicitHydrogenCount(implicitValence - explicitValence);
             }
         }
     }
 
     private void fixHydrogenIsotopes(IAtomContainer molecule, IsotopeFactory isotopeFactory) {
         for (IAtom atom : AtomContainerManipulator.getAtomArray(molecule)) {
             if (atom instanceof IPseudoAtom) {
                 IPseudoAtom pseudo = (IPseudoAtom) atom;
                 if ("D".equals(pseudo.getLabel())) {
                     IAtom newAtom = molecule.getBuilder().newInstance(IAtom.class, atom);
                     newAtom.setSymbol("H");
                     newAtom.setAtomicNumber(1);
                     isotopeFactory.configure(newAtom, isotopeFactory.getIsotope("H", 2));
                     AtomContainerManipulator.replaceAtomByAtom(molecule, atom, newAtom);
                 } else if ("T".equals(pseudo.getLabel())) {
                     IAtom newAtom = molecule.getBuilder().newInstance(IAtom.class, atom);
                     newAtom.setSymbol("H");
                     newAtom.setAtomicNumber(1);
                     isotopeFactory.configure(newAtom, isotopeFactory.getIsotope("H", 3));
                     AtomContainerManipulator.replaceAtomByAtom(molecule, atom, newAtom);
                 }
             }
         }
     }
 
     @Override
     public void close() throws IOException {
         input.close();
     }
 
     private void initIOSettings() {
         forceReadAs3DCoords = addSetting(new BooleanIOSetting("ForceReadAs3DCoordinates", IOSetting.Importance.LOW,
                 "Should coordinates always be read as 3D?", "false"));
         interpretHydrogenIsotopes = addSetting(new BooleanIOSetting("InterpretHydrogenIsotopes",
                 IOSetting.Importance.LOW, "Should D and T be interpreted as hydrogen isotopes?", "true"));
         addStereoElements = addSetting(new BooleanIOSetting("AddStereoElements", IOSetting.Importance.LOW,
                 "Detect and create IStereoElements for the input.", "true"));
     }
 
     public void customizeJob() {
         for (IOSetting setting : getSettings()) {
             fireIOSettingQuestion(setting);
         }
     }
 
     private String removeNonDigits(String input) {
         StringBuilder sb = new StringBuilder();
         for (int i = 0; i < input.length(); i++) {
             char inputChar = input.charAt(i);
             if (Character.isDigit(inputChar)) sb.append(inputChar);
         }
         return sb.toString();
     }
 
     IAtom readAtomFast(String line, IChemObjectBuilder builder, int lineNum) throws CDKException, IOException {
         return readAtomFast(line, builder, Collections.<IAtom,Integer>emptyMap(), lineNum, false);
     }
 
     /**
      * Parse an atom line from the atom block using the format: {@code
      * xxxxx.xxxxyyyyy.yyyyzzzzz.zzzz aaaddcccssshhhbbbvvvHHHrrriiimmmnnneee}
      * where: <ul> <li>x: x coordinate</li> <li>y: y coordinate</li> <li>z: z
      * coordinate</li> <li>a: atom symbol</li> <li>d: mass difference</li>
      * <li>c: charge</li> <li>s: stereo parity</li> <li>h: hydrogen count + 1
      * (not read - query)</li> <li>b: stereo care (not read - query)</li> <li>v:
      * valence</li> <li>H: H0 designator (not read - query)</li> <li>r: not
      * used</li> <li>i: not used</li> <li>m: atom reaction mapping</li> <li>n:
      * inversion/retention flag</li> <li>e: exact change flag</li> </ul>
      *
      * The parsing is strict and does not allow extra columns (i.e. NMR shifts)
      * malformed input.
      *
      * @param line    input line
      * @param builder chem object builder to create the atom
      * @param parities map of atom parities for creation 0D stereochemistry
      * @param lineNum the line number - for printing error messages
      * @return a new atom instance
      */
     IAtom readAtomFast(String line, IChemObjectBuilder builder, Map<IAtom,Integer> parities, int lineNum, boolean isQuery) throws CDKException, IOException {
 
         // The line may be truncated and it's checked in reverse at the specified
         // lengths:
         //          1         2         3         4         5         6
         // 123456789012345678901234567890123456789012345678901234567890123456789
         //                                  | |  |  |  |  |  |  |  |  |  |  |  |
         // xxxxx.xxxxyyyyy.yyyyzzzzz.zzzz aaaddcccssshhhbbbvvvHHHrrriiimmmnnneee
 
         String symbol;
         double x, y, z;
         int massDiff = 0, charge = 0, parity = 0, valence = 0, mapping = 0, hcount = 0;
 
         int length = length(line);
         if (length > 69) // excess data we should check all fields
             length = 69;
 
         // given the length we jump to the position and parse all fields
         // that could be present (note - fall through switch)
         switch (length) {
             case 69: // eee: exact charge flag [reaction, query]
             case 66: // nnn: inversion / retention [reaction]
             case 63: // mmm: atom-atom mapping [reaction]
                 mapping = readMolfileInt(line, 60);
             case 60: // iii: not used
             case 57: // rrr: not used
             case 54: // HHH: H0 designation [redundant]
             case 51: // vvv: valence
                 valence = readMolfileInt(line, 48);
             case 48: // bbb: stereo care [query]
             case 45: // hhh: hydrogen count + 1 [query]
                 hcount = readMolfileInt(line, 42);
             case 42: // sss: stereo parity
                 parity = toInt(line.charAt(41));
             // case 40: SAChem: I don't think this can happen in a valid molfile, maybe with a trailing tab?
             case 39: // ccc: charge
                 charge = toCharge(line.charAt(38));
             case 36: // dd: mass difference
                 massDiff = sign(line.charAt(34)) * toInt(line.charAt(35));
             case 34: // x y z and aaa: atom coordinates and symbol
             case 33: // symbol is left aligned
             case 32:
                 x = readMDLCoordinate(line, 0);
                 y = readMDLCoordinate(line, 10);
                 z = readMDLCoordinate(line, 20);
                 symbol = line.substring(31, 34).trim().intern();
                 break;
             default:
                 handleError("invalid line length", lineNum, 0, 0);
                 throw new CDKException("invalid line length, " + length + ": " + line);
         }
 
         IAtom atom = createAtom(symbol, builder, lineNum);
 
         if (isQuery) {
             Expr expr = new Expr(Expr.Type.ELEMENT, atom.getAtomicNumber());
             if (hcount != 0) {
                 if (hcount < 0)
                     hcount = 0;
                 expr.and(new Expr(Expr.Type.IMPL_H_COUNT, hcount));
             }
             atom = new QueryAtom(builder);
             ((QueryAtom)atom).setExpression(expr);
         }
 
         atom.setPoint3d(new Point3d(x, y, z));
         atom.setFormalCharge(charge);
         atom.setStereoParity(parity);
         if (parity != 0)
             parities.put(atom, parity);
 
         // if there was a mass difference, set the mass number
         if (massDiff != 0 && atom.getAtomicNumber() > 0) {
             IIsotope majorIsotope = Isotopes.getInstance().getMajorIsotope(atom.getAtomicNumber());
             if (majorIsotope == null)
                 atom.setMassNumber(-1); // checked after M ISO is processed
             else
                 atom.setMassNumber(majorIsotope.getMassNumber() + massDiff);
         }
 
         if (valence > 0 && valence < 16) atom.setValency(valence == 15 ? 0 : valence);
 
         if (mapping != 0) atom.setProperty(CDKConstants.ATOM_ATOM_MAPPING, mapping);
 
 
         return atom;
     }
 
     // for testing
     IBond readBondFast(String line, IChemObjectBuilder builder, IAtom[] atoms, int[] explicitValence, int lineNum) throws CDKException {
         return readBondFast(line, builder, atoms, explicitValence, lineNum, false);
     }
 
     /**
      * Read a bond from a line in the MDL bond block. The bond block is
      * formatted as follows, {@code 111222tttsssxxxrrrccc}, where:
      * <ul>
      *     <li>111: first atom number</li>
      *     <li>222: second atom number</li>
      *     <li>ttt: bond type</li>
      *     <li>xxx: bond stereo</li>
      *     <li>rrr: bond topology</li>
      *     <li>ccc: reaction center</li>
      * </ul>
      *
      * @param line            the input line
      * @param builder         builder to create objects with
      * @param atoms           atoms read from the atom block
      * @param explicitValence array to fill with explicit valence
      * @param lineNum         the input line number
      * @return a new bond
      * @throws CDKException thrown if the input was malformed or didn't make
      *                      sense
      */
     IBond readBondFast(String line, IChemObjectBuilder builder, IAtom[] atoms, int[] explicitValence, int lineNum,
                        boolean isQuery)
             throws CDKException {
 
         // The line may be truncated and it's checked in reverse at the specified
         // lengths. Absolutely required is atom indices, bond type and stereo.
         //          1         2
         // 123456789012345678901
         //            |  |  |  |
         // 111222tttsssxxxrrrccc
 
         int length = length(line);
         if (length > 21) length = 21;
 
         int u, v, type, stereo = 0;
 
         switch (length) {
             case 21: // ccc: reaction centre status
             case 18: // rrr: bond topology
             case 15: // xxx: not used
             case 12: // sss: stereo
                 stereo = readUInt(line, 9, 3);
             case 9: // 111222ttt: atoms, type and stereo
                 u = readMolfileInt(line, 0) - 1;
                 v = readMolfileInt(line, 3) - 1;
                 type = readMolfileInt(line, 6);
                 break;
             default:
                 throw new CDKException("invalid line length: " + length + " " + line);
         }
 
         IBond bond = builder.newBond();
         bond.setAtoms(new IAtom[]{atoms[u], atoms[v]});
 
         switch (type) {
             case 1: // single
                 bond.setOrder(IBond.Order.SINGLE);
                 bond.setStereo(toStereo(stereo, type));
                 break;
             case 2: // double
                 bond.setOrder(IBond.Order.DOUBLE);
                 bond.setStereo(toStereo(stereo, type));
                 break;
             case 3: // triple
                 bond.setOrder(IBond.Order.TRIPLE);
                 break;
             case 4: // aromatic
                 bond.setOrder(IBond.Order.UNSET);
                 bond.setFlag(CDKConstants.ISAROMATIC, true);
                 bond.setFlag(CDKConstants.SINGLE_OR_DOUBLE, true);
                 atoms[u].setFlag(CDKConstants.ISAROMATIC, true);
                 atoms[v].setFlag(CDKConstants.ISAROMATIC, true);
                 break;
             case 5: // single or double
                 bond = new QueryBond(bond.getBegin(), bond.getEnd(), Expr.Type.SINGLE_OR_DOUBLE);
                 break;
             case 6: // single or aromatic
                 bond = new QueryBond(bond.getBegin(), bond.getEnd(), Expr.Type.SINGLE_OR_AROMATIC);
                 break;
             case 7: // double or aromatic
                 bond = new QueryBond(bond.getBegin(), bond.getEnd(), Expr.Type.DOUBLE_OR_AROMATIC);
                 break;
             case 8: // any
                 bond = new QueryBond(bond.getBegin(), bond.getEnd(), Expr.Type.TRUE);
                 break;
             default:
                 throw new CDKException("unrecognised bond type: " + type + ", " + line);
         }
 
         if (type < 4) {
             explicitValence[u] += type;
             explicitValence[v] += type;
         } else {
             explicitValence[u] = explicitValence[v] = Integer.MIN_VALUE;
         }
 
         if (isQuery && bond.getClass() != QueryBond.class) {
             IBond.Order order = bond.getOrder();
             Expr expr = null;
             if (bond.isAromatic()) {
                 expr = new Expr(Expr.Type.IS_AROMATIC);
             } else {
                 expr = new Expr(Expr.Type.ORDER,
                                 bond.getOrder().numeric());
             }
             bond = new QueryBond(atoms[u], atoms[v], expr);
         }
 
         return bond;
     }
 
 
/** Reads the property block from the {@code input} setting the values in the  container. */
 void readPropertiesFast(final BufferedReader input, final IAtomContainer container, final int nAtoms) throws IOException, CDKException{
        String line;
        while ((line = input.readLine()) != null) {
            if (line.length() == 0) break;
            if (line.charAt(0) == 'M') {
                readMolfileProperties(line, container, nAtoms);
            } else if (line.charAt(0) == 'A') {
                readMolfileProperties(line, container, nAtoms);
            } else if (line.charAt(0) == 'B') {
                readMolfileProperties(line, container, nAtoms);
            } else if (line.charAt(0) == 'C') {
                readMolfileProperties(line, container, nAtoms);
            } else if (line.charAt(0) == 'D') {
                readMolfileProperties(line, container, nAtoms);
            } else if (line.charAt(0) == 'E') {
                readMolfileProperties(line, container, nAtoms);
            } else if (line.charAt(0) == 'F') {
                readMolfileProperties(line, container, nAtoms);
            } else if (line.charAt(0) == 'G') {
                readMolfileProperties(line, container, nAtoms);
            } else if (line.charAt(0) == 'H') {
                readMolfileProperties(line, container, nAtoms);
            } else if (line.charAt(0) == 'I') {
                readMolfileProperties(line, container, nAtoms);
            } else if (line.charAt(0) == 'J') {
                readMolfileProperties(line, container, nAtoms);
            } else if (line.charAt(0) == 'K') {
                readMolfileProperties(line, container, nAtoms);
            } else if (line.charAt(0) == 'L') {
                readMolfileProperties(line, container, nAtoms);
            } else if (line.charAt(0) ==        
 }

 

}