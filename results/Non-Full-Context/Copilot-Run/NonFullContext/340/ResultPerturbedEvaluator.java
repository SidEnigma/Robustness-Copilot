
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
 public static List<Link> findIntersectingLinks(Link link, final Network network){
        List<Link> intersectingLinks = new ArrayList<>();
        for(Link l : network.getLinks().values()){
            if(l.getGeometry().intersects(link.getGeometry())){
                intersectingLinks.add(l);
            }
        }
        return intersectingLinks;       
 }

 

}