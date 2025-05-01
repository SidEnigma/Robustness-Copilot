package edu.harvard.iq.dataverse.authorization.users;
 
 import edu.harvard.iq.dataverse.Cart;
 import edu.harvard.iq.dataverse.DatasetLock;
 import edu.harvard.iq.dataverse.UserNotification;
 import edu.harvard.iq.dataverse.ValidateEmail;
 import edu.harvard.iq.dataverse.authorization.AccessRequest;
 import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
 import edu.harvard.iq.dataverse.authorization.AuthenticatedUserLookup;
 import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2TokenData;
 import edu.harvard.iq.dataverse.userdata.UserUtil;
 import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
 import edu.harvard.iq.dataverse.util.BundleUtil;
 import static edu.harvard.iq.dataverse.util.StringUtil.nonEmpty;
 import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
 import java.io.Serializable;
 import java.sql.Timestamp;
 import java.util.List;
 import java.util.Objects;
 import javax.json.Json;
 import javax.json.JsonObjectBuilder;
 import javax.persistence.CascadeType;
 import javax.persistence.Column;
 import javax.persistence.Entity;
 import javax.persistence.GeneratedValue;
 import javax.persistence.GenerationType;
 import javax.persistence.Id;
 import javax.persistence.NamedQueries;
 import javax.persistence.NamedQuery;
 import javax.persistence.OneToMany;
 import javax.persistence.OneToOne;
 import javax.persistence.Transient;
 import javax.validation.constraints.NotNull;
 import org.hibernate.validator.constraints.NotBlank;
 
 /**
  * When adding an attribute to this class, be sure to update the following:
  * 
  *  (1) AuthenticatedUser.toJSON() - within this class   (REQUIRED)
  *  (2) UserServiceBean.getUserListCore() - native SQL query
  *  (3) UserServiceBean.createAuthenticatedUserForView() - add values to a detached AuthenticatedUser object
  * 
  * @author rmp553
  */
 @NamedQueries({
     @NamedQuery( name="AuthenticatedUser.findAll",
                 query="select au from AuthenticatedUser au"),
     @NamedQuery( name="AuthenticatedUser.findSuperUsers",
                 query="SELECT au FROM AuthenticatedUser au WHERE au.superuser = TRUE"),
     @NamedQuery( name="AuthenticatedUser.findByIdentifier",
                 query="select au from AuthenticatedUser au WHERE LOWER(au.userIdentifier)=LOWER(:identifier)"),
     @NamedQuery( name="AuthenticatedUser.findByEmail",
                 query="select au from AuthenticatedUser au WHERE LOWER(au.email)=LOWER(:email)"),
     @NamedQuery( name="AuthenticatedUser.countOfIdentifier",
                 query="SELECT COUNT(a) FROM AuthenticatedUser a WHERE LOWER(a.userIdentifier)=LOWER(:identifier)"),
     @NamedQuery( name="AuthenticatedUser.filter",
                 query="select au from AuthenticatedUser au WHERE ("
                         + "LOWER(au.userIdentifier) like LOWER(:query) OR "
                         + "lower(concat(au.firstName,' ',au.lastName)) like lower(:query))"),
     @NamedQuery( name="AuthenticatedUser.findAdminUser",
                 query="select au from AuthenticatedUser au WHERE "
                         + "au.superuser = true "
                         + "order by au.id")
     
 })
 @Entity
 public class AuthenticatedUser implements User, Serializable {
     
     public static final String IDENTIFIER_PREFIX = "@";
     
     @Id
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     Long id;
 
     /**
      * @todo Shouldn't there be some constraints on what the userIdentifier is
      * allowed to be? It can't be as restrictive as the "userName" field on
      * BuiltinUser because we can't predict what Shibboleth Identity Providers
      * (IdPs) will send (typically in the "eppn" SAML assertion) but perhaps
      * spaces, for example, should be disallowed. Right now "elisah.da mota" can
      * be persisted as a userIdentifier per
      * https://github.com/IQSS/dataverse/issues/2945
      */
     @NotNull
     @Column(nullable = false, unique=true)
     private String userIdentifier;
 
     @ValidateEmail(message = "{user.invalidEmail}")
     @NotNull
     @Column(nullable = false, unique=true)
     private String email;
     private String affiliation;
     private String position;
     
     @NotBlank(message = "{user.lastName}")
     private String lastName;
     
     @NotBlank(message = "{user.firstName}")
     private String firstName;
     
     @Column(nullable = true)
     private Timestamp emailConfirmed;
  
     @Column(nullable=false)
     private Timestamp createdTime;
     
     @Column(nullable=true)
     private Timestamp lastLoginTime;    // last user login timestamp
 
     @Column(nullable=true)
     private Timestamp lastApiUseTime;   // last API use with user's token
     
     @Transient
     private Cart cart;
     
     private boolean superuser;
 
     @Column(nullable=false)
     private boolean deactivated;
 
     @Column(nullable=true)
     private Timestamp deactivatedTime;
 
     /**
      * @todo Consider storing a hash of *all* potentially interesting Shibboleth
      * attribute key/value pairs, not just the Identity Provider (IdP).
      */
     @Transient
     private String shibIdentityProvider;
 
     @Override
     public String getIdentifier() {
         return IDENTIFIER_PREFIX + userIdentifier;
     }
 
     @OneToMany(mappedBy = "user", cascade={CascadeType.REMOVE})
     private List<UserNotification> notifications;
 
     public List<UserNotification> getUserNotifications() {
         return notifications;
     }
 
     public void setUserNotifications(List<UserNotification> notifications) {
         this.notifications = notifications;
     }
     
     @OneToMany(mappedBy = "requestor", cascade={CascadeType.REMOVE})
     private List<UserNotification> requests;
 
     public List<UserNotification> getUserRequests() {
         return requests;
     }
 
     public void setUserRequestss(List<UserNotification> requests) {
         this.requests = requests;
     }
 
     
     @OneToMany(mappedBy = "user", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
     private List<DatasetLock> datasetLocks;
 	
     public List<DatasetLock> getDatasetLocks() {
         return datasetLocks;
     }
 
     public void setDatasetLocks(List<DatasetLock> datasetLocks) {
         this.datasetLocks = datasetLocks;
     }
 
     @OneToMany(mappedBy = "user", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
     private List<OAuth2TokenData> oAuth2TokenDatas;
 
     @Override
     public AuthenticatedUserDisplayInfo getDisplayInfo() {
         return new AuthenticatedUserDisplayInfo(firstName, lastName, email, affiliation, position);
     }
     
 
/** Update the internal fields according to the passed object values */
 public void applyDisplayInfo(AuthenticatedUserDisplayInfo inf){}

 

}