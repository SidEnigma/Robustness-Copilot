/*
  * Copyright (c) 2013 European Bioinformatics Institute (EMBL-EBI)
  *                    John May <jwmay@users.sf.net>
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as published by
  * the Free Software Foundation; either version 2.1 of the License, or (at
  * your option) any later version. All we ask is that proper credit is given
  * for our work, which includes - but is not limited to - adding the above
  * copyright notice to the beginning of your source code files, and to any
  * copyright notice that you may distribute with programs based on this work.
  *
  * This program is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  * License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 U
  *
  * Additionally the 'MDLValence' method has the following licence/copyright.
  *
  * Copyright (C) 2012 NextMove Software
  *
  * @@ All Rights Reserved @@ This file is part of the RDKit. The contents
  * are covered by the terms of the BSD license which is included in the file
  * license.txt, found at the root of the RDKit source tree.
  */
 
 package org.openscience.cdk.io;
 
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 
 import java.util.HashMap;
 import java.util.Map;
 
 /**
  * Adds implicit hydrogens and specifies valency using the MDL valence model.
  *
  * @author John May
  * @cdk.module io
  * @see <a href="http://nextmovesoftware.com/blog/2013/02/27/explicit-and-implicit-hydrogens-taking-liberties-with-valence/">Explicit
  *      and Implicit Hydrogens: taking liberties with valence</a>
  */
 final class MDLValence {
 
     private MDLValence() {}
 
 
/** The provided atom container has a MDL model applied to it. */
 static IAtomContainer apply(IAtomContainer container){
      Map<IAtom, Integer> valences = new HashMap<IAtom, Integer>();
      for (IAtom atom : container.atoms()) {
          valences.put(atom, getValence(atom));
      }
      for (IBond bond : container.bonds()) {
          IAtom a1 = bond.getBegin();
          IAtom a2 = bond.getEnd();
          int v1 = valences.get(a1);
          int v2 = valences.get(a2);
          int v = v1 - v2;
          if (v == 0) {
              v = 1;
          } else if (v == 1) {
              v = -1;
          } else if (v == -1) {
              v = 1;
          } else if (v == 2) {
              v = -2;
          } else if (v == -2) {
              v = 2;
          } else if (v == 3) {
              v = -3;
          } else if (v == -3) {
              v = 3;
          } else if (v == 4) {
              v = -4;
          } else if (v == -4) {
              v = 4;
          } else if (v == 5) {
              v = -5;
          } else if (v == -5) {
              v = 5;
          } else if (v == 6) {
              v = -6;
          } else if (v == -6) {
              v = 6;
          } else if (v == 7) {
              v = -7;
          } else if (v == -7) {
              v = 7;
          } else if (v == 8) {
              v = -8;
          } else if (v == -8) {
              v = 8;
          } else if (v == 9) {
              v = -9;
          } else if (v == -9) {
              v = 9;
          } else if (v == 10) {
              v = -10;
          } else if (v  
 }

 

}