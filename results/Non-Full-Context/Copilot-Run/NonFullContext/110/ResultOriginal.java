
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
 
 
/** Adds the mappings from "from" to "to". */
 public static void copyTo(Attributes from, Attributes to){
     	for (String key : from.getAttributeKeys()) {
     		to.putAttribute(key, from.getAttribute(key));
     	}       
 }

 

}