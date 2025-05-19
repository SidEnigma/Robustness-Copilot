/* Copyright (C) 1997-2007  Christoph Steinbeck <steinbeck@users.sf.net>
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public License
  * as published by the Free Software Foundation; either version 2.1
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 
 package org.openscience.cdk.silent;
 
 import java.io.Serializable;
 
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IRing;
 import org.openscience.cdk.interfaces.IRingSet;
 
 /**
  * Maintains a set of Ring objects.
  *
  * @cdk.module  silent
  * @cdk.githash
  *
  * @cdk.keyword     ring, set of
  */
 public class RingSet extends AtomContainerSet implements Serializable, IRingSet, Cloneable {
 
     private static final long serialVersionUID = 7168431521057961434L;
 
     /** Flag to denote that the set is order with the largest ring first? */
     public final static int   LARGE_FIRST      = 1;
     /** Flag to denote that the set is order with the smallest ring first? */
     public final static int   SMALL_FIRST      = 2;
 
     /**
      * The constructor.
      *
      */
     public RingSet() {
         super();
     }
 
     /**
      * Returns a vector of all rings that this bond is part of.
      *
      * @param   bond  The bond to be checked
      * @return   A vector of all rings that this bond is part of
      */
 
     @Override
     public IRingSet getRings(IBond bond) {
         IRingSet rings = bond.getBuilder().newInstance(IRingSet.class);
         Ring ring;
         for (int i = 0; i < getAtomContainerCount(); i++) {
             ring = (Ring) getAtomContainer(i);
             if (ring.contains(bond)) {
                 rings.addAtomContainer(ring);
             }
         }
         return rings;
     }
 
     /**
      * Returns a vector of all rings that this atom is part of.
      *
      * @param   atom  The atom to be checked
      * @return   A vector of all rings that this bond is part of
      */
 
     @Override
     public IRingSet getRings(IAtom atom) {
         IRingSet rings = new RingSet();
         IRing ring;
         for (int i = 0; i < getAtomContainerCount(); i++) {
             ring = (Ring) getAtomContainer(i);
             if (ring.contains(atom)) {
                 rings.addAtomContainer(ring);
             }
         }
         return rings;
     }
 
     /**
      * Returns all the rings in the RingSet that share
      * one or more atoms with a given ring.
      *
      * @param   ring  A ring with which all return rings must share one or more atoms
      * @return  All the rings that share one or more atoms with a given ring.
      */
 
     @Override
     public IRingSet getConnectedRings(IRing ring) {
         IRingSet connectedRings = ring.getBuilder().newInstance(IRingSet.class);
         IRing tempRing;
         IAtom atom;
         for (int i = 0; i < ring.getAtomCount(); i++) {
             atom = ring.getAtom(i);
             for (int j = 0; j < getAtomContainerCount(); j++) {
                 tempRing = (IRing) getAtomContainer(j);
                 if (tempRing != ring && !connectedRings.contains(tempRing) && tempRing.contains(atom)) {
                     connectedRings.addAtomContainer(tempRing);
                 }
             }
         }
         return connectedRings;
     }
 
 
/** If they are not part of the ring set, add all of them. */
 public void add(IRingSet ringSet){
        for (int i = 0; i < ringSet.getAtomContainerCount(); i++) {
            addAtomContainer(ringSet.getAtomContainer(i));
        }
    }
    
        /**
        * Returns all the rings in the RingSet that share
        * one or more atoms with a given ring.
        *
        * @param   ring  A ring with which all return rings must share one or more atoms
        * @return  All the rings that share one or more atoms with a given ring.
        */
    
        @Override
        public IRingSet getConnectedRings(IAtom atom) {
            IRingSet connectedRings = new RingSet();
            IRing tempRing;
            for (int i = 0; i < getAtomContainerCount(); i++) {
                tempRing = (IRing) getAtomContainer(i);
                if (tempRing.contains(atom)) {
                    connectedRings.addAtomContainer(tempRing);
                }
            }
            return connectedRings;
        }
    
        /**
        * Returns all the rings in the RingSet that share
        * one or more atoms with a given ring.
        *
        * @param   ring  A ring with which all return rings must share one or more atoms
        * @return  All the rings that share one or more atoms with a given ring.
        */
    
        @Override
        public IRingSet getConnectedRings(IBond bond) {
            IRingSet connectedRings = new RingSet();
            IRing tempRing;
            for (int i = 0; i < getAtomContainerCount(); i++) {
                tempRing = (IRing) getAtomContainer(i);
                if (tempRing.contains(bond)) {
                    connectedRings.addAtomContainer(tempRing);
                }
            }
            return connectedRings;
        }
    
        /**
        * Returns all the rings in the RingSet that share
        * one or more atoms with a given ring.
        *
        * @param   ring  A ring with which all return rings must share one or more atoms        
 }

 

}