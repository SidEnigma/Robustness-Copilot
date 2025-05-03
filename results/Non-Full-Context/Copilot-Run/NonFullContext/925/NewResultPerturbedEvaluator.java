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

    public static Point3d[] zmatrixToCartesian(double[] distances, int[] first_atoms, double[] angles, int[] second_atoms, double[] dihedrals, int[] third_atoms) {
        int numAtoms = distances.length;
        Point3d[] cartesianCoords = new Point3d[numAtoms];

        // Set the first atom at the origin
        cartesianCoords[0] = new Point3d(0, 0, 0);

        // Set the second atom on the x-axis
        cartesianCoords[1] = new Point3d(distances[0], 0, 0);

        // Set the third atom in the XY plane
        double angle = Math.toRadians(angles[0]);
        double x = distances[1] * Math.cos(angle);
        double y = distances[1] * Math.sin(angle);
        cartesianCoords[2] = new Point3d(x, y, 0);

        // Apply the remaining distances, angles, and dihedrals
        for (int i = 3; i < numAtoms; i++) {
            Vector3d v1 = new Vector3d(cartesianCoords[first_atoms[i]]);
            Vector3d v2 = new Vector3d(cartesianCoords[second_atoms[i]]);
            Vector3d v3 = new Vector3d(cartesianCoords[third_atoms[i]]);

            // Translate v2 to the origin
            v2.sub(v1);

            // Rotate v2 around v3 by the dihedral angle
            Matrix3f rotationMatrix = new Matrix3f();
            rotationMatrix.set(new AxisAngle4f(v3, (float) Math.toRadians(dihedrals[i])));
            rotationMatrix.transform(v2);

            // Translate v2 back to its original position
            v2.add(v1);

            cartesianCoords[i] = new Point3d(v2);
        }

        return cartesianCoords;
    }
 

}