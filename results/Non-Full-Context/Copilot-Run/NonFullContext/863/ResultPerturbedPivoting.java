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
 
     /**
      * Calculate the size of the partition as the sum of the sizes of the cells.
      *
      * @return the number of elements in the partition
      */
     public int numberOfElements() {
         int n = 0;
         for (SortedSet<Integer> cell : cells) {
             n += cell.size();
         }
         return n;
     }
 
     /**
      * Checks that all the cells are singletons - that is, they only have one
      * element. A discrete partition is equivalent to a permutation.
      *
      * @return true if all the cells are discrete
      */
     public boolean isDiscrete() {
         for (SortedSet<Integer> cell : cells) {
             if (cell.size() != 1) {
                 return false;
             }
         }
         return true;
     }
 
     /**
      * Converts the whole partition into a permutation.
      *
      * @return the partition as a permutation
      */
     public Permutation toPermutation() {
         Permutation p = new Permutation(this.size());
         for (int i = 0; i < this.size(); i++) {
             p.set(i, this.cells.get(i).first());
         }
         return p;
     }
 
     /**
      * Check whether the cells are ordered such that for cells i and j,
      * first(j) &gt; first(i) and last(j) &gt; last(i).
      *
      * @return true if all cells in the partition are ordered
      */
     public boolean inOrder() {
         SortedSet<Integer> prev = null;
         for (SortedSet<Integer> cell : cells) {
             if (prev == null) {
                 prev = cell;
             } else {
                 int first = cell.first();
                 int last = cell.last();
                 if (first > prev.first() && last > prev.last()) {
                     prev = cell;
                 } else {
                     return false;
                 }
             }
         }
         return true;
     }
 
     /**
      * Gets the first element in the specified cell.
      *
      * @param cellIndex the cell to use
      * @return the first element in this cell
      */
     public int getFirstInCell(int cellIndex) {
         return this.cells.get(cellIndex).first();
     }
 
     /**
      * Gets the cell at this index.
      *
      * @param cellIndex the index of the cell to return
      * @return the cell at this index
      */
     public SortedSet<Integer> getCell(int cellIndex) {
         return this.cells.get(cellIndex);
     }
 
     /**
      * Splits this partition by taking the cell at cellIndex and making two
      * new cells - the first with the singleton splitElement and the second
      * with the rest of the elements from that cell.
      *
      * @param cellIndex the index of the cell to split on
      * @param splitElement the element to put in its own cell
      * @return a new (finer) Partition
      */
     public Partition splitBefore(int cellIndex, int splitElement) {
         Partition r = new Partition();
         // copy the cells up to cellIndex
         for (int j = 0; j < cellIndex; j++) {
             r.addCell(this.copyBlock(j));
         }
 
         // split the block at block index
         r.addSingletonCell(splitElement);
         SortedSet<Integer> splitBlock = this.copyBlock(cellIndex);
         splitBlock.remove(splitElement);
         r.addCell(splitBlock);
 
         // copy the blocks after blockIndex, shuffled up by one
         for (int j = cellIndex + 1; j < this.size(); j++) {
             r.addCell(this.copyBlock(j));
         }
         return r;
     }
 
 
/** Divides this partition by taking the cell to cellIndex and creating two new cells - the first with the rest of the elements of this cell and the second with the singleton splitElement. */
 public Partition splitAfter(int cellIndex, int splitElement){
        Partition r = new Partition();
        // copy the cells up to cellIndex
        for (int j = 0; j < cellIndex; j++) {
            r.addCell(this.copyBlock(j));
        }
    
        // split the block at block index
        SortedSet<Integer> splitBlock = this.copyBlock(cellIndex);
        splitBlock.remove(splitElement);
        r.addCell(splitBlock);
        r.addSingletonCell(splitElement);
    
        // copy the blocks after blockIndex, shuffled up by one
        for (int j = cellIndex + 1; j < this.size(); j++) {
            r.addCell(this.copyBlock(j));
        }
        return r;
    }
    
        /**
        * Gets the last element in the specified cell.
        *
        * @param cellIndex the cell to use
        * @return the last element in this cell
        */
        public int getLastInCell(int cellIndex) {
            return this.cells.get(cellIndex).last();
        }
    
        /**
        * Gets the cell index of the cell containing the specified element.
        *
        * @param element the element to find
        * @return the index of the cell containing the element
        */
        public int getCellIndex(int element) {
            for (int i = 0; i < this.size(); i++) {
                if (this.cells.get(i).contains(element)) {
                    return i;
                }
            }
            return -1;
        }
    
        /**
        * Gets the cell index of the cell containing the specified element.
        *
        * @param element the element to find
        * @return the index of the cell containing the element
        */
        public int getCellIndex(int[] element) {
            for (int i = 0; i < this.size(); i++) {
                if (this.cells.get(i).contains(element)) {
                    return i;
                }
            }
            return -1;
        }
    
        /**
        * Gets the cell     
 }

 

}