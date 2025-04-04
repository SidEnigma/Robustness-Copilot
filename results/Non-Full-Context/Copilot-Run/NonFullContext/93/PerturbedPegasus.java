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
 
 import java.awt.Shape;
 import java.awt.geom.AffineTransform;
 import java.awt.geom.Line2D;
 import java.awt.geom.Path2D;
 import java.awt.geom.PathIterator;
 import java.awt.geom.Point2D;
 import java.util.ArrayDeque;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.Deque;
 import java.util.List;
 
 /**
  * Immutable convex hull that is the smallest set of convex points that surround a shape.
  *
  * <pre>{@code
  * ConvexHull hull = ConvexHull.ofShape(shape);
  *
  * // the hull can be transformed
  * hull = hull.transform(new AffineTransform());
  *
  * // given a line, a point on the hull can be found that intersects the line
  * Point2D point == hull.intersect(new Line2D.Double(...), 0);
  * }</pre>
  *
  * @author John May
  */
 final class ConvexHull {
 
     /** The convex hull. */
     private final Shape hull;
 
     /**
      * Internal constructor, the hull is an argument.
      *
      * @param hull the convex hull
      */
     private ConvexHull(final Shape hull) {
         this.hull = hull;
     }
 
     /**
      * Calculate the convex hull of a shape.
      *
      * @param shape a Java 2D shape
      * @return the convex hull
      */
     public static ConvexHull ofShape(final Shape shape) {
         return ofShapes(Collections.singletonList(shape));
     }
 
     /**
      * Calculate the convex hull of multiple shapes.
      *
      * @param shapes Java 2D shapes
      * @return the convex hull
      */
     public static ConvexHull ofShapes(final List<Shape> shapes) {
         final Path2D combined = new Path2D.Double();
         for (Shape shape : shapes)
             combined.append(shape, false);
         return new ConvexHull(shapeOf(grahamScan(pointsOf(combined))));
     }
 
     /**
      * The outline of the hull as a Java 2D shape.
      *
      * @return outline of the hull
      */
     Shape outline() {
         return hull;
     }
 
     /**
      * Apply the provided transformation to the convex hull.
      *
      * @param transform a transform
      * @return a new transformed hull
      */
     ConvexHull transform(final AffineTransform transform) {
         return new ConvexHull(transform.createTransformedShape(hull));
     }
 
     /**
      * Convert a list of points to a shape.
      *
      * @param points list of points
      * @return a shape
      */
     static Shape shapeOf(List<Point2D> points) {
         Path2D path = new Path2D.Double();
         if (!points.isEmpty()) {
             path.moveTo(points.get(0).getX(), points.get(0).getY());
             for (Point2D point : points)
                 path.lineTo(point.getX(), point.getY());
             path.closePath();
         }
         return path;
     }
 
     /**
      * Convert a Java 2D shape to a list of points.
      *
      * @param shape a shape
      * @return list of point
      */
     static List<Point2D> pointsOf(final Shape shape) {
         final List<Point2D> points = new ArrayList<Point2D>();
         final double[] coordinates = new double[6];
         for (PathIterator i = shape.getPathIterator(null); !i.isDone(); i.next()) {
             switch (i.currentSegment(coordinates)) {
                 case PathIterator.SEG_CLOSE:
                     break;
                 case PathIterator.SEG_MOVETO:
                 case PathIterator.SEG_LINETO:
                     points.add(new Point2D.Double(coordinates[0], coordinates[1]));
                     break;
                 case PathIterator.SEG_QUADTO:
                     points.add(new Point2D.Double(coordinates[0], coordinates[1]));
                     points.add(new Point2D.Double(coordinates[2], coordinates[3]));
                     break;
                 case PathIterator.SEG_CUBICTO:
                     points.add(new Point2D.Double(coordinates[0], coordinates[1]));
                     points.add(new Point2D.Double(coordinates[2], coordinates[3]));
                     points.add(new Point2D.Double(coordinates[4], coordinates[5]));
                     break;
             }
         }
 
         if (!points.isEmpty() && points.get(points.size() - 1).equals(points.get(0))) {
             points.remove(points.size() - 1);
         }
         return points;
     }
 
 
/** The points belonging to the hull are determined by the Graham Scan. */
 static List<Point2D> grahamScan(final List<Point2D> points){}

 

}