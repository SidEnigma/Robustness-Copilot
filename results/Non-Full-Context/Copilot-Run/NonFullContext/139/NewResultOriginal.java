/*
  * Copyright (c) 2013 John May <jwmay@users.sf.net>
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
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 U
  */
 
 package org.openscience.cdk.hash.stereo;
 
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomType;
 import org.openscience.cdk.interfaces.IBond;
 
 import javax.vecmath.Point2d;
 import javax.vecmath.Point3d;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 /**
  * A stereo encoder factory for tetrahedral centres. This factory generates
  * {@link StereoEncoder}s for centres with specified by 2D and 3D coordinates.
  * The required preconditions are the central atom must have 3/4 neighboring
  * atoms, Sp3 hybridization and no query bonds (e.g. wiggly). If there is at
  * least one up/down bond and all required atoms have coordinates a new 2D
  * encoder is created. If the there are no stereo bonds (up/down) and all
  * required atoms have 3D coordinates then a new 3D encoder is created.
  *
  * @author John May
  * @cdk.module hash
  * @cdk.githash
  */
 public class GeometricTetrahedralEncoderFactory implements StereoEncoderFactory {
 
 
/** Create a stereo encoder for all potential 2D and 3D tetrahedral  elements. */

    public StereoEncoder create(IAtomContainer container, int[][] graph) {
        // Implementation logic goes here
        
        // Create a new instance of the StereoEncoder
        StereoEncoder encoder = new StereoEncoder();
        
        // Add implementation logic to encode the stereochemistry
        
        // Return the created StereoEncoder
        return encoder;
    }
 

}