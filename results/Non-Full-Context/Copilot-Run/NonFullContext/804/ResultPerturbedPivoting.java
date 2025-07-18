/*  Copyright (C) 2003-2005  Christoph Steinbeck
  *                2003-2008  Egon Willighagen
  *                           Stefan Kuhn
  *                           Rajarshi Guha
  *                2015       John May
  *
  *  Contact: cdk-devel@lists.sourceforge.net
  *
  *  This program is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU Lesser General Public License
  *  as published by the Free Software Foundation; either version 2.1
  *  of the License, or (at your option) any later version.
  *  All we ask is that proper credit is given for our work, which includes
  *  - but is not limited to - adding the above copyright notice to the beginning
  *  of your source code files, and to any copyright notice that you may distribute
  *  with programs based on this work.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU Lesser General Public License for more details.
  *
  *  You should have received a copy of the GNU Lesser General Public License
  *  along with this program; if not, write to the Free Software
  *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 package org.openscience.cdk.layout;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.geometry.GeometryUtil;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomContainerSet;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemFile;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.io.CMLReader;
 import org.openscience.cdk.isomorphism.AtomMatcher;
 import org.openscience.cdk.isomorphism.BondMatcher;
 import org.openscience.cdk.isomorphism.Mappings;
 import org.openscience.cdk.isomorphism.Pattern;
 import org.openscience.cdk.isomorphism.VentoFoggia;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
 import org.openscience.cdk.tools.manipulator.ChemFileManipulator;
 
 import javax.vecmath.Point2d;
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 /**
  * Helper class for Structure Diagram Generation. Handles templates. This is
  * our layout solution for ring systems which are notoriously difficult to
  * layout, like cubane, adamantane, porphyrin, etc.
  *
  * @author steinbeck
  * @cdk.created 2003-09-04
  * @cdk.keyword layout
  * @cdk.keyword 2D-coordinates
  * @cdk.keyword structure diagram generation
  * @cdk.require java1.4+
  * @cdk.module sdg
  * @cdk.githash
  */
 public final class TemplateHandler {
 
     private final static ILoggingTool         LOGGER       = LoggingToolFactory.createLoggingTool(TemplateHandler.class);
     private final        List<IAtomContainer> templates    = new ArrayList<>();
     private final        List<Pattern>        anonPatterns = new ArrayList<>();
     private final        List<Pattern>        elemPatterns = new ArrayList<>();
 
 
     private final AtomMatcher elemAtomMatcher = new AtomMatcher() {
         @Override
         public boolean matches(IAtom a, IAtom b) {
             return a.getAtomicNumber().equals(b.getAtomicNumber());
         }
     };
     private final AtomMatcher anonAtomMatcher = new AtomMatcher() {
         @Override
         public boolean matches(IAtom a, IAtom b) {
             return true;
         }
     };
     private final BondMatcher anonBondMatcher = new BondMatcher() {
         @Override
         public boolean matches(IBond a, IBond b) {
             return true;
         }
     };
 
     /**
      * Creates a new TemplateHandler with default templates loaded.
      */
     public TemplateHandler(IChemObjectBuilder builder) {
         loadTemplates(builder);
     }
 
     /**
      * Creates a new TemplateHandler without any default templates.
      */
     public TemplateHandler() {
     }
 
     /**
      * Loads all existing templates into memory. To add templates to be used in
      * SDG, place a drawing with the new template in org/openscience/cdk/layout/templates and add the
      * template filename to org/openscience/cdk/layout/templates/template.list
      */
     public void loadTemplates(IChemObjectBuilder builder) {
         String line = null;
         try {
             InputStream ins = this.getClass().getClassLoader()
                                   .getResourceAsStream("org/openscience/cdk/layout/templates/templates.list");
             BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
             while (reader.ready()) {
                 line = reader.readLine();
                 line = "org/openscience/cdk/layout/templates/" + line;
                 LOGGER.debug("Attempting to read template ", line);
                 try {
                     CMLReader structureReader = new CMLReader(this.getClass().getClassLoader()
                                                                   .getResourceAsStream(line));
                     IChemFile file = (IChemFile) structureReader.read(builder.newInstance(IChemFile.class));
                     List<IAtomContainer> files = ChemFileManipulator.getAllAtomContainers(file);
                     for (int i = 0; i < files.size(); i++)
                         addMolecule(files.get(i));
                     LOGGER.debug("Successfully read template ", line);
                 } catch (CDKException | IllegalArgumentException e) {
                     LOGGER.warn("Could not read template ", line, ", reason: ", e.getMessage());
                     LOGGER.debug(e);
                 }
 
             }
         } catch (IOException e) {
             LOGGER.warn("Could not read (all of the) templates, reason: ", e.getMessage());
             LOGGER.debug(e);
         }
     }
 
 
 
/** Adds a molecule to the list of templates used by this TemplateHandler. */
 public void addMolecule(IAtomContainer molecule){
        templates.add(molecule);
    }
    
        /**
        * Adds a template to the list of templates used by this TemplateHandler.
        *
        * @param filename The filename of the template to be added.
        */
        public void addTemplate(String filename) {
            try {
                CMLReader structureReader = new CMLReader(this.getClass().getClassLoader()
                                                            .getResourceAsStream(filename));
                IChemFile file = (IChemFile) structureReader.read(null);
                List<IAtomContainer> files = ChemFileManipulator.getAllAtomContainers(file);
                for (int i = 0; i < files.size(); i++)
                    addMolecule(files.get(i));
            } catch (CDKException | IllegalArgumentException e) {
                LOGGER.warn("Could not read template ", filename, ", reason: ", e.getMessage());
                LOGGER.debug(e);
            }
        }
    
        /**
        * Adds a template to the list of templates used by this TemplateHandler.
        *
        * @param ins The InputStream of the template to be added.
        */
        public void addTemplate(InputStream ins) {
            try {
                CMLReader structureReader = new CMLReader(ins);
                IChemFile file = (IChemFile) structureReader.read(null);
                List<IAtomContainer> files = ChemFileManipulator.getAllAtomContainers(file);
                for (int i = 0; i < files.size(); i++)
                    addMolecule(files.get(i));
            } catch (CDKException | IllegalArgumentException e) {
                LOGGER.warn("Could not read template ", ins, ", reason: ", e.getMessage());
                LOGGER.debug(e);
            }
        }
    
        /**
        * Adds a template to the list of templates used by this TemplateHandler.
        *
        * @param ins The InputStream of the template to be added.
        */
        public void addTemplate(InputStream ins, String filename) {
            try     
 }

 

}