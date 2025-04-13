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
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 
 package org.openscience.cdk.layout;
 
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.smiles.SmilesGenerator;
 import org.openscience.cdk.smiles.SmilesParser;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 
 import javax.vecmath.Point2d;
 import java.io.BufferedReader;
 import java.io.BufferedWriter;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.text.DecimalFormat;
 import java.text.DecimalFormatSymbols;
 import java.util.AbstractMap;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.LinkedHashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 
 import static java.util.AbstractMap.SimpleEntry;
 import static java.util.Map.Entry;
 
 /**
  * A library for 2D layout templates that are retrieved based on identity. Such a library is useful
  * for ensure ring systems are laid out in their de facto orientation. Importantly, identity
  * templates means the library size can be very large but still searched in constant time.
  *
  * <pre>{@code
  *
  * // load from a resource file on the classpath
  * IdentityTemplateLibrary lib = IdentityTemplateLibrary.loadFromResource("/data/ring-templates.smi");
  *
  * IAtomContainer container, container2;
  *
  * // add to the library
  * lib.add(container);
  *
  * // assign a layout
  * boolean modified = lib.assignLayout(container2);
  *
  * // store
  * OutputStream out = new FileOutputStream("/tmp/lib.smi");
  * lib.store(out);
  * out.close();
  * }</pre>
  *
  * @author John May
  */
 final class IdentityTemplateLibrary {
 
     private final Map<String, List<Point2d[]>> templateMap = new LinkedHashMap<>();
 
     private final SmilesGenerator smigen = SmilesGenerator.unique();
     private final ILoggingTool    logger = LoggingToolFactory.createLoggingTool(getClass());
 
     private IdentityTemplateLibrary() {
     }
 
     /**
      * Add one template library to another.
      *
      * @param library another template library
      * @return this library with the other one added in (allows chaining)
      */
     public IdentityTemplateLibrary add(IdentityTemplateLibrary library) {
         for (Map.Entry<String,List<Point2d[]>> e : library.templateMap.entrySet()) {
             this.templateMap.computeIfAbsent(e.getKey(), k -> new LinkedList<>())
                             .addAll(e.getValue());
         }
         return this;
     }
 
 
/** Internal - create a canonical SMILES string temporarily adjusting to default  hydrogen count. */
 private String cansmi(IAtomContainer mol, int[] ordering) throws CDKException{
        IAtomContainer copy = mol.clone();
        for (IAtom atom : copy.atoms()) {
            if (atom.getImplicitHydrogenCount() == null)
                atom.setImplicitHydrogenCount(0);
        }
        return smigen.create(copy, ordering);
    }
    
        /**
        * Add a molecule to the library.
        *
        * @param mol the molecule to add
        * @return this library with the molecule added in (allows chaining)
        * @throws CDKException if the molecule is not canonical
        */
        public IdentityTemplateLibrary add(IAtomContainer mol) throws CDKException {
            int[] ordering = new int[mol.getAtomCount()];
            for (int i = 0; i < ordering.length; i++)
                ordering[i] = i;
            return add(cansmi(mol, ordering), mol);
        }
    
        /**
        * Add a molecule to the library.
        *
        * @param smiles the canonical SMILES string
        * @param mol the molecule to add
        * @return this library with the molecule added in (allows chaining)
        * @throws CDKException if the molecule is not canonical
        */
        public IdentityTemplateLibrary add(String smiles, IAtomContainer mol) throws CDKException {
            int[] ordering = new int[mol.getAtomCount()];
            for (int i = 0; i < ordering.length; i++)
                ordering[i] = i;
            return add(smiles, mol, ordering);
        }
    
        /**
        * Add a molecule to the library.
        *
        * @param smiles the canonical SMILES string
        * @param mol the molecule to add
        * @param ordering the atom ordering
        * @return this library with the molecule added in (allows chaining)
        * @throws CDKException if the molecule is not canonical
        */
        public IdentityTemplateLibrary add(String smiles, IAtomContainer mol, int[] ordering) throws CDKException {
            return add(cansmi(mol, ordering), mol, ordering);       
 }

 

}