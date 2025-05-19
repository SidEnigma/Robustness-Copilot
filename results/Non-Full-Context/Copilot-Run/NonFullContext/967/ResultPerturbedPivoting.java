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
 
 
/** Constructs a new {@link IDifference} object. */
 public static IDifference construct(String name, Point3d first, Point3d second){
        Point3dDifference diff = new Point3dDifference(name);
        diff.add(first, second);
        return diff;
    }
    
        /**
        * Constructs a new {@link IDifference} object.
        */
        public static IDifference construct(String name, Point3d first, Point3d second, Point3dDifference diff){
            Point3dDifference diff2 = new Point3dDifference(name);
            diff2.add(first, second);
            diff2.add(diff);
            return diff2;
        }
    
        /**
        * Constructs a new {@link IDifference} object.
        */
        public static IDifference construct(String name, Point3d first, Point3d second, Point3dDifference diff, Point3dDifference diff2){
            Point3dDifference diff3 = new Point3dDifference(name);
            diff3.add(first, second);
            diff3.add(diff);
            diff3.add(diff2);
            return diff3;
        }
    
        /**
        * Constructs a new {@link IDifference} object.
        */
        public static IDifference construct(String name, Point3d first, Point3d second, Point3dDifference diff, Point3dDifference diff2, Point3dDifference diff3){
            Point3dDifference diff4 = new Point3dDifference(name);
            diff4.add(first, second);
            diff4.add(diff);
            diff4.add(diff2);
            diff4.add(diff3);
            return diff4;
        }
    
        /**
        * Constructs a new {@link IDifference} object.
        */
        public static IDifference construct(String name, Point3d first, Point3d second, Point3dDifference diff, Point3dDifference diff2, Point3dDifference diff3, Point3dDifference diff4){
            Point3dDifference diff5 = new Point3dDifference(name);
            diff5.add(first, second);
            diff5.add(diff);        
 }

 

}