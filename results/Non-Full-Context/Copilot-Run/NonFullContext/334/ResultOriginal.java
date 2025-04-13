/* Copyright (C) 1997-2007  Christoph Steinbeck <steinbeck@users.sf.net>
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
 package org.openscience.cdk;
 
 import org.openscience.cdk.event.ChemObjectChangeEvent;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.interfaces.IChemObjectChangeEvent;
 import org.openscience.cdk.interfaces.IChemObjectListener;
 
 import java.io.Serializable;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Objects;
 
 /**
  *  The base class for all chemical objects in this cdk. It provides methods for
  *  adding listeners and for their notification of events, as well a a hash
  *  table for administration of physical or chemical properties
  *
  *@author        steinbeck
  * @cdk.githash
  *@cdk.module    data
  */
 public class ChemObject implements Serializable, IChemObject, Cloneable {
 
     /**
      * Determines if a de-serialized object is compatible with this class.
      *
      * This value must only be changed if and only if the new version
      * of this class is incompatible with the old version. See Sun docs
      * for <a href=http://java.sun.com/products/jdk/1.1/docs/guide
      * /serialization/spec/version.doc.html>details</a>.
      */
     private static final long         serialVersionUID = 2798134548764323328L;
 
     /**
      * List for listener administration.
      */
     private List<IChemObjectListener> chemObjectListeners;
     /**
      *  A hashtable for the storage of any kind of properties of this IChemObject.
      */
     private Map<Object, Object>       properties;
     /**
      *  You will frequently have to use some flags on a IChemObject. For example, if
      *  you want to draw a molecule and see if you've already drawn an atom, or in
      *  a ring search to check whether a vertex has been visited in a graph
      *  traversal. Use these flags while addressing particular positions in the
      *  flag array with self-defined constants (flags[VISITED] = true). 100 flags
      *  per object should be more than enough.
      */
     private short                     flags;                                  // flags are currently stored as a single short value MAX_FLAG_INDEX < 16
 
     /**
      *  The ID is null by default.
      */
     private String                    identifier;
 
     /**
      *  Constructs a new IChemObject.
      */
     public ChemObject() {
         chemObjectListeners = null;
         properties = null;
         identifier = null;
     }
 
     /**
      * Constructs a new IChemObject by copying the flags, and the
      * identifier. It does not copy the listeners and properties.
      *
      * @param chemObject the object to copy
      */
     public ChemObject(IChemObject chemObject) {
         // copy the flags
         flags = chemObject.getFlagValue().shortValue();
         // copy the identifier
         identifier = chemObject.getID();
     }
 
     /**
      *  Lazy creation of chemObjectListeners List.
      *
      *@return    List with the ChemObjects associated.
      */
     private List<IChemObjectListener> lazyChemObjectListeners() {
         if (chemObjectListeners == null) {
             chemObjectListeners = new ArrayList<IChemObjectListener>();
         }
         return chemObjectListeners;
     }
 
 
/** Use this to add yourself to this IChemObject as a listener. */
 public void addListener(IChemObjectListener col){
        lazyChemObjectListeners().add(col);     
 }

 

}