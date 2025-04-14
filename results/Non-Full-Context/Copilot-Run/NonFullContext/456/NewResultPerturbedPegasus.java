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
 
 
/** The config-group that is added to this config-instance is also known as a "core modules" group. */

public void addCoreModules() {
    if (locked) {
        throw new IllegalStateException("Config is locked. Cannot add core modules.");
    }

    // Add your core modules here
    modules.put("charyparNagelScoring", charyparNagelScoring);
    modules.put("controler", controler);
    modules.put("counts", counts);
    modules.put("facilities", facilities);
    modules.put("global", global);
    modules.put("households", households);
    modules.put("network", network);
    modules.put("parallelEventHandling", parallelEventHandling);
    modules.put("plansCalcRoute", plansCalcRoute);
    modules.put("plans", plans);
    modules.put("qSimConfigGroup", qSimConfigGroup);
    modules.put("scenarioConfigGroup", scenarioConfigGroup);
    modules.put("strategy", strategy);
    modules.put("transit", transit);
    modules.put("transitRouter", transitRouter);
    modules.put("linkStats", linkStats);
    modules.put("vspExperimentalGroup", vspExperimentalGroup);
    modules.put("timeAllocationMutator", timeAllocationMutator);
    modules.put("subtourModeChoice", subtourModeChoice);
    modules.put("travelTimeCalculatorConfigGroup", travelTimeCalculatorConfigGroup);
    modules.put("ptCounts", ptCounts);
    modules.put("vehicles", vehicles);
    modules.put("changeMode", changeMode);
    modules.put("jdeqSim", jdeqSim);
    modules.put("hermes", hermes);

    // Add consistency checkers for the core modules
    consistencyCheckers.add(new BeanValidationConfigConsistencyChecker());
    consistencyCheckers.add(new UnmaterializedConfigGroupChecker());
    consistencyCheckers.add(new VspConfigConsistencyCheckerImpl());

    log.info("Added core modules to the config.");
}
 

}