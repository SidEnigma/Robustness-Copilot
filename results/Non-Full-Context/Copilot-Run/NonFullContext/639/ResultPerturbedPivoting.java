/* *********************************************************************** *
  * project: org.matsim.*
  * LegHistogram.java
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
 
 package org.matsim.analysis;
 
 import org.matsim.api.core.v01.Id;
 import org.matsim.api.core.v01.events.PersonArrivalEvent;
 import org.matsim.api.core.v01.events.PersonDepartureEvent;
 import org.matsim.api.core.v01.events.PersonStuckEvent;
 import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
 import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
 import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
 import org.matsim.api.core.v01.population.Person;
 import org.matsim.api.core.v01.population.Population;
 import org.matsim.core.api.experimental.events.EventsManager;
 import org.matsim.core.utils.io.IOUtils;
 import org.matsim.core.utils.io.UncheckedIOException;
 import org.matsim.core.utils.misc.Time;
 
 import javax.inject.Inject;
 
 import java.io.IOException;
 import java.io.OutputStream;
 import java.io.PrintStream;
 import java.util.Map;
 import java.util.Set;
 import java.util.TreeMap;
 
 /**
  * @author mrieser
  *
  * Counts the number of persons departed, arrived or got stuck per time bin
  * based on events.
  *
  * The chart plotting was moved to its own class.
  * This class could be moved to trafficmonitoring.
  *
  */
 public class LegHistogram implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler {
 
 	private Set<Id<Person>> personIds;
 	private int iteration = 0;
 	private final int binSize;
 	private final int nofBins;
 	private final Map<String, DataFrame> data = new TreeMap<>();
 
 	@Inject
 	LegHistogram(Population population, EventsManager eventsManager) {
 		this(300);
 		if (population == null) {
 			this.personIds = null;
 		} else {
 			this.personIds = population.getPersons().keySet();
 		}
 		eventsManager.addHandler(this);
 	}
 
 	/**
 	 * Creates a new LegHistogram with the specified binSize and the specified number of bins.
 	 *
 	 * @param binSize The size of a time bin in seconds.
 	 * @param nofBins The number of time bins for this analysis.
 	 */
 	public LegHistogram(final int binSize, final int nofBins) {
 		super();
 		this.binSize = binSize;
 		this.nofBins = nofBins;
 		reset(0);
 	}
 
 	/** Creates a new LegHistogram with the specified binSize and a default number of bins, such
 	 * that 30 hours are analyzed.
 	 *
 	 * @param binSize The size of a time bin in seconds.
 	 */
 	public LegHistogram(final int binSize) {
 		this(binSize, 30*3600/binSize + 1);
 	}
 
 	/* Implementation of EventHandler-Interfaces */
 
 	@Override
 	public void handleEvent(final PersonDepartureEvent event) {
 		int index = getBinIndex(event.getTime());
 		if ((this.personIds == null || this.personIds.contains(event.getPersonId())) && event.getLegMode() != null) {
 			DataFrame dataFrame = getDataForMode(event.getLegMode());
 			dataFrame.countsDep[index]++;
 		}
 	}
 
 	@Override
 	public void handleEvent(final PersonArrivalEvent event) {
 		int index = getBinIndex(event.getTime());
 		if ((this.personIds == null || this.personIds.contains(event.getPersonId())) && event.getLegMode() != null) {
 			DataFrame dataFrame = getDataForMode(event.getLegMode());
 			dataFrame.countsArr[index]++;
 		}
 	}
 
 	@Override
 	public void handleEvent(final PersonStuckEvent event) {
 		int index = getBinIndex(event.getTime());
 		if ((this.personIds == null || this.personIds.contains(event.getPersonId())) && event.getLegMode() != null) {
 			DataFrame dataFrame = getDataForMode(event.getLegMode());
 			dataFrame.countsStuck[index]++;
 		}
 	}
 
 	@Override
 	public void reset(final int iter) {
 		this.iteration = iter;
 		this.data.clear();
 	}
 
 
/** Writes the collected data separated by tabs to a text file. */
 public void write(final String filename){
	 	try {
 		OutputStream out = IOUtils.getOutputStream(filename);
 		PrintStream ps = new PrintStream(out);
 		ps.println("# iteration=" + this.iteration);
 		ps.println("# binSize=" + this.binSize);
 		ps.println("# nofBins=" + this.nofBins);
 		ps.println("# mode\tdep\tarr\tstuck");
 		for (String mode : this.data.keySet()) {
 			DataFrame dataFrame = this.data.get(mode);
 			ps.print(mode);
 			for (int i = 0; i < this.nofBins; i++) {
 				ps.print("\t" + dataFrame.countsDep[i]);
 				ps.print("\t" + dataFrame.countsArr[i]);
 				ps.print("\t" + dataFrame.countsStuck[i]);
 			}
 			ps.println();
 		}
 		ps.close();
 	} catch (IOException e) {
 		throw new UncheckedIOException(e);
 	}
 }
 
 	private int getBinIndex(final double time) {
 		return (int) Math.floor(time / this.binSize);
 	}
 
 	private DataFrame getDataForMode(final String mode) {
 		DataFrame dataFrame = this.data.get(mode);
 		if (dataFrame == null) {
 			dataFrame = new DataFrame();
 			this.data.put(mode, dataFrame);
 		}
 		return dataFrame;
 	}
 
 	private static class DataFrame {
 		private int[] countsDep = new int[LegHistogram.nofBins];
 		private int[] countsArr = new int[LegHistogram.nof		
 }

 

}