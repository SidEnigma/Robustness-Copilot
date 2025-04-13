/* Copyright (C) 1997-2007  Christoph Steinbeck <steinbeck@users.sf.net>
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
  *
  */
 package org.openscience.cdk.layout;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.config.Elements;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.geometry.GeometryUtil;
 import org.openscience.cdk.graph.ConnectedComponents;
 import org.openscience.cdk.graph.ConnectivityChecker;
 import org.openscience.cdk.graph.Cycles;
 import org.openscience.cdk.graph.GraphUtil;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomContainerSet;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.interfaces.IPseudoAtom;
 import org.openscience.cdk.interfaces.IReaction;
 import org.openscience.cdk.interfaces.IRing;
 import org.openscience.cdk.interfaces.IRingSet;
 import org.openscience.cdk.interfaces.IStereoElement;
 import org.openscience.cdk.isomorphism.AtomMatcher;
 import org.openscience.cdk.isomorphism.BondMatcher;
 import org.openscience.cdk.isomorphism.Pattern;
 import org.openscience.cdk.isomorphism.VentoFoggia;
 import org.openscience.cdk.ringsearch.RingPartitioner;
 import org.openscience.cdk.sgroup.Sgroup;
 import org.openscience.cdk.sgroup.SgroupBracket;
 import org.openscience.cdk.sgroup.SgroupKey;
 import org.openscience.cdk.sgroup.SgroupType;
 import org.openscience.cdk.stereo.DoubleBondStereochemistry;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
 import org.openscience.cdk.tools.manipulator.ReactionManipulator;
 import org.openscience.cdk.tools.manipulator.RingSetManipulator;
 
 import javax.vecmath.Point2d;
 import javax.vecmath.Vector2d;
 import java.util.AbstractMap.SimpleImmutableEntry;
 import java.util.ArrayDeque;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.Deque;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.LinkedHashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.stream.StreamSupport;
 
 import static java.util.Comparator.comparingInt;
 
 /**
  * Generates 2D coordinates for a molecule.
  *
  * <b>Basic Usage:</b>
  * If you just want to generate coordinate for a molecule (or reaction) you
  * can use the following one-liner:
  * <pre>
  * new StructureDiagramGenerator().generateCoordinates(molecule);
  * </pre>
  * The older versions of the API suggested using the following a
  * set/generate/get idiom but this performs an unnecessary (in most cases) copy.
  * <pre>
  * StructureDiagramGenerator sdg = new StructureDiagramGenerator();
  * sdg.setMolecule(molecule); // cloned!
  * sdg.generateCoordinates();
  * molecule = sdg.getMolecule();
  * </pre>
  * This idiom only needs to be used when 'fixing' parts of an existing
  * layout with {@link #setMolecule(IAtomContainer, boolean, Set, Set)}
  * <br/>
  *
  * @author steinbeck
  * @cdk.created 2004-02-02
  * @cdk.keyword Layout
  * @cdk.keyword Structure Diagram Generation (SDG)
  * @cdk.keyword 2D-coordinates
  * @cdk.keyword Coordinate generation, 2D
  * @cdk.dictref blue-obelisk:layoutMolecule
  * @cdk.module sdg
  * @cdk.githash
  * @cdk.bug 1536561
  * @cdk.bug 1788686
  */
 public class StructureDiagramGenerator {
 
     static final double DEFAULT_BOND_LENGTH           = 1.5;
     static final double SGROUP_BRACKET_PADDING_FACTOR = 0.5;
     private static final Vector2d                   DEFAULT_BOND_VECTOR      = new Vector2d(0, 1);
     private static final IdentityTemplateLibrary    DEFAULT_TEMPLATE_LIBRARY = IdentityTemplateLibrary.loadFromResource("custom-templates.smi")
                                                                                                    .add(IdentityTemplateLibrary.loadFromResource("chebi-ring-templates.smi"));
     private static final double                     RAD_30                   = Math.toRadians(-30);
     private static final ILoggingTool               logger                   = LoggingToolFactory.createLoggingTool(StructureDiagramGenerator.class);
 
     public static final Comparator<IAtomContainer> LARGEST_FIRST_COMPARATOR = new Comparator<IAtomContainer>() {
         @Override
         public int compare(IAtomContainer o1, IAtomContainer o2) {
             return Integer.compare(o2.getBondCount(), o1.getBondCount());
         }
     };
 
     private IAtomContainer molecule;
     private IRingSet       sssr;
     private final double bondLength = DEFAULT_BOND_LENGTH;
     private Vector2d firstBondVector;
     private RingPlacer       ringPlacer          = new RingPlacer();
     private AtomPlacer       atomPlacer          = new AtomPlacer();
     private MacroCycleLayout macroPlacer         = null;
     private List<IRingSet>   ringSystems         = null;
     private Set<IAtom>       afix                = null;
     private Set<IBond>       bfix                = null;
     private boolean          useIdentTemplates   = true;
     private boolean          alignMappedReaction = true;
 
     // show we orient the structure (false: keep de facto ring systems drawn
     // the right way up)
     private boolean selectOrientation = true;
 
     /**
      * Identity templates - for laying out primary ring system.
      */
     private IdentityTemplateLibrary identityLibrary;
 
 
 
 
     /**
      * The empty constructor.
      */
     public StructureDiagramGenerator() {
         this(DEFAULT_TEMPLATE_LIBRARY);
     }
 
     private StructureDiagramGenerator(IdentityTemplateLibrary identityLibrary) {
         this.identityLibrary = identityLibrary;
     }
 
     /**
      * Creates an instance of this class while assigning a molecule to be layed
      * out.
      *
      * @param molecule The molecule to be layed out.
      */
     public StructureDiagramGenerator(IAtomContainer molecule) {
         this();
         setMolecule(molecule, false);
     }
 
     /**
      * <p>Convenience method for generating 2D coordinates.</p>
      *
      * <p>The method is short-hand for calling:</p>
      * <pre>
      * sdg.setMolecule(mol, false);
      * sdg.generateCoordinates();
      * </pre>
      *
      * @param mol molecule to layout
      * @throws CDKException problem with layout
      */
     public final void generateCoordinates(IAtomContainer mol) throws CDKException {
         setMolecule(mol, false);
         generateCoordinates();
     }
 
     /**
      * <p>Convenience method to generate 2D coordinates for a reaction. If atom-atom
      * maps are present on a reaction, the substructures are automatically aligned.</p>
      * <p>This feature can be disabled by changing the {@link #setAlignMappedReaction(boolean)}</p>
      *
      * @param reaction reaction to layout
      * @throws CDKException problem with layout
      */
     public final void generateCoordinates(final IReaction reaction) throws CDKException {
 
         // layout products and agents
         for (IAtomContainer mol : reaction.getProducts().atomContainers())
             generateCoordinates(mol);
         for (IAtomContainer mol : reaction.getAgents().atomContainers())
             generateCoordinates(mol);
 
         // do not align = simple layout of reactants
         if (alignMappedReaction) {
             final Set<IBond> mapped = ReactionManipulator.findMappedBonds(reaction);
 
             Map<Integer, List<Map<Integer, IAtom>>> refmap = new HashMap<>();
 
             for (IAtomContainer mol : reaction.getProducts().atomContainers()) {
                 Cycles.markRingAtomsAndBonds(mol);
                 final ConnectedComponents cc = new ConnectedComponents(GraphUtil.toAdjListSubgraph(mol, mapped));
                 final IAtomContainerSet parts = ConnectivityChecker.partitionIntoMolecules(mol, cc.components());
                 for (IAtomContainer part : parts.atomContainers()) {
                     // skip single atoms (unmapped)
                     if (part.getAtomCount() == 1)
                         continue;
                     final Map<Integer, IAtom> map = new HashMap<>();
                     for (IAtom atom : part.atoms()) {
                         // safe as substructure should only be mapped bonds and therefore atoms!
                         int idx = atom.getProperty(CDKConstants.ATOM_ATOM_MAPPING);
                         if (map.put(idx, atom) == null)
                             refmap.computeIfAbsent(idx, k -> new ArrayList<>()).add(map);
                     }
                 }
             }
 
             Map<IAtom,IAtom> afix = new HashMap<>();
             Set<IBond>       bfix = new HashSet<>();
 
             for (IAtomContainer mol : reaction.getReactants().atomContainers()) {
                 Cycles.markRingAtomsAndBonds(mol);
                 final ConnectedComponents cc = new ConnectedComponents(GraphUtil.toAdjListSubgraph(mol, mapped));
                 final IAtomContainerSet parts = ConnectivityChecker.partitionIntoMolecules(mol, cc.components());
 
                 // we only aligned the largest part
                 IAtomContainer largest = null;
                 for (IAtomContainer part : parts.atomContainers()) {
                     if (largest == null || part.getBondCount() > largest.getBondCount())
                         largest = part;
                 }
 
                 afix.clear();
                 bfix.clear();
 
                 boolean aggresive = false;
 
                 if (largest != null && largest.getAtomCount() > 1) {
 
                     int idx = largest.getAtom(0).getProperty(CDKConstants.ATOM_ATOM_MAPPING);
 
                     // select the largest and use those coordinates
                     Map<Integer, IAtom> reference = select(refmap.getOrDefault(idx, Collections.emptyList()));
                     for (IAtom atom : largest.atoms()) {
                         idx = atom.getProperty(CDKConstants.ATOM_ATOM_MAPPING);
                         final IAtom src = reference.get(idx);
                         if (src == null) continue;
                         if (!aggresive) {
                             // no way to get the container of 'src' without
                             // lots of refactoring, instead we just use the
                             // new API points - first checking these will not
                             // fail
                             if (src.getContainer() != null
                                 && atom.getContainer() != null
                                 && AtomPlacer.isColinear(src, src.bonds())
                                    != AtomPlacer.isColinear(atom, atom.bonds()))
                                 continue;
                         }
                         atom.setPoint2d(new Point2d(src.getPoint2d()));
                         afix.put(atom, src);
                     }
                 }
 
                 if (!afix.isEmpty()) {
                     if (aggresive) {
                         for (IBond bond : mol.bonds()) {
                             if (afix.containsKey(bond.getBegin()) && afix.containsKey(bond.getEnd())) {
                                 // only fix acyclic bonds if the source atoms were also acyclic
                                 if (!bond.isInRing()) {
                                     IAtom srcBeg = afix.get(bond.getBegin());
                                     IAtom srcEnd = afix.get(bond.getEnd());
                                     for (IAtomContainer product : reaction.getProducts().atomContainers()) {
                                         IBond srcBond = product.getBond(srcBeg, srcEnd);
                                         if (srcBond != null) {
                                             if (!srcBond.isInRing())
                                                 bfix.add(bond); // safe to add
                                             break;
                                         }
                                     }
                                 } else {
                                     bfix.add(bond);
                                 }
                             }
                         }
                     } else {
                         for (IBond bond : mol.bonds()) {
                             if (afix.containsKey(bond.getBegin()) && afix.containsKey(bond.getEnd())) {
                                 // only fix bonds that match their ring membership status
                                 IAtom srcBeg = afix.get(bond.getBegin());
                                 IAtom srcEnd = afix.get(bond.getEnd());
                                 for (IAtomContainer product : reaction.getProducts().atomContainers()) {
                                     IBond srcBond = product.getBond(srcBeg, srcEnd);
                                     if (srcBond != null) {
                                         if (srcBond.isInRing() == bond.isInRing())
                                             bfix.add(bond);
                                         break;
                                     }
                                 }
                             }
                         }
 
                         afix.clear();
                         for (IBond bond : bfix) {
                             afix.put(bond.getBegin(), null);
                             afix.put(bond.getEnd(), null);
                         }
 
                         int[] parts2 = new int[mol.getAtomCount()];
                         int numParts = 0;
                         Deque<IAtom> queue = new ArrayDeque<>();
                         for (IAtom atom : afix.keySet()) {
                             if (parts2[mol.indexOf(atom)] != 0)
                                 continue;
                             parts2[mol.indexOf(atom)] = ++numParts;
                             for (IBond bond : mol.getConnectedBondsList(atom)) {
                                 if (bfix.contains(bond))
                                     queue.add(bond.getOther(atom));
                             }
                             while (!queue.isEmpty()) {
                                 atom = queue.poll();
                                 if (parts2[mol.indexOf(atom)] != 0)
                                     continue;
                                 parts2[mol.indexOf(atom)] = numParts;
                                 for (IBond bond : mol.getConnectedBondsList(atom)) {
                                     if (bfix.contains(bond))
                                         queue.add(bond.getOther(atom));
                                 }
                             }
                         }
 
                         if (numParts > 1) {
                             int best     = 0;
                             int bestSize = 0;
                             for (int part = 1; part <= numParts; part++) {
                                 int size = 0;
                                 for (int i = 0; i < parts2.length; i++) {
                                     if (parts2[i] == part)
                                         ++size;
                                 }
                                 if (size > bestSize) {
                                     bestSize = size;
                                     best = part;
                                 }
                             }
 
                             for (IAtom atom : new ArrayList<>(afix.keySet())) {
                                 if (parts2[mol.indexOf(atom)] != best) {
                                     afix.remove(atom);
                                     bfix.removeAll(mol.getConnectedBondsList(atom));
                                 }
                             }
                         }
                     }
                 }
 
                 setMolecule(mol, false, afix.keySet(), bfix);
                 generateCoordinates();
             }
 
             // reorder reactants such that they are in the same order they appear on the right
             reaction.getReactants().sortAtomContainers(new Comparator<IAtomContainer>() {
                 @Override
                 public int compare(IAtomContainer a, IAtomContainer b) {
                     Point2d aCenter = GeometryUtil.get2DCenter(a);
                     Point2d bCenter = GeometryUtil.get2DCenter(b);
                     if (aCenter == null || bCenter == null)
                         return 0;
                     else
                         return Double.compare(aCenter.x, bCenter.x);
                 }
             });
 
         } else {
             for (IAtomContainer mol : reaction.getReactants().atomContainers())
                 generateCoordinates(mol);
         }
     }
 
     private Map<Integer, IAtom> select(Collection<Map<Integer, IAtom>> refs) {
         Map<Integer, IAtom> largest = Collections.emptyMap();
         for (Map<Integer, IAtom> ref : refs) {
             if (ref.size() > largest.size())
                 largest = ref;
         }
         return largest;
     }
 
     public void setMolecule(IAtomContainer mol, boolean clone) {
         setMolecule(mol, clone, Collections.<IAtom>emptySet(), Collections.<IBond>emptySet());
     }
 
     /**
      * Assigns a molecule to be laid out. After, setting the molecule call generateCoordinates() to assign
      * 2D coordinates. An optional set of atoms/bonds can be parsed in to allow partial layout, these will
      * be 'fixed' in place. This only applies to non-cloned molecules, and only atoms with coordinates can
      * be fixed.
      *
      * @param mol   the molecule for which coordinates are to be generated.
      * @param clone Should the whole process be performed with a cloned copy?
      * @param afix  Atoms that should be fixed in place, coordinates are not changed.
      * @param bfix  Bonds that should be fixed in place, they will not be flipped, bent, or streched.
      */
     public void setMolecule(IAtomContainer mol, boolean clone, Set<IAtom> afix, Set<IBond> bfix) {
         if (clone) {
             if (!afix.isEmpty() || !bfix.isEmpty())
                 throw new IllegalArgumentException("Laying out a cloned molecule, can't fix atom or bonds.");
             try {
                 this.molecule = (IAtomContainer) mol.clone();
             } catch (CloneNotSupportedException e) {
                 logger.error("Should clone, but exception occurred: ", e.getMessage());
                 logger.debug(e);
             }
         } else {
             this.molecule = mol;
         }
         this.afix = afix;
         this.bfix = bfix;
         for (IAtom atom : molecule.atoms()) {
 
             boolean afixed = afix.contains(atom);
 
             if (afixed && atom.getPoint2d() == null) {
                 afixed = false;
                 afix.remove(atom);
             }
 
             if (afixed) {
                 atom.setFlag(CDKConstants.ISPLACED, true);
                 atom.setFlag(CDKConstants.VISITED, true);
             } else {
                 atom.setPoint2d(null);
                 atom.setFlag(CDKConstants.ISPLACED, false);
                 atom.setFlag(CDKConstants.VISITED, false);
                 atom.setFlag(CDKConstants.ISINRING, false);
                 atom.setFlag(CDKConstants.ISALIPHATIC, false);
             }
         }
         atomPlacer.setMolecule(this.molecule);
         ringPlacer.setMolecule(this.molecule);
         ringPlacer.setAtomPlacer(this.atomPlacer);
         macroPlacer = new MacroCycleLayout(mol);
         selectOrientation = afix.isEmpty();
     }
 
     /**
      * Sets whether to use templates or not. Some complicated ring systems
      * like adamantane are only nicely layouted when using templates. This
      * option is by default set true.
      *
      * @param useTemplates set true to use templates, false otherwise
      * @deprecated always false, substructure templates are not used anymore
      */
     @Deprecated
     public void setUseTemplates(boolean useTemplates) {
 
     }
 
     /**
      * Set whether identity templates are used. Identity templates use an exact match
      * are are very fast. They are used for layout of the 'primary' ring system
      * in de facto orientation.
      *
      * @param use whether to use identity templates
      */
     public void setUseIdentityTemplates(boolean use) {
         this.useIdentTemplates = use;
     }
 
     /**
      * Returns whether the use of templates is enabled or disabled.
      *
      * @return true, when the use of templates is enables, false otherwise
      * @deprecated always false, substructure templates are not used anymore
      */
     @Deprecated
     public boolean getUseTemplates() {
         return false;
     }
 
     /**
      * Sets the templateHandler attribute of the StructureDiagramGenerator object
      *
      * @param templateHandler The new templateHandler value
      * @deprecated substructure templates are no longer used for layout but those provided here
      * will be converted to identity templates
      */
     @Deprecated
     public void setTemplateHandler(TemplateHandler templateHandler) {
         IdentityTemplateLibrary lib = templateHandler.toIdentityTemplateLibrary();
         lib.add(identityLibrary);
         identityLibrary = lib; // new ones take priority
     }
 
     /**
      * Gets the templateHandler attribute of the StructureDiagramGenerator object
      *
      * @return The templateHandler value
      * @deprecated always null, substructure templates are not used anymore
      */
     @Deprecated
     public TemplateHandler getTemplateHandler() {
         return null;
     }
 
     /**
      * Assings a molecule to be layed out. Call generateCoordinates() to do the
      * actual layout.
      *
      * @param molecule the molecule for which coordinates are to be generated.
      */
     public void setMolecule(IAtomContainer molecule) {
         setMolecule(molecule, true);
     }
 
     /**
      * Set whether reaction reactants should be allignned to their product.
      *
      * @param align align setting
      */
     public void setAlignMappedReaction(boolean align) {
         this.alignMappedReaction = align;
     }
 
     /**
      * Returns the molecule, usually used after a call of generateCoordinates()
      *
      * @return The molecule with new coordinates (if generateCoordinates() had
      * been called)
      */
     public IAtomContainer getMolecule() {
         return molecule;
     }
 
     /**
      * This method uses generateCoordinates, but it removes the hydrogens first,
      * lays out the structure and then adds them again.
      *
      * @throws CDKException if an error occurs
      * @see #generateCoordinates
      * @deprecated use {@link #generateCoordinates()}
      */
     @Deprecated
     public void generateExperimentalCoordinates() throws CDKException {
         generateExperimentalCoordinates(DEFAULT_BOND_VECTOR);
     }
 
     /**
      * Generates 2D coordinates on the non-hydrogen skeleton, after which
      * coordinates for the hydrogens are calculated.
      *
      * @param firstBondVector the vector of the first bond to lay out
      * @throws CDKException if an error occurs
      * @deprecated use {@link #generateCoordinates()}
      */
     @Deprecated
     public void generateExperimentalCoordinates(Vector2d firstBondVector) throws CDKException {
         // first make a shallow copy: Atom/Bond references are kept
         IAtomContainer original = molecule;
         IAtomContainer shallowCopy = molecule.getBuilder().newInstance(IAtomContainer.class, molecule);
         // delete single-bonded H's from
         //IAtom[] atoms = shallowCopy.getAtoms();
         for (IAtom curAtom : shallowCopy.atoms()) {
             if (curAtom.getSymbol().equals("H")) {
                 if (shallowCopy.getConnectedBondsCount(curAtom) < 2) {
                     shallowCopy.removeAtom(curAtom);
                     curAtom.setPoint2d(null);
                 }
             }
         }
         // do layout on the shallow copy
         molecule = shallowCopy;
         generateCoordinates(firstBondVector);
         double bondLength = GeometryUtil.getBondLengthAverage(molecule);
         // ok, now create the coordinates for the hydrogens
         HydrogenPlacer hPlacer = new HydrogenPlacer();
         molecule = original;
         hPlacer.placeHydrogens2D(molecule, bondLength);
     }
 
     /**
      * The main method of this StructurDiagramGenerator. Assign a molecule to the
      * StructurDiagramGenerator, call the generateCoordinates() method and get
      * your molecule back.
      *
      * @param firstBondVector The vector of the first bond to lay out
      * @throws CDKException if an error occurs
      */
     public void generateCoordinates(Vector2d firstBondVector) throws CDKException {
         generateCoordinates(firstBondVector, false, false);
     }
 
     /**
      * The main method of this StructureDiagramGenerator. Assign a molecule to the
      * StructureDiagramGenerator, call the generateCoordinates() method and get
      * your molecule back.
      *
      * @param firstBondVector the vector of the first bond to lay out
      * @param isConnected     the 'molecule' attribute is guaranteed to be connected (we have checked)
      * @param isSubLayout     the 'molecule' is being laid out as part of a large collection of fragments
      * @throws CDKException problem occurred during layout
      */
     private void generateCoordinates(Vector2d firstBondVector, boolean isConnected, boolean isSubLayout) throws CDKException {
 
         // defensive copy, vectors are mutable!
         if (firstBondVector == DEFAULT_BOND_VECTOR)
             firstBondVector = new Vector2d(firstBondVector);
 
         final int numAtoms = molecule.getAtomCount();
         final int numBonds = molecule.getBondCount();
         this.firstBondVector = firstBondVector;
 
         // if molecule contains only one Atom, don't fail, simply set
         // coordinates to simplest: 0,0. See bug #780545
         logger.debug("Entry point of generateCoordinates()");
         logger.debug("We have a molecules with " + numAtoms + " atoms.");
         if (numAtoms == 0) {
             return;
         } 
         if (numAtoms == 1) {
             molecule.getAtom(0).setPoint2d(new Point2d(0, 0));
             return;
         } else if (molecule.getBondCount() == 1 && molecule.getAtomCount() == 2) {
             double xOffset = 0;
             for (IAtom atom : molecule.atoms()) {
                 atom.setPoint2d(new Point2d(xOffset, 0));
                 xOffset += bondLength;
             }
             return;
         }
 
         // intercept fragment molecules and lay them out in a grid
         if (!isConnected) {
             final IAtomContainerSet frags = ConnectivityChecker.partitionIntoMolecules(molecule);
             if (frags.getAtomContainerCount() > 1) {
                 IAtomContainer rollback = molecule;
 
                 // large => small (e.g. salt will appear on the right)
                 List<IAtomContainer> fragList = toList(frags);
                 Collections.sort(fragList, LARGEST_FIRST_COMPARATOR);
                 generateFragmentCoordinates(molecule, fragList);
 
                 // don't call set molecule as it wipes x,y coordinates!
                 // this looks like a self assignment but actually the fragment
                 // method changes this.molecule
                 this.molecule = rollback;
                 atomPlacer.setMolecule(this.molecule);
                 ringPlacer.setMolecule(this.molecule);
                 macroPlacer = new MacroCycleLayout(this.molecule);
                 return;
             }
         }
 
         // initial layout seeding either from a ring system of longest chain
         seedLayout();
 
         // Now, do the layout of the rest of the molecule
         int iter = 0;
         for (; !AtomPlacer.allPlaced(molecule) && iter < numAtoms; iter++) {
             logger.debug("*** Start of handling the rest of the molecule. ***");
             // layout for all acyclic parts of the molecule which are
             // connected to the parts which have already been laid out.
             layoutAcyclicParts();
             // layout cyclic parts of the molecule which
             // are connected to the parts which have already been laid out.
             layoutCyclicParts();
         }
 
         // display reasonable error on failed layout, otherwise we'll have a NPE somewhere
         if (iter == numAtoms && !AtomPlacer.allPlaced(molecule))
             throw new CDKException("Could not generate layout? If a set of 'fixed' atoms were provided"
                                        + " try removing these and regenerating the layout.");
 
         if (!isSubLayout) {
             // correct double-bond stereo, this changes the layout and in reality
             // should be done during the initial placement
             if (molecule.stereoElements().iterator().hasNext())
                 CorrectGeometricConfiguration.correct(molecule);
         }
 
         refinePlacement(molecule);
         finalizeLayout(molecule);
 
         // stereo must be after refinement (due to flipping!)
         if (!isSubLayout)
             assignStereochem(molecule);
 
     }
 
     /**
      * Determine if any atoms in a connected molecule are fixed (i.e. already have coordinates/
      * have been placed).
      *
      * @param mol the moleucle to check
      * @return atoms are fixed
      */
     private boolean hasFixedPart(final IAtomContainer mol) {
         if (afix.isEmpty()) return false;
         for (IAtom atom : mol.atoms())
             if (afix.contains(atom))
                 return true;
         return false;
     }
 
     private void seedLayout() throws CDKException {
 
         final int numAtoms = this.molecule.getAtomCount();
         final int numBonds = this.molecule.getBondCount();
         if (hasFixedPart(molecule)) {
 
             // no seeding needed as the molecule has atoms with coordinates, just calc rings if needed
             if (prepareRingSystems() > 0) {
                 for (IRingSet rset : ringSystems) {
                     if (rset.getFlag(CDKConstants.ISPLACED)) {
                         ringPlacer.placeRingSubstituents(rset, bondLength);
                     } else {
                         List<IRing> placed = new ArrayList<>();
                         List<IRing> unplaced = new ArrayList<>();
 
                         for (IAtomContainer ring : rset.atomContainers()) {
                             if (ring.getFlag(CDKConstants.ISPLACED))
                                 placed.add((IRing) ring);
                             else
                                 unplaced.add((IRing) ring);
                         }
 
                         // partially laid out rings
                         if (placed.isEmpty()) {
                             for (IRing ring : unplaced) {
                                 if (ringPlacer.completePartiallyPlacedRing(rset, ring, bondLength))
                                     placed.add(ring);
                             }
                             unplaced.removeAll(placed);
                         }
 
                         while (!unplaced.isEmpty() && !placed.isEmpty()) {
 
                             for (IAtomContainer ring : placed) {
                                 ringPlacer.placeConnectedRings(rset, (IRing) ring, RingPlacer.FUSED, bondLength);
                                 ringPlacer.placeConnectedRings(rset, (IRing) ring, RingPlacer.BRIDGED, bondLength);
                                 ringPlacer.placeConnectedRings(rset, (IRing) ring, RingPlacer.SPIRO, bondLength);
                             }
                             Iterator<IRing> unplacedIter = unplaced.iterator();
                             placed.clear();
                             while (unplacedIter.hasNext()) {
                                 IRing ring = unplacedIter.next();
                                 if (ring.getFlag(CDKConstants.ISPLACED)) {
                                     unplacedIter.remove();
                                     placed.add(ring);
                                 }
                             }
                         }
 
                         if (allPlaced(rset)) {
                             rset.setFlag(CDKConstants.ISPLACED, true);
                             ringPlacer.placeRingSubstituents(rset, bondLength);
                         }
                     }
                 }
             }
         } else if (prepareRingSystems() > 0) {
             logger.debug("*** Start of handling rings. ***");
             prepareRingSystems();
 
             // We got our ring systems now choose the best one based on size and
             // number of heteroatoms
             RingPlacer.countHetero(ringSystems);
             Collections.sort(ringSystems, RingPlacer.RING_COMPARATOR);
 
             int respect = layoutRingSet(firstBondVector, ringSystems.get(0));
 
             // rotate monocyclic and when >= 4 polycyclic
             if (respect == 1) {
                 if (ringSystems.get(0).getAtomContainerCount() == 1) {
                     respect = 0;
                 } else if (ringSystems.size() >= 4) {
                     int numPoly = 0;
                     for (IRingSet rset : ringSystems)
                         if (rset.getAtomContainerCount() > 1)
                             numPoly++;
                     if (numPoly >= 4)
                         respect = 0;
                 }
             }
 
             if (respect == 1 || respect == 2)
                 selectOrientation = false;
 
             logger.debug("First RingSet placed");
 
             // place of all the directly connected atoms of this ring system
             ringPlacer.placeRingSubstituents(ringSystems.get(0), bondLength);
         } else {
 
             logger.debug("*** Start of handling purely aliphatic molecules. ***");
 
             // We are here because there are no rings in the molecule so we get the longest chain in the molecule
             // and placed in on a horizontal axis
             logger.debug("Searching initialLongestChain for this purely aliphatic molecule");
             IAtomContainer longestChain = AtomPlacer.getInitialLongestChain(molecule);
             logger.debug("Found linear chain of length " + longestChain.getAtomCount());
             logger.debug("Setting coordinated of first atom to 0,0");
             longestChain.getAtom(0).setPoint2d(new Point2d(0, 0));
             longestChain.getAtom(0).setFlag(CDKConstants.ISPLACED, true);
 
             // place the first bond such that the whole chain will be horizontally alligned on the x axis
             logger.debug("Attempting to place the first bond such that the whole chain will be horizontally alligned on the x axis");
             if (firstBondVector != null && firstBondVector != DEFAULT_BOND_VECTOR)
                 atomPlacer.placeLinearChain(longestChain, firstBondVector, bondLength);
             else
                 atomPlacer.placeLinearChain(longestChain, new Vector2d(Math.cos(RAD_30), Math.sin(RAD_30)), bondLength);
             logger.debug("Placed longest aliphatic chain");
         }
     }
 
     private int prepareRingSystems() {
         final int numRings = Cycles.markRingAtomsAndBonds(molecule);
         // compute SSSR/MCB
         if (numRings > 0) {
             sssr = Cycles.sssr(molecule).toRingSet();
 
             if (sssr.getAtomContainerCount() < 1)
                 throw new IllegalStateException("Molecule expected to have rings, but had none?");
 
             // Give a handle of our molecule to the ringPlacer
             ringPlacer.checkAndMarkPlaced(sssr);
 
             // Partition the smallest set of smallest rings into disconnected
             // ring system. The RingPartioner returns a Vector containing
             // RingSets. Each of the RingSets contains rings that are connected
             // to each other either as bridged ringsystems, fused rings or via
             // spiro connections.
             ringSystems = RingPartitioner.partitionRings(sssr);
 
             // set the in-ring db stereo
             for (IStereoElement se : molecule.stereoElements()) {
                 if (se.getConfigClass() == IStereoElement.CisTrans) {
                     IBond stereoBond    = (IBond) se.getFocus();
                     IBond firstCarrier  = (IBond) se.getCarriers().get(0);
                     IBond secondCarrier = (IBond) se.getCarriers().get(1);
                     for (IRingSet ringSet : ringSystems) {
                         for (IAtomContainer ring : ringSet.atomContainers()) {
                             if (ring.contains(stereoBond)) {
                                 List<IBond> begBonds = ring.getConnectedBondsList(stereoBond.getBegin());
                                 List<IBond> endBonds = ring.getConnectedBondsList(stereoBond.getEnd());
                                 begBonds.remove(stereoBond);
                                 endBonds.remove(stereoBond);
                                 // something odd wrong, just skip it
                                 if (begBonds.size() != 1 || endBonds.size() != 1)
                                     continue;
                                 boolean flipped = begBonds.contains(firstCarrier) != endBonds.contains(secondCarrier);
                                 int cfg = flipped ? se.getConfigOrder() ^ 0x3 : se.getConfigOrder();
                                 ring.addStereoElement(new DoubleBondStereochemistry(stereoBond,
                                                                                     new IBond[]{begBonds.get(0), endBonds.get(0)},
                                                                                     cfg));
                             }
                         }
                     }
 
                 }
             }
         } else {
             sssr = molecule.getBuilder().newInstance(IRingSet.class);
             ringSystems = new ArrayList<>();
         }
         return numRings;
     }
 
     private void assignStereochem(IAtomContainer molecule) {
         // XXX: can't check this unless we store 'unspecified' double bonds
         // if (!molecule.stereoElements().iterator().hasNext())
         //     return;
 
         // assign up/down labels, this doesn't not alter layout and could be
         // done on-demand (e.g. when writing a MDL Molfile)
         NonplanarBonds.assign(molecule);
     }
 
     private void refinePlacement(IAtomContainer molecule) {
         AtomPlacer.prioritise(molecule);
 
         // refine the layout by rotating, bending, and stretching bonds
         LayoutRefiner refiner = new LayoutRefiner(molecule, afix, bfix);
         refiner.refine();
 
         // check for attachment points, these override the direction which we rorate structures
         IAtom begAttach = null;
         for (IAtom atom : molecule.atoms()) {
             if (atom instanceof IPseudoAtom && ((IPseudoAtom) atom).getAttachPointNum() == 1) {
                 begAttach = atom;
                 selectOrientation = true;
                 break;
             }
         }
 
         // choose the orientation in which to display the structure
         if (selectOrientation) {
             // no attachment point, rotate to maximise horizontal spread etc.
             if (begAttach == null) {
                 selectOrientation(molecule, DEFAULT_BOND_LENGTH, 1);
             }
             // use attachment point bond to rotate
             else {
                 final List<IBond> attachBonds = molecule.getConnectedBondsList(begAttach);
                 if (attachBonds.size() == 1) {
                     IAtom end = attachBonds.get(0).getOther(begAttach);
                     Point2d xyBeg = begAttach.getPoint2d();
                     Point2d xyEnd = end.getPoint2d();
 
                     // snap to horizontal '*-(end)-{rest of molecule}'
                     GeometryUtil.rotate(molecule,
                                         GeometryUtil.get2DCenter(molecule),
                                         -Math.atan2(xyEnd.y - xyBeg.y, xyEnd.x - xyBeg.x));
 
                     // put the larger part of the structure is above the bond so fragments are drawn
                     // semi-consistently
                     double ylo = 0;
                     double yhi = 0;
                     for (IAtom atom : molecule.atoms()) {
                         double yDelta = xyBeg.y - atom.getPoint2d().y;
                         if (yDelta > 0 && yDelta > yhi) {
                             yhi = yDelta;
                         } else if (yDelta < 0 && yDelta < ylo) {
                             ylo = yDelta;
                         }
                     }
 
                     // mirror points if larger part is below
                     if (Math.abs(ylo) < yhi)
                         for (IAtom atom : molecule.atoms())
                             atom.getPoint2d().y = -atom.getPoint2d().y;
 
                     // rotate pointing downwards 30-degrees
                     GeometryUtil.rotate(molecule,
                                         GeometryUtil.get2DCenter(molecule),
                                         -Math.toRadians(30));
                 }
             }
         }
     }
 
     /**
      * Finalize the molecule layout, primarily updating Sgroups.
      *
      * @param mol molecule being laid out
      */
     private void finalizeLayout(IAtomContainer mol) {
         placeMultipleGroups(mol);
         placePositionalVariation(mol);
         placeSgroupBrackets(mol);
     }
 
     /**
      * Calculates a histogram of bond directions, this allows us to select an
      * orientation that has bonds at nice angles (e.g. 60/120 deg). The limit
      * parameter is used to quantize the vectors within a range. For example
      * a limit of 60 will fill the histogram 0..59 and Bond's orientated at 0,
      * 60, 120 degrees will all be counted in the 0 bucket.
      *
      * @param mol molecule
      * @param counts the histogram is stored here, will be cleared
      * @param lim wrap angles to the (180 max)
      * @return number of aligned bonds
      */
     private static void calcDirectionHistogram(IAtomContainer mol,
                                                int[] counts,
                                                int lim) {
         if (lim > 180)
             throw new IllegalArgumentException("limit must be ≤ 180");
         Arrays.fill(counts, 0);
         for (IBond bond : mol.bonds()) {
             Point2d beg = bond.getBegin().getPoint2d();
             Point2d end = bond.getEnd().getPoint2d();
             Vector2d vec = new Vector2d(end.x - beg.x, end.y - beg.y);
             if (vec.x < 0)
                 vec.negate();
             double angle = Math.PI/2 + Math.atan2(vec.y, vec.x);
             counts[(int)(Math.round(Math.toDegrees(angle))%lim)]++;
         }
     }
 
     /**
      * Select the global orientation of the layout. We click round at 30 degree increments
      * and select the orientation that a) is the widest or b) has the most bonds aligned to
      * +/- 30 degrees {@cdk.cite Clark06}.
      *
      * @param mol       molecule
      * @param widthDiff parameter at which to consider orientations equally good (wide select)
      * @param alignDiff parameter at which we consider orientations equally good (bond align select)
      */
     private void selectOrientation(IAtomContainer mol, double widthDiff, int alignDiff) {
 
         int[]    dirhist = new int[180];
         double[] minmax  = GeometryUtil.getMinMax(mol);
         Point2d pivot = new Point2d(minmax[0] + ((minmax[2] - minmax[0]) / 2),
                                     minmax[1] + ((minmax[3] - minmax[1]) / 2));
 
         // initial alignment to snapping bonds 60 degrees
         calcDirectionHistogram(mol, dirhist, 60);
         int max = 0;
         for (int i = 1; i < dirhist.length; i++)
             if (dirhist[i] > dirhist[max])
                 max = i;
         // only apply if 50% of the bonds are pointing the same 'wrapped'
         // direction, max=0 means already aligned
         if (max != 0 && dirhist[max]/(double)mol.getBondCount() > 0.5)
             GeometryUtil.rotate(mol, pivot, Math.toRadians(60-max));
 
         double maxWidth = minmax[2] - minmax[0];
         double begWidth = maxWidth;
         calcDirectionHistogram(mol, dirhist, 180);
         int maxAligned = dirhist[60]+dirhist[120];
 
         Point2d[] coords = new Point2d[mol.getAtomCount()];
         for (int i = 0; i < mol.getAtomCount(); i++)
             coords[i] = new Point2d(mol.getAtom(i).getPoint2d());
 
         double step = Math.PI/3;
         double tau = 2*Math.PI;
         double total = 0;
 
         while (total < tau) {
 
             total += step;
             GeometryUtil.rotate(mol, pivot, step);
             minmax = GeometryUtil.getMinMax(mol);
 
             double width = minmax[2] - minmax[0];
             double delta = Math.abs(width - begWidth);
 
             // if this orientation is significantly wider than the
             // best so far select it
             if (delta >= widthDiff && width > maxWidth) {
                 maxWidth = width;
                 for (int j = 0; j < mol.getAtomCount(); j++)
                     coords[j] = new Point2d(mol.getAtom(j).getPoint2d());
             }
             // width is not significantly better or worse so check
             // the number of bonds aligned to 30 deg (aesthetics)
             else if (delta <= widthDiff) {
                 calcDirectionHistogram(mol, dirhist, 180);
                 int aligned = dirhist[60]+dirhist[120];
                 int alignDelta = aligned - maxAligned;
                 if (alignDelta > alignDiff || (alignDelta == 0 && width > maxWidth)) {
                     maxAligned = aligned;
                     maxWidth = width;
                     for (int j = 0; j < mol.getAtomCount(); j++)
                         coords[j] = new Point2d(mol.getAtom(j).getPoint2d());
                 }
             }
         }
 
         // set the best coordinates we found
         for (int i = 0; i < mol.getAtomCount(); i++)
             mol.getAtom(i).setPoint2d(coords[i]);
     }
 
     private final double adjustForHydrogen(IAtom atom, IAtomContainer mol) {
         Integer hcnt = atom.getImplicitHydrogenCount();
         if (hcnt == null || hcnt == 0)
             return 0;
         List<IBond> bonds = mol.getConnectedBondsList(atom);
 
         int pos = 0; // right
 
         // isolated atoms, HCl vs NH4+ etc
         if (bonds.isEmpty()) {
             Elements elem = Elements.ofNumber(atom.getAtomicNumber());
             // see HydrogenPosition for canonical list
             switch (elem) {
                 case Oxygen:
                 case Sulfur:
                 case Selenium:
                 case Tellurium:
                 case Fluorine:
                 case Chlorine:
                 case Bromine:
                 case Iodine:
                     pos = -1; // left
                     break;
                 default:
                     pos = +1; // right
                     break;
             }
         } else if (bonds.size() == 1) {
             IAtom  other  = bonds.get(0).getOther(atom);
             double deltaX = atom.getPoint2d().x - other.getPoint2d().x;
             if (Math.abs(deltaX) > 0.05)
                 pos = (int) Math.signum(deltaX);
         }
         return pos * (bondLength/2);
     }
 
     /**
      * Similar to the method {@link GeometryUtil#getMinMax(IAtomContainer)} but considers
      * heteroatoms with hydrogens.
      *
      * @param mol molecule
      * @return the min/max x and y bounds
      */
     private final double[] getAprxBounds(IAtomContainer mol) {
         double maxX = -Double.MAX_VALUE;
         double maxY = -Double.MAX_VALUE;
         double minX = Double.MAX_VALUE;
         double minY = Double.MAX_VALUE;
         IAtom[] boundedAtoms = new IAtom[4];
         for (int i = 0; i < mol.getAtomCount(); i++) {
             IAtom atom = mol.getAtom(i);
             if (atom.getPoint2d() != null) {
                 if (atom.getPoint2d().x < minX) {
                     minX = atom.getPoint2d().x;
                     boundedAtoms[0] = atom;
                 }
                 if (atom.getPoint2d().y < minY) {
                     minY = atom.getPoint2d().y;
                     boundedAtoms[1] = atom;
                 }
                 if (atom.getPoint2d().x > maxX) {
                     maxX = atom.getPoint2d().x;
                     boundedAtoms[2] = atom;
                 }
                 if (atom.getPoint2d().y > maxY) {
                     maxY = atom.getPoint2d().y;
                     boundedAtoms[3] = atom;
                 }
             }
         }
         double[] minmax = new double[4];
         minmax[0] = minX;
         minmax[1] = minY;
         minmax[2] = maxX;
         minmax[3] = maxY;
         double minXAdjust = adjustForHydrogen(boundedAtoms[0], mol);
         double maxXAdjust = adjustForHydrogen(boundedAtoms[1], mol);
         if (minXAdjust < 0) minmax[0] += minXAdjust;
         if (maxXAdjust > 0) minmax[1] += maxXAdjust;
         return minmax;
     }
 
     private void generateFragmentCoordinates(IAtomContainer mol, List<IAtomContainer> frags) throws CDKException {
         final List<IBond> ionicBonds = makeIonicBonds(frags);
 
         if (!ionicBonds.isEmpty()) {
             // add tmp bonds and re-fragment
             int rollback = mol.getBondCount();
             for (IBond bond : ionicBonds)
                 mol.addBond(bond);
             frags = toList(ConnectivityChecker.partitionIntoMolecules(mol));
 
             // rollback temporary bonds
             int numBonds = mol.getBondCount();
             while (numBonds-- > rollback)
                 mol.removeBond(numBonds);
         }
 
         List<double[]> limits = new ArrayList<>();
         final int numFragments = frags.size();
 
         // avoid overwriting our state
         Set<IAtom> afixbackup = new HashSet<>(afix);
         Set<IBond> bfixbackup = new HashSet<>(bfix);
 
         List<Sgroup> sgroups = mol.getProperty(CDKConstants.CTAB_SGROUPS);
 
         // generate the sub-layouts
         for (IAtomContainer fragment : frags) {
             setMolecule(fragment, false, afix, bfix);
             generateCoordinates(DEFAULT_BOND_VECTOR, true, true);
             lengthenIonicBonds(ionicBonds, fragment);
             double[] aprxBounds = getAprxBounds(fragment);
 
             if (sgroups != null && sgroups.size() > 0) {
                 boolean hasBracket = false;
                 for (Sgroup sgroup : sgroups) {
                     if (!hasBrackets(sgroup))
                         continue;
                     boolean contained = true;
                     Set<IAtom> aset = sgroup.getAtoms();
                     for (IAtom atom : sgroup.getAtoms()) {
                         if (!aset.contains(atom))
                             contained = false;
                     }
                     if (contained) {
                         hasBracket = true;
                         break;
                     }
                 }
 
                 if (hasBracket) {
                     // consider potential Sgroup brackets
                     aprxBounds[0] -= SGROUP_BRACKET_PADDING_FACTOR * bondLength;
                     aprxBounds[1] -= SGROUP_BRACKET_PADDING_FACTOR * bondLength;
                     aprxBounds[2] += SGROUP_BRACKET_PADDING_FACTOR * bondLength;
                     aprxBounds[3] += SGROUP_BRACKET_PADDING_FACTOR * bondLength;
                 }
             }
 
             limits.add(aprxBounds);
         }
 
         // restore
         afix = afixbackup;
         bfix = bfixbackup;
 
         final int nRow = (int) Math.floor(Math.sqrt(numFragments));
         final int nCol = (int) Math.ceil(numFragments / (double) nRow);
 
         final double[] xOffsets = new double[nCol + 1];
         final double[] yOffsets = new double[nRow + 1];
 
         // calc the max widths/height of each row, we also add some
         // spacing
         double spacing = bondLength;
         for (int i = 0; i < numFragments; i++) {
             // +1 because first offset is always 0
             int col = 1 + i % nCol;
             int row = 1 + i / nCol;
 
             double[] minmax = limits.get(i);
             final double width = spacing + (minmax[2] - minmax[0]);
             final double height = spacing + (minmax[3] - minmax[1]);
 
             if (width > xOffsets[col])
                 xOffsets[col] = width;
             if (height > yOffsets[row])
                 yOffsets[row] = height;
         }
 
         // cumulative counts
         for (int i = 1; i < xOffsets.length; i++)
             xOffsets[i] += xOffsets[i - 1];
         for (int i = 1; i < yOffsets.length; i++)
             yOffsets[i] += yOffsets[i - 1];
 
         // translate the molecules, note need to flip y axis
         for (int i = 0; i < limits.size(); i++) {
             final int row = nRow - (i / nCol) - 1;
             final int col = i % nCol;
             Point2d dest = new Point2d((xOffsets[col] + xOffsets[col + 1]) / 2,
                                        (yOffsets[row] + yOffsets[row + 1]) / 2);
             double[] minmax = limits.get(i);
             Point2d curr = new Point2d((minmax[0] + minmax[2]) / 2, (minmax[1] + minmax[3]) / 2);
             GeometryUtil.translate2D(frags.get(i),
                                      dest.x - curr.x, dest.y - curr.y);
         }
 
         // correct double-bond stereo, this changes the layout and in reality
         // should be done during the initial placement
         if (mol.stereoElements().iterator().hasNext())
             CorrectGeometricConfiguration.correct(mol);
 
         // finalize
         assignStereochem(mol);
         finalizeLayout(mol);
     }
 
     private void lengthenIonicBonds(List<IBond> ionicBonds, IAtomContainer fragment) {
 
         final IChemObjectBuilder bldr = fragment.getBuilder();
 
         if (ionicBonds.isEmpty())
             return;
 
         IAtomContainer newfrag = bldr.newInstance(IAtomContainer.class);
         IAtom[] atoms = new IAtom[fragment.getAtomCount()];
         for (int i = 0; i < atoms.length; i++)
             atoms[i] = fragment.getAtom(i);
         newfrag.setAtoms(atoms);
 
         for (IBond bond : fragment.bonds()) {
             if (!ionicBonds.contains(bond)) {
                 newfrag.addBond(bond);
             } else {
                 Integer numBegIonic = bond.getBegin().getProperty("ionicDegree");
                 Integer numEndIonic = bond.getEnd().getProperty("ionicDegree");
                 if (numBegIonic == null) numBegIonic = 0;
                 if (numEndIonic == null) numEndIonic = 0;
                 numBegIonic++;
                 numEndIonic++;
                 bond.getBegin().setProperty("ionicDegree", numBegIonic);
                 bond.getEnd().setProperty("ionicDegree", numEndIonic);
             }
         }
 
         if (newfrag.getBondCount() == fragment.getBondCount())
             return;
 
         IAtomContainerSet subfragments = ConnectivityChecker.partitionIntoMolecules(newfrag);
         Map<IAtom, IAtomContainer> atomToFrag = new HashMap<>();
 
         // index atom->fragment
         for (IAtomContainer subfragment : subfragments.atomContainers())
             for (IAtom atom : subfragment.atoms())
                 atomToFrag.put(atom, subfragment);
 
         for (IBond bond : ionicBonds) {
             IAtom beg = bond.getBegin();
             IAtom end = bond.getEnd();
 
             // select which bond to stretch from
             Integer numBegIonic = bond.getBegin().getProperty("ionicDegree");
             Integer numEndIonic = bond.getEnd().getProperty("ionicDegree");
             if (numBegIonic == null || numEndIonic == null)
                 continue;
             if (numBegIonic > numEndIonic) {
                 IAtom tmp = beg;
                 beg = end;
                 end = tmp;
             } else if (numBegIonic.equals(numEndIonic) && numBegIonic > 1) {
                 // can't stretch these
                 continue;
             }
 
             IAtomContainer begFrag  = atomToFrag.get(beg);
             IAtomContainer endFrags = bldr.newInstance(IAtomContainer.class);
             if (begFrag == null)
                 continue;
             for (IAtomContainer mol : subfragments.atomContainers()) {
                 if (mol != begFrag)
                     endFrags.add(mol);
             }
             double dx = end.getPoint2d().x - beg.getPoint2d().x;
             double dy = end.getPoint2d().y - beg.getPoint2d().y;
             Vector2d bondVec = new Vector2d(dx, dy);
             bondVec.normalize();
             bondVec.scale(bondLength/2); // 1.5 bond length
             GeometryUtil.translate2D(endFrags, bondVec);
         }
     }
 
     /**
      * Property to cache the charge of a fragment.
      */
     private static final String FRAGMENT_CHARGE = "FragmentCharge";
 
     /**
      * Merge fragments with duplicate atomic ions (e.g. [Na+].[Na+].[Na+]) into
      * single fragments.
      *
      * @param frags input fragments (all connected)
      * @return the merge ions
      */
     private List<IAtomContainer> mergeAtomicIons(final List<IAtomContainer> frags) {
         final List<IAtomContainer> res = new ArrayList<>(frags.size());
         for (IAtomContainer frag : frags) {
 
             IChemObjectBuilder bldr = frag.getBuilder();
 
             if (frag.getBondCount() > 0 || res.isEmpty()) {
                 res.add(bldr.newInstance(IAtomContainer.class, frag));
             } else {
                 // try to find matching atomic ion
                 int i = 0;
                 while (i < res.size()) {
                     IAtom iAtm = frag.getAtom(0);
                     if (res.get(i).getBondCount() == 0) {
                         IAtom jAtm = res.get(i).getAtom(0);
                         if (nullAsZero(iAtm.getFormalCharge()) == nullAsZero(jAtm.getFormalCharge()) &&
                             nullAsZero(iAtm.getAtomicNumber()) == nullAsZero(jAtm.getAtomicNumber()) &&
                             nullAsZero(iAtm.getImplicitHydrogenCount()) == nullAsZero(jAtm.getImplicitHydrogenCount())) {
                             break;
                         }
                     }
                     i++;
                 }
 
                 if (i < res.size()) {
                     res.get(i).add(frag);
                 } else {
                     res.add(bldr.newInstance(IAtomContainer.class, frag));
                 }
             }
         }
         return res;
     }
 
     /**
      * Select ions from a charged fragment. Ions not in charge separated
      * bonds are favoured but select if needed. If an atom has lost or
      * gained more than one electron it is added mutliple times to the
      * output list
      *
      * @param frag charged fragment
      * @param sign the charge sign to select (+1 : cation, -1: anion)
      * @return the select atoms (includes duplicates)
      */
     private List<IAtom> selectIons(IAtomContainer frag, int sign) {
         int fragChg = frag.getProperty(FRAGMENT_CHARGE);
         assert Integer.signum(fragChg) == sign;
         final List<IAtom> atoms = new ArrayList<>();
 
         FIRST_PASS:
         for (IAtom atom : frag.atoms()) {
             if (fragChg == 0)
                 break;
             int atmChg = nullAsZero(atom.getFormalCharge());
             if (Integer.signum(atmChg) == sign) {
 
                 // skip in first pass if charge separated
                 for (IBond bond : frag.getConnectedBondsList(atom)) {
                     if (Integer.signum(nullAsZero(bond.getOther(atom).getFormalCharge())) + sign == 0)
                         continue FIRST_PASS;
                 }
 
                 while (fragChg != 0 && atmChg != 0) {
                     atoms.add(atom);
                     atmChg -= sign;
                     fragChg -= sign;
                 }
             }
         }
 
         if (fragChg == 0)
             return atoms;
 
         for (IAtom atom : frag.atoms()) {
             if (fragChg == 0)
                 break;
             int atmChg = nullAsZero(atom.getFormalCharge());
             if (Math.signum(atmChg) == sign) {
                 while (fragChg != 0 && atmChg != 0) {
                     atoms.add(atom);
                     atmChg -= sign;
                     fragChg -= sign;
                 }
             }
         }
 
         return atoms;
     }
 
     /**
      * Alternative method name "Humpty Dumpty" (a la. R Sayle).
      * 
      * (Re)bonding of ionic fragments for improved layout. This method takes a list
      * of two or more fragments and creates zero or more bonds (return value) that
      * should be temporarily used for layout generation. In general this problem is
      * difficult but since molecules will be laid out in a grid by default - any
      * positioning is an improvement. Heuristics could be added if bad (re)bonds
      * are seen.
      *
      * @param frags connected fragments
      * @return ionic bonds to make
      */
     private List<IBond> makeIonicBonds(final List<IAtomContainer> frags) {
         assert frags.size() > 1;
 
         // merge duplicates together, e.g. [H-].[H-].[H-].[Na+].[Na+].[Na+]
         // would be two needsMerge fragments. We currently only do single
         // atoms but in theory could also do larger ones
         final List<IAtomContainer> mergedFrags = mergeAtomicIons(frags);
         final List<IAtomContainer> posFrags = new ArrayList<>();
         final List<IAtomContainer> negFrags = new ArrayList<>();
 
         int chgSum = 0;
         for (IAtomContainer frag : mergedFrags) {
             int chg = 0;
             for (final IAtom atom : frag.atoms())
                 chg += nullAsZero(atom.getFormalCharge());
             chgSum += chg;
             frag.setProperty(FRAGMENT_CHARGE, chg);
             if (chg < 0)
                 negFrags.add(frag);
             else if (chg > 0)
                 posFrags.add(frag);
         }
 
         // non-neutral or we only have one needsMerge fragment?
         if (chgSum != 0 || mergedFrags.size() == 1)
             return Collections.emptyList();
 
         List<IAtom> cations = new ArrayList<>();
         List<IAtom> anions = new ArrayList<>();
 
         // trivial case
         if (posFrags.size() == 1 && negFrags.size() == 1) {
             cations.addAll(selectIons(posFrags.get(0), +1));
             anions.addAll(selectIons(negFrags.get(0), -1));
         } else {
 
             // sort hi->lo fragment charge, if same charge then we put smaller
             // fragments (bond count) before in cations and after in anions
             Comparator<IAtomContainer> comparator = new Comparator<IAtomContainer>() {
                 @Override
                 public int compare(IAtomContainer a, IAtomContainer b) {
                     int qA = a.getProperty(FRAGMENT_CHARGE);
                     int qB = b.getProperty(FRAGMENT_CHARGE);
                     int cmp = Integer.compare(Math.abs(qA), Math.abs(qB));
                     if (cmp != 0) return cmp;
                     int sign = Integer.signum(qA);
                     return Integer.compare(sign * a.getBondCount(), sign * b.getBondCount());
                 }
             };
 
             // greedy selection
             Collections.sort(posFrags, comparator);
             Collections.sort(negFrags, comparator);
 
             for (IAtomContainer posFrag : posFrags)
                 cations.addAll(selectIons(posFrag, +1));
             for (IAtomContainer negFrag : negFrags)
                 anions.addAll(selectIons(negFrag, -1));
         }
 
         if (cations.size() != anions.size() && cations.isEmpty())
             return Collections.emptyList();
 
         final IChemObjectBuilder bldr = frags.get(0).getBuilder();
 
         // make the bonds
         final List<IBond> ionicBonds = new ArrayList<>(cations.size());
         for (int i = 0; i < cations.size(); i++) {
             final IAtom beg = cations.get(i);
             final IAtom end = anions.get(i);
 
             boolean unique = true;
             for (IBond bond : ionicBonds)
                 if (bond.getBegin().equals(beg) && bond.getEnd().equals(end) ||
                     bond.getEnd().equals(beg) && bond.getBegin().equals(end))
                     unique = false;
 
             if (unique)
                 ionicBonds.add(bldr.newInstance(IBond.class, beg, end));
         }
 
         // we could merge the fragments here using union-find structures
         // but it's much simpler (and probably more efficient) to return
         // the new bonds and re-fragment the molecule with these bonds added.
 
         return ionicBonds;
     }
 
     /**
      * Utility - safely access Object Integers as primitives, when we want the
      * default value of null to be zero.
      *
      * @param x number
      * @return the number primitive or zero if null
      */
     private static int nullAsZero(Integer x) {
         return x == null ? 0 : x;
     }
 
     /**
      * Utility - get the IAtomContainers as a list.
      *
      * @param frags connected fragments
      * @return list of fragments
      */
     private List<IAtomContainer> toList(IAtomContainerSet frags) {
         List<IAtomContainer> res = new ArrayList<>(frags.getAtomContainerCount());
         frags.atomContainers().forEach(res::add);
         return res;
     }
 
     /**
      * The main method of this StructurDiagramGenerator. Assign a molecule to the
      * StructurDiagramGenerator, call the generateCoordinates() method and get
      * your molecule back.
      *
      * @throws CDKException if an error occurs
      */
     public void generateCoordinates() throws CDKException {
         generateCoordinates(DEFAULT_BOND_VECTOR);
     }
 
     /**
      * Using a fast identity template library, lookup the the ring system and assign coordinates.
      * The method indicates whether a match was found and coordinates were assigned.
      *
      * @param rs       the ring set
      * @param molecule the rest of the compound
      * @param anon     check for anonmised templates
      * @return coordinates were assigned
      */
     private boolean lookupRingSystem(IRingSet rs, IAtomContainer molecule, boolean anon) {
 
         // identity templates are disabled
         if (!useIdentTemplates) return false;
 
         final IChemObjectBuilder bldr = molecule.getBuilder();
 
         final IAtomContainer ringSystem = bldr.newInstance(IAtomContainer.class);
         for (IAtomContainer container : rs.atomContainers())
             ringSystem.add(container);
 
         final Set<IAtom> ringAtoms = new HashSet<>();
         for (IAtom atom : ringSystem.atoms())
             ringAtoms.add(atom);
 
         // a temporary molecule of the ring system and 'stubs' of the attached substituents
         final IAtomContainer ringWithStubs = bldr.newInstance(IAtomContainer.class);
         ringWithStubs.add(ringSystem);
         for (IBond bond : molecule.bonds()) {
             IAtom atom1 = bond.getBegin();
             IAtom atom2 = bond.getEnd();
             if (isHydrogen(atom1) || isHydrogen(atom2)) continue;
             if (ringAtoms.contains(atom1) ^ ringAtoms.contains(atom2)) {
                 ringWithStubs.addAtom(atom1);
                 ringWithStubs.addAtom(atom2);
                 ringWithStubs.addBond(bond);
             }
         }
 
         // Three levels of identity to check are as follows:
         //   Level 1 - check for a skeleton ring system and attached substituents
         //   Level 2 - check for a skeleton ring system
         //   Level 3 - check for an anonymous ring system
         // skeleton = all single bonds connecting different elements
         // anonymous = all single bonds connecting carbon
         final IAtomContainer skeletonStub = clearHydrogenCounts(AtomContainerManipulator.skeleton(ringWithStubs));
         final IAtomContainer skeleton = clearHydrogenCounts(AtomContainerManipulator.skeleton(ringSystem));
         final IAtomContainer anonymous = clearHydrogenCounts(AtomContainerManipulator.anonymise(ringSystem));
 
         for (IAtomContainer container : Arrays.asList(skeletonStub, skeleton, anonymous)) {
 
             if (!anon && container == anonymous)
                 continue;
 
             // assign the atoms 0 to |ring|, the stubs are added at the end of the container
             // and are not placed here (since the index of each stub atom is > |ring|)
             if (identityLibrary.assignLayout(container)) {
                 for (int i = 0; i < ringSystem.getAtomCount(); i++) {
                     IAtom atom = ringSystem.getAtom(i);
                     atom.setPoint2d(container.getAtom(i).getPoint2d());
                     atom.setFlag(CDKConstants.ISPLACED, true);
                 }
                 return true;
             }
         }
 
         return false;
     }
 
     /**
      * Is an atom a hydrogen atom.
      *
      * @param atom an atom
      * @return the atom is a hydrogen
      */
     private static boolean isHydrogen(IAtom atom) {
         if (atom.getAtomicNumber() != null) return atom.getAtomicNumber() == 1;
         return "H".equals(atom.getSymbol());
     }
 
 
/** Simple assistance function that sets all hydrogen counts to 0. */
 private static IAtomContainer clearHydrogenCounts(IAtomContainer container){}

 

}