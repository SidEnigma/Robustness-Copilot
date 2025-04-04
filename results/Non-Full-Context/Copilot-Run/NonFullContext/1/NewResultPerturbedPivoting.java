/*
  * Copyright (c) 2014 European Bioinformatics Institute (EMBL-EBI)
  *                    John May <jwmay@users.sf.net>
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
 
 package org.openscience.cdk.forcefield.mmff;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.graph.Cycles;
 import org.openscience.cdk.graph.GraphUtil;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.isomorphism.Pattern;
 import org.openscience.cdk.smarts.SmartsPattern;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import static org.openscience.cdk.graph.GraphUtil.EdgeToBondMap;
 
 /**
  * Determine the MMFF symbolic atom types {@cdk.cite Halgren96a}. The matcher uses SMARTS patterns
  * to assign preliminary symbolic types. The types are then adjusted considering aromaticity {@link
  * MmffAromaticTypeMapping}. The assigned atom types validate completely with the validation suite
  * (http://server.ccl.net/cca/data/MMFF94/).
  *
  * <pre>{@code
  * MmffAtomTypeMatcher mmffAtomTypes = new MmffAtomTypeMatcher();
  *
  * for (IAtomContainer container : containers) {
  *     String[] symbs = mmffAtomTypes.symbolicTypes(container);
  * }
  * }</pre>
  *
  * @author John May
  */
 final class MmffAtomTypeMatcher {
 
     /** Aromatic types are assigned by this class. */
     private final MmffAromaticTypeMapping aromaticTypes = new MmffAromaticTypeMapping();
 
     /** Substructure patterns for atom types. */
     private final AtomTypePattern[]       patterns;
 
     /** Mapping of parent to hydrogen symbols. */
     private final Map<String, String>     hydrogenMap;
 
     /**
      * Create a new MMFF atom type matcher, definitions are loaded at instantiation.
      */
     MmffAtomTypeMatcher() {
 
         final InputStream smaIn = getClass().getResourceAsStream("MMFFSYMB.sma");
         final InputStream hdefIn = getClass().getResourceAsStream("mmff-symb-mapping.tsv");
 
         try {
             this.patterns = loadPatterns(smaIn);
             this.hydrogenMap = loadHydrogenDefinitions(hdefIn);
         } catch (IOException e) {
             throw new InternalError("Atom type definitions for MMFF94 Atom Types could not be loaded: "
                     + e.getMessage());
         } finally {
             close(smaIn);
             close(hdefIn);
         }
     }
 
     /**
      * Obtain the MMFF symbolic types to the atoms of the provided structure.
      *
      * @param container structure representation
      * @return MMFF symbolic types for each atom index
      */
     String[] symbolicTypes(final IAtomContainer container) {
         EdgeToBondMap bonds = EdgeToBondMap.withSpaceFor(container);
         int[][] graph = GraphUtil.toAdjList(container, bonds);
         return symbolicTypes(container, graph, bonds, new HashSet<IBond>());
     }
 
     /**
      * Obtain the MMFF symbolic types to the atoms of the provided structure.
      *
      * @param container structure representation
      * @param graph     adj list data structure
      * @param bonds     bond lookup map
      * @param mmffArom  flags which bonds are aromatic by MMFF model
      * @return MMFF symbolic types for each atom index
      */
     String[] symbolicTypes(final IAtomContainer container, final int[][] graph, final EdgeToBondMap bonds, final Set<IBond> mmffArom) {
 
         // Array of symbolic types, MMFF refers to these as 'SYMB' and the numeric
         // value a s 'TYPE'.
         final String[] symbs = new String[container.getAtomCount()];
 
         checkPreconditions(container);
 
         assignPreliminaryTypes(container, symbs);
 
         // aromatic types, set by upgrading preliminary types in specified positions
         // and conditions. This requires a fair bit of code and is delegated to a separate class.
         aromaticTypes.assign(container, symbs, bonds, graph, mmffArom);
 
         // special case, 'NCN+' matches entries that the validation suite say should
         // actually be 'NC=N'. We can achieve 100% compliance by checking if NCN+ is still
         // next to CNN+ or CIM+ after aromatic types are assigned
         fixNCNTypes(symbs, graph);
 
         assignHydrogenTypes(container, symbs, graph);
 
         return symbs;
     }
 
     /**
      * Special case, 'NCN+' matches entries that the validation suite say should actually be 'NC=N'.
      * We can achieve 100% compliance by checking if NCN+ is still next to CNN+ or CIM+ after
      * aromatic types are assigned
      *
      * @param symbs symbolic types
      * @param graph adjacency list graph
      */
     private void fixNCNTypes(String[] symbs, int[][] graph) {
         for (int v = 0; v < graph.length; v++) {
             if ("NCN+".equals(symbs[v])) {
                 boolean foundCNN = false;
                 for (int w : graph[v]) {
                     foundCNN = foundCNN || "CNN+".equals(symbs[w]) || "CIM+".equals(symbs[w]);
                 }
                 if (!foundCNN) {
                     symbs[v] = "NC=N";
                 }
             }
         }
     }
 
     /**
      * preconditions, 1. all hydrogens must be present as explicit nodes in the connection table.
      * this requires that each atom explicitly states it has exactly 0 hydrogens 2. the SMARTS treat
      * all atoms as aliphatic and therefore no aromatic flags should be set, we could remove this
      * but ideally we don't want to modify the structure
      *
      * @param container input structure representation
      */
     private void checkPreconditions(IAtomContainer container) {
         for (IAtom atom : container.atoms()) {
             if (atom.getImplicitHydrogenCount() == null || atom.getImplicitHydrogenCount() != 0)
                 throw new IllegalArgumentException("Hydrogens should be unsuppressed (explicit)");
             if (atom.getFlag(CDKConstants.ISAROMATIC))
                 throw new IllegalArgumentException("No aromatic flags should be set");
         }
     }
 
     /**
      * Hydrogen types, assigned based on the MMFFHDEF.PAR parent associations.
      *
      * @param container input structure representation
      * @param symbs     symbolic atom types
      * @param graph     adjacency list graph
      */
     private void assignHydrogenTypes(IAtomContainer container, String[] symbs, int[][] graph) {
         for (int v = 0; v < graph.length; v++) {
             if ("H".equals(container.getAtom(v).getSymbol()) && graph[v].length == 1) {
                 int w = graph[v][0];
                 symbs[v] = this.hydrogenMap.get(symbs[w]);
             }
         }
     }
 
     /**
      * Preliminary atom types are assigned using SMARTS definitions.
      *
      * @param container input structure representation
      * @param symbs     symbolic atom types
      */
     private void assignPreliminaryTypes(IAtomContainer container, String[] symbs) {
         // shallow copy
         IAtomContainer cpy = container.getBuilder().newInstance(IAtomContainer.class, container);
         Cycles.markRingAtomsAndBonds(cpy);
         for (AtomTypePattern matcher : patterns) {
             for (final int idx : matcher.matches(cpy)) {
                 if (symbs[idx] == null) {
                     symbs[idx] = matcher.symb;
                 }
             }
         }
     }
 
 
/** Internal - load SMARTS models for each type of atom from MMFFSYMB. */

static AtomTypePattern[] loadPatterns(InputStream smaIn) throws IOException {
    List<AtomTypePattern> patterns = new ArrayList<>();
    BufferedReader reader = new BufferedReader(new InputStreamReader(smaIn));
    String line;
    while ((line = reader.readLine()) != null) {
        if (line.startsWith("#")) {
            continue;
        }
        String[] parts = line.split("\t");
        if (parts.length != 2) {
            throw new IOException("Invalid format in MMFFSYMB file");
        }
        String symb = parts[0];
        String smarts = parts[1];
        SmartsPattern pattern = SmartsPattern.create(smarts);
        patterns.add(new AtomTypePattern(symb, pattern));
    }
    reader.close();
    return patterns.toArray(new AtomTypePattern[0]);
}
 

}