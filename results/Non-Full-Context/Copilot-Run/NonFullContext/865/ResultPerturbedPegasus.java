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
 
 
/** A new discretizer is created with bin borders that allow for the size of the samples in each bin to be determined. */
 public static FixedBordersDiscretizer create(double[] samples, int size){
    TDoubleArrayList borders = new TDoubleArrayList();
    borders.add(Double.NEGATIVE_INFINITY);
    TDoubleIntHashMap counts = new TDoubleIntHashMap();
    for(double sample : samples){
      counts.adjustOrPutValue(sample, 1, 1);
    }
    double[] sorted = counts.keys();
    Arrays.sort(sorted);
    double[] binBorders = new double[sorted.length];
    for(int i = 0; i < sorted.length; i++){
      binBorders[i] = sorted[i];
    }
    double[] binSizes = new double[binBorders.length - 1];
    for(int i = 0; i < binSizes.length; i++){
      binSizes[i] = (binBorders[i + 1] - binBorders[i]) / size;
    }
    return new FixedBordersDiscretizer(binBorders, binSizes);   
 }

 

}