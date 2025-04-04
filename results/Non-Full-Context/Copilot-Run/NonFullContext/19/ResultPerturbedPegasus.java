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
 
 
/** The @link Carrier is now an @link Carrier Service. */
 public static void addService(Carrier carrier, CarrierService carrierService){
	 		carrier.getServices().add(carrierService);
 	}
 
 	public static CarrierService getService(Carrier carrier, Id<CarrierService> serviceId){
 		for(CarrierService service : carrier.getServices()){
 			if(service.getId().equals(serviceId)){
 				return service;
 			}
 		}
 		log.error("Service with Id does not exists", new IllegalStateException("service with id " + serviceId + " is missing in Carrier: " + carrier.getId()));
 		return null;
 	}
 
 	public static void addCarrierPlan(Carrier carrier, CarrierPlan carrierPlan){
 		carrier.getPlans().add(carrierPlan);
 	}
 
 	public static CarrierPlan getCarrierPlan(Carrier carrier, Id<CarrierPlan> planId){
 		for(CarrierPlan plan : carrier.getPlans()){
 			if(plan.getId().equals(planId)){
 				return plan;
 			}
 		}
 		log.error("Plan with Id does not exists", new IllegalStateException("plan with id " + planId + " is missing in Carrier: " + carrier.getId()));
 		return null;
 	}
 
 	public static void addCarrierPlan(Carrier carrier, CarrierPlan carrierPlan, int index){
 		carrier.getPlans().add(index, carrierPlan);
 	}
 
 	public static void removeCarrierPlan(Carrier carrier, Id<CarrierPlan> planId){
 		for(CarrierPlan plan : carrier.getPlans()){
 			if(plan.getId().equals(planId)){
 				carrier.getPlans().remove(plan);
 				return;
 			}
 		}
 		log.error("Plan with Id does not exists", new IllegalStateException("		
 }

 

}