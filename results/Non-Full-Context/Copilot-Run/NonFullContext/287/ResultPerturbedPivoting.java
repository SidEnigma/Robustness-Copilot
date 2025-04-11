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
 
 	/**
 	 * Adds all the commonly used config-groups, also known as "core modules",
 	 * to this config-instance. This should be called before reading any
 	 * configuration from file.
 	 */
 	public void addCoreModules() {
 		this.global = new GlobalConfigGroup();
 		this.modules.put(GlobalConfigGroup.GROUP_NAME, this.global);
 
 		this.controler = new ControlerConfigGroup();
 		this.modules.put(ControlerConfigGroup.GROUP_NAME, this.controler);
 
 		this.qSimConfigGroup = new QSimConfigGroup();
 		this.modules.put(QSimConfigGroup.GROUP_NAME, this.qSimConfigGroup);
 
 		this.counts = new CountsConfigGroup();
 		this.modules.put(CountsConfigGroup.GROUP_NAME, this.counts);
 
 		this.charyparNagelScoring = new PlanCalcScoreConfigGroup();
 		this.modules.put(PlanCalcScoreConfigGroup.GROUP_NAME, this.charyparNagelScoring);
 
 		this.network = new NetworkConfigGroup();
 		this.modules.put(NetworkConfigGroup.GROUP_NAME, this.network);
 
 		this.plans = new PlansConfigGroup();
 		this.modules.put(PlansConfigGroup.GROUP_NAME, this.plans);
 
 		this.households = new HouseholdsConfigGroup();
 		this.modules.put(HouseholdsConfigGroup.GROUP_NAME, this.households);
 
 		this.parallelEventHandling = new ParallelEventHandlingConfigGroup();
 		this.modules.put(ParallelEventHandlingConfigGroup.GROUP_NAME, this.parallelEventHandling );
 
 		this.facilities = new FacilitiesConfigGroup();
 		this.modules.put(FacilitiesConfigGroup.GROUP_NAME, this.facilities);
 
 		this.strategy = new StrategyConfigGroup();
 		this.modules.put(StrategyConfigGroup.GROUP_NAME, this.strategy);
 
 		this.travelTimeCalculatorConfigGroup = new TravelTimeCalculatorConfigGroup();
 		this.modules.put(TravelTimeCalculatorConfigGroup.GROUPNAME, this.travelTimeCalculatorConfigGroup);
 
 		this.scenarioConfigGroup = new ScenarioConfigGroup();
 		this.modules.put(ScenarioConfigGroup.GROUP_NAME, this.scenarioConfigGroup);
 
 		this.plansCalcRoute = new PlansCalcRouteConfigGroup();
 		this.modules.put(PlansCalcRouteConfigGroup.GROUP_NAME, this.plansCalcRoute);
 
 		this.timeAllocationMutator = new TimeAllocationMutatorConfigGroup();
 		this.modules.put(TimeAllocationMutatorConfigGroup.GROUP_NAME, this.timeAllocationMutator );
 
 		this.vspExperimentalGroup = new VspExperimentalConfigGroup();
 		this.modules.put(VspExperimentalConfigGroup.GROUP_NAME, this.vspExperimentalGroup);
 		
 		this.ptCounts = new PtCountsConfigGroup();
 		this.modules.put(PtCountsConfigGroup.GROUP_NAME, this.ptCounts);
 
 		this.transit = new TransitConfigGroup();
 		this.modules.put(TransitConfigGroup.GROUP_NAME, this.transit);
 
 		this.linkStats = new LinkStatsConfigGroup();
 		this.modules.put(LinkStatsConfigGroup.GROUP_NAME, this.linkStats);
 
 		this.transitRouter = new TransitRouterConfigGroup();
 		this.modules.put(TransitRouterConfigGroup.GROUP_NAME, this.transitRouter);
 
 		this.subtourModeChoice = new SubtourModeChoiceConfigGroup();
 		this.modules.put( SubtourModeChoiceConfigGroup.GROUP_NAME , this.subtourModeChoice );
 		
 		this.vehicles = new VehiclesConfigGroup() ;
 		this.modules.put( VehiclesConfigGroup.GROUP_NAME , this.vehicles ) ;
 
 		this.changeMode = new ChangeModeConfigGroup();
 		this.modules.put(ChangeModeConfigGroup.CONFIG_MODULE, this.changeMode);
 
 		this.modules.put(ChangeLegModeConfigGroup.CONFIG_MODULE, new ChangeLegModeConfigGroup());
 		// only to provide error messages. kai, may'16
 
 		this.jdeqSim = new JDEQSimConfigGroup();
 		this.modules.put(JDEQSimConfigGroup.NAME, this.jdeqSim);
 
         this.hermes = new HermesConfigGroup();
         this.modules.put(HermesConfigGroup.NAME, this.hermes);
 
 
 		this.addConfigConsistencyChecker(new VspConfigConsistencyCheckerImpl());
 		this.addConfigConsistencyChecker(new UnmaterializedConfigGroupChecker());
 		this.addConfigConsistencyChecker(new BeanValidationConfigConsistencyChecker());
 	}
 
 	/**
 	 * Checks each module for consistency, e.g. if the parameters that are
 	 * currently set make sense in their combination.
 	 */
 	public void checkConsistency() {
 		for (ConfigGroup m : this.modules.values()) {
 			m.checkConsistency(this);
 		}
 		for (ConfigConsistencyChecker c : this.consistencyCheckers) {
 			c.checkConsistency(this);
 		}
 //        for (Module m : this.modules.values()) {
 //            if (m.getClass() == Module.class) {
 //                throw new RuntimeException("Config group " + m.getName() + " is present, but has never been read." +
 //                        "This is probably an error: You may be expecting functionality which is not available." +
 //                        "Maybe you need to add something to the Controler?");
 //            }
 //        }
 	}
 
 	// ////////////////////////////////////////////////////////////////////
 	// add / set methods
 	// ////////////////////////////////////////////////////////////////////
 
 	/**
 	 * Creates a new module / config-group with the specified name.
 	 *
 	 * @param name
 	 *            The name of the config-group to be created.
 	 *
 	 * @return the newly created config group
 	 * @throws IllegalArgumentException
 	 *             if a config-group with the specified name already exists.
 	 */
 	public final ConfigGroup createModule(final String name) {
 		if (this.modules.containsKey(name)) {
 			throw new IllegalArgumentException("Module " + name + " exists already.");
 		}
 		ConfigGroup m = new ConfigGroup(name);
 		this.modules.put(name, m);
 		return m;
 	}
 
 
