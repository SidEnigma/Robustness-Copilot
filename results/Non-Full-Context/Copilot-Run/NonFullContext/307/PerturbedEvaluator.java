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
 
 package playground.vsp.andreas.osmBB;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Set;
 import java.util.TreeSet;
 
 import org.apache.log4j.Logger;
 import org.matsim.api.core.v01.Id;
 import org.matsim.api.core.v01.Scenario;
 import org.matsim.api.core.v01.TransportMode;
 import org.matsim.api.core.v01.network.Link;
 import org.matsim.api.core.v01.network.Network;
 import org.matsim.api.core.v01.network.Node;
 import org.matsim.core.config.Config;
 import org.matsim.core.config.ConfigUtils;
 import org.matsim.core.network.NetworkUtils;
 import org.matsim.core.network.algorithms.NetworkCalcTopoType;
 import org.matsim.core.network.io.MatsimNetworkReader;
 import org.matsim.core.network.io.NetworkWriter;
 import org.matsim.core.population.routes.LinkNetworkRouteFactory;
 import org.matsim.core.population.routes.NetworkRoute;
 import org.matsim.core.scenario.MutableScenario;
 import org.matsim.core.scenario.ScenarioUtils;
 import org.matsim.counts.Count;
 import org.matsim.counts.Counts;
 import org.matsim.counts.CountsReaderMatsimV1;
 import org.matsim.counts.CountsWriter;
 import org.matsim.counts.Volume;
 import org.matsim.pt.transitSchedule.api.TransitLine;
 import org.matsim.pt.transitSchedule.api.TransitRoute;
 import org.matsim.pt.transitSchedule.api.TransitSchedule;
 import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
 import org.matsim.pt.transitSchedule.api.TransitStopFacility;
 import org.matsim.run.NetworkCleaner;
 
 import playground.vsp.andreas.utils.net.NetworkRemoveUnusedNodes;
 import playground.vsp.andreas.utils.pt.TransitScheduleCleaner;
 
 /**
  * Simplifies a given network, by merging links.
  *
  * @author aneumann
  *
  */
 public class PTCountsNetworkSimplifier {
 
 	private static final Logger log = Logger.getLogger(PTCountsNetworkSimplifier.class);
 	private boolean mergeLinkStats = false;
 	private TransitSchedule transitSchedule;
 	private Network network;
 
 	private String netInFile;
 	private String scheduleInFile;
 	private String vehiclesInFile;
 	private String netOutFile;
 	private String scheduleOutFile;
 	private Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
 	private HashMap<String, String> shortNameMap;
 	private Counts inCounts;
 	private Counts outCounts = new Counts();
 	private String countsOutFile;
 	
 	private boolean usePT;
 
 	private TreeSet<String> linksBlockedByFacility = new TreeSet<String>();
 	private Set<String> additionalLinksBlockedBySomething;
 
 	public PTCountsNetworkSimplifier(String netInFile, String scheduleInFile, String netOutFile, String scheduleOutFile, HashMap<String,String> shortNameMap, String inCounts, String countsOutFile, String vehiclesInFile, Set<String> linksBlocked){
 		this.netInFile = netInFile;
 		
 		if(scheduleInFile == null){
 			this.usePT = false;
 		} else {
 			this.usePT = true;
 		}
 		this.scheduleInFile = scheduleInFile;
 		this.netOutFile = netOutFile;
 		this.scheduleOutFile = scheduleOutFile;
 		this.shortNameMap = shortNameMap;
 		
 		if(inCounts == null){
 			this.inCounts = null;
 		} else {
 			this.inCounts = new Counts();
 			CountsReaderMatsimV1 countsReader = new CountsReaderMatsimV1(this.inCounts);
 			countsReader.readFile(inCounts);
 		}
 		
 		this.countsOutFile = countsOutFile;
 		this.vehiclesInFile = vehiclesInFile;
 
 		// set some nonsense, cause writer allows for empty fields, but reader complains
 		this.outCounts.setYear(2009);
 		this.outCounts.setName("hab ich nicht");
 		
 		this.additionalLinksBlockedBySomething = linksBlocked;
 	}
 
 	public static void main(String[] args) {
 		PTCountsNetworkSimplifier simplifier = new PTCountsNetworkSimplifier("f:/simplifyTest/net.xml", "f:/simplifyTest/transitSchedule.xml", "f:/simplifyTest/_out/net_simplified_merged.xml", "f:/simplifyTest/_out/transitSchedule_subway_merged.xml", null, null, null, "f:/simplifyTest/vehicles.xml", new TreeSet<String>());
 		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
 		nodeTypesToMerge.add(new Integer(4));
 		nodeTypesToMerge.add(new Integer(5));
 		simplifier.setNodesToMerge(nodeTypesToMerge);
 		simplifier.setMergeLinkStats(false);
 		simplifier.simplifyPTNetwork();
 	}
 
 	public void simplifyPTNetwork(){
 
 		log.info("Start...");
 		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
 		this.network = scenario.getNetwork();
 		log.info("Reading " + this.netInFile);
 		new MatsimNetworkReader(scenario.getNetwork()).readFile(this.netInFile);
 
 		MutableScenario osmScenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
 		Config osmConfig = osmScenario.getConfig();
 		if(this.usePT){
 			osmConfig.transit().setUseTransit(true);
 			osmConfig.transit().setTransitScheduleFile(this.scheduleInFile);
 			osmConfig.transit().setVehiclesFile(this.vehiclesInFile);
 		}
 		osmConfig.network().setInputFile(this.netInFile);
 		ScenarioUtils.loadScenario(osmScenario);
 
 		if(this.usePT){
 			log.info("Running cleaner first...");
 			TransitScheduleCleaner.removeAllRoutesWithMissingLinksFromSchedule(osmScenario.getTransitSchedule(), this.network);
 			TransitScheduleCleaner.removeEmptyLines(osmScenario.getTransitSchedule());
 			TransitScheduleCleaner.removeStopsNotUsed(osmScenario.getTransitSchedule());
 			this.network = TransitScheduleCleaner.removeAllPtTagsFromNetwork(this.network);
 			this.network = TransitScheduleCleaner.tagTransitLinksInNetwork(osmScenario.getTransitSchedule(), this.network);
 		}
 		
 		log.info("Running simplifier...");
 		run(this.network, osmScenario.getTransitSchedule());
 		log.info("Running cleaner for the second time...");
 		
 		if(this.usePT){
 			TransitScheduleCleaner.removeAllRoutesWithMissingLinksFromSchedule(osmScenario.getTransitSchedule(), this.network);
 			TransitScheduleCleaner.removeEmptyLines(osmScenario.getTransitSchedule());
 			TransitScheduleCleaner.removeStopsNotUsed(osmScenario.getTransitSchedule());
 		}
 		
 		log.info("Writing network to " + this.netOutFile);
 		new NetworkWriter(this.network).write(this.netOutFile + "_not_cleaned.xml.gz");
 		if(this.usePT){
 			log.info("Writing transit schedule to " + this.scheduleOutFile);
 			new TransitScheduleWriter(osmScenario.getTransitSchedule()).writeFile(this.scheduleOutFile);
 		}
 		if(this.inCounts != null){
 			log.info("Writing counts file to " + this.countsOutFile);
 			new CountsWriter(this.outCounts).write(this.countsOutFile);
 		}
 		log.info("Running network cleaner... Result may not be consistent with countsfile");
 		scenario = null; this.network = null; osmScenario = null; osmConfig = null;
 		new NetworkCleaner().run(this.netOutFile + "_not_cleaned.xml.gz", this.netOutFile);
 
 	}
 
 	public void run(final Network net, final TransitSchedule tranSched) {
 
 		this.network = net;
 		this.transitSchedule = tranSched;
 		int nodesProcessed = 0;
 		int nextMessageAt = 2;
 
 		if(this.nodeTypesToMerge.size() == 0){
 			throw new RuntimeException("No types of node specified. Please use setNodesToMerge to specify which nodes should be merged");
 		}
 
 		log.info("running " + this.getClass().getName() + " algorithm...");
 		log.info("  checking " + this.network.getNodes().size() + " nodes and " + this.network.getLinks().size() + " links for dead-ends...");
 
 		NetworkCalcTopoType nodeTopo = new NetworkCalcTopoType();
 		nodeTopo.run(this.network);
 		nodeTopo = new NetworkCalcTopoType(); // save memory
 		
 		if(this.usePT){
 			registerLinksBlockedByTransitStopFacility();
 		}
 		
 		if(this.inCounts != null){
 			registerLinksBlockedByCountStation();
 		}
 
 		if(this.additionalLinksBlockedBySomething != null){
 			registerLinksBlockedByAnythingElse();
 		}
 
 		for (Node node : this.network.getNodes().values()) {
 			nodesProcessed++;
 			if(nextMessageAt == nodesProcessed){
 				log.info(nodesProcessed + " nodes processed so far");
 				nextMessageAt = 2 * nodesProcessed;
 			}
 
 			if(this.nodeTypesToMerge.contains(Integer.valueOf(NetworkCalcTopoTypeAN.run(node)))){
 
 				List<Link> iLinks = new ArrayList<Link> (node.getInLinks().values());
 
 				for (Link iL : iLinks) {
 					Link inLink = (Link) iL;
 
 					List<Link> oLinks = new ArrayList<Link> (node.getOutLinks().values());
 
 					for (Link oL : oLinks) {
 						Link outLink = (Link) oL;
 
 						if(inLink != null && outLink != null){
 
 							if(!outLink.getToNode().equals(inLink.getFromNode())){
 
 								if(!this.linksBlockedByFacility.contains(inLink.getId().toString()) && !this.linksBlockedByFacility.contains(outLink.getId().toString())){
 
 									Link link = null;
 
 									if(this.mergeLinkStats){
 
 										// Try to merge both links by guessing the resulting links attributes
 										link = this.network.getFactory().createLink(
 												Id.create(inLink.getId() + "-" + outLink.getId(), Link.class),
 												inLink.getFromNode(),
 												outLink.getToNode());
 
 										// length can be summed up
 										link.setLength(inLink.getLength() + outLink.getLength());
 
 										// freespeed depends on total length and time needed for inLink and outLink
 										link.setFreespeed(
 												(inLink.getLength() + outLink.getLength()) /
 												(NetworkUtils.getFreespeedTravelTime(inLink) + NetworkUtils.getFreespeedTravelTime(outLink))
 										);
 
 										// the capacity and the new links end is important, thus it will be set to the minimum
 										link.setCapacity(Math.min(inLink.getCapacity(), outLink.getCapacity()));
 
 										// number of lanes can be derived from the storage capacity of both links
 										link.setNumberOfLanes((inLink.getLength() * inLink.getNumberOfLanes()
 												+ outLink.getLength() * outLink.getNumberOfLanes())
 												/ (inLink.getLength() + outLink.getLength())
 										);
 
 										// Take all of them
 										Set<String> aM = inLink.getAllowedModes();
 										aM.addAll(outLink.getAllowedModes());
 										link.setAllowedModes(aM);
 
 									} else {
 
 										// Only merge links with same attributes
 										if(bothLinksHaveSameLinkStats(inLink, outLink)){
 											link = this.network.getFactory().createLink(
 													Id.create(inLink.getId() + "-" + outLink.getId(), Link.class),
 													inLink.getFromNode(),
 													outLink.getToNode());
 
 											link.setLength(inLink.getLength() + outLink.getLength());
 											link.setFreespeed(inLink.getFreespeed());
 											link.setCapacity(inLink.getCapacity());
 											link.setNumberOfLanes(inLink.getNumberOfLanes());
 											link.setAllowedModes(inLink.getAllowedModes());
 										}
 
 									}
 
 									if(link != null){
 										if(inLink.getAllowedModes().contains(TransportMode.pt) && outLink.getAllowedModes().contains(TransportMode.pt)){
 											// both have the pt tag, so they can possibly be merged
 											if(removeLinksFromTransitSchedule(link, inLink, outLink)){
 												// Links could be removed from transit schedule, so proceed with the network
 												this.network.addLink(link);
 												this.network.removeLink(inLink.getId());
 												this.network.removeLink(outLink.getId());
 											}
 										} else if(!inLink.getAllowedModes().contains(TransportMode.pt) && !outLink.getAllowedModes().contains(TransportMode.pt)){
 											// both do not have the pt tag, so they can be merged definitely
 											this.network.addLink(link);
 											this.network.removeLink(inLink.getId());
 											this.network.removeLink(outLink.getId());
 										}
 									}
 
 								} else {
 //									log.info("inLink " + inLink.getId().toString() + " and outLink " + outLink.getId().toString() + " will not be touched");
 								}
 							}
 						}
 					}
 				}
 			}
 
 		}
 
 		NetworkRemoveUnusedNodes nc = new NetworkRemoveUnusedNodes();
 		nc.run(this.network);
 
 		nodeTopo = new NetworkCalcTopoType();
 		nodeTopo.run(this.network);
 
 		log.info("  resulting network contains " + this.network.getNodes().size() + " nodes and " +
 				this.network.getLinks().size() + " links.");
 		log.info("done.");
 	}
 
 	private void registerLinksBlockedByCountStation() {
 		for (Node node : this.network.getNodes().values()) {
 			this.checkNodeIsMarkedAsCountStation(node);
 		}		
 	}
 
 	private void registerLinksBlockedByAnythingElse() {
 		// add additional links blocked by something
 		int i = 0;
 		for (String idString : this.additionalLinksBlockedBySomething) {
 			this.linksBlockedByFacility.add(idString);
 			i++;
 		}
 		log.info("Marked " + i + " links blocked by anything");
 		this.additionalLinksBlockedBySomething = null;
 	}
 
 	private void registerLinksBlockedByTransitStopFacility() {
 		for (TransitStopFacility transitStop : this.transitSchedule.getFacilities().values()) {
 			this.linksBlockedByFacility.add(transitStop.getLinkId().toString());
 		}
 		log.info("Marked " + this.linksBlockedByFacility.size() + " links as blocked by transit stops.");
 	}
 
 
/** If the node is marked as count station, move the count data to outCount and mark the node as non changeable */
 private void checkNodeIsMarkedAsCountStation(Node node){}

 

}