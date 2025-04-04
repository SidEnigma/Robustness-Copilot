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
 
 
/** Returns a hash code for {@code obj}. */
 public static int hashCode(Object obj){
  if (obj == null) {
   return 0;
  }
  if (obj.getClass().isArray()) {
   int hash = obj.getClass().hashCode();
   for (int i = 0; i < Array.getLength(obj); i++) {
    hash = hash * 31 + hashCode(Array.get(obj, i));
   }
   return hash;
  } else {
   return obj.hashCode();
  }     
 }

 

}