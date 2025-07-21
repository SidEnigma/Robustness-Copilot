/*
  * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
  * Copyright (C) 2011, 2013-2021 The JavaParser Team.
  *
  * This file is part of JavaParser.
  *
  * JavaParser can be used either under the terms of
  * a) the GNU Lesser General Public License as published by
  *     the Free Software Foundation, either version 3 of the License, or
  *     (at your option) any later version.
  * b) the terms of the Apache License
  *
  * You should have received a copy of both licenses in LICENCE.LGPL and
  * LICENCE.APACHE. Please refer to those files for details.
  *
  * JavaParser is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Lesser General Public License for more details.
  */
 
 package com.github.javaparser.generator.core.utils;
 
 import com.github.javaparser.ast.type.Type;
 
 public final class CodeUtils {
 
 	private CodeUtils() {
 		// This constructor is used to hide the public one
 	}
 
 
/** In case the current type does not match the required type it casts the value. In the variable conCast we have double as the required type and int as the value type. Whereas in the case where we have int as the required type and int as the value type the variable withoutCast does not need to be cast. */
 public static String castValue(String value, Type requiredType, String valueType){}

                        
 }
