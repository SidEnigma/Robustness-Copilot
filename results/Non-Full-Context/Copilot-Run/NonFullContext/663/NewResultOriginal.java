/* Copyright (C) 2012  Gilleain Torrance <gilleain.torrance@gmail.com>
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
  */
 package org.openscience.cdk.group;
 
 import java.util.ArrayList;
 import java.util.List;
 
 
 /**
  * <p>
  * A permutation group with a Schreier-Sims representation. For a number n, a
  * list of permutation sets is stored (U0,...,Un-1). All n! permutations of
  * [0...n-1] can be reconstructed from this list by backtracking - see, for
  * example, the <a href="#generateAll">generateAll</a> method.
  * </p>
  *
  * <p>
  * So if G is a group on X = {0, 1, 2, 3, ..., n-1}, then:
  *
  * <blockquote>
  *      G<sub>0</sub> = {g &isin; G  : g(0) = 0} <br>
  *      G<sub>1</sub> = {g &isin; G<sub>0</sub> : g(1) = 1} <br>
  *      G<sub>2</sub> = {g &isin; G<sub>1</sub> : g(2) = 2} <br>
  *      ... <br>
  *      G<sub>n-1</sub> = {g in G<sub>n-2</sub> : g(n - 1) = n - 1} = {I} <br>
  * </blockquote>
  *
  * and G<sub>0</sub>, G<sub>1</sub>, G<sub>2</sub>, ..., G<sub>n-1</sub> are
  * subgroups of G.
  *
  * <p>
  * Now let orb(0) = {g(0) : g &isin; G} be the orbit of 0 under G. Then |orb(0)|
  * (the size of the orbit) is n<sub>0</sub> for some integer 0 &lt; n<sub>0</sub>
  * &le; n and write orb(0) = {x<sub>0,1</sub>, x<sub>0,2</sub>, ...,
  * x<sub>0,n₀</sub>} and for each i, 1 &le; i &le; n<sub>0</sub> choose
  * some h<sub>0,1</sub> in G such that h<sub>0,i</sub>(0) = x<sub>0,1</sub>. Set
  * U<sub>0</sub> = {h<sub>0,1</sub>, ..., h<sub>0,n₀</sub>}.
  * </p>
  *
  * <p>
  * Given the above, the list of permutation sets in this class is
  * [U<sub>0</sub>,..,U<sub>n</sub>]. Also, for all i = 1, ..., n-1 the set U<sub>i</sub> is
  * a left transversal of G<sub>i</sub> in G<sub>i-1</sub>.
  * </p>
  *
  * <p>
  * This is port of the code from the C.A.G.E.S. book {@cdk.cite Kreher98}. The
  * mathematics in the description above is also from that book (pp. 203).
  * </p>
  *
  * @author maclean
  * @cdk.module group
  *
  */
 public class PermutationGroup {
 
     /**
      * An interface for use with the apply method, which runs through all the
      * permutations in this group.
      *
      */
     public interface Backtracker {
 
         /**
          * Do something to the permutation
          *
          * @param p a permutation in the full group
          */
         public void applyTo(Permutation p);
 
         /**
          * Check to see if the backtracker is finished.
          *
          * @return true if complete
          */
         public boolean isFinished();
     }
 
     /**
      * The compact list of permutations that make up this group
      */
     private Permutation[][] permutations;
 
     /**
      * The size of the group - strictly, the size of the permutation
      */
     private final int       size;
 
     /**
      * The base of the group
      */
     private Permutation     base;
 
     /**
      * Make a group with just a single identity permutation of size n.
      *
      * @param size the number of elements in the base permutation
      */
     public PermutationGroup(int size) {
         this(new Permutation(size));
     }
 
     /**
      * Creates the initial group, with the base <code>base</code>.
      *
      * @param base the permutation that the group is based on
      */
     public PermutationGroup(Permutation base) {
         this.size = base.size();
         this.base = new Permutation(base);
         this.permutations = new Permutation[size][size];
         for (int i = 0; i < size; i++) {
             this.permutations[i][this.base.get(i)] = new Permutation(size);
         }
     }
 
     /**
      * Creates a group from a set of generators. See the makeSymN method for
      * where this is used to make the symmetric group on N using the two
      * generators (0, 1) and (1, 2, ..., n - 1, 0)
      *
      * @param size the size of the group
      * @param generators the generators to use to make the group
      */
     public PermutationGroup(int size, List<Permutation> generators) {
         this(new Permutation(size));
         for (Permutation generator : generators) {
             this.enter(generator);
         }
     }
 
 
/** Make the symmetric group Sym(N) for N. */

public static PermutationGroup makeSymN(int size) {
    List<Permutation> generators = new ArrayList<>();
    for (int i = 0; i < size - 1; i++) {
        generators.add(new Permutation(new int[]{i, i + 1}));
    }
    return new PermutationGroup(size, generators);
}
 

}