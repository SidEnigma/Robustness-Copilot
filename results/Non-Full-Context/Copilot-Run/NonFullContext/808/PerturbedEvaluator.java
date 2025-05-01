/* *********************************************************************** *
  * project: org.matsim.*
  * AStarEuclidean.java
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
 
 package org.matsim.core.router;
 
 import org.apache.log4j.Logger;
 import org.matsim.api.core.v01.network.Link;
 import org.matsim.api.core.v01.network.Network;
 import org.matsim.api.core.v01.network.Node;
 import org.matsim.core.router.util.*;
 import org.matsim.core.utils.collections.RouterPriorityQueue;
 import org.matsim.core.utils.geometry.CoordUtils;
 
 /**
  * Implements the <a href="http://en.wikipedia.org/wiki/A%2A">A* router algorithm</a>
  * for a given NetworkLayer by using the euclidean distance divided by the
  * maximal free speed per length unit as the heuristic estimate during routing.
  *
  * AStarEuclidean about 3 times faster than Dijkstra.<br />
  *
  * <p>For every router, there exists a class which computes some
  * preprocessing data and is passed to the router class
  * constructor in order to accelerate the routing procedure.
  * The one used for AStarEuclidean is org.matsim.demandmodeling.router.util.PreProcessEuclidean.<br />
  *
  * Network conditions:<br />
  * <ul>
  * <li>The same as for Dijkstra: The link cost must be non-negative,
  * otherwise Dijkstra does not work.</li>
  * <li>The length stored in the links must be greater or equal to the euclidean distance of the
  * link's start and end node, otherwise the algorithm is not guaranteed to deliver least-cost paths.
  * In this case PreProcessEuclidean gives out a warning message.</li>
  * <li>The CostCalculator which calculates the cost for each link must implement the TravelMinCost
  * interface, i.e. it must implement the function getLinkMinimumTravelCost(Link link).
  * The TravelTimeCalculator class does implement it.</li></p>
  * <p><code> PreProcessEuclidean.run() </code>is very fast and needs (almost) no additional
  * memory.<code><br />
  * </code> Code Example:<code><br />
  * TravelMinCost costFunction = ...<br />
  * PreProcessEuclidean preProcessData = new PreProcessEuclidean(costFunction);<br />
  * preProcessData.run(network);<br />...<br />
  * LeastCostPathCalculator routingAlgo = new AStarEuclidean(network, preProcessData);<br />
  * ...</code></p>
  * <p>A note about the so-called overdo factor: You can drastically accelerate the routing of
  * AStarEuclidean by providing an overdo factor &gt; 1 (e.g. 1.5, 2 or 3). In this case,
  * AStarEuclidean does not calculate least-cost paths anymore but tends to deliver distance-minimal
  * paths. The greater the overdo factor, the faster the algorithm but the more the calculated routes
  * diverge from the least-cost ones.<br />
  * A typical invocation then looks like this:<br>
  * <code>LeastCostPathCalculator routingAlgo = new AStarEuclidean(network, preProcessData, 2);</code>
  * <br />
  * @see org.matsim.core.router.util.PreProcessEuclidean
  * @see org.matsim.core.router.Dijkstra
  * @see org.matsim.core.router.AStarLandmarks
  * @author lnicolas
  */
 public class AStarEuclidean extends Dijkstra {
 	private static final Logger log = Logger.getLogger( AStarEuclidean.class ) ;
 
 	protected final double overdoFactor;
 
 	private double minTravelCostPerLength;
 
 	/**
 	 * Default constructor; sets the overdo factor to 1.
 	 * @param network
 	 * @param preProcessData
 	 * @param timeFunction
 	 */
 	AStarEuclidean(final Network network, final PreProcessEuclidean preProcessData,
 			final TravelTime timeFunction) {
 		this(network, preProcessData, timeFunction, 1);
 	}
 
 	/**
 	 * @param network Where we do the routing.
 	 * @param preProcessData The pre-process data (containing the landmarks etc.).
 	 * @param timeFunction Determines the travel time on each link.
 	 * @param overdoFactor The factor which is multiplied with the output of the A* heuristic function. The higher 
 	 * the overdo factor the greedier the router, i.e. it visits less nodes during routing and is thus faster, but 
 	 * for an overdo factor > 1, it is not guaranteed that the router returns the least-cost paths. Rather it tends 
 	 * to return distance-minimal paths.
 	 */
 	AStarEuclidean(final Network network, final PreProcessEuclidean preProcessData,
 			final TravelTime timeFunction, final double overdoFactor) {
 		this(network, preProcessData, preProcessData.getCostFunction(),
 				timeFunction, overdoFactor);
 	}
 
 	/**
 	 * @param network Where we do the routing.
 	 * @param preProcessData The pre-process data (containing the landmarks etc.).
 	 * @param timeFunction Determines the travel time on each link.
 	 * @param costFunction Calculates the travel cost on links.
 	 * @param overdoFactor The factor which is multiplied with the output of the A* heuristic function. The higher 
 	 * the overdo factor the greedier the router, i.e. it visits less nodes during routing and is thus faster, but 
 	 * for an overdo factor > 1, it is not guaranteed that the router returns the least-cost paths. Rather it tends 
 	 * to return distance-minimal paths.
 	 */
 	AStarEuclidean(final Network network,
 			final PreProcessEuclidean preProcessData,
 			final TravelDisutility costFunction, final TravelTime timeFunction, final double overdoFactor) {
 		super(network, costFunction, timeFunction, preProcessData);
 
 		setMinTravelCostPerLength(preProcessData.getMinTravelCostPerLength());
 
 		this.overdoFactor = overdoFactor;
 	}
 
 	/**
 	 * Initializes the first node of a route.
 	 *
 	 * @param fromNode
 	 *            The Node to be initialized.
 	 * @param toNode
 	 *            The Node at which the route should end.
 	 * @param pendingNodes
 	 *            The pending nodes so far.
 	 * @param toNode
 	 *            The Node at which the route should end.
 	 * @param startTime
 	 *            The time we start routing.
 	 */
 	@Override
 	protected void initFromNode(final Node fromNode, final Node toNode, final double startTime, final RouterPriorityQueue<Node> pendingNodes) {
 		AStarNodeData data = getData(fromNode);
 		visitNode(fromNode, data, pendingNodes, startTime, 0, null);
 		data.setExpectedRemainingCost(estimateRemainingTravelCost(fromNode, toNode));
 	}
 
 	@Override
 	protected boolean addToPendingNodes(final Link l, final Node n, final RouterPriorityQueue<Node> pendingNodes,
 			final double currTime, final double currCost, final Node toNode) {
 
 		final double travelTime = this.timeFunction.getLinkTravelTime(l, currTime, this.person, this.vehicle);
 		final double travelCost = this.costFunction.getLinkTravelDisutility(l, currTime, this.person, this.vehicle);		
 		final AStarNodeData data = getData(n);
 		if (!data.isVisited(getIterationId())) {
 			double remainingTravelCost = estimateRemainingTravelCost(n, toNode);
 			visitNode(n, data, pendingNodes, currTime + travelTime, currCost + travelCost, remainingTravelCost, l);
 			return true;
 		}
 		
 		final double nCost = data.getCost();
 		final double totalCost = currCost + travelCost;
 		if (totalCost < nCost) {
 			revisitNode(n, data, pendingNodes, currTime + travelTime, totalCost, l);
 			return true;
 		} else if (totalCost == nCost) {
 			// Special case: a node can be reached from two links with exactly the same costs.
 			// Decide based on the linkId which one to take... just have to common criteria to be deterministic.
 			
 			if ( totalCost==0. ) {
 				log.warn( "finding totalCost=" + totalCost + "; this will often (or always?) lead to a null " +
 								  "pointer exception later.  In my own case, it was related to a network " +
 								  "having freespeed infinity at places.  kai, jan'18") ;
 			}
 			
 			if (data.getPrevLink().getId().compareTo(l.getId()) > 0) {
 				revisitNode(n, data, pendingNodes, currTime + travelTime, totalCost, l);
 				return true;
 			}
 		}
 		return false;
 	}
 
 
/** Add the given Node into the pendingNodes queue and updates its time and cost information */
 private void visitNode(final Node n, final AStarNodeData data, final RouterPriorityQueue<Node> pendingNodes, final double time, final double cost, final double expectedRemainingCost, final Link outLink){}

 

}