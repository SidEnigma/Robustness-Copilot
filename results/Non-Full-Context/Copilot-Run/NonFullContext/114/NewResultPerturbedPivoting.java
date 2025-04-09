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
 
   /**
    * Adds patches to the specified patch builder to correct differences in the current vs required
    * maps.
    *
    * @param patchBuilder a builder for the patches
    * @param basePath     the base for the patch path (excluding the name)
    * @param current      a map of the values found in a Kubernetes resource
    * @param required     a map of the values specified for the resource by the domain
    */
   static void addPatches(
         JsonPatchBuilder patchBuilder,
         String basePath,
         Map<String, String> current,
         Map<String, String> required) {
 
     for (String name : required.keySet()) {
       // We must encode each '/' and '~' in a JSON patch token using '~1' and '~0', otherwise
       // the JSON patch will incorrectly treat '/' and '~' as special delimiters. (RFC 6901).
       // The resulting patched JSON will have '/' and '~' within the token (not ~0 or ~1).
       String encodedPath = basePath + name.replace("~","~0").replace("/","~1");
       if (!current.containsKey(name)) {
         patchBuilder.add(encodedPath, required.get(name));
       } else {
         patchBuilder.replace(encodedPath, required.get(name));
       }
     }
   }
 
   /**
    * Returns the name of the resource, extracted from its metadata.
    *
    * @param resource a Kubernetes resource
    * @return the name, if found
    */
   static String getResourceName(Object resource) {
     return Optional.ofNullable(getResourceMetadata(resource)).map(V1ObjectMeta::getName).orElse(null);
   }
 
   /**
    * Returns the metadata of the resource.
    *
    * @param resource a Kubernetes resource
    * @return the metadata, if found; otherwise a newly created one.
    */
   static V1ObjectMeta getResourceMetadata(Object resource) {
     try {
       Field metadataField = resource.getClass().getDeclaredField("metadata");
       metadataField.setAccessible(true);
       return (V1ObjectMeta) metadataField.get(resource);
     } catch (NoSuchFieldException
           | SecurityException
           | IllegalArgumentException
           | IllegalAccessException e) {
       return new V1ObjectMeta();
     }
   }
 
 
/** Returns true if the first metadata indicates a newer resource than the second. */

public static boolean isFirstNewer(V1ObjectMeta first, V1ObjectMeta second) {
  if (first == null || second == null) {
    return false;
  }

  OffsetDateTime firstCreationTimestamp = first.getCreationTimestamp();
  OffsetDateTime secondCreationTimestamp = second.getCreationTimestamp();

  if (firstCreationTimestamp == null || secondCreationTimestamp == null) {
    return false;
  }

  return firstCreationTimestamp.isAfter(secondCreationTimestamp);
}
 

}