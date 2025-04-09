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
 
 
/** Creates a step to initiate processing for all servers in the domain for which a configuration is defined,  checking the configuration of each exporter sidecar and updating it if necessary. */

public static Step updateExporterSidecars() {
    return new Step() {
        @Override
        public NextAction apply(Packet packet) {
            DomainPresenceInfo info = packet.getSPI(DomainPresenceInfo.class);
            Domain domain = info.getDomain();
            MonitoringExporterConfiguration exporterConfig = domain.getMonitoringExporterConfiguration();

            List<Step> steps = new ArrayList<>();

            // Iterate over all servers in the domain
            for (String serverName : domain.getServerConfigs().keySet()) {
                // Check if exporter sidecar configuration is defined for the server
                if (exporterConfig != null && exporterConfig.getServers().containsKey(serverName)) {
                    // Get the existing exporter sidecar configuration for the server
                    MonitoringExporterConfiguration.ServerConfig serverConfig = exporterConfig.getServers().get(serverName);

                    // Check if the exporter sidecar is already running
                    if (serverConfig.isRunning()) {
                        // Check if the exporter sidecar configuration needs to be updated
                        if (serverConfig.needsUpdate()) {
                            // Create a step to update the exporter sidecar configuration
                            Step updateStep = createUpdateStep(serverName, serverConfig);
                            steps.add(updateStep);
                        }
                    } else {
                        // Create a step to start the exporter sidecar
                        Step startStep = createStartStep(serverName, serverConfig);
                        steps.add(startStep);
                    }
                }
            }

            // Create a parallel step to execute all the update/start steps
            Step parallelStep = Step.chain(steps.toArray(new Step[0]));

            // Return the parallel step as the next step
            return doNext(parallelStep, packet);
        }

        private Step createUpdateStep(String serverName, MonitoringExporterConfiguration.ServerConfig serverConfig) {
            // TODO: Implement the logic to update the exporter sidecar configuration
            // ...

            return null; // Replace with the actual update step
        }

        private Step createStartStep(String serverName, MonitoringExporterConfiguration.ServerConfig serverConfig) {
            // TODO: Implement the logic to start the exporter sidecar
            // ...

            return null; // Replace with the actual start step
        }
    };
}
 

}