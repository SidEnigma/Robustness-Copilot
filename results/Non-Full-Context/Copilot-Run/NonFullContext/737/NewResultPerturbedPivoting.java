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

public T selectPlan(final HasPlansAndId<T, I> person) {
    T selectedPlan = person.getPlans().get(0); // Default to the first plan

    double maxScore = Double.NEGATIVE_INFINITY;
    double sumExpDeltaScores = 0.0;

    for (T plan : person.getPlans()) {
        double deltaScore = calculateDeltaScore(plan);
        double expDeltaScore = Math.exp(beta * deltaScore);
        sumExpDeltaScores += expDeltaScore;

        if (expDeltaScore > maxScore) {
            maxScore = expDeltaScore;
            selectedPlan = plan;
        }
    }

    // Normalize the probabilities
    for (T plan : person.getPlans()) {
        double deltaScore = calculateDeltaScore(plan);
        double expDeltaScore = Math.exp(beta * deltaScore);
        double probability = expDeltaScore / sumExpDeltaScores;

        if (MatsimRandom.getRandom().nextDouble() < probability) {
            selectedPlan = plan;
        }
    }

    return selectedPlan;
}
 

}