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
 
 	/**
 	 * Associates the specified value with the specified coordinates in this
 	 * QuadTree.
 	 *
 	 * @param x x-coordinate where the specified value is to be associated.
 	 * @param y y-coordinate where the specified value is to be associated.
 	 * @param value value to be associated with the specified coordinates.
 	 *
 	 * @return true if insertion was successful and the data structure changed,
 	 *         false otherwise.
 	 */
 	public boolean put(final double x, final double y, final T value) {
 		if (!this.top.bounds.containsOrEquals(x, y)) {
 			throw new IllegalArgumentException("cannot add a point at x=" + x + ", y=" + y + " with bounds " + this.top.bounds);
 		}
 		if (this.top.put(x, y, value)) {
 			incrementSize();
 			return true;
 		}
 		return false;
 	}
 
 	/**
 	 * Removes the specified object from the specified location.
 	 *
 	 * @param x x-coordinate from which the specified value should be removed
 	 * @param y y-coordinate from which the specified value should be removed
 	 * @param value the value to be removed from the specified coordinates
 	 *
 	 * @return true if the specified value was found at the specified coordinates
 	 *         and was successfully removed (data structure changed), false
 	 *         otherwise.
 	 */
 	public boolean remove(final double x, final double y, final T value) {
 		if (this.top.remove(x, y, value)) {
 			decrementSize();
 			return true;
 		}
 		return false;
 	}
 
 	/** Clear the QuadTree. */
 	public void clear() {
 		this.top.clear();
 		this.size = 0;
 		this.modCount++;
 	}
 
 	/**
 	 * Gets the object closest to x/y
 	 *
 	 * @param x easting, left-right location, longitude
 	 * @param y northing, up-down location, latitude
 	 * @return the object found closest to x/y
 	 */
 	public T getClosest(final double x, final double y) {
 		return this.top.get(x, y, new MutableDouble(Double.POSITIVE_INFINITY));
 	}
 
 	/**
 	 * Gets all objects within a certain distance around x/y
 	 *
 	 * @param x left-right location, longitude
 	 * @param y up-down location, latitude
 	 * @param distance the maximal distance returned objects can be away from x/y
 	 * @return the objects found within distance to x/y
 	 */
 	public Collection<T> getDisk(final double x, final double y, final double distance) {
 		return this.top.get(x, y, distance, new ArrayList<>());
 	}
 
 	/**
 	 * Gets all objects within a linear ring (including borders).
 	 *
 	 * Note by JI (sept '15): This method can be significant faster than calling {@link #getDisk(double, double, double)}
 	 * and a manual check on the returned elements for >= r_min. For randomly distributed points one can use the
 	 * following rule-of-thumb: if r_min/r_max > 0.4 this method is likely to be faster than retrieving all elements within r_max.
 	 *
 	 * @param x left-right location, longitude
 	 * @param y up-down location, latitude
 	 * @param r_min inner ring radius
 	 * @param r_max outer rind radius
 	 * @return objects within the ring
 	 */
 	public Collection<T> getRing(final double x, final double y, final double r_min, final double r_max) {
 		return this.top.get(x, y, r_min, r_max, new ArrayList<>());
 	}
 
 	/**
 	 * Gets all objects within an elliptical region.
 	 *
 	 * @param x1 first focus, longitude
 	 * @param y1 first focus, latitude
 	 * @param x2 second focus, longitude
 	 * @param y2 second focus, latitude
 	 * @param distance the maximal sum of the distances between an object and the two foci
 	 * @return the objects found in the elliptical region
 	 * @throws IllegalArgumentException if the distance is shorter than the distance between the foci
 	 */
 	public Collection<T> getElliptical(
 			final double x1,
 			final double y1,
 			final double x2,
 			final double y2,
 			final double distance) {
 		if ( Math.pow( distance , 2 ) < Math.pow( (x1 - x2), 2 ) + Math.pow( (y1 - y2) , 2 ) ) {
 			throw new IllegalArgumentException( "wrong ellipse specification: distance must be greater than distance between foci."
 					+" x1="+x1
 					+" y1="+y1
 					+" x2="+x2
 					+" y2="+y2
 					+" distance="+distance );
 		}
 		return this.top.getElliptical(x1, y1, x2, y2, distance, new ArrayList<>());
 	}
 
 
 	/**
 	 * Gets all objects inside the specified boundary. Objects on the border of the
 	 * boundary are not included.
 	 *
 	 * @param bounds The bounds of the area of interest.
 	 * @param values1 A collection to store the found objects in.
 	 * @return The objects found within the area.
 	 */
 	public Collection<T> getRectangle(final Rect bounds, final Collection<T> values1) {
 		return this.top.get(bounds, values1);
 	}
 
 	/**
 	 * Gets all objects inside the specified area. Objects on the border of
 	 * the area are not included.
 	 *
 	 * @param minX The minimum left-right location, longitude
 	 * @param minY The minimum up-down location, latitude
 	 * @param maxX The maximum left-right location, longitude
 	 * @param maxY The maximum up-down location, latitude
 	 * @param values1 A collection to store the found objects in.
 	 * @return The objects found within the area.
 	 */
 	public Collection<T> getRectangle(final double minX, final double minY, final double maxX, final double maxY, final Collection<T> values1) {
 		return getRectangle(new Rect(minX, minY, maxX, maxY), values1);
 	}
 
 	/**
 	 * Executes executor on all objects inside a certain boundary
 	 *
 	 * @param bounds The boundary in which the executor will be applied.
 	 * @param executor is executed on the fitting objects
 	 * @return the count of objects found within the bounds.
 	 */
 	public int execute(final Rect bounds, final Executor<T> executor) {
 		if (bounds == null) {
 			return this.top.execute(this.top.getBounds(), executor);
 		}
 		return this.top.execute(bounds, executor);
 	}
 
 	/**
 	 * Executes executor on all objects inside the rectangle (minX,minY):(maxX,maxY)
 	 *
 	 * @param minX The minimum left-right location, longitude
 	 * @param minY The minimum up-down location, latitude
 	 * @param maxX The maximum left-right location, longitude
 	 * @param maxY The maximum up-down location, latitude
 	 * @param executor is executed on the fitting objects
 	 * @return the count of objects found within the rectangle.
 	 */
 	public int execute(final double minX, final double minY, final double maxX, final double maxY, final Executor<T> executor) {
 		return execute(new Rect(minX, minY, maxX, maxY), executor);
 	}
 
 	/**
 	 * Returns the number of entries in this QuadTree.
 	 *
 	 * @return the number of entries in this QuadTree.
 	 */
 	public int size() {
 		return this.size;
 	}
 
 	/** @return the minimum x coordinate (left-right, longitude, easting) of the bounds of the QuadTree. */
 	public double getMinEasting() {
 		return this.top.getBounds().minX;
 	}
 
 	/** @return the maximum x coordinate (left-right, longitude, easting) of the bounds of the QuadTree. */
 	public double getMaxEasting() {
 		return this.top.getBounds().maxX;
 	}
 
 	/** @return the minimum y coordinate (up-down, latitude, northing) of the bounds of the QuadTree. */
 	public double getMinNorthing() {
 		return this.top.getBounds().minY;
 	}
 
 	/** @return the minimum y coordinate (up-down, latitude, northing) of the bounds of the QuadTree. */
 	public double getMaxNorthing() {
 		return this.top.getBounds().maxY;
 	}
 
 
