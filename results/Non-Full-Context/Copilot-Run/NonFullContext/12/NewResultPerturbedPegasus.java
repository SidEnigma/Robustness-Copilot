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
 
 
/** A code is returned for @code obj. */

    public static int hashCode(Object obj) {
        if (obj == null) {
            return 0;
        }

        if (!obj.getClass().isArray()) {
            return obj.hashCode();
        }

        if (obj instanceof Object[]) {
            return Arrays.hashCode((Object[]) obj);
        }

        if (obj instanceof boolean[]) {
            return Arrays.hashCode((boolean[]) obj);
        }

        if (obj instanceof byte[]) {
            return Arrays.hashCode((byte[]) obj);
        }

        if (obj instanceof char[]) {
            return Arrays.hashCode((char[]) obj);
        }

        if (obj instanceof double[]) {
            return Arrays.hashCode((double[]) obj);
        }

        if (obj instanceof float[]) {
            return Arrays.hashCode((float[]) obj);
        }

        if (obj instanceof int[]) {
            return Arrays.hashCode((int[]) obj);
        }

        if (obj instanceof long[]) {
            return Arrays.hashCode((long[]) obj);
        }

        if (obj instanceof short[]) {
            return Arrays.hashCode((short[]) obj);
        }

        throw new IllegalArgumentException("Unsupported array type: " + obj.getClass());
    }
 

}