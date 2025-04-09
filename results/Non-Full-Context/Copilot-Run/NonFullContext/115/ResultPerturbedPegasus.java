/*
  * Copyright (c) 2013 European Bioinformatics Institute (EMBL-EBI)
  *                    John May <jwmay@users.sf.net>
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
 
 package org.openscience.cdk.isomorphism;
 
 import org.openscience.cdk.graph.GraphUtil;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.isomorphism.matchers.IQueryAtomContainer;
 
 import java.util.Iterator;
 import java.util.Map;
 
 import static org.openscience.cdk.graph.GraphUtil.EdgeToBondMap;
 
 /**
  * A structure pattern which utilises the Ullmann algorithm {@cdk.cite
  * Ullmann76}.
  *
  * 
  *
  * Find and count the number molecules which contain the query substructure.
  *
  * <blockquote><pre>
  * IAtomContainer query   = ...;
  * Pattern        pattern = Ullmann.findSubstructure(query);
  *
  * int hits = 0;
  * for (IAtomContainer m : ms)
  *     if (pattern.matches(m))
  *         hits++;
  * </pre></blockquote>
  * 
  *
  * Finding the matching to molecules which contain the query substructure. It is
  * more efficient to obtain the {@link #match} and check it's size rather than
  * test if it {@link #matches} first. These methods automatically verify
  * stereochemistry.
  *
  * <blockquote><pre>{@code
  * IAtomContainer query   = ...;
  * Pattern        pattern = Ullmann.findSubstructure(query);
  *
  * int hits = 0;
  * for (IAtomContainer m : ms) {
  *     int[] match = pattern.match(m);
  *     if (match.length > 0)
  *         hits++;
  * }
  * }</pre></blockquote>
  *
  * @author John May
  * @cdk.module isomorphism
  */
 public final class Ullmann extends Pattern {
 
     /** The query structure. */
     private final IAtomContainer query;
 
     /** The query structure adjacency list. */
     private final int[][]        g1;
 
     /** The bonds of the query structure. */
     private final EdgeToBondMap  bonds1;
 
     /** The atom matcher to determine atom feasibility. */
     private final AtomMatcher    atomMatcher;
 
     /** The bond matcher to determine atom feasibility. */
     private final BondMatcher    bondMatcher;
 
     /**
      * Non-public constructor for-now the atom/bond semantics are fixed.
      *
      * @param query       the query structure
      * @param atomMatcher how atoms should be matched
      * @param bondMatcher how bonds should be matched
      */
     private Ullmann(IAtomContainer query, AtomMatcher atomMatcher, BondMatcher bondMatcher) {
         this.query = query;
         this.atomMatcher = atomMatcher;
         this.bondMatcher = bondMatcher;
         this.bonds1 = EdgeToBondMap.withSpaceFor(query);
         this.g1 = GraphUtil.toAdjList(query, bonds1);
         determineFilters(query);
     }
 
     @Override
     public int[] match(IAtomContainer target) {
         return matchAll(target).first();
     }
 
     @Override
     public Mappings matchAll(IAtomContainer target) {
         EdgeToBondMap bonds2 = EdgeToBondMap.withSpaceFor(target);
         int[][] g2 = GraphUtil.toAdjList(target, bonds2);
         Iterable<int[]> iterable = new UllmannIterable(query, target, g1, g2, bonds1, bonds2, atomMatcher, bondMatcher);
         Mappings mappings = new Mappings(query, target, iterable);
         return filter(mappings, query, target);
     }
 
 
/** A pattern can be created to find the @code query structure. */
 public static Pattern findSubstructure(IAtomContainer query){
        return new Ullmann(query, new UniversalAtomMatcher(), new UniversalBondMatcher());      
 }

 

}