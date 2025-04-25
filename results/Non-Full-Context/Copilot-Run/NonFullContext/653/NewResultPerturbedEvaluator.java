/* 
  * Copyright (C) 2003-2007  Christoph Steinbeck <steinbeck@users.sf.net>
  *                    2014  Mark B Vine (orcid:0000-0002-7794-0426)
  *
  *  Contact: cdk-devel@lists.sourceforge.net
  *
  *  This program is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU Lesser General Public License
  *  as published by the Free Software Foundation; either version 2.1
  *  of the License, or (at your option) any later version.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU Lesser General Public License for more details.
  *
  *  You should have received a copy of the GNU Lesser General Public License
  *  along with this program; if not, write to the Free Software
  *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 package org.openscience.cdk;
 
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomContainerSet;
 import org.openscience.cdk.interfaces.IChemObjectChangeEvent;
 import org.openscience.cdk.interfaces.IChemObjectListener;
 
 import java.io.Serializable;
 import java.util.Arrays;
 import java.util.Comparator;
 import java.util.Iterator;
 
 /**
  * A set of AtomContainers.
  *
  * @author        hel
  * @cdk.module    data
  * @cdk.githash
  */
 public class AtomContainerSet extends ChemObject implements Serializable, IAtomContainerSet, IChemObjectListener,
         Cloneable {
 
     /**
      * Determines if a de-serialized object is compatible with this class.
      *
      * This value must only be changed if and only if the new version
      * of this class is incompatible with the old version. See Sun docs
      * for <a href=http://java.sun.com/products/jdk/1.1/docs/guide/serialization/spec/version.doc.html>details</a>.
      */
     private static final long  serialVersionUID = -521290255592768395L;
 
     /**  Array of AtomContainers. */
     protected IAtomContainer[] atomContainers;
 
     /**  Number of AtomContainers contained by this container. */
     protected int              atomContainerCount;
 
     /**
      * Defines the number of instances of a certain molecule
      * in the set. It is 1 by default.
      */
     protected Double[]         multipliers;
 
     /**
      *  Amount by which the AtomContainers array grows when elements are added and
      *  the array is not large enough for that.
      */
     protected int              growArraySize    = 5;
 
     /**  Constructs an empty AtomContainerSet. */
     public AtomContainerSet() {
         atomContainerCount = 0;
         atomContainers = new IAtomContainer[growArraySize];
         multipliers = new Double[growArraySize];
     }
 
     /**
      * Adds an atomContainer to this container.
      *
      * @param  atomContainer  The atomContainer to be added to this container
      */
     @Override
     public void addAtomContainer(IAtomContainer atomContainer) {
         atomContainer.addListener(this);
         addAtomContainer(atomContainer, 1.0);
         /*
          * notifyChanged is called below
          */
     }
 
     /**
      * Removes an AtomContainer from this container.
      *
      * @param  atomContainer  The atomContainer to be removed from this container
      */
     @Override
     public void removeAtomContainer(IAtomContainer atomContainer) {
         for (int i = atomContainerCount - 1; i >= 0; i--) {
             if (atomContainers[i] == atomContainer) removeAtomContainer(i);
         }
     }
 
     /**
      * Removes all AtomContainer from this container.
      */
     @Override
     public void removeAllAtomContainers() {
         for (int pos = atomContainerCount - 1; pos >= 0; pos--) {
             atomContainers[pos].removeListener(this);
             multipliers[pos] = 0.0;
             atomContainers[pos] = null;
         }
         atomContainerCount = 0;
         notifyChanged();
     }
 
     /**
      * Removes an AtomContainer from this container.
      *
      * @param  pos  The position of the AtomContainer to be removed from this container
      */
     @Override
     public void removeAtomContainer(int pos) {
         atomContainers[pos].removeListener(this);
         for (int i = pos; i < atomContainerCount - 1; i++) {
             atomContainers[i] = atomContainers[i + 1];
             multipliers[i] = multipliers[i + 1];
         }
         atomContainers[atomContainerCount - 1] = null;
         atomContainerCount--;
         notifyChanged();
     }
 
 
/** The AtomContainer will be replaced at a specific given position. */

public void replaceAtomContainer(int position, IAtomContainer container) {
    if (position < 0 || position >= atomContainerCount) {
        throw new IndexOutOfBoundsException("Invalid position");
    }
    
    atomContainers[position].removeListener(this);
    atomContainers[position] = container;
    container.addListener(this);
    
    notifyChanged();
}
 

}