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
 	
 
/** Find the 2nd terminus stop (1st terminus is at index 0 per definition). */
 public static final int findSecondTerminusStop(ArrayList<TransitStopFacility> stops){
    
    int secondTerminusStop = 0;
    
    double minDistance = Double.MAX_VALUE;
    
    for(int i = 0; i < stops.size(); i++){
      double distance = CoordUtils.calcDistance(stops.get(i).getCoord(), stops.get(0).getCoord());
      if(distance < minDistance){
        minDistance = distance;
        secondTerminusStop = i;
      }
    }
    
    return secondTerminusStop;      
 }

 

}