/* *********************************************************************** *
  * project: org.matsim.*
  * RandomPlanSelector.java
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
 
 import org.matsim.api.core.v01.population.BasicPlan;
 import org.matsim.api.core.v01.population.HasPlansAndId;
 import org.matsim.core.gbl.MatsimRandom;
 
 
 /**
  * Select randomly one of the existing plans of the person.
  *
  * @author mrieser
  */
 public class RandomPlanSelector<T extends BasicPlan, I> implements PlanSelector<T, I> {
 
 
/** Select a random plan from the person and return it */

    public T selectPlan(final HasPlansAndId<T, I> person) {
        // Get the list of plans from the person
        List<T> plans = person.getPlans();

        // Check if the person has any plans
        if (plans.isEmpty()) {
            throw new IllegalArgumentException("Person does not have any plans.");
        }

        // Generate a random index to select a plan
        int randomIndex = MatsimRandom.getRandom().nextInt(plans.size());

        // Return the randomly selected plan
        return plans.get(randomIndex);
    }
 

}