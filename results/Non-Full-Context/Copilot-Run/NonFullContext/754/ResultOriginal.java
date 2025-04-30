/* *********************************************************************** *
  * project: org.matsim.*
  * RoutingHandler.java
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
 package org.matsim.core.router;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import javax.inject.Inject;
 import javax.inject.Provider;
 
 import org.apache.log4j.Logger;
 import org.matsim.api.core.v01.population.Activity;
 import org.matsim.api.core.v01.population.Leg;
 import org.matsim.api.core.v01.population.Person;
 import org.matsim.api.core.v01.population.Plan;
 import org.matsim.api.core.v01.population.PlanElement;
 import org.matsim.core.api.internal.MatsimExtensionPoint;
 import org.matsim.core.config.Config;
 import org.matsim.core.gbl.Gbl;
 import org.matsim.core.population.PopulationUtils;
 import org.matsim.facilities.Facility;
 
 import com.google.common.base.Preconditions;
 
 /**
  * Class acting as an intermediate between clients needing to
  * compute routes and all registered {@link RoutingModule}s.
  * It provides convenience methods to route an individual trip with
  * a desired mode or to identify trips.
  * <p></p>
  *
  * See {@link tutorial.programming.ownMobsimAgentUsingRouter.RunOwnMobsimAgentUsingRouterExample} for an example
  * how to use this API from your own code.
  * See {@link tutorial.programming.example12PluggableTripRouter.RunPluggableTripRouterExample} and {@link tutorial.programming.example13MultiStageTripRouting.RunTeleportationMobsimWithCustomRoutingExample} for examples
  * how to extend or replace this behavior with your own.
  *
  * @author thibautd
  */
 public final class TripRouter implements MatsimExtensionPoint {
 	private static final Logger log = Logger.getLogger(TripRouter.class );
 
 	private final Map<String, RoutingModule> routingModules = new HashMap<>();
 	private final FallbackRoutingModule fallbackRoutingModule;
 
 	private Config config;
 	// (I need the config in the PlanRouter to figure out activity end times. And since the PlanRouter is not
 	// injected, I cannot get it there directly.  kai, oct'17)
 
 	public static final class Builder {
 		private final Config config;
 		private FallbackRoutingModule fallbackRoutingModule = new FallbackRoutingModuleDefaultImpl() ;
 		private Map<String, Provider<RoutingModule>> routingModuleProviders = new LinkedHashMap<>() ;
 		public Builder( Config config ) {
 			this.config = config ;
 		}
 		public Builder setRoutingModule(String mainMode, RoutingModule routingModule ) {
 			// the initial API accepted routing modules.  injection, however, takes routing module providers.  (why?)
 			// trying to bring these two into line here.  maybe some other approach would be preferred, don't know.  kai, jun'18
 			this.routingModuleProviders.put( mainMode, new Provider<RoutingModule>(){
 				@Override public RoutingModule get() {
 					return routingModule ;
 				}
 			} ) ;
 			return this ;
 		}
 		public TripRouter build() {
 			return new TripRouter( routingModuleProviders, config, fallbackRoutingModule ) ;
 		}
 	}
 
 //	@Deprecated // use the Builder instead.  kai, oct'17
 //	public TripRouter() {}
 //	// yyyyyy I guess this is meant as a way to create the trip router without injection, and to set its internals afterwards.  But
 //	// is it so sensible to have this in this way?  The injection stuff states that the material is immutable after injection; here we introduce a
 //	// way to get around that again, and even to change the injected material later.
 //	// I would expect a Builder instead.
 //	// kai, sep'16
 
 	@Inject
 	TripRouter( Map<String, Provider<RoutingModule>> routingModuleProviders, Config config,
 			FallbackRoutingModule fallbackRoutingModule ) {
 		this.fallbackRoutingModule = fallbackRoutingModule;
 
 		for (Map.Entry<String, Provider<RoutingModule>> entry : routingModuleProviders.entrySet()) {
 			setRoutingModule(entry.getKey(), entry.getValue().get());
 		}
 		this.config = config ;
 	}
 
 	// /////////////////////////////////////////////////////////////////////////
 	// constructors
 	// /////////////////////////////////////////////////////////////////////////
 
 	// /////////////////////////////////////////////////////////////////////////
 	// setters / getters
 	// /////////////////////////////////////////////////////////////////////////
 	/**
 	 * Sets the {@link RoutingModule} to use for the given (main) mode.
 	 * @param mainMode the mode
 	 * @param module the module to use with this mode
 	 * @return the previously registered {@link RoutingModule} for this mode if any, null otherwise.
 	 */
 	@Deprecated // use the Builder instead.  kai, oct'17
 	/* package-private */ RoutingModule setRoutingModule(
 			final String mainMode,
 			final RoutingModule module) {
 		RoutingModule old = routingModules.put( mainMode , module );
 
 		return old;
 	}
 
 	public RoutingModule getRoutingModule(final String mainMode) {
 		return routingModules.get( mainMode );
 	}
 
 	public Set<String> getRegisteredModes() {
 		return Collections.unmodifiableSet( routingModules.keySet() );
 	}
 
 	// /////////////////////////////////////////////////////////////////////////
 	// Handling methods
 	// /////////////////////////////////////////////////////////////////////////
 
/** Routes a trip between the given O/D pair, with the given main mode. */
 public synchronized List<? extends PlanElement> calcRoute(final String mainMode, final Facility fromFacility, final Facility toFacility, final double departureTime, final Person person){
	 		Preconditions.checkNotNull(mainMode);
 		Preconditions.checkNotNull(fromFacility);
 		Preconditions.checkNotNull(toFacility);
 		Preconditions.checkNotNull(person);
 
 		RoutingModule routingModule = routingModules.get(mainMode);
 		if (routingModule == null) {
 			log.error("No routing module for mode " + mainMode + " found. Using fallback routing module.");
 			routingModule = fallbackRoutingModule;
 		}
 
 		return routingModule.calcRoute(fromFacility, toFacility, departureTime, person);
 	}
 
 	/**
 	 * Routes a trip between the given O/D pair, with the given main mode.
 	 * @param mainMode the mode
 	 * @param fromFacility the origin facility
 	 * @param toFacility the destination facility
 	 * @param departureTime the departure time
 	 * @param person the person
 	 * @return the route as a list of {@link PlanElement}s
 	 */
 	public synchronized List<? extends PlanElement> calcRoute(final String mainMode, final Facility fromFacility, final Facility toFacility, final double departureTime, final Person person, final boolean isVehicleAvailable) {
 		Preconditions.checkNotNull(mainMode);
 		Preconditions.checkNotNull(fromFacility);
 		Preconditions.checkNotNull(toFacility);
 		Preconditions.checkNotNull(person);
 
 		RoutingModule routingModule = routingModules.get(mainMode);
 		if (routingModule == null) {
 			log.error("No routing module for mode " + mainMode + " found. Using fallback routing module.");
 			routingModule = fallbackRoutingModule;
 		}
 
 		return routingModule.calcRoute(fromFacility, toFacility, departureTime, person, is		
 }

 

}