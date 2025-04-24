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
 public static void addCarrierVehicle(Carrier carrier, CarrierVehicle carrierVehicle){
	 		carrier.addVehicle(carrierVehicle);
 	}
 
 	public static void addCarrierService(Carrier carrier, CarrierService carrierService){
 		carrier.addService(carrierService);
 	}
 
 	public static void addCarrierPlan(Carrier carrier, CarrierPlan carrierPlan){
 		carrier.addPlan(carrierPlan);
 	}
 
 	public static void addCarrierPlan(Carrier carrier, CarrierPlan carrierPlan, int index){
 		carrier.addPlan(carrierPlan, index);
 	}
 
 	public static void addCarrierPlan(Carrier carrier, CarrierPlan carrierPlan, String name){
 		carrier.addPlan(carrierPlan, name);
 	}
 
 	public static void addCarrierPlan(Carrier carrier, CarrierPlan carrierPlan, String name, int index){
 		carrier.addPlan(carrierPlan, name, index);
 	}
 
 	public static void addCarrierPlan(Carrier carrier, CarrierPlan carrierPlan, String name, int index, boolean replace){
 		carrier.addPlan(carrierPlan, name, index, replace);
 	}
 
 	public static void addCarrierPlan(Carrier carrier, CarrierPlan carrierPlan, String name, int index, boolean replace, boolean addToEnd){
 		carrier.addPlan(carrierPlan, name, index, replace, addToEnd);
 	}
 
 	public static void addCarrierPlan(Carrier carrier, CarrierPlan carrierPlan, String name, int index, boolean replace, boolean addToEnd, boolean addToFront){
 		carrier.addPlan(carrierPlan, name, index, replace, addToEnd, addToFront);
 	}
 
 	public static void addCarrierPlan(Carrier carrier, CarrierPlan carrierPlan, String name, int index, boolean replace, boolean addToEnd, boolean addToFront, boolean addToFrontOfName){
 		carrier.addPlan(carrierPlan, name, index, replace,		
 }

 

}