/** A collection view of the values is returned. */
 public Collection<T> values(){
	 	return this.top.values();
 }
 
 	/**
 	 * Returns the number of entries in this QuadTree.
 	 *
 	 * @return the number of entries in this QuadTree.
 	 */
 	public int getSize() {
 		return this.size;
 	}
 
 	/**
 	 * Returns the number of entries in this QuadTree.
 	 *
 	 * @return the number of entries in this QuadTree.
 	 */
 	public int getSize(final Rect bounds) {
 		return this.top.getSize(bounds);
 	}
 
 	/**
 	 * Returns the number of entries in this QuadTree.
 	 *
 	 * @return the number of entries in this QuadTree.
 	 */
 	public int getSize(final double minX, final double minY, final double maxX, final double maxY) {
 		return getSize(new Rect(minX, minY, maxX, maxY));
 	}
 
 	/**
 	 * Returns the number of entries in this QuadTree.
 	 *
 	 * @return the number of entries in this QuadTree.
 	 */
 	public int getSize(final double minX, final double minY, final double maxX, final double maxY, final Rect bounds) {
 		return getSize(new Rect(minX, minY, maxX, maxY), bounds);
 	}
 
 	/**
 	 * Returns the number of entries in this QuadTree.
 	 *
 	 * @return the number of entries in this QuadTree.
 	 */
 	public int getSize(final double minX, final double minY, final double maxX, final double maxY, final double distance) {
 		return getSize(new Rect(minX, minY, maxX, maxY), distance);
 	}
 
 	/**
 	 * Returns the number of entries in this QuadTree.
 	 *
 	 * @return the number of entries in this QuadTree.		
 }

 

}