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

public static void addCarrierVehicle(Carrier carrier, CarrierVehicle carrierVehicle) {
    // Check if the carrier and carrierVehicle objects are not null
    if (carrier != null && carrierVehicle != null) {
        // Get the list of carrier vehicles from the carrier object
        List<CarrierVehicle> carrierVehicles = carrier.getCarrierCapabilities().getCarrierVehicles();

        // If the list is null, create a new ArrayList
        if (carrierVehicles == null) {
            carrierVehicles = new ArrayList<>();
        }

        // Add the carrierVehicle object to the list
        carrierVehicles.add(carrierVehicle);

        // Set the updated list of carrier vehicles back to the carrier object
        carrier.getCarrierCapabilities().setCarrierVehicles(carrierVehicles);

        // Log the addition of the carrierVehicle
        log.info("Added carrierVehicle " + carrierVehicle.getId() + " to carrier " + carrier.getId());
    } else {
        // Log an error if either the carrier or carrierVehicle object is null
        log.error("Carrier or carrierVehicle object is null");
    }
}
 

}