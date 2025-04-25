/**
  *
  * Copyright (C) 2006-2010  Syed Asad Rahman <asad@ebi.ac.uk>
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
 package org.openscience.cdk.smsd.filters;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 import java.util.TreeMap;
 import org.openscience.cdk.smsd.helper.FinalMappings;
 
 /**
  * Class that cleans redundant mappings from the solution set.
  * <OL>
  *
  * <lI>1: Stereo match, bond type, ring etc,
  * <lI>2: Fragment size,
  * <lI>3: Bond breaking energy
  *
  * </OL>
  * @cdk.module smsd
  * @cdk.githash
  * @author Syed Asad Rahman &lt;asad@ebi.ac.uk&gt;
  * @deprecated SMSD has been deprecated from the CDK with a newer, more recent
  *             version of SMSD is available at <a href="http://github.com/asad/smsd">http://github.com/asad/smsd</a>.
  */
 @Deprecated
 public class PostFilter {
 
 
/** Creates an instance of Post Filter and removes the redundant mapping(s). */
 public static List<Map<Integer, Integer>> filter(List<List<Integer>> mappings){
      List<Map<Integer, Integer>> filteredMappings = new ArrayList<Map<Integer, Integer>>();
      for (List<Integer> mapping : mappings) {
          Map<Integer, Integer> filteredMapping = new TreeMap<Integer, Integer>();
          for (Integer key : mapping.keySet()) {
              filteredMapping.put(key, mapping.get(key));
          }
          filteredMappings.add(filteredMapping);
      }
      return filteredMappings;  
 }

 

}