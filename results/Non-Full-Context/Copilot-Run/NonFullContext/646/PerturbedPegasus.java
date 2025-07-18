/* Copyright (C) 2008  Egon Willighagen <egonw@users.sf.net>
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public License
  * as published by the Free Software Foundation; either version 2.1
  * of the License, or (at your option) any later version.
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
 package org.openscience.cdk.tools.diff.tree;
 
 import javax.vecmath.Point3d;
 import java.util.Iterator;
 
 /**
  * Difference between two boolean[]'s.
  *
  * @author     egonw
  * @cdk.module diff
  * @cdk.githash
  */
 public class Point3dDifference extends AbstractDifferenceList implements IDifferenceList {
 
     private String name;
 
     private Point3dDifference(String name) {
         this.name = name;
     }
 
     /**
      * Constructs a new {@link IDifference} object.
      *
      * @param name   a name reflecting the nature of the created {@link IDifference}
      * @param first  the first object to compare
      * @param second the second object to compare
      * @return       an {@link IDifference} reflecting the differences between the first and second object
      */
     public static IDifference construct(String name, Point3d first, Point3d second) {
         if (first == null && second == null) return null;
 
         Point3dDifference totalDiff = new Point3dDifference(name);
         totalDiff.addChild(DoubleDifference.construct("x", first == null ? null : first.x, second == null ? null
                 : second.x));
         totalDiff.addChild(DoubleDifference.construct("y", first == null ? null : first.y, second == null ? null
                 : second.y));
         totalDiff.addChild(DoubleDifference.construct("z", first == null ? null : first.z, second == null ? null
                 : second.z));
         if (totalDiff.childCount() == 0) {
             return null;
         }
         return totalDiff;
     }
 
 
/** This is a representation of the @link IDifference. */
 public String toString(){}

 

}