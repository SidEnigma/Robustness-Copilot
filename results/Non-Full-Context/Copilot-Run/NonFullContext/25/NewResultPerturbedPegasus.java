/* Copyright (C) 2011 Mark Rijnbeek <markr@ebi.ac.uk>
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
 package org.openscience.cdk.tautomers;
 
 import java.util.*;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.graph.invariant.InChINumbersTools;
 import org.openscience.cdk.inchi.InChIGenerator;
 import org.openscience.cdk.inchi.InChIGeneratorFactory;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomType;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.isomorphism.AtomMatcher;
 import org.openscience.cdk.isomorphism.BondMatcher;
 import org.openscience.cdk.smiles.SmiFlavor;
 import org.openscience.cdk.smiles.SmilesGenerator;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
 
 /**
  * Creates tautomers for a given input molecule, based on the mobile H atoms listed in the InChI.
  * Algorithm described in {@cdk.cite Thalheim2010}.
  * <br>
  * <b>Provide your input molecules in Kekule form, and make sure atom type are perceived.</b><br>
  * When creating an input molecule by reading an MDL file, make sure to set implicit hydrogens. See the
  * InChITautomerGeneratorTest test case.
  *
  * @author Mark Rijnbeek
  * @cdk.module tautomer
  * @cdk.githash
  */
 public final class InChITautomerGenerator {
 
     private final static ILoggingTool LOGGER = LoggingToolFactory.createLoggingTool(InChITautomerGenerator.class);
 
     private static final SmilesGenerator CANSMI = new SmilesGenerator(SmiFlavor.Canonical);
 
     /** Generate InChI with -KET (keto-enol tautomers) option. */
     public static final int KETO_ENOL      = 0x1;
 
     /** Generate InChI with -15T (1,5-shift tautomers) option. */
     public static final int ONE_FIVE_SHIFT = 0x2;
 
     private final int flags;
 
     /**
      * Create a tautomer generator specifygin whether to enable, keto-enol (-KET) and 1,5-shifts (-15T).
      *
      * <pre>{@code
      * // enabled -KET option
      * InChITautomerGenerator tautgen = new InChITautomerGenerator(InChITautomerGenerator.KETO_ENOL);
      * // enabled both -KET and -15T
      * InChITautomerGenerator tautgen = new InChITautomerGenerator(InChITautomerGenerator.KETO_ENOL | InChITautomerGenerator.ONE_FIVE_SHIFT);
      * }</pre>
      *
      * @param flags the options
      */
     public InChITautomerGenerator(int flags) {
         this.flags = flags;
     }
 
     /**
      * Create a tautomer generator, keto-enol (-KET) and 1,5-shifts (-15T) are disabled.
      */
     public InChITautomerGenerator() {
         this(0);
     }
 
     /**
      * Public method to get tautomers for an input molecule, based on the InChI which will be calculated by JNI-InChI.
      * @param mol molecule for which to generate tautomers
      * @return a list of tautomers, if any
      * @throws CDKException
      * @throws CloneNotSupportedException
      */
     public List<IAtomContainer> getTautomers(IAtomContainer mol) throws CDKException, CloneNotSupportedException {
 
         String opt = "";
         if ((flags & KETO_ENOL) != 0)
             opt += " -KET";
         if ((flags & ONE_FIVE_SHIFT) != 0)
             opt += " -15T";
 
         InChIGenerator gen   = InChIGeneratorFactory.getInstance().getInChIGenerator(mol, opt);
         String         inchi = gen.getInchi();
         String         aux   = gen.getAuxInfo();
 
         long[] amap = new long[mol.getAtomCount()];
         InChINumbersTools.parseAuxInfo(aux, amap);
 
         if (inchi == null)
             throw new CDKException(InChIGenerator.class
                     + " failed to create an InChI for the provided molecule, InChI -> null.");
         return getTautomers(mol, inchi, amap);
     }
 
     /**
      * This method is slower than recalculating the InChI with {@link #getTautomers(IAtomContainer)} as the mapping
      * between the two can be found more efficiently.
      *
      * @param mol
      * @param inchi
      * @return
      * @throws CDKException
      * @throws CloneNotSupportedException
      * @deprecated use {@link #getTautomers(IAtomContainer)} directly
      */
     @Deprecated
     public List<IAtomContainer> getTautomers(IAtomContainer mol, String inchi) throws CDKException, CloneNotSupportedException {
         return getTautomers(mol, inchi, null);
     }
 
     /**
      * Overloaded {@link #getTautomers(IAtomContainer)} to get tautomers for an input molecule with the InChI already
      * provided as input argument.
      *
      * @param mol   and input molecule for which to generate tautomers
      * @param inchi InChI for the input molecule
      * @param amap  ordering of the molecules atoms in the InChI
      * @return a list of tautomers
      * @throws CDKException
      * @throws CloneNotSupportedException no longer thrown
      */
     private List<IAtomContainer> getTautomers(IAtomContainer mol, String inchi, long[] amap) throws CDKException, CloneNotSupportedException {
 
         //Initial checks
         if (mol == null || inchi == null)
             throw new CDKException("Please provide a valid input molecule and its corresponding InChI value.");
 
         // shallow copy since we will suppress hydrogens
         mol = mol.getBuilder().newInstance(IAtomContainer.class, mol);
         for (IAtom atom : mol.atoms()) {
             if (atom.getImplicitHydrogenCount() == null)
                 atom.setImplicitHydrogenCount(0);
         }
 
         List<IAtomContainer> tautomers = new ArrayList<IAtomContainer>();
         if (!inchi.contains("(H")) { //No mobile H atoms according to InChI, so bail out.
             tautomers.add(mol);
             return tautomers;
         }
 
         //Preparation: translate the InChi
         Map<Integer, IAtom> inchiAtomsByPosition = getElementsByPosition(inchi, mol);
         IAtomContainer inchiMolGraph = connectAtoms(inchi, mol, inchiAtomsByPosition);
 
         if (amap != null && amap.length == mol.getAtomCount()) {
             for (int i = 0; i < amap.length; i++) {
                 mol.getAtom(i)
                    .setID(Long.toString(amap[i]));
             }
             mol = AtomContainerManipulator.suppressHydrogens(mol);
         } else {
             mol = AtomContainerManipulator.suppressHydrogens(mol);
             mapInputMoleculeToInchiMolgraph(inchiMolGraph, mol);
         }
 
         List<Integer> mobHydrAttachPositions = new ArrayList<Integer>();
         int totalMobHydrCount = parseMobileHydrogens(mobHydrAttachPositions, inchi);
 
         tautomers = constructTautomers(mol, mobHydrAttachPositions, totalMobHydrCount);
         //Remove duplicates
         return removeDuplicates(tautomers);
     }
 
     /**
      * Parses the InChI's formula (ignoring hydrogen) and returns a map
      * with with a position for each atom, increasing in the order
      * of the elements as listed in the formula.
      * @param inputInchi user input InChI
      * @param inputMolecule user input molecule
      * @return <Integer,IAtom> map indicating position and atom
      */
     private Map<Integer, IAtom> getElementsByPosition(String inputInchi, IAtomContainer inputMolecule)
             throws CDKException {
         Map<Integer, IAtom> inchiAtomsByPosition = new HashMap<Integer, IAtom>();
         int position = 0;
         String inchi = inputInchi;
 
         inchi = inchi.substring(inchi.indexOf('/') + 1);
         String formula = inchi.substring(0, inchi.indexOf('/'));
 
         /*
          * Test for dots in the formula. For now, bail out when encountered; it
          * would require more sophisticated InChI connection table parsing.
          * Example: what happened to the platinum connectivity below?
          * N.N.O=C1O[Pt]OC(=O)C12CCC2<br>
          * InChI=1S/C6H8O4.2H3N.Pt/c7-4(8)6(5(9)10
          * )2-1-3-6;;;/h1-3H2,(H,7,8)(H,9,10);2*1H3;/q;;;+2/p-2
          */
         if (formula.contains("."))
             throw new CDKException("Cannot parse InChI, formula contains dot (unsupported feature). Input formula="
                     + formula);
 
         Pattern formulaPattern = Pattern.compile("\\.?[0-9]*[A-Z]{1}[a-z]?[0-9]*");
         Matcher match = formulaPattern.matcher(formula);
         while (match.find()) {
             String symbolAndCount = match.group();
             String elementSymbol = (symbolAndCount.split("[0-9]"))[0];
             if (!elementSymbol.equals("H")) {
                 int elementCnt = 1;
                 if (!(elementSymbol.length() == symbolAndCount.length())) {
                     elementCnt = Integer.valueOf(symbolAndCount.substring(elementSymbol.length()));
                 }
 
                 for (int i = 0; i < elementCnt; i++) {
                     position++;
                     IAtom atom = inputMolecule.getBuilder().newInstance(IAtom.class, elementSymbol);
                     /*
                      * This class uses the atom's ID attribute to keep track of
                      * atom positions defined in the InChi. So if for example
                      * atom.ID=14, it means this atom has position 14 in the
                      * InChI connection table.
                      */
                     atom.setID(position + "");
                     inchiAtomsByPosition.put(position, atom);
                 }
             }
         }
         return inchiAtomsByPosition;
     }
 
     /**
      * Pops and pushes its ways through the InChI connection table to build up a simple molecule.
      * @param inputInchi user input InChI
      * @param inputMolecule user input molecule
      * @param inchiAtomsByPosition
      * @return molecule with single bonds and no hydrogens.
      */
     private IAtomContainer connectAtoms(String inputInchi, IAtomContainer inputMolecule,
             Map<Integer, IAtom> inchiAtomsByPosition) throws CDKException {
         String inchi = inputInchi;
         inchi = inchi.substring(inchi.indexOf('/') + 1);
         inchi = inchi.substring(inchi.indexOf('/') + 1);
         String connections = inchi.substring(1, inchi.indexOf('/'));
         Pattern connectionPattern = Pattern.compile("(-|\\(|\\)|,|([0-9])*)");
         Matcher match = connectionPattern.matcher(connections);
         Stack<IAtom> atomStack = new Stack<IAtom>();
         IAtomContainer inchiMolGraph = inputMolecule.getBuilder().newInstance(IAtomContainer.class);
         boolean pop = false;
         boolean push = true;
         while (match.find()) {
             String group = match.group();
             push = true;
             if (!group.isEmpty()) {
                 if (group.matches("[0-9]*")) {
                     IAtom atom = inchiAtomsByPosition.get(Integer.valueOf(group));
                     if (!inchiMolGraph.contains(atom)) inchiMolGraph.addAtom(atom);
                     IAtom prevAtom = null;
                     if (atomStack.size() != 0) {
                         if (pop) {
                             prevAtom = atomStack.pop();
                         } else {
                             prevAtom = atomStack.get(atomStack.size() - 1);
                         }
                         IBond bond = inputMolecule.getBuilder().newInstance(IBond.class, prevAtom, atom,
                                 IBond.Order.SINGLE);
                         inchiMolGraph.addBond(bond);
                     }
                     if (push) {
                         atomStack.push(atom);
                     }
                 } else if (group.equals("-")) {
                     pop = true;
                     push = true;
                 } else if (group.equals(",")) {
                     atomStack.pop();
                     pop = false;
                     push = false;
                 } else if (group.equals("(")) {
                     pop = false;
                     push = true;
                 } else if (group.equals(")")) {
                     atomStack.pop();
                     pop = true;
                     push = true;
                 } else {
                     throw new CDKException("Unexpected token " + group + " in connection table encountered.");
                 }
             }
         }
         //put any unconnected atoms in the output as well
         for (IAtom at : inchiAtomsByPosition.values()) {
             if (!inchiMolGraph.contains(at)) inchiMolGraph.addAtom(at);
         }
         return inchiMolGraph;
     }
 
     /**
      * Atom-atom mapping of the input molecule to the bare container constructed from the InChI connection table.
      * This makes it possible to map the positions of the mobile hydrogens in the InChI back to the input molecule.
      * @param inchiMolGraph molecule (bare) as defined in InChI
      * @param mol user input molecule
      * @throws CDKException
      */
     private void mapInputMoleculeToInchiMolgraph(IAtomContainer inchiMolGraph, IAtomContainer mol) throws CDKException {
         Iterator<Map<IAtom, IAtom>> iter = org.openscience.cdk.isomorphism.VentoFoggia.findIdentical(inchiMolGraph,
                                                                                                      AtomMatcher.forElement(),
                                                                                                      BondMatcher.forAny())
                                                                                   .matchAll(mol)
                                                                                   .limit(1)
                                                                                   .toAtomMap()
                                                                                   .iterator();
         if (iter.hasNext()) {
             for (Map.Entry<IAtom,IAtom> e : iter.next().entrySet()) {
                 IAtom src = e.getKey();
                 IAtom dst = e.getValue();
                 String position = src.getID();
                 dst.setID(position);
                 LOGGER.debug("Mapped InChI ", src.getSymbol(), " ", src.getID(), " to ", dst.getSymbol(),
                              " " + dst.getID());
             }
         } else {
             throw new IllegalArgumentException(CANSMI.create(inchiMolGraph) + " " + CANSMI.create(mol));
         }
     }
 
 
/** Parses mobile H group in an InChI String. */

private int parseMobileHydrogens(List<Integer> mobHydrAttachPositions, String inputInchi) {
    int totalMobHydrCount = 0;
    String inchi = inputInchi;
    inchi = inchi.substring(inchi.indexOf('/') + 1);
    inchi = inchi.substring(inchi.indexOf('/') + 1);
    String mobileHydrogens = inchi.substring(inchi.indexOf('/') + 1);
    Pattern mobHydrPattern = Pattern.compile("([0-9]*)(H)");
    Matcher match = mobHydrPattern.matcher(mobileHydrogens);
    while (match.find()) {
        String count = match.group(1);
        String positions = match.group(2);
        int mobHydrCount = 1;
        if (!count.isEmpty()) {
            mobHydrCount = Integer.valueOf(count);
        }
        totalMobHydrCount += mobHydrCount;
        for (int i = 0; i < mobHydrCount; i++) {
            mobHydrAttachPositions.add(Integer.valueOf(positions));
        }
    }
    return totalMobHydrCount;
}
 

}