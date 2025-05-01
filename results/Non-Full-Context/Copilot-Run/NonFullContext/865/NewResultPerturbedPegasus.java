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

public static FixedBordersDiscretizer create(double[] samples, int size) {
    // Sort the samples in ascending order
    Arrays.sort(samples);

    // Calculate the number of bins needed
    int numBins = (int) Math.ceil(samples.length / (double) size);

    // Create an array to store the bin borders
    double[] binBorders = new double[numBins + 1];

    // Calculate the bin borders
    for (int i = 0; i < numBins; i++) {
        int startIndex = i * size;
        int endIndex = Math.min(startIndex + size, samples.length);
        double[] binSamples = Arrays.copyOfRange(samples, startIndex, endIndex);
        double binBorder = StatUtils.percentile(binSamples, 50);
        binBorders[i] = binBorder;
    }

    // Set the last bin border to be the maximum value in the samples array
    binBorders[numBins] = samples[samples.length - 1];

    // Create and return a FixedBordersDiscretizer object with the bin borders
    return new FixedBordersDiscretizer(binBorders);
}
 

}