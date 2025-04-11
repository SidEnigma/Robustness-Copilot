/* Copyright (C) 1997-2007  Christoph Steinbeck <steinbeck@users.sf.net>
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
  *
  */
 package org.openscience.cdk;
 
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 
 import javax.vecmath.Point2d;
 import javax.vecmath.Point3d;
 import java.io.Serializable;
 import java.util.Iterator;
 
 /**
  * Implements the concept of a covalent bond between two or more atoms. A bond is
  * considered to be a number of electrons connecting two or more  of atoms.
  * 
  * It should be noted that the majority of applications will consider 2-center bonds,
  * especially since the bond orders currently supported are really only valid for
  * 2-center bonds. However the code does support multi-center bonds, though the
  * orders may not make sense at this point.
  * 
  * In general code that assumes bonds are 2-centered can use this class seamlessly, as
  * the semantics are identical to the older versions. Care shoud be exercised when
  * using multi-center bonds using this class as the orders may not make sense.
  *
  * @author steinbeck
  * @cdk.module data
  * @cdk.githash
  * @cdk.created 2003-10-02
  * @cdk.keyword bond
  * @cdk.keyword atom
  * @cdk.keyword electron
  */
 public class Bond extends ElectronContainer implements IBond, Serializable, Cloneable {
 
     /**
      * Determines if a de-serialized object is compatible with this class.
      * 
      * This value must only be changed if and only if the new version
      * of this class is incompatible with the old version. See Sun docs
      * for <a href=http://java.sun.com/products/jdk/1.1/docs/guide
      * /serialization/spec/version.doc.html>details</a>.
      */
     private static final long serialVersionUID = 7057060562283387384L;
 
     /**
      * The bond order of this bond.
      */
     protected IBond.Order     order            = (Order) CDKConstants.UNSET;
 
     /**
      * Number of atoms contained by this object.
      */
     protected int             atomCount        = 0;
 
     /**
      * A list of atoms participating in this bond.
      */
     protected IAtom[]         atoms            = null;
 
     /**
      * A descriptor the stereochemical orientation of this bond.
      */
     protected IBond.Stereo    stereo;
 
     protected IBond.Display display = Display.Solid;
 
     /**
      * Constructs an empty bond.
      */
     public Bond() {
         this(null, null, null, IBond.Stereo.NONE);
         atomCount = 0;
     }
 
     /**
      * Constructs a bond with a single bond order..
      *
      * @param atom1 the first Atom in the bond
      * @param atom2 the second Atom in the bond
      */
     public Bond(IAtom atom1, IAtom atom2) {
         this(atom1, atom2, IBond.Order.SINGLE, IBond.Stereo.NONE);
     }
 
     /**
      * Constructs a bond with a given order.
      *
      * @param atom1 the first Atom in the bond
      * @param atom2 the second Atom in the bond
      * @param order the bond order
      */
     public Bond(IAtom atom1, IAtom atom2, Order order) {
         this(atom1, atom2, order, IBond.Stereo.NONE);
     }
 
     /**
      * Constructs a multi-center bond, with undefined order and no stereo information.
      *
      * @param atoms An array of IAtom containing the atoms constituting the bond
      */
     public Bond(IAtom[] atoms) {
         this.atoms = new IAtom[atoms.length];
         System.arraycopy(atoms, 0, this.atoms, 0, atoms.length);
         atomCount = this.atoms.length;
     }
 
     /**
      * Constructs a multi-center bond, with a specified order and no stereo information.
      *
      * @param atoms An array of IAtom containing the atoms constituting the bond
      * @param order The order of the bond
      */
     public Bond(IAtom[] atoms, Order order) {
         this.atoms = new IAtom[atoms.length];
         System.arraycopy(atoms, 0, this.atoms, 0, atoms.length);
         atomCount = this.atoms.length;
         this.order = order;
     }
 
     /**
      * Constructs a bond with a given order and stereo orientation from an array
      * of atoms.
      *
      * @param atom1  the first Atom in the bond
      * @param atom2  the second Atom in the bond
      * @param order  the bond order
      * @param stereo a descriptor the stereochemical orientation of this bond
      */
     public Bond(IAtom atom1, IAtom atom2, Order order, IBond.Stereo stereo) {
         atoms = new IAtom[2];
         atoms[0] = atom1;
         atoms[1] = atom2;
         this.order = order;
         updateElectronCount(order);
         this.stereo = stereo;
         this.atomCount = 2;
     }
 
 
/** Returns the iterator to the atoms that make up this bond. */

public Iterable<IAtom> atoms() {
    return new Iterable<IAtom>() {
        @Override
        public Iterator<IAtom> iterator() {
            return new Iterator<IAtom>() {
                private int currentIndex = 0;

                @Override
                public boolean hasNext() {
                    return currentIndex < atomCount;
                }

                @Override
                public IAtom next() {
                    return atoms[currentIndex++];
                }
            };
        }
    };
}
 

}