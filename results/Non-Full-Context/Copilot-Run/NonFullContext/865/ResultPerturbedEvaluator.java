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
 
 
/** Creates a new discretizer with bin borders defined such that each bin would contain approximately the size samples from the given samples */
 public static FixedBordersDiscretizer create(double[] samples, int size){
    TDoubleArrayList borders = new TDoubleArrayList();
    borders.add(Double.NEGATIVE_INFINITY);
    TDoubleIntHashMap counts = new TDoubleIntHashMap();
    for(double sample : samples){
      counts.adjustOrPutValue(sample, 1, 1);
    }
    double[] sortedSamples = counts.keys();
    Arrays.sort(sortedSamples);
    double[] binBorders = new double[sortedSamples.length];
    for(int i = 0; i < sortedSamples.length; i++){
      double binBorder = sortedSamples[i];
      borders.add(binBorder);
      int count = counts.get(binBorder);
      if(count > size){
        binBorders[i] = binBorder;
      }
    }
    borders.add(Double.POSITIVE_INFINITY);
    return new FixedBordersDiscretizer(borders.toArray());    
 }

 

}