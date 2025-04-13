// Copyright (c) 2017, 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.operator.helpers;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
 import java.util.Map;
 import java.util.Optional;
 import javax.annotation.Nonnull;
 import javax.annotation.Nullable;
 
 import io.kubernetes.client.openapi.models.V1DeleteOptions;
 import io.kubernetes.client.openapi.models.V1EnvVar;
 import io.kubernetes.client.openapi.models.V1EnvVarBuilder;
 import io.kubernetes.client.openapi.models.V1ObjectMeta;
 import io.kubernetes.client.openapi.models.V1Pod;
 import io.kubernetes.client.openapi.models.V1PodCondition;
 import io.kubernetes.client.openapi.models.V1PodSpec;
 import io.kubernetes.client.openapi.models.V1PodStatus;
 import oracle.kubernetes.operator.DomainStatusUpdater;
 import oracle.kubernetes.operator.LabelConstants;
 import oracle.kubernetes.operator.MakeRightDomainOperation;
 import oracle.kubernetes.operator.PodAwaiterStepFactory;
 import oracle.kubernetes.operator.ProcessingConstants;
 import oracle.kubernetes.operator.TuningParameters;
 import oracle.kubernetes.operator.calls.CallResponse;
 import oracle.kubernetes.operator.calls.RetryStrategy;
 import oracle.kubernetes.operator.logging.LoggingFacade;
 import oracle.kubernetes.operator.logging.LoggingFactory;
 import oracle.kubernetes.operator.logging.MessageKeys;
 import oracle.kubernetes.operator.steps.DefaultResponseStep;
 import oracle.kubernetes.operator.utils.Certificates;
 import oracle.kubernetes.operator.work.Component;
 import oracle.kubernetes.operator.work.NextAction;
 import oracle.kubernetes.operator.work.Packet;
 import oracle.kubernetes.operator.work.Step;
 import oracle.kubernetes.weblogic.domain.model.ServerSpec;
 import oracle.kubernetes.weblogic.domain.model.Shutdown;
 
 import static oracle.kubernetes.operator.LabelConstants.CLUSTERNAME_LABEL;
 import static oracle.kubernetes.operator.LabelConstants.SERVERNAME_LABEL;
 import static oracle.kubernetes.operator.ProcessingConstants.SERVERS_TO_ROLL;
 
 @SuppressWarnings("ConstantConditions")
 public class PodHelper {
   static final long DEFAULT_ADDITIONAL_DELETE_TIME = 10;
   private static final LoggingFacade LOGGER = LoggingFactory.getLogger("Operator", "Operator");
 
   private PodHelper() {
   }
 
   /**
    * Specifies the product version used to create pods.
    * @param productVersion the version of the operator.
    */
   public static void setProductVersion(String productVersion) {
     PodStepContext.setProductVersion(productVersion);
   }
 
   /**
    * Creates an admin server pod resource, based on the specified packet.
    * Expects the packet to contain a domain presence info as well as:
    *   SCAN                 the topology for the server (WlsServerConfig)
    *   DOMAIN_TOPOLOGY      the topology for the domain (WlsDomainConfig)
    *
    *
    * @param packet a packet describing the domain model and topology.
    * @return an appropriate Kubernetes resource
    */
   public static V1Pod createAdminServerPodModel(Packet packet) {
     return new AdminPodStepContext(null, packet).createPodModel();
   }
 
   /**
    * Creates a managed server pod resource, based on the specified packet.
    * Expects the packet to contain a domain presence info as well as:
    *   CLUSTER_NAME         (optional) the name of the cluster to which the server is assigned
    *   SCAN                 the topology for the server (WlsServerConfig)
    *   DOMAIN_TOPOLOGY      the topology for the domain (WlsDomainConfig)
    *
    *
    * @param packet a packet describing the domain model and topology.
    * @return an appropriate Kubernetes resource
    */
   public static V1Pod createManagedServerPodModel(Packet packet) {
     return new ManagedPodStepContext(null, packet).createPodModel();
   }
 
   /**
    * check if pod is ready.
    * @param pod pod
    * @return true, if pod is ready
    */
   public static boolean isReady(V1Pod pod) {
     boolean ready = getReadyStatus(pod);
     if (ready) {
       LOGGER.fine(MessageKeys.POD_IS_READY, pod.getMetadata().getName());
     }
     return ready;
   }
 
   static boolean hasReadyServer(V1Pod pod) {
     return Optional.ofNullable(pod).map(PodHelper::getReadyStatus).orElse(false);
   }
 
   static boolean isScheduled(@Nullable V1Pod pod) {
     return Optional.ofNullable(pod).map(V1Pod::getSpec).map(V1PodSpec::getNodeName).isPresent();
   }
 
   static boolean hasClusterNameOrNull(@Nullable V1Pod pod, String clusterName) {
     return getClusterName(pod) == null || getClusterName(pod).equals(clusterName);
   }
 
   private static String getClusterName(@Nullable V1Pod pod) {
     return Optional.ofNullable(pod)
           .map(V1Pod::getMetadata)
           .map(V1ObjectMeta::getLabels)
           .map(PodHelper::getClusterName)
           .orElse(null);
   }
 
   private static String getClusterName(@Nonnull Map<String,String> labels) {
     return labels.get(CLUSTERNAME_LABEL);
   }
 
   static boolean isNotAdminServer(@Nullable V1Pod pod, String adminServerName) {
     return Optional.ofNullable(getServerName(pod)).map(s -> !s.equals(adminServerName)).orElse(true);
   }
 
   private static String getServerName(@Nullable V1Pod pod) {
     return Optional.ofNullable(pod)
             .map(V1Pod::getMetadata)
             .map(V1ObjectMeta::getLabels)
             .map(PodHelper::getServerName)
             .orElse(null);
   }
 
   private static String getServerName(@Nonnull Map<String,String> labels) {
     return labels.get(SERVERNAME_LABEL);
   }
 
   /**
    * get if pod is in ready state.
    * @param pod pod
    * @return true, if pod is ready
    */
   public static boolean getReadyStatus(V1Pod pod) {
     return Optional.ofNullable(pod.getStatus())
           .filter(PodHelper::isRunning)
           .map(V1PodStatus::getConditions)
           .orElse(Collections.emptyList())
           .stream()
           .anyMatch(PodHelper::isReadyCondition);
   }
 
   private static boolean isRunning(@Nonnull V1PodStatus status) {
     return "Running".equals(status.getPhase());
   }
 
   private static boolean isReadyCondition(V1PodCondition condition) {
     return "Ready".equals(condition.getType()) && "True".equals(condition.getStatus());
   }
 
   /**
    * Check if pod is deleting.
    * @param pod pod
    * @return true, if pod is deleting
    */
   public static boolean isDeleting(V1Pod pod) {
     V1ObjectMeta meta = pod.getMetadata();
     if (meta != null) {
       return meta.getDeletionTimestamp() != null;
     }
     return false;
   }
 
   /**
    * Check if pod is in failed state.
    * @param pod pod
    * @return true, if pod is in failed state
    */
   public static boolean isFailed(V1Pod pod) {
     V1PodStatus status = pod.getStatus();
     if (status != null) {
       if ("Failed".equals(status.getPhase())) {
         LOGGER.severe(MessageKeys.POD_IS_FAILED, pod.getMetadata().getName());
         return true;
       }
     }
     return false;
   }
 
   /**
    * get pod domain UID.
    * @param pod pod
    * @return domain UID
    */
   public static String getPodDomainUid(V1Pod pod) {
     return KubernetesUtils.getDomainUidLabel(
         Optional.ofNullable(pod).map(V1Pod::getMetadata).orElse(null));
   }
 
   /**
    * get pod's server name.
    * @param pod pod
    * @return server name
    */
   public static String getPodServerName(V1Pod pod) {
     V1ObjectMeta meta = pod.getMetadata();
     Map<String, String> labels = meta.getLabels();
     if (labels != null) {
       return labels.get(LabelConstants.SERVERNAME_LABEL);
     }
     return null;
   }
 
 
   /**
    * Factory for {@link Step} that creates admin server pod.
    *
    * @param next Next processing step
    * @return Step for creating admin server pod
    */
   public static Step createAdminPodStep(Step next) {
     return new AdminPodStep(next);
   }
 
   static void addToPacket(Packet packet, PodAwaiterStepFactory pw) {
     packet
         .getComponents()
         .put(
             ProcessingConstants.PODWATCHER_COMPONENT_NAME,
             Component.createFor(PodAwaiterStepFactory.class, pw));
   }
 
   static PodAwaiterStepFactory getPodAwaiterStepFactory(Packet packet) {
     return packet.getSpi(PodAwaiterStepFactory.class);
   }
 
   /**
    * Factory for {@link Step} that creates managed server pod.
    *
    * @param next Next processing step
    * @return Step for creating managed server pod
    */
   public static Step createManagedPodStep(Step next) {
     return new ManagedPodStep(next);
   }
 
   /**
    * Factory for {@link Step} that deletes server pod.
    *
    * @param serverName the name of the server whose pod is to be deleted
    * @param next Next processing step
    * @return Step for deleting server pod
    */
   public static Step deletePodStep(String serverName, Step next) {
     return new DeletePodStep(serverName, next);
   }
 
 
/** Given a list of environment variables, generate a copy of that V1EnvVar list. */

public static List<V1EnvVar> createCopy(List<V1EnvVar> envVars) {
  List<V1EnvVar> copy = new ArrayList<>();
  
  for (V1EnvVar envVar : envVars) {
    V1EnvVarBuilder builder = new V1EnvVarBuilder();
    
    builder.withName(envVar.getName());
    builder.withValue(envVar.getValue());
    builder.withValueFrom(envVar.getValueFrom());
    builder.withSecretKeyRef(envVar.getSecretKeyRef());
    builder.withConfigMapKeyRef(envVar.getConfigMapKeyRef());
    builder.withFieldRef(envVar.getFieldRef());
    builder.withResourceFieldRef(envVar.getResourceFieldRef());
    builder.withPrefix(envVar.getPrefix());
    
    copy.add(builder.build());
  }
  
  return copy;
}
 

}