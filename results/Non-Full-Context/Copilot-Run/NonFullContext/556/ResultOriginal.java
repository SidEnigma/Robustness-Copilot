package org.matsim.contrib.analysis.spatial;
 
 import org.apache.commons.math3.special.Erf;
 import org.locationtech.jts.geom.Coordinate;
 
 public abstract class SpatialInterpolation {
 
 
/** This uses a gaussian distance weighting to calculate the impact of link based emissions onto the centroid of a  grid cell. */
 public static double calculateWeightFromLine(final Coordinate from, final Coordinate to, final Coordinate cellCentroid, final double smoothingRadius){
     	double weight = 0.0;
     	double distance = from.distance(to);
     	double distanceFromCellCentroid = cellCentroid.distance(from);
     	double distanceToCellCentroid = cellCentroid.distance(to);
     	double distanceFromTo = distanceFromCellCentroid + distanceToCellCentroid;
     	double distanceFromToSquared = distanceFromTo * distanceFromTo;
     	double distanceFromToCubed = distanceFromToSquared * distanceFromTo;
     	double distanceFromToQuart = distanceFromToCubed * distanceFromTo;
     	double distanceFromToQuint = distanceFromToQuart * distanceFromTo;
     	double distanceFromToSext = distanceFromToQuint * distanceFromTo;
     	double distanceFromToSept = distanceFromToSext * distanceFromTo;
     	double distanceFromToOct = distanceFromToSept * distanceFromTo;
     	double distanceFromToNov = distanceFromToOct * distanceFromTo;
     	double distanceFromToDec = distanceFromToNov * distanceFromTo;
     	double distanceFromToJan = distanceFromToDec * distanceFromTo;
     	double distanceFromToFeb = distanceFromToJan * distanceFromTo;
     	double distanceFromToMar = distanceFromToFeb * distanceFromTo;
     	double distanceFromToApr = distanceFromToMar * distanceFromTo;
     	double distanceFromToMay = distanceFromToApr * distanceFromTo;
     	double distanceFromToJun = distanceFromToMay * distanceFromTo;
     	double distanceFromToJul = distanceFromToJun * distanceFromTo;
     	double distanceFromToAug = distanceFromToJul * distanceFromTo;
     	double distanceFromToSep = distanceFromToAug * distanceFromTo;
     	double distanceFromToOct2 = distanceFromToSep * distanceFromTo;
     	double distanceFromToNov2 = distanceFromToOct2 * distanceFromTo;
     	double distanceFromToDec2 = distanceFromToNov2 * distanceFromTo;
     	double distanceFromToJan2 = distanceFromToDec2 * distanceFromTo;
     	double distanceFromToFeb2 =     
 }

 

}