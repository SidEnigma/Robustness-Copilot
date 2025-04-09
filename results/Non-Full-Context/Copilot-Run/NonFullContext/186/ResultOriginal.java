/*  Copyright (C) 2002-2007  Christoph Steinbeck <steinbeck@users.sf.net>
  *                200?-2007  Egon Willighagen <egonw@users.sf.net>
  *
  *  Contact: cdk-devel@lists.sourceforge.net
  *
  *  This program is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU Lesser General Public License
  *  as published by the Free Software Foundation; either version 2.1
  *  of the License, or (at your option) any later version.
  *  All I ask is that proper credit is given for my work, which includes
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
 package org.openscience.cdk.smiles;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.exception.InvalidSmilesException;
 import org.openscience.cdk.graph.ConnectivityChecker;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomContainerSet;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.interfaces.IPseudoAtom;
 import org.openscience.cdk.interfaces.IReaction;
 import org.openscience.cdk.interfaces.ISingleElectron;
 import org.openscience.cdk.interfaces.IStereoElement;
 import org.openscience.cdk.sgroup.Sgroup;
 import org.openscience.cdk.sgroup.SgroupKey;
 import org.openscience.cdk.sgroup.SgroupType;
 import org.openscience.cdk.smiles.CxSmilesState.CxDataSgroup;
 import org.openscience.cdk.smiles.CxSmilesState.CxPolymerSgroup;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
 import org.openscience.cdk.tools.manipulator.ReactionManipulator;
 import uk.ac.ebi.beam.Graph;
 
 import javax.vecmath.Point2d;
 import javax.vecmath.Point3d;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 /**
  * Read molecules and reactions from a SMILES {@cdk.cite SMILESTUT} string.
  *
  * <b>Example usage</b>
  *
  * <blockquote><pre>
  * try {
  *     SmilesParser   sp  = new SmilesParser(SilentChemObjectBuilder.getInstance());
  *     IAtomContainer m   = sp.parseSmiles("c1ccccc1");
  * } catch (InvalidSmilesException e) {
  *     System.err.println(e.getMessage());
  * }
  * </pre>
  * </blockquote>
  *
  * <b>Reading Aromatic SMILES</b>
  * <p>
  * Aromatic SMILES are automatically kekulised producing a structure with
  * assigned bond orders. The aromatic specification on the atoms is maintained
  * from the SMILES even if the structures are not considered aromatic. For
  * example 'c1ccc1' will correctly have two pi bonds assigned but the
  * atoms/bonds will still be flagged as aromatic. Recomputing or clearing the
  * aromaticty will remove these erroneous flags. If a kekulé structure could not
  * be assigned this is considered an error. The most common example is the
  * omission of hydrogens on aromatic nitrogens (aromatic pyrrole is specified as
  * '[nH]1cccc1' not 'n1cccc1'). These structures can not be corrected without
  * modifying their formula. If there are multiple locations a hydrogen could be
  * placed the returned structure would differ depending on the atom input order.
  * If you wish to skip the kekulistation (not recommended) then it can be
  * disabled with {@link #kekulise}. SMILES can be verified for validity with the
  * <a href="http://www.daylight.com/daycgi/depict">DEPICT</a> service.
  *
  * <b>Unsupported Features</b>
  * <p>
  * The following features are not supported by this parser. <ul> <li>variable
  * order of bracket atom attributes, '[C-H]', '[CH@]' are considered invalid.
  * The predefined order required by this parser follows the <a
  * href="http://www.opensmiles.org/opensmiles.html">OpenSMILES</a> specification
  * of 'isotope', 'symbol', 'chiral', 'hydrogens', 'charge', 'atom class'</li>
  * <li>atom class indication - <i>this information is loaded but not annotated
  * on the structure</i> </li> <li>extended tetrahedral stereochemistry
  * (cumulated double bonds)</li> <li>trigonal bipyramidal stereochemistry</li>
  * <li>octahedral stereochemistry</li> </ul>
  *
  * <b>Atom Class</b>
  * <p>
  * The atom class is stored as the {@link org.openscience.cdk.CDKConstants#ATOM_ATOM_MAPPING}
  * property.
  *
  * <blockquote><pre>
  *
  * SmilesParser   sp  = new SmilesParser(SilentChemObjectBuilder.getInstance());
  * IAtomContainer m   = sp.parseSmiles("c1[cH:5]cccc1");
  * Integer        c1  = m.getAtom(1)
  *                       .getProperty(CDKConstants.ATOM_ATOM_MAPPING); // 5
  * Integer        c2  = m.getAtom(2)
  *                       .getProperty(CDKConstants.ATOM_ATOM_MAPPING); // null
  *
  * </pre>
  * </blockquote>
  *
  * @author Christoph Steinbeck
  * @author Egon Willighagen
  * @author John May
  * @cdk.module smiles
  * @cdk.githash
  * @cdk.created 2002-04-29
  * @cdk.keyword SMILES, parser
  */
 public final class SmilesParser {
 
     private ILoggingTool logger = LoggingToolFactory.createLoggingTool(SmilesParser.class);
 
     /**
      * The builder determines which CDK domain objects to create.
      */
     private final IChemObjectBuilder builder;
 
     /**
      * Direct converter from Beam to CDK.
      */
     private final BeamToCDK beamToCDK;
 
     /**
      * Kekulise the molecule on load. Generally this is a good idea as a
      * lower-case symbols in a SMILES do not really mean 'aromatic' but rather
      * 'conjugated'. Loading with kekulise 'on' will automatically assign
      * bond orders (if possible) using an efficient algorithm from the
      * underlying Beam library (soon to be added to CDK).
      */
     private boolean kekulise = true;
 
     /**
      * Whether the parser is in strict mode or not.
      */
     private boolean strict = false;
 
     /**
      * Create a new SMILES parser which will create {@link IAtomContainer}s with
      * the specified builder.
      *
      * @param builder used to create the CDK domain objects
      */
     public SmilesParser(final IChemObjectBuilder builder) {
         this.builder = builder;
         this.beamToCDK = new BeamToCDK(builder);
     }
 
     /**
      * Sets whether the parser is in strict mode. In non-strict mode (default)
      * recoverable issues with SMILES are reported as warnings.
      *
      * @param strict strict mode true/false.
      */
     public void setStrict(boolean strict) {
         this.strict = strict;
     }
 
     /**
      * Parse a reaction SMILES.
      *
      * @param smiles The SMILES string to parse
      * @return An instance of {@link org.openscience.cdk.interfaces.IReaction}
      * @throws InvalidSmilesException if the string cannot be parsed
      * @see #parseSmiles(String)
      */
     public IReaction parseReactionSmiles(String smiles) throws InvalidSmilesException {
 
         if (!smiles.contains(">"))
             throw new InvalidSmilesException("Not a reaction SMILES: " + smiles);
 
         final int first = smiles.indexOf('>');
         final int second = smiles.indexOf('>', first + 1);
 
         if (second < 0)
             throw new InvalidSmilesException("Invalid reaction SMILES:" + smiles);
 
         final String reactants = smiles.substring(0, first);
         final String agents = smiles.substring(first + 1, second);
         final String products = smiles.substring(second + 1, smiles.length());
 
         IReaction reaction = builder.newInstance(IReaction.class);
 
         // add reactants
         if (!reactants.isEmpty()) {
             IAtomContainer reactantContainer = parseSmiles(reactants, true);
             IAtomContainerSet reactantSet = ConnectivityChecker.partitionIntoMolecules(reactantContainer);
             for (int i = 0; i < reactantSet.getAtomContainerCount(); i++) {
                 reaction.addReactant(reactantSet.getAtomContainer(i));
             }
         }
 
         // add agents
         if (!agents.isEmpty()) {
             IAtomContainer agentContainer = parseSmiles(agents, true);
             IAtomContainerSet agentSet = ConnectivityChecker.partitionIntoMolecules(agentContainer);
             for (int i = 0; i < agentSet.getAtomContainerCount(); i++) {
                 reaction.addAgent(agentSet.getAtomContainer(i));
             }
         }
 
         String title = null;
 
         // add products
         if (!products.isEmpty()) {
             IAtomContainer productContainer = parseSmiles(products, true);
             IAtomContainerSet productSet = ConnectivityChecker.partitionIntoMolecules(productContainer);
             for (int i = 0; i < productSet.getAtomContainerCount(); i++) {
                 reaction.addProduct(productSet.getAtomContainer(i));
             }
             reaction.setProperty(CDKConstants.TITLE, title = productContainer.getProperty(CDKConstants.TITLE));
         }
 
         try {
             // CXSMILES layer
             parseRxnCXSMILES(title, reaction);
         } catch (Exception e) {
             e.printStackTrace();
             throw new InvalidSmilesException("Error parsing CXSMILES:" + e.getMessage());
         }
 
         return reaction;
     }
 
     /**
      * Parses a SMILES string and returns a structure ({@link IAtomContainer}).
      *
      * @param smiles A SMILES string
      * @return A structure representing the provided SMILES
      * @throws InvalidSmilesException thrown when the SMILES string is invalid
      */
     public IAtomContainer parseSmiles(String smiles) throws InvalidSmilesException {
         return parseSmiles(smiles, false);
     }
 
     private IAtomContainer parseSmiles(String smiles, boolean isRxnPart) throws InvalidSmilesException {
         try {
             // create the Beam object from parsing the SMILES
             Set<String> warnings = new HashSet<>();
             Graph g = Graph.parse(smiles, strict, warnings);
             for (String warning : warnings)
                 logger.warn(warning);
 
             // convert the Beam object model to the CDK - note exception thrown
             // if a kekule structure could not be assigned.
             IAtomContainer mol = beamToCDK.toAtomContainer(kekulise ? g.kekule() : g,
                     kekulise);
 
             if (!isRxnPart) {
                 try {
                     // CXSMILES layer
                     parseMolCXSMILES(g.getTitle(), mol);
                 } catch (Exception e) {
                     e.printStackTrace();
                     throw new InvalidSmilesException("Error parsing CXSMILES:" + e.getMessage());
                 }
             }
 
             return mol;
         } catch (IOException e) {
             throw new InvalidSmilesException("could not parse '" + smiles + "', " + e.getMessage());
         } catch (Exception e) {
             throw new InvalidSmilesException("could not parse '" + smiles + "'");
         }
     }
 
     /**
      * Safely parses an integer from a string and will not fail if a number is missing.
      *
      * @param val value
      * @return the integer value
      */
     private int parseIntSafe(String val) {
         try {
             return Integer.parseInt(val);
         } catch (NumberFormatException e) {
             return 0;
         }
     }
 
 
/** Parses CXSMILES layer and set attributes for atoms and bonds on the provided molecule. */
 private void parseMolCXSMILES(String title, IAtomContainer mol) throws InvalidSmilesException{
        if (title == null)
            return;
    
        // parse the title
        String[] tokens = title.split("\\s+");
        if (tokens.length < 2)
            throw new InvalidSmilesException("Invalid CXSMILES title: " + title);
    
        // parse the atom attributes
        for (int i = 1; i < tokens.length; i++) {
            String[] attr = tokens[i].split(":");
            if (attr.length != 2)
                throw new InvalidSmilesException("Invalid CXSMILES atom attribute: " + tokens[i]);
    
            int atomIdx = parseIntSafe(attr[0]);
            if (atomIdx < 1 || atomIdx > mol.getAtomCount())
                throw new InvalidSmilesException("Invalid CXSMILES atom index: " + atomIdx);
    
            String[] attrVal = attr[1].split(",");
            if (attrVal.length != 3)
                throw new InvalidSmilesException("Invalid CXSMILES atom attribute value: " + attr[1]);
    
            int atomCharge = parseIntSafe(attrVal[0]);
            int atomRadical = parseIntSafe(attrVal[1]);
            int atomMultiplicity = parseIntSafe(attrVal[2]);
    
            mol.getAtom(atomIdx - 1).setProperty(CDKConstants.ATOM_ATOMIC_NUMBER, atomCharge);
            mol.getAtom(atomIdx - 1).setProperty(CDKConstants.ATOM_RADICAL, atomRadical);
            mol.getAtom(atomIdx - 1).setProperty(CDKConstants.ATOM_MULTIPLICITY, atomMultiplicity);
        }
    
        // parse the bond attributes
        for (int i = 1; i < tokens.length; i++) {
            String[] attr = tokens[i].split(":");
            if (attr.length != 2)
                throw new InvalidSmilesException("Invalid CXSMILES bond attribute: " + tokens[i]);      
 }

 

}