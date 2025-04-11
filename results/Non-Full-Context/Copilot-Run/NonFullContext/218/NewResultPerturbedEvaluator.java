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
 
 
/** Nodes that have no incoming or outgoing connections are removed from the network. */

public void removeNodesWithoutLinks() {
    // Create a set to store the nodes that need to be removed
    Set<Id<Node>> nodesToRemove = new HashSet<>();

    // Iterate over all nodes in the network
    for (Node node : network.getNodes().values()) {
        // Check if the node has any incoming or outgoing links
        if (node.getInLinks().isEmpty() && node.getOutLinks().isEmpty()) {
            // Add the node to the set of nodes to be removed
            nodesToRemove.add(node.getId());
        }
    }

    // Iterate over the set of nodes to be removed
    for (Id<Node> nodeId : nodesToRemove) {
        // Get the node from the network
        Node node = network.getNodes().get(nodeId);

        // Remove the node from the network
        network.removeNode(node);

        // Log the removal of the node
        log.info("Removed node: " + nodeId);
    }
}
 

}