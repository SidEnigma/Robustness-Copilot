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
 
 	/**
 	 * Removes nodes from the network that have no incoming or outgoing links attached to them.
 	 */
 	public void removeNodesWithoutLinks() {
 		List<Node> toBeRemoved = new ArrayList<>();
 		for (Node node : this.network.getNodes().values()) {
 			if ((node.getInLinks().size() == 0) && (node.getOutLinks().size() == 0)) {
 				toBeRemoved.add(node);
 			}
 		}
 		for (Node node : toBeRemoved) {
 			this.network.removeNode(node.getId());
 		}
 	}
 
 	/**
 	 * Modifies the network such that the subnetwork containing only links that have at least
 	 * one of the specified transport modes in their set of allowed transport modes is strongly
 	 * connected (=every link/node can be reached by every other link/node). If multiple modes
 	 * are given, the algorithm does <em>not</em> guarantee that the resulting network is strongly
 	 * connected for each of the modes individually! Nodes having links connected to them before
 	 * cleaning, but none after cleaning, are removed from the network.
 	 *
 	 * @param modes
 	 */
 	public void run(final Set<String> modes) {
 		run(modes, new HashSet<String>());
 	}
 
 	/**
 	 * Modifies the network such that the subnetwork containing only links that have at least
 	 * one of the specified transport modes (<code>cleaningModes</code> as well as
 	 * <code>connectivityModes</code>) in their set of allowed transport modes is strongly
 	 * connected (=every link/node can be reached by every other link/node). In contrast to
 	 * {@link #run(Set)}, this method will only remove <code>cleaningModes</code> from links,
 	 * but not <code>connectivityModes</code>. Thus, the resulting  network may still contain
 	 * nodes that are sources or sinks for modes of <code>connectivityModes</code>, but not
 	 * for modes of <code>cleaningModes</code> and <code>connectivityModes</code> combined. If
 	 * multiple modes are given as <code>cleaningModes</code>, the algorithm does <em>not</em>
 	 * guarantee that the resulting network is strongly connected for each of the modes
 	 * individually! The subnetwork consisting of links having only modes from
 	 * <code>cleaningModes</code> may not be strongly connected, only in combination with links
 	 * having modes from <code>connectivityModes</code>, a strongly connected subnetwork emerges.
 	 * Nodes having links connected to them before cleaning, but none after cleaning, are removed
 	 * from the network.
 	 *
 	 * @param cleaningModes
 	 * @param connectivityModes
 	 */
 	public void run(final Set<String> cleaningModes, final Set<String> connectivityModes) {
 		final Set<String> combinedModes = new HashSet<>(cleaningModes);
 		combinedModes.addAll(connectivityModes);
 		final Map<Id<Link>, Link> visitedLinks = new TreeMap<>();
 		Map<Id<Link>, Link> biggestCluster = new TreeMap<>();
 
 		log.info("running " + this.getClass().getName() + " algorithm for modes " + Arrays.toString(cleaningModes.toArray())
 				+ " with connectivity modes " + Arrays.toString(connectivityModes.toArray()) + "...");
 
 		// search the biggest cluster of nodes in the network
 		log.info("  checking " + this.network.getNodes().size() + " nodes and " +
 				this.network.getLinks().size() + " links for dead-ends...");
 		boolean stillSearching = true;
 		Iterator<? extends Link> iter = this.network.getLinks().values().iterator();
 		while (iter.hasNext() && stillSearching) {
 			Link startLink = iter.next();
 			if ((!visitedLinks.containsKey(startLink.getId())) && (intersectingSets(combinedModes, startLink.getAllowedModes()))) {
 				Map<Id<Link>, Link> cluster = this.findCluster(startLink, combinedModes);
 				visitedLinks.putAll(cluster);
 				if (cluster.size() > biggestCluster.size()) {
 					biggestCluster = cluster;
 					if (biggestCluster.size() >= (this.network.getLinks().size() - visitedLinks.size())) {
 						// stop searching here, because we cannot find a bigger cluster in the lasting nodes
 						stillSearching = false;
 					}
 				}
 			}
 		}
 		log.info("    The biggest cluster consists of " + biggestCluster.size() + " links.");
 		log.info("  done.");
 
 		/* Remove the modes from all links not being part of the cluster. If a link has no allowed mode
 		 * anymore after this, remove the link from the network.
 		 */
 		List<Link> allLinks = new ArrayList<>(this.network.getLinks().values());
 		for (Link link : allLinks) {
 			if (!biggestCluster.containsKey(link.getId())) {
 				Set<String> reducedModes = new HashSet<>(link.getAllowedModes());
 				reducedModes.removeAll(cleaningModes);
 				link.setAllowedModes(reducedModes);
 				if (reducedModes.isEmpty()) {
 					this.network.removeLink(link.getId());
 					if ((link.getFromNode().getInLinks().size() + link.getFromNode().getOutLinks().size()) == 0) {
 						this.network.removeNode(link.getFromNode().getId());
 					}
 					if ((link.getToNode().getInLinks().size() + link.getToNode().getOutLinks().size()) == 0) {
 						this.network.removeNode(link.getToNode().getId());
 					}
 					this.removedLinks.add(link.getId());
 				}
 				if(!removedLinks.contains(link.getId())) modifiedLinks.add(link.getId());
 			}
 		}
 		log.info("  resulting network contains " + this.network.getNodes().size() + " nodes and " +
 				this.network.getLinks().size() + " links.");
 		log.info("done.");
 	}
 
 
/** Finds the link cluster that <pre>startLink</pre> is a part of. */
 private Map<Id<Link>, Link> findCluster(final Link startLink, final Set<String> modes){
	 		final Map<Id<Link>, Link> visitedLinks = new TreeMap<>();
 		final Map<Id<Link>, Link> cluster = new TreeMap<>();
 		final Queue<Link> queue = new LinkedList<>();
 		queue.add(startLink);
 		visitedLinks.put(startLink.getId(), startLink);
 		while (!queue.isEmpty()) {
 			Link currentLink = queue.remove();
 			if (intersectingSets(modes, currentLink.getAllowedModes())) {
 				cluster.put(currentLink.getId(), currentLink);
 				for (Link outLink : currentLink.getToNode().getOutLinks().values()) {
 					if (!visitedLinks.containsKey(outLink.getId())) {
 						visitedLinks.put(outLink.getId(), outLink);
 						queue.add(outLink);
 					}
 				}
 				for (Link inLink : currentLink.getFromNode().getInLinks().values()) {
 					if (!visitedLinks.containsKey(inLink.getId())) {
 						visitedLinks.put(inLink.getId(), inLink);
 						queue.add(inLink);
 					}
 				}
 			}
 		}
 		return cluster;
 	}
 
 	/**
 	 * Checks if the sets <code>set1</code> and <code>set2</code> have at least one element in
 	 * common.
 	 *
 	 * @param set1
 	 * @param set2
 	 * @return
 	 */
 	private boolean intersectingSets(final Set<String> set1, final Set<String> set2) {
 		for (		
 }

 

}