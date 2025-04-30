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
 
 
/** Changes to another plan with a probability proportional to exp( Delta scores ). */
 public T selectPlan(final HasPlansAndId<T, I> person){
   	if (person.getPlans().size() < 2) {
 		return null;
 	}
 	double maxScore = Double.NEGATIVE_INFINITY;
 	T bestPlan = null;
 	for (T plan : person.getPlans()) {
 		if (plan.getScore() > maxScore) {
 			maxScore = plan.getScore();
 			bestPlan = plan;
 		}
 	}
 	if (bestPlan == null) {
 		log.error("No best plan found for person " + person.getId());
 		return null;
 	}
 	double sum = 0.0;
 	for (T plan : person.getPlans()) {
 		if (plan != bestPlan) {
 			double scoreDiff = plan.getScore() - bestPlan.getScore();
 			if (scoreDiff < 0) {
 				log.error("Score difference is negative for person " + person.getId() + ": " + scoreDiff);
 				if (scoreWrnFlag) {
 					scoreWrnFlag = false;
 					log.warn("Score difference is negative for person " + person.getId() + ": " + scoreDiff);
 				}
 			}
 			sum += Math.exp(beta * scoreDiff);
 		}
 	}
 	double rnd = MatsimRandom.getRandom().nextDouble() * sum;
 	double sum2 = 0.0;
 	for (T plan : person.getPlans()) {
 		if (plan != bestPlan) {
 			double scoreDiff = plan.getScore() - bestPlan.getScore();
 			if (scoreDiff < 0) {
 				log.error("Score difference is negative for person " + person.getId() + ": " + scoreDiff);
 				if (scoreWrnFlag) {   
 }

 

}