/* Copyright (C) 2006-2010  Syed Asad Rahman <asad@ebi.ac.uk>
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
  * MERCHANTABILITY or FITNESS FOR sourceAtom PARTICULAR PURPOSE.  See the
  * GNU Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 package org.openscience.cdk.smsd.algorithm.mcsplus;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.TreeMap;
 
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.isomorphism.matchers.IQueryAtomContainer;
 import org.openscience.cdk.smsd.filters.PostFilter;
 import org.openscience.cdk.smsd.helper.FinalMappings;
 import org.openscience.cdk.smsd.interfaces.AbstractMCSAlgorithm;
 import org.openscience.cdk.smsd.interfaces.IMCSBase;
 import org.openscience.cdk.smsd.tools.MolHandler;
 
 /**
  * This class acts as a handler class for MCSPlus algorithm.
  * {@link org.openscience.cdk.smsd.algorithm.mcsplus.MCSPlus}
  * @cdk.module smsd
  * @cdk.githash
  * @author Syed Asad Rahman &lt;asad@ebi.ac.uk&gt;
  * @deprecated SMSD has been deprecated from the CDK with a newer, more recent
  *             version of SMSD is available at <a href="http://github.com/asad/smsd">http://github.com/asad/smsd</a>.
  */
 @Deprecated
 public class MCSPlusHandler extends AbstractMCSAlgorithm implements IMCSBase {
 
     private static List<Map<IAtom, IAtom>>     allAtomMCS   = null;
     private static Map<IAtom, IAtom>           atomsMCS     = null;
     private static Map<Integer, Integer>       firstMCS     = null;
     private static List<Map<Integer, Integer>> allMCS       = null;
     private IAtomContainer                     source       = null;
     private IAtomContainer                     target       = null;
     private boolean                            flagExchange = false;
 
     /**
      * Constructor for the MCS Plus algorithm class
      */
     public MCSPlusHandler() {
         allAtomMCS = new ArrayList<Map<IAtom, IAtom>>();
         atomsMCS = new HashMap<IAtom, IAtom>();
         firstMCS = new TreeMap<Integer, Integer>();
         allMCS = new ArrayList<Map<Integer, Integer>>();
     }
 
     /** {@inheritDoc}
      *
      * @param source
      * @param target
      */
     @Override
     public synchronized void set(MolHandler source, MolHandler target) {
         this.source = source.getMolecule();
         this.target = target.getMolecule();
     }
 
     /** {@inheritDoc}
      *
      * @param source
      * @param target
      */
     @Override
     public void set(IQueryAtomContainer source, IAtomContainer target) {
         this.source = source;
         this.target = target;
     }
 
 
/** {@inheritDoc}  Function is called by the main program and serves as a starting point for the comparison procedure. */
 public synchronized void searchMCS(boolean shouldMatchBonds){
        try {
            if (source == null || target == null) {
                throw new CDKException("Source or target is null");
            }
            if (source.getAtomCount() == 0 || target.getAtomCount() == 0) {
                throw new CDKException("Source or target is empty");
            }
            if (source.getAtomCount() > target.getAtomCount()) {
                flagExchange = true;
                IAtomContainer temp = source;
                source = target;
                target = temp;
            }
            if (shouldMatchBonds) {
                matchBonds();
            }
            matchAtoms();
            if (flagExchange) {
                flagExchange = false;
                IAtomContainer temp = source;
                source = target;
                target = temp;
            }
            if (atomsMCS.size() == 0) {
                throw new CDKException("No MCS found");
            }
            FinalMappings.setMCS(atomsMCS);
            FinalMappings.setFirstMCS(firstMCS);
            FinalMappings.setAllMCS(allMCS);
            FinalMappings.setAllAtomMCS(allAtomMCS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
        /** {@inheritDoc} */
        @Override
        public synchronized void matchBonds() {
            try {
                if (source == null || target == null) {
                    throw new CDKException("Source or target is null");
                }
                if (source.getAtomCount() == 0 || target.getAtomCount() == 0) {
                    throw new CDKException("Source or target is empty");
                }
                if (source.getAtomCount() > target.getAtomCount()) {
                    flagExchange = true;
                    IAtomContainer temp = source;
                    source = target;
                    target = temp;
                }
                matchBonds(source, target);
                if (flagExchange) {
                    flagExchange = false;
                    I       
 }

 

}