/* Copyright (C) 2005-2007  Christian Hoppe <chhoppe@users.sf.net>
  *
  *  Contact: cdk-devel@list.sourceforge.net
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
 package org.openscience.cdk.charges;
 
 import java.util.Iterator;
 
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 
 /**
  * <p>The calculation of the Gasteiger Marsili (PEOE) partial charges is based on
  * {@cdk.cite GM80}. This class only implements the original method which only
  * applies to &sigma;-bond systems.</p>
  *
  *
  * @author      chhoppe
  * @author      rojas
  *
  * @cdk.module  charges
  * @cdk.githash
  * @cdk.created 2004-11-03
  * @cdk.keyword partial atomic charges
  * @cdk.keyword charge distribution
  * @cdk.keyword electronegativities, partial equalization of orbital
  * @cdk.keyword PEOE
  */
 public class GasteigerMarsiliPartialCharges implements IChargeCalculator {
 
     private double DEOC_HYDROGEN = 20.02;
     private double MX_DAMP       = 0.5;
     private double MX_ITERATIONS = 20;
     private int    STEP_SIZE     = 5;
 
     /** Flag is set if the formal charge of a chemobject is changed due to resonance.*/
 
     /**
      *  Constructor for the GasteigerMarsiliPartialCharges object.
      */
     public GasteigerMarsiliPartialCharges() {}
 
     /**
     *  Sets chi_cat value for hydrogen, because H poses a special problem due to lack of possible second ionisation.
      *
      *@param  chiCat  The new DEOC_HYDROGEN value
      */
     public void setChiCatHydrogen(double chiCat) {
         DEOC_HYDROGEN = chiCat;
     }
 
     /**
      *  Sets the maxGasteigerDamp attribute of the GasteigerMarsiliPartialCharges
      *  object.
      *
      *@param  damp  The new maxGasteigerDamp value
      */
     public void setMaxGasteigerDamp(double damp) {
         MX_DAMP = damp;
     }
 
     /**
      *  Sets the maxGasteigerIters attribute of the GasteigerMarsiliPartialCharges
      *  object.
      *
      *@param  iters  The new maxGasteigerIters value
      */
     public void setMaxGasteigerIters(double iters) {
         MX_ITERATIONS = iters;
     }
 
     /**
      *  Gets chi_cat value for hydrogen, because H poses a special problem due to lack of possible second ionisation.
       *
       * @return  The new DEOC_HYDROGEN value
       */
     public double getChiCatHydrogen() {
         return DEOC_HYDROGEN;
     }
 
     /**
      *  Gets the maxGasteigerDamp attribute of the GasteigerMarsiliPartialCharges
      *  object.
      *
      * @return  The new maxGasteigerDamp value
      */
     public double getMaxGasteigerDamp() {
         return MX_DAMP;
     }
 
     /**
      *  Gets the maxGasteigerIters attribute of the GasteigerMarsiliPartialCharges
      *  object.
      *
      * @return  The new maxGasteigerIters value
      */
     public double getMaxGasteigerIters() {
         return MX_ITERATIONS;
     }
 
 
/** The main method assigns partial sigma charges. */
 public IAtomContainer assignGasteigerMarsiliSigmaPartialCharges(IAtomContainer ac, boolean setCharge) throws Exception{
        if (ac == null) {
            throw new NullPointerException("The AtomContainer is null.");
        }
        if (setCharge) {
            setPartialCharges(ac);
        } else {
            getPartialCharges(ac);
        }
        return ac;
    }
    
        /**
        *  Assigns partial charges to the atoms in the given AtomContainer.
        *
        *@param  ac  The AtomContainer to assign partial charges to
        *@throws  CDKException  if the AtomContainer is null
        */
        public void setPartialCharges(IAtomContainer ac) throws CDKException {
            if (ac == null) {
                throw new NullPointerException("The AtomContainer is null.");
            }
            for (int i = 0; i < ac.getAtomCount(); i++) {
                IAtom atom = ac.getAtom(i);
                atom.setCharge(getPartialCharge(atom));
            }
        }
    
        /**
        *  Gets the partial charges of the atoms in the given AtomContainer.
        *
        *@param  ac  The AtomContainer to get partial charges from
        *@throws  CDKException  if the AtomContainer is null
        */
        public void getPartialCharges(IAtomContainer ac) throws CDKException {
            if (ac == null) {
                throw new NullPointerException("The AtomContainer is null.");
            }
            for (int i = 0; i < ac.getAtomCount(); i++) {
                IAtom atom = ac.getAtom(i);
                atom.setCharge(getPartialCharge(atom));
            }
        }
    
        /**
        *  Gets the partial charge of the given Atom.
        *
        *@param  atom  The Atom to get the partial charge from
        *@return       The partial charge of the Atom
        *@throws  CDKException  if the Atom is null
        */
        public double getPartialCharge(IAtom atom) throws CDKException {
            if (atom == null) {
                throw new       
 }

 

}