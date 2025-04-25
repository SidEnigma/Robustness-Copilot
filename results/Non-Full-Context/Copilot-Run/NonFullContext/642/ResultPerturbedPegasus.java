/* Copyright (C) 2000-2003  The Jmol Development Team
  * Copyright (C) 2003-2007  The CDK Project
  *
  * Contact: cdk-devel@lists.sf.net
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
 import java.util.List;
 
 import javax.vecmath.Point3d;
 import javax.vecmath.Vector3d;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.geometry.CrystalGeometryTools;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IChemFile;
 import org.openscience.cdk.interfaces.IChemModel;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.interfaces.IChemSequence;
 import org.openscience.cdk.interfaces.ICrystal;
 import org.openscience.cdk.io.formats.IResourceFormat;
 import org.openscience.cdk.io.formats.PDBFormat;
 import org.openscience.cdk.io.setting.BooleanIOSetting;
 import org.openscience.cdk.io.setting.IOSetting;
 import org.openscience.cdk.tools.FormatStringBuffer;
 import org.openscience.cdk.tools.manipulator.ChemModelManipulator;
 
 /**
  * Saves small molecules in a rudimentary PDB format. It does not allow
  * writing of PDBProtein data structures.
  *
  * @author Gilleain Torrance &lt;gilleain.torrance@gmail.com&gt;
  * @cdk.module pdb
  * @cdk.iooptions
  * @cdk.githash
  */
 public class PDBWriter extends DefaultChemObjectWriter {
 
     public final String      SERIAL_FORMAT    = "%5d";
     public final String      ATOM_NAME_FORMAT = "%-5s";
     public final String      POSITION_FORMAT  = "%8.3f";
     public final String      RESIDUE_FORMAT   = "%s";
 
     private BooleanIOSetting writeAsHET;
     private BooleanIOSetting useElementSymbolAsAtomName;
     private BooleanIOSetting writeCONECTRecords;
     private BooleanIOSetting writeTERRecord;
     private BooleanIOSetting writeENDRecord;
 
     private BufferedWriter   writer;
 
     public PDBWriter() {
         this(new StringWriter());
     }
 
     /**
      * Creates a PDB writer.
      *
      * @param out the stream to write the PDB file to.
      */
     public PDBWriter(Writer out) {
         try {
             if (out instanceof BufferedWriter) {
                 writer = (BufferedWriter) out;
             } else {
                 writer = new BufferedWriter(out);
             }
         } catch (Exception exc) {
         }
         writeAsHET = addSetting(new BooleanIOSetting("WriteAsHET", IOSetting.Importance.LOW,
                 "Should the output file use HETATM", "false"));
         useElementSymbolAsAtomName = addSetting(new BooleanIOSetting("UseElementSymbolAsAtomName",
                 IOSetting.Importance.LOW, "Should the element symbol be written as the atom name", "false"));
         writeCONECTRecords = addSetting(new BooleanIOSetting("WriteCONECT", IOSetting.Importance.LOW,
                 "Should the bonds be written as CONECT records?", "true"));
         writeTERRecord = addSetting(new BooleanIOSetting("WriteTER", IOSetting.Importance.LOW,
                 "Should a TER record be put at the end of the atoms?", "false"));
         writeENDRecord = addSetting(new BooleanIOSetting("WriteEND", IOSetting.Importance.LOW,
                 "Should an END record be put at the end of the file?", "true"));
     }
 
     public PDBWriter(OutputStream output) {
         this(new OutputStreamWriter(output));
     }
 
     @Override
     public IResourceFormat getFormat() {
         return PDBFormat.getInstance();
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
 
     @Override
     public boolean accepts(Class<? extends IChemObject> classObject) {
         if (IChemFile.class.equals(classObject)) return true;
         if (ICrystal.class.equals(classObject)) return true;
         if (IAtomContainer.class.equals(classObject)) return true;
         Class<?>[] interfaces = classObject.getInterfaces();
         for (int i = 0; i < interfaces.length; i++) {
             if (ICrystal.class.equals(interfaces[i])) return true;
             if (IAtomContainer.class.equals(interfaces[i])) return true;
             if (IChemFile.class.equals(interfaces[i])) return true;
         }
         Class superClass = classObject.getSuperclass();
         if (superClass != null) return this.accepts(superClass);
         return false;
     }
 
     @Override
     public void write(IChemObject object) throws CDKException {
         if (object instanceof ICrystal) {
             writeCrystal((ICrystal) object);
         } else if (object instanceof IAtomContainer) {
             writeMolecule((IAtomContainer) object);
         } else if (object instanceof IChemFile) {
             IChemFile chemFile = (IChemFile) object;
             IChemSequence sequence = chemFile.getChemSequence(0);
             if (sequence != null) {
                 IChemModel model = sequence.getChemModel(0);
                 if (model != null) {
                     ICrystal crystal = model.getCrystal();
                     if (crystal != null) {
                         write(crystal);
                     } else {
                         Iterator<IAtomContainer> containers = ChemModelManipulator.getAllAtomContainers(model)
                                 .iterator();
                         while (containers.hasNext()) {
                             writeMolecule(model.getBuilder().newInstance(IAtomContainer.class, containers.next()));
                         }
                     }
                 }
             }
         } else {
             throw new CDKException("Only supported is writing of Molecule, Crystal and ChemFile objects.");
         }
     }
 
 
/** A single frame is written in PDB format. */
 public void writeMolecule(IAtomContainer molecule) throws CDKException{
        try {
            writeHeader(molecule);
            writeAtoms(molecule);
            writeBonds(molecule);
            writeFooter(molecule);
        } catch (IOException e) {
            throw new CDKException("IOException while writing PDB file: " + e.getMessage(), e);
        }       
 }

 

}