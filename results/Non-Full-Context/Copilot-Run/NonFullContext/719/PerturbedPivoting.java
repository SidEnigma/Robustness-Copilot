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
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.List;
 import java.util.SortedSet;
 import java.util.TreeSet;
 
 
 /**
  * A partition of a set of integers, such as the discrete partition {{1}, {2},
  * {3}, {4}} or the unit partition {{1, 2, 3, 4}} or an intermediate like {{1,
  * 2}, {3, 4}}.
  *
  * @author maclean
  * @cdk.module group
  */
 public class Partition {
 
     /**
      * The subsets of the partition, known as cells.
      */
     private List<SortedSet<Integer>> cells;
 
     /**
      * Creates a new, empty partition with no cells.
      */
     public Partition() {
         this.cells = new ArrayList<SortedSet<Integer>>();
     }
 
     /**
      * Copy constructor to make one partition from another.
      *
      * @param other the partition to copy
      */
     public Partition(Partition other) {
         this();
         for (SortedSet<Integer> block : other.cells) {
             this.cells.add(new TreeSet<Integer>(block));
         }
     }
 
     /**
      * Constructor to make a partition from an array of int arrays.
      *
      * @param cellData the partition to copy
      */
     public Partition(int[][] cellData) {
         this();
         for (int[] aCellData : cellData) {
             addCell(aCellData);
         }
     }
 
     /**
      * Create a unit partition - in other words, the coarsest possible partition
      * where all the elements are in one cell.
      *
      * @param size the number of elements
      * @return a new Partition with one cell containing all the elements
      */
     public static Partition unit(int size) {
         Partition unit = new Partition();
         unit.cells.add(new TreeSet<Integer>());
         for (int i = 0; i < size; i++) {
             unit.cells.get(0).add(i);
         }
         return unit;
     }
 
     /**
      *{@inheritDoc}
      */
     @Override
     public boolean equals(Object o) {
 
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
 
         Partition partition = (Partition) o;
 
         return cells != null ? cells.equals(partition.cells) : partition.cells == null;
 
     }
 
     /**
      *{@inheritDoc}
      */
     @Override
     public int hashCode() {
         return cells != null ? cells.hashCode() : 0;
     }
 
     /**
      * Gets the size of the partition, in terms of the number of cells.
      *
      * @return the number of cells in the partition
      */
     public int size() {
         return this.cells.size();
     }
 
 
/** Calculate the partition size as the sum of the cell sizes. */
 public int numberOfElements(){}

 

}