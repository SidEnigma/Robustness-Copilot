
 /* *********************************************************************** *
  * project: org.matsim.*
  * AttributesUtils.java
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
 
  package org.matsim.utils.objectattributes.attributable;
 
 /**
  * @author thibautd
  */
 public class AttributesUtils {
 	public static final String ATTRIBUTES = "attributes";
 	public static final String ATTRIBUTE = "attribute";
 
 
/** The method in question allows mappings to be added from 'from' to 'to'. While for copying the objects themselves, nothing is done, which should be fine for 99.9% of Attribute (value object) use cases. */
 public static void copyTo(Attributes from, Attributes to){
     	to.getAttributes().putAll(from.getAttributes());        
 }

 

}