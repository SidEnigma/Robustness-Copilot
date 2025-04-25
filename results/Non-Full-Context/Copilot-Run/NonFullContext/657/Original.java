/*
  * Copyright (c) 2015 John May <jwmay@users.sf.net>
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as published by
  * the Free Software Foundation; either version 2.1 of the License, or (at
  * your option) any later version. All we ask is that proper credit is given
  * for our work, which includes - but is not limited to - adding the above
  * copyright notice to the beginning of your source code files, and to any
  * copyright notice that you may distribute with programs based on this work.
  *
  * This program is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  * License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 U
  */
 package org.openscience.cdk.forcefield.mmff;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.config.Elements;
 import org.openscience.cdk.graph.GraphUtil;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemObject;
 
 import java.math.BigDecimal;
 import java.util.ArrayDeque;
 import java.util.Deque;
 import java.util.HashSet;
 import java.util.Set;
 
 /**
  * Facade to access Merck Molecular Force Field (MMFF) functions.
  * 
  * <ul>
  *     <li>{@cdk.cite Halgren96a}</li>
  *     <li>{@cdk.cite Halgren96b}</li>
  *     <li>{@cdk.cite Halgren96c}</li>
  *     <li>{@cdk.cite Halgren96d}</li>
  *     <li>{@cdk.cite Halgren96e}</li>     
  * </ul>
  *
  * <br>
  * <b>Atom Types</b>
  * 
  * Symbolic atom types are assigned with {@link Mmff#assignAtomTypes(IAtomContainer)}.
  * The atom type name can be accessed with {@link IAtom#getAtomTypeName()}.
  *
  * <br>
  * <b>Partial Charges</b>
  * 
  * Partial charges are assigned with {@link Mmff#partialCharges(IAtomContainer)}.
  * Atom types must be assigned before calling this function. Effective formal
  * charges can also be obtained with {@link Mmff#effectiveCharges(IAtomContainer)}
  * both charge values are accessed with {@link IAtom#getCharge()}. Atoms of
  * unknown type are assigned a neutral charge - to avoid this check the return
  * value of {@link Mmff#assignAtomTypes(IAtomContainer)}.
  * 
  * <pre>{@code
  * IAtomContainer mol = ...;
  * 
  * Mmff mmff = new Mmff();
  * mmff.assignAtomTypes(mol);
  * mmff.partialCharges(mol);
  * mmff.clearProps(mol); // optional
  * }</pre>
  * 
  * @author John May
  * @cdk.githash
  */
 public class Mmff {
 
     private static final String MMFF_ADJLIST_CACHE = "mmff.adjlist.cache";
     private static final String MMFF_EDGEMAP_CACHE = "mmff.edgemap.cache";
     private static final String MMFF_AROM          = "mmff.arom";
 
     private final MmffAtomTypeMatcher mmffAtomTyper = new MmffAtomTypeMatcher();
     private final MmffParamSet        mmffParamSet  = MmffParamSet.INSTANCE;
     
     /**
      * Assign MMFF Symbolic atom types. The symbolic type can be accessed with
      * {@link IAtom#getAtomTypeName()}. An atom of unknown type is assigned the
      * symbolic type {@code 'UNK'}. 
      * All atoms, including hydrogens must be explicitly represented.
      *
      * @param mol molecule
      * @return all atoms had a type assigned
      */
     public boolean assignAtomTypes(IAtomContainer mol) {
 
         // preconditions need explicit hydrogens
         for (IAtom atom : mol.atoms()) {
             if (atom.getImplicitHydrogenCount() == null || atom.getImplicitHydrogenCount() > 0)
                 throw new IllegalArgumentException("Hydrogens must be explicit nodes, each must have a zero (non-null) impl H count.");
         }
 
         // conversion to faster data structures
         GraphUtil.EdgeToBondMap edgeMap = GraphUtil.EdgeToBondMap.withSpaceFor(mol);
         int[][] adjList = GraphUtil.toAdjList(mol, edgeMap);
 
         mol.setProperty(MMFF_ADJLIST_CACHE, adjList);
         mol.setProperty(MMFF_EDGEMAP_CACHE, edgeMap);
 
         Set<IBond> aromBonds = new HashSet<>();
 
         Set<IChemObject> oldArom = getAromatics(mol);
 
         // note: for MMFF we need to remove current aromatic flags for type
         // assignment (they are restored after)
         for (IChemObject chemObj : oldArom)
             chemObj.setFlag(CDKConstants.ISAROMATIC, false);
         String[] atomTypes = mmffAtomTyper.symbolicTypes(mol, adjList, edgeMap, aromBonds);
 
         boolean hasUnkType = false;
         for (int i = 0; i < mol.getAtomCount(); i++) {
             if (atomTypes[i] == null) {
                 mol.getAtom(i).setAtomTypeName("UNK");
                 hasUnkType = true;
             }
             else {
                 mol.getAtom(i).setAtomTypeName(atomTypes[i]);
             }
         }
 
         // restore aromatic flags and mark the MMFF aromatic bonds
         for (IChemObject chemObj : oldArom)
             chemObj.setFlag(CDKConstants.ISAROMATIC, true);
         for (IBond bond : aromBonds)
             bond.setProperty(MMFF_AROM, true);
 
         return !hasUnkType;
     }
 
     /**
      * Assign the effective formal charges used by MMFF in calculating the
      * final partial charge values. Atom types must be assigned first. All 
      * existing charges are cleared.
      * 
      * @param mol molecule
      * @return charges were assigned
      * @see #partialCharges(IAtomContainer) 
      * @see #assignAtomTypes(IAtomContainer) 
      */
     public boolean effectiveCharges(IAtomContainer mol) {
 
         int[][] adjList = mol.getProperty(MMFF_ADJLIST_CACHE);
         GraphUtil.EdgeToBondMap edgeMap = mol.getProperty(MMFF_EDGEMAP_CACHE);
 
         if (adjList == null || edgeMap == null)
             throw new IllegalArgumentException("Invoke assignAtomTypes first.");
 
         primaryCharges(mol, adjList, edgeMap);
         effectiveCharges(mol, adjList);
 
         return true;
     }
 
 
/** Assign the partial charges, all existing charges are cleared. */
 public boolean partialCharges(IAtomContainer mol){}

 

}