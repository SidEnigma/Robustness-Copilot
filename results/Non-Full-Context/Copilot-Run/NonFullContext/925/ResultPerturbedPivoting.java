/* Copyright (C) 2004-2007  The Chemistry Development Kit (CDK) project
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public License
  * as published by the Free Software Foundation; either version 2.1
  * of the License, or (at your option) any later version.
  * All we ask is that proper credit is given for our work, which includes
  * - but is not limited to - adding the above copyright notice to the beginning
  * of your source code files, and to any copyright notice that you may distribute
  * with programs based on this work.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  *  */
 package org.openscience.cdk.geometry;
 
 import javax.vecmath.AxisAngle4f;
 import javax.vecmath.Matrix3f;
 import javax.vecmath.Point3d;
 import javax.vecmath.Vector3d;
 import javax.vecmath.Vector3f;
 
 /**
  * A set of static utility classes for dealing with Z matrices.
  *
  * @cdk.module  io
  * @cdk.githash
  * @cdk.keyword Z-matrix
  *
  * @cdk.created 2004-02-09
  */
 public class ZMatrixTools {
 
 
/** Takes the coordinates of the given Z matrix and converts them to Cartesian coordinates. */
 public static Point3d[] zmatrixToCartesian(double[] distances, int[] first_atoms, double[] angles, int[] second_atoms, double[] dihedrals, int[] third_atoms){
    Point3d[] coordinates = new Point3d[distances.length];
    for (int i = 0; i < distances.length; i++) {
      coordinates[i] = new Point3d();
    }
    for (int i = 0; i < distances.length; i++) {
      if (i == 0) {
        coordinates[i].set(0, 0, 0);
      } else {
        coordinates[i].set(coordinates[first_atoms[i]].x + distances[i] * Math.cos(angles[i] * Math.PI / 180),
                            coordinates[first_atoms[i]].y + distances[i] * Math.sin(angles[i] * Math.PI / 180),
                            coordinates[first_atoms[i]].z);
      }
    }
    for (int i = 0; i < distances.length; i++) {
      if (i == 0) {
        coordinates[i].set(0, 0, 0);
      } else {
        double x = coordinates[i].x;
        double y = coordinates[i].y;
        double z = coordinates[i].z;
        double x1 = coordinates[first_atoms[i]].x;
        double y1 = coordinates[first_atoms[i]].y;
        double z1 = coordinates[first_atoms[i]].z;
        double x2 = coordinates[second_atoms[i]].x;
        double y2 = coordinates[second_atoms[i]].y;
        double z2 = coordinates[second_atoms[i]].z;
        double x3 = coordinates[third_atoms[i]].x;
        double y3 = coordinates[third_atoms[i]].y;
        double z3 = coordinates[third_atoms[i]].z;
        double x4 = x1 + x2 - x3;
        double y4 = y1 + y2 - y3;
        double z4 = z1 + z2 - z3;
        double x5 = x1 + x3 - x2;
        double y5   
 }

 

}