/* Copyright (C) 1997-2007  The Chemistry Development Kit (CDK) project
  *                    2014  Mark B Vine (orcid:0000-0002-7794-0426)
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
  *  */
 package org.openscience.cdk.io;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.Reader;
 import java.io.StringReader;
 import java.nio.charset.StandardCharsets;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Hashtable;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import javax.vecmath.Point3d;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.DefaultChemObjectBuilder;
 import org.openscience.cdk.config.AtomTypeFactory;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.graph.rebond.RebondTool;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomContainerSet;
 import org.openscience.cdk.interfaces.IAtomType;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemFile;
 import org.openscience.cdk.interfaces.IChemModel;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.interfaces.IChemSequence;
 import org.openscience.cdk.interfaces.IMonomer;
 import org.openscience.cdk.interfaces.IStrand;
 import org.openscience.cdk.io.formats.IResourceFormat;
 import org.openscience.cdk.io.formats.PDBFormat;
 import org.openscience.cdk.io.setting.BooleanIOSetting;
 import org.openscience.cdk.io.setting.IOSetting;
 import org.openscience.cdk.protein.data.PDBAtom;
 import org.openscience.cdk.protein.data.PDBMonomer;
 import org.openscience.cdk.protein.data.PDBPolymer;
 import org.openscience.cdk.protein.data.PDBStrand;
 import org.openscience.cdk.protein.data.PDBStructure;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.AtomTypeManipulator;
 
 /**
  * Reads the contents of a PDBFile.
  *
  * <p>A description can be found at <a href="http://www.rcsb.org/pdb/static.do?p=file_formats/pdb/index.html">
  * http://www.rcsb.org/pdb/static.do?p=file_formats/pdb/index.html</a>.
  *
  * @cdk.module  pdb
  * @cdk.githash
  * @cdk.iooptions
  *
  * @author      Edgar Luttmann
  * @author Bradley Smith &lt;bradley@baysmith.com&gt;
  * @author Martin Eklund &lt;martin.eklund@farmbio.uu.se&gt;
  * @author Ola Spjuth &lt;ola.spjuth@farmbio.uu.se&gt;
  * @author Gilleain Torrance &lt;gilleain.torrance@gmail.com&gt;
  * @cdk.created 2001-08-06
  * @cdk.keyword file format, PDB
  * @cdk.bug     1714141
  * @cdk.bug     1794439
  */
 public class PDBReader extends DefaultChemObjectReader {
 
     private static ILoggingTool    logger            = LoggingToolFactory.createLoggingTool(PDBReader.class);
     private BufferedReader         _oInput;                                                                  // The internal used BufferedReader
     private BooleanIOSetting       useRebondTool;
     private BooleanIOSetting       readConnect;
     private BooleanIOSetting       useHetDictionary;
 
     private Map<Integer, IAtom>    atomNumberMap;
 
     /*
      * This is a temporary store for bonds from CONNECT records. As CONNECT is
      * deliberately fully redundant (a->b and b->a) we need to use this to weed
      * out the duplicates.
      */
     private List<IBond>            bondsFromConnectRecords;
 
     /**
      * A mapping between HETATM 3-letter codes + atomNames to CDK atom type
      * names; for example "RFB.N13" maps to "N.planar3".
      */
     private Map<String, String>    hetDictionary;
     private Set<String>            hetResidues;
 
     private AtomTypeFactory        cdkAtomTypeFactory;
 
     private static final String    hetDictionaryPath = "type_map.txt";
     private static final String    resDictionaryPath = "type_res.txt";
 
     /**
      *
      * Constructs a new PDBReader that can read Molecules from a given
      * InputStream.
      *
      * @param oIn  The InputStream to read from
      *
      */
     public PDBReader(InputStream oIn) {
         this(new InputStreamReader(oIn));
     }
 
     /**
      *
      * Constructs a new PDBReader that can read Molecules from a given
      * Reader.
      *
      * @param oIn  The Reader to read from
      *
      */
     public PDBReader(Reader oIn) {
         _oInput = new BufferedReader(oIn);
         initIOSettings();
         hetDictionary = null;
         cdkAtomTypeFactory = null;
     }
 
     public PDBReader() {
         this(new StringReader(""));
     }
 
     @Override
     public IResourceFormat getFormat() {
         return PDBFormat.getInstance();
     }
 
     @Override
     public void setReader(Reader input) throws CDKException {
         if (input instanceof BufferedReader) {
             this._oInput = (BufferedReader) input;
         } else {
             this._oInput = new BufferedReader(input);
         }
     }
 
     @Override
     public void setReader(InputStream input) throws CDKException {
         setReader(new InputStreamReader(input));
     }
 
     @Override
     public boolean accepts(Class<? extends IChemObject> classObject) {
         Class<?>[] interfaces = classObject.getInterfaces();
         for (int i = 0; i < interfaces.length; i++) {
             if (IChemFile.class.equals(interfaces[i])) return true;
         }
         if (IChemFile.class.equals(classObject)) return true;
         Class superClass = classObject.getSuperclass();
         if (superClass != null) return this.accepts(superClass);
         return false;
     }
 
     /**
      *
      * Takes an object which subclasses IChemObject, e.g. Molecule, and will
      * read this (from file, database, internet etc). If the specific
      * implementation does not support a specific IChemObject it will throw
      * an Exception.
      *
      * @param oObj  The object that subclasses IChemObject
      * @return      The IChemObject read
      * @exception   CDKException
      *
      */
     @Override
     public <T extends IChemObject> T read(T oObj) throws CDKException {
         if (oObj instanceof IChemFile) {
             return (T) readChemFile((IChemFile) oObj);
         } else {
             throw new CDKException("Only supported is reading of ChemFile objects.");
         }
     }
 
 
/** Reads and returns a ChemFile object in PDB format. */

private IChemFile readChemFile(IChemFile oFile) throws CDKException {
    // Implementation logic goes here
    
    return oFile;
}
 

}