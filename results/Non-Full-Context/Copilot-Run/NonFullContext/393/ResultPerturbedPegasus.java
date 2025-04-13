/*
  * Copyright (c) 2015 John May <jwmay@users.sf.net>
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as published by
  * the Free Software Foundation; either version 2.1 of the License, or (at
  * your option) any later version. All we ask is that proper credit is given
  * for our work, which includes - but is not limited to - adding the above
  * copyright notice to the beginning of your source code files, and to any
  * copyright notice that you may distribute with programs based on this work.
  *
  * This program is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  * License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 U
  */
 
 package org.openscience.cdk.io;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.config.Elements;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.interfaces.IPseudoAtom;
 import org.openscience.cdk.interfaces.IStereoElement;
 import org.openscience.cdk.interfaces.ITetrahedralChirality;
 import org.openscience.cdk.interfaces.ITetrahedralChirality.Stereo;
 import org.openscience.cdk.io.formats.IResourceFormat;
 import org.openscience.cdk.io.formats.MDLV3000Format;
 import org.openscience.cdk.io.setting.IOSetting;
 import org.openscience.cdk.io.setting.StringIOSetting;
 import org.openscience.cdk.sgroup.Sgroup;
 import org.openscience.cdk.sgroup.SgroupBracket;
 import org.openscience.cdk.sgroup.SgroupKey;
 import org.openscience.cdk.sgroup.SgroupType;
 
 import javax.vecmath.Point2d;
 import javax.vecmath.Point3d;
 import java.io.BufferedWriter;
 import java.io.Closeable;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.io.Writer;
 import java.nio.charset.StandardCharsets;
 import java.text.DecimalFormat;
 import java.text.DecimalFormatSymbols;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.Set;
 import java.util.TreeMap;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import static org.openscience.cdk.CDKConstants.ATOM_ATOM_MAPPING;
 import static org.openscience.cdk.io.MDLV2000Writer.OptProgramName;
 
 /**
  * Ctab V3000 format output. This writer provides output to the more modern (but less widely
  * supported) V3000 format. Unlikely the V2000 format that is limited to 999 atoms or bonds
  * V3000 can write arbitrarily large molecules. Beyond this the format removes some (but not all)
  * ambiguities and simplifies output values with tagging (e.g 'CHG=-1' instead of '5').
  * 
  * Supported Features:
  * <ul>
  *     <li>Atom Block, non-query features</li>
  *     <li>Bond Block, non-query features</li>
  *     <li>Sgroup Block, partial support for all chemical Sgroups, complete support for: Abbreviations,
  *     MultipleGroup, SRUs, (Un)ordered Mixtures</li>
  * </ul>
  * The 3D block and enhanced stereochemistry is not currently supported.
  */
 public final class MDLV3000Writer extends DefaultChemObjectWriter {
 
     private static final Pattern         R_GRP_NUM = Pattern.compile("R(\\d+)");
     private              V30LineWriter   writer;
     private              StringIOSetting programNameOpt;
 
     /**
      * Create a new V3000 writer, output to the provided JDK writer.
      *
      * @param writer output location
      */
     public MDLV3000Writer(Writer writer) {
         this();
         this.writer = new V30LineWriter(writer);
     }
 
     /**
      * Create a new V3000 writer, output to the provided JDK output stream.
      *
      * @param out output location
      */
     public MDLV3000Writer(OutputStream out) throws CDKException {
         this();
         this.setWriter(out);
     }
 
     /**
      * Default empty constructor.
      */
     public MDLV3000Writer() {
         initIOSettings();
     }
 
     /**
      * Safely access nullable int fields by defaulting to zero.
      *
      * @param x value
      * @return value, or zero if null
      */
     private static int nullAsZero(Integer x) {
         return x == null ? 0 : x;
     }
 
 
/** If the entry isn't found, we return -1. */
 private static Integer findIdx(Map<T, Integer> idxs, T obj){
        Integer idx = idxs.get(obj);
        return idx == null ? -1 : idx;
    }
    
        /**
        * Safely access nullable double fields by defaulting to zero.
        *
        * @param x value
        * @return value, or zero if null
        */
        private static double nullAsZero(Double x) {
            return x == null ? 0 : x;
        }
    
        /**
        * Safely access nullable String fields by defaulting to an empty string.
        *
        * @param x value
        * @return value, or empty string if null
        */
        private static String nullAsEmpty(String x) {
            return x == null ? "" : x;
        }
    
        /**
        * Safely access nullable String fields by defaulting to an empty string.
        *
        * @param x value
        * @return value, or empty string if null
        */
        private static String nullAsEmpty(Integer x) {
            return x == null ? "" : x.toString();
        }
    
        /**
        * Safely access nullable String fields by defaulting to an empty string.
        *
        * @param x value
        * @return value, or empty string if null
        */
        private static String nullAsEmpty(Double x) {
            return x == null ? "" : x.toString();
        }
    
        /**
        * Safely access nullable String fields by defaulting to an empty string.
        *
        * @param x value
        * @return value, or empty string if null
        */
        private static String nullAsEmpty(Boolean x) {
            return x == null ? "" : x.toString();
        }
    
        /**
        * Safely access nullable String fields by defaulting to an empty string.
        *
        * @param x value
        * @return value, or empty string if null
        */
        private static String nullAsEmpty(Stereo x) {
            return x == null ? "" : x.toString();
        }
    
        /**
        * Safely        
 }

 

}