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
        public static IDifference construct(String name, Point3d first, Point3d second, String firstLabel, String secondLabel){
            Point3dDifference diff = new Point3dDifference(name);
            diff.add(first, second, firstLabel, secondLabel);
            return diff;
        }
    
        /**
        * Constructs a new {@link IDifference} object.
        */
        public static IDifference construct(String name, Point3d first, Point3d second, String firstLabel, String secondLabel, String firstValue, String secondValue){
            Point3dDifference diff = new Point3dDifference(name);
            diff.add(first, second, firstLabel, secondLabel, firstValue, secondValue);
            return diff;
        }
    
        /**
        * Constructs a new {@link IDifference} object.
        */
        public static IDifference construct(String name, Point3d first, Point3d second, String firstLabel, String secondLabel, String firstValue, String secondValue, String firstUnit, String secondUnit){
            Point3dDifference diff = new Point3dDifference(name);
            diff.add(first, second, firstLabel, secondLabel, firstValue, secondValue, firstUnit, secondUnit);
            return diff;
        }
    
        /**
        * Constructs a new {@link IDifference} object.
        */
        public static IDifference construct(String name, Point3d first, Point3d second, String firstLabel, String secondLabel, String firstValue, String secondValue, String firstUnit, String secondUnit, String firstError, String secondError){
            Point3dDifference diff = new Point3dDifference(name);
            diff.add(first, second, firstLabel, secondLabel, firstValue, secondValue, firstUnit, secondUnit, firstError, secondError);
            return diff;
        }
    
        /**     
 }

 

}