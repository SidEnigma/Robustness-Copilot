/* *********************************************************************** *
  * project: org.matsim.*
  * MatsimRandom.java
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
 
 package org.matsim.core.gbl;
 
 import java.util.Random;
 
 import org.apache.log4j.Logger;
 
 
 /**
  * An abstract class, providing random numbers for MATSim. Also provides
  * Random Number Generators (RNG) for use in threads, which should all
  * use their own RNGs for deterministic behavior.
  *
  * @author mrieser
  */
 public abstract class MatsimRandom {
 	private static final Logger log = Logger.getLogger( MatsimRandom.class ) ;
 
 	private static final class InstrumentedRandom extends Random {
 		InstrumentedRandom(long defaultRandomSeed) {
 			super(defaultRandomSeed) ;
 		}
 		private long cntDbl = 0 ;
 		@Override public double nextDouble() {
 			cntDbl++ ; 
 			return super.nextDouble() ;
 		}
 		long numberOfDrawnDoubles() {
 			return cntDbl ;
 		}
 	}
 
 	private static final long DEFAULT_RANDOM_SEED = 4711;
 
 	private static long lastUsedSeed = DEFAULT_RANDOM_SEED;
 	private static int internalCounter = 0;
 
 	/** the global random number generator */
 	private static final Random random = new Random(DEFAULT_RANDOM_SEED);
 //	private static final Random random = new InstrumentedRandom(DEFAULT_RANDOM_SEED);
 
 	/** Resets the random number generator with a default random seed. */
 	public static void reset() {
 		reset(DEFAULT_RANDOM_SEED);
 	}
 
 	/** Resets the random number generator with the given seed.
 	 *
 	 * @param seed The seed used to draw random numbers.
 	 */
 	public static void reset(final long seed) {
 		lastUsedSeed = seed;
 		internalCounter = 0;
 		getRandom().setSeed(seed);
 //		prepareRNG(random);
 	}
 	public static Random getRandom() {
 		return random;
 	}
 
 	/** Returns an instance of a random number generator, which can be used
 	 * locally, e.g. in threads.
 	 *
 	 * @return pseudo random number generator
 	 */
 	public static Random getLocalInstance() {
 		internalCounter++;
 		Random r = new Random(lastUsedSeed + internalCounter*23l);
 		prepareRNG(r);
 		return r;
 	}
 
 
/** Draw random numbers to better initialize the pseudo-random number generator. */
 private static void prepareRNG(final Random rng){
	 		// draw random numbers to better initialize the pseudo-random number generator
 		for (int i = 0; i < 100; i++) {
 			rng.nextDouble();
 		}
 	}
 
 	/** Returns a random number between 0.0 (inclusive) and 1.0 (exclusive). */
 	public static double nextDouble() {
 		return random.nextDouble();
 	}
 
 	/** Returns a random number between 0.0 (inclusive) and the given upper bound (exclusive). */
 	public static double nextDouble(final double bound) {
 		return random.nextDouble() * bound;
 	}
 
 	/** Returns a random number between 0 (inclusive) and the given upper bound (exclusive). */
 	public static int nextInt(final int bound) {
 		return random.nextInt(bound);
 	}
 
 	/** Returns a random number between 0 (inclusive) and the given upper bound (exclusive). */
 	public static long nextLong(final long bound) {
 		return random.nextLong() % bound;
 	}
 
 	/** Returns a random number between 0 (inclusive) and the given upper bound (exclusive). */
 	public static int nextInt(final int from, final int to) {
 		return from + random.nextInt(to - from);
 	}
 
 	/** Returns a random number between 0 (inclusive) and the given upper bound (exclusive). */
 	public static long nextLong(final long from, final long to) {
 		return from + random.nextLong() % (to - from);
 	}
 
 	/** Returns a random number between 0 (inclusive) and the given upper bound (exclusive). */
 	public static double nextDouble(final double from, final double to) {
 		return from + random.nextDouble() * (to - from);
 	}
 
 	/** Returns a random number between 0 (inclusive) and the given upper bound (exclusive). */
 	public static double nextDouble		
 }

         
 	public static final void printRNGState(String label) {
 		if ( random instanceof InstrumentedRandom ) {
 			log.warn( "label=" + label + ";\tnumber of doubles draws = " + ((InstrumentedRandom) random).numberOfDrawnDoubles() ) ;
 		}
 		log.warn("label=" + label + ";\tnumber of local instances =" + internalCounter );
 	}
 
 }
