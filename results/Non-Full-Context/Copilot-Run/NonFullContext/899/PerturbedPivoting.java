/* *********************************************************************** *
  * project: org.matsim.*
  * BinaryMinHeap.java
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
 
 package org.matsim.core.router.priorityqueue;
 
 import java.util.ConcurrentModificationException;
 import java.util.Iterator;
 import java.util.NoSuchElementException;
 
 /**
  * The (Wrapped)BinaryMinHeap replaces the PseudoRemovePriorityQueue as default
  * PriorityQueue. It offers a decreaseKey method and uses less memory. As a result,
  * the routing performance is increased by ~20%.
  * 
  * When an ArrayRoutingNetwork is used, an BinaryMinHeap can be used which uses
  * the getArrayIndex() method of the ArrayRoutingNetworkNodes which further reduces
  * the memory consumption and increases the performance by another ~10%.
  * 
  * @author cdobler
  * @author muelleki
  *
  * @param <E> the type of elements held in this collection
  */
 public class BinaryMinHeap<E extends HasIndex> implements MinHeap<E> {
 	
 	/**
 	 * Each HeapEntry contains a final integer value that points to a
 	 * position in the indices array. The value in the indices array points
 	 * to the position in the data and costs arrays where the HeapEntry
 	 * is currently located.
 	 * 
 	 * 	 * data objects, their indices and costs:	A(0, 5), B(1, 6), C(2, 7), D(3, 8)
 	 * 
 	 * 			[A][B][C][D]
 	 * indices:	[3][0][1][2]
 	 * 
 	 * data:	[B][C][D][A]
 	 * costs:	[6][7][8][5]
 	 * 
 	 * Example: 
 	 * Index of object A is 0. entry in indices array at position 0 is 3.
 	 * Therefore, the object is at index 3 in the data array.
 	 */
 	private final E[] data;
 	final double[] costs;
 	final int[] indices;
 	
 	private int heapSize;
 	private transient int modCount;
 	
 	/**
 	 *  The classic approach of removing the heap's head (poll) is to replace the 
 	 *  head with the heap's last entry. Afterwards this entry is sifted downwards
 	 *  until a valid position is reached. However, when doing so, two compare
 	 *  operations have to be performed for each level (comparing with left and right
 	 *  child).
 	 * 
 	 *  In the alternative approach, the head is sifted downwards to the left level in
 	 *  a special way. It replaces always the smaller ones of its children (only they
 	 *  are compared!). After reaching the bottom level, it its replaced with the heap's
 	 *  last entry. Finally, this entry is sifted upwards until a valid position is found.
 	 *  This approach should perform fewer compare operations than the classical approach.
 	 *  Idea: see http://magazin.c-plusplus.de/artikel/Binary%20Heaps
 	 * 
 	 */
 	private final boolean classicalRemove;
 	
 	private final int fanout;
 
 	/*package*/ static final int defaultFanout = 6;
 	
 	public BinaryMinHeap(int maxSize) {
 		this(maxSize, defaultFanout, false);
 	}
 
 	@SuppressWarnings("unchecked")
 	public BinaryMinHeap(int maxSize, int fanout, boolean classicalRemove) {
 		this.fanout = fanout;
 		this.classicalRemove = classicalRemove;
 
 		this.data = (E[]) new HasIndex[maxSize];
 
 		this.costs = new double[maxSize];
 		for (int i = 0; i < costs.length; i++) {
 			costs[i] = Double.MAX_VALUE;
 		}
 
 		this.indices = new int[maxSize];
 		for (int i = 0; i < indices.length; i++) {
 			indices[i] = -1;
 		}
 		this.heapSize = 0;
 		this.modCount = 0;
 	}
 
 	/**
 	 * Resets the queue to its initial state.
 	 */
 	@Override
 	public void reset() {
 		/*
 		 * For a small number of remaining entries in the heap, only removing
 		 * them might be faster than overwriting all entries. However, when doing so,
 		 * we have to do twice as much array accesses. 
 		 */
 		if (heapSize < indices.length / 10) {
 			for (int i = 0; i < heapSize; i++) {
 				indices[this.getIndex(data[i])] = -1;
 			}			
 		} else {
 			for (int i = 0; i < indices.length; i++) {
 				indices[i] = -1;
 			}
 		}
 		
 		for (int i = 0; i < heapSize; i++) {
 			costs[i] = Double.MAX_VALUE;
 		}
 		
 		this.heapSize = 0;
 		this.modCount = 0;
 	}
 	
 	@Override
 	public E peek() {
 		if (isEmpty()) return null;
 		else return peek(0);
 	}
 
 	private E peek(int index) {
 		return data[index];
 	}
 
 	/**
 	 * Retrieves and removes the head of this queue, or <tt>null</tt> if this
 	 * queue is empty.
 	 * 
 	 * @return the head of this queue, or <tt>null</tt> if this queue is empty.
 	 */
 	@Override
 	public E poll() {
 		E minValue;
 		if (isEmpty()) return null;
 		else {
 			this.modCount++;
 			minValue = data[0];
 			if (classicalRemove) {
 				data[0] = data[heapSize - 1];
 				costs[0] = costs[heapSize - 1];
 				indices[this.getIndex(data[0])] = 0;
 				indices[this.getIndex(minValue)] = -1;
 
 				heapSize--;
 				if (heapSize > 0) siftDown(0);
 			} else {
 				siftDownUp(0);
 
 				indices[this.getIndex(minValue)] = -1;
 			}
 			return minValue;
 		}
 	}
 
 	private void siftDownUp(int index) {
 		index = removeSiftDown(index);
 
 		// Swap entry with heap's last entry.
 		heapSize--;
 
 		// Sift up entry that was previously at the heap's end.
 		siftUp(index, data[heapSize], costs[heapSize]);
 
 		// Reset sentinel here:
 		costs[heapSize] = Double.MAX_VALUE;
 	}
 
 	/*
 	 * Used by alternative remove() approach. The costs have been set to
 	 * Double.MAX_VALUE. Therefore we only have to compare the node's children.
 	 */
 	private int removeSiftDown(int nodeIndex) {
 		while(true) {
 			int leftChildIndex = getLeftChildIndex(nodeIndex);
 			if (leftChildIndex >= heapSize) break;
 			
 			double leftCosts = costs[leftChildIndex];
 
 			int limitChildIndex = Math.min(leftChildIndex + fanout, heapSize);
 
 			for (int rightChildIndex = leftChildIndex + 1; rightChildIndex < limitChildIndex; rightChildIndex++) {
 				/*
 				 *  We use the sentinel values Double.MAX_VALUE to protect 
 				 *  ourselves from looking beyond the heap's true size
 				 */
 				double rightCosts = costs[rightChildIndex];
 				if (leftCosts >= rightCosts && 
 						(leftCosts > rightCosts || this.getIndex(data[leftChildIndex]) > this.getIndex(data[rightChildIndex]))) {
 					leftChildIndex = rightChildIndex;
 					leftCosts = rightCosts;
 				}
 			}
 
 			copyData(nodeIndex, leftChildIndex);
 			nodeIndex = leftChildIndex;
 		}
 
 		return nodeIndex;
 	}
 	
 	/**
 	 * Returns the number of elements in this priority queue.
 	 *
 	 * @return the number of elements in this collection.
 	 */
 	@Override
 	public int size() {
 		return this.heapSize;
 	}
 	
 	/**
 	 * Checks whether the queue is empty.
 	 *
 	 * @return <tt>true</tt> if the queue is empty.
 	 */
 	@Override
 	public boolean isEmpty() {
 		return (heapSize == 0);
 	}
 	
 	/**
 	 * Adds the specified element to this priority queue, with the given priority.
 	 * If the element is already present in the queue, it is not added a second
 	 * time.
 	 * @param value
 	 * @param priority
 	 * @return <tt>true</tt> if the element was added to the collection.
 	 */
 	@Override
 	public boolean add(E value, double priority) {
 		
 		if (value == null) {
 			throw new NullPointerException("null values are not supported!");
 		}
 		
 		// if the element is already present in the queue, return false
 		if (indices[this.getIndex(value)] >= 0) {
 			return false;
 		} else {			
 			if (heapSize == data.length) throw new RuntimeException("Heap's underlying storage is overflow!");			
 			
 			this.modCount++;
 			siftUp(heapSize, value, priority);
 			heapSize++;			
 			return true;
 		}		
 	}
 	
 	/**
 	 * Removes a single instance of the specified element from this
 	 * queue, if it is present.
 	 *
 	 * @return <tt>true</tt> if the queue contained the specified
 	 *         element.
 	 */
 	@Override
 	public boolean remove(E value) {
 		
 		if (value == null) return false;
 				
 		/*
 		 * Check the elements index. "-1" means that the element is not
 		 * present in the heap. 
 		 */
 		int index = indices[this.getIndex(value)];
 		if (index < 0) {
 			return false;
 		} else {
 			if (classicalRemove) {
 				// Move entry to heap's top and then remove the heap's head.
 				boolean decreasedKey = decreaseKey(value, Double.NEGATIVE_INFINITY);
 				if (decreasedKey && data[0] == value) {
 					this.poll();
 					return true;
 				} else return false;
 			} else {
 				siftDownUp(index);
 
 				// index has changed, therefore we cannot use "index" again
 				indices[this.getIndex(value)] = -1;
 				this.modCount++;
 				return true;
 			}
 		}
 	}
 	
 
/** Increases the priority (= decrease the double value given) of the element. */
 public boolean decreaseKey(E value, double cost){}

 

}