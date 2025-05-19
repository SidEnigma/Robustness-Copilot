/* Copyright (C) 2004-2007  The Chemistry Development Kit (CDK) project
  *
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 2.1 of the License, or (at your option) any later version.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this library; if not, write to the Free Software
  * Foundation, 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  * (or see http://www.gnu.org/copyleft/lesser.html)
  */
 package org.openscience.cdk.smiles.smarts.parser;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.ReactionRole;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.interfaces.ITetrahedralChirality;
 import org.openscience.cdk.isomorphism.matchers.IQueryAtom;
 import org.openscience.cdk.isomorphism.matchers.IQueryAtomContainer;
 import org.openscience.cdk.isomorphism.matchers.IQueryBond;
 import org.openscience.cdk.isomorphism.matchers.QueryAtomContainer;
 import org.openscience.cdk.isomorphism.matchers.smarts.AliphaticAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.AliphaticSymbolAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.AnyAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.AnyOrderQueryBond;
 import org.openscience.cdk.isomorphism.matchers.smarts.AromaticAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.AromaticOrSingleQueryBond;
 import org.openscience.cdk.isomorphism.matchers.smarts.AromaticQueryBond;
 import org.openscience.cdk.isomorphism.matchers.smarts.AromaticSymbolAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.AtomicNumberAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.ChiralityAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.ExplicitConnectionAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.FormalChargeAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.HybridizationNumberAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.HydrogenAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.ImplicitHCountAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.LogicalOperatorAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.LogicalOperatorBond;
 import org.openscience.cdk.isomorphism.matchers.smarts.MassAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.NonCHHeavyAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.OrderQueryBond;
 import org.openscience.cdk.isomorphism.matchers.smarts.PeriodicGroupNumberAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.ReactionRoleQueryAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.RecursiveSmartsAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.RingBond;
 import org.openscience.cdk.isomorphism.matchers.smarts.RingIdentifierAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.RingMembershipAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.SMARTSAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.SMARTSBond;
 import org.openscience.cdk.isomorphism.matchers.smarts.SmallestRingAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.StereoBond;
 import org.openscience.cdk.isomorphism.matchers.smarts.TotalConnectionAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.TotalHCountAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.TotalRingConnectionAtom;
 import org.openscience.cdk.isomorphism.matchers.smarts.TotalValencyAtom;
 import org.openscience.cdk.stereo.DoubleBondStereochemistry;
 import org.openscience.cdk.stereo.TetrahedralChirality;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.BitSet;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import static org.openscience.cdk.interfaces.IDoubleBondStereochemistry.Conformation;
 
 /**
  * An AST tree visitor. It builds an instance of <code>QueryAtomContainer</code>
  * from the AST tree.
  *
  * To use this visitor:
  * <pre>
  * SMARTSParser parser = new SMARTSParser(new java.io.StringReader("C*C"));
  * ASTStart ast = parser.start();
  * SmartsQueryVisitor visitor = new SmartsQueryVisitor();
  * QueryAtomContainer query = visitor.visit(ast, null);
  * </pre>
  *
  * @author Dazhi Jiao
  * @cdk.created 2007-04-24
  * @cdk.module smarts
  * @cdk.githash
  * @cdk.keyword SMARTS AST
  */
 @Deprecated
 public class SmartsQueryVisitor implements SMARTSParserVisitor {
 
     // current atoms with a ring identifier
     private RingIdentifierAtom[]                ringAtoms;
 
     // query
     private IQueryAtomContainer                 query;
 
     private final IChemObjectBuilder            builder;
 
     /**
      * Maintain order of neighboring atoms - required for atom-based
      * stereochemistry.
      */
     private Map<IAtom, List<IAtom>>             neighbors      = new HashMap<IAtom, List<IAtom>>();
 
     /**
      * Lookup of atom indices.
      */
     private BitSet                              tetrahedral    = new BitSet();
 
     /**
      * Stores the directional '/' or '\' bonds. Speeds up looking for double
      * bond configurations.
      */
     private List<IBond>                         stereoBonds    = new ArrayList<IBond>();
 
     /**
      * Stores the double bonds in the query.
      */
     private List<IBond>                         doubleBonds    = new ArrayList<IBond>();
 
     public SmartsQueryVisitor(IChemObjectBuilder builder) {
         this.builder = builder;
     }
 
     public Object visit(ASTRingIdentifier node, Object data) {
         IQueryAtom atom = (IQueryAtom) data;
         RingIdentifierAtom ringIdAtom = new RingIdentifierAtom(builder);
         ringIdAtom.setAtom(atom);
         IQueryBond bond;
         if (node.jjtGetNumChildren() == 0) { // implicit bond
             bond = null;
         } else {
             bond = (IQueryBond) node.jjtGetChild(0).jjtAccept(this, data);
         }
         ringIdAtom.setRingBond(bond);
         return ringIdAtom;
     }
 
     public Object visit(ASTAtom node, Object data) {
         IQueryAtom atom = (IQueryAtom) node.jjtGetChild(0).jjtAccept(this, data);
         for (int i = 1; i < node.jjtGetNumChildren(); i++) { // if there are ring identifiers
             throw new IllegalStateException();
         }
         return atom;
     }
 
     private void handleRingClosure(IQueryAtom atom, ASTRingIdentifier ringIdentifier) {
         RingIdentifierAtom ringIdAtom = (RingIdentifierAtom) ringIdentifier.jjtAccept(this, atom);
 
         // if there is already a RingIdentifierAtom, create a bond between
         // them and add the bond to the query
         int ringId = ringIdentifier.getRingId();
 
         // ring digit > 9 - expand capacity
         if (ringId >= ringAtoms.length) ringAtoms = Arrays.copyOf(ringAtoms, 100);
 
         // Ring Open
         if (ringAtoms[ringId] == null) {
             ringAtoms[ringId] = ringIdAtom;
             if (neighbors.containsKey(atom)) {
                 neighbors.get(atom).add(ringIdAtom);
             }
         }
 
         // Ring Close
         else {
             IQueryBond ringBond;
             // first check if the two bonds ma
             if (ringAtoms[ringId].getRingBond() == null) {
                 if (ringIdAtom.getRingBond() == null) {
                     if (atom instanceof AromaticSymbolAtom
                             && ringAtoms[ringId].getAtom() instanceof AromaticSymbolAtom) {
                         ringBond = new AromaticQueryBond(builder);
                     } else {
                         ringBond = new RingBond(builder);
                     }
                 } else {
                     ringBond = ringIdAtom.getRingBond();
                 }
             } else {
                 // Here I assume the bond are always same. This should be checked by the parser already
                 ringBond = ringAtoms[ringId].getRingBond();
             }
             ((IBond) ringBond).setAtoms(new IAtom[]{ringAtoms[ringId].getAtom(), atom});
             query.addBond((IBond) ringBond);
 
             // if the connected atoms was tracking neighbors, replace the
             // placeholder reference
             if (neighbors.containsKey(ringAtoms[ringId].getAtom())) {
                 List<IAtom> localNeighbors = neighbors.get(ringAtoms[ringId].getAtom());
                 localNeighbors.set(localNeighbors.indexOf(ringAtoms[ringId]), atom);
             }
             if (neighbors.containsKey(atom)) {
                 neighbors.get(atom).add(ringAtoms[ringId].getAtom());
             }
 
             ringAtoms[ringId] = null;
         }
     }
 
     private final static ILoggingTool logger = LoggingToolFactory.createLoggingTool(SmartsQueryVisitor.class);
 
     public Object visit(SimpleNode node, Object data) {
         return null;
     }
 
     public Object visit(ASTStart node, Object data) {
         return node.jjtGetChild(0).jjtAccept(this, data);
     }
 
     public Object visit(ASTReaction node, Object data) {
         IAtomContainer query = new QueryAtomContainer(builder);
         for (int grpIdx = 0; grpIdx < node.jjtGetNumChildren(); grpIdx++) {
 
             int rollback = query.getAtomCount();
 
             ASTGroup group = (ASTGroup) node.jjtGetChild(grpIdx);
             group.jjtAccept(this, query);
 
             // fill in the roles for newly create atoms
             if (group.getRole() != ASTGroup.ROLE_ANY) {
                 IQueryAtom roleQueryAtom = null;
                 ReactionRole role = null;
 
                 // use single instances
                 switch (group.getRole()) {
                     case ASTGroup.ROLE_REACTANT:
                         roleQueryAtom = ReactionRoleQueryAtom.RoleReactant;
                         role = ReactionRole.Reactant;
                         break;
                     case ASTGroup.ROLE_AGENT:
                         roleQueryAtom = ReactionRoleQueryAtom.RoleAgent;
                         role = ReactionRole.Agent;
                         break;
                     case ASTGroup.ROLE_PRODUCT:
                         roleQueryAtom = ReactionRoleQueryAtom.RoleProduct;
                         role = ReactionRole.Product;
                         break;
                 }
 
                 if (roleQueryAtom != null) {
                     while (rollback < query.getAtomCount()) {
                         IAtom org = query.getAtom(rollback);
                         IAtom rep = LogicalOperatorAtom.and(roleQueryAtom, (IQueryAtom) org);
                         // ensure AAM is propagated
                         rep.setProperty(CDKConstants.ATOM_ATOM_MAPPING, org.getProperty(CDKConstants.ATOM_ATOM_MAPPING));
                         rep.setProperty(CDKConstants.REACTION_ROLE, role);
                         AtomContainerManipulator.replaceAtomByAtom(query,
                                                                    org,
                                                                    rep);
                         rollback++;
                     }
                 }
             }
         }
         return query;
     }
 
     public Object visit(ASTGroup node, Object data) {
         IAtomContainer fullQuery = (IAtomContainer) data;
 
         if (fullQuery == null)
             fullQuery = new QueryAtomContainer(builder);
 
         // keeps track of component grouping
         int[] components = fullQuery.getProperty("COMPONENT.GROUPING") != null
                 ? fullQuery.getProperty("COMPONENT.GROUPING", int[].class)
                 : new int[0];
         int maxId = 0;
         if (components.length > 0) {
             for (int id : components)
                 if (id > maxId) maxId = id;
         }
 
         for (int i = 0; i < node.jjtGetNumChildren(); i++) {
             ASTSmarts smarts = (ASTSmarts) node.jjtGetChild(i);
             ringAtoms = new RingIdentifierAtom[10];
             query = new QueryAtomContainer(builder);
 
             smarts.jjtAccept(this, null);
 
             // update component info
             if (components.length > 0 || smarts.componentId() > 0) {
                 components = Arrays.copyOf(components, 1 + fullQuery.getAtomCount() + query.getAtomCount());
                 int id = smarts.componentId();
                 Arrays.fill(components, fullQuery.getAtomCount(), components.length, id);
                 if (id > maxId) maxId = id;
             }
 
             fullQuery.add(query);
         }
 
         // only store if there was a component grouping
         if (maxId > 0) {
             components[components.length - 1] = maxId; // we left space to store how many groups there were
             fullQuery.setProperty("COMPONENT.GROUPING", components);
         }
 
         // create tetrahedral elements
         for (IAtom atom : neighbors.keySet()) {
             List<IAtom> localNeighbors = neighbors.get(atom);
             if (localNeighbors.size() == 4) {
                 fullQuery.addStereoElement(new TetrahedralChirality(atom, localNeighbors.toArray(new IAtom[4]),
                         ITetrahedralChirality.Stereo.CLOCKWISE)); // <- to be modified later
             } else if (localNeighbors.size() == 5) {
                 localNeighbors.remove(atom); // remove central atom (which represented implicit part)
                 fullQuery.addStereoElement(new TetrahedralChirality(atom, localNeighbors.toArray(new IAtom[4]),
                         ITetrahedralChirality.Stereo.CLOCKWISE)); // <- to be modified later
             }
         }
 
         // for each double bond, find the stereo bonds. Currently doesn't
         // handle logical bonds i.e. C/C-,=C/C
         for (IBond bond : doubleBonds) {
             IAtom left = bond.getBegin();
             IAtom right = bond.getEnd();
             StereoBond leftBond = findStereoBond(left);
             StereoBond rightBond = findStereoBond(right);
             if (leftBond == null || rightBond == null) continue;
             Conformation conformation = leftBond.direction(left) == rightBond.direction(right) ? Conformation.TOGETHER
                     : Conformation.OPPOSITE;
             fullQuery.addStereoElement(new DoubleBondStereochemistry(bond, new IBond[]{leftBond, rightBond},
                     conformation));
         }
 
         return fullQuery;
     }
 
 
/** Find a stereo bond that is close to the atom. */
 private StereoBond findStereoBond(IAtom atom){
        for (StereoBond bond : stereoBonds) {
            if (bond.contains(atom)) return bond;
        }
        return null;
    }
    
        public Object visit(ASTSmarts node, Object data) {
            if (node.jjtGetNumChildren() == 0) {
                // this is a single atom
                IAtom atom = node.getAtom();
                if (atom != null) {
                    query.addAtom(atom);
                }
            } else {
                // this is a smarts query
                for (int i = 0; i < node.jjtGetNumChildren(); i++) {
                    node.jjtGetChild(i).jjtAccept(this, query);
                }
            }
            return null;
        }
    
        public Object visit(ASTOr node, Object data) {
            IAtomContainer query = (IAtomContainer) data;
            for (int i = 0; i < node.jjtGetNumChildren(); i++) {
                IAtomContainer subQuery = new QueryAtomContainer(builder);
                node.jjtGetChild(i).jjtAccept(this, subQuery);
                query.add(subQuery);
            }
            return null;
        }
    
        public Object visit(ASTAnd node, Object data) {
            IAtomContainer query = (IAtomContainer) data;
            for (int i = 0; i < node.jjtGetNumChildren(); i++) {
                IAtomContainer subQuery = new QueryAtomContainer(builder);
                node.jjtGetChild(i).jjtAccept(this, subQuery);
                query.add(subQuery);
            }
            return null;
        }
    
        public Object visit(ASTNot node, Object data) {
            IAtomContainer query = (IAtomContainer) data;
            IAtomContainer subQuery = new QueryAtomContainer(builder);
            node.jjtGetChild(0).jjtAccept(this, subQuery);
            query.add(subQuery);
            return null;
        }
    
        public Object       
 }

 

}