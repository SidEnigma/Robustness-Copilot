/* *********************************************************************** *
  * project: org.matsim.*
  * Config.java
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
 
 package org.matsim.core.config;
 
 import org.apache.log4j.Logger;
 import org.matsim.core.api.internal.MatsimExtensionPoint;
 import org.matsim.core.config.consistency.BeanValidationConfigConsistencyChecker;
 import org.matsim.core.config.consistency.ConfigConsistencyChecker;
 import org.matsim.core.config.consistency.UnmaterializedConfigGroupChecker;
 import org.matsim.core.config.consistency.VspConfigConsistencyCheckerImpl;
 import org.matsim.core.config.groups.*;
 import org.matsim.core.mobsim.hermes.HermesConfigGroup;
 import org.matsim.core.mobsim.jdeqsim.JDEQSimConfigGroup;
 import org.matsim.pt.config.TransitConfigGroup;
 import org.matsim.pt.config.TransitRouterConfigGroup;
 import org.matsim.run.CreateFullConfig;
 
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.nio.file.Paths;
 import java.util.*;
 
 /**
  * Stores all configuration settings specified in a configuration file and
  * provides access to the settings at runtime.
  * 
  * @see CreateFullConfig
  *
  * @author mrieser
  */
 public final class Config implements MatsimExtensionPoint {
 
 	// ////////////////////////////////////////////////////////////////////
 	// member variables
 	// ////////////////////////////////////////////////////////////////////
 
 	/** Map of all config-groups known to this instance. */
 	private final TreeMap<String, ConfigGroup> modules = new TreeMap<>();
 
 	/*
 	 * the following members are for the direct access to the core config
 	 * groups.
 	 */
 
 	// config groups that are in org.matsim.core.config.groups:
 	private PlanCalcScoreConfigGroup charyparNagelScoring = null;
 	private ControlerConfigGroup controler = null;
 	private CountsConfigGroup counts = null;
 	private FacilitiesConfigGroup facilities = null;
 	private GlobalConfigGroup global = null;
 	private HouseholdsConfigGroup households;
 	private NetworkConfigGroup network = null;
 	private ParallelEventHandlingConfigGroup parallelEventHandling = null;
 	private PlansCalcRouteConfigGroup plansCalcRoute = null;
 	private PlansConfigGroup plans = null;
 	private QSimConfigGroup qSimConfigGroup = null;
 	private ScenarioConfigGroup scenarioConfigGroup = null;
 	private StrategyConfigGroup strategy = null;
 	private TransitConfigGroup transit = null;
 	private TransitRouterConfigGroup transitRouter = null;
 	private LinkStatsConfigGroup linkStats = null;
 	private VspExperimentalConfigGroup vspExperimentalGroup = null;
 	private TimeAllocationMutatorConfigGroup timeAllocationMutator = null;
 	private SubtourModeChoiceConfigGroup subtourModeChoice = null;
 	private TravelTimeCalculatorConfigGroup travelTimeCalculatorConfigGroup = null;
 	private PtCountsConfigGroup ptCounts = null;
 	private VehiclesConfigGroup vehicles = null ;
 	private ChangeModeConfigGroup changeMode = null;
 	private JDEQSimConfigGroup jdeqSim = null;
     private HermesConfigGroup hermes = null;
 
 	private final List<ConfigConsistencyChecker> consistencyCheckers = new ArrayList<>();
 
 	/** static Logger-instance. */
 	private static final Logger log = Logger.getLogger(Config.class);
 
 	private boolean locked = false;
 	private URL context;
 
 
 	// ////////////////////////////////////////////////////////////////////
 	// constructor
 	// ////////////////////////////////////////////////////////////////////
 
 	public Config() {
 		try {
 			URL currentDir = Paths.get("").toUri().toURL();
 			setContext(currentDir);
 		} catch (MalformedURLException e) {
 			throw new RuntimeException(e);
 		}
 	}
 
 
/** Adds all commonly used configuration groups, also known as "core modules," to this configuration instance. */
 public void addCoreModules(){
	 	addModule(new PlanCalcScoreConfigGroup());
 	addModule(new ControlerConfigGroup());
 	addModule(new CountsConfigGroup());
 	addModule(new FacilitiesConfigGroup());
 	addModule(new GlobalConfigGroup());
 	addModule(new HouseholdsConfigGroup());
 	addModule(new NetworkConfigGroup());
 	addModule(new ParallelEventHandlingConfigGroup());
 	addModule(new PlansCalcRouteConfigGroup());
 	addModule(new PlansConfigGroup());
 	addModule(new QSimConfigGroup());
 	addModule(new ScenarioConfigGroup());
 	addModule(new StrategyConfigGroup());
 	addModule(new TransitConfigGroup());
 	addModule(new TransitRouterConfigGroup());
 	addModule(new LinkStatsConfigGroup());
 	addModule(new VspExperimentalConfigGroup());
 	addModule(new TimeAllocationMutatorConfigGroup());
 	addModule(new SubtourModeChoiceConfigGroup());
 	addModule(new TravelTimeCalculatorConfigGroup());
 	addModule(new PtCountsConfigGroup());
 	addModule(new VehiclesConfigGroup());
 	addModule(new ChangeModeConfigGroup());
 	addModule(new JDEQSimConfigGroup());
 	addModule(new HermesConfigGroup());
 }
 
 	/**
 	 * Adds a configuration group to this configuration instance.
 	 * @param module The configuration group to add.
 	 */
 	public void addModule(ConfigGroup module) {
 		if (locked) {
 			throw new IllegalStateException("Cannot add modules to a locked configuration.");
 		}
 		if (modules.containsKey(module.getName())) {
 			throw new IllegalArgumentException("Module " + module.getName() + " already exists.");
 		}
 		modules.put(module.getName(), module);
 		module.setConfig(this);
 	}
 
 	/**
 	 * Returns the configuration group with the given name.		
 }

 

}