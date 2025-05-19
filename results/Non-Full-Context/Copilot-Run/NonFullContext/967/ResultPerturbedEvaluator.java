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
 
 
/** Creates a new IDifference object, given its name and the two objects to compare. */
 public static IDifference construct(String name, Point3d first, Point3d second){
        Point3dDifference diff = new Point3dDifference(name);
        diff.addDifference(first, second);
        return diff;
    }
    
        /**
        * Adds a difference to this difference list.
        *
        * @param first the first object
        * @param second the second object
        */
        public void addDifference(Point3d first, Point3d second) {
            if (first == null && second == null) {
                return;
            }
            if (first == null) {
                addDifference(name, "null", second.toString());
            } else if (second == null) {
                addDifference(name, first.toString(), "null");
            } else if (!first.equals(second)) {
                addDifference(name, first.toString(), second.toString());
            }
        }
    
        /**
        * Returns the name of this difference list.
        *
        * @return the name of this difference list
        */
        public String getName() {
            return name;
        }
    
        /**
        * Returns the first object.
        *
        * @return the first object
        */
        public Object getFirst() {
            return null;
        }
    
        /**
        * Returns the second object.
        *
        * @return the second object
        */
        public Object getSecond() {
            return null;
        }
    
        /**
        * Returns an iterator over the differences.
        *
        * @return an iterator over the differences
        */
        public Iterator<IDifference> iterator() {
            return differences.iterator();
        }
    
        /**
        * Returns the number of differences.
        *
        * @return the number of differences
        */
        public int size() {
            return differences.size();
        }
    
        /**
        * Returns true if this difference list is empty.
        *
        * @return true if this difference list is empty
        */
        public boolean isEmpty() {
            return differences.isEmpty();
        }
    
        /**     
 }

 

}