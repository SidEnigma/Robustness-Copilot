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

public V1TokenReviewStatus check(String principal, String token, String namespace) {
    V1TokenReview tokenReview = new V1TokenReview();
    V1TokenReviewSpec spec = new V1TokenReviewSpec();
    spec.setToken(token);
    tokenReview.setSpec(spec);

    try {
        V1TokenReview result = authorizationProxy.createTokenReview(tokenReview, namespace);
        V1TokenReviewStatus status = result.getStatus();

        if (status != null && status.getAuthenticated()) {
            LOGGER.info(MessageKeys.TOKEN_VERIFIED, principal);
            return status;
        } else {
            LOGGER.warning(MessageKeys.TOKEN_NOT_VERIFIED, principal);
            return null;
        }
    } catch (ApiException e) {
        LOGGER.warning(MessageKeys.TOKEN_VERIFICATION_FAILED, principal, e.getMessage());
        return null;
    }
}
 

}