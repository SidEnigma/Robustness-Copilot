// Copyright (c) 2018, 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.operator.calls;
 
 import java.util.List;
 import java.util.Map;
 import java.util.Optional;
 import java.util.Random;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicBoolean;
 import java.util.function.Predicate;
 
 import io.kubernetes.client.common.KubernetesListObject;
 import io.kubernetes.client.openapi.ApiCallback;
 import io.kubernetes.client.openapi.ApiClient;
 import io.kubernetes.client.openapi.ApiException;
 import io.kubernetes.client.openapi.models.V1ListMeta;
 import oracle.kubernetes.operator.helpers.CallBuilder;
 import oracle.kubernetes.operator.helpers.ClientPool;
 import oracle.kubernetes.operator.helpers.DomainPresenceInfo;
 import oracle.kubernetes.operator.helpers.ResponseStep;
 import oracle.kubernetes.operator.logging.LoggingContext;
 import oracle.kubernetes.operator.logging.LoggingFacade;
 import oracle.kubernetes.operator.logging.LoggingFactory;
 import oracle.kubernetes.operator.logging.MessageKeys;
 import oracle.kubernetes.operator.work.AsyncFiber;
 import oracle.kubernetes.operator.work.Component;
 import oracle.kubernetes.operator.work.NextAction;
 import oracle.kubernetes.operator.work.Packet;
 import oracle.kubernetes.operator.work.Step;
 
 import static oracle.kubernetes.operator.calls.CallResponse.createFailure;
 import static oracle.kubernetes.operator.calls.CallResponse.createSuccess;
 import static oracle.kubernetes.operator.helpers.NamespaceHelper.getOperatorNamespace;
 import static oracle.kubernetes.operator.logging.MessageKeys.ASYNC_SUCCESS;
 
 /**
  * A Step driven by an asynchronous call to the Kubernetes API, which results in a series of
  * callbacks until canceled.
  */
 public class AsyncRequestStep<T> extends Step implements RetryStrategyListener {
   public static final String RESPONSE_COMPONENT_NAME = "response";
   public static final String CONTINUE = "continue";
 
   private static final Random R = new Random();
   private static final int HIGH = 200;
   private static final int LOW = 10;
   private static final int SCALE = 100;
   private static final int MAX = 10000;
   private static final LoggingFacade LOGGER = LoggingFactory.getLogger("Operator", "Operator");
 
   private final ClientPool helper;
   private final RequestParams requestParams;
   private final CallFactory<T> factory;
   private final int maxRetryCount;
   private final RetryStrategy customRetryStrategy;
   private final String fieldSelector;
   private final String labelSelector;
   private final String resourceVersion;
   private int timeoutSeconds;
 
   /**
    * Construct async step.
    *
    * @param next Next
    * @param requestParams Request parameters
    * @param factory Factory
    * @param helper Client pool
    * @param timeoutSeconds Timeout
    * @param maxRetryCount Max retry count
    * @param fieldSelector Field selector
    * @param labelSelector Label selector
    * @param resourceVersion Resource version
    */
   public AsyncRequestStep(
       ResponseStep<T> next,
       RequestParams requestParams,
       CallFactory<T> factory,
       ClientPool helper,
       int timeoutSeconds,
       int maxRetryCount,
       String fieldSelector,
       String labelSelector,
       String resourceVersion) {
     this(next, requestParams, factory, null, helper, timeoutSeconds, maxRetryCount,
             null, fieldSelector, labelSelector, resourceVersion);
   }
 
   /**
    * Construct async step.
    *
    * @param next Next
    * @param requestParams Request parameters
    * @param factory Factory
    * @param customRetryStrategy Custom retry strategy
    * @param helper Client pool
    * @param timeoutSeconds Timeout
    * @param maxRetryCount Max retry count
    * @param gracePeriodSeconds Grace period
    * @param fieldSelector Field selector
    * @param labelSelector Label selector
    * @param resourceVersion Resource version
    */
   public AsyncRequestStep(
           ResponseStep<T> next,
           RequestParams requestParams,
           CallFactory<T> factory,
           RetryStrategy customRetryStrategy,
           ClientPool helper,
           int timeoutSeconds,
           int maxRetryCount,
           Integer gracePeriodSeconds,
           String fieldSelector,
           String labelSelector,
           String resourceVersion) {
     super(next);
     this.helper = helper;
     this.requestParams = requestParams;
     this.factory = factory;
     this.customRetryStrategy = customRetryStrategy;
     this.timeoutSeconds = timeoutSeconds;
     this.maxRetryCount = maxRetryCount;
     this.fieldSelector = fieldSelector;
     this.labelSelector = labelSelector;
     this.resourceVersion = resourceVersion;
 
     // TODO, RJE: consider reimplementing the connection between the response and request steps using just
     // elements in the packet so that all step implementations are stateless.
     next.setPrevious(this);
   }
 
 
/** From a list containing metadata, access the continue field. */
 public static String accessContinue(Object result){
    if (result instanceof KubernetesListObject) {
      KubernetesListObject list = (KubernetesListObject) result;
      V1ListMeta meta = list.getMetadata();
      if (meta != null) {
        return meta.getContinue();
      }
    }
    return null;    
 }

 

}