/** Adds the specified configuration module/group with the specified name to the configuration. */
 public final void addModule(final ConfigGroup specializedConfigModule){
	 if (this.modules.containsKey(specializedConfigModule.getName())) {
		 throw new IllegalArgumentException("Module " + specializedConfigModule.getName() + " exists already.");
	 }
	 this.modules.put(specializedConfigModule.getName(), specializedConfigModule);
 }
 
 	/**
 	 * Adds the specified configuration module/group with the specified name to the configuration.
 	 *
 	 * @param name
 	 *            The name of the config-group to be added.
 	 * @param specializedConfigModule
 	 *            The config-group to be added.
 	 * @throws IllegalArgumentException
 	 *             if a config-group with the specified name already exists.
 	 */
 	public final void addModule(final String name, final ConfigGroup specializedConfigModule) {
 		if (this.modules.containsKey(name)) {
 			throw new IllegalArgumentException("Module " + name + " exists already.");
 		}
 		this.modules.put(name, specializedConfigModule);
 	}
 
 	/**
 	 * Adds the specified configuration module/group with the specified name to the configuration.
 	 *
 	 * @param name
 	 *            The name of the config-group to be added.
 	 * @param specializedConfigModule
 	 *            The config-group to be added.
 	 * @throws IllegalArgumentException
 	 *             if a config-group with the specified name already exists.
 	 */
 	public final void addModule(final String name, final Module specializedConfigModule) {
 		if (this.modules.containsKey(name)) {
 			throw new IllegalArgumentException("Module " + name + " exists already.");
 		}
 		this.modules.put(name, specializedConfigModule);
 	}
 
 	/**
 	 * Adds the specified configuration module/group with the specified name to the configuration.
 	 *
 	 * @param name
 	 *            The name of the config			
 }

 

}