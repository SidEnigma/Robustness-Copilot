/* *********************************************************************** *
  * project: org.matsim.*
  * FixedSampleSizeDiscretizer.java
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2010 by the members listed in the COPYING,        *
  *                   LICENSE and WARRANTY file.                            *
  * email           : info at matsim dot org                                *
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  *   This program is free software; you can redistribute it and/or modify  *
  *   it under the terms of the GNU General Public License as published by  *
  *   the Free Software Foundation; either version 2 of the License, or     *
  *   (at your option) any later version.                                   *
  *   See also COPYING, LICENSE and WARRANTY file                           *
  *                                                                         *
  * *********************************************************************** */
 package org.matsim.contrib.common.stats;
 
 import gnu.trove.list.array.TDoubleArrayList;
 import gnu.trove.map.hash.TDoubleIntHashMap;
 import org.apache.commons.math.stat.StatUtils;
 
 import java.util.Arrays;
 
 /**
  * A discretizer with fixed bin borders defined such that each bin would contain
  * approximately the same number of samples given at construction.
  * 
  * @author illenberger
  * 
  */
 public class FixedSampleSizeDiscretizer {
 
 
/** Creates a new discretizer with bin borders defined such that each bin  would contain approximately <tt>size</tt> samples from <tt>samples</tt>. */
 public static FixedBordersDiscretizer create(double[] samples, int size){
    TDoubleArrayList borders = new TDoubleArrayList();
    borders.add(Double.NEGATIVE_INFINITY);
    TDoubleIntHashMap counts = new TDoubleIntHashMap();
    for(double sample : samples){
      counts.adjustOrPutValue(sample, 1, 1);
    }
    double[] sorted = counts.keys();
    Arrays.sort(sorted);
    int[] countsSorted = counts.values();
    int[] countsSortedSorted = new int[countsSorted.length];
    System.arraycopy(countsSorted, 0, countsSortedSorted, 0, countsSorted.length);
    Arrays.sort(countsSortedSorted);
    int[] countsSortedSortedSorted = new int[countsSortedSorted.length];
    System.arraycopy(countsSortedSorted, 0, countsSortedSortedSorted, 0, countsSortedSorted.length);
    int[] countsSortedSortedSortedSorted = new int[countsSortedSortedSorted.length];
    System.arraycopy(countsSortedSortedSorted, 0, countsSortedSortedSortedSorted, 0, countsSortedSortedSorted.length);
    int[] countsSortedSortedSortedSortedSorted = new int[countsSortedSortedSortedSorted.length];
    System.arraycopy(countsSortedSortedSortedSorted, 0, countsSortedSortedSortedSortedSorted, 0, countsSortedSortedSortedSorted.length);
    int[] countsSortedSortedSortedSortedSortedSorted = new int[countsSortedSortedSortedSortedSorted.length];
    System.arraycopy(countsSortedSortedSortedSortedSorted, 0, countsSortedSortedSortedSortedSortedSorted, 0, countsSortedSortedSortedSortedSorted.length);
    int[] countsSortedSortedSortedSortedSortedSortedSorted = new int[countsSortedSortedSortedSortedSortedSorted.length];    
 }

 

}