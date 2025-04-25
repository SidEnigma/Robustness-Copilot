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
  */
 
 package org.openscience.cdk.smiles;
 
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.interfaces.IPseudoAtom;
 import org.openscience.cdk.interfaces.IStereoElement;
 import org.openscience.cdk.stereo.DoubleBondStereochemistry;
 import org.openscience.cdk.stereo.ExtendedCisTrans;
 import org.openscience.cdk.stereo.Octahedral;
 import org.openscience.cdk.stereo.SquarePlanar;
 import org.openscience.cdk.stereo.TetrahedralChirality;
 import org.openscience.cdk.stereo.TrigonalBipyramidal;
 import uk.ac.ebi.beam.Atom;
 import uk.ac.ebi.beam.Bond;
 import uk.ac.ebi.beam.Configuration;
 import uk.ac.ebi.beam.Edge;
 import uk.ac.ebi.beam.Element;
 import uk.ac.ebi.beam.Graph;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 
 import static org.openscience.cdk.CDKConstants.ATOM_ATOM_MAPPING;
 import static org.openscience.cdk.interfaces.IDoubleBondStereochemistry.Conformation;
 import static org.openscience.cdk.interfaces.ITetrahedralChirality.Stereo;
 
 /**
  * Convert the Beam toolkit object model to the CDK. Currently the aromatic
  * bonds from SMILES are loaded as singly bonded {@link IBond}s with the {@link
  * org.openscience.cdk.CDKConstants#ISAROMATIC} flag set.
  *
  * <blockquote><pre>
  * IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
  * ChemicalGraph      g       = ChemicalGraph.fromSmiles("CCO");
  *
  * BeamToCDK          g2c     = new BeamToCDK(builder);
  *
  * // make sure the Beam notation is expanded - this converts organic
  * // subset atoms with inferred hydrogen counts to atoms with a
  * // set implicit hydrogen property
  * IAtomContainer    ac       = g2c.toAtomContainer(Functions.expand(g));
  * </pre></blockquote>
  *
  * @author John May
  * @cdk.module smiles
  * @see <a href="http://johnmay.github.io/beam">Beam SMILES Toolkit</a>
  */
 final class BeamToCDK {
 
     /** The builder used to create the CDK objects. */
     private final IChemObjectBuilder builder;
 
     /** Base atom objects for cloning - SMILES is very efficient and noticeable
      *  lag is seen using the IChemObjectBuilders. */
     private final IAtom              templateAtom;
 
     /** Base atom objects for cloning - SMILES is very efficient and noticeable
      *  lag is seen using the IChemObjectBuilders. */
     private final IBond              templateBond;
 
     /**
      * Base atom container for cloning - SMILES is very efficient and noticeable
      * lag is seen using the IChemObjectBuilders.
      */
     private final IAtomContainer     emptyContainer;
 
     /**
      * Create a new converter for the Beam SMILES toolkit. The converter needs
      * an {@link IChemObjectBuilder}. Currently the 'cdk-silent' builder will
      * give the best performance.
      *
      * @param builder chem object builder
      */
     BeamToCDK(IChemObjectBuilder builder) {
         this.builder = builder;
         this.templateAtom = builder.newInstance(IAtom.class);
         this.templateBond = builder.newInstance(IBond.class);
         this.emptyContainer = builder.newInstance(IAtomContainer.class, 0, 0, 0, 0);
     }
 
     /**
      * Convert a Beam ChemicalGraph to a CDK IAtomContainer.
      *
      * @param g Beam graph instance
      * @param kekule the input has been kekulzied
      * @return the CDK {@link IAtomContainer} for the input
      * @throws IllegalArgumentException the Beam graph was not 'expanded' - and
      *                                  contained organic subset atoms. If this
      *                                  happens use the Beam Functions.expand()
      *                                  to
      */
     IAtomContainer toAtomContainer(Graph g, boolean kekule) {
 
         IAtomContainer ac    = emptyContainer();
         int            numAtoms = g.order();
         IAtom[]        atoms = new IAtom[numAtoms];
         IBond[]        bonds = new IBond[g.size()];
 
         int j = 0; // bond index
 
         boolean checkAtomStereo = false;
         boolean checkBondStereo = false;
 
         for (int i = 0; i < g.order(); i++) {
             checkAtomStereo = checkAtomStereo || g.configurationOf(i).type() != Configuration.Type.None;
             atoms[i] = toCDKAtom(g.atom(i), g.implHCount(i));
         }
         ac.setAtoms(atoms);
         // get the atom-refs
         for (int i = 0; i < g.order(); i++)
             atoms[i] = ac.getAtom(i);
         for (Edge edge : g.edges()) {
 
             final int u = edge.either();
             final int v = edge.other(u);
             IBond bond = builder.newBond();
             bond.setAtoms(new IAtom[]{atoms[u], atoms[v]});
             bonds[j++] = bond;
 
             switch (edge.bond()) {
                 case SINGLE:
                     bond.setOrder(IBond.Order.SINGLE);
                     break;
                 case UP:
                 case DOWN:
                     checkBondStereo = true;
                     bond.setOrder(IBond.Order.SINGLE);
                     break;
                 case IMPLICIT:
                     bond.setOrder(IBond.Order.SINGLE);
                     if (!kekule && atoms[u].isAromatic() && atoms[v].isAromatic()) {
                         bond.setIsAromatic(true);
                         bond.setOrder(IBond.Order.UNSET);
                         atoms[u].setIsAromatic(true);
                         atoms[v].setIsAromatic(true);
                     }
                     break;
                 case IMPLICIT_AROMATIC:
                 case AROMATIC:
                     bond.setOrder(IBond.Order.SINGLE);
                     bond.setIsAromatic(true);
                     atoms[u].setIsAromatic(true);
                     atoms[v].setIsAromatic(true);
                     break;
                 case DOUBLE:
                     bond.setOrder(IBond.Order.DOUBLE);
                     break;
                 case DOUBLE_AROMATIC:
                     bond.setOrder(IBond.Order.DOUBLE);
                     bond.setIsAromatic(true);
                     atoms[u].setIsAromatic(true);
                     atoms[v].setIsAromatic(true);
                     break;
                 case TRIPLE:
                     bond.setOrder(IBond.Order.TRIPLE);
                     break;
                 case QUADRUPLE:
                     bond.setOrder(IBond.Order.QUADRUPLE);
                     break;
                 default:
                     throw new IllegalArgumentException("Edge label " + edge.bond()
                                                        + "cannot be converted to a CDK bond order");
             }
         }
 
         // atom-centric stereo-specification (only tetrahedral ATM)
         if (checkAtomStereo) {
             for (int u = 0; u < g.order(); u++) {
 
                 Configuration c = g.configurationOf(u);
                 switch (c.type()) {
                     case Tetrahedral: {
 
                         IStereoElement se = newTetrahedral(u, g.neighbors(u), atoms, c);
 
                         if (se != null) ac.addStereoElement(se);
                         break;
                     }
                     case ExtendedTetrahedral: {
                         IStereoElement se = newExtendedTetrahedral(u, g, atoms);
                         if (se != null) ac.addStereoElement(se);
                         break;
                     }
                     case DoubleBond: {
                         checkBondStereo = true;
                         break;
                     }
                     case SquarePlanar: {
                         IStereoElement se = newSquarePlanar(u, g.neighbors(u), atoms, c);
                         if (se != null) ac.addStereoElement(se);
                         break;
                     }
                     case TrigonalBipyramidal: {
                         IStereoElement se = newTrigonalBipyramidal(u, g.neighbors(u), atoms, c);
                         if (se != null) ac.addStereoElement(se);
                         break;
                     }
                     case Octahedral: {
                         IStereoElement se = newOctahedral(u, g.neighbors(u), atoms, c);
                         if (se != null) ac.addStereoElement(se);
                         break;
                     }
                 }
             }
         }
 
         ac.setBonds(bonds);
 
         // use directional bonds to assign bond-based stereo-specification
         if (checkBondStereo) {
             addDoubleBondStereochemistry(g, ac);
         }
 
         // title suffix
         ac.setTitle(g.getTitle());
 
         return ac;
     }
 
     private Edge findCumulatedEdge(Graph g, int v, Edge e) {
         Edge res = null;
         for (Edge f : g.edges(v)) {
             if (f != e && f.bond() == Bond.DOUBLE) {
                 if (res != null) return null;
                 res = f;
             }
         }
         return res;
     }
 
 
/** Adds double-bonded conformations ({@link DoubleBondStereochemistry}) to the atom container. */

private void addDoubleBondStereochemistry(Graph g, IAtomContainer ac) {
    for (Edge edge : g.edges()) {
        if (edge.bond() == Bond.DOUBLE) {
            int u = edge.either();
            int v = edge.other(u);
            Edge cumulatedEdge = findCumulatedEdge(g, v, edge);
            if (cumulatedEdge != null) {
                int w = cumulatedEdge.other(v);
                IAtom[] atoms = new IAtom[]{ac.getAtom(u), ac.getAtom(v), ac.getAtom(w)};
                Conformation conformation = getConformation(edge, cumulatedEdge);
                DoubleBondStereochemistry stereochemistry = new DoubleBondStereochemistry(atoms, conformation);
                ac.addStereoElement(stereochemistry);
            }
        }
    }
}
}