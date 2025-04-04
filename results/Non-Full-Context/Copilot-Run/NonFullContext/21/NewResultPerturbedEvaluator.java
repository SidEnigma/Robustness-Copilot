// Copyright (c) 2018, 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.operator.builders;
 
 import java.util.concurrent.TimeUnit;
 import java.util.function.BiFunction;
 
 import io.kubernetes.client.openapi.ApiClient;
 import io.kubernetes.client.openapi.ApiException;
 import io.kubernetes.client.openapi.apis.BatchV1Api;
 import io.kubernetes.client.openapi.apis.CoreV1Api;
 import io.kubernetes.client.openapi.apis.PolicyV1beta1Api;
 import io.kubernetes.client.openapi.models.CoreV1Event;
 import io.kubernetes.client.openapi.models.V1ConfigMap;
 import io.kubernetes.client.openapi.models.V1Job;
 import io.kubernetes.client.openapi.models.V1Namespace;
 import io.kubernetes.client.openapi.models.V1Pod;
 import io.kubernetes.client.openapi.models.V1Service;
 import io.kubernetes.client.openapi.models.V1beta1PodDisruptionBudget;
 import io.kubernetes.client.util.Watchable;
 import okhttp3.Call;
 import oracle.kubernetes.weblogic.domain.api.WeblogicApi;
 import oracle.kubernetes.weblogic.domain.model.Domain;
 
 import static oracle.kubernetes.utils.OperatorUtils.isNullOrEmpty;
 
 public class WatchBuilder {
   /** Always true for watches. */
   private static final boolean WATCH = true;
 
   /** Ignored for watches. */
   private static final String START_LIST = null;
 
   private static final Boolean ALLOW_BOOKMARKS = true;
 
   private static final String RESOURCE_VERSION_MATCH_UNSET = null;
 
   @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"}) // Leave non-final for unit test
   private static WatchFactory FACTORY = new WatchFactoryImpl();
 
   private final CallParamsImpl callParams = new CallParamsImpl();
 
   public WatchBuilder() {
   }
 
   /**
    * Creates a web hook object to track service calls.
    *
    * @param namespace the namespace
    * @return the active web hook
    * @throws ApiException if there is an error on the call that sets up the web hook.
    */
   public Watchable<V1Service> createServiceWatch(String namespace) throws ApiException {
     return FACTORY.createWatch(callParams, V1Service.class, new ListNamespacedServiceCall(namespace));
   }
 
   /**
    * Creates a web hook object to track pod disruption budgets.
    *
    * @param namespace the namespace
    * @return the active web hook
    * @throws ApiException if there is an error on the call that sets up the web hook.
    */
   public Watchable<V1beta1PodDisruptionBudget> createPodDisruptionBudgetWatch(String namespace) throws ApiException {
     return FACTORY.createWatch(callParams, V1beta1PodDisruptionBudget.class,
         new ListPodDisruptionBudgetCall(namespace));
   }
 
   /**
    * Creates a web hook object to track pods.
    *
    * @param namespace the namespace
    * @return the active web hook
    * @throws ApiException if there is an error on the call that sets up the web hook.
    */
   public Watchable<V1Pod> createPodWatch(String namespace) throws ApiException {
     return FACTORY.createWatch(
         callParams, V1Pod.class, new ListPodCall(namespace));
   }
 
   /**
    * Creates a web hook object to track jobs.
    *
    * @param namespace the namespace
    * @return the active web hook
    * @throws ApiException if there is an error on the call that sets up the web hook.
    */
   public Watchable<V1Job> createJobWatch(String namespace) throws ApiException {
     return FACTORY.createWatch(
         callParams, V1Job.class, new ListJobCall(namespace));
   }
 
   /**
    * Creates a web hook object to track events.
    *
    * @param namespace the namespace
    * @return the active web hook
    * @throws ApiException if there is an error on the call that sets up the web hook.
    */
   public Watchable<CoreV1Event> createEventWatch(String namespace) throws ApiException {
     return FACTORY.createWatch(
         callParams, CoreV1Event.class, new ListEventCall(namespace));
   }
 
   /**
    * Creates a web hook object to track changes to WebLogic domains in one namespaces.
    *
    * @param namespace the namespace in which to track domains
    * @return the active web hook
    * @throws ApiException if there is an error on the call that sets up the web hook.
    */
   public Watchable<Domain> createDomainWatch(String namespace) throws ApiException {
     return FACTORY.createWatch(
         callParams, Domain.class, new ListDomainsCall(namespace));
   }
 
 
/** Given the configuration map to keep track of calls a web hook object is created.   The method returns the active web hook otherwise an ApiException if    there is an error in the call set by the web hook */

public Watchable<V1ConfigMap> createConfigMapWatch(String namespace) throws ApiException {
    return FACTORY.createWatch(
        callParams, V1ConfigMap.class, new ListConfigMapCall(namespace));
}
 

}