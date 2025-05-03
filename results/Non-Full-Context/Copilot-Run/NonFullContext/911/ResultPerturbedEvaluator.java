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
 
 
/** Returns a list of points related to the given Java 2D shape. */
 static List<Point2D> pointsOf(final Shape shape){
        final List<Point2D> points = new ArrayList<Point2D>();
        final PathIterator iterator = shape.getPathIterator(null);
        while (!iterator.isDone()) {
            final double[] coords = new double[2];
            final int type = iterator.currentSegment(coords);
            points.add(new Point2D.Double(coords[0], coords[1]));
            iterator.next();
        }
        return points;
    }
    
        /**
        * Graham scan to find the convex hull.
        *
        * @param points list of points
        * @return the convex hull
        */
        static List<Point2D> grahamScan(final List<Point2D> points) {
            final List<Point2D> sorted = new ArrayList<Point2D>(points);
            Collections.sort(sorted, new Comparator<Point2D>() {
                @Override
                public int compare(Point2D o1, Point2D o2) {
                    return Double.compare(o1.getY(), o2.getY());
                }
            });
            final Point2D lowest = sorted.get(0);
            final Point2D highest = sorted.get(sorted.size() - 1);
            final List<Point2D> hull = new ArrayList<Point2D>();
            hull.add(lowest);
            hull.add(highest);
            final Deque<Point2D> stack = new ArrayDeque<Point2D>();
            stack.push(lowest);
            stack.push(highest);
            for (int i = 1; i < sorted.size() - 1; i++) {
                final Point2D point = sorted.get(i);
                while (!stack.isEmpty() && stack.peek().compareTo(point) > 0)
                    stack.pop();
                stack.push(point);
            }
            while (!stack.isEmpty())
                hull.add(stack.pop());
            return hull;
        }
    
    }
            
 }

 

}