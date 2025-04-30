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
 
 
/** Changes to another plan with a probability that is proportional to exp. */
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
 		return null;
 	}
 	double minScore = Double.POSITIVE_INFINITY;
 	T worstPlan = null;
 	for (T plan : person.getPlans()) {
 		if (plan.getScore() < minScore) {
 			minScore = plan.getScore();
 			worstPlan = plan;
 		}
 	}
 	if (worstPlan == null) {
 		return null;
 	}
 	double scoreDiff = maxScore - minScore;
 	if (scoreDiff == 0) {
 		return null;
 	}
 	double p = Math.exp(beta * (bestPlan.getScore() - worstPlan.getScore()));
 	if (MatsimRandom.getRandom().nextDouble() < p) {
 		return worstPlan;
 	} else {
 		return null;
 	}
 }
 
 	public static void warnAboutBeta(double beta) {
 		if (betaWrnFlag) {
 			log.warn("The beta parameter of the ExpBetaPlanChanger is set to " + beta + ". This is not a good idea, as it will lead to a lot of " +
 					"switching. The best bete is 0.5.");
 			betaWrnFlag = false;
 		}
 	}
 
 	public static void warnAboutScore(double score) {
 		if (scoreWrnFlag)   
 }

 

}