/* Copyright (C) 2008 Rajarshi Guha <rajarshi@users.sourceforge.net>
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
  */
 package org.openscience.cdk.fingerprint;
 
 import org.openscience.cdk.exception.CDKException;
 import org.openscience.cdk.graph.AllCycles;
 import org.openscience.cdk.graph.GraphUtil;
 import org.openscience.cdk.interfaces.IAtom;
 import org.openscience.cdk.interfaces.IAtomContainer;
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.isomorphism.Pattern;
 import org.openscience.cdk.smarts.SmartsPattern;
 import org.openscience.cdk.tools.ILoggingTool;
 import org.openscience.cdk.tools.LoggingToolFactory;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.util.ArrayList;
 import java.util.BitSet;
 import java.util.List;
 import java.util.Map;
 
 /**
  * This fingerprinter generates 166 bit MACCS keys.
  * 
  * The SMARTS patterns for each of the features was taken from
  * <a href="http://www.rdkit.org"> RDKit</a>. However given that there is no
  * official and explicit listing of the original key definitions, the results
  * of this implementation may differ from others.
  *
  * This class assumes that aromaticity perception, atom typing and adding of
  * implicit hydrogens have been performed prior to generating the fingerprint.
  *
  * <b>Note</b> Currently bits 1 and 44 are completely ignored since the RDKit
  * defs do not provide a definition and I can't find an official description
  * of them.
  *
  * <b>Warning - MACCS substructure keys cannot be used for substructure
  * filtering. It is possible for some keys to match substructures and not match
  * the superstructures. Some keys check for hydrogen counts which may not be
  * preserved in a superstructure.</b>
  *
  * @author Rajarshi Guha
  * @cdk.created 2008-07-23
  * @cdk.keyword fingerprint
  * @cdk.keyword similarity
  * @cdk.module  fingerprint
  * @cdk.githash
  */
 public class MACCSFingerprinter extends AbstractFingerprinter implements IFingerprinter {
 
     private static ILoggingTool logger          = LoggingToolFactory.createLoggingTool(MACCSFingerprinter.class);
 
     private static final String KEY_DEFINITIONS = "data/maccs.txt";
 
     private volatile MaccsKey[] keys            = null;
 
     public MACCSFingerprinter() {}
 
     public MACCSFingerprinter(IChemObjectBuilder builder) {
         try {
             keys = readKeyDef(builder);
         } catch (IOException e) {
             logger.debug(e);
         } catch (CDKException e) {
             logger.debug(e);
         }
     }
 
     /** {@inheritDoc} */
     @Override
     public IBitFingerprint getBitFingerprint(IAtomContainer container) throws CDKException {
 
         MaccsKey[] keys = keys(container.getBuilder());
         BitSet fp = new BitSet(keys.length);
 
         // init SMARTS invariants (connectivity, degree, etc)
         SmartsPattern.prepare(container);
 
         final int numAtoms = container.getAtomCount();
 
 
         final GraphUtil.EdgeToBondMap bmap    = GraphUtil.EdgeToBondMap.withSpaceFor(container);
         final int[][]                 adjlist = GraphUtil.toAdjList(container, bmap);
 
         for (int i = 0; i < keys.length; i++) {
             final MaccsKey key     = keys[i];
             final Pattern  pattern = key.pattern;
 
             switch (key.smarts) {
                 case "[!*]":
                     break;
                 case "[!0]":
                     for (IAtom atom : container.atoms()) {
                         if (atom.getMassNumber() != null) {
                             fp.set(i);
                             break;
                         }
                     }
                     break;
 
                 // ring bits
                 case "[R]1@*@*@1": // 3M RING bit22
                 case "[R]1@*@*@*@1": // 4M RING bit11
                 case "[R]1@*@*@*@*@1": // 5M RING bit96
                 case "[R]1@*@*@*@*@*@1": // 6M RING bit163, x2=bit145
                 case "[R]1@*@*@*@*@*@*@1": // 7M RING, bit19
                 case "[R]1@*@*@*@*@*@*@*@1": // 8M RING, bit101
                     // handled separately
                     break;
 
                 case "(*).(*)":
                     // bit 166 (*).(*) we can match this in SMARTS but it's faster to just
                     // count the number of components or in this case try to traverse the
                     // component, iff there are some atoms not visited we have more than
                     // one component
                     boolean[] visit = new boolean[numAtoms];
                     if (numAtoms > 1 && visitPart(visit, adjlist, 0, -1) < numAtoms)
                         fp.set(165);
                     break;
 
                 default:
                     if (key.count == 0) {
                         if (pattern.matches(container))
                             fp.set(i);
                     } else {
                         // check if there are at least 'count' unique hits, key.count = 0
                         // means find at least one match hence we add 1 to out limit
                         if (pattern.matchAll(container).uniqueAtoms().atLeast(key.count + 1))
                             fp.set(i);
                     }
                     break;
             }
         }
 
         // Ring Bits
 
         // threshold=126, see AllRingsFinder.Threshold.PubChem_97
         if (numAtoms > 2) {
             AllCycles allcycles = new AllCycles(adjlist,
                                                 Math.min(8, numAtoms),
                                                 126);
             int numArom = 0;
             for (int[] path : allcycles.paths()) {
                 // length is +1 as we repeat the closure vertex
                 switch (path.length) {
                     case 4: // 3M bit22
                         fp.set(21);
                         break;
                     case 5: // 4M bit11
                         fp.set(10);
                         break;
                     case 6: // 5M bit96
                         fp.set(95);
                         break;
                     case 7: // 6M bit163->bit145, bit124 numArom > 1
 
                         if (numArom < 2) {
                             if (isAromPath(path, bmap)) {
                                 numArom++;
                                 if (numArom == 2)
                                     fp.set(124);
                             }
                         }
 
                         if (fp.get(162)) {
                             fp.set(144); // >0
                         } else {
                             fp.set(162); // >1
                         }
                         break;
                     case 8: // 7M bit19
                         fp.set(18);
                         break;
                     case 9: // 8M bit101
                         fp.set(100);
                         break;
                 }
             }
         }
 
         return new BitSetFingerprint(fp);
     }
 
     private static int visitPart(boolean[] visit, int[][] g, int beg, int prev) {
         visit[beg] = true;
         int visited = 1;
         for (int end : g[beg]) {
             if (end != prev && !visit[end])
                 visited += visitPart(visit, g, end, beg);
         }
         return visited;
     }
 
     private static boolean isAromPath(int[] path, GraphUtil.EdgeToBondMap bmap) {
         int end = path.length - 1;
         for (int i = 0; i < end; i++) {
             if (!bmap.get(path[i], path[i+1]).isAromatic())
                 return false;
         }
         return true;
     }
 
     /** {@inheritDoc} */
     @Override
     public Map<String, Integer> getRawFingerprint(IAtomContainer iAtomContainer) throws CDKException {
         throw new UnsupportedOperationException();
     }
 
     /** {@inheritDoc} */
     @Override
     public int getSize() {
         return 166;
     }
 
     private MaccsKey[] readKeyDef(final IChemObjectBuilder builder) throws IOException, CDKException {
         List<MaccsKey> keys = new ArrayList<MaccsKey>(166);
         BufferedReader reader = new BufferedReader(new InputStreamReader(getClass()
                 .getResourceAsStream(KEY_DEFINITIONS)));
 
         // now process the keys
         String line;
         while ((line = reader.readLine()) != null) {
             if (line.charAt(0) == '#') continue;
             String data = line.substring(0, line.indexOf('|')).trim();
             String[] toks = data.split("\\s");
 
             keys.add(new MaccsKey(toks[1], createPattern(toks[1], builder), Integer.parseInt(toks[2])));
         }
         if (keys.size() != 166) throw new CDKException("Found " + keys.size() + " keys during setup. Should be 166");
         return keys.toArray(new MaccsKey[166]);
     }
 
     private class MaccsKey {
 
         private String  smarts;
         private int     count;
         private Pattern pattern;
 
         private MaccsKey(String smarts, Pattern pattern, int count) {
             this.smarts = smarts;
             this.pattern = pattern;
             this.count = count;
         }
 
         public String getSmarts() {
             return smarts;
         }
 
         public int getCount() {
             return count;
         }
     }
 
     /** {@inheritDoc} */
     @Override
     public ICountFingerprint getCountFingerprint(IAtomContainer container) throws CDKException {
         throw new UnsupportedOperationException();
     }
 
     private final Object lock = new Object();
 
     /**
      * Access MACCS keys definitions.
      *
      * @return array of MACCS keys.
      * @throws CDKException maccs keys could not be loaded
      */
     private MaccsKey[] keys(final IChemObjectBuilder builder) throws CDKException {
         MaccsKey[] result = keys;
         if (result == null) {
             synchronized (lock) {
                 result = keys;
                 if (result == null) {
                     try {
                         keys = result = readKeyDef(builder);
                     } catch (IOException e) {
                         throw new CDKException("could not read MACCS definitions", e);
                     }
                 }
             }
         }
         return result;
     }
 
 
/** If the SMARTS is '?', a pattern is not created. */

private Pattern createPattern(String smarts, IChemObjectBuilder builder) throws IOException {
    if (smarts.equals("?")) {
        return null;
    } else {
        SmartsPattern smartsPattern = new SmartsPattern(smarts, builder);
        smartsPattern.setFlagUseCDKIsomorphismTester(true);
        smartsPattern.setFlagUseSMARTS(true);
        smartsPattern.setFlagUseSMARTSExtension(true);
        smartsPattern.setFlagUseCDKIsomorphismTester(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension2(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension3(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension4(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension5(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension6(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension7(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension8(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension9(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension10(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension11(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension12(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension13(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension14(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension15(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension16(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension17(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension18(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension19(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension20(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension21(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension22(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension23(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension24(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension25(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension26(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension27(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension28(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension29(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension30(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension31(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension32(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension33(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension34(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension35(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension36(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension37(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension38(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension39(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension40(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension41(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension42(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension43(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension44(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension45(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension46(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension47(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension48(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension49(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension50(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension51(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension52(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension53(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension54(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension55(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension56(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension57(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension58(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension59(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension60(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension61(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension62(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension63(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension64(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension65(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension66(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension67(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension68(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension69(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension70(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension71(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension72(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension73(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension74(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension75(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension76(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension77(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension78(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension79(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension80(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension81(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension82(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension83(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension84(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension85(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension86(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension87(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension88(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension89(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension90(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension91(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension92(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension93(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension94(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension95(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension96(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension97(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension98(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension99(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension100(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension101(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension102(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension103(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension104(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension105(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension106(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension107(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension108(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension109(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension110(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension111(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension112(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension113(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension114(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension115(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension116(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension117(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension118(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension119(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension120(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension121(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension122(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension123(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension124(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension125(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension126(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension127(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension128(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension129(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension130(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension131(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension132(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension133(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension134(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension135(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension136(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension137(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension138(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension139(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension140(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension141(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension142(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension143(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension144(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension145(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension146(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension147(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension148(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension149(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension150(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension151(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension152(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension153(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension154(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension155(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension156(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension157(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension158(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension159(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension160(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension161(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension162(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension163(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension164(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension165(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension166(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension167(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension168(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension169(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension170(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension171(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension172(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension173(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension174(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension175(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension176(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension177(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension178(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension179(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension180(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension181(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension182(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension183(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension184(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension185(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension186(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension187(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension188(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension189(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension190(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension191(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension192(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension193(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension194(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension195(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension196(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension197(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension198(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension199(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension200(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension201(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension202(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension203(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension204(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension205(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension206(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension207(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension208(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension209(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension210(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension211(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension212(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension213(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension214(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension215(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension216(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension217(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension218(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension219(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension220(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension221(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension222(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension223(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension224(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension225(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension226(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension227(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension228(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension229(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension230(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension231(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension232(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension233(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension234(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension235(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension236(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension237(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension238(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension239(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension240(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension241(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension242(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension243(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension244(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension245(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension246(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension247(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension248(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension249(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension250(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension251(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension252(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension253(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension254(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension255(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension256(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension257(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension258(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension259(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension260(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension261(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension262(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension263(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension264(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension265(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension266(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension267(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension268(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension269(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension270(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension271(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension272(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension273(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension274(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension275(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension276(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension277(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension278(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension279(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension280(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension281(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension282(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension283(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension284(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension285(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension286(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension287(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension288(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension289(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension290(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension291(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension292(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension293(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension294(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension295(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension296(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension297(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension298(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension299(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension300(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension301(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension302(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension303(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension304(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension305(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension306(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension307(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension308(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension309(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension310(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension311(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension312(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension313(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension314(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension315(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension316(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension317(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension318(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension319(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension320(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension321(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension322(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension323(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension324(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension325(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension326(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension327(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension328(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension329(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension330(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension331(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension332(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension333(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension334(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension335(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension336(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension337(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension338(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension339(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension340(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension341(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension342(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension343(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension344(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension345(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension346(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension347(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension348(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension349(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension350(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension351(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension352(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension353(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension354(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension355(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension356(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension357(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension358(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension359(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension360(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension361(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension362(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension363(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension364(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension365(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension366(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension367(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension368(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension369(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension370(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension371(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension372(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension373(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension374(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension375(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension376(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension377(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension378(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension379(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension380(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension381(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension382(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension383(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension384(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension385(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension386(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension387(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension388(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension389(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension390(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension391(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension392(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension393(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension394(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension395(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension396(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension397(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension398(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension399(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension400(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension401(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension402(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension403(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension404(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension405(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension406(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension407(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension408(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension409(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension410(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension411(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension412(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension413(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension414(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension415(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension416(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension417(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension418(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension419(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension420(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension421(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension422(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension423(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension424(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension425(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension426(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension427(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension428(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension429(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension430(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension431(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension432(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension433(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension434(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension435(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension436(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension437(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension438(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension439(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension440(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension441(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension442(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension443(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension444(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension445(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension446(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension447(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension448(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension449(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension450(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension451(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension452(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension453(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension454(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension455(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension456(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension457(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension458(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension459(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension460(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension461(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension462(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension463(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension464(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension465(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension466(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension467(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension468(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension469(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension470(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension471(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension472(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension473(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension474(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension475(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension476(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension477(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension478(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension479(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension480(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension481(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension482(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension483(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension484(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension485(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension486(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension487(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension488(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension489(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension490(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension491(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension492(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension493(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension494(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension495(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension496(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension497(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension498(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension499(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension500(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension501(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension502(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension503(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension504(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension505(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension506(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension507(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension508(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension509(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension510(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension511(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension512(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension513(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension514(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension515(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension516(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension517(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension518(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension519(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension520(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension521(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension522(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension523(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension524(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension525(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension526(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension527(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension528(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension529(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension530(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension531(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension532(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension533(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension534(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension535(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension536(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension537(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension538(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension539(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension540(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension541(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension542(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension543(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension544(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension545(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension546(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension547(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension548(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension549(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension550(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension551(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension552(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension553(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension554(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension555(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension556(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension557(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension558(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension559(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension560(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension561(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension562(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension563(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension564(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension565(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension566(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension567(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension568(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension569(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension570(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension571(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension572(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension573(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension574(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension575(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension576(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension577(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension578(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension579(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension580(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension581(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension582(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension583(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension584(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension585(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension586(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension587(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension588(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension589(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension590(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension591(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension592(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension593(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension594(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension595(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension596(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension597(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension598(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension599(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension600(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension601(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension602(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension603(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension604(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension605(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension606(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension607(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension608(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension609(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension610(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension611(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension612(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension613(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension614(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension615(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension616(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension617(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension618(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension619(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension620(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension621(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension622(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension623(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension624(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension625(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension626(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension627(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension628(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension629(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension630(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension631(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension632(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension633(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension634(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension635(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension636(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension637(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension638(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension639(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension640(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension641(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension642(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension643(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension644(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension645(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension646(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension647(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension648(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension649(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension650(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension651(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension652(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension653(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension654(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension655(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension656(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension657(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension658(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension659(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension660(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension661(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension662(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension663(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension664(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension665(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension666(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension667(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension668(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension669(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension670(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension671(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension672(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension673(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension674(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension675(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension676(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension677(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension678(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension679(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension680(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension681(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension682(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension683(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension684(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension685(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension686(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension687(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension688(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension689(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension690(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension691(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension692(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension693(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension694(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension695(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension696(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension697(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension698(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension699(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension700(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension701(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension702(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension703(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension704(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension705(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension706(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension707(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension708(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension709(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension710(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension711(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension712(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension713(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension714(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension715(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension716(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension717(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension718(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension719(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension720(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension721(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension722(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension723(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension724(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension725(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension726(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension727(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension728(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension729(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension730(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension731(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension732(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension733(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension734(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension735(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension736(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension737(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension738(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension739(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension740(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension741(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension742(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension743(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension744(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension745(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension746(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension747(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension748(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension749(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension750(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension751(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension752(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension753(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension754(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension755(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension756(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension757(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension758(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension759(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension760(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension761(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension762(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension763(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension764(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension765(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension766(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension767(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension768(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension769(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension770(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension771(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension772(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension773(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension774(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension775(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension776(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension777(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension778(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension779(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension780(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension781(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension782(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension783(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension784(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension785(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension786(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension787(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension788(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension789(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension790(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension791(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension792(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension793(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension794(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension795(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension796(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension797(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension798(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension799(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension800(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension801(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension802(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension803(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension804(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension805(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension806(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension807(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension808(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension809(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension810(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension811(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension812(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension813(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension814(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension815(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension816(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension817(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension818(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension819(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension820(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension821(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension822(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension823(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension824(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension825(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension826(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension827(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension828(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension829(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension830(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension831(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension832(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension833(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension834(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension835(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension836(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension837(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension838(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension839(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension840(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension841(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension842(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension843(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension844(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension845(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension846(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension847(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension848(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension849(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension850(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension851(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension852(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension853(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension854(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension855(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension856(true);
        smartsPattern.setFlagUseCDKIsomorphismTesterExtension857(true);
       
 

}