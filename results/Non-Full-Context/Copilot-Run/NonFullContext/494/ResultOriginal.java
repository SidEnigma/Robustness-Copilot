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
 
 
/** Returns true if the two maps of values match. */
  static boolean mapEquals(Map<K, V> first, Map<K, V> second){
    if (first == second) {
      return true;
    }
    if (first == null || second == null) {
      return false;
    }
    if (first.size() != second.size()) {
      return false;
    }
    for (Map.Entry<K, V> entry : first.entrySet()) {
      if (!second.containsKey(entry.getKey())) {
        return false;
      }
      if (!Objects.equals(entry.getValue(), second.get(entry.getKey()))) {
        return false;
      }
    }
    return true;      
  }

 

}