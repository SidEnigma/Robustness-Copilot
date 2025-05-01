package edu.harvard.iq.dataverse.authorization.groups.impl.maildomain;
 
 import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
 import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
 import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
 import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailServiceBean;
 
 import java.util.*;
 import java.util.logging.Logger;
 import java.util.regex.Pattern;
 import java.util.stream.Collectors;
 import javax.annotation.PostConstruct;
 import javax.ejb.*;
 import javax.inject.Inject;
 import javax.inject.Named;
 import javax.persistence.EntityManager;
 import javax.persistence.NoResultException;
 import javax.persistence.PersistenceContext;
 import javax.ws.rs.NotFoundException;
 
 /**
  * A bean providing the {@link MailDomainGroupProvider}s with container services, such as database connectivity.
  * Also containing the business logic to decide about matching groups.
  */
 @Named
 @Singleton
 @Startup
 @DependsOn("StartupFlywayMigrator")
 public class MailDomainGroupServiceBean {
     
     private static final Logger logger = Logger.getLogger(edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean.class.getName());
     
     @PersistenceContext(unitName = "VDCNet-ejbPU")
     protected EntityManager em;
     
     @Inject
     ConfirmEmailServiceBean confirmEmailSvc;
     @Inject
     ActionLogServiceBean actionLogSvc;
 	
     MailDomainGroupProvider provider;
     List<MailDomainGroup> simpleGroups = Collections.EMPTY_LIST;
     Map<MailDomainGroup, Pattern> regexGroups = new HashMap<>();
     
     @PostConstruct
     void setup() {
         provider = new MailDomainGroupProvider(this);
         this.updateGroups();
     }
     
     /**
      * Update the groups from the database.
      * This is done because regex compilation is an expensive operation and should be cached.
      */
     @Lock(LockType.WRITE)
     public void updateGroups() {
         List<MailDomainGroup> all = findAll();
         this.simpleGroups = all.stream().filter(mg -> !mg.isRegEx()).collect(Collectors.toList());
         this.regexGroups = all.stream()
             .filter(MailDomainGroup::isRegEx)
             .collect(Collectors.toMap(
                 mg -> mg,
                 mg -> Pattern.compile(mg.getEmailDomains().replace(";","|"))
             ));
     }
     
     @Lock(LockType.READ)
     public MailDomainGroupProvider getProvider() {
         return provider;
     }
     
     /**
      * Find groups for users mail address. Only done when email has been verified.
      * @param user
      * @return A collection of groups with matching email domains
      */
     @Lock(LockType.READ)
     public Set<MailDomainGroup> findAllWithDomain(AuthenticatedUser user) {
         
         // if the mail address is not verified, escape...
         if (!confirmEmailSvc.hasVerifiedEmail(user)) { return Collections.emptySet(); }
         
         // otherwise start to bisect the mail and lookup groups.
         // NOTE: the email from the user has been validated via {@link EMailValidator} when persisted.
         Optional<String> oDomain = getDomainFromMail(user.getEmail());
         if ( oDomain.isPresent() ) {
             // transform to lowercase, in case someone uses uppercase letters. (we store the comparison values in lowercase)
             String domain = oDomain.get().toLowerCase();
             
             // scan simple groups (containing an exact match of the domain)
             Set<MailDomainGroup> result = this.simpleGroups.stream()
                                                            .filter(mg -> mg.getEmailDomainsAsList().contains(domain))
                                                            .collect(Collectors.toSet());
             // scan regex based groups (domain matching a regular expression)
             result.addAll(this.regexGroups.keySet().stream()
                                                    .filter(MailDomainGroup::isRegEx)
                                                    .filter(mg -> regexGroups.get(mg).matcher(domain).matches())
                                                    .collect(Collectors.toSet()));
             return result;
             
         }
         return Collections.emptySet();
     }
     
     /**
      * Get all mail domain groups from the database.
      * @return A result list from the database. May be null if no results found.
      */
     @Lock(LockType.READ)
     public List<MailDomainGroup> findAll() {
         return em.createNamedQuery("MailDomainGroup.findAll", MailDomainGroup.class).getResultList();
     }
     
 
/** This method returns a specific mail domain group by it is alias */
  Optional<MailDomainGroup> findByAlias(String groupAlias){
        try {
            return Optional.of(em.createNamedQuery("MailDomainGroup.findByAlias", MailDomainGroup.class).setParameter("alias", groupAlias).getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
         
         /**
        * Get the domain part of a mail address.
        * @param mail
        * @return The domain part of the mail address. May be null if no domain found.
        */
         @Lock(LockType.READ)
         public Optional<String> getDomainFromMail(String mail) {
             if (mail == null) { return Optional.empty(); }
             int at = mail.indexOf('@');
             if (at < 0) { return Optional.empty(); }
             return Optional.of(mail.substring(at+1));
         }
         
         /**
        * Get the domain part of a mail address.
        * @param mail
        * @return The domain part of the mail address. May be null if no domain found.
        */
         @Lock(LockType.READ)
         public Optional<String> getDomainFromMail(String mail, String defaultDomain) {
             if (mail == null) { return Optional.of(defaultDomain); }
             int at = mail.indexOf('@');
             if (at < 0) { return Optional.of(defaultDomain); }
             return Optional.of(mail.substring(at+1));
         }
         
         /**
        * Get the domain part of a mail address.
        * @param mail
        * @return The domain part of the mail address. May be null if no domain found.
        */
         @Lock(LockType.READ)
         public Optional<String> getDomainFromMail(String mail, String defaultDomain, String defaultDomain2) {
             if (mail == null) { return Optional.of(defaultDomain); }
             int at = mail.indexOf('@');
             if (at < 0) { return Optional.of(defaultDomain); }
             return Optional.of(mail.substring(at+1));      
  }

 

}