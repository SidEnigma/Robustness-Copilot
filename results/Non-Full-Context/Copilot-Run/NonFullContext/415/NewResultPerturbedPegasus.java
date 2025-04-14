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
     
 
/** The provider's login page link will be used if the OAuth2 Provider URL is generated. */

public String linkFor(String idpId, String redirectPage) {
    try {
        // Generate a random state value
        SecureRandom random = new SecureRandom();
        String state = StringUtil.randomString(random, 30);

        // Create the OAuth2 login URL
        String loginUrl = systemConfig.getOAuth2ProviderUrl(idpId);
        loginUrl += "?response_type=code";
        loginUrl += "&client_id=" + systemConfig.getOAuth2ClientId(idpId);
        loginUrl += "&redirect_uri=" + systemConfig.getOAuth2RedirectUri(idpId);
        loginUrl += "&state=" + state;

        // Store the state value in the session
        session.put("oauth2State", state);
        session.put("oauth2RedirectPage", redirectPage);

        return loginUrl;
    } catch (Exception e) {
        logger.log(Level.SEVERE, "Error generating OAuth2 login link", e);
        return null;
    }
}
 

}