/* *********************************************************************** *
  * project: org.matsim.*
  * Count.java
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
 
 package org.matsim.counts;
 
 import java.util.HashMap;
 
 import org.matsim.api.core.v01.Coord;
 import org.matsim.api.core.v01.Id;
 import org.matsim.api.core.v01.Identifiable;
 
 public class Count<T> implements Identifiable<T> {
 
 	private final Id<T> linkId;
 	private String stationName;
 
 	private final HashMap<Integer,Volume> volumes = new HashMap<>();
 	private Coord coord;
 
 
 	protected Count(final Id<T> linkId2, final String stationName) {
 		this.linkId = linkId2;
 		this.stationName = stationName;
 	}
 
 
/** The function creates a Volume and adds it to the Count station and */
 public final Volume createVolume(final int h, final double val){
   	Volume vol = volumes.get(h);
 	if (vol == null){
 		vol = new Volume(h, val);
 		volumes.put(h, vol);
 	} else {
 		vol.add(val);
 	}
 	return vol;   
 }

 

}