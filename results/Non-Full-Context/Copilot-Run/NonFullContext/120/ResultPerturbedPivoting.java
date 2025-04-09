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
 
 
/** Creates a step to start processing all servers in the domain for which a configuration is defined, checking the configuration of each exporting sidecar and updating it as needed. */
 public static Step updateExporterSidecars(){
    return new Step("updateExporterSidecars") {
      @Override
      public NextAction apply(Packet packet) {
        LoggingContext context = LoggingContext.builder().logger(this.getClass()).packet(packet).build();
        DomainPresenceInfo info = packet.get(DomainPresenceInfo.class);
        Domain domain = info.getDomain();
        MonitoringExporterConfiguration config = domain.getMonitoringExporterConfiguration();
        if (config == null) {
          return NextAction.CONTINUE;
        }
        List<V1Pod> pods = info.getPods();
        List<V1Service> services = info.getServices();
        List<V1Service> newServices = new ArrayList<>();
        List<V1Service> updatedServices = new ArrayList<>();
        List<V1Service> deletedServices = new ArrayList<>();
        List<V1Service> existingServices = new ArrayList<>();
        List<V1Service> servicesToDelete = new ArrayList<>();
        List<V1Service> servicesToUpdate = new ArrayList<>();
        List<V1Service> servicesToCreate = new ArrayList<>();
        List<V1Service> servicesToRecreate = new ArrayList<>();
        List<V1Service> servicesToKeep = new ArrayList<>();
        List<V1Service> servicesToUpdate = new ArrayList<>();
        List<V1Service> servicesToUpdateRecreate = new ArrayList<>();
        List<V1Service> servicesToUpdateKeep = new ArrayList<>();
        List<V1Service> servicesToUpdateRecreateKeep = new ArrayList<>();
        List<V1Service> servicesToUpdateRecreateDelete = new ArrayList<>();
        List<V1Service> servicesToUpdateRecreateDeleteKeep = new ArrayList<>();
        List<V1Service> servicesToUpdateRecreateDeleteKeepUpdate = new ArrayList<>();
        List<V1Service> servicesToUpdateRecreateDeleteKeepUpdateRecreate = new ArrayList<>();
        List<V1   
 }

 

}