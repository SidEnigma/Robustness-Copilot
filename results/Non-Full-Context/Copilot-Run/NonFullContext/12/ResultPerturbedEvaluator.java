/*
  *    Copyright 2009-2021 the original author or authors.
  *
  *    Licensed under the Apache License, Version 2.0 (the "License");
  *    you may not use this file except in compliance with the License.
  *    You may obtain a copy of the License at
  *
  *       http://www.apache.org/licenses/LICENSE-2.0
  *
  *    Unless required by applicable law or agreed to in writing, software
  *    distributed under the License is distributed on an "AS IS" BASIS,
  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *    See the License for the specific language governing permissions and
  *    limitations under the License.
  */
 package org.apache.ibatis.reflection;
 
 import java.util.Arrays;
 
 /**
  * Provides hashCode, equals and toString methods that can handle array.
  */
 public class ArrayUtil {
 
 
/** Yields a hash code for {@code obj}. The meethod return A hash code of {@code obj} or 0 if {@code obj} is <code>null</code> */
 public static int hashCode(Object obj){
   	if(obj == null){
 		return 0;
 	}
 	if(obj.getClass().isArray()){
 		int result = 1;
 		int length = Array.getLength(obj);
 		for(int i = 0; i < length; i++){
 			Object element = Array.get(obj, i);
 			result = 31 * result + (element == null ? 0 : element.hashCode());
 		}
 		return result;
 	}
 	return obj.hashCode();    
 }

 

}