/* *********************************************************************** *
  * project: org.matsim.*
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
 
 package org.matsim.contrib.drt.optimizer.insertion;
 
 import java.util.function.DoubleSupplier;
 import java.util.function.ToDoubleFunction;
 
 import javax.annotation.Nullable;
 
 import org.matsim.contrib.drt.optimizer.VehicleEntry;
 import org.matsim.contrib.drt.optimizer.Waypoint;
 import org.matsim.contrib.drt.passenger.DrtRequest;
 import org.matsim.contrib.drt.run.DrtConfigGroup;
 import org.matsim.contrib.drt.schedule.DrtStayTask;
 import org.matsim.contrib.dvrp.schedule.Schedules;
 import org.matsim.core.mobsim.framework.MobsimTimer;
 
 import com.google.common.annotations.VisibleForTesting;
 
 /**
  * @author michalm
  */
 public class InsertionCostCalculator<D> {
 	public static class DetourTimeInfo {
 		// expected departure time for the new request
 		public final double departureTime;
 		// expected arrival time for the new request
 		public final double arrivalTime;
 		// time delay of each stop placed after the pickup insertion point
 		public final double pickupTimeLoss;
 		// ADDITIONAL time delay of each stop placed after the dropoff insertion point
 		public final double dropoffTimeLoss;
 
 		public DetourTimeInfo(double departureTime, double arrivalTime, double pickupTimeLoss, double dropoffTimeLoss) {
 			this.departureTime = departureTime;
 			this.arrivalTime = arrivalTime;
 			this.pickupTimeLoss = pickupTimeLoss;
 			this.dropoffTimeLoss = dropoffTimeLoss;
 		}
 
 		// TOTAL time delay of each stop placed after the dropoff insertion point
 		// (this is the amount of extra time the vehicle will operate if this insertion is applied)
 		public double getTotalTimeLoss() {
 			return pickupTimeLoss + dropoffTimeLoss;
 		}
 	}
 
 	public static final double INFEASIBLE_SOLUTION_COST = Double.POSITIVE_INFINITY;
 
 	private final DoubleSupplier timeOfDay;
 	private final CostCalculationStrategy costCalculationStrategy;
 	private final InsertionDetourTimeCalculator<D> detourTimeCalculator;
 
 	public InsertionCostCalculator(DrtConfigGroup drtConfig, MobsimTimer timer,
 			CostCalculationStrategy costCalculationStrategy, ToDoubleFunction<D> detourTime,
 			@Nullable DetourTimeEstimator replacedDriveTimeEstimator) {
 		this(timer::getTimeOfDay, costCalculationStrategy,
 				new InsertionDetourTimeCalculator<>(drtConfig.getStopDuration(), detourTime,
 						replacedDriveTimeEstimator));
 	}
 
 	@VisibleForTesting
 	InsertionCostCalculator(DoubleSupplier timeOfDay, CostCalculationStrategy costCalculationStrategy,
 			InsertionDetourTimeCalculator<D> detourTimeCalculator) {
 		this.timeOfDay = timeOfDay;
 		this.costCalculationStrategy = costCalculationStrategy;
 		this.detourTimeCalculator = detourTimeCalculator;
 	}
 
 
/** As the main goal is to minimise bus operation time, this method calculates how much longer the bus will operate  after insertion. */
 public double calculate(DrtRequest drtRequest, InsertionWithDetourData<D> insertion){}

 

}