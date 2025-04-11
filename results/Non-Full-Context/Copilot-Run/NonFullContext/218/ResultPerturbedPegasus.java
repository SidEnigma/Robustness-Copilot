/* *********************************************************************** *
  * project: org.matsim.*
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
 
 package org.matsim.core.network.algorithms;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.TreeMap;
 
 import org.apache.log4j.Logger;
 import org.matsim.api.core.v01.Id;
 import org.matsim.api.core.v01.network.Link;
 import org.matsim.api.core.v01.network.Network;
 import org.matsim.api.core.v01.network.Node;
 
 /**
  * Variant of {@link org.matsim.core.network.algorithms.NetworkCleaner NetworkCleaner} that supports
  * multi-modal networks. If the cleaner is run, it will make sure that the sub-network of all those
  * links having at least one of the given transport modes is strongly connected. Other links are not
  * modified. If a link does not belong to the biggest cluster, the to-be-cleaned modes are removed
  * from the set of allowed modes for this link. If a link has no allowed mode anymore, it is removed
  * from the network, along with nodes that lose all their in- and out-links by that way.
  *
  * @author mrieser
  */
 public final class MultimodalNetworkCleaner {
 
 	private final static Logger log = Logger.getLogger(MultimodalNetworkCleaner.class);
 
 	private final Network network;
 	
 	private final Set<Id<Link>> removedLinks = new HashSet<>();
 	private final Set<Id<Link>> modifiedLinks = new HashSet<>();
 
 	public MultimodalNetworkCleaner(final Network network) {
 		this.network = network;
 	}
 
 
/** The network has no incoming or outgoing links for the nodes that are removed. */
 public void removeNodesWithoutLinks(){
   	for (Node node : network.getNodes().values()) {
 		if (node.getInLinks().isEmpty() && node.getOutLinks().isEmpty()) {
 			network.removeNode(node.getId());
 		}
 	}
 }
 
 	/**
 	 * Removes all links that are not part of the biggest cluster.
 	 *
 	 * @param modes The modes to be used for the clustering.
 	 */
 	public void removeLinksNotInBiggestCluster(final Set<String> modes) {
 		final Map<Id<Link>, Set<String>> modesPerLink = new HashMap<>();
 		for (String mode : modes) {
 			for (Link link : network.getLinks().values()) {
 				if (!modesPerLink.containsKey(link.getId())) {
 					modesPerLink.put(link.getId(), new HashSet<String>());
 				}
 				modesPerLink.get(link.getId()).add(mode);
 			}
 		}
 		final Map<Id<Link>, Set<Id<Link>>> cluster = new TreeMap<>();
 		for (Link link : network.getLinks().values()) {
 			if (!cluster.containsKey(link.getId())) {
 				cluster.put(link.getId(), new HashSet<Id<Link>>());
 			}
 			cluster.get(link.getId()).add(link.getId());
 			for (Link l : link.getToNode().getOutLinks().values()) {
 				if (modesPerLink.get(l.getId()).containsAll(modes)) {
 					cluster.get(link.getId()).add(l.getId());
 				}
 			}
 			for (Link   
 }

 

}