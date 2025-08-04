/* Copyright (C) 2001-2007  The Chemistry Development Kit (CDK) Project
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
 package org.openscience.cdk.aromaticity;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IRing;
 
 /**
  * @cdk.module standard
  * @cdk.githash
  *
  * @author Oliver Horlacher &lt;oliver.horlacher@therastrat.com&gt;
  * @cdk.created    2002-03-14
  *
  * @cdk.keyword aromaticity detector
  * @deprecated use {@link Aromaticity}
  */
 @Deprecated
 public class AromaticityCalculator {
 
 
/** Tests <code>the aromaticity of the ring</code> in the <code>molecule</code>. */
 public static boolean isAromatic(IRing ring, IAtomContainer atomContainer){
  
      if(ring.getFlag(CDKConstants.ISAROMATIC)) return true;
  
      if(ring.getFlag(CDKConstants.ISALIPHATIC)) return false;
  
      int size = ring.getAtomCount();
  
      if(size < 5) return false;
  
      int[] ringAtomFlags = new int[size];
  
      for(int i = 0; i < size; i++){
          IAtom atom = ring.getAtomAt(i);
          ringAtomFlags[i] = atom.getFlag(CDKConstants.ISAROMATIC) ? 1 : 0;
      }
  
      int[] ringBondFlags = new int[size];
  
      for(int i = 0; i < size; i++){
          IBond bond = ring.getBond(i);
          ringBondFlags[i] = bond.getFlag(CDKConstants.ISAROMATIC) ? 1 : 0;
      }
  
      int[] atomFlags = new int[size];
  
      for(int i = 0; i < size; i++){
          IAtom atom = ring.getAtomAt(i);
          atomFlags[i] = atom.getFlag(CDKConstants.ISAROMATIC) ? 1 : 0;
      }
  
      int[] bondFlags = new int[size];
  
      for(int i = 0; i < size; i++){
          IBond bond = ring.getBond(i);
          bondFlags[i] = bond.getFlag(CDKConstants.ISAROMATIC) ? 1 : 0;
      }
  
      int[] atomConnectivityFlags = new int[size];
  
      for(int i = 0; i < size; i++){
          IAtom atom = ring.getAtomAt(i);
          atomConnectivityFlags[i] = atom.getFlag(CDKConstants.ISAROMATIC) ? 1 : 0;
      }
  
      int[] bondConnectivityFlags = new int[      
 }

                                                          }
