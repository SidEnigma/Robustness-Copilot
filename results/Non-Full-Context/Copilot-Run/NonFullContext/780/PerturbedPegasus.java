/* *********************************************************************** *
  * project: org.matsim.*
  * CoordUtils.java
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
 
 package org.matsim.core.utils.geometry;
 
 import org.apache.log4j.Logger;
 import org.locationtech.jts.geom.Coordinate;
 import org.matsim.api.core.v01.Coord;
 import org.matsim.core.gbl.Gbl;
 
 public abstract class CoordUtils {
 	final private static Logger LOG = Logger.getLogger(CoordUtils.class);
 	
 	public static Coordinate createGeotoolsCoordinate( final Coord coord ) {
 		return new Coordinate( coord.getX(), coord.getY() ) ;
 	}
 	
 	public static Coord createCoord( final Coordinate coordinate ) {
 		return new Coord( coordinate.x, coordinate.y ) ;
 	}
 	
 	public static Coord createCoord( final double xx, final double yy ) {
 		return new Coord(xx, yy);
 	}
 	
 	public static Coord createCoord( final double xx, final double yy, final double zz){
 		return new Coord(xx, yy, zz);
 	}
 	
 	public static Coord plus ( Coord coord1, Coord coord2 ) {
 		if( !coord1.hasZ() && !coord2.hasZ() ){
 			/* Both are 2D coordinates. */
 			double xx = coord1.getX() + coord2.getX();
 			double yy = coord1.getY() + coord2.getY();
 			return new Coord(xx, yy);			
 		} else if( coord1.hasZ() && coord2.hasZ() ){
 			/* Both are 3D coordinates. */
 			double xx = coord1.getX() + coord2.getX();
 			double yy = coord1.getY() + coord2.getY();
 			double zz = coord1.getZ() + coord2.getZ();
 			return new Coord(xx, yy, zz);			
 		} else{
 			throw new RuntimeException("Cannot 'plus' coordinates if one has elevation (z) and the other not; coord1.hasZ=" + coord1.hasZ()
 			+ "; coord2.hasZ=" + coord2.hasZ() );
 		}
 	}
 	
 	public static Coord minus ( Coord coord1, Coord coord2 ) {
 		if( !coord1.hasZ() && !coord2.hasZ() ){
 			/* Both are 2D coordinates. */
 			double xx = coord1.getX() - coord2.getX();
 			double yy = coord1.getY() - coord2.getY();
 			return new Coord(xx, yy);			
 		} else if( coord1.hasZ() && coord2.hasZ() ){
 			/* Both are 3D coordinates. */
 			double xx = coord1.getX() - coord2.getX();
 			double yy = coord1.getY() - coord2.getY();
 			double zz = coord1.getZ() - coord2.getZ();
 			return new Coord(xx, yy, zz);			
 		} else{
 			throw new RuntimeException("Cannot 'minus' coordinates if one has elevation (z) and the other not.");
 		}
 	}
 	
 	public static Coord scalarMult( double alpha, Coord coord ) {
 		if(!coord.hasZ()){
 			/* 2D coordinate. */
 			double xx = alpha * coord.getX();
 			double yy = alpha * coord.getY();
 			return new Coord(xx, yy);			
 		} else {
 			/* 3D coordinate. */
 			double xx = alpha * coord.getX();
 			double yy = alpha * coord.getY();
 			double zz = alpha * coord.getZ();
 			return new Coord(xx, yy, zz);			
 		} 
 	}
 	
 	
 	public static Coord getCenter( Coord coord1, Coord coord2 ) {
 		if( !coord1.hasZ() && !coord2.hasZ() ){
 			/* Both are 2D coordinates. */
 			double xx = 0.5*( coord1.getX() + coord2.getX() ) ;
 			double yy = 0.5*( coord1.getY() + coord2.getY() ) ;
 			return new Coord(xx, yy);			
 		} else if( coord1.hasZ() && coord2.hasZ() ){
 			/* Both are 3D coordinates. */
 			double xx = 0.5*( coord1.getX() + coord2.getX() ) ;
 			double yy = 0.5*( coord1.getY() + coord2.getY() ) ;
 			double zz = 0.5*( coord1.getZ() + coord2.getZ() ) ;
 			return new Coord(xx, yy, zz);			
 		} else{
 			throw new RuntimeException("Cannot get the center for coordinates if one has elevation (z) and the other not.");
 		}
 	}
 	
 	public static double length( Coord coord ) {
 		if(!coord.hasZ()){
 			return Math.sqrt( 
 					coord.getX()*coord.getX() + 
 					coord.getY()*coord.getY() ) ;
 		} else{
 			return Math.sqrt( 
 					coord.getX()*coord.getX() + 
 					coord.getY()*coord.getY() +
 					coord.getZ()*coord.getZ()) ;
 		}
 	}
 	
 	/**
 	 * Note: If the given {@link Coord} has elevation, it's elevation will stay 
 	 * the same (jjoubert, Sep '16). 
 	 * @param coord
 	 * @return
 	 */
 	public static Coord rotateToRight( Coord coord ) {
 		if( !coord.hasZ() ){
 			/* 2D coordinate */
 			final double y = -coord.getX();
 			return new Coord(coord.getY(), y);
 		} else{
 			/* 3D coordinate */
 			final double y = -coord.getX();
 			return new Coord(coord.getY(), y, coord.getZ());			
 		}
 	}
 
 	
 	public static Coord getCenterWOffset( Coord coord1, Coord coord2 ) {
 		if( !coord1.hasZ() && !coord2.hasZ() ){
 			/* Both are 2D coordinates. */
 			Coord fromTo = minus( coord2, coord1 ) ;
 			Coord offset = scalarMult( 0.1 , rotateToRight( fromTo ) ) ;
 			Coord centerWOffset = plus( getCenter( coord1, coord2 ) , offset ) ;
 			return centerWOffset ;
 		} else if( coord1.hasZ() && coord2.hasZ() ){
 			/* TODO Both are 3D coordinates. */
 			throw new RuntimeException("3D version not implemented.");
 		} else{
 			throw new RuntimeException("Cannot get the center for coordinates if one has elevation (z) and the other not.");
 		}
 	}
 
 	public static double calcEuclideanDistance(Coord coord, Coord other) {
 		/* Depending on the coordinate system that is used, determining the 
 		 * distance based on the euclidean distance will lead to wrong results. 
 		 * However, if the distance is not to large (<1km) this will be a usable 
 		 * distance estimation. Another comfortable way to calculate correct 
 		 * distances would be, to use the distance functions provided by 
 		 * geotools lib. May be we need to discuss what part of GIS functionality 
 		 * we should implement by our own and for what part we could use an 
 		 * existing GIS like geotools. We need to discuss this in terms of code 
 		 * robustness, performance and so on ... [gl] */
 		if( !coord.hasZ() && !other.hasZ() ){
 			/* Both are 2D coordinates. */
 			double xDiff = other.getX()-coord.getX();
 			double yDiff = other.getY()-coord.getY();
 			return Math.sqrt((xDiff*xDiff) + (yDiff*yDiff));
 		} else if( coord.hasZ() && other.hasZ() ){
 			/* Both are 3D coordinates. */
 			double xDiff = other.getX()-coord.getX();
 			double yDiff = other.getY()-coord.getY();
 			double zDiff = other.getZ()-coord.getZ();
 			return Math.sqrt((xDiff*xDiff) + (yDiff*yDiff) + (zDiff*zDiff));
 		} else{
 			LOG.warn("Mixed use of elevation in coordinates: " + coord.toString() + 
 					"; " + other.toString());
 			LOG.warn("Returning projected coordinate distance (using x and y components only)");
 			return calcProjectedEuclideanDistance(coord, other);
 		}
 	}
 
 
 
/** When only the x and y-components of the coordinates are used, the method is used. */
 public static double calcProjectedEuclideanDistance(Coord coord, Coord other){}

 

}