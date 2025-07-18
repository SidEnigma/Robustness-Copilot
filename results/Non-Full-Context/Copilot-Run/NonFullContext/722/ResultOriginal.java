/*  Copyright (C) 1997-2007  Christoph Steinbeck <steinbeck@users.sf.net>
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
 package org.openscience.cdk.silent;
 
 import java.io.Serializable;
 import java.util.Iterator;
 
 import javax.vecmath.Point2d;
 import javax.vecmath.Point3d;
 
 import org.openscience.cdk.BondRef;
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 
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
  * @cdk.module  silent
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
     protected IAtom[]         atoms            = new IAtom[2];
 
     /**
      * A descriptor the stereochemical orientation of this bond.
      */
     protected IBond.Stereo    stereo;
 
     protected IBond.Display   display = Display.Solid;
 
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
      * @param beg  the first Atom in the bond
      * @param end  the second Atom in the bond
      * @param order  the bond order
      * @param stereo a descriptor the stereochemical orientation of this bond
      */
     public Bond(IAtom beg, IAtom end, Order order, IBond.Stereo stereo) {
         atoms[0] = beg;
         atoms[1] = end;
         setOrder(order);
         this.stereo = stereo;
         this.atomCount = 2;
     }
 
     /**
      * Returns the Iterator to atoms making up this bond.
      * Iterator.remove() is not implemented.
      *
      * @return An Iterator to atoms participating in this bond
      * @see #setAtoms
      */
     @Override
     public Iterable<IAtom> atoms() {
         return new Iterable<IAtom>() {
 
             @Override
             public Iterator<IAtom> iterator() {
                 return new AtomsIterator();
             }
         };
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public int getIndex() {
         return -1;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public IAtomContainer getContainer() {
         return null;
     }
 
     /**
      * The inner Iterator class.
      */
     private class AtomsIterator implements Iterator<IAtom> {
 
         private int pointer = 0;
 
         @Override
         public boolean hasNext() {
             return pointer < atomCount;
         }
 
         @Override
         public IAtom next() {
             ++pointer;
             return atoms[pointer - 1];
         }
 
         @Override
         public void remove() {}
 
     }
 
     /**
      * Sets the array of atoms making up this bond.
      *
      * @param atoms An array of atoms that forms this bond
      * @see #atoms
      */
     @Override
     public void setAtoms(IAtom[] atoms) {
         this.atoms = atoms;
         atomCount = atoms.length;
     }
 
     /**
      * Returns the number of Atoms in this Bond.
      *
      * @return The number of Atoms in this Bond
      */
     @Override
     public int getAtomCount() {
         return atomCount;
     }
 
     /**
      * Returns an Atom from this bond.
      *
      * @param position The position in this bond where the atom is
      * @return The atom at the specified position, null if there are no atoms in the bond
      * @see #setAtom
      */
     @Override
     public IAtom getAtom(int position) {
         if (atoms == null)
             return null;
         else
             return atoms[position];
     }
 
     /**
      * {@inheritDoc}
      */
     public IAtom getBegin() {
         return atomCount < 1 ? null : atoms[0];
     }
 
     /**
      * {@inheritDoc}
      */
     public IAtom getEnd() {
         return atomCount < 2 ? null : atoms[1];
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public IAtom getOther(IAtom atom) {
         if (atoms[0].equals(atom))
             return atoms[1];
         else if (atoms[1].equals(atom))
             return atoms[0];
         return null;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public IAtom getConnectedAtom(IAtom atom) {
         return getOther(atom);
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public IAtom[] getConnectedAtoms(IAtom atom) {
         if (atomCount < 1) return null;
         IAtom[] connected = new IAtom[atomCount-1];
         int j = 0;
         for (int i = 0; i < atomCount; i++) {
             if (!this.atoms[i].equals(atom)) {
                 if (j >= connected.length)
                     return null;
                 connected[j++] = this.atoms[i];
             }
         }
         return connected;
     }
 
     /**
      * Returns true if the given atom participates in this bond.
      *
      * @param atom The atom to be tested if it participates in this bond
      * @return true if the atom participates in this bond
      */
     @Override
     public boolean contains(IAtom atom) {
         if (atoms == null) return false;
         for (IAtom localAtom : atoms) {
             if (localAtom.equals(atom)) return true;
         }
         return false;
     }
 
     /**
      * Sets an Atom in this bond.
      *
      * @param atom     The atom to be set
      * @param position The position in this bond where the atom is to be inserted
      * @see #getAtom
      */
     @Override
     public void setAtom(IAtom atom, int position) {
         if (atoms[position] == null && atom != null) atomCount++;
         if (atoms[position] != null && atom == null) atomCount--;
         atoms[position] = atom;
     }
 
     /**
      * Returns the bond order of this bond.
      *
      * @return The bond order of this bond
      * @see org.openscience.cdk.CDKConstants org.openscience.cdk.CDKConstants
      *      for predefined values.
      * @see #setOrder
      */
     @Override
     public Order getOrder() {
         return this.order;
     }
 
     /**
      * Sets the bond order of this bond.
      *
      * @param order The bond order to be assigned to this bond
      * @see org.openscience.cdk.CDKConstants
      *      org.openscience.cdk.CDKConstants for predefined values.
      * @see #getOrder
      */
     @Override
     public void setOrder(Order order) {
         this.order = order;
         if (order != null) {
             switch (order) {
                 case SINGLE:
                     this.electronCount = 2;
                     break;
                 case DOUBLE:
                     this.electronCount = 4;
                     break;
                 case TRIPLE:
                     this.electronCount = 6;
                     break;
                 case QUADRUPLE:
                     this.electronCount = 8;
                     break;
                 case QUINTUPLE:
                     this.electronCount = 10;
                     break;
                 case SEXTUPLE:
                     this.electronCount = 12;
                     break;
                 default:
                     this.electronCount = 0;
                     break;
             }
         }
     }
 
     /**
      * Returns the stereo descriptor for this bond.
      *
      * @return The stereo descriptor for this bond
      * @see #setStereo
      * @see org.openscience.cdk.CDKConstants for predefined values.
      */
     @Override
     public IBond.Stereo getStereo() {
         return this.stereo;
     }
 
     /**
      * Sets the stereo descriptor for this bond.
      *
      * @param stereo The stereo descriptor to be assigned to this bond.
      * @see #getStereo
      * @see org.openscience.cdk.CDKConstants for predefined values.
      */
     @Override
     public void setStereo(IBond.Stereo stereo) {
         this.stereo = stereo;
         if (stereo == null) {
             this.display = Display.Solid;
         } else {
             switch (stereo) {
                 case UP:
                     display = Display.WedgeBegin;
                     break;
                 case DOWN:
                     display = Display.WedgedHashBegin;
                     break;
                 case UP_INVERTED:
                     display = Display.WedgeEnd;
                     break;
                 case DOWN_INVERTED:
                     display = Display.WedgedHashEnd;
                     break;
                 case UP_OR_DOWN:
                 case UP_OR_DOWN_INVERTED:
                     display = Display.Wavy;
                     break;
             }
         }
     }
 
     /**
      * {@inheritDoc}
      */
     public IBond.Display getDisplay() {
         return display;
     }
 
     /**
      * {@inheritDoc}
      */
     public void setDisplay(IBond.Display display) {
         this.display = display;
     }
 
     /**
      * Returns the geometric 2D center of the bond.
      *
      * @return The geometric 2D center of the bond
      */
     @Override
     public Point2d get2DCenter() {
         double xOfCenter = 0;
         double yOfCenter = 0;
         for (IAtom atom : atoms) {
             xOfCenter += atom.getPoint2d().x;
             yOfCenter += atom.getPoint2d().y;
         }
 
         return new Point2d(xOfCenter / ((double) getAtomCount()), yOfCenter / ((double) getAtomCount()));
     }
 
     /**
      * Returns the geometric 3D center of the bond.
      *
      * @return The geometric 3D center of the bond
      */
     @Override
     public Point3d get3DCenter() {
         double xOfCenter = 0;
         double yOfCenter = 0;
         double zOfCenter = 0;
         for (IAtom atom : atoms) {
             xOfCenter += atom.getPoint3d().x;
             yOfCenter += atom.getPoint3d().y;
             zOfCenter += atom.getPoint3d().z;
         }
 
         return new Point3d(xOfCenter / getAtomCount(), yOfCenter / getAtomCount(), zOfCenter / getAtomCount());
     }
 
     /**
      * Compares a bond with this bond.
      *
      * @param object Object of type Bond
      * @return true if the bond is equal to this bond
      */
     @Override
     public boolean compare(Object object) {
         if (object instanceof IBond) {
             Bond bond = (Bond) object;
             for (IAtom atom : atoms) {
                 if (!bond.contains(atom)) {
                     return false;
                 }
             }
 
             // not important ??!!
             //if (order==bond.order)
             //  return false;
 
             return true;
         }
         return false;
     }
 
 
/** Checks whether a bond is connected to another one. */
 public boolean isConnectedTo(IBond bond){
        for (IAtom atom : atoms) {
            if (bond.contains(atom)) return true;
        }
        return false;
    }
    
        /**
        * Returns the number of atoms in this bond.
        *
        * @return The number of atoms in this bond
        */
        @Override
        public int getAtomCount() {
            return atomCount;
        }
    
        /**
        * Returns the number of electrons in this bond.
        *
        * @return The number of electrons in this bond
        */
        @Override
        public int getElectronCount() {
            return electronCount;
        }
    
        /**
        * Returns the number of bonds in this bond.
        *
        * @return The number of bonds in this bond
        */
        @Override
        public int getBondCount() {
            return 1;
        }
    
        /**
        * Returns the number of bonds in this bond.
        *
        * @return The number of bonds in this bond
        */
        @Override
        public int getBondOrder() {
            return order.numeric();
        }
    
        /**
        * Returns the number of bonds in this bond.
        *
        * @return The number of bonds in this bond
        */
        @Override
        public int getBondOrderAsDouble() {
            return order.numeric();
        }
    
        /**
        * Returns the number of bonds in this bond.
        *
        * @return The number of bonds in this bond
        */
        @Override
        public int getBondOrderAsString() {
            return order.numeric();
        }
    
        /**
        * Returns the number of bonds in this bond.
        *
        * @return The number of bonds in this bond
        */
        @Override
        public int getBondOrderAsInt() {
            return order.numeric();
        }
    
        /**
        * Returns the number of bonds in this bond.
        *
        * @return The number of bonds in this bond
        */
        @Override
        public int get      
 }

 

}