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
 
 
/** Adds a {carrierservice @link} to the {@link Carrier}. */
 public static void addService(Carrier carrier, CarrierService carrierService){
	 	carrier.getServices().add(carrierService);		
 }

 

}