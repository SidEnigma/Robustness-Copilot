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
 
 
/** It allows the creation of a pass to start processing all servers in the domain, checking, if defined, the configuration of each export sidecar and updating it if necessary. */
 public static Step updateExporterSidecars(){
    return new Step() {
      @Override
      public NextAction apply(Packet packet) {
        LoggingContext context = LoggingContext.builder().loggerName(this.getClass().getName()).build();
        DomainPresenceInfo info = packet.get(DomainPresenceInfo.class);
        Domain domain = info.getDomain();
        MonitoringExporterConfiguration config = domain.getMonitoringExporterConfiguration();
        if (config == null) {
          return NextAction.CONTINUE;
        }
        List<V1Pod> pods = info.getPods();
        if (pods == null || pods.isEmpty()) {
          return NextAction.CONTINUE;
        }
        List<V1Pod> exporterPods = pods.stream()
          .filter(pod -> pod.getMetadata().getName().startsWith(KubernetesConstants.EXPORTER_POD_PREFIX))
          .collect(Collectors.toList());
        if (exporterPods.isEmpty()) {
          return NextAction.CONTINUE;
        }
        List<V1Pod> exporterPodsToUpdate = new ArrayList<>();
        for (V1Pod exporterPod : exporterPods) {
          V1PodSpec spec = exporterPod.getSpec();
          if (spec == null) {
            continue;
          }
          List<V1Container> containers = spec.getContainers();
          if (containers == null || containers.isEmpty()) {
            continue;
          }
          V1Container container = containers.get(0);
          if (container == null) {
            continue;
          }
          List<V1EnvVar> envVars = container.getEnv();
          if (envVars == null || envVars.isEmpty()) {
            continue;
          }
          V1EnvVar envVar = envVars.stream()
            .filter(env -> env.getName().equals(KubernetesConstants.EXPORTER_ENV_VAR_NAME))
            .findFirst()
            .orElse(null);    
 }

 

}