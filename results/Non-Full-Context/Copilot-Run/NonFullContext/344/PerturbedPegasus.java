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
 
 package org.openscience.cdk.forcefield.mmff;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.math.BigDecimal;
 import java.nio.charset.StandardCharsets;
 import java.util.HashMap;
 import java.util.Map;
 
 /**
  * Internal class for accessing MMFF parameters.
  * 
  * @author John May
  */
 enum MmffParamSet {
     
     INSTANCE;
 
     private static final int MAX_MMFF_ATOMTYPE = 99;
 
    
     /**
      * Bond charge increments.
      */
     private Map<BondKey, BigDecimal> bcis = new HashMap<>();
 
     /**
      * Atom type properties.
      */
     private MmffProp[] properties = new MmffProp[MAX_MMFF_ATOMTYPE + 1];
 
     private Map<String, Integer> typeMap = new HashMap<>();
 
     /**
      * Symbolic formal charges - some are varible and assigned in code.
      */
     private Map<String, BigDecimal> fCharges = new HashMap<>();
 
     MmffParamSet() {
         try (InputStream in = getClass().getResourceAsStream("MMFFCHG.PAR")) {
             parseMMFFCHARGE(in, bcis);
         } catch (IOException e) {
             throw new InternalError("Could not load MMFFCHG.PAR");
         }
         try (InputStream in = getClass().getResourceAsStream("MMFFFORMCHG.PAR")) {
             parseMMFFFORMCHG(in, fCharges);
         } catch (IOException e) {
             throw new InternalError("Could not load MMFFFORMCHG.PAR");
         }
         try (InputStream in = getClass().getResourceAsStream("MMFFPROP.PAR")) {
             parseMMFFPPROP(in, properties);
         } catch (IOException e) {
             throw new InternalError("Could not load MMFFPROP.PAR");
         }
         try (InputStream in = getClass().getResourceAsStream("MMFFPBCI.PAR")) {
             parseMMFFPBCI(in, properties);
         } catch (IOException e) {
             throw new InternalError("Could not load MMFFPBCI.PAR");
         }
         try (InputStream in = getClass().getResourceAsStream("mmff-symb-mapping.tsv")) {
             parseMMFFTypeMap(in, typeMap);
         } catch (IOException e) {
             throw new InternalError("Could not load mmff-symb-mapping.tsv");
         }
     }
 
 
/** Obtain the MMFF atom type for a symbolic MMFF type. */
 int intType(final String sym){}

 

}