// Copyright (c) 2018, 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.operator.helpers;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Optional;
 import java.util.StringTokenizer;
 import java.util.regex.Pattern;
 
 import oracle.kubernetes.operator.TuningParameters;
 
 import static oracle.kubernetes.utils.OperatorUtils.isNullOrEmpty;
 
 /** A class to create DNS-1123 legal names for Kubernetes objects. */
 public class LegalNames {
 
   public static final String DEFAULT_EXTERNAL_SERVICE_NAME_SUFFIX = "-ext";
   private static final String SERVER_PATTERN = "%s-%s";
   private static final String CLUSTER_SERVICE_PATTERN = "%s-cluster-%s";
   public static final String DEFAULT_INTROSPECTOR_JOB_NAME_SUFFIX = "-introspector";
   private static final String DOMAIN_INTROSPECTOR_JOB_PATTERN = "%s%s";
   private static final String EXTERNAL_SERVICE_PATTERN = "%s-%s%s";
 
   public static final String DNS_1123_FIELDS_PARAM = "dns1123Fields";
   public static final String INTROSPECTOR_JOB_NAME_SUFFIX_PARAM = "introspectorJobNameSuffix";
   public static final String EXTERNAL_SERVICE_NAME_SUFFIX_PARAM = "externalServiceNameSuffix";
 
   // Fields that requires values to be DNS1123 legal
   private static final String[] DEFAULT_DNS1123_FIELDS = {
       "ClaimName",          // V1PersistentVolumeClaimVolumeSource
       "ClusterName",        // V1ObjectMetaData
       "ContainerName",      // V1ResourceFieldSelector
       "ExternalName",       // V1ServiceSpec
       "GenerateName",       // V1ObjectMetaData
       "MetricName",         // V2beta1PodsMetricSource, etc
       "Name",
       "NodeName",           // V1PodSpec, etc
       // "NominatedNodeName",  // V1PodStatus - excluded since it is not used within V1PodSpec
       "PersistentVolumeName",// V1VolumeAttachmentSource, etc
       "PriorityClassName",  // V1PodSpec
       "RuntimeClassName",   // V1PodSpec
       "SchedulerName",      // V1PodSpec
       "ScopeName",          // V1ScopedResourceSelectorRequirement
       "SecretName",         // V1SecretVolumeSource, etc
       "ServiceAccountName", // V1PodSpec
       "ServiceName",        // NetworkingV1beta1IngressBackend, etc
       "SingularName",       // V1APIResource
       "StorageClassName",   // V1PersistentVolumeSpec, V1PersistentVolumeClaimSpec
       "VolumeName"         // V1PersistentVolumeClaimSpec, etc
   };
 
   // The maximum length of a legal DNS label name
   public static final int LEGAL_DNS_LABEL_NAME_MAX_LENGTH = 63;
 
   // The maximum length of a container port name
   public static final int LEGAL_CONTAINER_PORT_NAME_MAX_LENGTH = 15;
 
   private static final String DNS_NAME_REGEXP = "[a-z0-9]([-a-z0-9]*[a-z0-9])?";
   private static final Pattern DNS_NAME_PATTERN = Pattern.compile(DNS_NAME_REGEXP);
 
   static String[] dns1123Fields;
 
   public static String toServerServiceName(String domainUid, String serverName) {
     return toServerName(domainUid, serverName);
   }
 
   private static String toServerName(String domainUid, String serverName) {
     return toDns1123LegalName(String.format(SERVER_PATTERN, domainUid, serverName));
   }
 
   public static String toEventName(String domainUid, String serverName) {
     return toServerName(domainUid, serverName);
   }
 
   public static String toPodName(String domainUid, String serverName) {
     return toServerName(domainUid, serverName);
   }
 
   public static String toClusterServiceName(String domainUid, String clusterName) {
     return toDns1123LegalName(String.format(CLUSTER_SERVICE_PATTERN, domainUid, clusterName));
   }
 
   /**
    * Generates the introspector job name based on the given domainUid.
    *
    * @param domainUid domainUid
    * @return String introspector job name
    */
   public static String toJobIntrospectorName(String domainUid) {
     return toDns1123LegalName(String.format(
         DOMAIN_INTROSPECTOR_JOB_PATTERN,
         domainUid,
         getIntrospectorJobNameSuffix()));
   }
 
   /**
    * Gets the configured domain introspector job name suffix.
    * @return String suffix
    */
   public static String getIntrospectorJobNameSuffix() {
     return Optional.ofNullable(TuningParameters.getInstance())
           .map(t -> t.get(INTROSPECTOR_JOB_NAME_SUFFIX_PARAM))
           .orElse(LegalNames.DEFAULT_INTROSPECTOR_JOB_NAME_SUFFIX);
   }
 
 
/** The job name is generated based on the domainUid. */
 public static String toExternalServiceName(String domainUid, String serverName){}

 

}