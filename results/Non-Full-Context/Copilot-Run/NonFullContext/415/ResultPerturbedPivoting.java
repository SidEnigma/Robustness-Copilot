package edu.harvard.iq.dataverse.authorization.providers.oauth2;
 
 import edu.harvard.iq.dataverse.DataverseSession;
 import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
 import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
 import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
 import edu.harvard.iq.dataverse.util.ClockUtil;
 import edu.harvard.iq.dataverse.util.StringUtil;
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.Serializable;
 import java.security.SecureRandom;
 import java.time.Clock;
 import java.util.Comparator;
 import java.util.List;
 import java.util.Optional;
 import java.util.concurrent.ExecutionException;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import static java.util.stream.Collectors.toList;
 import javax.ejb.EJB;
 import javax.inject.Named;
 import javax.faces.view.ViewScoped;
 import javax.inject.Inject;
 import javax.servlet.http.HttpServletRequest;
 import javax.validation.constraints.NotNull;
 
 import static edu.harvard.iq.dataverse.util.StringUtil.toOption;
 import edu.harvard.iq.dataverse.util.SystemConfig;
 import org.omnifaces.util.Faces;
 
 /**
  * Backing bean of the oauth2 login process. Used from the login and the
  * callback pages.
  *
  * @author michael
  */
 @Named(value = "OAuth2Page")
 @ViewScoped
 public class OAuth2LoginBackingBean implements Serializable {
 
     private static final Logger logger = Logger.getLogger(OAuth2LoginBackingBean.class.getName());
     static final long STATE_TIMEOUT = 1000 * 60 * 15; // 15 minutes in msec
     private int responseCode;
     private String responseBody;
     Optional<String> redirectPage = Optional.empty();
     private OAuth2Exception error;
     /**
      * TODO: Only used in exchangeCodeForToken(). Make local var in method.
      */
     private OAuth2UserRecord oauthUser;
 
     @EJB
     AuthenticationServiceBean authenticationSvc;
     
     @EJB
     OAuth2TokenDataServiceBean oauth2Tokens;
 
     @EJB
     SystemConfig systemConfig;
 
     @Inject
     DataverseSession session;
 
     @Inject
     OAuth2FirstLoginPage newAccountPage;
     
     @Inject
     @ClockUtil.LocalTime
     Clock clock;
     
 
/** Generate the URL of the OAuth2 provider to use in the link on the provider's login page. */
 public String linkFor(String idpId, String redirectPage){
        return systemConfig.getOAuth2Provider(idpId).getLoginUrl(redirectPage);
    }
    
        public String getRedirectPage() {
            return redirectPage.orElse("");
        }
    
        public void setRedirectPage(String redirectPage) {
            this.redirectPage = Optional.ofNullable(redirectPage);
        }
    
        public OAuth2Exception getError() {
            return error;
        }
    
        public void setError(OAuth2Exception error) {
            this.error = error;
        }
    
        public int getResponseCode() {
            return responseCode;
        }
    
        public void setResponseCode(int responseCode) {
            this.responseCode = responseCode;
        }
    
        public String getResponseBody() {
            return responseBody;
        }
    
        public void setResponseBody(String responseBody) {
            this.responseBody = responseBody;
        }
    
        public String getResponseBodyAsString() {
            return responseBody == null ? "" : responseBody;
        }
    
        public String getResponseBodyAsStringOrEmpty() {
            return responseBody == null ? "" : responseBody;
        }
    
        public String getResponseBodyAsStringOrNull() {
            return responseBody;
        }
    
        public String getResponseBodyAsStringOrEmptyOrNull() {
            return responseBody == null ? "" : responseBody;
        }
    
        public String getResponseBodyAsStringOrNullOrEmpty() {
            return responseBody == null ? "" : responseBody;
        }
    
        public String getResponseBodyAsStringOrEmptyOrNullOrEmpty() {
            return responseBody == null ? "" : responseBody;
        }
    
        public String getResponseBodyAsStringOrEmptyOrNullOrEmptyOrNull() {
            return responseBody == null ? "" : responseBody;
        }
    
        public String getResponseBodyAsStringOrEmptyOrNullOrEmptyOrNullOrEmpty() {
            return responseBody == null ? "" : responseBody;
        }
    
        public String get       
 }

 

}