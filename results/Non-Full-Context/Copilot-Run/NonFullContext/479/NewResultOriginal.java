// Copyright (c) 2018, 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.operator.helpers;
 
 import java.time.OffsetDateTime;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.List;
 import java.util.Objects;
 import java.util.Optional;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ConcurrentMap;
 import java.util.concurrent.atomic.AtomicBoolean;
 import java.util.concurrent.atomic.AtomicReference;
 import java.util.concurrent.locks.ReadWriteLock;
 import java.util.concurrent.locks.ReentrantReadWriteLock;
 import java.util.function.Predicate;
 import java.util.stream.Stream;
 import javax.annotation.Nonnull;
 import javax.annotation.Nullable;
 
 import io.kubernetes.client.openapi.models.V1EnvVar;
 import io.kubernetes.client.openapi.models.V1ObjectMeta;
 import io.kubernetes.client.openapi.models.V1Pod;
 import io.kubernetes.client.openapi.models.V1Secret;
 import io.kubernetes.client.openapi.models.V1Service;
 import io.kubernetes.client.openapi.models.V1beta1PodDisruptionBudget;
 import oracle.kubernetes.operator.TuningParameters;
 import oracle.kubernetes.operator.WebLogicConstants;
 import oracle.kubernetes.operator.wlsconfig.WlsServerConfig;
 import oracle.kubernetes.operator.work.Packet;
 import oracle.kubernetes.utils.SystemClock;
 import oracle.kubernetes.weblogic.domain.model.Domain;
 import oracle.kubernetes.weblogic.domain.model.ServerSpec;
 import org.apache.commons.lang3.builder.EqualsBuilder;
 import org.apache.commons.lang3.builder.HashCodeBuilder;
 import org.apache.commons.lang3.builder.ToStringBuilder;
 
 import static java.lang.System.lineSeparator;
 import static oracle.kubernetes.operator.helpers.EventHelper.EventItem;
 import static oracle.kubernetes.operator.helpers.PodHelper.hasClusterNameOrNull;
 import static oracle.kubernetes.operator.helpers.PodHelper.isNotAdminServer;
 
 /**
  * Operator's mapping between custom resource Domain and runtime details about that domain,
  * including the scan and the Pods and Services for servers.
  */
 public class DomainPresenceInfo {
   private final String namespace;
   private final String domainUid;
   private final AtomicReference<Domain> domain;
   private final AtomicBoolean isDeleting = new AtomicBoolean(false);
   private final AtomicBoolean isPopulated = new AtomicBoolean(false);
   private final AtomicReference<Collection<ServerStartupInfo>> serverStartupInfo;
   private final AtomicReference<Collection<ServerShutdownInfo>> serverShutdownInfo;
 
   private final ConcurrentMap<String, ServerKubernetesObjects> servers = new ConcurrentHashMap<>();
   private final ConcurrentMap<String, V1Service> clusters = new ConcurrentHashMap<>();
   private final ConcurrentMap<String, V1beta1PodDisruptionBudget> podDisruptionBudgets = new ConcurrentHashMap<>();
   private final ReadWriteLock webLogicCredentialsSecretLock = new ReentrantReadWriteLock();
   private V1Secret webLogicCredentialsSecret;
   private OffsetDateTime webLogicCredentialsSecretLastSet;
 
   private final List<String> validationWarnings = Collections.synchronizedList(new ArrayList<>());
   private EventItem lastEventItem;
 
   /**
    * Create presence for a domain.
    *
    * @param domain Domain
    */
   public DomainPresenceInfo(Domain domain) {
     this.domain = new AtomicReference<>(domain);
     this.namespace = domain.getMetadata().getNamespace();
     this.domainUid = domain.getDomainUid();
     this.serverStartupInfo = new AtomicReference<>(null);
     this.serverShutdownInfo = new AtomicReference<>(null);
   }
 
   /**
    * Create presence for a domain.
    *
    * @param namespace Namespace
    * @param domainUid The unique identifier assigned to the WebLogic domain when it was registered
    */
   public DomainPresenceInfo(String namespace, String domainUid) {
     this.domain = new AtomicReference<>(null);
     this.namespace = namespace;
     this.domainUid = domainUid;
     this.serverStartupInfo = new AtomicReference<>(null);
     this.serverShutdownInfo = new AtomicReference<>(null);
   }
 
   private static <K, V> boolean removeIfPresentAnd(
       ConcurrentMap<K, V> map, K key, Predicate<? super V> predicateFunction) {
     Objects.requireNonNull(predicateFunction);
     for (V oldValue; (oldValue = map.get(key)) != null; ) {
       if (!predicateFunction.test(oldValue)) {
         return false;
       } else if (map.remove(key, oldValue)) {
         return true;
       }
     }
     return false;
   }
 
   /**
    * Counts the number of non-clustered servers and servers in the specified cluster that are scheduled.
    * @param clusterName cluster name of the pod server
    * @return Number of scheduled servers
    */
   long getNumScheduledServers(String clusterName) {
     return getServersInNoOtherCluster(clusterName)
             .filter(PodHelper::isScheduled)
             .count();
   }
 
   /**
    * Counts the number of non-clustered managed servers and managed servers in the specified cluster that are scheduled.
    * @param clusterName cluster name of the pod server
    * @param adminServerName Name of the admin server
    * @return Number of scheduled managed servers
    */
   public long getNumScheduledManagedServers(String clusterName, String adminServerName) {
     return getManagedServersInNoOtherCluster(clusterName, adminServerName)
           .filter(PodHelper::isScheduled)
           .count();
   }
 
   /**
    * Counts the number of non-clustered servers (including admin) and servers in the specified cluster that are ready.
    * @param clusterName cluster name of the pod server
    * @return Number of ready servers
    */
   long getNumReadyServers(String clusterName) {
     return getServersInNoOtherCluster(clusterName)
             .filter(PodHelper::hasReadyServer)
             .count();
   }
 
   /**
    * Counts the number of non-clustered managed servers and managed servers in the specified cluster that are ready.
    * @param clusterName cluster name of the pod server
    * @return Number of ready servers
    */
   public long getNumReadyManagedServers(String clusterName, String adminServerName) {
     return getManagedServersInNoOtherCluster(clusterName, adminServerName)
           .filter(PodHelper::hasReadyServer)
           .count();
   }
 
   @Nonnull
   private Stream<V1Pod> getServersInNoOtherCluster(String clusterName) {
     return getServers().values().stream()
             .map(ServerKubernetesObjects::getPod)
             .map(AtomicReference::get)
             .filter(this::isNotDeletingPod)
             .filter(p -> hasClusterNameOrNull(p, clusterName));
   }
 
   @Nonnull
   private Stream<V1Pod> getManagedServersInNoOtherCluster(String clusterName, String adminServerName) {
     return getServers().values().stream()
           .map(ServerKubernetesObjects::getPod)
           .map(AtomicReference::get)
           .filter(this::isNotDeletingPod)
           .filter(p -> isNotAdminServer(p, adminServerName))
           .filter(p -> hasClusterNameOrNull(p, clusterName));
   }
 
   private boolean isNotDeletingPod(@Nullable V1Pod pod) {
     return Optional.ofNullable(pod).map(V1Pod::getMetadata).map(V1ObjectMeta::getDeletionTimestamp).isEmpty();
   }
 
   public void setServerService(String serverName, V1Service service) {
     getSko(serverName).getService().set(service);
   }
 
   private ServerKubernetesObjects getSko(String serverName) {
     return getServers().computeIfAbsent(serverName, (n -> new ServerKubernetesObjects()));
   }
 
   public V1Service getServerService(String serverName) {
     return getSko(serverName).getService().get();
   }
 
   V1Service removeServerService(String serverName) {
     return getSko(serverName).getService().getAndSet(null);
   }
 
   public static Optional<DomainPresenceInfo> fromPacket(Packet packet) {
     return Optional.ofNullable(packet.getSpi(DomainPresenceInfo.class));
   }
 
   /**
    * Specifies the pod associated with an operator-managed server.
    *
    * @param serverName the name of the server
    * @param pod the pod
    */
   public void setServerPod(String serverName, V1Pod pod) {
     getSko(serverName).getPod().set(pod);
   }
 
   /**
    * Returns the pod associated with an operator-managed server.
    *
    * @param serverName the name of the server
    * @return the corresponding pod, or null if none exists
    */
   public V1Pod getServerPod(String serverName) {
     return getPod(getSko(serverName));
   }
 
   /**
    * Returns a stream of all server pods present.
    *
    * @return a pod stream
    */
   public Stream<V1Pod> getServerPods() {
     return getServers().values().stream().map(this::getPod).filter(Objects::nonNull);
   }
 
   private V1Pod getPod(ServerKubernetesObjects sko) {
     return sko.getPod().get();
   }
 
   private Boolean getPodIsBeingDeleted(ServerKubernetesObjects sko) {
     return sko.isPodBeingDeleted().get();
   }
 
   public Boolean isServerPodBeingDeleted(String serverName) {
     return getPodIsBeingDeleted(getSko(serverName));
   }
 
   public void setServerPodBeingDeleted(String serverName, Boolean isBeingDeleted) {
     getSko(serverName).isPodBeingDeleted().set(isBeingDeleted);
   }
 
   /**
    * Returns a collection of all servers defined.
    *
    * @return the servers
    */
   public Collection<String> getServerNames() {
     return getServers().keySet();
   }
 
   /**
    * Applies an add or modify event for a server pod. If the current pod is newer than the one
    * associated with the event, ignores the event.
    *
    * @param serverName the name of the server associated with the event
    * @param event the pod associated with the event
    */
   public void setServerPodFromEvent(String serverName, V1Pod event) {
     updateStatus(serverName, event);
     getSko(serverName).getPod().accumulateAndGet(event, this::getNewerPod);
   }
 
   private void updateStatus(String serverName, V1Pod event) {
     getSko(serverName)
         .getLastKnownStatus()
         .getAndUpdate(
             lastKnownStatus -> {
               LastKnownStatus updatedStatus = lastKnownStatus;
               if (PodHelper.isReady(event)) {
                 if (lastKnownStatus == null
                     || !WebLogicConstants.RUNNING_STATE.equals(lastKnownStatus.getStatus())) {
                   updatedStatus = new LastKnownStatus(WebLogicConstants.RUNNING_STATE);
                 }
               } else {
                 if (lastKnownStatus != null
                     && WebLogicConstants.RUNNING_STATE.equals(lastKnownStatus.getStatus())) {
                   updatedStatus = null;
                 }
               }
               return updatedStatus;
             });
   }
 
   private V1Pod getNewerPod(V1Pod first, V1Pod second) {
     return KubernetesUtils.isFirstNewer(getMetadata(first), getMetadata(second)) ? first : second;
   }
 
   private V1ObjectMeta getMetadata(V1Pod pod) {
     return pod == null ? null : pod.getMetadata();
   }
 
   private V1ObjectMeta getMetadata(V1Service service) {
     return service == null ? null : service.getMetadata();
   }
 
   private V1ObjectMeta getMetadata(V1beta1PodDisruptionBudget pdb) {
     return pdb == null ? null : pdb.getMetadata();
   }
 
   /**
    * Computes the result of a delete attempt. If the current pod is newer than the one associated
    * with the delete event, returns it; otherwise returns null, thus deleting the value.
    *
    * @param serverName the server name associated with the pod
    * @param event the pod associated with the delete event
    * @return the new value for the pod.
    */
   public boolean deleteServerPodFromEvent(String serverName, V1Pod event) {
     if (serverName == null) {
       return false;
     }
     ServerKubernetesObjects sko = getSko(serverName);
     V1Pod deletedPod = sko.getPod().getAndAccumulate(event, this::getNewerCurrentOrNull);
     if (deletedPod != null) {
       sko.getLastKnownStatus().set(new LastKnownStatus(WebLogicConstants.SHUTDOWN_STATE));
     }
     return deletedPod != null;
   }
 
   private V1Pod getNewerCurrentOrNull(V1Pod pod, V1Pod event) {
     return KubernetesUtils.isFirstNewer(getMetadata(pod), getMetadata(event)) ? pod : null;
   }
 
   /**
    * Computes the result of a delete attempt. If the current service is newer than the one
    * associated with the delete event, returns it; otherwise returns null, thus deleting the value.
    *
    * @param service the current service
    * @param event the service associated with the delete event
    * @return the new value for the service.
    */
   private V1Service getNewerCurrentOrNull(V1Service service, V1Service event) {
     return KubernetesUtils.isFirstNewer(getMetadata(service), getMetadata(event)) ? service : null;
   }
 
   /**
    * Returns the last status reported for the specified server.
    *
    * @param serverName the name of the server
    * @return the corresponding reported status
    */
   public LastKnownStatus getLastKnownServerStatus(String serverName) {
     return getSko(serverName).getLastKnownStatus().get();
   }
 
 
/** Updates the last status reported for the specified server. */

public void updateLastKnownServerStatus(String serverName, String status) {
  getSko(serverName).getLastKnownStatus().set(new LastKnownStatus(status));
}
 

}