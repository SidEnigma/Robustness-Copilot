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
 
 
/** atom labels are processed in a char iter. */

private static boolean processAtomLabels(final CharIter iter, final Map<Integer, String> dest) {
    // Logic for processing atom labels goes here
    
    // Example implementation:
    List<Character> labelChars = new ArrayList<>();
    char currentChar;
    
    // Iterate through the characters in the CharIter
    while (iter.hasNext()) {
        currentChar = iter.next();
        
        // Check if the current character is a comma separator
        if (currentChar == COMMA_SEPARATOR) {
            // Convert the labelChars list to a string and add it to the destination map
            String label = labelChars.stream().map(String::valueOf).collect(Collectors.joining());
            dest.put(dest.size() + 1, label);
            
            // Clear the labelChars list for the next label
            labelChars.clear();
        } else {
            // Add the current character to the labelChars list
            labelChars.add(currentChar);
        }
    }
    
    // Convert the labelChars list to a string and add the final label to the destination map
    String label = labelChars.stream().map(String::valueOf).collect(Collectors.joining());
    dest.put(dest.size() + 1, label);
    
    // Return true to indicate successful processing of atom labels
    return true;
}
 

}