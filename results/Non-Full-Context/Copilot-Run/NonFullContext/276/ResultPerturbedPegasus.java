// Copyright (c) 2018, 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.operator;
 
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.Reader;
 import java.util.Collection;
 import java.util.Optional;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ConcurrentMap;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.function.Function;
 import java.util.stream.Collectors;
 import javax.annotation.Nonnull;
 
 import io.kubernetes.client.openapi.ApiClient;
 import io.kubernetes.client.openapi.ApiException;
 import io.kubernetes.client.openapi.models.V1ObjectMeta;
 import io.kubernetes.client.openapi.models.V1Pod;
 import oracle.kubernetes.operator.helpers.ClientPool;
 import oracle.kubernetes.operator.helpers.DomainPresenceInfo;
 import oracle.kubernetes.operator.helpers.KubernetesUtils;
 import oracle.kubernetes.operator.helpers.LastKnownStatus;
 import oracle.kubernetes.operator.helpers.PodHelper;
 import oracle.kubernetes.operator.logging.LoggingContext;
 import oracle.kubernetes.operator.logging.LoggingFacade;
 import oracle.kubernetes.operator.logging.LoggingFactory;
 import oracle.kubernetes.operator.logging.MessageKeys;
 import oracle.kubernetes.operator.steps.ReadHealthStep;
 import oracle.kubernetes.operator.utils.KubernetesExec;
 import oracle.kubernetes.operator.utils.KubernetesExecFactory;
 import oracle.kubernetes.operator.utils.KubernetesExecFactoryImpl;
 import oracle.kubernetes.operator.work.NextAction;
 import oracle.kubernetes.operator.work.Packet;
 import oracle.kubernetes.operator.work.Step;
 import oracle.kubernetes.utils.OperatorUtils;
 import oracle.kubernetes.utils.SystemClock;
 import oracle.kubernetes.weblogic.domain.model.ServerHealth;
 
 import static oracle.kubernetes.operator.KubernetesConstants.WLS_CONTAINER_NAME;
 import static oracle.kubernetes.operator.ProcessingConstants.SERVER_HEALTH_MAP;
 import static oracle.kubernetes.operator.ProcessingConstants.SERVER_STATE_MAP;
 
 /** Creates an asynchronous step to read the WebLogic server state from a particular pod. */
 public class ServerStatusReader {
   private static final LoggingFacade LOGGER = LoggingFactory.getLogger("Operator", "Operator");
   private static final KubernetesExecFactory EXEC_FACTORY = new KubernetesExecFactoryImpl();
   private static final Function<Step, Step> STEP_FACTORY = ReadHealthStep::createReadHealthStep;
 
   private ServerStatusReader() {
   }
 
   static Step createDomainStatusReaderStep(
       DomainPresenceInfo info, long timeoutSeconds, Step next) {
     return new DomainStatusReaderStep(info, timeoutSeconds, next);
   }
 
 
/** A step is created to read the server state from a particular Pod. */
 private static Step createServerStatusReaderStep(DomainPresenceInfo info, V1Pod pod, String serverName, long timeoutSeconds){
    return new ServerStatusReaderStep(info, pod, serverName, timeoutSeconds);
  }
  
    static Step createServerStatusReaderStep(DomainPresenceInfo info, V1Pod pod, String serverName, long timeoutSeconds, Step next) {
      return new ServerStatusReaderStep(info, pod, serverName, timeoutSeconds, next);
    }
  
    static Step createServerStatusReaderStep(DomainPresenceInfo info, V1Pod pod, String serverName, long timeoutSeconds, Step next,
        ConcurrentMap<String, ServerHealth> serverHealthMap, ConcurrentMap<String, String> serverStateMap) {
      return new ServerStatusReaderStep(info, pod, serverName, timeoutSeconds, next, serverHealthMap, serverStateMap);
    }
  
    static Step createServerStatusReaderStep(DomainPresenceInfo info, V1Pod pod, String serverName, long timeoutSeconds, Step next,
        ConcurrentMap<String, ServerHealth> serverHealthMap, ConcurrentMap<String, String> serverStateMap,
        ConcurrentMap<String, LastKnownStatus> lastKnownStatusMap) {
      return new ServerStatusReaderStep(info, pod, serverName, timeoutSeconds, next, serverHealthMap, serverStateMap, lastKnownStatusMap);
    }
  
    static Step createServerStatusReaderStep(DomainPresenceInfo info, V1Pod pod, String serverName, long timeoutSeconds, Step next,
        ConcurrentMap<String, ServerHealth> serverHealthMap, ConcurrentMap<String, String> serverStateMap,
        ConcurrentMap<String, LastKnownStatus> lastKnownStatusMap, ConcurrentMap<String, Long> lastKnownStatusTimeMap) {
      return new ServerStatusReaderStep(info, pod, serverName, timeoutSeconds, next, serverHealthMap, serverStateMap, lastKnownStatusMap,
          lastKnownStatusTimeMap);
    }
  
    static Step createServerStatusReaderStep(DomainPresenceInfo info, V1Pod pod, String serverName, long timeoutSeconds, Step next,
        ConcurrentMap<String, ServerHealth> serverHealthMap, ConcurrentMap<String, String> serverStateMap,    
 }

 

}