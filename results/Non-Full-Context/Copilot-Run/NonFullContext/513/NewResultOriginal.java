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
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Queue;
 import java.util.Set;
 import java.util.SortedSet;
 import java.util.TreeSet;
 
 /**
  * Refines a 'coarse' partition (with more blocks) to a 'finer' partition that
  * is equitable.
  *
  * Closely follows algorithm 7.5 in CAGES {@cdk.cite Kreher98}. The basic idea is that the refiner
  * maintains a queue of blocks to refine, starting with all the initial blocks
  * in the partition to refine. These blocks are popped off the queue, and
  *
  * @author maclean
  * @cdk.module group
  */
 class EquitablePartitionRefiner {
     
     private final Refinable refinable;
 
     /**
      * A forward split order tends to favor partitions where the cells are
      * refined from lowest to highest. A reverse split order is, of course, the
      * opposite.
      *
      */
     public enum SplitOrder {
         FORWARD, REVERSE
     };
 
     /**
      * The bias in splitting cells when refining
      */
     private SplitOrder          splitOrder = SplitOrder.FORWARD;
 
     /**
      * The block of the partition that is being refined
      */
     private int                 currentBlockIndex;
 
     /**
      * The blocks to be refined, or at least considered for refinement
      */
     private Queue<Set<Integer>> blocksToRefine;
 
     public EquitablePartitionRefiner(Refinable refinable) {
         this.refinable = refinable;
     }
     
     /**
      * Set the preference for splitting cells.
      *
      * @param splitOrder either FORWARD or REVERSE
      */
     public void setSplitOrder(SplitOrder splitOrder) {
         this.splitOrder = splitOrder;
     }
 
 
/** Refines the coarse partition <code>a</code> into a finer one. */

public Partition refine(Partition coarser) {
    Partition finer = new Partition(coarser.size());
    List<Integer> order = new ArrayList<>(coarser.size());
    for (int i = 0; i < coarser.size(); i++) {
        order.add(i);
    }
    if (splitOrder == SplitOrder.REVERSE) {
        Collections.reverse(order);
    }
    currentBlockIndex = 0;
    blocksToRefine = new LinkedList<>();
    blocksToRefine.add(coarser.getBlocks());
    while (!blocksToRefine.isEmpty()) {
        Set<Integer> block = blocksToRefine.poll();
        if (block.size() == 1) {
            finer.addToBlock(currentBlockIndex, block.iterator().next());
        } else {
            Map<Integer, SortedSet<Integer>> neighbors = new HashMap<>();
            for (int vertex : block) {
                neighbors.put(vertex, new TreeSet<>());
            }
            for (int vertex : block) {
                for (int neighbor : refinable.getNeighbors(vertex)) {
                    if (block.contains(neighbor)) {
                        neighbors.get(vertex).add(neighbor);
                    }
                }
            }
            List<Integer> vertices = new ArrayList<>(block);
            vertices.sort((v1, v2) -> neighbors.get(v1).size() - neighbors.get(v2).size());
            int splitIndex = vertices.size() / 2;
            Set<Integer> block1 = new TreeSet<>(vertices.subList(0, splitIndex));
            Set<Integer> block2 = new TreeSet<>(vertices.subList(splitIndex, vertices.size()));
            blocksToRefine.add(block1);
            blocksToRefine.add(block2);
            finer.addToBlock(currentBlockIndex, block1);
            finer.addToBlock(currentBlockIndex + 1, block2);
            currentBlockIndex += 2;
        }
    }
    return finer;
}
 

}