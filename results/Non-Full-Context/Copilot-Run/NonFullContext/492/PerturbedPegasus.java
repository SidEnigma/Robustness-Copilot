/* *********************************************************************** *
  * project: org.matsim.*
  * FastRemovePriorityQueue.java
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2009 by the members listed in the COPYING,        *
  *                   LICENSE and WARRANTY file.                            *
  * email           : info at matsim dot org                                *
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  *   This program is free software; you can redistribute it and/or modify  *
  *   it under the terms of the GNU General Public License as published by  *
  *   the Free Software Foundation; either version 2 of the License, or     *
  *   (at your option) any later version.                                   *
  *   See also COPYING, LICENSE and WARRANTY file                           *
  *                                                                         *
  * *********************************************************************** */
 
 package org.matsim.core.utils.collections;
 
 import java.io.Serializable;
 import java.util.Comparator;
 import java.util.Iterator;
 import java.util.LinkedHashMap;
 import java.util.Map;
 import java.util.PriorityQueue;
 
 /**
  * A simple re-implementation of a priority queue that offers a much better
  * performance for {@link #remove(Object) remove} operations than the standard
  * {@link PriorityQueue}, but uses more memory to manage the data.<br />
  *
  * The actual implementation is backed by a standard {@link PriorityQueue}. Each
  * added object is encapsulated in a simple structure together with a valid-flag.
  * If an element should be removed, the valid-flag is invalidated but the element
  * remains in the collection, foregoing the expensive linear remove operation
  * from the standard <tt>PriorityQueue</tt> implementation. Polling for elements
  * makes sure only valid elements are returned, all others are just ignored.
  *
  * @param <E> the type of elements held in this collection
  *
  * @see PriorityQueue
  *
  * @author mrieser
  */
 public class PseudoRemovePriorityQueue<E> implements RouterPriorityQueue<E> {
 
 	private final PriorityQueue<PseudoEntry<E>> delegate;
 	/*package*/ final Map<E, PseudoEntry<E>> lastEntry;
 
 	public PseudoRemovePriorityQueue(final int initialCapacity) {
 		this.delegate = new PriorityQueue<PseudoEntry<E>>(initialCapacity, new PseudoComparator<E>());
 		this.lastEntry = new LinkedHashMap<E, PseudoEntry<E>>(initialCapacity);
 //		this.lastEntry = new IdentityHashMap<E, PseudoEntry<E>>(initialCapacity);
 	}
 
 
/** The specified element is added to the priority queue. */
 public boolean add(final E o, final double priority){}

 

}