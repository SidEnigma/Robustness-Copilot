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
 
 
/** If the access token can be verified, check it. */
 public V1TokenReviewStatus check(String principal, String token, String namespace){
    V1TokenReviewStatus status = new V1TokenReviewStatus();
    status.setAuthenticated(false);
    status.setError("");
    status.setUser("");
    try {
      V1TokenReview tokenReview = new V1TokenReview();
      V1TokenReviewSpec tokenReviewSpec = new V1TokenReviewSpec();
      tokenReviewSpec.setToken(token);
      tokenReview.setSpec(tokenReviewSpec);
      tokenReview = authorizationProxy.getClient().createTokenReview(tokenReview, namespace);
      if (tokenReview.getStatus() != null) {
        status.setAuthenticated(tokenReview.getStatus().getAuthenticated());
        status.setUser(tokenReview.getStatus().getUser());
      }
    } catch (ApiException e) {
      LOGGER.severe(MessageKeys.AUTHENTICATION_FAILED, e.getCode(), e.getResponseBody(), e.getMessage());
      status.setError(e.getMessage());
    }
    return status;
  }   
 }

 

}