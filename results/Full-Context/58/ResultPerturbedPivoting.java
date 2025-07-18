/*
  * Copyright (c) 2020 Network New Technologies Inc.
  *
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
  */
 package com.networknt.schema;
 
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
 
 /**
  * Context for holding the output returned by the {@link Collector}
  * implementations.
  */
 public class CollectorContext {
 
     // Using a namespace string as key in ThreadLocal so that it is unique in
     // ThreadLocal.
     static final String COLLECTOR_CONTEXT_THREAD_LOCAL_KEY = "com.networknt.schema.CollectorKey";
 
     // Get an instance from thread info (which uses ThreadLocal).
     public static CollectorContext getInstance() {
         return (CollectorContext) ThreadInfo.get(COLLECTOR_CONTEXT_THREAD_LOCAL_KEY);
     }
 
     /**
      * Map for holding the name and {@link Collector} or a simple Object.
      */
     private Map<String, Object> collectorMap = new HashMap<String, Object>();
 
     /**
      * Map for holding the name and {@link Collector} class collect method output.
      */
     private Map<String, Object> collectorLoadMap = new HashMap<String, Object>();
 
     /**
      * Adds a collector with give name. Preserving this method for backward
      * compatibility.
      *
      * @param <E>       element
      * @param name      String
      * @param collector Collector
      */
     public <E> void add(String name, Collector<E> collector) {
         collectorMap.put(name, collector);
     }
 
     /**
      * Adds a collector or a simple object with give name.
      *
      * @param <E>    element
      * @param object Object
      * @param name   String
      */
     public <E> void add(String name, Object object) {
         collectorMap.put(name, object);
     }
 
     /**
      * Gets the data associated with a given name. Please note if you are collecting
      * {@link Collector} instances you should wait till the validation is complete
      * to gather all data.
      * <p>
      * When {@link CollectorContext} is used to collect {@link Collector} instances
      * for a particular key, this method will return the {@link Collector} instance
      * as long as {@link #loadCollectors} method is not called. Once
      * the {@link #loadCollectors} method is called this method will
      * return the actual data collected by collector.
      *
      * @param name String
      * @return Object
      */
     public Object get(String name) {
         Object object = collectorMap.get(name);
         if (object instanceof Collector<?> && (collectorLoadMap.get(name) != null)) {
             return collectorLoadMap.get(name);
         }
         return collectorMap.get(name);
     }
 
     /**
      * Returns all the collected data. Please look into {@link #get(String)} method for more details.
      * @return Map
      */
     public Map<String, Object> getAll() {
         Map<String, Object> mergedMap = new HashMap<String, Object>();
         mergedMap.putAll(collectorMap);
         mergedMap.putAll(collectorLoadMap);
         return mergedMap;
     }
 
 
/** Combines data with collector identified by first name. */
 public void combineWithCollector(String name, Object data){
        if(collectorMap.get(name) instanceof Collector<?>){
            collectorLoadMap.put(name, data);
        }
    }
    
        /**
        * Loads the data collected by the {@link Collector} instances.
        *
        * @param <E>       element
        * @param name      String
        * @param collector Collector
        */
        public <E> void loadCollectors(String name, Collector<E> collector) {
            collectorLoadMap.put(name, collector.load());
        }
    
        /**
        * Loads the data collected by the {@link Collector} instances.
        *
        * @param <E>    element
        * @param object Object
        * @param name   String
        */
        public <E> void loadCollectors(String name, Object object) {
            collectorLoadMap.put(name, object);
        }
    
        /**
        * Loads the data collected by the {@link Collector} instances.
        *
        * @param <E>       element
        * @param name      String
        * @param collector Collector
        */
        public <E> void loadCollectors(String name, Collector<E> collector, Object data) {
            collectorLoadMap.put(name, data);
        }
    
        /**
        * Loads the data collected by the {@link Collector} instances.
        *
        * @param <E>    element
        * @param object Object
        * @param name   String
        */
        public <E> void loadCollectors(String name, Object object, Object data) {
            collectorLoadMap.put(name, data);
        }
    
        /**
        * Loads the data collected by the {@link Collector} instances.
        *
        * @param <E>       element
        * @param name      String
        * @param collector Collector
        */
        public <E> void loadCollectors(String name, Collector<E> collector, Object data, Object data2) {
            collectorLoadMap.put(name, data);
            collectorLoadMap.put(name, data2);      
 }

            
     /**
      * Reset the context
      */
     public void reset() {
         this.collectorMap = new HashMap<String, Object>();
         this.collectorLoadMap = new HashMap<String, Object>();
     }
 
     /**
      * Loads data from all collectors.
      */
     void loadCollectors() {
         Set<Entry<String, Object>> entrySet = collectorMap.entrySet();
         for (Entry<String, Object> entry : entrySet) {
             if (entry.getValue() instanceof Collector<?>) {
                 Collector<?> collector = (Collector<?>) entry.getValue();
                 collectorLoadMap.put(entry.getKey(), collector.collect());
             }
         }
 
     }
 
 }
