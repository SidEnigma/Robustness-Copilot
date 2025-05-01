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

    public static FixedBordersDiscretizer create(double[] samples, int size) {
        // Sort the samples in ascending order
        Arrays.sort(samples);

        // Calculate the number of bins needed
        int numBins = (int) Math.ceil(samples.length / (double) size);

        // Create a list to store the bin borders
        TDoubleArrayList binBorders = new TDoubleArrayList(numBins + 1);

        // Calculate the bin borders
        for (int i = 0; i < numBins; i++) {
            int startIndex = i * size;
            int endIndex = Math.min((i + 1) * size, samples.length);
            double[] binSamples = Arrays.copyOfRange(samples, startIndex, endIndex);
            double binBorder = StatUtils.percentile(binSamples, 50.0);
            binBorders.add(binBorder);
        }

        // Create a map to store the bin index for each sample
        TDoubleIntHashMap binIndexMap = new TDoubleIntHashMap();

        // Assign each sample to a bin
        for (int i = 0; i < samples.length; i++) {
            double sample = samples[i];
            int binIndex = Arrays.binarySearch(binBorders.toArray(), sample);
            if (binIndex < 0) {
                binIndex = -binIndex - 1;
            }
            binIndexMap.put(sample, binIndex);
        }

        // Create and return a new FixedBordersDiscretizer instance
        return new FixedBordersDiscretizer(binBorders.toArray(), binIndexMap);
    }
 

}