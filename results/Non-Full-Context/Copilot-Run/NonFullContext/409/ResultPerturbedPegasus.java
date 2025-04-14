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
 
 import java.io.BufferedReader;
 import java.io.InputStreamReader;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 import org.openscience.cdk.io.formats.IChemFormat;
 import org.openscience.cdk.io.formats.IResourceFormat;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 
 /**
  * Helper tool to create IChemObjectWriters.
  *
  * @author Egon Willighagen &lt;ewilligh@uni-koeln.de&gt;
  * @cdk.module io
  * @cdk.githash
  **/
 public class WriterFactory {
 
     private final static String                          IO_FORMATS_LIST = "io-formats.set";
 
     private static ILoggingTool                          logger          = LoggingToolFactory
                                                                                  .createLoggingTool(WriterFactory.class);
 
     private static List<IChemFormat>                     formats         = null;
 
     private static Map<String, Class<IChemObjectWriter>> registeredReaders;
 
     /**
      * Constructs a ChemObjectIOInstantionTests.
      */
     public WriterFactory() {
         registeredReaders = new HashMap<String, Class<IChemObjectWriter>>();
     }
 
     public void registerWriter(Class<?> writer) {
         if (writer == null) return;
         if (IChemObjectWriter.class.isAssignableFrom(writer)) {
             registeredReaders.put(writer.getName(), (Class<IChemObjectWriter>) writer);
         }
     }
 
 
/** IChemFormats provide a container for serialization. */
 public IChemFormat[] findChemFormats(int features){
        if (formats == null) {
            formats = new ArrayList<IChemFormat>();
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(WriterFactory.class.getClassLoader().getResourceAsStream(IO_FORMATS_LIST)));
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("#")) continue;
                    String[] tokens = line.split("\\s+");
                    if (tokens.length < 2) continue;
                    String name = tokens[0];
                    String className = tokens[1];
                    Class<?> clazz = Class.forName(className);
                    if (IChemFormat.class.isAssignableFrom(clazz)) {
                        IChemFormat format = (IChemFormat) clazz.getConstructor().newInstance();
                        if ((format.getSupportedFeatures() & features) == features) {
                            formats.add(format);
                        }
                    }
                }
                in.close();
            } catch (Exception e) {
                logger.error("Could not load formats from " + IO_FORMATS_LIST, e);
            }
        }
        return formats.toArray(new IChemFormat[formats.size()]);
    }
    
        public IChemObjectWriter createWriter(String formatName, IResourceFormat format) {
            if (formatName == null) return null;
            if (format == null) {
                IChemFormat[] formats = findChemFormats(IChemObjectWriter.class);
                for (IChemFormat chemFormat : formats) {
                    if (chemFormat.getName().equals(formatName)) {
                        format = chemFormat;
                        break;
                    }
                }
            }
            if (format == null) return null;
            Class<IChemObjectWriter> writerClass = registeredReaders.get(format.getClass().getName());
            if (writerClass == null) return null;
            try {
                return writerClass.getConstructor(IResourceFormat.class).newInstance(format);
            } catch (Exception e) {     
 }

 

}