/* Copyright (C) 2003-2007  The Chemistry Development Kit (CDK) project
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
  */
 package org.openscience.cdk.io.iterator;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.Reader;
 import java.io.StringReader;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.NoSuchElementException;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.io.ISimpleChemObjectReader;
 import org.openscience.cdk.io.MDLReader;
 import org.openscience.cdk.io.MDLV2000Reader;
 import org.openscience.cdk.io.MDLV3000Reader;
 import org.openscience.cdk.io.formats.IChemFormat;
 import org.openscience.cdk.io.formats.IResourceFormat;
 import org.openscience.cdk.io.formats.MDLFormat;
 import org.openscience.cdk.io.formats.MDLV2000Format;
 import org.openscience.cdk.io.formats.MDLV3000Format;
 import org.openscience.cdk.io.setting.BooleanIOSetting;
 import org.openscience.cdk.io.setting.IOSetting;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 
 /**
  * Iterating MDL SDF reader. It allows to iterate over all molecules
  * in the SD file, without reading them into memory first. Suitable
  * for (very) large SDF files. For parsing the molecules in the
  * SD file, it uses the <code>MDLV2000Reader</code> or
  * <code>MDLV3000Reader</code> reader; it does <b>not</b> work
  * for SDF files with MDL formats prior to the V2000 format.
  *
  * <p>Example use:
  * <pre>
  * File sdfFile = new File("../zinc-structures/ZINC_subset3_3D_charged_wH_maxmin1000.sdf");
  * IteratingSDFReader reader = new IteratingSDFReader(
  *   new FileInputStream(sdfFile), DefaultChemObjectBuilder.getInstance()
  * );
  * while (reader.hasNext()) {
  *   IAtomContainer molecule = (IAtomContainer)reader.next();
  * }
  * </pre>
  *
  * @cdk.module io
  * @cdk.githash
  *
  * @see org.openscience.cdk.io.MDLV2000Reader
  * @see org.openscience.cdk.io.MDLV3000Reader
  *
  * @author Egon Willighagen &lt;egonw@sci.kun.nl&gt;
  * @cdk.created    2003-10-19
  *
  * @cdk.keyword    file format, MDL molfile
  * @cdk.keyword    file format, SDF
  * @cdk.iooptions
  */
 public class IteratingSDFReader extends DefaultIteratingChemObjectReader<IAtomContainer> {
 
     private BufferedReader                                  input;
     private static ILoggingTool                             logger               = LoggingToolFactory
                                                                                          .createLoggingTool(IteratingSDFReader.class);
     private String                                          currentLine;
     private IChemFormat                                     currentFormat;
 
     private boolean                                         nextAvailableIsKnown;
     private boolean                                         hasNext;
     private IChemObjectBuilder                              builder;
     private IAtomContainer                                  nextMolecule;
 
     private BooleanIOSetting                                forceReadAs3DCoords;
 
     // if an error is encountered the reader will skip over the error
     private boolean                                         skip                 = false;
 
     // buffer to store pre-read Mol records in
     private StringBuilder                                   buffer               = new StringBuilder(10000);
 
     private static final String                             LINE_SEPARATOR       = "\n";
 
     // patterns to match
     private static Pattern MDL_VERSION          = Pattern.compile("[vV](2000|3000)");
     private static String  M_END                = "M  END";
     private static String  SDF_RECORD_SEPARATOR = "$$$$";
     private static String  SDF_DATA_HEADER      = "> ";
 
     // map of MDL formats to their readers
     private final Map<IChemFormat, ISimpleChemObjectReader> readerMap            = new HashMap<IChemFormat, ISimpleChemObjectReader>(
                                                                                          5);
 
     /**
      * Constructs a new IteratingMDLReader that can read Molecule from a given Reader.
      *
      * @param  in  The Reader to read from
      * @param builder The builder
      */
     public IteratingSDFReader(Reader in, IChemObjectBuilder builder) {
         this(in, builder, false);
     }
 
     /**
      * Constructs a new IteratingMDLReader that can read Molecule from a given InputStream.
      *
      * @param  in  The InputStream to read from
      * @param builder The builder
      */
     public IteratingSDFReader(InputStream in, IChemObjectBuilder builder) {
         this(new InputStreamReader(in), builder);
     }
 
     /**
      * Constructs a new IteratingMDLReader that can read Molecule from a given a
      * InputStream. This constructor allows specification of whether the reader will
      * skip 'null' molecules. If skip is set to false and a broken/corrupted molecule
      * is read the iterating reader will stop at the broken molecule. However if
      * skip is set to true then the reader will keep trying to read more molecules
      * until the end of the file is reached.
      *
      * @param in       the {@link InputStream} to read from
      * @param builder  builder to use
      * @param skip     whether to skip null molecules
      */
     public IteratingSDFReader(InputStream in, IChemObjectBuilder builder, boolean skip) {
         this(new InputStreamReader(in), builder, skip);
     }
 
     /**
      * Constructs a new IteratingMDLReader that can read Molecule from a given a
      * Reader. This constructor allows specification of whether the reader will
      * skip 'null' molecules. If skip is set to false and a broken/corrupted molecule
      * is read the iterating reader will stop at the broken molecule. However if
      * skip is set to true then the reader will keep trying to read more molecules
      * until the end of the file is reached.
      *
      * @param in       the {@link Reader} to read from
      * @param builder  builder to use
      * @param skip     whether to skip null molecules
      */
     public IteratingSDFReader(Reader in, IChemObjectBuilder builder, boolean skip) {
         this.builder = builder;
         setReader(in);
         initIOSettings();
         setSkip(skip);
     }
 
     @Override
     public IResourceFormat getFormat() {
         return currentFormat;
     }
 
     /**
      *                Method will return an appropriate reader for the provided format. Each reader is stored
      *                in a map, if no reader is available for the specified format a new reader is created. The
      *                {@see ISimpleChemObjectReadr#setErrorHandler(IChemObjectReaderErrorHandler)} and
      *                {@see ISimpleChemObjectReadr#setReaderMode(DefaultIteratingChemObjectReader)}
      *                methods are set.
      *
      * @param  format The format to obtain a reader for
      * @return        instance of a reader appropriate for the provided format
      */
     private ISimpleChemObjectReader getReader(IChemFormat format) {
 
         // create a new reader if not mapped
         if (!readerMap.containsKey(format)) {
 
             ISimpleChemObjectReader reader;
             if (format instanceof MDLV2000Format)
                 reader = new MDLV2000Reader();
             else if (format instanceof MDLV3000Format)
                 reader = new MDLV3000Reader();
             else if (format instanceof MDLFormat)
                 reader = new MDLReader();
             else
                 throw new IllegalArgumentException("Unexpected format: " + format);
             reader.setErrorHandler(this.errorHandler);
             reader.setReaderMode(this.mode);
             if (currentFormat instanceof MDLV2000Format) {
                 reader.addSettings(getSettings());
             }
 
             readerMap.put(format, reader);
 
         }
 
         return readerMap.get(format);
 
     }
 
 
/** Check if the another IAtomContainer can be read */
 public boolean hasNext(){}

 

}