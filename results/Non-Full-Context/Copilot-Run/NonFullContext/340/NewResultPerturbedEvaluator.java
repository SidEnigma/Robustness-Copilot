
 /* *********************************************************************** *
  * project: org.matsim.*
  * GeometryUtils.java
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 
  /**
  * 
  */
 package org.matsim.core.utils.geometry;
 
 import java.util.ArrayList;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Random;
 
 import org.geotools.geometry.jts.GeometryBuilder;
 import org.locationtech.jts.geom.Coordinate;
 import org.locationtech.jts.geom.Geometry;
 import org.locationtech.jts.geom.GeometryFactory;
 import org.locationtech.jts.geom.LineString;
 import org.locationtech.jts.geom.Point;
 import org.locationtech.jts.geom.Polygon;
 import org.matsim.api.core.v01.Coord;
 import org.matsim.api.core.v01.network.Link;
 import org.matsim.api.core.v01.network.Network;
 import org.matsim.core.gbl.Gbl;
 import org.matsim.core.utils.geometry.geotools.MGC;
 import org.opengis.feature.simple.SimpleFeature;
 
 /**
  * @author kainagel
  *
  */
 public class GeometryUtils {
 	private GeometryUtils() {} // do not instantiate
 
 
/** The method, that uses MATSim objects, find all the links in the Netwok that intersect the given Link. */

public static List<Link> findIntersectingLinks(Link link, final Network network) {
    List<Link> intersectingLinks = new ArrayList<>();

    // Create a JTS LineString from the given Link's start and end coordinates
    Coordinate[] coordinates = new Coordinate[2];
    coordinates[0] = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
    coordinates[1] = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
    LineString lineString = new GeometryFactory().createLineString(coordinates);

    // Iterate over all the links in the network
    for (Link networkLink : network.getLinks().values()) {
        // Create a JTS LineString from the current network link's start and end coordinates
        Coordinate[] networkCoordinates = new Coordinate[2];
        networkCoordinates[0] = new Coordinate(networkLink.getFromNode().getCoord().getX(), networkLink.getFromNode().getCoord().getY());
        networkCoordinates[1] = new Coordinate(networkLink.getToNode().getCoord().getX(), networkLink.getToNode().getCoord().getY());
        LineString networkLineString = new GeometryFactory().createLineString(networkCoordinates);

        // Check if the two LineStrings intersect
        if (lineString.intersects(networkLineString)) {
            intersectingLinks.add(networkLink);
        }
    }

    return intersectingLinks;
}
 

}