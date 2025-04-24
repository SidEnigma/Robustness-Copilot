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
 
 
/** Adds a carrierVehicle object to the CarrierCapabilities of the Carrirer. */
 public static void addCarrierVehicle(Carrier carrier, CarrierVehicle carrierVehicle){
	 		CarrierCapabilities carrierCapabilities = carrier.getCarrierCapabilities();
 		carrierCapabilities.addCarrierVehicle(carrierVehicle);
 	}		
 }

 

}