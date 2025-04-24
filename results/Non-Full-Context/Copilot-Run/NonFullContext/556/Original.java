package org.matsim.contrib.analysis.spatial;
 
 import org.apache.commons.math3.special.Erf;
 import org.locationtech.jts.geom.Coordinate;
 
 public abstract class SpatialInterpolation {
 
 
/** This uses a gaussian distance weighting to calculate the impact of link based emissions onto the centroid of a  grid cell. */
 public static double calculateWeightFromLine(final Coordinate from, final Coordinate to, final Coordinate cellCentroid, final double smoothingRadius){}

 

}