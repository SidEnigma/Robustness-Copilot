package edu.harvard.iq.dataverse.authorization.groups;
 
 import edu.harvard.iq.dataverse.DvObject;
 import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
 import edu.harvard.iq.dataverse.authorization.RoleAssignee;
 import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.BuiltInGroupsProvider;
 import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
 import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupProvider;
 import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
 import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroupProvider;
 import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroupsServiceBean;
 import edu.harvard.iq.dataverse.authorization.groups.impl.maildomain.MailDomainGroupProvider;
 import edu.harvard.iq.dataverse.authorization.groups.impl.maildomain.MailDomainGroupServiceBean;
 import edu.harvard.iq.dataverse.authorization.groups.impl.shib.ShibGroupProvider;
 import edu.harvard.iq.dataverse.authorization.groups.impl.shib.ShibGroupServiceBean;
 import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Set;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import static java.util.stream.Collectors.toSet;
 import java.util.stream.Stream;
 import javax.annotation.PostConstruct;
 import javax.ejb.EJB;
 import javax.ejb.Stateless;
 import javax.inject.Inject;
 import javax.inject.Named;
 
 /**
  *
  * @author michael
  */
 @Stateless
 @Named
 public class GroupServiceBean {
     private static final Logger logger = Logger.getLogger(GroupServiceBean.class.getName());
     
     @EJB
     IpGroupsServiceBean ipGroupsService;
     @EJB
     ShibGroupServiceBean shibGroupService;
     @EJB
     ExplicitGroupServiceBean explicitGroupService;
     @EJB
     MailDomainGroupServiceBean mailDomainGroupService;
     
     private final Map<String, GroupProvider> groupProviders = new HashMap<>();
     
     private IpGroupProvider ipGroupProvider;
     private ShibGroupProvider shibGroupProvider;
     private ExplicitGroupProvider explicitGroupProvider;
     private MailDomainGroupProvider mailDomainGroupProvider;
     
     @EJB
     RoleAssigneeServiceBean roleAssigneeSvc;
     
     @PostConstruct
     public void setup() {
         addGroupProvider( BuiltInGroupsProvider.get() );
         addGroupProvider( ipGroupProvider = new IpGroupProvider(ipGroupsService) );
         addGroupProvider( shibGroupProvider = new ShibGroupProvider(shibGroupService) );
         addGroupProvider( explicitGroupProvider = explicitGroupService.getProvider() );
         addGroupProvider( mailDomainGroupProvider = mailDomainGroupService.getProvider() );
         Logger.getLogger(GroupServiceBean.class.getName()).log(Level.INFO, null, "PostConstruct group service call");
     }
 
     public Group getGroup( String groupAlias ) {
         String[] comps = groupAlias.split( Group.PATH_SEPARATOR, 2 );
         GroupProvider gp = groupProviders.get( comps[0] );
         if ( gp == null ) {
             logger.log(Level.WARNING, "Cannot find group provider with alias {0}", comps[0]);
             return null;
         }
         return gp.get( comps[1] );
     }
 
     public IpGroupProvider getIpGroupProvider() {
         return ipGroupProvider;
     }
 
     public ShibGroupProvider getShibGroupProvider() {
         return shibGroupProvider;
     }
     
     public MailDomainGroupProvider getMailDomainGroupProvider() {
         return mailDomainGroupProvider;
     }
     
     /**
      * Finds all the groups {@code req} is part of in {@code dvo}'s context.
      * Recurses upwards in {@link ExplicitGroup}s, as needed.
      * @param req The request whose group memberships we seek.
      * @param dvo The {@link DvObject} we determining the context fo the membership.
      * @return The groups {@code req} is part of under {@code dvo}.
      */
     public Set<Group> groupsFor( DataverseRequest req, DvObject dvo ) {
         return groupProviders.values().stream()
                               .flatMap(gp->(Stream<Group>)gp.groupsFor(req, dvo).stream())
                               .collect(toSet());
     }
     
     /**
      * All the groups a Role assignee belongs to. Does not take request-level groups
      * (such as IPGroups) into account.
      * @param ra
      * @param dvo
      * @return 
      */
     public Set<Group> groupsFor( RoleAssignee ra, DvObject dvo ) {
         return groupProviders.values().stream()
                               .flatMap(gp->(Stream<Group>)gp.groupsFor(ra, dvo).stream())
                               .collect( toSet() );
     }
 
     /**
      * groupsFor( RoleAssignee ra, DvObject dvo ) doesn't really get *all* the
      * groups a Role assignee belongs to as advertised but this method comes
      * closer.
      *
      * @param ra An AuthenticatedUser.
      * @return As many groups as we can find for the AuthenticatedUser.
      * 
      * @deprecated Does not look into IP Groups. Use {@link #groupsFor(edu.harvard.iq.dataverse.engine.command.DataverseRequest)}
      */
     @Deprecated
     public Set<Group> groupsFor(RoleAssignee ra) {
         return groupProviders.values().stream()
                              .flatMap(gp->(Stream<Group>)gp.groupsFor(ra).stream())
                              .collect(toSet());
     }
 
     
     public Set<Group> groupsFor( DataverseRequest req ) {
         return groupProviders.values().stream()
                              .flatMap(gp->(Stream<Group>)gp.groupsFor(req).stream())
                              .collect( toSet());
     }
     
     /**
      * Collections of groups may include {@link ExplicitGroup}s, which have a 
      * recursive structure (more precisely, a Composite Pattern}. This has many 
      * advantages, but it makes answering the question "which groups are 
      * contained in this group" non-trivial. This method deals with this issue by
      * providing a "flat list" of the groups contained in the groups at the 
      * passed collection.
      * 
      * The resultant stream is distinct - groups appear in it only once, even if
      * some of them are members of multiple groups.
      * 
      * @param groups A collection of groups
      * @return A distinct stream of groups who are members of, or are 
      * descendants of members of the groups in {@code groups}.
      */
     public Stream<Group> flattenGroupsCollection( Collection<Group> groups ) {
         Stream.Builder<Group> out = Stream.builder();
         groups.forEach( g -> {
             out.accept(g);
             if ( g instanceof ExplicitGroup ) {
                 collectGroupContent((ExplicitGroup) g, out);
             } 
         });
         return out.build().distinct();
     }
     
     private void collectGroupContent( ExplicitGroup eg, Stream.Builder<Group> out ) {
         eg.getContainedRoleAssgineeIdentifiers().stream()
                 .map( idtf -> roleAssigneeSvc.getRoleAssignee(idtf) )
                 .filter( asn -> asn instanceof Group )
                 .forEach( group ->  out.accept((Group)group) );
         
         eg.getContainedExplicitGroups().forEach( meg -> {
             out.accept(meg);
             collectGroupContent(meg, out);
         });
     }
     
 
/** Returns all groups that are in, or are the ancestors of, a group in the transmitted group collection. */
 public Set<Group> collectAncestors(Collection<Group> groups){}

 

}