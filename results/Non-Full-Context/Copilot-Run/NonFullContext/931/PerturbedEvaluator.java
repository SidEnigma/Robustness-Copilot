/* *********************************************************************** *
  * project: org.matsim.*
  * QuadTree.java
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
 import java.util.*;
 
 /**
  * An implementation of a QuadTree to store data assigned to geometric points.
  * The expected bounds of all added points must be given to the constructor for
  * working properly. Correct functioning of the QuadTree with elements being
  * added outside of the given bounds cannot be guaranteed.<br />
  * At one location, several different objects can be put. An object can be put
  * to the QuadTree at different locations. But an object cannot be put more than
  * once at the same location.
  *
  * @author mrieser
  * @param <T> The type of data to be stored in the QuadTree.
  */
 public class QuadTree<T> implements Serializable {
 
 	private static final long serialVersionUID = 1L;
 
 	/** The top node or root of the tree */
 	protected Node<T> top = null;
 
 	/** The number of entries in the tree */
 	private int size = 0;
 
 	/** The number of structural modifications to the tree. */
 	private transient int modCount = 0;
 
 	/**
 	 * A cache to store all values of the QuadTree so it does not have to be
 	 * computed for every call to {@link #values()}. This is similar to
 	 * TreeMap.java and AbstractMap.java
 	 */
 	transient volatile Collection<T> values = null;
 
 	private void incrementSize() { this.modCount++; this.size++; this.values = null; }
 	private void decrementSize() { this.modCount++; this.size--; this.values = null; }
 
 	/**
 	 * Creates an empty QuadTree with the bounds minX/minY -- maxX/maxY. For
 	 * optimal performance, all points should be evenly distributed within this
 	 * rectangle.
 	 *
 	 * @param minX The smallest x coordinate (easting, longitude) expected
 	 * @param minY The smallest y coordinate (northing, latitude) expected
 	 * @param maxX The largest x coordinate (easting, longitude) expected
 	 * @param maxY The largest y coordinate (northing, latitude) expected
 	 */
 	public QuadTree(final double minX, final double minY, final double maxX, final double maxY) {
 		this.top = new Node<T>(minX, minY, maxX, maxY);
 	}
 
 
/** In this QuadTree, a connection is made between the supplied value and the specified coordinates. */
 public boolean put(final double x, final double y, final T value){}

 

}