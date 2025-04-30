/* *********************************************************************** *
  * project: org.matsim.*
  * NetworkCleaner.java
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
 
 package org.matsim.core.network.algorithms;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.TreeMap;
 
 import org.apache.log4j.Logger;
 import org.matsim.api.core.v01.Id;
 import org.matsim.api.core.v01.network.Link;
 import org.matsim.api.core.v01.network.Network;
 import org.matsim.api.core.v01.network.Node;
 import org.matsim.core.api.internal.NetworkRunnable;
 
 /**
  * Ensures that each link in the network can be reached by any other link.
  * Links that cannot be reached by some other links, or links from which it
  * is not possible to reach all other links, are removed from the network.
  * Nodes with no incoming or outgoing links are removed as well from the
  * network.
  *
  * @author mrieser
  * @author balmermi
  */
 public final class NetworkCleaner implements NetworkRunnable {
 
 	private static final Logger log = Logger.getLogger(NetworkCleaner.class);
 
 
/** Finds the cluster of nodes of which <code>startNode</code> is a part. */
 private Map<Id<Node>, Node> findCluster(final Node startNode, final Network network){
   	Map<Id<Node>, Node> cluster = new HashMap<Id<Node>, Node>();
 	cluster.put(startNode.getId(), startNode);
 	List<Node> todo = new ArrayList<Node>();
 	todo.add(startNode);
 	while (!todo.isEmpty()){
 		Node current = todo.remove(0);
 		for (Link link : current.getOutLinks().values()){
 			Node to = link.getToNode();
 			if (!cluster.containsKey(to.getId())){
 				cluster.put(to.getId(), to);
 				todo.add(to);
 			}
 		}
 		for (Link link : current.getInLinks().values()){
 			Node from = link.getFromNode();
 			if (!cluster.containsKey(from.getId())){
 				cluster.put(from.getId(), from);
 				todo.add(from);
 			}
 		}
 	}
 	return cluster;   
 }

 

}