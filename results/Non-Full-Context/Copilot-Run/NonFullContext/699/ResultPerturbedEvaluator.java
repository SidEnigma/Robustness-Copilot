/* *********************************************************************** *
  * project: org.matsim.*
  * LanesUtils
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
 package org.matsim.lanes;
 
 import org.matsim.api.core.v01.Id;
 import org.matsim.api.core.v01.Scenario;
 import org.matsim.api.core.v01.network.Link;
 import org.matsim.api.core.v01.network.Network;
 import org.matsim.core.config.Config;
 import org.matsim.core.config.ConfigUtils;
 import org.matsim.core.scenario.ScenarioUtils;
 
 import java.util.*;
 
 /**
  * @author dgrether
  * @author tthunig
  * 
  */
 public final class LanesUtils {
 
 	public static Lanes createLanesContainer(){
 		return new LanesImpl();
 	}
 
 	/**
 	 * Convenience method to create a lane with the given Id, the given length,
 	 * the given capacity, the given number of represented lanes, the given
 	 * alignment and the given Ids of the downstream links or lanes, respectively,
 	 * the lane leads to. The lane is added to the LanesToLinkAssignment given
 	 * as parameter.
 	 * 
 	 * @param l2l
 	 *            the LanesToLinkAssignment to that the created lane is added
 	 * @param factory
 	 *            a LaneDefinitionsFactory to create the lane
 	 * @param laneId
 	 * @param capacity
 	 * @param startsAtMeterFromLinkEnd
 	 * @param alignment
 	 * @param numberOfRepresentedLanes
 	 * @param toLinkIds
 	 * @param toLaneIds
 	 */
 	public static void createAndAddLane(LanesToLinkAssignment l2l,
 			LanesFactory factory, Id<Lane> laneId, double capacity,
 			double startsAtMeterFromLinkEnd, int alignment,
 			int numberOfRepresentedLanes, List<Id<Link>> toLinkIds, List<Id<Lane>> toLaneIds) {
 		
 		Lane lane = factory.createLane(laneId);
 		if (toLinkIds != null){
 			for (Id<Link> toLinkId : toLinkIds) {
 				lane.addToLinkId(toLinkId);
 			}
 		}
 		if (toLaneIds != null){
 			for (Id<Lane> toLaneId : toLaneIds) {
 				lane.addToLaneId(toLaneId);
 			}
 		}
 		lane.setCapacityVehiclesPerHour(capacity);
 		lane.setStartsAtMeterFromLinkEnd(startsAtMeterFromLinkEnd);
 		lane.setNumberOfRepresentedLanes(numberOfRepresentedLanes);
 		lane.setAlignment(alignment); 
 		l2l.addLane(lane);
 	}
 
 	/**
 	 * Replaces the method that converted a lane from format 11 to format 20.
 	 * Use this when you have not defined an original lane of the link and when you have not set lane capacities yet.
 	 */
 	public static void createOriginalLanesAndSetLaneCapacities(Network network, Lanes lanes){
 		LanesFactory factory = lanes.getFactory();
 		for (LanesToLinkAssignment l2l : lanes.getLanesToLinkAssignments().values()){
 			Link link = network.getLinks().get(l2l.getLinkId());
 
 			Lane olLane = factory.createLane(Id.create(l2l.getLinkId().toString() + ".ol", Lane.class));
 			l2l.addLane(olLane);
 			for (Lane lane : l2l.getLanes().values()) {
 				olLane.addToLaneId(lane.getId());
 
 				//set capacity of the lane depending on link capacity and number of representative lanes
 				LanesUtils.calculateAndSetCapacity(lane, true, link, network);
 			}
 			olLane.setNumberOfRepresentedLanes(link.getNumberOfLanes());
 			olLane.setStartsAtMeterFromLinkEnd(link.getLength());
 		}
 	}
 	
 
/** Create a list of lanes sorted for links */
 public static List<ModelLane> createLanes(Link link, LanesToLinkAssignment lanesToLinkAssignment){
	 List<ModelLane> lanes = new ArrayList<ModelLane>();
	 for (Lane lane : lanesToLinkAssignment.getLanes().values()){
		 if (lane.getToLinkIds().contains(link.getId())){
			 lanes.add(new ModelLane(lane));
		 }
	 }
	 return lanes;			
 }

 

}