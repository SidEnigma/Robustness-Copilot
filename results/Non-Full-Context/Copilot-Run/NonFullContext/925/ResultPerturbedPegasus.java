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
 
 
/** The Z Matrix coordinates are converted to cartesian coordinates. */
 public static Point3d[] zmatrixToCartesian(double[] distances, int[] first_atoms, double[] angles, int[] second_atoms, double[] dihedrals, int[] third_atoms){
    Point3d[] points = new Point3d[distances.length];
    for (int i = 0; i < distances.length; i++) {
      points[i] = new Point3d();
      points[i].set(distances[i], 0.0, 0.0);
    }
    for (int i = 0; i < angles.length; i++) {
      Point3d p1 = points[first_atoms[i]];
      Point3d p2 = points[second_atoms[i]];
      Point3d p3 = points[third_atoms[i]];
      Vector3d v1 = new Vector3d();
      v1.sub(p2, p1);
      Vector3d v2 = new Vector3d();
      v2.sub(p3, p2);
      v1.normalize();
      v2.normalize();
      Vector3d v3 = new Vector3d();
      v3.cross(v1, v2);
      v3.normalize();
      Matrix3d m = new Matrix3d();
      m.setColumn(0, v1);
      m.setColumn(1, v2);
      m.setColumn(2, v3);
      m.invert();
      Point3d p = new Point3d();
      p.set(distances[i], 0.0, 0.0);
      m.transform(p);
      points[i].set(p);
    }
    for (int i = 0; i < dihedrals.length; i++) {
      Point3d p1 = points[first_atoms[i]];
      Point3d p2 = points[second_atoms[i]];
      Point3d p3 = points[third_atoms[i]];
      Point3d p4 = points[fourth_atoms[i]];
      Vector3d v1 = new Vector3d();
      v1.sub(p2, p1);
      Vector3d v2 = new Vector3d();
      v2.sub(p3, p2);
      Vector    
 }

 

}