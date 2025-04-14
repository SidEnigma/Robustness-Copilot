/* *********************************************************************** *
  * project: org.matsim.*
  * ScenarioLoader
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
 package org.matsim.core.scenario;
 
 import com.google.inject.Inject;
 import org.apache.log4j.Logger;
 import org.matsim.api.core.v01.Identifiable;
 import org.matsim.api.core.v01.Scenario;
 import org.matsim.api.core.v01.network.Network;
 import org.matsim.core.config.Config;
 import org.matsim.core.config.ConfigGroup;
 import org.matsim.core.config.groups.FacilitiesConfigGroup;
 import org.matsim.core.config.groups.HouseholdsConfigGroup;
 import org.matsim.core.network.NetworkChangeEvent;
 import org.matsim.core.network.NetworkUtils;
 import org.matsim.core.network.io.MatsimNetworkReader;
 import org.matsim.core.network.io.NetworkChangeEventsParser;
 import org.matsim.core.population.PopulationUtils;
 import org.matsim.core.population.io.PopulationReader;
 import org.matsim.core.utils.io.IOUtils;
 import org.matsim.core.utils.io.UncheckedIOException;
 import org.matsim.facilities.MatsimFacilitiesReader;
 import org.matsim.households.HouseholdsReaderV10;
 import org.matsim.lanes.LanesReader;
 import org.matsim.pt.config.TransitConfigGroup;
 import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
 import org.matsim.utils.objectattributes.AttributeConverter;
 import org.matsim.utils.objectattributes.ObjectAttributes;
 import org.matsim.utils.objectattributes.ObjectAttributesUtils;
 import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
 import org.matsim.utils.objectattributes.attributable.Attributable;
 import org.matsim.vehicles.MatsimVehicleReader;
 
 
 import java.net.URL;
 import java.util.*;
 
 import static org.matsim.core.config.groups.PlansConfigGroup.PERSON_ATTRIBUTES_DEPRECATION_MESSAGE;
 
 /**
  * Loads elements of Scenario from file. Non standardized elements
  * can also be loaded however they require a specific instance of
  * Scenario.
  * {@link #loadScenario()} reads the complete scenario from files while the
  * other load...() methods only load specific parts
  * of the scenario assuming that required parts are already
  * loaded or created by the user.
  * <p></p>
  * Design thoughts:<ul>
  * <li> Given what we have now, does it make sense to leave this class public?  yy kai, mar'11
  * </ul>
  *
  * @see org.matsim.core.scenario.MutableScenario
  *
  * @author dgrether
  */
 // deliberately non-public.  Use method in ScenarioUtils.
 class ScenarioLoaderImpl {
 
 	private static final Logger log = Logger.getLogger(ScenarioLoaderImpl.class);
 
 	private final Config config;
 
 	private final MutableScenario scenario;
 
 	private Map<Class<?>, AttributeConverter<?>> attributeConverters = Collections.emptyMap();
 
 	@Inject
 	public void setAttributeConverters(Map<Class<?>, AttributeConverter<?>> attributeConverters) {
 		log.debug( "setting "+attributeConverters );
 		this.attributeConverters = attributeConverters;
 	}
 
 	ScenarioLoaderImpl(Config config) {
 		this.config = config;
 		this.scenario = (MutableScenario) ScenarioUtils.createScenario(this.config);
 	}
 
 	ScenarioLoaderImpl(Scenario scenario) {
 		this.scenario = (MutableScenario) scenario;
 		this.config = this.scenario.getConfig();
 	}
 
 
/** Loads all mandatory Scenario elements and  if activated in config's scenario module/group  optional elements. */
 Scenario loadScenario(){}

 

}