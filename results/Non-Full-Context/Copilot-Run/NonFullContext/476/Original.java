/*
  * Copyright (c) 2014 European Bioinformatics Institute (EMBL-EBI)
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
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 
 package org.openscience.cdk.renderer.generators.standard;
 
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IBond;
 
 import javax.vecmath.Point2d;
 import javax.vecmath.Tuple2d;
 import javax.vecmath.Vector2d;
 import java.awt.geom.Point2D;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.List;
 
 /**
  * A collection of static utilities for Java 3D javax.vecmath.* objects.
  *
  * @author John May
  */
 final class VecmathUtil {
 
     /**
      * Instantiation is disabled.
      */
     private VecmathUtil() {}
 
     /**
      * Convert a Vecmath (javax.vecmath.*) point to an AWT (java.awt.geom.*)
      * point.
      *
      * @param point a Vecmath point
      * @return an AWT point
      */
     static Point2D toAwtPoint(Point2d point) {
         return new Point2D.Double(point.x, point.y);
     }
 
     /**
      * Convert a AWT (java.awt.geom.*) point to a Vecmath (javax.vecmath.*)
      * point.
      *
      * @param point an AWT point
      * @return a Vecmath point
      */
     static Point2d toVecmathPoint(Point2D point) {
         return new Point2d(point.getX(), point.getY());
     }
 
     /**
      * Create a unit vector between two points.
      *
      * @param from start of vector
      * @param to end of vector
      * @return unit vector
      */
     static Vector2d newUnitVector(final Tuple2d from, final Tuple2d to) {
         final Vector2d vector = new Vector2d(to.x - from.x, to.y - from.y);
         vector.normalize();
         return vector;
     }
 
     /**
      * Create a unit vector for a bond with the start point being the specified atom.
      *
      * @param atom start of vector
      * @param bond the bond used to create the vector
      * @return unit vector
      */
     static Vector2d newUnitVector(final IAtom atom, final IBond bond) {
         return newUnitVector(atom.getPoint2d(), bond.getOther(atom).getPoint2d());
     }
 
     /**
      * Create unit vectors from one atom to all other provided atoms.
      *
      * @param fromAtom reference atom (will become 0,0)
      * @param toAtoms list of to atoms
      * @return unit vectors
      */
     static List<Vector2d> newUnitVectors(final IAtom fromAtom, final List<IAtom> toAtoms) {
         final List<Vector2d> unitVectors = new ArrayList<Vector2d>(toAtoms.size());
         for (final IAtom toAtom : toAtoms) {
             unitVectors.add(newUnitVector(fromAtom.getPoint2d(), toAtom.getPoint2d()));
         }
         return unitVectors;
     }
 
     /**
      * Create a new vector perpendicular (at a right angle) to the provided
      * vector. In 2D, there are two perpendicular vectors, the other
      * perpendicular vector can be obtained by negation.
      *
      * @param vector reference to which a perpendicular vector is returned
      * @return perpendicular vector
      */
     static Vector2d newPerpendicularVector(final Vector2d vector) {
         return new Vector2d(-vector.y, vector.x);
     }
 
     /**
      * Midpoint of a line described by two points, a and b.
      *
      * @param a first point
      * @param b second point
      * @return the midpoint
      */
     static Point2d midpoint(Point2d a, Point2d b) {
         return new Point2d((a.x + b.x) / 2, (a.y + b.y) / 2);
     }
 
     /**
      * Scale a vector by a given factor, the input vector is not modified.
      *
      * @param vector a vector to scale
      * @param factor how much the input vector should be scaled
      * @return scaled vector
      */
     static Vector2d scale(final Tuple2d vector, final double factor) {
         final Vector2d cpy = new Vector2d(vector);
         cpy.scale(factor);
         return cpy;
     }
 
 
/** Sum the components of two vectors, the input is not modified. */
 static Vector2d sum(final Tuple2d a, final Tuple2d b){}

 

}