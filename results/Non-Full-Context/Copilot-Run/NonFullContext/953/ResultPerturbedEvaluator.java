/* *********************************************************************** *
  * project: org.matsim.*
  * Dijkstra.java
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
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Set;
 
 import org.apache.log4j.Logger;
 import org.matsim.api.core.v01.Id;
 import org.matsim.api.core.v01.network.Link;
 import org.matsim.api.core.v01.network.Network;
 import org.matsim.api.core.v01.network.Node;
 import org.matsim.api.core.v01.population.Person;
 import org.matsim.core.router.priorityqueue.WrappedBinaryMinHeap;
 import org.matsim.core.router.util.DijkstraNodeData;
 import org.matsim.core.router.util.LeastCostPathCalculator;
 import org.matsim.core.router.util.PreProcessDijkstra;
 import org.matsim.core.router.util.TravelDisutility;
 import org.matsim.core.router.util.TravelTime;
 import org.matsim.core.utils.collections.RouterPriorityQueue;
 import org.matsim.vehicles.Vehicle;
 
 /**
  * Implementation of <a
  * href="http://en.wikipedia.org/wiki/Dijkstra%27s_algorithm">Dijkstra's
  * shortest-path algorithm</a> on a time-dependent network with arbitrary
  * non-negative cost functions (e.g. negative link cost are not allowed). So
  * 'shortest' in our context actually means 'least-cost'.
  *
  * <p>
  * For every router, there exists a class which computes some preprocessing data
  * and is passed to the router class constructor in order to accelerate the
  * routing procedure. The one used for Dijkstra is
  * {@link org.matsim.core.router.util.PreProcessDijkstra}.
  * </p>
  * <br>
  *
  * <h2>Code example:</h2>
  * <p>
  * <code>PreProcessDijkstra preProcessData = new PreProcessDijkstra();<br>
  * preProcessData.run(network);<br>
  * TravelCost costFunction = ...<br>
  * LeastCostPathCalculator routingAlgo = new Dijkstra(network, costFunction, preProcessData);<br>
  * routingAlgo.calcLeastCostPath(fromNode, toNode, startTime);</code>
  * </p>
  * <p>
  * If you don't want to preprocess the network, you can invoke Dijkstra as
  * follows:
  * </p>
  * <p>
  * <code> LeastCostPathCalculator routingAlgo = new Dijkstra(network, costFunction);</code>
  * </p>
  * 
  * <h2>Important note</h2>
  * This class is NOT thread-safe!
  *
  * @see org.matsim.core.router.util.PreProcessDijkstra
  * @see org.matsim.core.router.AStarEuclidean
  * @see org.matsim.core.router.AStarLandmarks
  * @author lnicolas
  * @author mrieser
  */
  public class Dijkstra implements LeastCostPathCalculator {
  	// yyyyyy I don't think that we should make this class publicly inheritable; as we know, will eventually lead
 	// to problems.  kai, feb'18
 
 	private final static Logger log = Logger.getLogger(Dijkstra.class);
 
 	/**
 	 * The network on which we find routes.
 	 */
 	protected Network network;
 
 	/**
 	 * The cost calculator. Provides the cost for each link and time step.
 	 */
 	protected final TravelDisutility costFunction;
 
 	/**
 	 * The travel time calculator. Provides the travel time for each link and time step.
 	 */
 	protected final TravelTime timeFunction;
 
 	final HashMap<Id<Node>, DijkstraNodeData> nodeData;
 
 	/**
 	 * Provides an unique id (loop number) for each routing request, so we don't
 	 * have to reset all nodes at the beginning of each re-routing but can use the
 	 * loop number instead.
 	 */
 	private int iterationID = Integer.MIN_VALUE + 1;
 
 	/**
 	 * Temporary field that is only used if dead ends are being pruned during
 	 * routing and is updated each time a new route has to be calculated.
 	 */
 	private Node deadEndEntryNode;
 
 	/**
 	 * Determines whether we should mark nodes in dead ends during a
 	 * pre-processing step so they won't be expanded during routing.
 	 */
 	/*package*/ final boolean pruneDeadEnds;
 
 
 	private final PreProcessDijkstra preProcessData;
 
 	private RouterPriorityQueue<Node> heap = null;
 
 	private String[] modeRestriction = null;
 	
 	/*package*/ Person person = null;
 	/*package*/ Vehicle vehicle = null;
 
 	/**
 	 * Default constructor.
 	 *
 	 * @param network
 	 *            The network on which to route.
 	 * @param costFunction
 	 *            Determines the link cost defining the cheapest route.
 	 * @param timeFunction
 	 *            Determines the travel time on links.
 	 */
 	// please use DijkstraFactory when you want to create an instance of this
 	protected Dijkstra(final Network network, final TravelDisutility costFunction, final TravelTime timeFunction) {
 		this(network, costFunction, timeFunction, null);
 	}
 
 	/**
 	 * Constructor.
 	 *
 	 * @param network
 	 *            The network on which to route.
 	 * @param costFunction
 	 *            Determines the link cost defining the cheapest route.
 	 * @param timeFunction
 	 *            Determines the travel time on each link.
 	 * @param preProcessData
 	 *            The pre processing data used during the routing phase.
 	 */
 	// please use DijkstraFactory when you want to create an instance of this
 	protected Dijkstra(final Network network, final TravelDisutility costFunction, final TravelTime timeFunction,
 			final PreProcessDijkstra preProcessData) {
 
 		this.network = network;
 		this.costFunction = costFunction;
 		this.timeFunction = timeFunction;
 		this.preProcessData = preProcessData;
 
 		this.nodeData = new HashMap<>((int)(network.getNodes().size() * 1.1), 0.95f);
 
 		if (preProcessData != null) {
 			if (!preProcessData.containsData()) {
 				this.pruneDeadEnds = false;
 				log.warn("The preprocessing data provided to router class Dijkstra contains no data! Please execute its run(...) method first!");
 				log.warn("Running without dead-end pruning.");
 			} else {
 				this.pruneDeadEnds = true;
 			}
 		} else {
 			this.pruneDeadEnds = false;
 		}
 	}
 
 	/**
 	 * @deprecated Use a filtered network instead which only contains the links you want.
 	 */
 	@Deprecated
 	public void setModeRestriction(final Set<String> modeRestriction) {
 		if (modeRestriction == null) {
 			this.modeRestriction = null;
 		} else {
 			this.modeRestriction = modeRestriction.toArray(new String[modeRestriction.size()]);
 		}
 	}
 
 
/** Finds the shortest path between Nodes 'fromNode' and 'toNode' at the specified start time. */
 public Path calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime, final Person person2, final Vehicle vehicle2){
					
 }

 

}