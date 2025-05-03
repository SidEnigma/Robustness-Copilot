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
 
 
/** Transforms the supplied Z Matrix coordinates to cartesian coordinates. The first atom is found at the point of origin, the second is found on the x-axis, and the third is found in the XY plane. Applying the Zmatrix distances, angles, and dihedrals adds the remaining information. Angles have a degree value. */
 public static Point3d[] zmatrixToCartesian(double[] distances, int[] first_atoms, double[] angles, int[] second_atoms, double[] dihedrals, int[] third_atoms){
    Point3d[] points = new Point3d[distances.length];
    for(int i=0; i<distances.length; i++){
      points[i] = new Point3d();
    }
    Point3d origin = new Point3d();
    Point3d xAxis = new Point3d();
    Point3d yAxis = new Point3d();
    Point3d zAxis = new Point3d();
    Point3d point = new Point3d();
    Vector3d xAxisVector = new Vector3d();
    Vector3d yAxisVector = new Vector3d();
    Vector3d zAxisVector = new Vector3d();
    Vector3d pointVector = new Vector3d();
    Vector3d tempVector = new Vector3d();
    Matrix3f rotationMatrix = new Matrix3f();
    AxisAngle4f rotation = new AxisAngle4f();
    for(int i=0; i<distances.length; i++){
      origin.set(0.0, 0.0, 0.0);
      xAxis.set(1.0, 0.0, 0.0);
      yAxis.set(0.0, 1.0, 0.0);
      zAxis.set(0.0, 0.0, 1.0);
      point.set(distances[i], 0.0, 0.0);
      xAxisVector.set(xAxis);
      yAxisVector.set(yAxis);
      zAxisVector.set(zAxis);
      pointVector.set(point);
      tempVector.cross(xAxisVector, yAxisVector);
      rotationMatrix.setColumn(0, xAxisVector);
      rotationMatrix.setColumn(1, yAxisVector);
      rotationMatrix.setColumn(2, tempVector);
      rotation.set(rotationMatrix);
      rotation.angle = angles[i]*Math.PI/180.0;
      pointVector.rotate(rotation);
      pointVector.add(origin);
      pointVector.add(point);   
 }

 

}