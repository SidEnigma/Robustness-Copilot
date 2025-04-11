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
 
     /**
      * Returns the mappings between the reactant and the product side.
      *
      * @return An Iterator to the Mappings.
      * @see    #addMapping
      */
     @Override
     public Iterable<IMapping> mappings() {
         return new Iterable<IMapping>() {
 
             @Override
             public Iterator<IMapping> iterator() {
                 return new MappingIterator();
             }
         };
     }
 
     /**
      * The inner Mapping Iterator class.
      *
      */
     private class MappingIterator implements Iterator<IMapping> {
 
         private int pointer = 0;
 
         @Override
         public boolean hasNext() {
             return pointer < mappingCount;
         }
 
         @Override
         public IMapping next() {
             return map[pointer++];
         }
 
         @Override
         public void remove() {
             removeMapping(--pointer);
         }
 
     }
 
     /**
      * Adds a reactant to this reaction.
      *
      * @param reactant   Molecule added as reactant to this reaction
      * @see   #getReactants
      */
     @Override
     public void addReactant(IAtomContainer reactant) {
         addReactant(reactant, 1.0);
         /*
          * notifyChanged() is called by addReactant(Molecule reactant, double
          * coefficient)
          */
     }
 
     /**
      * Adds an agent to this reaction.
      *
      * @param agent   Molecule added as agent to this reaction
      * @see   #getAgents
      */
     @Override
     public void addAgent(IAtomContainer agent) {
         agents.addAtomContainer(agent);
         notifyChanged();
     }
 
     /**
      * Adds a reactant to this reaction with a stoichiometry coefficient.
      *
      * @param reactant    Molecule added as reactant to this reaction
      * @param coefficient Stoichiometry coefficient for this molecule
      * @see   #getReactants
      */
     @Override
     public void addReactant(IAtomContainer reactant, Double coefficient) {
         reactants.addAtomContainer(reactant, coefficient);
         notifyChanged();
     }
 
     /**
      * Adds a product to this reaction.
      *
      * @param product    Molecule added as product to this reaction
      * @see   #getProducts
      */
     @Override
     public void addProduct(IAtomContainer product) {
         this.addProduct(product, 1.0);
         /*
          * notifyChanged() is called by addProduct(Molecule product, double
          * coefficient)
          */
     }
 
     /**
      * Adds a product to this reaction.
      *
      * @param product     Molecule added as product to this reaction
      * @param coefficient Stoichiometry coefficient for this molecule
      * @see   #getProducts
      */
     @Override
     public void addProduct(IAtomContainer product, Double coefficient) {
         products.addAtomContainer(product, coefficient);
         /*
          * notifyChanged() is called by addReactant(Molecule reactant, double
          * coefficient)
          */
     }
 
     /**
      * Returns the stoichiometry coefficient of the given reactant.
      *
      * @param  reactant Reactant for which the coefficient is returned.
      * @return -1, if the given molecule is not a product in this Reaction
      * @see    #setReactantCoefficient
      */
     @Override
     public Double getReactantCoefficient(IAtomContainer reactant) {
         return reactants.getMultiplier(reactant);
     }
 
     /**
      * Returns the stoichiometry coefficient of the given product.
      *
      * @param  product Product for which the coefficient is returned.
      * @return -1, if the given molecule is not a product in this Reaction
      * @see    #setProductCoefficient
      */
     @Override
     public Double getProductCoefficient(IAtomContainer product) {
         return products.getMultiplier(product);
     }
 
     /**
      * Sets the coefficient of a a reactant to a given value.
      *
      * @param   reactant    Reactant for which the coefficient is set
      * @param   coefficient The new coefficient for the given reactant
      * @return  true if Molecule has been found and stoichiometry has been set.
      * @see     #getReactantCoefficient
      */
     @Override
     public boolean setReactantCoefficient(IAtomContainer reactant, Double coefficient) {
         boolean result = reactants.setMultiplier(reactant, coefficient);
         notifyChanged();
         return result;
     }
 
     /**
      * Sets the coefficient of a a product to a given value.
      *
      * @param   product     Product for which the coefficient is set
      * @param   coefficient The new coefficient for the given product
      * @return  true if Molecule has been found and stoichiometry has been set.
      * @see     #getProductCoefficient
      */
     @Override
     public boolean setProductCoefficient(IAtomContainer product, Double coefficient) {
         boolean result = products.setMultiplier(product, coefficient);
         notifyChanged();
         return result;
     }
 
     /**
      * Returns an array of double with the stoichiometric coefficients
      * of the reactants.
      *
      * @return An array of double's containing the coefficients of the reactants
      * @see    #setReactantCoefficients
      */
     @Override
     public Double[] getReactantCoefficients() {
         return reactants.getMultipliers();
     }
 
     /**
      * Returns an array of double with the stoichiometric coefficients
      * of the products.
      *
      * @return An array of double's containing the coefficients of the products
      * @see    #setProductCoefficients
      */
     @Override
     public Double[] getProductCoefficients() {
         return products.getMultipliers();
     }
 
     /**
      * Sets the coefficients of the reactants.
      *
      * @param   coefficients An array of double's containing the coefficients of the reactants
      * @return  true if coefficients have been set.
      * @see     #getReactantCoefficients
      */
     @Override
     public boolean setReactantCoefficients(Double[] coefficients) {
         boolean result = reactants.setMultipliers(coefficients);
         notifyChanged();
         return result;
     }
 
     /**
      * Sets the coefficient of the products.
      *
      * @param   coefficients An array of double's containing the coefficients of the products
      * @return  true if coefficients have been set.
      * @see     #getProductCoefficients
      */
     @Override
     public boolean setProductCoefficients(Double[] coefficients) {
         boolean result = products.setMultipliers(coefficients);
         notifyChanged();
         return result;
     }
 
     /**
      * Sets the direction of the reaction.
      *
      * @param direction The new reaction direction
      * @see   #getDirection
      */
     @Override
     public void setDirection(IReaction.Direction direction) {
         reactionDirection = direction;
         notifyChanged();
     }
 
     /**
      * Returns the direction of the reaction.
      *
      * @return The direction of this reaction (FORWARD, BACKWARD or BIDIRECTIONAL).
      * @see    org.openscience.cdk.interfaces.IReaction.Direction
      * @see    #setDirection
      */
     @Override
     public IReaction.Direction getDirection() {
         return reactionDirection;
     }
 
 
/** Adds a mapping between the reactant and product side to this  Reaction. */

public void addMapping(IMapping mapping) {
    if (mappingCount >= map.length) {
        // Increase the size of the map array if it is full
        IMapping[] newMap = new IMapping[map.length + growArraySize];
        System.arraycopy(map, 0, newMap, 0, map.length);
        map = newMap;
    }
    map[mappingCount++] = mapping;
    notifyChanged();
}
 

}