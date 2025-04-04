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
 
 
/** Given a map, splits it so that no map has more total data than the specified limit, and returns a list of  target objects built from the resultant maps. */
 public List<T> split(Map<String, String> data){
    current = new HashMap<>();
    remainingRoom = DATA_LIMIT;
    result.clear();
    split(data);
    return result;
  }
  
    private void split(Map<String, String> data) {
      for (Map.Entry<String, String> entry : data.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        if (value.length() > remainingRoom) {
          // We can't fit this key/value pair in the current map.
          // Create a new map and add the current one to the result.
          result.add(factory.apply(current, remainingRoom));
          current = new HashMap<>();
          remainingRoom = DATA_LIMIT;
        }
        current.put(key, value);
        remainingRoom -= key.length() + value.length();
      }
      if (!current.isEmpty()) {
        result.add(factory.apply(current, remainingRoom));
      }
    }
  
    /**
      * Given a list of target objects, returns a map of the same kind of objects, but with all the data in the target objects
      * combined into a single map.
      *
      * @param targets a list of target objects
      * @return a map of the same kind of objects, but with all the data in the target objects combined into a single map
      */
    public static <T extends SplitterTarget> Map<String, String> combine(List<T> targets) {
      return targets.stream().flatMap(target -> target.getData().entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b));
    }
  
    /**
      * Given a list of target objects, returns a map of the same kind of objects, but with all the data in the target objects
      * combined into a single map.
      *
      * @param targets a list of target objects
      * @return a map of the same kind of objects, but with all the data in the target objects combined into a single map   
 }

 

}