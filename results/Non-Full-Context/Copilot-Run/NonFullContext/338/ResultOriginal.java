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
 
 package org.openscience.cdk.renderer.generators.standard;
 
 import org.openscience.cdk.config.Elements;
 
 import java.util.ArrayDeque;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Deque;
 import java.util.List;
 
 /**
  * Utility class for handling/formatting abbreviation (superatom) labels.
  * 
  * Depending on orientation a label may need to be reversed. For example
  * consider '-OAc', if the bond exits from the right it is preferable to
  * write it 'AcO-'. Other labels don't need reversing at all (e.g. tBu).
  * We reverse labels by spiting them up into 'tokens', reversing token order, * and then joining them back together.
  * 
  * Abbreviation labels that are formulas benefit from sub and subscripting
  * certain parts. For example OPO3H2 looks better with the digits 3 and 2
  * rendered in subscript.
  */
 final class AbbreviationLabel {
 
     /**
      * Better rendering of negative charge by using minus and not
      * an ascii hyphen.
      */
     private static final String MINUS_STRING = "\u2212";
 
     // chemical symbol prefixes
     private final static String[] PREFIX_LIST = new String[]{
             "n", "norm", "n-", "c", "cy", "cyc", "cyclo", "c-", "cy-", "cyc-", "i", "iso", "i-", "t", "tert", "t-", "s",
             "sec", "s-", "o", "ortho", "o-", "m", "meta", "m-", "p", "para", "p-", "1-", "2-", "3-", "4-", "5-", "6-",
             "7-", "8-", "9-"
     };
 
     // see https://en.wikipedia.org/wiki/Wikipedia:Naming_conventions_(chemistry)#Prefixes_in_titles
     private final static String[] ITAL_PREFIX = new String[]{
             "n", "norm", "sec", "s", "tert", "t",
             "ortho", "o", "meta", "m", "para", "p"
     };
 
     // chemical symbols excluding periodic symbols which are loaded separately
     // Some of these are derived from https://github.com/openbabel/superatoms that
     // has the following license:
     //    This is free and unencumbered software released into the public domain.
     //
     //    Anyone is free to copy, modify, publish, use, compile, sell, or
     //    distribute this software, either in source code form or as a compiled
     //    binary, for any purpose, commercial or non-commercial, and by any
     //    means.
     //
     //    In jurisdictions that recognize copyright laws, the author or authors
     //    of this software dedicate any and all copyright interest in the
     //    software to the public domain. We make this dedication for the benefit
     //    of the public at large and to the detriment of our heirs and
     //    successors. We intend this dedication to be an overt act of
     //    relinquishment in perpetuity of all present and future rights to this
     //    software under copyright law.
     //
     //    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
     //    EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
     //    MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
     //    IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
     //    OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
     //    ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
     //    OTHER DEALINGS IN THE SOFTWARE.
     //
     //    For more information, please refer to <http://unlicense.org/>
     private final static String[] SYMBOL_LIST = new String[]{"acac", "Ace", "Acetyl", "Acyl", "Ad", "All", "Alloc", "Allyl", "Amyl", "AOC",
                                                              "BDMS", "Benzoyl", "Benzyl", "Bn", "BOC", "Boc", "BOM", "bpy", "Bromo", "Bs", "Bu", "But", "Butyl", "Bz", "Bzl",
                                                              "Car", "Cbz", "Chloro", "CoA", "Cy",
                                                              "dppf", "dppp", "dba", "D", "Dan", "Dansyl", "DEIPS", "DEM", "Dip", "Dmb", "DPA", "DTBMS",
                                                              "EE", "EOM", "Et", "Ethyl",
                                                              "Fluoro", "FMOC", "Fmoc", "Formyl",
                                                              "Heptyl", "Hexyl",
                                                              "Iodo", "IPDMS",
                                                              "Me", "MEM", "Mesityl", "Mesyl", "Methoxy", "Methyl", "MOM", "Ms",
                                                              "Nitro",
                                                              "Oct", "Octyl",
                                                              "PAB", "Pentyl", "Ph", "Phenyl", "Pivaloyl", "PMB", "Pro", "Propargyl", "Propyl", "Pv",
                                                              "R", "SEM",
                                                              "T", "TBS", "TBDMS", "Trt", "TBDPS", "TES", "Tf", "THP", "THPO", "TIPS", "TMS", "Tos", "Tol", "Tosyl", "Tr", "Troc",
                                                              "Vinyl", "Voc", "Z"};
 
     private static Trie PREFIX_TRIE = new Trie();
     private static Trie ITAL_PREFIX_TRIE = new Trie();
     private static Trie SYMBOL_TRIE = new Trie();
 
     // build the tries on class init
     static {
         for (String str : PREFIX_LIST)
             insert(PREFIX_TRIE, str, 0);
         for (String str : ITAL_PREFIX)
             insert(ITAL_PREFIX_TRIE, str, 0);
         for (Elements elem : Elements.values())
             if (!elem.symbol().isEmpty())
                 insert(SYMBOL_TRIE, elem.symbol(), 0);
         for (String str : SYMBOL_LIST)
             insert(SYMBOL_TRIE, str, 0);
     }
 
     static int STYLE_NORMAL    = 0;
     static int STYLE_SUBSCRIPT = -1;
     static int STYLE_SUPSCRIPT = +1;
     static int STYLE_ITALIC    = 2;
 
     /**
      * A small class to help describe which parts of a string
      * are super and subscript (style field).
      */
     static final class FormattedText {
         String text;
         final int style;
 
         public FormattedText(String text, int style) {
             this.text = text;
             this.style = style;
         }
     }
 
 
/** Split a label it to recognised tokens for reversing, the  validity of the label is not checked! The method is intended  for zero/single attachments only and linkers are not supported. */
 static boolean parse(String label, List<String> tokens){
        if (label == null)
            return false;
        if (tokens == null)
            return false;
        tokens.clear();
        int len = label.length();
        if (len == 0)
            return false;
        int i = 0;
        int style = STYLE_NORMAL;
        StringBuilder sb = new StringBuilder();
        while (i < len) {
            char c = label.charAt(i);
            if (c == '_') {
                if (style == STYLE_NORMAL)
                    style = STYLE_SUBSCRIPT;
                else if (style == STYLE_SUBSCRIPT)
                    style = STYLE_NORMAL;
                else
                    style = STYLE_SUBSCRIPT;
            } else if (c == '^') {
                if (style == STYLE_NORMAL)
                    style = STYLE_SUPSCRIPT;
                else if (style == STYLE_SUPSCRIPT)
                    style = STYLE_NORMAL;
                else
                    style = STYLE_SUPSCRIPT;
            } else if (c == ' ') {
                if (sb.length() > 0) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
            } else if (c == '\\') {
                if (i + 1 < len) {
                    char c2 = label.charAt(i + 1);
                    if (c2 == '_' || c2 == '^') {
                        if (sb.length() > 0) {
                            tokens.add(sb.toString());
                            sb.setLength(0);
                        }
                        tokens.add(Character.toString(c2));
                        i++;
                    } else {
                        sb.append(c);
                        sb.append(c2);
                        i++;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
            i++;
        }
        if      
 }

 

}