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
 
 
/** Creates a new discretizer with tray borders defined so that each tray contains approximate <tt>sample sizes</tt> <tt>from samples</tt>. */

    public static FixedBordersDiscretizer create(double[] samples, int size) {
        // Sort the samples in ascending order
        Arrays.sort(samples);

        // Calculate the number of trays needed
        int numTrays = (int) Math.ceil(samples.length / (double) size);

        // Create a list to store the tray borders
        TDoubleArrayList trayBorders = new TDoubleArrayList(numTrays);

        // Calculate the tray borders
        for (int i = 0; i < numTrays; i++) {
            int startIndex = i * size;
            int endIndex = Math.min(startIndex + size, samples.length);
            double[] traySamples = Arrays.copyOfRange(samples, startIndex, endIndex);
            double trayBorder = StatUtils.percentile(traySamples, 50.0);
            trayBorders.add(trayBorder);
        }

        // Create a map to store the tray index for each sample
        TDoubleIntHashMap sampleTrayMap = new TDoubleIntHashMap(samples.length);

        // Assign each sample to a tray
        for (int i = 0; i < samples.length; i++) {
            double sample = samples[i];
            int trayIndex = Arrays.binarySearch(trayBorders.toArray(), sample);
            if (trayIndex < 0) {
                trayIndex = -(trayIndex + 1);
            }
            sampleTrayMap.put(sample, trayIndex);
        }

        // Create and return a new FixedBordersDiscretizer object
        return new FixedBordersDiscretizer(trayBorders.toArray(), sampleTrayMap);
    }
 

}