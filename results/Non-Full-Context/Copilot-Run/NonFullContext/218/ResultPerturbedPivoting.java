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
 
 
/** Removes nodes from the network to which no inbound or outbound links are attached. */
 public void removeNodesWithoutLinks(){
  		Set<Id<Node>> nodesToRemove = new HashSet<>();
  		for (Node node : network.getNodes().values()) {
  			if (node.getInLinks().isEmpty() && node.getOutLinks().isEmpty()) {
  				nodesToRemove.add(node.getId());
  			}
  		}
  		for (Id<Node> nodeId : nodesToRemove) {
  			network.removeNode(nodeId);
  		}
  	}
  
  	/**
  	 * Removes links from the network that are not part of the biggest cluster.
  	 *
  	 * @param modes The modes to be used for the clustering.
  	 */
  	public void removeLinksNotInBiggestCluster(final Set<String> modes) {
  		Set<Id<Link>> linksToRemove = new HashSet<>();
  		Map<Id<Link>, Set<String>> linkModes = new HashMap<>();
  		for (Link link : network.getLinks().values()) {
  			linkModes.put(link.getId(), new HashSet<String>(modes));
  		}
  		Map<Id<Link>, Set<Id<Link>>> clusters = getClusters(linkModes);
  		Map<Id<Link>, Set<Id<Link>>> biggestCluster = getBiggestCluster(clusters);
  		for (Id<Link> linkId : linkModes.keySet()) {
  			if (!biggestCluster.get(linkId).equals(linkModes.get(linkId))) {
  				linksToRemove.add(linkId);
  			}
  		}
  		for (Id<Link> linkId : linksToRemove) {
  			network.removeLink(linkId);
  		}
  	}
  
  	/**
  	 * Removes links from the network that are not part of the biggest cluster.
  	 *    
 }

 

}