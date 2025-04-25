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
 
 
/** In case of overlap, call the main method to resolve such an overlap. */
 public double resolveOverlap(IAtomContainer ac, IRingSet sssr){
      double step = 0.1;
      double stepSize = 0.1;
      double maxDistance = 0.0;
      double distance = 0.0;
      double maxDistance2 = 0.0;
      double distance2 = 0.0;
      double maxDistance3 = 0.0;
      double distance3 = 0.0;
      double maxDistance4 = 0.0;
      double distance4 = 0.0;
      double maxDistance5 = 0.0;
      double distance5 = 0.0;
      double maxDistance6 = 0.0;
      double distance6 = 0.0;
      double maxDistance7 = 0.0;
      double distance7 = 0.0;
      double maxDistance8 = 0.0;
      double distance8 = 0.0;
      double maxDistance9 = 0.0;
      double distance9 = 0.0;
      double maxDistance10 = 0.0;
      double distance10 = 0.0;
      double maxDistance11 = 0.0;
      double distance11 = 0.0;
      double maxDistance12 = 0.0;
      double distance12 = 0.0;
      double maxDistance13 = 0.0;
      double distance13 = 0.0;
      double maxDistance14 = 0.0;
      double distance14 = 0.0;
      double maxDistance15 = 0.0;
      double distance15 = 0.0;
      double maxDistance16 = 0.0;
      double distance16 = 0.0;
      double maxDistance17 = 0.0;
      double distance17 = 0.0;
      double maxDistance18 = 0.0;
      double distance18 = 0.0;
      double maxDistance19 = 0.0;
      double distance19 = 0.0;
      double maxDistance20 = 0.0;
      double distance20 = 0.0;
      double maxDistance21 = 0.0;
      double distance21 = 0.0;
      double maxDistance22 = 0.0;
      double distance22 = 0.0;
      double maxDistance23 = 0.0;
      double distance23 = 0.0;  
 }

 

}