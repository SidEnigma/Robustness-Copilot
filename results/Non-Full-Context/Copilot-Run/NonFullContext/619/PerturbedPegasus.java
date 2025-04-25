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
 
 
 import java.util.Iterator;
 
 /**
  * Given a (subgraph-)isomorphism state this class can lazily iterate over the
  * mappings in a non-recursive manner. The class currently implements and {@link
  * Iterator} but is better suited to the {@code Stream} class (which will be
  * available in JDK 8).
  *
  * @author John May
  * @cdk.module isomorphism
  */
 final class StateStream implements Iterator<int[]> {
 
     /** A mapping state. */
     private final State          state;
 
     /** The stack replaces the call-stack in a recursive matcher. */
     private final CandidateStack stack;
 
     /** Current candidates. */
     private int                  n = 0, m = -1;
 
     /** The next mapping. */
     private int[]                next;
 
     /**
      * Create a stream for the provided state.
      *
      * @param state the state to stream over
      */
     StateStream(final State state) {
         this.state = state;
         this.stack = new CandidateStack(state.nMax());
         this.next = state.nMax() == 0 || state.mMax() == 0 ? null : findNext(); // first-mapping
     }
 
     /**{@inheritDoc} */
     @Override
     public boolean hasNext() {
         return next != null;
     }
 
     /**{@inheritDoc} */
     @Override
     public int[] next() {
         int[] ret = next;
         next = findNext();
         return ret;
     }
 
     /**{@inheritDoc} */
     @Override
     public void remove() {
         throw new UnsupportedOperationException("a graph matching cannot be removed");
     }
 
     /**
      * Finds the next mapping from the current state.
      *
      * @return the next state (or null if none)
      */
     private int[] findNext() {
         while (map());
         if (state.size() == state.nMax()) return state.mapping();
         return null;
     }
 
 
/** When a mapping is done, the function return false. */
 private boolean map(){}

 

}