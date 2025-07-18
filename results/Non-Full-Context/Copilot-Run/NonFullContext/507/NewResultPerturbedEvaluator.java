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
 
 
/** If the required map misses the current object, return true, false otherwise. */

  static boolean isMissingValues(Map<String, String> current, Map<String, String> required) {
    return Objects.equals(current, required) || (isNullOrEmpty(current) && isNullOrEmpty(required));
  }
 

}