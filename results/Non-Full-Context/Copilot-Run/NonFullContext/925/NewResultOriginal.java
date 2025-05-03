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
 
 
/** Takes the given Z Matrix coordinates and converts them to cartesian coordinates. */

    public static Point3d[] zmatrixToCartesian(double[] distances, int[] first_atoms, double[] angles, int[] second_atoms, double[] dihedrals, int[] third_atoms) {
        int numAtoms = distances.length;
        Point3d[] cartesianCoordinates = new Point3d[numAtoms];

        // Create a matrix to store the rotation and translation transformations
        Matrix3f rotationMatrix = new Matrix3f();
        Vector3f translationVector = new Vector3f();

        // Initialize the first atom at the origin
        cartesianCoordinates[0] = new Point3d(0, 0, 0);

        // Iterate over the remaining atoms and calculate their cartesian coordinates
        for (int i = 1; i < numAtoms; i++) {
            // Get the indices of the atoms involved in the current bond, angle, and dihedral
            int firstAtomIndex = first_atoms[i] - 1;
            int secondAtomIndex = second_atoms[i] - 1;
            int thirdAtomIndex = third_atoms[i] - 1;

            // Get the distances, angles, and dihedrals for the current atom
            double distance = distances[i];
            double angle = angles[i];
            double dihedral = dihedrals[i];

            // Calculate the translation vector for the current atom
            Vector3d translation = new Vector3d(distance, 0, 0);
            rotationMatrix.set(new AxisAngle4f(0, 0, 1, (float) Math.toRadians(dihedral)));
            rotationMatrix.transform(translation);
            translationVector.set(translation);

            // Calculate the rotation matrix for the current atom
            Vector3d axis = new Vector3d(0, 0, 1);
            rotationMatrix.set(new AxisAngle4f(axis, (float) Math.toRadians(angle)));

            // Calculate the cartesian coordinates for the current atom
            Point3d previousAtom = cartesianCoordinates[firstAtomIndex];
            Vector3d cartesianCoordinatesVector = new Vector3d(previousAtom);
            rotationMatrix.transform(cartesianCoordinatesVector);
            cartesianCoordinatesVector.add(translationVector);
            cartesianCoordinates[i] = new Point3d(cartesianCoordinatesVector);
        }

        return cartesianCoordinates;
    }
 

}