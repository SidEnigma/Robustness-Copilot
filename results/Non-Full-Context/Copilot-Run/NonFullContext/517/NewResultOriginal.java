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
 
 
/** Adds an carrierVehicle to the CarrierCapabilites of the Carrier. */

public static void addCarrierVehicle(Carrier carrier, CarrierVehicle carrierVehicle) {
    // Check if the carrier and carrierVehicle are not null
    if (carrier != null && carrierVehicle != null) {
        // Get the list of carrier vehicles from the carrier
        List<CarrierVehicle> carrierVehicles = carrier.getCarrierCapabilities().getCarrierVehicles();

        // Check if the carrier vehicles list is null, create a new list if it is
        if (carrierVehicles == null) {
            carrierVehicles = new ArrayList<>();
            carrier.getCarrierCapabilities().setCarrierVehicles(carrierVehicles);
        }

        // Add the carrierVehicle to the list
        carrierVehicles.add(carrierVehicle);

        // Log the addition of the carrierVehicle
        log.info("Added carrierVehicle " + carrierVehicle.getId() + " to carrier " + carrier.getId());
    } else {
        log.error("Carrier or carrierVehicle is null");
    }
}
 

}