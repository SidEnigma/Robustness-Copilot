/* Copyright (C) 2005-2008  Egon Willighagen <egonw@users.sf.net>
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public License
  * as published by the Free Software Foundation, version 2.1.
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
 package org.openscience.cdk.atomtype;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.openscience.cdk.config.AtomTypeFactory;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IAtomType;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 import org.openscience.cdk.tools.manipulator.BondManipulator;
 
 /**
  * AtomTypeMatcher that finds an AtomType by matching the Atom's element symbol.
  * This atom type matcher takes into account formal charge and number of
  * implicit hydrogens, and requires bond orders to be given.
  *
  * <p>This class uses the <b>cdk/config/data/structgen_atomtypes.xml</b>
  * list. If there is not an atom type defined for the tested atom, then null
  * is returned.
  *
  * @author         egonw
  * @cdk.created    2006-09-22
  * @cdk.module     structgen
  * @cdk.githash
  */
 public class StructGenAtomTypeGuesser implements IAtomTypeGuesser {
 
     private static AtomTypeFactory factory = null;
     private static ILoggingTool    logger  = LoggingToolFactory.createLoggingTool(StructGenAtomTypeGuesser.class);
 
     /**
      * Constructor for the StructGenMatcher object.
      */
     public StructGenAtomTypeGuesser() {}
 
 
/** Finds the AtomType corresponding to the element symbol, formal load, and hybridization state of the Atom. */
 public List<IAtomType> possibleAtomTypes(IAtomContainer atomContainer, IAtom atom) throws CDKException{
      List<IAtomType> atomTypes = new ArrayList<IAtomType>();
      String symbol = atom.getSymbol();
      int formalCharge = atom.getFormalCharge();
      int implicitHydrogenCount = atom.getImplicitHydrogenCount();
      int bondCount = atomContainer.getConnectedBondsCount(atom);
      int bondOrderSum = 0;
      for (IBond bond : atomContainer.getConnectedBondsList(atom)) {
          bondOrderSum += BondManipulator.getOrder(bond);
      }
      if (factory == null) {
          factory = AtomTypeFactory.getInstance();
      }
      IAtomType type = factory.getAtomType(symbol, formalCharge, implicitHydrogenCount, bondCount, bondOrderSum);
      if (type != null) {
          atomTypes.add(type);
      }
      return atomTypes; 
 }

 

}