// Copyright (c) 2017, 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.operator.helpers;
 
 import io.kubernetes.client.openapi.ApiException;
 import io.kubernetes.client.openapi.models.V1TokenReview;
 import io.kubernetes.client.openapi.models.V1TokenReviewSpec;
 import io.kubernetes.client.openapi.models.V1TokenReviewStatus;
 import oracle.kubernetes.operator.logging.LoggingFacade;
 import oracle.kubernetes.operator.logging.LoggingFactory;
 import oracle.kubernetes.operator.logging.MessageKeys;
 
 /** Delegate authentication decisions to Kubernetes. */
 public class AuthenticationProxy {
   private static final LoggingFacade LOGGER = LoggingFactory.getLogger("Operator", "Operator");
   @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
   private static AuthorizationProxy authorizationProxy = new AuthorizationProxy();
 
 
/** Check if the specified access token can be authenticated. */
 public V1TokenReviewStatus check(String principal, String token, String namespace){
    V1TokenReview tokenReview = new V1TokenReview();
    tokenReview.setApiVersion("authentication.k8s.io/v1");
    tokenReview.setKind("TokenReview");
    tokenReview.setSpec(new V1TokenReviewSpec());
    tokenReview.getSpec().setToken(token);
    tokenReview.getSpec().setAudiences(new String[] { "system:authentication:kubernetes:oidc" });
    tokenReview.getSpec().setGroups(new String[] { "system:authenticated" });
    tokenReview.getSpec().setUsername(principal);
    tokenReview.getSpec().setNamespace(namespace);
    V1TokenReviewStatus status = null;
    try {
      status = authorizationProxy.check(tokenReview);
    } catch (ApiException e) {
      LOGGER.severe(MessageKeys.AUTHENTICATION_FAILED, e.getCode(), e.getResponseBody(), e.getMessage());
    }
    return status;
  }   
 }

 

}