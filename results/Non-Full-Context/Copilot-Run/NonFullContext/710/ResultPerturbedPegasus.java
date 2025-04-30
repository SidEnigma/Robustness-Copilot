/*
  * #%L
  * ACS AEM Commons Bundle
  * %%
  * Copyright (C) 2013 Adobe
  * %%
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * 
  *      http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  * #L%
  */
 package com.adobe.acs.commons.util;
 
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.time.OffsetDateTime;
 import java.util.Arrays;
 import java.util.Calendar;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.LinkedHashMap;
 import java.util.Map;
 import java.util.regex.Pattern;
 
 import org.apache.commons.lang.ClassUtils;
 import org.apache.commons.lang.StringUtils;
 import org.apache.sling.api.resource.ValueMap;
 import org.apache.sling.api.wrappers.ValueMapDecorator;
 import org.osgi.annotation.versioning.ProviderType;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.google.gson.Gson;
 import com.google.gson.JsonObject;
 
 @ProviderType
 public final class TypeUtil {
     private static final Logger log = LoggerFactory.getLogger(TypeUtil.class);
 
     private static final Pattern JSON_DATE =
             Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}[-+]{1}[0-9]{2}[:]{0,1}[0-9]{2}$");
 
     private TypeUtil() {
     }
 
     /**
      * Turn a even length Array into a Map. The Array is expected to be in the
      * format: { key1, value1, key2, value2, ... , keyN, valueN }
      *
      * @param <T>
      * @param list
      * @return
      */
     public static <T> Map<T, T> arrayToMap(T[] list) {
         final HashMap<T, T> map = new HashMap<T, T>();
         if (list == null) {
             return map;
         }
         if (list.length > 0 && (list.length % 2) == 1) {
             throw new IllegalArgumentException(
                     "Array must be even in length, representing a series of Key, Value pairs.");
         }
 
         for (int i = 0; i < list.length; i = i + 2) {
             map.put(list[i], list[i + 1]);
         }
 
         return map;
     }
 
     /**
      * Convenience wrapper for toMap(jsonObj, Object.class).
      *
      * @param json
      * @return
      */
     public static Map<String, Object> toMap(JsonObject json) {
         Gson gson = new Gson();
         return gson.fromJson(json, Map.class);
     }
 
     /**
      * Determines the type of the parameter object.
      * <p/>
      * TODO - review this method
      *
      * @param object
      * @param <T>
      * @return
      */
     @SuppressWarnings({ "unchecked", "PMD.CollapsibleIfStatements" })
     public static <T> Class<T> getType(final Object object) {
         if (object instanceof Double || object instanceof Float) {
             return (Class<T>) Double.class;
         } else if (object instanceof Number) {
             return (Class<T>) Long.class;
         } else if (object instanceof Boolean) {
             return (Class<T>) Boolean.class;
         } else if (object instanceof String) {
             if (JSON_DATE.matcher((String) object).matches()) {
                 return (Class<T>) Date.class;
             }
         } else if(object instanceof Calendar) {
             return (Class<T>) Calendar.class;
         } else if(object instanceof Date) {
             return (Class<T>) Date.class;
         }
 
         return (Class<T>) String.class;
     }
 
 
/** The dates in the string are converted to the corresponding objects. */
 public static T toObjectType(String data, Class<T> klass){
        if(klass == String.class){
            return (T) data;
        } else if(klass == Double.class || klass == Double.TYPE){
            return (T) Double.valueOf(data);
        } else if(klass == Float.class || klass == Float.TYPE){
            return (T) Float.valueOf(data);
        } else if(klass == Long.class || klass == Long.TYPE){
            return (T) Long.valueOf(data);
        } else if(klass == Integer.class || klass == Integer.TYPE){
            return (T) Integer.valueOf(data);
        } else if(klass == Boolean.class || klass == Boolean.TYPE){
            return (T) Boolean.valueOf(data);
        } else if(klass == Date.class){
            return (T) OffsetDateTime.parse(data).toDate();
        } else if(klass == Calendar.class){
            return (T) Calendar.getInstance();
        } else if(klass == ValueMap.class){
            return (T) new ValueMapDecorator(new HashMap<String, Object>());
        } else if(klass == JsonObject.class){
            return (T) new JsonObject();
        } else if(klass == Map.class){
            return (T) new HashMap<String, Object>();
        } else if(klass == LinkedHashMap.class){
            return (T) new LinkedHashMap<String, Object>();
        } else if(klass == Arrays.class){
            return (T) new String[0];
        } else if(klass == String[].class){
            return (T) new String[]{data};
        } else if(klass == Double[].class){
            return (T) new Double[]{Double.valueOf(data)};
        } else if(klass == Float[].class){
            return (T) new Float[]{Float.valueOf(data)};
        } else if(klass == Long[].class){
            return (T) new Long[]       
 }

 

}