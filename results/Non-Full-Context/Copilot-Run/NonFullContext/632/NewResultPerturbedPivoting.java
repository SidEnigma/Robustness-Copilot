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
     
     /**
      * Generate the OAuth2 Provider URL to be used in the login page link for the provider.
      * @param idpId Unique ID for the provider (used to lookup in authn service bean)
      * @param redirectPage page part of URL where we should be redirected after login (e.g. "dataverse.xhtml")
      * @return A generated link for the OAuth2 provider login
      */
     public String linkFor(String idpId, String redirectPage) {
         AbstractOAuth2AuthenticationProvider idp = authenticationSvc.getOAuth2Provider(idpId);
         String state = createState(idp, toOption(redirectPage));
         return idp.buildAuthzUrl(state, systemConfig.getOAuth2CallbackUrl());
     }
     
     /**
      * View action for callback.xhtml, the browser redirect target for the OAuth2 provider.
      * @throws IOException
      */
     public void exchangeCodeForToken() throws IOException {
         HttpServletRequest req = Faces.getRequest();
         
         try {
             Optional<AbstractOAuth2AuthenticationProvider> oIdp = parseStateFromRequest(req.getParameter("state"));
             Optional<String> code = parseCodeFromRequest(req);
 
             if (oIdp.isPresent() && code.isPresent()) {
                 AbstractOAuth2AuthenticationProvider idp = oIdp.get();
                 oauthUser = idp.getUserRecord(code.get(), systemConfig.getOAuth2CallbackUrl());
                 
                 UserRecordIdentifier idtf = oauthUser.getUserRecordIdentifier();
                 AuthenticatedUser dvUser = authenticationSvc.lookupUser(idtf);
     
                 if (dvUser == null) {
                     // need to create the user
                     newAccountPage.setNewUser(oauthUser);
                     Faces.redirect("/oauth2/firstLogin.xhtml");
         
                 } else {
                     // login the user and redirect to HOME of intended page (if any).
                     // setUser checks for deactivated users.
                     session.setUser(dvUser);
                     final OAuth2TokenData tokenData = oauthUser.getTokenData();
                     if (tokenData != null) {
                         tokenData.setUser(dvUser);
                         tokenData.setOauthProviderId(idp.getId());
                         oauth2Tokens.store(tokenData);
                     }
                     
                     Faces.redirect(redirectPage.orElse("/"));
                 }
             }
         } catch (OAuth2Exception ex) {
             error = ex;
             logger.log(Level.INFO, "OAuth2Exception caught. HTTP return code: {0}. Message: {1}. Message body: {2}", new Object[]{error.getHttpReturnCode(), error.getLocalizedMessage(), error.getMessageBody()});
             Logger.getLogger(OAuth2LoginBackingBean.class.getName()).log(Level.SEVERE, null, ex);
         } catch (InterruptedException | ExecutionException ex) {
             error = new OAuth2Exception(-1, "Please see server logs for more details", "Could not login due to threading exceptions.");
             logger.log(Level.WARNING, "Threading exception caught. Message: {0}", ex.getLocalizedMessage());
         }
     }
     
     /**
      * TODO: Refactor this to be included in calling method.
      * TODO: Use org.apache.commons.io.IOUtils.toString(req.getReader()) instead of overcomplicated code below.
      */
     private Optional<String> parseCodeFromRequest(@NotNull HttpServletRequest req) {
         String code = req.getParameter("code");
         if (code == null || code.trim().isEmpty()) {
             try (BufferedReader rdr = req.getReader()) {
                 StringBuilder sb = new StringBuilder();
                 String line;
                 while ((line = rdr.readLine()) != null) {
                     sb.append(line).append("\n");
                 }
                 error = new OAuth2Exception(-1, sb.toString(), "Remote system did not return an authorization code.");
                 logger.log(Level.INFO, "OAuth2Exception getting code parameter. HTTP return code: {0}. Message: {1} Message body: {2}", new Object[]{error.getHttpReturnCode(), error.getLocalizedMessage(), error.getMessageBody()});
                 return Optional.empty();
             } catch (IOException e) {
                 error = new OAuth2Exception(-1, "", "Could not parse OAuth2 code due to IO error.");
                 logger.log(Level.WARNING, "IOException getting code parameter.", e.getLocalizedMessage());
                 return Optional.empty();
             }
         }
         return Optional.of(code);
     }
     
 
/** Analyze and check the status returned by the provider. */

private Optional<AbstractOAuth2AuthenticationProvider> parseStateFromRequest(@NotNull String state) {
    try {
        String[] parts = state.split(":");
        if (parts.length != 2) {
            error = new OAuth2Exception(-1, "", "Invalid state parameter received.");
            logger.log(Level.WARNING, "Invalid state parameter received: {0}", state);
            return Optional.empty();
        }
        String idpId = parts[0];
        String redirectPage = parts[1];
        AbstractOAuth2AuthenticationProvider idp = authenticationSvc.getOAuth2Provider(idpId);
        if (idp == null) {
            error = new OAuth2Exception(-1, "", "Invalid OAuth2 provider ID.");
            logger.log(Level.WARNING, "Invalid OAuth2 provider ID: {0}", idpId);
            return Optional.empty();
        }
        this.redirectPage = Optional.of(redirectPage);
        return Optional.of(idp);
    } catch (Exception ex) {
        error = new OAuth2Exception(-1, "", "Error parsing state parameter.");
        logger.log(Level.WARNING, "Error parsing state parameter: {0}", state);
        return Optional.empty();
    }
}
 

}