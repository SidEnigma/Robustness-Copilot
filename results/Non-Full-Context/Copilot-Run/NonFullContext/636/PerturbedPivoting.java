package org.dcache.auth;
 
 import static com.google.common.base.Preconditions.checkArgument;
 
 import com.google.common.net.InetAddresses;
 import eu.emi.security.authn.x509.impl.OpensslNameUtils;
 import eu.emi.security.authn.x509.proxy.ProxyUtils;
 import java.lang.reflect.Constructor;
 import java.lang.reflect.InvocationTargetException;
 import java.security.Principal;
 import java.security.cert.CertPath;
 import java.security.cert.X509Certificate;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.List;
 import java.util.NoSuchElementException;
 import java.util.Set;
 import java.util.stream.Collectors;
 import javax.security.auth.Subject;
 import javax.security.auth.kerberos.KerberosPrincipal;
 import org.dcache.util.PrincipalSetMaker;
 import org.globus.gsi.gssapi.jaas.GlobusPrincipal;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public class Subjects {
 
     private static final Logger LOGGER = LoggerFactory.getLogger(Subjects.class);
 
     public static final String UNKNOWN = "<unknown>";
 
     /**
      * Ordered list of principals considered as displayable.
      */
     private static final Class<? extends Principal>[] DISPLAYABLE = new Class[]
           {
                 FullNamePrincipal.class,
                 UserNamePrincipal.class,
                 GlobusPrincipal.class,
                 KerberosPrincipal.class,
                 Origin.class,
                 Principal.class
           };
 
     /**
      * The subject representing the root user, that is, a user that is empowered to do everything.
      */
     public static final Subject ROOT;
     public static final Subject NOBODY;
 
     static {
         ROOT = new Subject();
         ROOT.getPrincipals().add(new UidPrincipal(0));
         ROOT.getPrincipals().add(new GidPrincipal(0, true));
         ROOT.setReadOnly();
 
         NOBODY = new Subject();
         NOBODY.setReadOnly();
     }
 
     /**
      * Returns true if and only if the subject is root, that is, has the user ID 0.
      */
     public static boolean isRoot(Subject subject) {
         return hasUid(subject, 0);
     }
 
     /**
      * Return true if the subject is root or has the special ExemptFromNamespaceChecks principal.
      *
      * @param subject The identity of the user.
      * @return if the user is except from namespace checks.
      * @see #isRoot(javax.security.auth.Subject)
      */
     public static boolean isExemptFromNamespaceChecks(Subject subject) {
         return subject.getPrincipals().stream()
               .anyMatch(p -> p instanceof UidPrincipal && ((UidPrincipal) p).getUid() == 0
                     ||
                     p instanceof ExemptFromNamespaceChecks);
     }
 
     /**
      * Returns true if and only if the subject is nobody, i.e., does not have a UID.
      * <p>
      * Being nobody does not imply that the user is anonymous: The subjects's identiy may have been
      * established through some authentication mechanism. However the subject could not be assigned
      * an internal identity in dCache.
      */
     public static boolean isNobody(Subject subject) {
         for (Principal principal : subject.getPrincipals()) {
             if (principal instanceof UidPrincipal) {
                 return false;
             }
         }
         return true;
     }
 
 
/** Returns true if and only if the object has the given user ID. */
 public static boolean hasUid(Subject subject, long uid){}

 

}