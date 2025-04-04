/* *********************************************************************** *
  * project: org.matsim.*
  * StrategyManager.java
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2007, 2009 by the members listed in the COPYING,  *
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
 
 package org.matsim.core.replanning;
 
 import org.apache.log4j.Logger;
 import org.matsim.api.core.v01.population.Person;
 import org.matsim.api.core.v01.population.Plan;
 import org.matsim.api.core.v01.population.Population;
 import org.matsim.core.api.internal.MatsimManager;
 import org.matsim.core.config.groups.ControlerConfigGroup;
 import org.matsim.core.config.groups.PlansConfigGroup;
 import org.matsim.core.config.groups.StrategyConfigGroup;
 import org.matsim.core.replanning.selectors.PlanSelector;
 import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;
 
 import javax.inject.Inject;
 import javax.inject.Singleton;
 import java.util.List;
 import java.util.Map;
 
 /**
  * Manages and applies strategies to agents for re-planning.
  *
  * @author mrieser
  * @author kai
  */
 @Singleton
 public class StrategyManager implements MatsimManager {
 
 	private static final Logger log = Logger.getLogger(StrategyManager.class);
 
 	private final GenericStrategyManager<Plan, Person> delegate;
 
 	@Inject
 	StrategyManager(StrategyConfigGroup strategyConfigGroup, PlansConfigGroup plansConfigGroup,
 					ControlerConfigGroup controlerConfigGroup,
 					Map<StrategyConfigGroup.StrategySettings, PlanStrategy> planStrategies) {
 
 		this();
 		setMaxPlansPerAgent(strategyConfigGroup.getMaxAgentPlanMemorySize());
 
 		int globalInnovationDisableAfter = (int) ((controlerConfigGroup.getLastIteration() - controlerConfigGroup.getFirstIteration())
 				* strategyConfigGroup.getFractionOfIterationsToDisableInnovation() + controlerConfigGroup.getFirstIteration());
 		log.info("global innovation switch off after iteration: " + globalInnovationDisableAfter);
 
 //		setSubpopulationAttributeName(plansConfigGroup.getSubpopulationAttributeName());
 		for (Map.Entry<StrategyConfigGroup.StrategySettings, PlanStrategy> entry : planStrategies.entrySet()) {
 			PlanStrategy strategy = entry.getValue();
 			StrategyConfigGroup.StrategySettings settings = entry.getKey();
 			addStrategy(strategy, settings.getSubpopulation(), settings.getWeight());
 
 			// now check if this modules should be disabled after some iterations
 			int maxIter = settings.getDisableAfter();
 			if ( maxIter > globalInnovationDisableAfter || maxIter==-1 ) {
 				if (!ReplanningUtils.isOnlySelector(strategy)) {
 					maxIter = globalInnovationDisableAfter ;
 				}
 			}
 
 			if (maxIter >= 0) {
 				if (maxIter >= controlerConfigGroup.getFirstIteration()) {
 					addChangeRequest(maxIter + 1, strategy, settings.getSubpopulation(), 0.0);
 				} else {
 					/* The services starts at a later iteration than this change request is scheduled for.
 					 * make the change right now.					 */
 					changeWeightOfStrategy(strategy, settings.getSubpopulation(), 0.0);
 				}
 			}
 		}
 	}
 
 	public StrategyManager() {
 		this.delegate = new GenericStrategyManager<>();
 	}
 
 //	/**
 //	 * @param name the name of the subpopulation attribute
 //	 * in the person's object attributes.
 //	 */
 //	public final void setSubpopulationAttributeName(final String name) {
 //		delegate.setSubpopulationAttributeName(name);
 //	}
 
 	@Deprecated
 	public final void addStrategyForDefaultSubpopulation(
 			final PlanStrategy strategy,
 			final double weight) {
 		addStrategy(strategy, null, weight);
 	}
 
 
/** Allows a strategy to be added to this manager with the specified weight.  The probability of this strategy being used for an agent is defined   by this weight relative to the sum of the weights of all strategies in this manager. */
 public final void addStrategy(final PlanStrategy strategy, final String subpopulation, final double weight){
	 	delegate.addStrategy(strategy, subpopulation, weight);		
 }

 

}