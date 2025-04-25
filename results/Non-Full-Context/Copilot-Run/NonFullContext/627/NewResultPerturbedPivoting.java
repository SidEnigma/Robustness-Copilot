/* Copyright (C) 2009  Egon Willighagen <egonw@users.sf.net>
  *               2014  Mark B Vine (orcid:0000-0002-7794-0426)
  *
  * Contact: jchempaint-devel@lists.sourceforge.net
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
 package org.openscience.cdk.libio.jena;
 
 import java.util.HashMap;
 import java.util.Map;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomType;
 import org.openscience.cdk.interfaces.IAtomType.Hybridization;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IBond.Order;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.interfaces.IElectronContainer;
 import org.openscience.cdk.interfaces.IElement;
 import org.openscience.cdk.interfaces.IIsotope;
 import org.openscience.cdk.interfaces.IPseudoAtom;
 
 import com.hp.hpl.jena.rdf.model.Model;
 import com.hp.hpl.jena.rdf.model.ModelFactory;
 import com.hp.hpl.jena.rdf.model.ResIterator;
 import com.hp.hpl.jena.rdf.model.Resource;
 import com.hp.hpl.jena.rdf.model.Statement;
 import com.hp.hpl.jena.rdf.model.StmtIterator;
 import com.hp.hpl.jena.vocabulary.RDF;
 
 /**
  * Helper class that converts a CDK {@link IChemObject} into RDF using a
  * Jena model and the CDK data model ontology.
  *
  * @cdk.module       iordf
  * @cdk.githash
  * @cdk.keyword      Resource Description Framework
  * @cdk.keyword      Jena
  * @cdk.keyword      RDF
  * @cdk.keyword      Web Ontology Language
  * @cdk.keyword      OWL
  */
 public class Convertor {
 
 
/** Converts an {@link IAtomContainer} to a {@link Model} representation using the OWL CDK. */

public static Model molecule2Model(IAtomContainer molecule) {
    // Create a new Jena Model
    Model model = ModelFactory.createDefaultModel();

    // Create a map to store the Jena Resource objects for each CDK object
    Map<IChemObject, Resource> resourceMap = new HashMap<>();

    // Iterate over the atoms in the molecule
    for (IAtom atom : molecule.atoms()) {
        // Create a Jena Resource for the atom
        Resource atomResource = model.createResource();
        resourceMap.put(atom, atomResource);

        // Set the RDF type of the atom to CDK:Atom
        atomResource.addProperty(RDF.type, CDKConstants.Atom);

        // Set the atomic number of the atom
        atomResource.addProperty(CDKConstants.AtomicNumber, String.valueOf(atom.getAtomicNumber()));

        // Set the atom type of the atom
        IAtomType atomType = atom.getAtomType();
        if (atomType != null) {
            atomResource.addProperty(CDKConstants.AtomType, atomType.getAtomTypeName());
        }

        // Set the hybridization of the atom
        Hybridization hybridization = atom.getHybridization();
        if (hybridization != null) {
            atomResource.addProperty(CDKConstants.Hybridization, hybridization.toString());
        }
    }

    // Iterate over the bonds in the molecule
    for (IBond bond : molecule.bonds()) {
        // Get the Jena Resource objects for the atoms involved in the bond
        Resource sourceAtomResource = resourceMap.get(bond.getAtom(0));
        Resource targetAtomResource = resourceMap.get(bond.getAtom(1));

        // Create a Jena Resource for the bond
        Resource bondResource = model.createResource();
        bondResource.addProperty(RDF.type, CDKConstants.Bond);

        // Set the source and target atoms of the bond
        bondResource.addProperty(CDKConstants.SourceAtom, sourceAtomResource);
        bondResource.addProperty(CDKConstants.TargetAtom, targetAtomResource);

        // Set the order of the bond
        Order bondOrder = bond.getOrder();
        if (bondOrder != null) {
            bondResource.addProperty(CDKConstants.BondOrder, bondOrder.toString());
        }
    }

    return model;
}
 

}