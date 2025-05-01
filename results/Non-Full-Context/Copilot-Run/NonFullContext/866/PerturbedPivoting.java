/* Copyright (C) 2002-2003  Bradley A. Smith <yeldar@home.com>
  *  Copyright (C) 2003-2007  Egon Willighagen <egonw@users.sf.net>
  *  Copyright (C) 2003-2007  Christoph Steinbeck <steinbeck@users.sf.net>
  *
  *  Contact: cdk-devel@lists.sourceforge.net
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
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.Reader;
 import java.io.StreamTokenizer;
 import java.io.StringReader;
 import java.util.List;
 import java.util.StringTokenizer;
 
 import javax.vecmath.Point3d;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomContainerSet;
 import org.openscience.cdk.interfaces.IChemFile;
 import org.openscience.cdk.interfaces.IChemModel;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.interfaces.IChemSequence;
 import org.openscience.cdk.io.formats.Gaussian98Format;
 import org.openscience.cdk.io.formats.IResourceFormat;
 import org.openscience.cdk.io.setting.BooleanIOSetting;
 import org.openscience.cdk.io.setting.IOSetting;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.ChemModelManipulator;
 import org.openscience.cdk.tools.periodictable.PeriodicTable;
 
 /**
  * A reader for Gaussian98 output. Gaussian 98 is a quantum chemistry program
  * by Gaussian, Inc. (<a href="http://www.gaussian.com/">http://www.gaussian.com/</a>).
  * 
  * <p>Molecular coordinates, energies, and normal coordinates of vibrations are
  * read. Each set of coordinates is added to the ChemFile in the order they are
  * found. Energies and vibrations are associated with the previously read set
  * of coordinates.
  * 
  * <p>This reader was developed from a small set of example output files, and
  * therefore, is not guaranteed to properly read all Gaussian98 output. If you
  * have problems, please contact the author of this code, not the developers of
  * Gaussian98.
  *
  * @author Bradley A. Smith &lt;yeldar@home.com&gt;
  * @author Egon Willighagen
  * @author Christoph Steinbeck
  * @cdk.module io
  * @cdk.githash
  * @cdk.iooptions
  */
 public class Gaussian98Reader extends DefaultChemObjectReader {
 
     private BufferedReader      input;
     private static ILoggingTool logger    = LoggingToolFactory.createLoggingTool(Gaussian98Reader.class); ;
     private int                 atomCount = 0;
     private String              lastRoute = "";
 
     /**
      * Customizable setting
      */
     private BooleanIOSetting    readOptimizedStructureOnly;
 
     /**
      * Constructor for the Gaussian98Reader object
      */
     public Gaussian98Reader() {
         this(new StringReader(""));
     }
 
     public Gaussian98Reader(InputStream input) {
         this(new InputStreamReader(input));
     }
 
     @Override
     public IResourceFormat getFormat() {
         return Gaussian98Format.getInstance();
     }
 
     /**
      * Sets the reader attribute of the Gaussian98Reader object.
      *
      * @param input The new reader value
      * @throws CDKException Description of the Exception
      */
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
 
     /**
      * Create an Gaussian98 output reader.
      *
      * @param input source of Gaussian98 data
      */
     public Gaussian98Reader(Reader input) {
         if (input instanceof BufferedReader) {
             this.input = (BufferedReader) input;
         } else {
             this.input = new BufferedReader(input);
         }
         initIOSettings();
     }
 
     @Override
     public boolean accepts(Class<? extends IChemObject> classObject) {
         if (IChemFile.class.equals(classObject)) return true;
         Class<?>[] interfaces = classObject.getInterfaces();
         for (int i = 0; i < interfaces.length; i++) {
             if (IChemFile.class.equals(interfaces[i])) return true;
         }
         Class superClass = classObject.getSuperclass();
         if (superClass != null) return this.accepts(superClass);
         return false;
     }
 
     @Override
     public <T extends IChemObject> T read(T object) throws CDKException {
         customizeJob();
 
         if (object instanceof IChemFile) {
             IChemFile file = (IChemFile) object;
             try {
                 file = readChemFile(file);
             } catch (IOException exception) {
                 throw new CDKException("Error while reading file: " + exception.toString(), exception);
             }
             return (T) file;
         } else {
             throw new CDKException("Reading of a " + object.getClass().getName() + " is not supported.");
         }
     }
 
     @Override
     public void close() throws IOException {
         input.close();
     }
 
     /**
      * Read the Gaussian98 output.
      *
      * @return a ChemFile with the coordinates, energies, and
      *         vibrations.
      * @throws IOException  if an I/O error occurs
      * @throws CDKException Description of the Exception
      */
     private IChemFile readChemFile(IChemFile chemFile) throws CDKException, IOException {
         IChemSequence sequence = chemFile.getBuilder().newInstance(IChemSequence.class);
         IChemModel model = null;
         String line = input.readLine();
         String levelOfTheory;
         String description;
         int modelCounter = 0;
 
         // Find first set of coordinates by skipping all before "Standard orientation"
         while (input.ready() && (line != null)) {
             if (line.indexOf("Standard orientation:") >= 0) {
 
                 // Found a set of coordinates
                 model = chemFile.getBuilder().newInstance(IChemModel.class);
                 readCoordinates(model);
                 break;
             }
             line = input.readLine();
         }
         if (model != null) {
 
             // Read all other data
             line = input.readLine().trim();
             while (input.ready() && (line != null)) {
                 if (line.indexOf('#') == 0) {
                     // Found the route section
                     // Memorizing this for the description of the chemmodel
                     lastRoute = line;
                     modelCounter = 0;
 
                 } else if (line.indexOf("Standard orientation:") >= 0) {
 
                     // Found a set of coordinates
                     // Add current frame to file and create a new one.
                     if (!readOptimizedStructureOnly.isSet()) {
                         sequence.addChemModel(model);
                     } else {
                         logger.info("Skipping frame, because I was told to do");
                     }
                     fireFrameRead();
                     model = chemFile.getBuilder().newInstance(IChemModel.class);
                     modelCounter++;
                     readCoordinates(model);
                 } else if (line.indexOf("SCF Done:") >= 0) {
 
                     // Found an energy
                     model.setProperty(CDKConstants.REMARK, line.trim());
                 } else if (line.indexOf("Harmonic frequencies") >= 0) {
 
                     // Found a set of vibrations
                     // readFrequencies(frame);
                 } else if (line.indexOf("Total atomic charges") >= 0) {
                     readPartialCharges(model);
                 } else if (line.indexOf("Magnetic shielding") >= 0) {
 
                     // Found NMR data
                     readNMRData(model, line);
 
                 } else if (line.indexOf("GINC") >= 0) {
 
                     // Found calculation level of theory
                     levelOfTheory = parseLevelOfTheory(line);
                     logger.debug("Level of Theory for this model: " + levelOfTheory);
                     description = lastRoute + ", model no. " + modelCounter;
                     model.setProperty(CDKConstants.DESCRIPTION, description);
                 } else {
                     //logger.debug("Skipping line: " + line);
                 }
                 line = input.readLine();
             }
 
             // Add last frame to file
             sequence.addChemModel(model);
             fireFrameRead();
         }
         chemFile.addChemSequence(sequence);
 
         return chemFile;
     }
 
     /**
      * Reads a set of coordinates into ChemFrame.
      *
      * @param model Description of the Parameter
      * @throws IOException  if an I/O error occurs
      * @throws CDKException Description of the Exception
      */
     private void readCoordinates(IChemModel model) throws CDKException, IOException {
         IAtomContainerSet moleculeSet = model.getBuilder().newInstance(IAtomContainerSet.class);
         IAtomContainer molecule = model.getBuilder().newInstance(IAtomContainer.class);
         String line = input.readLine();
         line = input.readLine();
         line = input.readLine();
         line = input.readLine();
         while (input.ready()) {
             line = input.readLine();
             if ((line == null) || (line.indexOf("-----") >= 0)) {
                 break;
             }
             int atomicNumber;
             StringReader sr = new StringReader(line);
             StreamTokenizer token = new StreamTokenizer(sr);
             token.nextToken();
 
             // ignore first token
             if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                 atomicNumber = (int) token.nval;
                 if (atomicNumber == 0) {
 
                     // Skip dummy atoms. Dummy atoms must be skipped
                     // if frequencies are to be read because Gaussian
                     // does not report dummy atoms in frequencies, and
                     // the number of atoms is used for reading frequencies.
                     continue;
                 }
             } else {
                 throw new CDKException("Error while reading coordinates: expected integer.");
             }
             token.nextToken();
 
             // ignore third token
             double x;
             double y;
             double z;
             if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                 x = token.nval;
             } else {
                 throw new IOException("Error reading x coordinate");
             }
             if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                 y = token.nval;
             } else {
                 throw new IOException("Error reading y coordinate");
             }
             if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                 z = token.nval;
             } else {
                 throw new IOException("Error reading z coordinate");
             }
             String symbol = "Du";
             symbol = PeriodicTable.getSymbol(atomicNumber);
             IAtom atom = model.getBuilder().newInstance(IAtom.class, symbol);
             atom.setPoint3d(new Point3d(x, y, z));
             molecule.addAtom(atom);
         }
         /*
          * this is the place where we store the atomcount to be used as a
          * counter in the nmr reading
          */
         atomCount = molecule.getAtomCount();
         moleculeSet.addAtomContainer(molecule);
         model.setMoleculeSet(moleculeSet);
     }
 
 
/** Reads the partial atomic charges and adds them to the given ChemModel. */
 private void readPartialCharges(IChemModel model) throws CDKException, IOException{}

 

}