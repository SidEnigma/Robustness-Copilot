package org.matsim.contrib.analysis.spatial;
 
 import org.apache.commons.math3.special.Erf;
 import org.locationtech.jts.geom.Coordinate;
 
 public abstract class SpatialInterpolation {
 
 
/** Computes and returns the distance weighting concerning the impact of the link, based emission onto the centroid of a grid cell. */

    public static double calculateWeightFromLine(final Coordinate from, final Coordinate to, final Coordinate cellCentroid, final double smoothingRadius) {
        // Calculate the distance between the link and the centroid of the grid cell
        double distance = calculateDistance(from, to, cellCentroid);

        // Apply the distance weighting formula
        double weight = Erf.erf(distance / smoothingRadius);

        return weight;
    }
 

}