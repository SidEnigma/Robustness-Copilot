/* Copyright (C) 2010  Rajarshi Guha <rajarshi.guha@gmail.com>
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public License
  * as published by the Free Software Foundation; either version 2.1
  * of the License, or (at your option) any later version.
  * All I ask is that proper credit is given for my work, which includes
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
 package org.openscience.cdk.similarity;
 
 
 import java.util.Map;
 import java.util.TreeSet;
 
 /**
  * A class to evaluate the similarity between two LINGO's as described in {@cdk.cite Vidal2005}.
  *
  * The similarity calculation is a variant of the Tanimoto coefficient and hence its
  * value ranges from 0 to 1
  *
  * @author Rajarshi Guha
  * @cdk.githash
  * @cdk.keyword lingo
  * @cdk.keyword similarity, tanimoto
  * @cdk.module fingerprint
  */
 public class LingoSimilarity {
 
     private LingoSimilarity() {}
 
 
/** Compute the LINGO similarity between the (key,value) and the (style, fingerprint) The similiary will range from 0.0 to 1.0 */

public static float calculate(Map<String, Integer> features1, Map<String, Integer> features2) {
    // Calculate the intersection of keys between features1 and features2
    TreeSet<String> intersection = new TreeSet<>(features1.keySet());
    intersection.retainAll(features2.keySet());

    // Calculate the sum of the squared differences between the values of intersecting keys
    int sumOfSquaredDifferences = 0;
    for (String key : intersection) {
        int value1 = features1.get(key);
        int value2 = features2.get(key);
        int difference = value1 - value2;
        sumOfSquaredDifferences += difference * difference;
    }

    // Calculate the LINGO similarity
    float similarity = 1.0f - (float) sumOfSquaredDifferences / (float) (features1.size() + features2.size());

    return similarity;
}
}
