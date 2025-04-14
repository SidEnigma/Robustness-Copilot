/* Copyright (C) 2008 Gilleain Torrance <gilleain.torrance@gmail.com>
  *               2011 Egon Willighagen <egonw@users.sf.net>
  *
  *  Contact: cdk-devel@list.sourceforge.net
  *
  *  This program is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU Lesser General Public License
  *  as published by the Free Software Foundation; either version 2.1
  *  of the License, or (at your option) any later version.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU Lesser General Public License for more details.
  *
  *  You should have received a copy of the GNU Lesser General Public License
  *  along with this program; if not, write to the Free Software
  *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 package org.openscience.cdk.renderer.visitor;
 
 import java.awt.FontMetrics;
 import java.awt.Graphics2D;
 import java.awt.Point;
 import java.awt.font.TextLayout;
 import java.awt.geom.AffineTransform;
 import java.awt.geom.Rectangle2D;
 
 
 /**
  * Partial implementation of the {@link IDrawVisitor} interface for the AWT
  * widget toolkit, allowing molecules to be rendered with toolkits based on
  * AWT, like the Java reference graphics platform Swing.
  *
  * @cdk.module renderawt
  * @cdk.githash
  */
 public abstract class AbstractAWTDrawVisitor implements IDrawVisitor {
 
     /**
      * This is initially null, and must be set in the setTransform method!
      */
     protected AffineTransform transform = null;
 
 
/** Given the current affine transformation, change the point converting a world coordinate into a screen coordinate. */
 public int[] transformPoint(double xCoord, double yCoord){
      int[] xy = new int[2];
      xy[0] = (int)Math.round(transform.getTranslateX() + transform.getScaleX() * xCoord);
      xy[1] = (int)Math.round(transform.getTranslateY() + transform.getScaleY() * yCoord);
      return xy;
  }
  
      /**
        * Given the current affine transformation, change the point converting a world coordinate into a screen coordinate.
        */
      public Point transformPoint(Point point){
          int[] xy = transformPoint(point.x, point.y);
          return new Point(xy[0], xy[1]);
      }
  
      /**
        * Given the current affine transformation, change the point converting a world coordinate into a screen coordinate.
        */
      public Point transformPoint(int x, int y){
          int[] xy = transformPoint(x, y);
          return new Point(xy[0], xy[1]);
      }
  
      /**
        * Given the current affine transformation, change the point converting a world coordinate into a screen coordinate.
        */
      public Point transformPoint(double x, double y){
          int[] xy = transformPoint(x, y);
          return new Point(xy[0], xy[1]);
      }
  
      /**
        * Given the current affine transformation, change the point converting a world coordinate into a screen coordinate.
        */
      public int[] transformPoint(int[] xy){
          return transformPoint(xy[0], xy[1]);
      }
  
      /**
        * Given the current affine transformation, change the point converting a world coordinate into a screen coordinate.
        */
      public int[] transformPoint(Point[] points){
          int[] xy = new int[points.length * 2];
          for (int i = 0; i < points.length; i++) {
              xy[i * 2] = points[i].x;
              xy[i * 2 + 1] = points[i].y;
          }
          return transformPoint(xy);
      } 
 }

 

}