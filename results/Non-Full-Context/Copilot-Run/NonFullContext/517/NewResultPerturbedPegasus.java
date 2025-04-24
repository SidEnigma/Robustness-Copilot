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
 
 
/** The CarrierCapabilites are added to the carrier vehicle. */

public static void addCarrierVehicle(Carrier carrier, CarrierVehicle carrierVehicle) {
    // The CarrierCapabilites are added to the carrier vehicle.
    carrierVehicle.setCarrierCapabilities(carrier.getCarrierCapabilities());
    carrierVehicle.setTransportMode(TransportMode.truck);
    carrierVehicle.setCarrierId(carrier.getId());

    List<Vehicle> vehicles = new ArrayList<>();
    vehicles.add(carrierVehicle);
    carrier.setVehicles(vehicles);

    log.info("Carrier vehicle added successfully.");
}
 

}