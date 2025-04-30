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
 
 
/** Creates a web hook object to track service calls. */
 public Watchable<V1Service> createServiceWatch(String namespace) throws ApiException{}

 

}