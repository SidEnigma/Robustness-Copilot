/* Copyright (C) 2003-2007  The Chemistry Development Kit (CDK) project
  *
  *  Contact: cdk-devel@lists.sourceforge.net
  *
  *  This program is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU Lesser General Public License
  *  as published by the Free Software Foundation; either version 2.1
  *  of the License, or (at your option) any later version.
  *  All we ask is that proper credit is given for our work, which includes
  *  - but is not limited to - adding the above copyright notice to the beginning
  *  of your source code files, and to any copyright notice that you may distribute
  *  with programs based on this work.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU Lesser General Public License for more details.
  *
  *  You should have received a copy of the GNU Lesser General Public License
  *  along with this program; if not, write to the Free Software
  *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  *
  */
 package org.openscience.cdk.layout;
 
 import java.awt.geom.Line2D;
 import java.awt.geom.Point2D;
 import java.util.Vector;
 
 import javax.vecmath.Point2d;
 import javax.vecmath.Vector2d;
 
 import org.openscience.cdk.geometry.GeometryUtil;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.interfaces.IRingSet;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 
 /**
  * Helper class for Structure Diagram Generation. Resolves atom or bond
  * overlaps after the actual SDG was done
  *
  * @author      steinbeck
  * @cdk.created 2003-09-4
  * @cdk.keyword layout
  * @cdk.keyword 2D-coordinates
  * @cdk.module  sdg
  * @cdk.githash
  * @deprecated does not resolve overlaps correctly
  */
 @Deprecated
 public class OverlapResolver {
 
     private static ILoggingTool logger   = LoggingToolFactory.createLoggingTool(OverlapResolver.class);
 
     int                         maxSteps = 10000;
 
     public OverlapResolver() {}
 
 
/** The primary method to call to resolve overlap situations. */
 public double resolveOverlap(IAtomContainer ac, IRingSet sssr){
      double energy = 0.0;
      int steps = 0;
      double oldEnergy = 0.0;
      double newEnergy = 0.0;
      double delta = 0.0;
      double stepSize = 0.1;
      double stepSize2 = 0.01;
      double stepSize3 = 0.001;
      double stepSize4 = 0.0001;
      double stepSize5 = 0.00001;
      double stepSize6 = 0.000001;
      double stepSize7 = 0.0000001;
      double stepSize8 = 0.00000001;
      double stepSize9 = 0.000000001;
      double stepSize10 = 0.0000000001;
      double stepSize11 = 0.00000000001;
      double stepSize12 = 0.000000000001;
      double stepSize13 = 0.0000000000001;
      double stepSize14 = 0.00000000000001;
      double stepSize15 = 0.000000000000001;
      double stepSize16 = 0.0000000000000001;
      double stepSize17 = 0.00000000000000001;
      double stepSize18 = 0.000000000000000001;
      double stepSize19 = 0.0000000000000000001;
      double stepSize20 = 0.00000000000000000001;
      double stepSize21 = 0.000000000000000000001;
      double stepSize22 = 0.0000000000000000000001;
      double stepSize23 = 0.00000000000000000000001;
      double stepSize24 = 0.000000000000000000000001;
      double stepSize25 = 0.0000000000000000000000001;
      double stepSize26 = 0.00000000000000000000000001;
      double stepSize27 = 0.000000000000000000000000001;
      double stepSize28 = 0.0000000000000000000000000001;
      double stepSize29 = 0.00000000000000000000000000001;
      double stepSize30 = 0.000000000000000000000000000001;
      double stepSize31 = 0.0000000000000000000000000000001;
      double stepSize32 = 0.00000000000000000000000000000001; 
 }

 

}