/* Copyright (C) 2001-2007  Edgar Luttmann <edgar@uni-paderborn.de>
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
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  *  */
 package org.openscience.cdk;
 
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.interfaces.ILonePair;
 import org.openscience.cdk.interfaces.IMonomer;
 import org.openscience.cdk.interfaces.IPolymer;
 import org.openscience.cdk.interfaces.ISingleElectron;
 import org.openscience.cdk.interfaces.IStereoElement;
 import org.openscience.cdk.sgroup.Sgroup;
 import org.openscience.cdk.tools.manipulator.SgroupManipulator;
 
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.Hashtable;
 import java.util.Map;
 
 /**
  * Subclass of Molecule to store Polymer specific attributes that a Polymer has.
  *
  * @cdk.module data
  * @cdk.githash
  *
  * @author Edgar Luttmann &lt;edgar@uni-paderborn.de&gt;
  * @author Martin Eklund &lt;martin.eklund@farmbio.uu.se&gt;
  * @cdk.created 2001-08-06
  * @cdk.keyword polymer
  */
 public class Polymer extends AtomContainer implements java.io.Serializable, IPolymer {
 
     /**
      * Determines if a de-serialized object is compatible with this class.
      *
      * This value must only be changed if and only if the new version
      * of this class is incompatible with the old version. See Sun docs
      * for <a href=http://java.sun.com/products/jdk/1.1/docs/guide/serialization/spec/version.doc.html>details</a>.
      */
     private static final long     serialVersionUID = -2596790658835319339L;
 
     private Map<String, IMonomer> monomers;                                // the list of all the contained Monomers.
 
     /**
      * Constructs a new Polymer to store the Monomers.
      */
     public Polymer() {
         super();
         monomers = new Hashtable<String, IMonomer>();
     }
 
     /**
      * Adds the atom oAtom to a specified Monomer.
      *
      * @param oAtom  The atom to add
      * @param oMonomer  The monomer the atom belongs to
      */
     @Override
     public void addAtom(IAtom oAtom, IMonomer oMonomer) {
 
         if (!contains(oAtom)) {
             super.addAtom(oAtom);
 
             if (oMonomer != null) { // Not sure what's better here...throw nullpointer exception?
                 oMonomer.addAtom(oAtom);
                 if (!monomers.containsKey(oMonomer.getMonomerName())) {
                     monomers.put(oMonomer.getMonomerName(), oMonomer);
                 }
             }
         }
         /*
          * notifyChanged() is called by addAtom in AtomContainer
          */
     }
 
     /**
      * Returns the number of monomers present in the Polymer.
      *
      * @return number of monomers
      */
     @Override
     public int getMonomerCount() {
         return monomers.size();
     }
 
     /**
      * Retrieves a Monomer object by specifying its name.
      *
      * @param cName  The name of the monomer to look for
      * @return The Monomer object which was asked for
      */
     @Override
     public IMonomer getMonomer(String cName) {
         return monomers.get(cName);
     }
 
     /**
      * Returns a collection of the names of all <code>Monomer</code>s in this
      * polymer.
      *
      * @return a <code>Collection</code> of all the monomer names.
      */
     @Override
     public Collection<String> getMonomerNames() {
         return monomers.keySet();
     }
 
 
/** Removes a particular monomer, specified by name. */
 public void removeMonomer(String name){}

 

}