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
 
 
/** This method applies the MDL valence model to the provided atom container. */
 static IAtomContainer apply(IAtomContainer container){}

 

}