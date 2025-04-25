/*
  * Copyright (c) 2016 John May <jwmay@users.sf.net>
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
 
 package org.openscience.cdk.smiles;
 
 import org.openscience.cdk.interfaces.IStereoElement;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.TreeMap;
 
 /**
  * Parse CXSMILES (ChemAxon Extended SMILES) layers. The layers are suffixed after the SMILES but before the title
  * and encode a large number of the features. CXSMILES was not intended for outside consumption so has some quirks
  * but does provide some useful features. This parser handles a subset of the grammar:
  * <br>
  * <pre>
  * - Atom Labels
  * - Atom Values
  * - Atom Coordinates
  * - Positional Variations
  * - Polymer Sgroups
  * - Atom Radicals
  * - Fragment grouping
  * </pre>
  * The following properties are ignored
  * <pre>
  * - cis/trans specification
  * - relative stereochemistry
  * </pre>
  */
 final class CxSmilesParser {
 
     private static final char COMMA_SEPARATOR = ',';
     private static final char DOT_SEPARATOR   = '.';
 
     private CxSmilesParser() {
     }
 
     /**
      * Process atom labels from extended SMILES in a char iter.
      *
      * @param iter char iteration
      * @param dest destination of labels (atomidx->label)
      * @return parse success/failure
      */
     private static boolean processAtomLabels(final CharIter iter, final Map<Integer, String> dest) {
         int atomIdx = 0;
         while (iter.hasNext()) {
 
             // fast forward through empty labels
             while (iter.nextIf(';'))
                 atomIdx++;
 
             char c = iter.next();
             if (c == '$') {
                 iter.nextIf(','); // optional
                 // end of atom label
                 return true;
             } else {
                 iter.pos--; // push back
                 int beg = iter.pos;
                 int rollback = beg;
                 while (iter.hasNext()) {
 
                     if (iter.pos == beg && iter.curr() == '_' &&
                         iter.peek() == 'R') {
                         ++beg;
                     }
 
                     // correct step over of escaped label
                     if (iter.curr() == '&') {
                         rollback = iter.pos;
                         if (iter.nextIf('&') && iter.nextIf('#') && iter.nextIfDigit()) {
                             while (iter.nextIfDigit()){} // more digits
                             if (!iter.nextIf(';')) {
                                 iter.pos = rollback;
                             } else {
                             }
                         } else {
                             iter.pos = rollback;
                         }
                     }
                     else if (iter.curr() == ';')
                         break;
                     else if (iter.curr() == '$')
                         break;
                     else
                         iter.next();
                 }
                 dest.put(atomIdx, unescape(iter.substr(beg, iter.pos)));
                 atomIdx++;
                 if (iter.nextIf('$')) {
                     iter.nextIf(','); // optional
                     return true;
                 }
                 if (!iter.nextIf(';'))
                     return false;
             }
         }
         return false;
     }
 
     private static double readDouble(CharIter iter) {
         int sign = +1;
         if (iter.nextIf('-'))
             sign = -1;
         else if (iter.nextIf('+'))
             sign = +1;
         double intPart;
         double fracPart = 0;
         int divisor = 1;
 
         intPart = (double) processUnsignedInt(iter);
         if (intPart < 0) intPart = 0;
         iter.nextIf('.');
 
         char c;
         while (iter.hasNext() && isDigit(c = iter.curr())) {
             fracPart *= 10;
             fracPart += c - '0';
             divisor *= 10;
             iter.next();
         }
 
         return sign * (intPart + (fracPart / divisor));
     }
 
     /**
      * Coordinates are written between parenthesis. The z-coord may be omitted '(0,1,),(2,3,)'.
      * @param iter input characters, iterator is progressed by this method
      * @param state output CXSMILES state
      * @return parse was a success (or not)
      */
     private static boolean processCoords(CharIter iter, CxSmilesState state) {
         if (state.atomCoords == null)
             state.atomCoords = new ArrayList<>();
         while (iter.hasNext()) {
 
             // end of coordinate list
             if (iter.curr() == ')') {
                 iter.next();
                 iter.nextIf(','); // optional
                 return true;
             }
 
             double x = readDouble(iter);
             if (!iter.nextIf(','))
                 return false;
             double y = readDouble(iter);
             if (!iter.nextIf(','))
                 return false;
             double z = readDouble(iter);
             iter.nextIf(';');
 
             state.coordFlag = state.coordFlag || z != 0;
             state.atomCoords.add(new double[]{x, y, z});
         }
         return false;
     }
 
     /**
      * Fragment grouping defines disconnected components that should be considered part of a single molecule (i.e.
      * Salts). Examples include NaH, AlCl3, Cs2CO3, HATU, etc.
      *
      * @param iter input characters, iterator is progressed by this method
      * @param state output CXSMILES state
      * @return parse was a success (or not)
      */
     private static boolean processFragmentGrouping(final CharIter iter, final CxSmilesState state) {
         if (state.fragGroups == null)
             state.fragGroups = new ArrayList<>();
         List<Integer> dest = new ArrayList<>();
         while (iter.hasNext()) {
             dest.clear();
             if (!processIntList(iter, DOT_SEPARATOR, dest))
                 return false;
             iter.nextIf(COMMA_SEPARATOR);
             if (dest.isEmpty())
                 return true;
             state.fragGroups.add(new ArrayList<>(dest));
         }
         return false;
     }
 
     /**
      * Sgroup polymers in CXSMILES can be variable length so may be terminated either with the next group
      * or the end of the CXSMILES.
      *
      * @param c character
      * @return character an delimit an Sgroup
      */
     private static boolean isSgroupDelim(char c) {
         return c == ':' || c == ',' || c == '|';
     }
 
     private static boolean processDataSgroups(CharIter iter, CxSmilesState state) {
 
         if (state.mysgroups == null)
             state.mysgroups = new ArrayList<>(4);
 
         final List<Integer> atomset = new ArrayList<>();
         if (!processIntList(iter, COMMA_SEPARATOR, atomset))
             return false;
 
         if (!iter.nextIf(':'))
             return false;
         int beg = iter.pos;
         while (iter.hasNext() && !isSgroupDelim(iter.curr()))
             iter.next();
         final String field = unescape(iter.substr(beg, iter.pos));
 
         if (!iter.nextIf(':'))
             return false;
         beg = iter.pos;
         while (iter.hasNext() && !isSgroupDelim(iter.curr()))
             iter.next();
         final String value = unescape(iter.substr(beg, iter.pos));
 
         if (!iter.nextIf(':')) {
             state.mysgroups.add(new CxSmilesState.CxDataSgroup(atomset, field, value, "", "", ""));
             return true;
         }
 
         beg = iter.pos;
         while (iter.hasNext() && !isSgroupDelim(iter.curr()))
             iter.next();
         final String operator = unescape(iter.substr(beg, iter.pos));
 
         if (!iter.nextIf(':')) {
             state.mysgroups.add(new CxSmilesState.CxDataSgroup(atomset, field, value, operator, "", ""));
             return true;
         }
 
         beg = iter.pos;
         while (iter.hasNext() && !isSgroupDelim(iter.curr()))
             iter.next();
         final String unit = unescape(iter.substr(beg, iter.pos));
 
         if (!iter.nextIf(':')) {
             state.mysgroups.add(new CxSmilesState.CxDataSgroup(atomset, field, value, operator, unit, ""));
             return true;
         }
 
         beg = iter.pos;
         while (iter.hasNext() && !isSgroupDelim(iter.curr()))
             iter.next();
         final String tag = unescape(iter.substr(beg, iter.pos));
 
         state.mysgroups.add(new CxSmilesState.CxDataSgroup(atomset, field, value, operator, unit, tag));
 
         return true;
     }
 
     /**
      * Polymer Sgroups describe variations of repeating units. Only the atoms and not crossing bonds are written.
      *
      * @param iter input characters, iterator is progressed by this method
      * @param state output CXSMILES state
      * @return parse was a success (or not)
      */
     private static boolean processPolymerSgroups(CharIter iter, CxSmilesState state) {
         if (state.mysgroups == null)
             state.mysgroups = new ArrayList<>();
         int beg = iter.pos;
         while (iter.hasNext() && !isSgroupDelim(iter.curr()))
             iter.next();
         final String keyword = iter.substr(beg, iter.pos);
         if (!iter.nextIf(':'))
             return false;
         final List<Integer> atomset = new ArrayList<>();
         if (!processIntList(iter, COMMA_SEPARATOR, atomset))
             return false;
 
 
         String subscript;
         String supscript;
 
         if (!iter.nextIf(':'))
             return false;
 
         // "If the subscript equals the keyword of the Sgroup this field can be empty", ergo
         // if omitted it equals the keyword
         beg = iter.pos;
         while (iter.hasNext() && !isSgroupDelim(iter.curr()))
             iter.next();
         subscript = unescape(iter.substr(beg, iter.pos));
         if (subscript.isEmpty())
             subscript = keyword;
 
         // "In the superscript only connectivity and flip information is allowed.", default
         // appears to be "eu" either/unspecified for SRU
         if (!iter.nextIf(':'))
             return false;
         beg = iter.pos;
         while (iter.hasNext() && !isSgroupDelim(iter.curr()))
             iter.next();
         supscript = unescape(iter.substr(beg, iter.pos));
         if (supscript.isEmpty() &&
             !keyword.equals("c")&&!keyword.equals("mix")&&
             !keyword.equals("f")&&!keyword.equals("mod"))
             supscript = "eu";
 
         if (iter.nextIf(',') || iter.curr() == '|') {
             state.mysgroups.add(new CxSmilesState.CxPolymerSgroup(keyword, atomset, subscript, supscript));
             return true;
         }
         // not supported: crossing bond info (difficult to work out from doc) and bracket orientation
 
         return false;
     }
 
     private static boolean processIntListMap(Map<Integer,List<Integer>> map, CharIter iter) {
         while (iter.hasNext()) {
             if (isDigit(iter.curr())) {
                 final int beg = processUnsignedInt(iter);
                 if (!iter.nextIf(':'))
                     return false;
                 List<Integer> endpoints = new ArrayList<>(6);
                 if (!processIntList(iter, DOT_SEPARATOR, endpoints))
                     return false;
                 iter.nextIf(',');
                 map.put(beg, endpoints);
             } else {
                 return true;
             }
         }
         return false;
     }
 
     /**
      * Positional variation/multi centre bonding. Describe as a begin atom and one or more end points.
      *
      * @param iter input characters, iterator is progressed by this method
      * @param state output CXSMILES state
      * @return parse was a success (or not)
      */
     private static boolean processPositionalVariation(CharIter iter, CxSmilesState state) {
         if (state.positionVar == null)
             state.positionVar = new TreeMap<>();
         return processIntListMap(state.positionVar, iter);
     }
 
 
     /**
      * Ligand ordering indicate attachments around R groups.
      * @param iter the character iterator
      * @param state the CX state
      * @return parse was a success (or not)
      */
     private static boolean processLigandOrdering(CharIter iter, CxSmilesState state) {
         if (state.ligandOrdering == null)
             state.ligandOrdering = new TreeMap<>();
         return processIntListMap(state.ligandOrdering, iter);
     }
 
     /**
      * CXSMILES radicals.
      *
      * @param iter input characters, iterator is progressed by this method
      * @param state output CXSMILES state
      * @return parse was a success (or not)
      */
     private static boolean processRadicals(CharIter iter, CxSmilesState state) {
         if (state.atomRads == null)
             state.atomRads = new TreeMap<>();
         CxSmilesState.Radical rad;
         switch (iter.next()) {
             case '1':
                 rad = CxSmilesState.Radical.Monovalent;
                 break;
             case '2':
                 rad = CxSmilesState.Radical.Divalent;
                 break;
             case '3':
                 rad = CxSmilesState.Radical.DivalentSinglet;
                 break;
             case '4':
                 rad = CxSmilesState.Radical.DivalentTriplet;
                 break;
             case '5':
                 rad = CxSmilesState.Radical.Trivalent;
                 break;
             case '6':
                 rad = CxSmilesState.Radical.TrivalentDoublet;
                 break;
             case '7':
                 rad = CxSmilesState.Radical.TrivalentQuartet;
                 break;
             default:
                 return false;
         }
         if (!iter.nextIf(':'))
             return false;
         List<Integer> dest = new ArrayList<>(4);
         if (!processIntList(iter, COMMA_SEPARATOR, dest))
             return false;
         for (Integer atomidx : dest)
             state.atomRads.put(atomidx, rad);
         return true;
     }
 
 
/** Parse an string possibly containing CXSMILES into an intermediate state  ({@link CxSmilesState}) representation. */

static int processCx(final String str, final CxSmilesState state) {
    CharIter iter = new CharIter(str);
    int count = 0;
    while (iter.hasNext()) {
        char c = iter.curr();
        switch (c) {
            case 'A':
                iter.next();
                if (!processAtomLabels(iter, state.atomLabels)) {
                    return count;
                }
                break;
            case 'C':
                iter.next();
                if (!processCoords(iter, state)) {
                    return count;
                }
                break;
            case 'F':
                iter.next();
                if (!processFragmentGrouping(iter, state)) {
                    return count;
                }
                break;
            case 'D':
                iter.next();
                if (!processDataSgroups(iter, state)) {
                    return count;
                }
                break;
            case 'P':
                iter.next();
                if (!processPolymerSgroups(iter, state)) {
                    return count;
                }
                break;
            case 'V':
                iter.next();
                if (!processPositionalVariation(iter, state)) {
                    return count;
                }
                break;
            case 'L':
                iter.next();
                if (!processLigandOrdering(iter, state)) {
                    return count;
                }
                break;
            case 'R':
                iter.next();
                if (!processRadicals(iter, state)) {
                    return count;
                }
                break;
            default:
                iter.next();
                break;
        }
        count++;
    }
    return count;
}

}