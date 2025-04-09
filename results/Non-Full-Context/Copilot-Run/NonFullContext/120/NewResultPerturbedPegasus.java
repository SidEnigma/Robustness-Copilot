// Copyright (c) 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.operator.steps;
 
 import java.net.http.HttpRequest;
 import java.net.http.HttpResponse;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.List;
 import java.util.Optional;
 import java.util.concurrent.TimeUnit;
 import java.util.function.Function;
 import java.util.stream.Collectors;
 import javax.annotation.Nonnull;
 
 import io.kubernetes.client.openapi.models.V1Pod;
 import io.kubernetes.client.openapi.models.V1PodSpec;
 import io.kubernetes.client.openapi.models.V1Service;
 import io.kubernetes.client.openapi.models.V1ServicePort;
 import io.kubernetes.client.openapi.models.V1ServiceSpec;
 import oracle.kubernetes.operator.KubernetesConstants;
 import oracle.kubernetes.operator.helpers.DomainPresenceInfo;
 import oracle.kubernetes.operator.helpers.PodHelper;
 import oracle.kubernetes.operator.helpers.SecretHelper;
 import oracle.kubernetes.operator.http.HttpResponseStep;
 import oracle.kubernetes.operator.logging.LoggingContext;
 import oracle.kubernetes.operator.wlsconfig.PortDetails;
 import oracle.kubernetes.operator.work.NextAction;
 import oracle.kubernetes.operator.work.Packet;
 import oracle.kubernetes.operator.work.Step;
 import oracle.kubernetes.weblogic.domain.model.Domain;
 import oracle.kubernetes.weblogic.domain.model.MonitoringExporterConfiguration;
 
 import static oracle.kubernetes.operator.ProcessingConstants.SERVER_NAME;
 import static oracle.kubernetes.operator.steps.HttpRequestProcessing.createRequestStep;
 
 public class MonitoringExporterSteps {
 
   /** Time in seconds to wait to recheck for ready state. **/
   private static final int READY_RECHECK_INTERVAL = 2;
 
 
/** If necessary, it creates a step to initiate exporter processing for all the server in the domain that has a configuration defined. */

public static Step updateExporterSidecars() {
    return new Step() {
        @Override
        public NextAction apply(Packet packet) {
            DomainPresenceInfo info = packet.getSPI(DomainPresenceInfo.class);
            Domain domain = info.getDomain();
            MonitoringExporterConfiguration exporterConfig = domain.getMonitoringExporterConfiguration();

            if (exporterConfig == null || exporterConfig.getServers().isEmpty()) {
                return doNext(packet);
            }

            List<Step> steps = new ArrayList<>();

            for (String serverName : exporterConfig.getServers()) {
                Step step = createExporterSidecarStep(serverName);
                steps.add(step);
            }

            return doForkJoin(steps, packet);
        }

        private Step createExporterSidecarStep(String serverName) {
            return new Step() {
                @Override
                public NextAction apply(Packet packet) {
                    DomainPresenceInfo info = packet.getSPI(DomainPresenceInfo.class);
                    Domain domain = info.getDomain();
                    MonitoringExporterConfiguration exporterConfig = domain.getMonitoringExporterConfiguration();

                    if (exporterConfig == null) {
                        return doNext(packet);
                    }

                    V1Pod pod = info.getServerPod(serverName);
                    V1PodSpec podSpec = pod.getSpec();
                    V1Service service = info.getServerService(serverName);
                    V1ServiceSpec serviceSpec = service.getSpec();

                    List<V1ServicePort> ports = serviceSpec.getPorts();
                    List<PortDetails> portDetails = ports.stream()
                            .map(PortDetails::fromServicePort)
                            .collect(Collectors.toList());

                    // Add logic to create exporter sidecar container and update pod spec

                    return doNext(packet);
                }
            };
        }
    };
}
 

}