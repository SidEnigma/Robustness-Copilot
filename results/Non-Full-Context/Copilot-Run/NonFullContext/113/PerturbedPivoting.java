/* *********************************************************************** *
  * project: org.matsim.*
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 
 package org.matsim.contrib.freight.utils;
 
 import com.graphhopper.jsprit.analysis.toolbox.StopWatch;
 import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
 import com.graphhopper.jsprit.core.algorithm.listener.VehicleRoutingAlgorithmListeners;
 import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
 import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
 import com.graphhopper.jsprit.core.util.Solutions;
 import org.apache.log4j.Logger;
 import org.matsim.api.core.v01.Id;
 import org.matsim.api.core.v01.Scenario;
 import org.matsim.api.core.v01.network.Link;
 import org.matsim.contrib.freight.FreightConfigGroup;
 import org.matsim.contrib.freight.carrier.*;
 import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
 import org.matsim.contrib.freight.carrier.Tour.TourElement;
 import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
 import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
 import org.matsim.contrib.freight.jsprit.NetworkRouter;
 import org.matsim.core.config.ConfigUtils;
 import org.matsim.core.utils.io.IOUtils;
 import org.matsim.utils.objectattributes.attributable.Attributes;
 import org.matsim.vehicles.VehicleType;
 
 import javax.management.InvalidAttributeValueException;
 import java.util.*;
 import java.util.concurrent.ExecutionException;
 import java.util.concurrent.ForkJoinPool;
 import java.util.stream.Collectors;
 
 /**
  * Utils for the work with the freight contrib
  * 
  * @author kturner
  *
  */
 public class FreightUtils {
 
 	/**
 	 * From the outside, rather use {@link FreightUtils#getCarriers(Scenario)} .
 	 * This string constant will eventually become private.
 	 */
 	private static final String CARRIERS = "carriers";
 	private static final String CARRIERVEHICLETYPES = "carrierVehicleTypes";
 	private static final Logger log = Logger.getLogger(FreightUtils.class);
 
 	private static final String ATTR_SKILLS = "skills";
 
 	/**
 	 * Runs jsprit and so solves the VehicleRoutingProblem (VRP) for all {@link Carriers}, doing the following steps:
 	 * 	- creating NetbasedCosts based on the network
 	 * 	- building and solving the VRP for all carriers using jsprit
 	 * 	- take the (best) solution, route and add it as {@link CarrierPlan} to the {@link Carrier}.
 	 *
 	 *
 	 * @param scenario
 	 * @throws ExecutionException, InterruptedException
 	 */
 	public static void runJsprit(Scenario scenario) throws ExecutionException, InterruptedException{
 		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule( scenario.getConfig(), FreightConfigGroup.class );
 
 		NetworkBasedTransportCosts.Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance(
 				scenario.getNetwork(), FreightUtils.getCarrierVehicleTypes(scenario).getVehicleTypes().values() );
 		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build() ;
 
 		Carriers carriers = FreightUtils.getCarriers(scenario);
 
 		HashMap<Id<Carrier>, Integer> carrierActivityCounterMap = new HashMap<>();
 
 		// Fill carrierActivityCounterMap -> basis for sorting the carriers by number of activities before solving in parallel
 		for (Carrier carrier : carriers.getCarriers().values()) {
 			carrierActivityCounterMap.put(carrier.getId(), carrierActivityCounterMap.getOrDefault(carrier.getId(), 0) + carrier.getServices().size());
 			carrierActivityCounterMap.put(carrier.getId(), carrierActivityCounterMap.getOrDefault(carrier.getId(), 0) + carrier.getShipments().size());
 		}
 
 		HashMap<Id<Carrier>, Integer> sortedMap = carrierActivityCounterMap.entrySet().stream()
 				.sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
 				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
 
 		ArrayList<Id<Carrier>> tempList = new ArrayList<>(sortedMap.keySet());
 		ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
 		forkJoinPool.submit(() -> tempList.parallelStream().forEach(carrierId -> {
 			Carrier carrier = carriers.getCarriers().get(carrierId);
 
 			double start = System.currentTimeMillis();
 			int serviceCount = carrier.getServices().size();
 			log.info("Start tour planning for " + carrier.getId() + " which has " + serviceCount + " services");
 
 			VehicleRoutingProblem problem = MatsimJspritFactory.createRoutingProblemBuilder(carrier, scenario.getNetwork())
 					.setRoutingCost(netBasedCosts)
 					.build();
 			VehicleRoutingAlgorithm algorithm = MatsimJspritFactory.loadOrCreateVehicleRoutingAlgorithm(scenario, freightConfigGroup, netBasedCosts, problem);
 
 			algorithm.getAlgorithmListeners().addListener(new StopWatch(), VehicleRoutingAlgorithmListeners.Priority.HIGH);
 			int jspritIterations = CarrierUtils.getJspritIterations(carrier);
 			try {
 				if (jspritIterations > 0) {
 				algorithm.setMaxIterations(jspritIterations);
 				} else {
 				throw new InvalidAttributeValueException(
 						"Carrier has invalid number of jsprit iterations. They must be positive! Carrier id: "
 								+ carrier.getId().toString());}
 			} catch (Exception e) {
 				throw new RuntimeException(e);
 //				e.printStackTrace();
 			}
 
 			VehicleRoutingProblemSolution solution = Solutions.bestOf(algorithm.searchSolutions());
 
 			log.info("tour planning for carrier " + carrier.getId() + " took "
 					+ (System.currentTimeMillis() - start) / 1000 + " seconds.");
 
 			CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, solution);
 
 			log.info("routing plan for carrier " + carrier.getId());
 			NetworkRouter.routePlan(newPlan, netBasedCosts);
 			log.info("routing for carrier " + carrier.getId() + " finished. Tour planning plus routing took "
 					+ (System.currentTimeMillis() - start) / 1000 + " seconds.");
 
 			carrier.setSelectedPlan(newPlan);
 		})).get();
 
 	}
 
 
/** Creates a container {@link Carriers} only with {@link CarrierShipment} to create a new VRP. */
 public static Carriers createShipmentVRPCarrierFromServiceVRPSolution(Carriers carriers){}

 

}