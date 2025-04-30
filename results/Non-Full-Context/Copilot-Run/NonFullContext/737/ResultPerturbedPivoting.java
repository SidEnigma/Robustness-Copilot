/* *********************************************************************** *
  * project: org.matsim.*
  * ExpBetaPlanChanger.java
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
 
 package org.matsim.core.replanning.selectors;
 
 import org.apache.log4j.Logger;
 import org.matsim.api.core.v01.population.BasicPlan;
 import org.matsim.api.core.v01.population.HasPlansAndId;
 import org.matsim.core.gbl.MatsimRandom;
 
 /**
  * Changes to another plan if that plan is better.  Probability to change depends on score difference.
  *
  * @author kn based on mrieser
  */
 public final class ExpBetaPlanChanger<T extends BasicPlan, I> implements PlanSelector<T, I> {
 	private static final Logger log = Logger.getLogger(ExpBetaPlanChanger.class);
 
 	private final double beta;
 	static boolean betaWrnFlag = true ;
 	static boolean scoreWrnFlag = true ;
 
 	public ExpBetaPlanChanger(double beta) {
 		this.beta = beta;
 	}
 
 
/** Changes to another plane with probability proportional to exp( Delta) scores. */
 public T selectPlan(final HasPlansAndId<T, I> person){
   	T selectedPlan = null;
 	double maxScore = Double.NEGATIVE_INFINITY;
 	double minScore = Double.POSITIVE_INFINITY;
 	double scoreDiff = 0.0;
 	double expScoreDiff = 0.0;
 	double random = MatsimRandom.getRandom().nextDouble();
 	for (T plan : person.getPlans()) {
 		if (plan.getScore() > maxScore) {
 			maxScore = plan.getScore();
 		}
 		if (plan.getScore() < minScore) {
 			minScore = plan.getScore();
 		}
 	}
 	scoreDiff = maxScore - minScore;
 	expScoreDiff = Math.exp(beta * scoreDiff);
 	if (expScoreDiff < random) {
 		selectedPlan = person.getPlans().get(0);
 	} else {
 		for (T plan : person.getPlans()) {
 			expScoreDiff = Math.exp(beta * (plan.getScore() - minScore));
 			if (expScoreDiff > random) {
 				selectedPlan = plan;
 				break;
 			}
 		}
 	}
 	if (selectedPlan == null) {
 		if (betaWrnFlag) {
 			log.warn("No plan selected.  beta = " + beta + " scoreDiff = " + scoreDiff + " expScoreDiff = " + expScoreDiff + " random = " + random);
 			betaWrnFlag = false ;
 		}
 	}
 	return selectedPlan;
 }
    
 }

 

}