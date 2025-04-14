/*
  * Copyright (c) 2015 John May <jwmay@users.sf.net>
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
 
 package org.openscience.cdk.io;
 
 import org.openscience.cdk.CDKConstants;
 import org.openscience.cdk.config.Elements;
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IBond;
 import org.openscience.cdk.interfaces.IChemObject;
 import org.openscience.cdk.interfaces.IPseudoAtom;
 import org.openscience.cdk.interfaces.IStereoElement;
 import org.openscience.cdk.interfaces.ITetrahedralChirality;
 import org.openscience.cdk.interfaces.ITetrahedralChirality.Stereo;
 import org.openscience.cdk.io.formats.IResourceFormat;
 import org.openscience.cdk.io.formats.MDLV3000Format;
 import org.openscience.cdk.io.setting.IOSetting;
 import org.openscience.cdk.io.setting.StringIOSetting;
 import org.openscience.cdk.sgroup.Sgroup;
 import org.openscience.cdk.sgroup.SgroupBracket;
 import org.openscience.cdk.sgroup.SgroupKey;
 import org.openscience.cdk.sgroup.SgroupType;
 
 import javax.vecmath.Point2d;
 import javax.vecmath.Point3d;
 import java.io.BufferedWriter;
 import java.io.Closeable;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.io.Writer;
 import java.nio.charset.StandardCharsets;
 import java.text.DecimalFormat;
 import java.text.DecimalFormatSymbols;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.Set;
 import java.util.TreeMap;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import static org.openscience.cdk.CDKConstants.ATOM_ATOM_MAPPING;
 import static org.openscience.cdk.io.MDLV2000Writer.OptProgramName;
 
 /**
  * Ctab V3000 format output. This writer provides output to the more modern (but less widely
  * supported) V3000 format. Unlikely the V2000 format that is limited to 999 atoms or bonds
  * V3000 can write arbitrarily large molecules. Beyond this the format removes some (but not all)
  * ambiguities and simplifies output values with tagging (e.g 'CHG=-1' instead of '5').
  * 
  * Supported Features:
  * <ul>
  *     <li>Atom Block, non-query features</li>
  *     <li>Bond Block, non-query features</li>
  *     <li>Sgroup Block, partial support for all chemical Sgroups, complete support for: Abbreviations,
  *     MultipleGroup, SRUs, (Un)ordered Mixtures</li>
  * </ul>
  * The 3D block and enhanced stereochemistry is not currently supported.
  */
 public final class MDLV3000Writer extends DefaultChemObjectWriter {
 
     private static final Pattern         R_GRP_NUM = Pattern.compile("R(\\d+)");
     private              V30LineWriter   writer;
     private              StringIOSetting programNameOpt;
 
     /**
      * Create a new V3000 writer, output to the provided JDK writer.
      *
      * @param writer output location
      */
     public MDLV3000Writer(Writer writer) {
         this();
         this.writer = new V30LineWriter(writer);
     }
 
     /**
      * Create a new V3000 writer, output to the provided JDK output stream.
      *
      * @param out output location
      */
     public MDLV3000Writer(OutputStream out) throws CDKException {
         this();
         this.setWriter(out);
     }
 
     /**
      * Default empty constructor.
      */
     public MDLV3000Writer() {
         initIOSettings();
     }
 
     /**
      * Safely access nullable int fields by defaulting to zero.
      *
      * @param x value
      * @return value, or zero if null
      */
     private static int nullAsZero(Integer x) {
         return x == null ? 0 : x;
     }
 
     /**
      * Access the index of Obj->Int map, if the entry isn't found we return -1.
      *
      * @param idxs index map
      * @param obj the object
      * @param <T> the object type
      * @return index or -1 if not found
      */
     private static <T> Integer findIdx(Map<T, Integer> idxs, T obj) {
         Integer idx = idxs.get(obj);
         if (idx == null)
             return -1;
         return idx;
     }
 
     private String getProgName() {
         String progname = programNameOpt.getSetting();
         if (progname == null)
             return "        ";
         else if (progname.length() > 8)
             return progname.substring(0, 8);
         else if (progname.length() < 8)
             return String.format("%-8s", progname);
         else
             return progname;
     }
 
     /**
      * Write the three line header of the MDL format: title, version/timestamp, remark.
      *
      * @param mol molecule being output
      * @throws IOException low-level IO error
      */
     private void writeHeader(IAtomContainer mol) throws IOException {
         final String title = mol.getTitle();
         if (title != null)
             writer.writeDirect(title.substring(0, Math.min(80, title.length())));
         writer.writeDirect('\n');
 
         /*
          * From CTX spec This line has the format:
          * IIPPPPPPPPMMDDYYHHmmddSSssssssssssEEEEEEEEEEEERRRRRR (FORTRAN:
          * A2<--A8--><---A10-->A2I2<--F10.5-><---F12.5--><-I6-> ) User's first
          * and last initials (l), program name (P), date/time (M/D/Y,H:m),
          * dimensional codes (d), scaling factors (S, s), energy (E) if modeling
          * program input, internal registry number (R) if input through MDL
          * form. A blank line can be substituted for line 2.
          */
         writer.writeDirect("  ");
         writer.writeDirect(getProgName());
         writer.writeDirect(new SimpleDateFormat("MMddyyHHmm").format(System.currentTimeMillis()));
         final int dim = getNumberOfDimensions(mol);
         if (dim != 0) {
             writer.writeDirect(Integer.toString(dim));
             writer.writeDirect('D');
         }
         writer.writeDirect('\n');
 
         String comment = mol.getProperty(CDKConstants.REMARK);
         if (comment != null)
             writer.writeDirect(comment.substring(0, Math.min(80, comment.length())));
         writer.writeDirect('\n');
         writer.writeDirect("  0  0  0     0  0            999 V3000\n");
     }
 
     /**
      * Utility function for computing CTfile windings. The return value is adjusted
      * to the MDL's model (look to lowest rank/highest number) from CDK's model (look from
      * first).
      *
      * @param idxs atom/bond index lookup
      * @param stereo the tetrahedral configuration
      * @return winding to write to molfile
      */
     private static Stereo getLocalParity(Map<IChemObject, Integer> idxs, ITetrahedralChirality stereo) {
         IAtom[] neighbours   = stereo.getLigands();
         int[]   neighbourIdx = new int[neighbours.length];
         assert neighbours.length == 4;
         for (int i = 0; i < 4; i++) {
             // impl H is last
             if (stereo.getChiralAtom().equals(neighbours[i])) {
                 neighbourIdx[i] = Integer.MAX_VALUE;
             } else {
                 neighbourIdx[i] = idxs.get(neighbours[i]);
             }
         }
 
         // determine winding swaps
         boolean inverted = false;
         for (int i = 0; i < 4; i++) {
             for (int j = i + 1; j < 4; j++) {
                 if (neighbourIdx[i] > neighbourIdx[j])
                     inverted = !inverted;
             }
         }
 
         // CDK winding is looking from the first atom, MDL is looking
         // towards the last so we invert by default, note inverting twice
         // would be a no op and is omitted
         return inverted ? stereo.getStereo()
                         : stereo.getStereo().invert();
     }
 
     /**
      * Write the atoms of a molecule. We pass in the order of atoms since for compatibility we
      * have shifted all hydrogens to the back.
      *
      * @param mol molecule
      * @param atoms the atoms of a molecule in desired output order
      * @param idxs index lookup
      * @param atomToStereo tetrahedral stereo lookup
      * @throws IOException low-level IO error
      * @throws CDKException inconsistent state etc
      */
     private void writeAtomBlock(IAtomContainer mol, IAtom[] atoms, Map<IChemObject, Integer> idxs,
                                 Map<IAtom, ITetrahedralChirality> atomToStereo) throws IOException, CDKException {
         if (mol.getAtomCount() == 0)
             return;
         final int dim = getNumberOfDimensions(mol);
         writer.write("BEGIN ATOM\n");
         int atomIdx = 0;
         for (IAtom atom : atoms) {
             final int elem = nullAsZero(atom.getAtomicNumber());
             final int chg  = nullAsZero(atom.getFormalCharge());
             final int mass = nullAsZero(atom.getMassNumber());
             final int hcnt = nullAsZero(atom.getImplicitHydrogenCount());
             final int elec = mol.getConnectedSingleElectronsCount(atom);
             int rad  = 0;
             switch (elec) {
                 case 1: // 2
                     rad = MDLV2000Writer.SPIN_MULTIPLICITY.Monovalent.getValue();
                     break;
                 case 2:
                     MDLV2000Writer.SPIN_MULTIPLICITY spinMultiplicity = atom.getProperty(CDKConstants.SPIN_MULTIPLICITY);
                     if (spinMultiplicity != null)
                         rad = spinMultiplicity.getValue();
                     else // 1 or 3? Information loss as to which
                         rad = MDLV2000Writer.SPIN_MULTIPLICITY.DivalentSinglet.getValue();
                     break;
             }
 
             int expVal = 0;
             for (IBond bond : mol.getConnectedBondsList(atom)) {
                 if (bond.getOrder() == null)
                     throw new CDKException("Unsupported bond order: " + bond.getOrder());
                 expVal += bond.getOrder().numeric();
             }
 
             String symbol = getSymbol(atom, elem);
 
             int rnum = -1;
             if (symbol.charAt(0) == 'R') {
                 Matcher matcher = R_GRP_NUM.matcher(symbol);
                 if (matcher.matches()) {
                     symbol = "R#";
                     rnum   = Integer.parseInt(matcher.group(1));
                 }
             }
 
 
 
             writer.write(++atomIdx)
                   .write(' ')
                   .write(symbol)
                   .write(' ');
 
             Point2d p2d = atom.getPoint2d();
             Point3d p3d = atom.getPoint3d();
             switch (dim) {
                 case 0:
                     writer.write("0 0 0 ");
                     break;
                 case 2:
                     if (p2d != null) {
                         writer.write(p2d.x).writeDirect(' ')
                               .write(p2d.y).writeDirect(' ')
                               .write("0 ");
                     } else {
                         writer.write("0 0 0 ");
                     }
                     break;
                 case 3:
                     if (p3d != null) {
                         writer.write(p3d.x).writeDirect(' ')
                               .write(p3d.y).writeDirect(' ')
                               .write(p3d.z).writeDirect(' ');
                     } else {
                         writer.write("0 0 0 ");
                     }
                     break;
             }
             writer.write(nullAsZero(atom.getProperty(ATOM_ATOM_MAPPING, Integer.class)));
 
             if (chg != 0 && chg >= -15 && chg <= 15)
                 writer.write(" CHG=").write(chg);
             if (mass != 0)
                 writer.write(" MASS=").write(mass);
             if (rad > 0 && rad < 4)
                 writer.write(" RAD=").write(rad);
             if (rnum >= 0)
                 writer.write(" RGROUPS=(1 ").write(rnum).write(")");
 
 
             // determine if we need to write the valence
             if (MDLValence.implicitValence(elem, chg, expVal) - expVal != hcnt) {
                 int val = expVal + hcnt;
                 if (val <= 0 || val > 14)
                     val = -1; // -1 is 0
                 writer.write(" VAL=").write(val);
             }
 
             ITetrahedralChirality stereo = atomToStereo.get(atom);
             if (stereo != null) {
                 switch (getLocalParity(idxs, stereo)) {
                     case CLOCKWISE:
                         writer.write(" CFG=1");
                         break;
                     case ANTI_CLOCKWISE:
                         writer.write(" CFG=2");
                         break;
                     default:
                         break;
                 }
             }
 
             writer.write('\n');
         }
         writer.write("END ATOM\n");
     }
 
     /**
      * Access the atom symbol to write.
      *
      * @param atom atom
      * @param elem atomic number
      * @return atom symbol
      */
     private String getSymbol(IAtom atom, int elem) {
         if (atom instanceof IPseudoAtom)
             return ((IPseudoAtom) atom).getLabel();
         String symbol = Elements.ofNumber(elem).symbol();
         if (symbol.isEmpty())
             symbol = atom.getSymbol();
         if (symbol == null)
             symbol = "*";
         if (symbol.length() > 3)
             symbol = symbol.substring(0, 3);
         return symbol;
     }
 
     /**
      * Write the bonds of a molecule.
      *
      * @param mol molecule
      * @param idxs index lookup
      * @throws IOException low-level IO error
      * @throws CDKException inconsistent state etc
      */
     private void writeBondBlock(IAtomContainer mol,
                                 Map<IChemObject, Integer> idxs) throws IOException, CDKException {
         if (mol.getBondCount() == 0)
             return;
 
         // collect multicenter Sgroups before output
         List<Sgroup> sgroups = mol.getProperty(CDKConstants.CTAB_SGROUPS);
         Map<IBond,Sgroup> multicenterSgroups = new HashMap<>();
         if (sgroups != null) {
             for (Sgroup sgroup : sgroups) {
                 if (sgroup.getType() != SgroupType.ExtMulticenter)
                     continue;
                 for (IBond bond : sgroup.getBonds())
                     multicenterSgroups.put(bond, sgroup);
             }
         }
 
         writer.write("BEGIN BOND\n");
         int bondIdx = 0;
         for (IBond bond : mol.bonds()) {
             IAtom beg = bond.getBegin();
             IAtom end = bond.getEnd();
             if (beg == null || end == null)
                 throw new IllegalStateException("Bond " + bondIdx + " had one or more atoms.");
             int begIdx = findIdx(idxs, beg);
             int endIdx = findIdx(idxs, end);
             if (begIdx < 0 || endIdx < 0)
                 throw new IllegalStateException("Bond " + bondIdx + " had atoms not present in the molecule.");
 
             IBond.Stereo stereo = bond.getStereo();
 
             // swap beg/end if needed
             if (stereo == IBond.Stereo.UP_INVERTED ||
                 stereo == IBond.Stereo.DOWN_INVERTED ||
                 stereo == IBond.Stereo.UP_OR_DOWN_INVERTED) {
                 int tmp = begIdx;
                 begIdx = endIdx;
                 endIdx = tmp;
             }
 
             final int order = bond.getOrder() == null ? 0 : bond.getOrder().numeric();
 
             if (order < 1 || order > 3)
                 throw new CDKException("Bond order " + bond.getOrder() + " cannot be written to V3000");
 
             writer.write(++bondIdx)
                   .write(' ')
                   .write(order)
                   .write(' ')
                   .write(begIdx)
                   .write(' ')
                   .write(endIdx);
 
 
             switch (stereo) {
                 case UP:
                 case UP_INVERTED:
                     writer.write(" CFG=1");
                     break;
                 case UP_OR_DOWN:
                 case UP_OR_DOWN_INVERTED:
                     writer.write(" CFG=2");
                     break;
                 case DOWN:
                 case DOWN_INVERTED:
                     writer.write(" CFG=3");
                     break;
                 case NONE:
                     break;
                 default:
                     // warn?
                     break;
             }
 
             Sgroup sgroup = multicenterSgroups.get(bond);
             if (sgroup != null) {
                 List<IAtom> atoms = new ArrayList<>(sgroup.getAtoms());
                 atoms.remove(bond.getBegin());
                 atoms.remove(bond.getEnd());
                 writer.write(" ATTACH=ANY ENDPTS=(").write(atoms, idxs).write(')');
             }
 
             writer.write('\n');
         }
         writer.write("END BOND\n");
     }
 
 
 
/** Since the old CTfile specification is ambiguous in the way in which parity values are written for implicit hydrogens, we actually push all hydrogens atoms at the end of the atom list and assigning them the highest value (4) for the parity values. */

private IAtom[] pushHydrogensToBack(IAtomContainer mol, Map<IChemObject, Integer> atomToIdx) {
    List<IAtom> atoms = new ArrayList<>(mol.atoms());
    List<IAtom> hydrogens = new ArrayList<>();
    
    for (IAtom atom : atoms) {
        if (atom.getSymbol().equals("H")) {
            hydrogens.add(atom);
        }
    }
    
    atoms.removeAll(hydrogens);
    atoms.addAll(hydrogens);
    
    IAtom[] atomArray = new IAtom[atoms.size()];
    atoms.toArray(atomArray);
    
    return atomArray;
}
 

}