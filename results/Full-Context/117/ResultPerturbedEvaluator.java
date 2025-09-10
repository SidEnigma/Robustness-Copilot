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
 
 
/** The method verifies whether the specified access token can be authenticated. The method verifies whether the specified access token can be authenticated. Return V1TokenReviewStatus containing either info about the authenticated user or an error. */
 public V1TokenReviewStatus check(String principal, String token, String namespace){
    V1TokenReview tokenReview = new V1TokenReview();
    V1TokenReviewSpec tokenReviewSpec = new V1TokenReviewSpec();
    tokenReviewSpec.setToken(token);
    tokenReviewSpec.setAudience(namespace);
    tokenReviewSpec.setUsername(principal);
    tokenReview.setSpec(tokenReviewSpec);
    V1TokenReviewStatus tokenReviewStatus = null;
    try {
      tokenReviewStatus = authorizationProxy.check(tokenReview);
    } catch (ApiException e) {
      LOGGER.severe(MessageKeys.OPERATOR_AUTHENTICATION_FAILED, e.getCode(), e.getResponseBody(), e.getMessage());
    }
    return tokenReviewStatus;
  }   
 }

                                     
   private V1TokenReview prepareTokenReview(String token) {
     return new V1TokenReview().spec(new V1TokenReviewSpec().token(token));
   }
 }
