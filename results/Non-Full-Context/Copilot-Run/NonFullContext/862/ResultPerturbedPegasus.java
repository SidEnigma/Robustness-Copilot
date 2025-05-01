// Copyright (c) 2018, 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.operator.helpers;
 
 import java.lang.reflect.Field;
 import java.math.BigInteger;
 import java.time.OffsetDateTime;
 import java.util.Map;
 import java.util.Objects;
 import java.util.Optional;
 
 import io.kubernetes.client.common.KubernetesListObject;
 import io.kubernetes.client.openapi.models.V1ListMeta;
 import io.kubernetes.client.openapi.models.V1ObjectMeta;
 import jakarta.json.JsonPatchBuilder;
 import oracle.kubernetes.operator.KubernetesConstants;
 import oracle.kubernetes.operator.LabelConstants;
 
 import static oracle.kubernetes.operator.KubernetesConstants.ALWAYS_IMAGEPULLPOLICY;
 import static oracle.kubernetes.operator.KubernetesConstants.IFNOTPRESENT_IMAGEPULLPOLICY;
 import static oracle.kubernetes.operator.LabelConstants.CREATEDBYOPERATOR_LABEL;
 import static oracle.kubernetes.operator.LabelConstants.DOMAINUID_LABEL;
 import static oracle.kubernetes.utils.OperatorUtils.isNullOrEmpty;
 
 public class KubernetesUtils {
 
   /**
    * Returns true if the two maps of values match. A null map is considered to match an empty map.
    *
    * @param first  the first map to compare
    * @param second the second map to compare
    * @return true if the maps match.
    */
   static <K, V> boolean mapEquals(Map<K, V> first, Map<K, V> second) {
     return Objects.equals(first, second) || (isNullOrEmpty(first) && isNullOrEmpty(second));
   }
 
   /**
    * Returns true if the current map is missing values from the required map. This method is
    * typically used to compare labels and annotations against specifications derived from the
    * domain.
    *
    * @param current  a map of the values found in a Kubernetes resource
    * @param required a map of the values specified for the resource by the domain
    * @return true if there is a problem that must be fixed by patching
    */
   static boolean isMissingValues(Map<String, String> current, Map<String, String> required) {
     if (!hasAllRequiredNames(current, required)) {
       return true;
     }
     for (String name : required.keySet()) {
       if (!Objects.equals(current.get(name), required.get(name))) {
         return true;
       }
     }
 
     return false;
   }
 
   private static boolean hasAllRequiredNames(Map<String, ?> current, Map<String, ?> required) {
     return current.keySet().containsAll(required.keySet());
   }
 
 
/** Patching is done to correct differences in the current vs required maps. */
 static void addPatches(JsonPatchBuilder patchBuilder, String basePath, Map<String, String> current, Map<String, String> required){
    for (String name : required.keySet()) {
      if (!current.containsKey(name)) {
        patchBuilder.add(basePath + "/" + name, required.get(name));
      }
    }
  }
  
    static void addPatches(JsonPatchBuilder patchBuilder, String basePath, Map<String, String> current, Map<String, String> required, String key){
      if (!current.containsKey(key)) {
        patchBuilder.add(basePath + "/" + key, required.get(key));
      }
    }
  
    static void addPatches(JsonPatchBuilder patchBuilder, String basePath, Map<String, String> current, Map<String, String> required, String key, String value){
      if (!current.containsKey(key)) {
        patchBuilder.add(basePath + "/" + key, value);
      }
    }
  
    static void addPatches(JsonPatchBuilder patchBuilder, String basePath, Map<String, String> current, Map<String, String> required, String key, int value){
      if (!current.containsKey(key)) {
        patchBuilder.add(basePath + "/" + key, value);
      }
    }
  
    static void addPatches(JsonPatchBuilder patchBuilder, String basePath, Map<String, String> current, Map<String, String> required, String key, boolean value){
      if (!current.containsKey(key)) {
        patchBuilder.add(basePath + "/" + key, value);
      }
    }
  
    static void addPatches(JsonPatchBuilder patchBuilder, String basePath, Map<String, String> current, Map<String, String> required, String key, OffsetDateTime value){
      if (!current.containsKey(key)) {
        patchBuilder.add(basePath + "/" + key, value);
      }
    }
  
    static void addPatches(JsonPatchBuilder patchBuilder, String basePath, Map<String, String> current, Map<String, String> required, String key, BigInteger value){    
 }

 

}