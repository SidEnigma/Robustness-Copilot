/* 
  * Copyright (C) 2005-2007  Christian Hoppe <chhoppe@users.sf.net>
  *                    2014  Mark B Vine (orcid:0000-0002-7794-0426)
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
 package org.openscience.cdk.modeling.builder3d;
 
 import java.io.IOException;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import javax.vecmath.Point3d;
 import javax.vecmath.Vector3d;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.exception.NoSuchAtomTypeException;
 import org.openscience.cdk.geometry.GeometryUtil;
 import org.openscience.cdk.graph.ConnectivityChecker;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.interfaces.IRingSet;
 import org.openscience.cdk.layout.AtomPlacer;
 import org.openscience.cdk.ringsearch.RingPartitioner;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.RingSetManipulator;
 
 /**
  *  The main class to generate the 3D coordinates of a molecule ModelBuilder3D.
  *  Its use looks like:
  *  <pre>
  *  ModelBuilder3D mb3d = ModelBuilder3D.getInstance();
  *  IAtomContainer molecule = mb3d.generate3DCoordinates(molecule, false);
  *  </pre>
  *
  *  <p>Standing problems:
  *  <ul>
  *    <li>condensed ring systems which are unknown for the template class
  *    <li>vdWaals clashes
  *    <li>stereochemistry
  *    <li>chains running through ring systems
  *  </ul>
  *
  * @author      cho
  * @author      steinbeck
  * @cdk.created 2004-09-07
  * @cdk.module  builder3d
  * @cdk.githash
  * @cdk.keyword 3D coordinates
  * @cdk.keyword coordinate generation, 3D
  */
 public class ModelBuilder3D {
 
     private static Map<String, ModelBuilder3D> memyselfandi    = new HashMap<String, ModelBuilder3D>();
 
     private TemplateHandler3D                  templateHandler = null;
 
     private Map                                parameterSet    = null;
 
     private final ForceFieldConfigurator       ffc             = new ForceFieldConfigurator();
 
     String                                     forceFieldName  = "mm2";
 
     private static ILoggingTool                logger          = LoggingToolFactory
                                                                        .createLoggingTool(ModelBuilder3D.class);
 
     /**
      * Constructor for the ModelBuilder3D object.
      *
      * @param  templateHandler  templateHandler Object
      * @param  ffname           name of force field
      */
     private ModelBuilder3D(TemplateHandler3D templateHandler, String ffname, IChemObjectBuilder builder)
             throws CDKException {
         setTemplateHandler(templateHandler);
         setForceField(ffname, builder);
     }
 
     public static ModelBuilder3D getInstance(TemplateHandler3D templateHandler, String ffname,
             IChemObjectBuilder chemObjectBuilder) throws CDKException {
         if (ffname == null || ffname.length() == 0) throw new CDKException("The given ffname is null or empty!");
         if (templateHandler == null) throw new CDKException("The given template handler is null!");
 
         String builderCode = templateHandler.getClass().getName() + "#" + ffname;
         if (!memyselfandi.containsKey(builderCode)) {
             ModelBuilder3D builder = new ModelBuilder3D(templateHandler, ffname, chemObjectBuilder);
             memyselfandi.put(builderCode, builder);
             return builder;
         }
         return memyselfandi.get(builderCode);
     }
 
     public static ModelBuilder3D getInstance(IChemObjectBuilder builder) throws CDKException {
         return getInstance(TemplateHandler3D.getInstance(), "mm2", builder);
     }
 
     /**
      * Gives a list of possible force field types.
      *
      * @return                the list
      */
     public String[] getFfTypes() {
         return ffc.getFfTypes();
     }
 
     /**
      * Sets the forceField attribute of the ModelBuilder3D object.
      *
      * @param  ffname  forceField name
      */
     private void setForceField(String ffname, IChemObjectBuilder builder) throws CDKException {
         if (ffname == null) {
             ffname = "mm2";
         }
         try {
             forceFieldName = ffname;
             ffc.setForceFieldConfigurator(ffname, builder);
             parameterSet = ffc.getParameterSet();
         } catch (CDKException ex1) {
             logger.error("Problem with ForceField configuration due to>" + ex1.getMessage());
             logger.debug(ex1);
             throw new CDKException("Problem with ForceField configuration due to>" + ex1.getMessage(), ex1);
         }
     }
 
     /**
      * Generate 3D coordinates with force field information.
      */
     public IAtomContainer generate3DCoordinates(IAtomContainer molecule, boolean clone) throws CDKException,
             NoSuchAtomTypeException, CloneNotSupportedException, IOException {
         String[] originalAtomTypeNames = new String[molecule.getAtomCount()];
         for (int i = 0; i < originalAtomTypeNames.length; i++) {
             originalAtomTypeNames[i] = molecule.getAtom(i).getAtomTypeName();
         }
 
         logger.debug("******** GENERATE COORDINATES ********");
         for (int i = 0; i < molecule.getAtomCount(); i++) {
             molecule.getAtom(i).setFlag(CDKConstants.ISPLACED, false);
             molecule.getAtom(i).setFlag(CDKConstants.VISITED, false);
         }
         //CHECK FOR CONNECTIVITY!
         logger.debug("#atoms>" + molecule.getAtomCount());
         if (!ConnectivityChecker.isConnected(molecule)) {
             throw new CDKException("Molecule is NOT connected, could not layout.");
         }
 
         // setup helper classes
         AtomPlacer atomPlacer = new AtomPlacer();
         AtomPlacer3D ap3d = new AtomPlacer3D();
         AtomTetrahedralLigandPlacer3D atlp3d = new AtomTetrahedralLigandPlacer3D();
         ap3d.initilize(parameterSet);
         atlp3d.setParameterSet(parameterSet);
 
         if (clone) molecule = (IAtomContainer) molecule.clone();
         atomPlacer.setMolecule(molecule);
 
         if (ap3d.numberOfUnplacedHeavyAtoms(molecule) == 1) {
             logger.debug("Only one Heavy Atom");
             ap3d.getUnplacedHeavyAtom(molecule).setPoint3d(new Point3d(0.0, 0.0, 0.0));
             try {
                 atlp3d.add3DCoordinatesForSinglyBondedLigands(molecule);
             } catch (CDKException ex3) {
                 logger.error("PlaceSubstitutensERROR: Cannot place substitutents due to:" + ex3.getMessage());
                 logger.debug(ex3);
                 throw new CDKException("PlaceSubstitutensERROR: Cannot place substitutents due to:" + ex3.getMessage(),
                         ex3);
             }
             return molecule;
         }
         //Assing Atoms to Rings,Aliphatic and Atomtype
         IRingSet ringSetMolecule = ffc.assignAtomTyps(molecule);
         List ringSystems = null;
         IRingSet largestRingSet = null;
         int numberOfRingAtoms = 0;
 
         if (ringSetMolecule.getAtomContainerCount() > 0) {
             if (templateHandler == null) {
                 throw new CDKException(
                         "You are trying to generate coordinates for a molecule with rings, but you have no template handler set. Please do setTemplateHandler() before generation!");
             }
             ringSystems = RingPartitioner.partitionRings(ringSetMolecule);
             largestRingSet = RingSetManipulator.getLargestRingSet(ringSystems);
             IAtomContainer largestRingSetContainer = RingSetManipulator.getAllInOneContainer(largestRingSet);
             numberOfRingAtoms = largestRingSetContainer.getAtomCount();
             templateHandler.mapTemplates(largestRingSetContainer, numberOfRingAtoms);
             if (!checkAllRingAtomsHasCoordinates(largestRingSetContainer)) {
                 throw new CDKException("RingAtomLayoutError: Not every ring atom is placed! Molecule cannot be layout.");
             }
 
             setAtomsToPlace(largestRingSetContainer);
             searchAndPlaceBranches(molecule, largestRingSetContainer, ap3d, atlp3d, atomPlacer);
             largestRingSet = null;
         } else {
             //logger.debug("****** Start of handling aliphatic molecule ******");
             IAtomContainer ac = null;
 
             ac = atomPlacer.getInitialLongestChain(molecule);
             setAtomsToUnVisited(molecule);
             setAtomsToUnPlaced(molecule);
             ap3d.placeAliphaticHeavyChain(molecule, ac);
             //ZMatrixApproach
             ap3d.zmatrixChainToCartesian(molecule, false);
             searchAndPlaceBranches(molecule, ac, ap3d, atlp3d, atomPlacer);
         }
         layoutMolecule(ringSystems, molecule, ap3d, atlp3d, atomPlacer);
         //logger.debug("******* PLACE SUBSTITUENTS ******");
         try {
             atlp3d.add3DCoordinatesForSinglyBondedLigands(molecule);
         } catch (CDKException ex3) {
             logger.error("PlaceSubstitutensERROR: Cannot place substitutents due to:" + ex3.getMessage());
             logger.debug(ex3);
             throw new CDKException("PlaceSubstitutensERROR: Cannot place substitutents due to:" + ex3.getMessage(), ex3);
         }
         // restore the original atom type names
         for (int i = 0; i < originalAtomTypeNames.length; i++) {
             molecule.getAtom(i).setAtomTypeName(originalAtomTypeNames[i]);
         }
 
         return molecule;
     }
 
     /**
      * Gets the ringSetOfAtom attribute of the ModelBuilder3D object.
      *
      *@return              The ringSetOfAtom value
      */
     private IRingSet getRingSetOfAtom(List ringSystems, IAtom atom) {
         IRingSet ringSetOfAtom = null;
         for (int i = 0; i < ringSystems.size(); i++) {
             if (((IRingSet) ringSystems.get(i)).contains(atom)) {
                 return (IRingSet) ringSystems.get(i);
             }
         }
         return ringSetOfAtom;
     }
 
 
/** Starting with ring systems and moving on to aliphatic chains, the molecule is laid out. */
private void layoutMolecule(List ringSetMolecule, IAtomContainer molecule, AtomPlacer3D ap3d, AtomTetrahedralLigandPlacer3D atlp3d, AtomPlacer atomPlacer) throws CDKException, IOException, CloneNotSupportedException {
    /** Starting with ring systems and moving on to aliphatic chains, the molecule is laid out. */
    for (int i = 0; i < ringSetMolecule.size(); i++) {
        IRingSet ringSet = (IRingSet) ringSetMolecule.get(i);
        IAtomContainer ringSetContainer = RingSetManipulator.getAllInOneContainer(ringSet);
        int numberOfRingAtoms = ringSetContainer.getAtomCount();
        templateHandler.mapTemplates(ringSetContainer, numberOfRingAtoms);
        if (!checkAllRingAtomsHasCoordinates(ringSetContainer)) {
            throw new CDKException("RingAtomLayoutError: Not every ring atom is placed! Molecule cannot be layout.");
        }
        setAtomsToPlace(ringSetContainer);
        searchAndPlaceBranches(molecule, ringSetContainer, ap3d, atlp3d, atomPlacer);
    }
    IAtomContainer ac = null;
    ac = atomPlacer.getInitialLongestChain(molecule);
    setAtomsToUnVisited(molecule);
    setAtomsToUnPlaced(molecule);
    ap3d.placeAliphaticHeavyChain(molecule, ac);
    ap3d.zmatrixChainToCartesian(molecule, false);
    searchAndPlaceBranches(molecule, ac, ap3d, atlp3d, atomPlacer);
}
 

}