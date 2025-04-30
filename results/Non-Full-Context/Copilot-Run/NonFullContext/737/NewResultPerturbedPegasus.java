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

public T selectPlan(final HasPlansAndId<T, I> person) {
    T selectedPlan = person.getPlans().get(0); // Default to the first plan

    double maxScore = Double.NEGATIVE_INFINITY;
    double sumExpScores = 0.0;

    for (T plan : person.getPlans()) {
        double score = calculateScore(plan); // Calculate the score for each plan

        if (score > maxScore) {
            maxScore = score;
        }

        sumExpScores += Math.exp(beta * score); // Calculate the sum of exponential scores
    }

    double randomValue = MatsimRandom.getRandom().nextDouble() * sumExpScores;
    double cumulativeExpScore = 0.0;

    for (T plan : person.getPlans()) {
        double score = calculateScore(plan);
        cumulativeExpScore += Math.exp(beta * score);

        if (cumulativeExpScore >= randomValue) {
            selectedPlan = plan;
            break;
        }
    }

    return selectedPlan;
}
 

}