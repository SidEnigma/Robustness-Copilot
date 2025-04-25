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
 
 
/** The CDK OWL is used to convert a @link IAtomContainer into a @link Model representation. */

public static Model molecule2Model(IAtomContainer molecule) {
    // Create a new Jena Model
    Model model = ModelFactory.createDefaultModel();

    // Iterate over the atoms in the molecule
    for (IAtom atom : molecule.atoms()) {
        // Create a new resource for the atom
        Resource atomResource = model.createResource();

        // Set the RDF type of the atom resource to CDK:Atom
        atomResource.addProperty(RDF.type, CDKConstants.Atom);

        // Set the CDK:element property of the atom resource
        IElement element = atom.getElement();
        atomResource.addProperty(CDKConstants.element, element.getSymbol());

        // Set the CDK:atomType property of the atom resource
        IAtomType atomType = atom.getAtomType();
        if (atomType != null) {
            atomResource.addProperty(CDKConstants.atomType, atomType.getAtomTypeName());
        }

        // Set the CDK:hybridization property of the atom resource
        Hybridization hybridization = atom.getHybridization();
        if (hybridization != null) {
            atomResource.addProperty(CDKConstants.hybridization, hybridization.toString());
        }

        // Add the atom resource to the model
        model.add(atomResource);
    }

    // Iterate over the bonds in the molecule
    for (IBond bond : molecule.bonds()) {
        // Create a new resource for the bond
        Resource bondResource = model.createResource();

        // Set the RDF type of the bond resource to CDK:Bond
        bondResource.addProperty(RDF.type, CDKConstants.Bond);

        // Set the CDK:order property of the bond resource
        Order order = bond.getOrder();
        bondResource.addProperty(CDKConstants.order, order.toString());

        // Set the CDK:atom1 property of the bond resource
        IAtom atom1 = bond.getAtom(0);
        Resource atom1Resource = model.getResource(atom1.getID());
        bondResource.addProperty(CDKConstants.atom1, atom1Resource);

        // Set the CDK:atom2 property of the bond resource
        IAtom atom2 = bond.getAtom(1);
        Resource atom2Resource = model.getResource(atom2.getID());
        bondResource.addProperty(CDKConstants.atom2, atom2Resource);

        // Add the bond resource to the model
        model.add(bondResource);
    }

    return model;
}
 

}