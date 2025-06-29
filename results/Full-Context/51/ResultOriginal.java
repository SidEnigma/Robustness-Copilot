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
          // Create a new map and add the current key/value pair to it.
          createNewMap();
          current.put(key, value);
        } else {
          // We can fit this key/value pair in the current map.
          // Add the key/value pair to the current map.
          current.put(key, value);
          remainingRoom -= value.length();
        }
      }
      // We've finished processing the map.
      // Create a new map and add the current map to the result.
      createNewMap();
    }
  
    private void createNewMap() {
      if (current.isEmpty()) {
        return;
      }
      result.add(factory.apply(current, DATA_LIMIT - remainingRoom));
      current = new HashMap<>();
      remainingRoom = DATA_LIMIT;
    }
  
    public static class SplitterTarget {
      private final Map<String, String> data;
      private final int dataLimit;
  
      public SplitterTarget(Map<String, String> data, int dataLimit) {
        this.data = data;
        this.dataLimit = dataLimit;
      }
  
      public Map<String, String> getData() {
        return data;
      }
  
      public int getDataLimit() {
        return dataLimit;
      }
    }
  
    public static class SplitterTargetBuilder {
      private final Map<String, String> data = new HashMap<>();
      private int dataLimit;
  
      public SplitterTargetBuilder() {    
 }

                 
   @Nonnull
   private List<DataEntry> getSortedEntrySizes(Map<String, String> data) {
     return data.entrySet().stream().map(DataEntry::new).sorted().collect(Collectors.toList());
   }
 
   private void startSplitResult() {
     current = new HashMap<>();
     remainingRoom = DATA_LIMIT;
   }
 
   /**
    * Adds the specified data entry to one or more split results, recording its location if it is not wholly
    * in the first split result.
    * @param entry a data entry
    */
   private void addToSplitResult(DataEntry entry) {
     while (entry.getRemainingLength() > 0) {
       remainingRoom -= entry.addToMap(current, remainingRoom);
       if (remainingRoom == 0) {
         recordSplitResult();
         startSplitResult();
       }
     }
   }
 
   private void recordSplitResult() {
     result.add(factory.apply(current, result.size()));
   }
 
   private void recordTargetInfo(T target, int size) {
     target.recordNumTargets(size);
   }
 
   static class DataEntry implements Comparable<DataEntry> {
     private final String key;
     private String value;
 
     public DataEntry(Map.Entry<String, String> mapEntry) {
       key = mapEntry.getKey();
       value = mapEntry.getValue();
     }
 
     /**
      * Adds to the specified map, as much of this entry as will fit, removing it from the string
      * still to be added. Returns the number of characters added.
      * @param map the map to update
      * @param limit the maximum number of characters to add
      */
     int addToMap(Map<String, String> map, int limit) {
       final int numCharsAdded = Math.min(limit, value.length());
       map.put(key, value.substring(0, numCharsAdded));
       value = value.substring(numCharsAdded);
 
       return numCharsAdded;
     }
 
     private int getRemainingLength() {
       return value.length();
     }
 
     @Override
     public int compareTo(@Nonnull DataEntry o) {
       return Integer.compare(getRemainingLength(), o.getRemainingLength());
     }
   }
 
 }
