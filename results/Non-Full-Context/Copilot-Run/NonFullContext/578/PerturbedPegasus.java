/* *********************************************************************** *
  * project: org.matsim.*
  * DJCluster.java
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
 
 package org.matsim.core.network.algorithms.intersectionSimplifier;
 
 import java.io.BufferedWriter;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.TreeMap;
 
 import org.apache.log4j.Logger;
 import org.matsim.api.core.v01.Coord;
 import org.matsim.api.core.v01.Id;
 import org.matsim.api.core.v01.network.Node;
 import org.matsim.core.network.algorithms.intersectionSimplifier.containers.ClusterActivity;
 import org.matsim.core.network.algorithms.intersectionSimplifier.containers.Cluster;
 import org.matsim.core.utils.collections.QuadTree;
 import org.matsim.core.utils.io.IOUtils;
 
 
 /**
  * <par>This class implements the density-based clustering approach as published
  * by Zhou <i>et al</i> (2004).</par>
  * <ul>
  * 		<i>``The basic idea of DJ-DigicoreCluster is as follows. For each point, calculate its 
  * 		<b>neighborhood</b>: the neighborhood consists of points within distance 
  * 		<b>Eps</b>, under condition that there are at least <b>MinPts</b> of them. If no
  * 		such neighborhood is found, the point is labeled noise; otherwise, the points are
  * 		created as a new DigicoreCluster if no neighbor is in an existing DigicoreCluster, or joined with
  * 		an existing DigicoreCluster if any neighbour is in an existing DigicoreCluster.''</i>
  * </ul>
  * <h4>Reference</h4>
  * Zhou, C., Frankowski, D., Ludford, P.m Shekar, S. and Terveen, L. (2004). Discovering 
  * personal gazeteers: An interactive clustering approach. <i> Proceedings of the 12th annual 
  * ACM International workshop on Geographic Information Systems</i>, p. 266-273. Washington, DC.
  * <h4></h4>
  * @author jwjoubert
  */
 public class DensityCluster {
 	private List<Node> inputPoints;
 	private Map<Id<Coord>, ClusterActivity> lostPoints = new TreeMap<Id<Coord>, ClusterActivity>();
 	private QuadTree<ClusterActivity> quadTree;
 	private List<Cluster> clusterList;
 	private final static Logger log = Logger.getLogger(DensityCluster.class);
 	private String delimiter = ",";
 	private final boolean silent;
 
 	/**
 	 * Creates a new instance of the density-based cluster with an empty list 
 	 * of clusters.
 	 * @param radius the radius of the search circle within which other activity points
 	 * 			are searched.
 	 * @param minimumPoints the minimum number of points considered to constitute an
 	 * 			independent {@link Cluster}.
 	 * @param pointsToCluster 
 	 */
 	public DensityCluster(List<Node> nodesToCluster, boolean silent){
 		this.inputPoints = nodesToCluster;
 		
 		/*TODO Remove later. */
 		int nullCounter = 0;
 		for(Node node : inputPoints){
 			if(node == null || node.getCoord() == null){
 				nullCounter++;
 			}
 		}
 		if(nullCounter > 0){
 			log.warn("In DJCluster: of the " + inputPoints.size() + " points, " + nullCounter + " were null.");
 		}
 		
 		this.clusterList = new ArrayList<Cluster>();
 		this.silent = silent;
 	}
 
 	
 
/** A code ofDigicoreCluster is built with code>ArrayList/code>. */
 public void clusterInput(double radius, int minimumPoints){}

 

}