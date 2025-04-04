package edu.harvard.iq.dataverse.authorization.providers.builtin;
 
 import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
 import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
 import edu.harvard.iq.dataverse.authorization.AuthenticationRequest;
 import edu.harvard.iq.dataverse.authorization.AuthenticationResponse;
 import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
 import edu.harvard.iq.dataverse.authorization.CredentialsAuthenticationProvider;
 import java.util.Arrays;
 import java.util.List;
 import static edu.harvard.iq.dataverse.authorization.CredentialsAuthenticationProvider.Credential;
 import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
 import edu.harvard.iq.dataverse.passwordreset.PasswordResetException;
 import edu.harvard.iq.dataverse.util.BundleUtil;
 import edu.harvard.iq.dataverse.validation.PasswordValidatorServiceBean;
 
 /**
  * An authentication provider built into the application. Uses JPA and the 
  * local database to store the users.
  * 
  * @author michael
  */
 public class BuiltinAuthenticationProvider implements CredentialsAuthenticationProvider {
     
     public static final String PROVIDER_ID = "builtin";
     /**
      * TODO: Think more about if it really makes sense to have the key for a
      * credential be a Bundle key. What if we want to reorganize our Bundle
      * files and rename some Bundle keys? Would login be broken until we update
      * the strings below?
      */
     public static final String KEY_USERNAME_OR_EMAIL = "login.builtin.credential.usernameOrEmail";
     public static final String KEY_PASSWORD = "login.builtin.credential.password";
     private static List<Credential> CREDENTIALS_LIST;
       
     final BuiltinUserServiceBean bean;
     final AuthenticationServiceBean authBean;
     private PasswordValidatorServiceBean passwordValidatorService;
 
     public BuiltinAuthenticationProvider( BuiltinUserServiceBean aBean, PasswordValidatorServiceBean passwordValidatorService, AuthenticationServiceBean auBean  ) {
         this.bean = aBean;
         this.authBean = auBean;
         this.passwordValidatorService = passwordValidatorService;
         CREDENTIALS_LIST = Arrays.asList(new Credential(KEY_USERNAME_OR_EMAIL), new Credential(KEY_PASSWORD, true));
     }
 
     @Override
     public String getId() {
         return PROVIDER_ID;
     }
 
     @Override
     public AuthenticationProviderDisplayInfo getInfo() {
         return new AuthenticationProviderDisplayInfo(getId(), BundleUtil.getStringFromBundle("auth.providers.title.builtin"), "Internal user repository");
     }
 
     @Override
     public boolean isPasswordUpdateAllowed() {
         return true;
     }
 
     @Override
     public boolean isUserInfoUpdateAllowed() {
         return true;
     }
 
     @Override
     public boolean isUserDeletionAllowed() {
         return true;
     }
     
     @Override
     public void deleteUser(String userIdInProvider) {
         bean.removeUser(userIdInProvider);
     }
     
     @Override
     public void updatePassword(String userIdInProvider, String newPassword) {
         BuiltinUser biUser = bean.findByUserName( userIdInProvider  );
         biUser.updateEncryptedPassword(PasswordEncryption.get().encrypt(newPassword),
                                        PasswordEncryption.getLatestVersionNumber());
         bean.save(biUser);
     }
     
 
/** Validates that the password passed is the user's password. */
 public Boolean verifyPassword(String userIdInProvider, String password){
        BuiltinUser biUser = bean.findByUserName( userIdInProvider  );
        return PasswordEncryption.get().verify(password, biUser.getEncryptedPassword(), biUser.getPasswordEncryptionVersion());
    }
    
        @Override
        public void updateUserInfo(String userIdInProvider, String firstName, String lastName, String affiliation) {
            BuiltinUser biUser = bean.findByUserName( userIdInProvider  );
            biUser.setFirstName(firstName);
            biUser.setLastName(lastName);
            biUser.setAffiliation(affiliation);
            bean.save(biUser);
        }
    
        @Override
        public List<Credential> getCredentials() {
            return CREDENTIALS_LIST;
        }
    
        @Override
        public AuthenticationResponse authenticate(AuthenticationRequest authReq) {
            String userIdInProvider = authReq.getCredential(KEY_USERNAME_OR_EMAIL);
            String password = authReq.getCredential(KEY_PASSWORD);
            if (userIdInProvider == null || password == null) {
                return new AuthenticationResponse(AuthenticationResponse.Status.INVALID_CREDENTIALS, BundleUtil.getStringFromBundle("login.builtin.error.invalidCredentials"));
            }
            BuiltinUser biUser = bean.findByUserName( userIdInProvider  );
            if (biUser == null) {
                return new AuthenticationResponse(AuthenticationResponse.Status.INVALID_CREDENTIALS, BundleUtil.getStringFromBundle("login.builtin.error.invalidCredentials"));
            }
            if (!verifyPassword(userIdInProvider, password)) {
                return new AuthenticationResponse(AuthenticationResponse.Status.INVALID_CREDENTIALS, BundleUtil.getStringFromBundle("login.builtin.error.invalidCredentials"));
            }
            AuthenticatedUser au = new AuthenticatedUser(biUser.getUserName(), biUser.getFirstName(), biUser        
 }

 

}