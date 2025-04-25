package org.matsim.contrib.freight.carrier;
 
 import org.apache.log4j.Logger;
 import org.matsim.api.core.v01.Id;
 import org.matsim.api.core.v01.TransportMode;
 import org.matsim.vehicles.Vehicle;
 
 import java.util.ArrayList;
 import java.util.List;
 
 public class CarrierUtils{
 
 	private static Logger log = Logger.getLogger(CarrierUtils.class);
 
 	public static Carrier createCarrier( Id<Carrier> id ){
 		return new CarrierImpl(id);
 	}
 
 	/**
 	 * Adds an carrierVehicle to the CarrierCapabilites of the Carrier.
 	 * @param carrier
 	 * @param carrierVehicle
 	 */
 	public static void addCarrierVehicle(Carrier carrier, CarrierVehicle carrierVehicle){
 		carrier.getCarrierCapabilities().getCarrierVehicles().put(carrierVehicle.getId(), carrierVehicle);
 	}
 
 	public static CarrierVehicle getCarrierVehicle(Carrier carrier, Id<Vehicle> vehicleId){
 		if(carrier.getCarrierCapabilities().getCarrierVehicles().containsKey(vehicleId)){
 			return carrier.getCarrierCapabilities().getCarrierVehicles().get(vehicleId);
 		}
 		log.error("Vehicle with Id does not exists", new IllegalStateException("vehicle with id " + vehicleId + " is missing in Carrier: " + carrier.getId()));
 		return null;
 	}
 
 	/**
 	 * Adds an {@link CarrierService} to the {@link Carrier}.
 	 * @param carrier
 	 * @param carrierService
 	 */
 	public static void addService(Carrier carrier, CarrierService carrierService){
 		carrier.getServices().put(carrierService.getId(), carrierService);
 	}
 
 	public static CarrierService getService(Carrier carrier, Id<CarrierService> serviceId){
 		if(carrier.getServices().containsKey(serviceId)){
 			return carrier.getServices().get(serviceId);
 		}
 		log.error("Service with Id does not exists", new IllegalStateException("Serice with id " + serviceId + " is missing in Carrier: " + carrier.getId()));
 		return null;
 	}
 
 
/** Adds a {@link CarrierShipment} to the {@link Carrier}. */
 public static void addShipment(Carrier carrier, CarrierShipment carrierShipment){
	 		carrier.getShipments().put(carrierShipment.getId(), carrierShipment);
 	}
 
 	public static CarrierShipment getShipment(Carrier carrier, Id<CarrierShipment> shipmentId){
 		if(carrier.getShipments().containsKey(shipmentId)){
 			return carrier.getShipments().get(shipmentId);
 		}
 		log.error("Shipment with Id does not exists", new IllegalStateException("Shipment with id " + shipmentId + " is missing in Carrier: " + carrier.getId()));
 		return null;
 	}
 
 	/**
 	 * Adds a {@link CarrierPlan} to the {@link Carrier}.
 	 * @param carrier
 	 * @param carrierPlan
 	 */
 	public static void addPlan(Carrier carrier, CarrierPlan carrierPlan){
 		carrier.getPlans().put(carrierPlan.getId(), carrierPlan);
 	}
 
 	public static CarrierPlan getPlan(Carrier carrier, Id<CarrierPlan> planId){
 		if(carrier.getPlans().containsKey(planId)){
 			return carrier.getPlans().get(planId);
 		}
 		log.error("Plan with Id does not exists", new IllegalStateException("Plan with id " + planId + " is missing in Carrier: " + carrier.getId()));
 		return null;
 	}
 
 	/**
 	 * Adds a {@link CarrierPlan} to the {@link Carrier}.
 	 * @param carrier
 	 * @param carrierPlan
 	 */
 	public static void addPlan(Carrier carrier, CarrierPlan carrierPlan, Id<CarrierPlan> planId){
 		carrier.getPlans().put(planId, carrierPlan);
 	}
 
 	public static CarrierPlan getPlan(Carrier carrier, Id<CarrierPlan> planId, Id<Carrier> carrierId){
 		if(		
 }

 

}