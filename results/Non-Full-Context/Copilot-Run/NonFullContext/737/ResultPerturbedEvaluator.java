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
 
 
/** This method changes to another plan with a probability proportional to exp scores */
 public T selectPlan(final HasPlansAndId<T, I> person){
  		double maxScore = Double.NEGATIVE_INFINITY;
  		T bestPlan = null;
  		for (T plan : person.getPlans()) {
  			double score = plan.getScore();
  			if (score > maxScore) {
  				maxScore = score;
  				bestPlan = plan;
  			}
  		}
  		if (bestPlan == null) {
  			log.error("No plan found for person " + person.getId());
  			return null;
  		}
  		double bestScore = bestPlan.getScore();
  		double sum = 0.0;
  		for (T plan : person.getPlans()) {
  			double score = plan.getScore();
  			double expScore = Math.exp(beta * (score - bestScore));
  			sum += expScore;
  		}
  		double rnd = MatsimRandom.getRandom().nextDouble() * sum;
  		double sum2 = 0.0;
  		for (T plan : person.getPlans()) {
  			double score = plan.getScore();
  			double expScore = Math.exp(beta * (score - bestScore));
  			sum2 += expScore;
  			if (sum2 >= rnd) {
  				return plan;
  			}
  		}
  		log.error("No plan found for person " + person.getId());
  		return null;
  	}
  
  	public static void warnAboutBeta(double beta) {
  		if (betaWrnFlag) {
  			log.warn("The beta parameter of the ExpBetaPlanChanger is " + beta + ". This is not a good choice. "
  					+ "It is recommended to use a value between 0.0 and 1.0.");
  			betaWrnFlag = false;    
 }

 

}