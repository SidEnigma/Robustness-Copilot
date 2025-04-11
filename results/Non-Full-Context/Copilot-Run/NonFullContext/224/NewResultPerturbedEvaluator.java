/* *********************************************************************** *
  * project: org.matsim.*
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 
 package org.matsim.contrib.minibus.genericUtils;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Map;
 
 import org.matsim.core.utils.geometry.CoordUtils;
 import org.matsim.pt.transitSchedule.api.TransitStopFacility;
 
 public class TerminusStopFinder {
 	
 
/** Find the 2nd terminus (based on the definition, the 1st terminus is at index 0). To take into account stops that are served several times returns the stop index instead of the stop. */

    public static final int findSecondTerminusStop(ArrayList<TransitStopFacility> stops) {
        Map<TransitStopFacility, Integer> stopCount = new HashMap<>();

        // Count the number of times each stop appears
        for (TransitStopFacility stop : stops) {
            stopCount.put(stop, stopCount.getOrDefault(stop, 0) + 1);
        }

        int terminusCount = 0;
        for (int i = 0; i < stops.size(); i++) {
            TransitStopFacility stop = stops.get(i);
            int count = stopCount.get(stop);

            // Check if the stop is a terminus
            if (count == 1) {
                terminusCount++;

                // Check if it is the 2nd terminus
                if (terminusCount == 2) {
                    return i;
                }
            }
        }

        // If no 2nd terminus is found, return -1
        return -1;
    }
 

}