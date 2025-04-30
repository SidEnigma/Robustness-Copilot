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
 
   /**
    * Given a map, splits it so that no map has more total data than the specified limit, and returns a list of
    * target objects built from the resultant maps. This may result in some maps receiving partial value for the largest
    * items. If the target type implements CountRecorder, the 'recordCount' method of the first target will be invoked
    * with the number of targets created.
    *
    * @param data the map to split.
    */
   public List<T> split(Map<String, String> data) {
     startSplitResult();
     for (DataEntry dataEntry : getSortedEntrySizes(data)) {
       addToSplitResult(dataEntry);
     }
     recordSplitResult();
 
     recordTargetInfo(result.get(0), result.size());
     return result;
   }
 
   @Nonnull
   private List<DataEntry> getSortedEntrySizes(Map<String, String> data) {
     return data.entrySet().stream().map(DataEntry::new).sorted().collect(Collectors.toList());
   }
 
   private void startSplitResult() {
     current = new HashMap<>();
     remainingRoom = DATA_LIMIT;
   }
 
 
/** Adds the specified data entry to one or more split results, saving its location if it is not entirely in the first split result. */
 private void addToSplitResult(DataEntry entry){}

 

}