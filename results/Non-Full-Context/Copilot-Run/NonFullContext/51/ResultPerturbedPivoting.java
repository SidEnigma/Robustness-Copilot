// Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.operator.helpers;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.function.BiFunction;
 import java.util.stream.Collectors;
 import javax.annotation.Nonnull;
 
 /**
  * A Kubernetes ConfigMap has a hard size limit; attempts to create one larger will fail. This is a
  * problem when we need to store more data in a config map. Our solution is to split the data among multiple maps.
  *
  * @param <T> the kind of target object to create, which will ultimately be used to create config maps
  */
 public class ConfigMapSplitter<T extends SplitterTarget> {
 
   // The limit for a Kubernetes Config Map is 1MB, including all components of the map. We use a data limit a bit
   // below that to ensure that the map structures, including the keys, metadata and the results of JSON encoding, don't
   // accidentally put us over the limit.
 
   // not private or local so that unit tests can set it.
   @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal", "FieldCanBeLocal"})
   private static int DATA_LIMIT = 900_000;
 
   private final BiFunction<Map<String, String>, Integer, T> factory;
 
   private final List<T> result = new ArrayList<>();
   private Map<String, String> current;
   private int remainingRoom;
 
   /**
    * Constructs a splitter object.
    *
    * @param factory a function that the splitter should use to create its target objects.
    */
   public ConfigMapSplitter(BiFunction<Map<String, String>, Integer, T> factory) {
     this.factory = factory;
   }
 
 
/** Given a map, divide it so that no map has more total data than the specified limit and returns a list of target objects built from the resulting maps. */
 public List<T> split(Map<String, String> data){
    current = new HashMap<>();
    remainingRoom = DATA_LIMIT;
    for (Map.Entry<String, String> entry : data.entrySet()) {
      add(entry.getKey(), entry.getValue());
    }
    result.add(factory.apply(current, remainingRoom));
    return result;
  }
  
    /**
      * Adds a key/value pair to the current map. If the key is too long to fit in the current map, then the current map is
      * added to the list of results and a new map is created.
      *
      * @param key the key to add
      * @param value the value to add
      */
    private void add(String key, String value) {
      int keyLength = key.length();
      int valueLength = value.length();
      if (keyLength + valueLength > remainingRoom) {
        result.add(factory.apply(current, remainingRoom));
        current = new HashMap<>();
        remainingRoom = DATA_LIMIT;
      }
      current.put(key, value);
      remainingRoom -= keyLength + valueLength;
    }
  
    /**
      * A target object that can be used to create a config map.
      */
    public interface SplitterTarget {
  
      /**
        * Returns the map of key/value pairs that should be used to create the config map.
        *
        * @return the map of key/value pairs that should be used to create the config map.
        */
      @Nonnull
      Map<String, String> getData();
  
      /**
        * Returns the name of the config map that should be used to create the target object.
        *
        * @return the name of the config map that should be used to create the target object.
        */
      @Nonnull
      String getName();
  
      /**
        * Returns the namespace of the config map that should be used to create the target object.
        *
        * @return the namespace of the config map that should be used to create the target object.
        */
      @Nonnull
      String getNamespace();    
 }

 

}