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
 
 
/** The method in question splits the input map with the goal that no map has more total data than the specified limit. It then returns a list of target objects constructed from the resulting maps. In some cases it may happen that some maps get a partial value for larger elements.  The 'recordCount' method of the first target will be invoked with the number of targets  created if the target type implements CountRecorder */

public List<T> split(Map<String, String> data) {
  int totalDataSize = calculateTotalDataSize(data);
  int numTargets = calculateNumTargets(totalDataSize);

  if (numTargets == 0) {
    return result;
  }

  int dataSizePerTarget = totalDataSize / numTargets;
  int remainingDataSize = totalDataSize % numTargets;

  current = new HashMap<>();
  remainingRoom = DATA_LIMIT;

  for (Map.Entry<String, String> entry : data.entrySet()) {
    String key = entry.getKey();
    String value = entry.getValue();
    int dataSize = calculateDataSize(key, value);

    if (dataSize > remainingRoom) {
      addCurrentTarget();
      current = new HashMap<>();
      remainingRoom = DATA_LIMIT;
    }

    current.put(key, value);
    remainingRoom -= dataSize;
  }

  addCurrentTarget();

  if (result.size() > numTargets) {
    trimTargets(numTargets);
  }

  return result;
}
 

}