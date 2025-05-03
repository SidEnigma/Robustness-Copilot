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
 
 	/**
 	 * Creates a new {@link Carriers} container only with {@link CarrierShipment}s
 	 * for creating a new VRP. As consequence of the transformation of
 	 * {@link CarrierService}s to {@link CarrierShipment}s the solution of the VRP
 	 * can have tours with vehicles returning to the depot and load for another tour
 	 * instead of creating another vehicle with additional (fix) costs. <br/>
 	 * The method is meant for multi-depot problems. Here, the original "services"
 	 * input does not have an assignment of services to depots. The solution to the
 	 * problem, however, does. So the assignment is taken from that solution, and
 	 * each returned {@link Carrier} has that depot as pickup location in each
 	 * shipment.
 	 *
 	 * @param carriers carriers with a Solution (result of solving the VRP).
 	 * @return Carriers carriersWithShipments
 	 */
 	public static Carriers createShipmentVRPCarrierFromServiceVRPSolution(Carriers carriers) {
 		Carriers carriersWithShipments = new Carriers();
 		for (Carrier carrier : carriers.getCarriers().values()) {
 			Carrier carrierWS = CarrierUtils.createCarrier(carrier.getId());
 			if (carrier.getShipments().size() > 0) {
 				copyShipments(carrierWS, carrier);
 			}
 			//			copyPickups(carrierWS, carrier);	//Not implemented yet due to missing CarrierPickup in freight contrib, kmt Sep18
 			//			copyDeliveries(carrierWS, carrier); //Not implemented yet due to missing CarrierDelivery in freight contrib, kmt Sep18
 			if (carrier.getServices().size() > 0) {
 				createShipmentsFromServices(carrierWS, carrier);
 			}
 			carrierWS.setCarrierCapabilities(carrier.getCarrierCapabilities()); // vehicles and other carrierCapabilites
 			carriersWithShipments.addCarrier(carrierWS);
 		}
 		return carriersWithShipments;
 	}
 
 	/**
 	 * @deprecated -- please inline.  Reason: move syntax closer to how it is done in {@link ConfigUtils}.
 	 */
 	public static Carriers getOrCreateCarriers(Scenario scenario){
 		return addOrGetCarriers( scenario );
 	}
 	public static Carriers addOrGetCarriers( Scenario scenario ) {
 		// I have separated getOrCreateCarriers and getCarriers, since when the
 		// controler is started, it is better to fail if the carriers are not found.
 		// kai, oct'19
 		Carriers carriers = (Carriers) scenario.getScenarioElement(CARRIERS);
 		if (carriers == null) {
 			carriers = new Carriers();
 			scenario.addScenarioElement(CARRIERS, carriers);
 		}
 		return carriers;
 	}
 
 	public static Carriers getCarriers(Scenario scenario) {
 		// I have separated getOrCreateCarriers and getCarriers, since when the controler is started, it is better to fail if the carriers are
 		// not found. kai, oct'19
 		if ( scenario.getScenarioElement( CARRIERS ) == null ) {
 			throw new RuntimeException( "\n\ncannot retrieve carriers from scenario; typical ways to resolve that problem are to call " +
 								    "FreightUtils.getOrCreateCarriers(...) or FreightUtils.loadCarriersAccordingToFreightConfig(...) early enough\n") ;
 		}
 		return (Carriers) scenario.getScenarioElement(CARRIERS);
 	}
 
 	public static CarrierVehicleTypes getCarrierVehicleTypes(Scenario scenario) {
 		CarrierVehicleTypes types = (CarrierVehicleTypes) scenario.getScenarioElement(CARRIERVEHICLETYPES);
 		if (types == null) {
 			types = new CarrierVehicleTypes();
 			scenario.addScenarioElement(CARRIERVEHICLETYPES, types);
 		}
 		return types;
 	}
 
 	/**
 	 * Use if carriers and carrierVehicleTypes are set by input file
 	 *
 	 * @param scenario
 	 */
 	public static void loadCarriersAccordingToFreightConfig(Scenario scenario) {
 		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), FreightConfigGroup.class);
 
 		Carriers carriers = addOrGetCarriers( scenario ); // also registers with scenario
 		new CarrierPlanXmlReader( carriers ).readURL( IOUtils.extendUrl(scenario.getConfig().getContext(), freightConfigGroup.getCarriersFile()) );
 		CarrierVehicleTypes vehTypes = getCarrierVehicleTypes(scenario);
 		new CarrierVehicleTypeReader( vehTypes ).readURL( IOUtils.extendUrl(scenario.getConfig().getContext(), freightConfigGroup.getCarriersVehicleTypesFile()) );
 		new CarrierVehicleTypeLoader( carriers ).loadVehicleTypes( vehTypes );
 	}
 
 	/**
 	 * NOT implemented yet due to missing CarrierDelivery in freight contrib, kmt
 	 * Sep18
 	 *
 	 * @param carrierWS
 	 * @param carrier
 	 */
 	private void copyDeliveries(Carrier carrierWS, Carrier carrier) {
 		log.error("Coping of Deliveries is NOT implemented yet due to missing CarrierDelivery in freight contrib");
 	}
 
 	/**
 	 * NOT implemented yet due to missing CarrierPickup in freight contrib, kmt
 	 * Sep18
 	 *
 	 * @param carrierWS
 	 * @param carrier
 	 */
 	private void copyPickups(Carrier carrierWS, Carrier carrier) {
 		log.error("Coping of Pickup is NOT implemented yet due to missing CarrierPickup in freight contrib");
 	}
 
 
/** Transfer every shipment made with the old carrier to the new one. */

private static void copyShipments(Carrier carrierWS, Carrier carrier) {
    for (Shipment shipment : carrier.getShipments()) {
        Shipment newShipment = new Shipment(shipment.getId(), shipment.getPickupLocation(), shipment.getDeliveryLocation(), shipment.getPickupTimeWindow(), shipment.getDeliveryTimeWindow(), shipment.getCapacity());
        carrierWS.addShipment(newShipment);
    }
}
 

}