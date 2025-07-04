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
 public static Model molecule2Model(IAtomContainer molecule){
   	Model model = ModelFactory.createDefaultModel();
 	model.setNsPrefix("cdk", "http://www.openscience.org/cdk/owl/");
 	model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
 	model.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
 	model.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
 	model.setNsPrefix("dc", "http://purl.org/dc/elements/1.1/");
 	model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
 	model.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
 	model.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
 	model.setNsPrefix("cc", "http://web.resource.org/cc/");
 	model.setNsPrefix("bibo", "http://purl.org/ontology/bibo/");
 	model.setNsPrefix("dcam", "http://purl.org/dc/dcam/");
 	model.setNsPrefix("dct", "http://purl.org/dc/terms/");
 	model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
 	model.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
 	model.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
 	model.setNsPrefix("dc", "http://purl.org/dc/elements/1.1/");    
 }

 

}