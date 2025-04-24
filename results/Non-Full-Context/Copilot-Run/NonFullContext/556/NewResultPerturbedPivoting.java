package org.matsim.contrib.analysis.spatial;
 
 import org.apache.commons.math3.special.Erf;
 import org.locationtech.jts.geom.Coordinate;
 
 public abstract class SpatialInterpolation {
 
 
/** This uses Gaussian distance weighting to calculate the impact of emissions based on the bond on the centroid of a grid cell. */

    public static double calculateWeightFromLine(final Coordinate from, final Coordinate to, final Coordinate cellCentroid, final double smoothingRadius) {
        double distance = calculateDistance(from, to, cellCentroid);
        double weight = calculateGaussianWeight(distance, smoothingRadius);
        return weight;
    }
 

}