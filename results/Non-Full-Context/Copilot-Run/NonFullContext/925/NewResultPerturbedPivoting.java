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

    public static Point3d[] zmatrixToCartesian(double[] distances, int[] first_atoms, double[] angles, int[] second_atoms, double[] dihedrals, int[] third_atoms) {
        int numAtoms = distances.length;
        Point3d[] cartesianCoordinates = new Point3d[numAtoms];

        // Initialize the first three atoms
        cartesianCoordinates[0] = new Point3d(0, 0, 0);
        cartesianCoordinates[1] = new Point3d(distances[0], 0, 0);
        cartesianCoordinates[2] = new Point3d(distances[1] * Math.cos(angles[0]), distances[1] * Math.sin(angles[0]), 0);

        // Calculate the Cartesian coordinates for the remaining atoms
        for (int i = 3; i < numAtoms; i++) {
            Vector3d v1 = new Vector3d(cartesianCoordinates[first_atoms[i]]);
            Vector3d v2 = new Vector3d(cartesianCoordinates[second_atoms[i]]);
            Vector3d v3 = new Vector3d(cartesianCoordinates[third_atoms[i]]);

            Vector3d v12 = new Vector3d();
            v12.sub(v2, v1);
            v12.normalize();

            Vector3d v23 = new Vector3d();
            v23.sub(v3, v2);
            v23.normalize();

            Vector3d v12CrossV23 = new Vector3d();
            v12CrossV23.cross(v12, v23);
            v12CrossV23.normalize();

            Matrix3f rotationMatrix = new Matrix3f();
            rotationMatrix.set(new AxisAngle4f(v12CrossV23, (float) dihedrals[i - 3]));

            Vector3f v23f = new Vector3f((float) v23.x, (float) v23.y, (float) v23.z);
            rotationMatrix.transform(v23f);

            Vector3d v23Transformed = new Vector3d(v23f.x, v23f.y, v23f.z);
            v23Transformed.scale(distances[i - 1]);

            Point3d cartesianCoordinate = new Point3d();
            cartesianCoordinate.add(cartesianCoordinates[second_atoms[i]], v23Transformed);
            cartesianCoordinates[i] = cartesianCoordinate;
        }

        return cartesianCoordinates;
    }
 

}