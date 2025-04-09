/* Copyright (C) 2003-2008  Egon Willighagen <egonw@users.sf.net>
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
  *
  */
 package org.openscience.cdk;
 
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomContainerSet;
 import org.openscience.cdk.interfaces.IMapping;
 import org.openscience.cdk.interfaces.IReaction;
 
 import java.io.Serializable;
 import java.util.Hashtable;
 import java.util.Iterator;
 import java.util.Map;
 
 /**
  * Represents the idea of a chemical reaction. The reaction consists of
  * a set of reactants and a set of products.
  *
  * <p>The class mostly represents abstract reactions, such as 2D diagrams,
  * and is not intended to represent reaction trajectories. Such can better
  * be represented with a ChemSequence.
  *
  * @cdk.module data
  * @cdk.githash
  *
  * @author Egon Willighagen &lt;elw38@cam.ac.uk&gt;
  * @cdk.created 2003-02-13
  * @cdk.keyword reaction
  */
 public class Reaction extends ChemObject implements Serializable, IReaction, Cloneable {
 
     /**
      * Determines if a de-serialized object is compatible with this class.
      *
      * This value must only be changed if and only if the new version
      * of this class is incompatible with the old version. See Sun docs
      * for <a href=http://java.sun.com/products/jdk/1.1/docs/guide
      * /serialization/spec/version.doc.html>details</a>.
      */
     private static final long   serialVersionUID = -554752558363533678L;
 
     protected int               growArraySize    = 3;
 
     protected IAtomContainerSet reactants;
     protected IAtomContainerSet products;
     /** These are the used solvent, catalysts etc that normally appear above
         the reaction arrow */
     protected IAtomContainerSet agents;
 
     protected IMapping[]        map;
     protected int               mappingCount;
 
     private IReaction.Direction reactionDirection;
 
     /**
      * Constructs an empty, forward reaction.
      */
     public Reaction() {
         this.reactants = getBuilder().newInstance(IAtomContainerSet.class);
         this.products = getBuilder().newInstance(IAtomContainerSet.class);
         this.agents = getBuilder().newInstance(IAtomContainerSet.class);
         this.map = new Mapping[growArraySize];
         mappingCount = 0;
         reactionDirection = IReaction.Direction.FORWARD;
     }
 
     /**
      * Returns the number of reactants in this reaction.
      *
      * @return The number of reactants in this reaction
      */
     @Override
     public int getReactantCount() {
         return reactants.getAtomContainerCount();
     }
 
     /**
      * Returns the number of products in this reaction.
      *
      * @return The number of products in this reaction
      */
     @Override
     public int getProductCount() {
         return products.getAtomContainerCount();
     }
 
     /**
      * Returns a MoleculeSet containing the reactants in this reaction.
      *
      * @return A MoleculeSet containing the reactants in this reaction
      * @see    org.openscience.cdk.interfaces.IReaction#setReactants
      */
     @Override
     public IAtomContainerSet getReactants() {
         return reactants;
     }
 
     /**
      * Assigns a MoleculeSet to the reactants in this reaction.
      *
      *
      * @param setOfMolecules The new set of reactants
      * @see   #getReactants
      */
     @Override
     public void setReactants(IAtomContainerSet setOfMolecules) {
         reactants = setOfMolecules;
         notifyChanged();
     }
 
     /**
      * Returns a MoleculeSet containing the products of this reaction.
      *
      * @return A MoleculeSet containing the products in this reaction
      * @see    org.openscience.cdk.interfaces.IReaction#setProducts
      */
     @Override
     public IAtomContainerSet getProducts() {
         return products;
     }
 
     /**
      * Assigns a MoleculeSet to the products of this reaction.
      *
      *
      * @param setOfMolecules The new set of products
      * @see   #getProducts
      */
     @Override
     public void setProducts(IAtomContainerSet setOfMolecules) {
         products = setOfMolecules;
         notifyChanged();
     }
 
     /**
      * Returns a MoleculeSet containing the agents in this reaction.
      *
      * @return A MoleculeSet containing the agents in this reaction
      * @see    #addAgent
      */
     @Override
     public IAtomContainerSet getAgents() {
         return agents;
     }
 
 
/** Returns the mappings between the reagent and the product side. */
 public Iterable<IMapping> mappings(){}

 

}