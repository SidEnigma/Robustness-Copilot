/* *********************************************************************** *
  * project: matsim
  * PopulationUtils.java
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
 
 package org.matsim.core.population;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.PipedInputStream;
 import java.io.PipedOutputStream;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Random;
 import java.util.SortedMap;
 import java.util.TreeMap;
 
 import org.apache.log4j.Logger;
 import org.matsim.api.core.v01.Coord;
 import org.matsim.api.core.v01.Id;
 import org.matsim.api.core.v01.Scenario;
 import org.matsim.api.core.v01.TransportMode;
 import org.matsim.api.core.v01.network.Link;
 import org.matsim.api.core.v01.network.Network;
 import org.matsim.api.core.v01.population.Activity;
 import org.matsim.api.core.v01.population.HasPlansAndId;
 import org.matsim.api.core.v01.population.Leg;
 import org.matsim.api.core.v01.population.Person;
 import org.matsim.api.core.v01.population.Plan;
 import org.matsim.api.core.v01.population.PlanElement;
 import org.matsim.api.core.v01.population.Population;
 import org.matsim.api.core.v01.population.PopulationFactory;
 import org.matsim.api.core.v01.population.Route;
 import org.matsim.core.config.Config;
 import org.matsim.core.config.ConfigUtils;
 import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
 import org.matsim.core.config.groups.PlansConfigGroup;
 import org.matsim.core.gbl.Gbl;
 import org.matsim.core.gbl.MatsimRandom;
 import org.matsim.core.population.io.PopulationReader;
 import org.matsim.core.population.io.PopulationWriter;
 import org.matsim.core.population.io.StreamingPopulationReader;
 import org.matsim.core.population.routes.CompressedNetworkRouteFactory;
 import org.matsim.core.population.routes.LinkNetworkRouteFactory;
 import org.matsim.core.population.routes.NetworkRoute;
 import org.matsim.core.population.routes.RouteFactories;
 import org.matsim.core.population.routes.RouteFactory;
 import org.matsim.core.population.routes.RouteUtils;
 import org.matsim.core.router.TripStructureUtils;
 import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
 import org.matsim.core.scenario.MutableScenario;
 import org.matsim.core.scenario.ScenarioUtils;
 import org.matsim.core.utils.io.IOUtils;
 import org.matsim.core.utils.io.UncheckedIOException;
 import org.matsim.core.utils.misc.OptionalTime;
 import org.matsim.facilities.ActivityFacilities;
 import org.matsim.facilities.ActivityFacility;
 import org.matsim.utils.objectattributes.attributable.Attributable;
 import org.matsim.utils.objectattributes.attributable.Attributes;
 import org.matsim.utils.objectattributes.attributable.AttributesUtils;
 
 /**
  * @author nagel, ikaddoura
  */
 public final class PopulationUtils {
 	private static final Logger log = Logger.getLogger( PopulationUtils.class );
 //	private static final PopulationFactory populationFactory = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getPopulation().getFactory() ;
 	private static final PopulationFactory populationFactory = createPopulation( new PlansConfigGroup(), null  ).getFactory() ;
 	// try to avoid misleading comment about config context.  kai, dec'18
 
 	/**
 	 * @deprecated -- this is public only because it is needed in the also deprecated method {@link PlansConfigGroup#getSubpopulationAttributeName()}
 	 */
 	@Deprecated
 	public static final String SUBPOPULATION_ATTRIBUTE_NAME = "subpopulation";
 
 
 	/**
 	 * Is a namespace, so don't instantiate:
 	 */
 	private PopulationUtils() {}
 
 	/**
 	 *
 	 * Creates a new Population container. Population instances need a Config, because they need to know
 	 * about the modes of transport.
 	 *
 	 * @param config the configuration which is used to create the Population.
 	 * @return the new Population instance
 	 */
 	public static Population createPopulation(Config config) {
 		return createPopulation(config, null);
 	}
 
 	/**
 	 *
 	 * Creates a new Population container which, depending on
 	 * configuration, may make use of the specified Network instance to store routes
 	 * more efficiently.
 	 *
 	 * @param config the configuration which is used to create the Population.
 	 * @param network the Network to which Plans in this Population will refer.
 	 * @return the new Population instance
 	 */
 	public static Population createPopulation(Config config, Network network) {
 		return createPopulation(config.plans(), network);
 	}
 
 	public static Population createPopulation(PlansConfigGroup plansConfigGroup, Network network) {
 		// yyyy my intuition would be to rather get this out of a standard scenario. kai, jun'16
 		RouteFactories routeFactory = new RouteFactories();
 		String networkRouteType = plansConfigGroup.getNetworkRouteType();
 		RouteFactory factory;
 		if (PlansConfigGroup.NetworkRouteType.LinkNetworkRoute.equals(networkRouteType)) {
 			factory = new LinkNetworkRouteFactory();
 		} else if (PlansConfigGroup.NetworkRouteType.CompressedNetworkRoute.equals(networkRouteType) && network != null) {
 			factory = new CompressedNetworkRouteFactory(network);
 		} else {
 			throw new IllegalArgumentException("The type \"" + networkRouteType + "\" is not a supported type for network routes.");
 		}
 		routeFactory.setRouteFactory(NetworkRoute.class, factory);
 		return new PopulationImpl(new PopulationFactoryImpl(routeFactory));
 	}
 
 	//	public static Population createStreamingPopulation(PlansConfigGroup plansConfigGroup, Network network) {
 	//		// yyyy my intuition would be to rather get this out of a standard scenario. kai, jun'16
 	//		RouteFactories routeFactory = new RouteFactories();
 	//		String networkRouteType = plansConfigGroup.getNetworkRouteType();
 	//		RouteFactory factory;
 	//		if (PlansConfigGroup.NetworkRouteType.LinkNetworkRoute.equals(networkRouteType)) {
 	//			factory = new LinkNetworkRouteFactory();
 	//		} else if (PlansConfigGroup.NetworkRouteType.CompressedNetworkRoute.equals(networkRouteType) && network != null) {
 	//			factory = new CompressedNetworkRouteFactory(network);
 	//		} else {
 	//			throw new IllegalArgumentException("The type \"" + networkRouteType + "\" is not a supported type for network routes.");
 	//		}
 	//		routeFactory.setRouteFactory(NetworkRoute.class, factory);
 	//		return new Population(new PopulationFactoryImpl(routeFactory));
 	//	}
 
 	public static Leg unmodifiableLeg( Leg leg ) {
 		return new UnmodifiableLeg( leg ) ;
 	}
 
 	public static void resetRoutes( final Plan plan ) {
 		// loop over all <leg>s, remove route-information
 		// routing is done after location choice
 		for (PlanElement pe : plan.getPlanElements()) {
 			if (pe instanceof Leg) {
 				((Leg) pe).setRoute(null);
 			}
 		}
 	}
 
 	static class UnmodifiableLeg implements Leg {
 		private final Leg delegate ;
 		public UnmodifiableLeg( Leg leg ) {
 			this.delegate = leg ;
 		}
 		@Override
 		public String getMode() {
 			return this.delegate.getMode() ;
 		}
 
 		@Override
 		public void setMode(String mode) {
 			throw new UnsupportedOperationException() ;
 		}
 
 		@Override
 		public Route getRoute() {
 			// route should be unmodifiable. kai
 			return this.delegate.getRoute() ;
 		}
 
 		@Override
 		public void setRoute(Route route) {
 			throw new UnsupportedOperationException() ;
 		}
 
 		@Override
 		public OptionalTime getDepartureTime() {
 			return this.delegate.getDepartureTime() ;
 		}
 
 		@Override
 		public void setDepartureTime(double seconds) {
 			throw new UnsupportedOperationException() ;
 		}
 
 		@Override
 		public void setDepartureTimeUndefined() {
 			throw new UnsupportedOperationException();
 		}
 
 		@Override
 		public OptionalTime getTravelTime() {
 			return this.delegate.getTravelTime() ;
 		}
 
 		@Override
 		public void setTravelTime(double seconds) {
 			throw new UnsupportedOperationException() ;
 		}
 
 		@Override
 		public void setTravelTimeUndefined() {
 			throw new UnsupportedOperationException() ;
 		}
 
 		@Override
 		public String toString() {
 			return this.delegate.toString() ;
 		}
 
 		@Override
 		public Attributes getAttributes() {
 			// attributes should be made unmodifiable
 			return delegate.getAttributes();
 		}
 	}
 
 	public static Activity unmodifiableActivity( Activity act ) {
 		return new UnmodifiableActivity( act ) ;
 	}
 
 	static class UnmodifiableActivity implements Activity {
 		private final Activity delegate ;
 		public UnmodifiableActivity( Activity act ) {
 			this.delegate = act ;
 		}
 
 		@Override
 		public OptionalTime getEndTime() {
 			return this.delegate.getEndTime();
 		}
 
 		@Override
 		public void setEndTime(double seconds) {
 			throw new UnsupportedOperationException() ;
 		}
 
 		@Override
 		public void setEndTimeUndefined() {
 			throw new UnsupportedOperationException() ;
 		}
 
 		@Override
 		public String getType() {
 			return this.delegate.getType() ;
 		}
 
 		@Override
 		public void setType(String type) {
 			throw new UnsupportedOperationException() ;
 		}
 
 		@Override
 		public Coord getCoord() {
 			return this.delegate.getCoord() ;
 		}
 
 		@Override
 		public OptionalTime getStartTime() {
 			return this.delegate.getStartTime() ;
 		}
 
 		@Override
 		public void setStartTime(double seconds) {
 			throw new UnsupportedOperationException() ;
 		}
 
 		@Override
 		public void setStartTimeUndefined() {
 			throw new UnsupportedOperationException() ;
 		}
 
 		@Override
 		public OptionalTime getMaximumDuration() {
 			return this.delegate.getMaximumDuration() ;
 		}
 
 		@Override
 		public void setMaximumDuration(double seconds) {
 			throw new UnsupportedOperationException() ;
 		}
 
 		@Override
 		public void setMaximumDurationUndefined() {
 			throw new UnsupportedOperationException();
 		}
 
 		@Override
 		public Id<Link> getLinkId() {
 			return this.delegate.getLinkId() ;
 		}
 
 		@Override
 		public Id<ActivityFacility> getFacilityId() {
 			return this.delegate.getFacilityId() ;
 		}
 		@Override
 		public String toString() {
 			return this.delegate.toString() ;
 		}
 
 		@Override
 		public void setLinkId(Id<Link> id) {
 			throw new UnsupportedOperationException() ;
 		}
 
 		@Override
 		public void setFacilityId(Id<ActivityFacility> id) {
 			throw new UnsupportedOperationException() ;
 		}
 
 		@Override
 		public void setCoord(Coord coord) {
 			throw new RuntimeException("not implemented") ;
 		}
 
 		@Override
 		public Attributes getAttributes() {
 			// attributes should be made unmodifiable
 			return delegate.getAttributes();
 		}
 	}
 
 	/**
 	 * The idea of this method is to mirror the concept of Collections.unmodifiableXxx( xxx ) .
 	 * <p></p>
 	 */
 	public static Plan unmodifiablePlan(Plan plan) {
 		return new UnmodifiablePlan(plan);
 	}
 
 	static class UnmodifiablePlan implements Plan {
 		private final Plan delegate;
 		private final List<PlanElement> unmodifiablePlanElements;
 
 		public UnmodifiablePlan( Plan plan ) {
 			this.delegate = plan;
 			List<PlanElement> tmp = new ArrayList<>() ;
 			for ( PlanElement pe : plan.getPlanElements() ) {
 				if (pe instanceof Activity) {
 					tmp.add(unmodifiableActivity((Activity) pe));
 				} else if (pe instanceof Leg) {
 					tmp.add(unmodifiableLeg((Leg) pe));
 				}
 			}
 			this.unmodifiablePlanElements = Collections.unmodifiableList(tmp);
 		}
 
 		@Override
 		public void addActivity(Activity act) {
 			throw new UnsupportedOperationException() ;
 		}
 
 		@Override
 		public String getType() {
 			return delegate.getType();
 		}
 
 		@Override
 		public void setType(String type) {
 			throw new UnsupportedOperationException();
 		}
 
 		@Override
 		public void addLeg(Leg leg) {
 			throw new UnsupportedOperationException() ;
 		}
 
 		@Override
 		public Map<String, Object> getCustomAttributes() {
 			return delegate.getCustomAttributes();
 		}
 
 		@Override
 		public Person getPerson() {
 			return delegate.getPerson();
 		}
 
 		@Override
 		public List<PlanElement> getPlanElements() {
 			return this.unmodifiablePlanElements;
 		}
 
 		@Override
 		public Double getScore() {
 			return delegate.getScore();
 		}
 
 		@Override
 		public void setPerson(Person person) {
 			throw new UnsupportedOperationException() ;
 		}
 
 		@Override
 		public void setScore(Double score) {
 			throw new UnsupportedOperationException() ;
 		}
 
 		@Override
 		public Attributes getAttributes() {
 			// TODO yyyy should be made unmodifiable.  kai, jan'17
 			return delegate.getAttributes() ;
 		}
 	}
 
 	/**
 	 * @return sorted map containing containing the persons as values and their ids as keys.
 	 */
 	public static SortedMap<Id<Person>, Person> getSortedPersons(final Population population) {
 		return new TreeMap<>(population.getPersons());
 	}
 
 	/**
 	 * Sorts the persons in the given population.
 	 */
 	@SuppressWarnings("unchecked")
 	public static void sortPersons(final Population population) {
 		Map<Id<Person>, Person> map = (Map<Id<Person>, Person>) population.getPersons();
 
 		if (map instanceof SortedMap) return;
 
 		Map<Id<Person>, Person> treeMap = new TreeMap<>(map);
 		map.clear();
 		map.putAll(treeMap);
 	}
 
 	/**
 	 * @deprecated Use {@link #decideOnActivityEndTime(Activity, double, Config)}
 	 */
 	@Deprecated // was renamed
 	public static double getActivityEndTime( Activity act, double now, Config config ) {
 		return decideOnActivityEndTime( act, now, config ).seconds() ;
 	}
 
 
/** The end time is computed based on the configured time interpretation. */
 public static OptionalTime decideOnActivityEndTime(Activity act, double now, Config config){}

 

}