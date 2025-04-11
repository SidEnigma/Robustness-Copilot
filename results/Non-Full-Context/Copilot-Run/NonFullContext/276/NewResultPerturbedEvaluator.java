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
 
 
/** From a specific pod, reads the WebLogic server state in an asyncronous step */

private static Step createServerStatusReaderStep(DomainPresenceInfo info, V1Pod pod, String serverName, long timeoutSeconds) {
    return new Step() {
        @Override
        public NextAction apply(Packet packet) {
            LOGGER.entering();

            try {
                String namespace = info.getNamespace();
                String domainUid = info.getDomainUid();
                String podName = pod.getMetadata().getName();

                // Create a unique key for the server status
                String serverStatusKey = OperatorUtils.getServerStatusKey(namespace, domainUid, serverName);

                // Check if the server status is already known
                LastKnownStatus lastKnownStatus = (LastKnownStatus) packet.get(SERVER_STATE_MAP).get(serverStatusKey);
                if (lastKnownStatus != null && !lastKnownStatus.isExpired()) {
                    LOGGER.fine(MessageKeys.SERVER_STATE_ALREADY_KNOWN, serverName);
                    return doNext(packet);
                }

                // Create a new step to read the server health
                Step readHealthStep = STEP_FACTORY.apply(this);

                // Create a new packet for the read health step
                Packet readHealthPacket = packet.clone();
                readHealthPacket.put(SERVER_HEALTH_MAP, new ConcurrentHashMap<>());
                readHealthPacket.put(SERVER_STATE_MAP, new ConcurrentHashMap<>());
                readHealthPacket.put(LoggingContext.LOGGING_CONTEXT_KEY, LoggingContext.setThreadContext().namespace(namespace).domainUid(domainUid).pod(podName));

                // Execute the read health step asynchronously
                readHealthStep.execute(readHealthPacket);

                // Schedule a timeout step to handle the case when the read health step takes too long
                Step timeoutStep = createTimeoutStep(timeoutSeconds, readHealthStep, serverName);
                timeoutStep.execute(packet);

                LOGGER.exiting();
                return doNext(packet);
            } catch (Exception e) {
                LOGGER.warning(MessageKeys.EXCEPTION, e);
                return doNext(packet);
            }
        }
    };
}
 

}