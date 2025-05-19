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
 import java.util.Arrays;
 import java.util.List;
 
 import static org.openscience.cdk.interfaces.IBond.Order.DOUBLE;
 import static org.openscience.cdk.interfaces.IBond.Stereo.E_OR_Z;
 
 /**
  * A stereo encoder factory encoding double bond configurations by 2D and 3D
  * coordinates. This factory will attempt to encode all double bonds that meet
  * the following conditions. Are not {@literal -N=N-} bonds, non-cumulated,
  * non-query and have each double bonded atom has at least one substituent. In
  * future the encoding rules may be more strict or even configurable but
  * currently they may be over zealous when encoding configurations with 3D
  * coordinates. <br> This class is intended to be used with a the hash
  * encoding classes and is easier used via the {@link org.openscience.cdk.hash.HashGeneratorMaker}.
  *
  * @author John May
  * @cdk.module hash
  * @cdk.githash
  * @see org.openscience.cdk.hash.HashGeneratorMaker
  */
 public final class GeometricDoubleBondEncoderFactory implements StereoEncoderFactory {
 
 
/** A stereo Encoder is needed for all double bond stereo configurations. */
 public StereoEncoder create(IAtomContainer container, int[][] graph){}

 

}