/* *********************************************************************** *
  * project: org.matsim.*
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
 
 package org.matsim.core.network;
 
 import org.apache.log4j.Logger;
 import org.matsim.api.core.v01.Coord;
 import org.matsim.api.core.v01.Id;
 import org.matsim.api.core.v01.IdMap;
 import org.matsim.api.core.v01.network.Link;
 import org.matsim.api.core.v01.network.Network;
 import org.matsim.api.core.v01.network.NetworkFactory;
 import org.matsim.api.core.v01.network.Node;
 import org.matsim.core.scenario.Lockable;
 import org.matsim.core.utils.collections.QuadTree;
 import org.matsim.utils.objectattributes.attributable.Attributes;
 
 import java.util.*;
 
 /**
  * Design thoughts:<ul>
  * <li> This class is final, since it is sitting behind an interface, and thus delegation can be used for 
  * implementation modifications.  Access to the quad tree might be justified in some cases, but should then be realized
  * by specific methods and not via inheritance of the field (I would think).
 
  </ul>
  * 
  * @author nagel
  * @author mrieser
  */
 /*deliberately package*/ final class NetworkImpl implements Network, Lockable, TimeDependentNetwork, SearchableNetwork {
 
 	private final static Logger log = Logger.getLogger(NetworkImpl.class);
 
 	private double capacityPeriod = 3600.0 ;
 
 	private final IdMap<Node, Node> nodes = new IdMap<>(Node.class);
 
 	private final IdMap<Link, Link> links = new IdMap<>(Link.class);
 
 	private QuadTree<Node> nodeQuadTree = null;
 
 	private LinkQuadTree linkQuadTree = null;
 
 	private static final double DEFAULT_EFFECTIVE_CELL_SIZE = 7.5;
 
 	private double effectiveCellSize = DEFAULT_EFFECTIVE_CELL_SIZE;
 
 	private double effectiveLaneWidth = 3.75;
 
 	private NetworkFactory factory;
 
 //	private final Collection<NetworkChangeEvent> networkChangeEvents = new ArrayList<>();
 	
 	private final Queue<NetworkChangeEvent> networkChangeEvents
 //			= new PriorityQueue<>(11, new Comparator<NetworkChangeEvent>() {
 //		@Override
 //		public int compare(NetworkChangeEvent arg0, NetworkChangeEvent arg1) {
 //			return Double.compare(arg0.getStartTime(), arg1.getStartTime()) ;
 //		}
 //	});
 			= new PriorityQueue<>(11, new NetworkChangeEvent.StartTimeComparator() ) ;
 	
 	private String name = null;
 
 	private int counter=0;
 
 	private int nextMsg=1;
 
 	private int counter2=0;
 
 	private int nextMsg2=1;
 
 	private boolean locked = false ;
 	private final Attributes attributes = new Attributes();
 
 	NetworkImpl() {
 		this.factory = new NetworkFactoryImpl(this);
 	}
 
 	@Override
 	public void addLink(final Link link) {
 		Link testLink = links.get(link.getId());
 		if (testLink != null) {
 			if (testLink == link) {
 				log.warn("Trying to add a link a second time to the network. link id = " + link.getId().toString());
 				return;
 			}
 			throw new IllegalArgumentException("There exists already a link with id = " + link.getId().toString() +
 					".\nExisting link: " + testLink + "\nLink to be added: " + link +
 					".\nLink is not added to the network.");
 		}
 
         /* Check if the link's nodes are in the network. */
         Node fromNode = nodes.get(link.getFromNode().getId());
         if (fromNode == null) {
             throw new IllegalArgumentException("Trying to add link = " + link.getId() + ", but its fromNode = " + link.getFromNode().getId() + " has not been added to the network.");
         }
         Node toNode = nodes.get(link.getToNode().getId());
         if (toNode == null) {
             throw new IllegalArgumentException("Trying to add link = " + link.getId() + ", but its toNode = " + link.getToNode().getId() + " has not been added to the network.");
         }
 
         if (!fromNode.getOutLinks().containsKey(link.getId()))
             fromNode.addOutLink(link);
         if (!toNode.getInLinks().containsKey(link.getId()))
             toNode.addInLink(link);
 
         link.setFromNode(fromNode);
         link.setToNode(toNode);
 
         links.put(link.getId(), link);
 
         if (this.linkQuadTree != null) {
             double linkMinX = Math.min(link.getFromNode().getCoord().getX(), link.getToNode().getCoord().getX());
             double linkMaxX = Math.max(link.getFromNode().getCoord().getX(), link.getToNode().getCoord().getX());
             double linkMinY = Math.min(link.getFromNode().getCoord().getY(), link.getToNode().getCoord().getY());
 			double linkMaxY = Math.max(link.getFromNode().getCoord().getY(), link.getToNode().getCoord().getY());
 			if (Double.isInfinite(this.linkQuadTree.getMinEasting())) {
 				// looks like the quad tree was initialized with infinite bounds, see MATSIM-278.
 				this.linkQuadTree = null;
 			} else if (this.linkQuadTree.getMinEasting() <= linkMinX && this.linkQuadTree.getMaxEasting() > linkMaxX
 					&& this.linkQuadTree.getMinNorthing() <= linkMinY && this.linkQuadTree.getMaxNorthing() > linkMaxY) {
 				this.linkQuadTree.put(link);
 			} else {
 				// we add a link outside the current bounds, invalidate it
 				this.linkQuadTree = null;
 			}
 		}
 
 
 		// show counter
 		this.counter++;
 		if (this.counter % this.nextMsg == 0) {
 			this.nextMsg *= 4;
 			printLinksCount();
 		}
 		if ( this.locked && link instanceof Lockable ) {
 			((Lockable)link).setLocked() ;
 		}
 	}
 
 	private void printLinksCount() {
 		log.info(" link # " + this.counter);
 	}
 
 	private void printNodesCount() {
 		log.info(" node # " + this.counter2);
 	}
 
 	@Override
 	public void addNode(final Node nn) {
 		Id<Node> id = nn.getId() ;
 		Node node = this.nodes.get(id);
 		if (node != null) {
 			if (node == nn) {
 				log.warn("Trying to add a node a second time to the network. node id = " + id.toString());
 				return;
 			}
 			throw new IllegalArgumentException("There exists already a node with id = " + id.toString() +
 					".\nExisting node: " + node + "\nNode to be added: " + nn +
 					".\nNode is not added to the network.");
 		}
 		this.nodes.put(id, nn);
 		if (this.nodeQuadTree != null) {
 			if (Double.isInfinite(this.nodeQuadTree.getMinEasting())) {
 				// looks like the quad tree was initialized with infinite bounds, see MATSIM-278.
 				this.nodeQuadTree.clear();
 				this.nodeQuadTree = null;
 			} else if (this.nodeQuadTree.getMinEasting() <= nn.getCoord().getX() && this.nodeQuadTree.getMaxEasting() > nn.getCoord().getX()
 					&& this.nodeQuadTree.getMinNorthing() <= nn.getCoord().getY() && this.nodeQuadTree.getMaxNorthing() > nn.getCoord().getY()) {
 				this.nodeQuadTree.put(nn.getCoord().getX(), nn.getCoord().getY(), nn);
 			} else {
 				// we add a node outside the current bounds, invalidate it
 				this.nodeQuadTree.clear();
 				this.nodeQuadTree = null;
 			}
 		}
 
 		// show counter
 		this.counter2++;
 		if (this.counter2 % this.nextMsg2 == 0) {
 			this.nextMsg2 *= 4;
 			printNodesCount();
 		}
 
 		if ( this.locked && nn instanceof Lockable ) {
 			((Lockable)nn).setLocked() ;
 		}
 	}
 	// ////////////////////////////////////////////////////////////////////
 	// remove methods
 	// ////////////////////////////////////////////////////////////////////
 
 	@Override
 	public Node removeNode(final Id<Node> nodeId) {
 		Node n = this.nodes.remove(nodeId);
 		if (n == null) {
 			return null;
 		}
 		HashSet<Link> links1 = new HashSet<>();
 		links1.addAll(n.getInLinks().values());
 		links1.addAll(n.getOutLinks().values());
 		for (Link l : links1) {
 			removeLink(l.getId());
 		}
 		if (this.nodeQuadTree != null) {
 			this.nodeQuadTree.remove(n.getCoord().getX(),n.getCoord().getY(),n);
 		}
 		return n;
 	}
 
 	@Override
 	public Link removeLink(final Id<Link> linkId) {
 		Link l = this.links.remove(linkId);
 		if (l == null) {
 			return null;
 		}
 		l.getFromNode().removeOutLink(l.getId()) ;
 		l.getToNode().removeInLink(l.getId()) ;
 
 		if (this.linkQuadTree != null) {
 			this.linkQuadTree.remove(l);
 		}
 
 		return l;
 	}
 
 	// ////////////////////////////////////////////////////////////////////
 	// set methods
 	// ////////////////////////////////////////////////////////////////////
 
 	/**
 	 * @param capPeriod the capacity-period in seconds
 	 */
 	@Override
 	public void setCapacityPeriod(final double capPeriod) {
 		testForLocked() ;
 		this.capacityPeriod = (int) capPeriod;
 	}
 	@Override
 	public void setEffectiveCellSize(final double effectiveCellSize) {
 		testForLocked() ;
 		if (this.effectiveCellSize != effectiveCellSize) {
 			if (effectiveCellSize != DEFAULT_EFFECTIVE_CELL_SIZE) {
 				log.warn("Setting effectiveCellSize to a non-default value of " + effectiveCellSize);
 			} else {
 				log.info("Setting effectiveCellSize to " + effectiveCellSize);
 			}
 			this.effectiveCellSize = effectiveCellSize;
 		}
 	}
 	@Override
 	public void setEffectiveLaneWidth(final double effectiveLaneWidth) {
 		testForLocked() ;
 		if (!Double.isNaN(this.effectiveLaneWidth) && this.effectiveLaneWidth != effectiveLaneWidth) {
 			log.warn(this + "[effectiveLaneWidth=" + this.effectiveLaneWidth + " already set. Will be overwritten with " + effectiveLaneWidth + "]");
 		}
 		this.effectiveLaneWidth = effectiveLaneWidth;
 	}
 
 	/**
 	 * Sets the network change events and replaces existing events. Before
 	 * events are applied to their corresponding links, all links are reset to
 	 * their initial state. Pass an empty event list to reset the complete network.
 	 *
 	 * @param events a list of events.
 	 */
 	@Override public void setNetworkChangeEvents(final List<NetworkChangeEvent> events) {
 		this.networkChangeEvents.clear();
 		for(Link link : getLinks().values()) {
 			if (link instanceof TimeVariantLinkImpl) {
 				((TimeVariantLinkImpl)link).clearEvents();
 			}
 			// Presumably, there is no exception here if this fails because it can be interpreted: maybe only some links are time-dependent
 			// and others are not, and it is sufficient if the time-dependent ones can be configured by the addNetworkChangeEvent method.
 			// kai, jul'16
 		}
 		for (NetworkChangeEvent event : events) {
 			this.addNetworkChangeEvent(event);
 		}
 	}
 
 
/** A single network change event is added and applied to the links. */
 public void addNetworkChangeEvent(final NetworkChangeEvent event){
	 	testForLocked() ;
 	this.networkChangeEvents.add(event);
 	for (Link link : this.getLinks().values()) {
 		if (link instanceof TimeVariantLinkImpl) {
 			((TimeVariantLinkImpl)link).addEvent(event);
 		}
 	}
 }
 
 	/**
 	 * Sets the network change events and replaces existing events. Before
 	 * events are applied to their corresponding links, all links are reset to
 	 * their initial state. Pass an empty event list to reset the complete network.
 	 *
 	 * @param events a list of events.
 	 */
 	@Override public void setNetworkChangeEvents(final NetworkChangeEvent events[]) {
 		this.networkChangeEvents.clear();
 		for(Link link : getLinks().values()) {
 			if (link instanceof TimeVariantLinkImpl) {
 				((TimeVariantLinkImpl)link).clearEvents();
 			}
 			// Presumably, there is no exception here if this fails because it can be interpreted: maybe only some links are time-dependent
 			// and others are not, and it is sufficient if the time-dependent ones can be configured by the addNetworkChangeEvent method.
 			// kai, jul'16
 		}
 		for (NetworkChangeEvent event : events) {
 			this.addNetworkChangeEvent(event);
 		}
 	}
 
 	/**
 	 * Adds a network change event to the network.
 	 *
 	 * @param event the event to add
 	 */
 	@Override public void addNetworkChangeEvent(final NetworkChangeEvent event) {
 		testForLocked() ;
 		this.networkChangeEvents.add(event);
 		for (Link link : this.getLinks().values()) {
 			if (link instanceof TimeVariantLinkImpl) {
 				((TimeVariantLinkImpl)link).addEvent(event		
 }

 

}