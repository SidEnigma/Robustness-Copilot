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
 
 
/** The partition is refined into a fine one. */

public Partition refine(Partition coarser) {
    Partition finer = new Partition(coarser.getPartitionSize());
    List<Integer> atoms = new ArrayList<>(coarser.getPartitionSize());
    for (int i = 0; i < coarser.getPartitionSize(); i++) {
        atoms.add(i);
    }
    Collections.shuffle(atoms);
    
    blocksToRefine = new LinkedList<>();
    blocksToRefine.add(new TreeSet<>(atoms));
    
    while (!blocksToRefine.isEmpty()) {
        Set<Integer> block = blocksToRefine.poll();
        if (block.size() == 1) {
            finer.add(block.iterator().next());
        } else {
            List<Integer> blockList = new ArrayList<>(block);
            if (splitOrder == SplitOrder.REVERSE) {
                Collections.reverse(blockList);
            }
            currentBlockIndex = 0;
            while (currentBlockIndex < blockList.size()) {
                int currentAtom = blockList.get(currentBlockIndex);
                int nextAtom = blockList.get(currentBlockIndex + 1);
                if (!refinable.isRefinable(currentAtom, nextAtom)) {
                    finer.add(currentAtom);
                } else {
                    SortedSet<Integer> newBlock = new TreeSet<>();
                    newBlock.add(currentAtom);
                    while (refinable.isRefinable(currentAtom, nextAtom)) {
                        newBlock.add(nextAtom);
                        currentAtom = nextAtom;
                        currentBlockIndex++;
                        if (currentBlockIndex < blockList.size() - 1) {
                            nextAtom = blockList.get(currentBlockIndex + 1);
                        } else {
                            break;
                        }
                    }
                    blocksToRefine.add(newBlock);
                }
                currentBlockIndex++;
            }
        }
    }
    
    return finer;
}
 

}