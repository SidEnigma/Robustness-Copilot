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
 
 
/** Choose a random plan of the person and flip it over. */

    public T selectPlan(final HasPlansAndId<T, I> person) {
        // Get the list of plans of the person
        List<T> plans = person.getPlans();

        // Check if the person has any plans
        if (plans.isEmpty()) {
            throw new IllegalArgumentException("Person does not have any plans.");
        }

        // Choose a random plan index
        int randomIndex = MatsimRandom.getRandom().nextInt(plans.size());

        // Get the selected plan
        T selectedPlan = plans.get(randomIndex);

        // Flip the selected plan over
        flipPlan(selectedPlan);

        return selectedPlan;
    }
 

}