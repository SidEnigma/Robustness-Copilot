package edu.harvard.iq.dataverse.authorization.groups.impl.explicit;
 
 import edu.harvard.iq.dataverse.DvObject;
 import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
 import edu.harvard.iq.dataverse.authorization.groups.Group;
 import edu.harvard.iq.dataverse.authorization.RoleAssignee;
 import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
 import edu.harvard.iq.dataverse.authorization.users.User;
 import edu.harvard.iq.dataverse.authorization.groups.GroupException;
 import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AuthenticatedUsers;
 import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
 import java.util.HashSet;
 import java.util.Objects;
 import java.util.Set;
 import java.util.TreeSet;
 import javax.persistence.Column;
 import javax.persistence.ElementCollection;
 import javax.persistence.Entity;
 import javax.persistence.GeneratedValue;
 import javax.persistence.GenerationType;
 import javax.persistence.Id;
 import javax.persistence.Index;
 import javax.persistence.JoinColumn;
 import javax.persistence.JoinTable;
 import javax.persistence.ManyToMany;
 import javax.persistence.ManyToOne;
 import javax.persistence.NamedQueries;
 import javax.persistence.NamedQuery;
 import javax.persistence.PostLoad;
 import javax.persistence.PrePersist;
 import javax.persistence.Table;
 import javax.persistence.Transient;
 import javax.validation.constraints.Pattern;
 import org.hibernate.validator.constraints.NotBlank;
 
 /**
  * A group that explicitly lists {@link RoleAssignee}s that belong to it. Implementation-wise,
  * there are three cases here: {@link AuthenticatedUser}s, other {@link ExplicitGroup}s, and all the rest.
  * AuthenticatedUsers and ExplicitGroups go in tables of their own. The rest are kept via their identifier.
  * 
  * @author michael
  */
 @NamedQueries({
     @NamedQuery( name="ExplicitGroup.findAll",
                  query="SELECT eg FROM ExplicitGroup eg"),
     @NamedQuery( name="ExplicitGroup.findByOwnerIdAndAlias",
                  query="SELECT eg FROM ExplicitGroup eg WHERE eg.owner.id=:ownerId AND eg.groupAliasInOwner=:alias"),
     @NamedQuery( name="ExplicitGroup.findByAlias",
                  query="SELECT eg FROM ExplicitGroup eg WHERE eg.groupAlias=:alias"),
     @NamedQuery( name="ExplicitGroup.findByOwnerId",
                  query="SELECT eg FROM ExplicitGroup eg WHERE eg.owner.id=:ownerId"),
     @NamedQuery( name="ExplicitGroup.findByOwnerAndAuthUserId",
                  query="SELECT eg FROM ExplicitGroup eg join eg.containedAuthenticatedUsers au "
                       +"WHERE eg.owner.id=:ownerId AND au.id=:authUserId"),
     @NamedQuery( name="ExplicitGroup.findByOwnerAndSubExGroupId",
                  query="SELECT eg FROM ExplicitGroup eg join eg.containedExplicitGroups ceg "
                       +"WHERE eg.owner.id=:ownerId AND ceg.id=:subExGroupId"),
     @NamedQuery( name="ExplicitGroup.findByOwnerAndRAIdtf",
                  query="SELECT eg FROM ExplicitGroup eg join eg.containedRoleAssignees ra "
                       +"WHERE eg.owner.id=:ownerId AND ra=:raIdtf"),
     @NamedQuery( name="ExplicitGroup.findByAuthenticatedUserIdentifier",
                  query="SELECT eg FROM ExplicitGroup eg JOIN eg.containedAuthenticatedUsers au "
                      + "WHERE au.userIdentifier=:authenticatedUserIdentifier"),
     @NamedQuery( name="ExplicitGroup.findByRoleAssgineeIdentifier",
                  query="SELECT eg FROM ExplicitGroup eg JOIN eg.containedRoleAssignees cra "
                      + "WHERE cra=:roleAssigneeIdentifier"),
     @NamedQuery( name="ExplicitGroup.findByContainedExplicitGroupId",
                  query="SELECT eg FROM ExplicitGroup eg join eg.containedExplicitGroups ceg "
                       +"WHERE ceg.id=:containedExplicitGroupId")
 })
 @Entity
 @Table(indexes = {@Index(columnList="owner_id"),
                   @Index(columnList="groupaliasinowner")})
 public class ExplicitGroup implements Group, java.io.Serializable {
     
     @Id
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     Long id;
     
     /**
      * Authenticated users directly added to the group.
      */
     @ManyToMany
     private Set<AuthenticatedUser> containedAuthenticatedUsers;
     
     /**
      * Explicit groups that belong to {@code this} explicit gorups.
      */
     @ManyToMany
     @JoinTable(name = "explicitgroup_explicitgroup", 
             joinColumns = @JoinColumn(name="explicitgroup_id", referencedColumnName = "id"),
             inverseJoinColumns = @JoinColumn(name="containedexplicitgroups_id", referencedColumnName = "id") )
     Set<ExplicitGroup> containedExplicitGroups;
     
     /**
      * All the role assignees that belong to this group
      * and are not {@link authenticatedUser}s or {@ExplicitGroup}s, are stored
      * here via their identifiers.
      * 
      * @see RoleAssignee#getIdentifier() 
      */
     @ElementCollection
     private Set<String> containedRoleAssignees;
     
     @Column( length = 1024 )
     private String description;
     
     @NotBlank
     private String displayName;
     
     /**
      * The DvObject under which this group is defined.
      */
     @ManyToOne
     DvObject owner;
     
     /** Given alias of the group, e.g by the user that created it. Unique in the owner. */
     @NotBlank
     @Pattern(regexp = "[a-zA-Z0-9\\_\\-]*", message = "{dataverse.nameIllegalCharacters}")
     private String groupAliasInOwner;
     
     /** Alias of the group. Calculated from the group's name and its owner id. Unique in the table. */
     @Column( unique = true )
     private String groupAlias;
     
     @Transient
     private ExplicitGroupProvider provider;
     
     public ExplicitGroup( ExplicitGroupProvider prv ) {
         provider = prv;
         containedAuthenticatedUsers = new HashSet<>();
         containedExplicitGroups = new HashSet<>();
         containedRoleAssignees = new TreeSet<>();
     }
 
     public Set<AuthenticatedUser> getContainedAuthenticatedUsers() {
         return containedAuthenticatedUsers;
     }
 
     public Set<ExplicitGroup> getContainedExplicitGroups() {
         if ( getGroupProvider() != null ) {
             for ( ExplicitGroup g : containedExplicitGroups ) {
                 g.setProvider(getGroupProvider());
             }
         }
         return containedExplicitGroups;
     }
 
     /**
      * Constructor for JPA.
      */
     protected ExplicitGroup() {}
     
     public void add( User u ) {
         if ( u == null ) throw new IllegalArgumentException("Cannot add a null user to an explicit group.");
         if ( u instanceof AuthenticatedUser ) {
             containedAuthenticatedUsers.add((AuthenticatedUser)u);
         } else {
             containedRoleAssignees.add( u.getIdentifier() );
         }
     }
     
     /**
      * Adds the {@link RoleAssignee} to {@code this} group. 
      * 
      * @param ra the role assignee to be added to this group.
      * @throws GroupException if {@code ra} is a group, and is either an ancestor of {@code this},
      *         or is defined in a dataverse that is not an ancestor of {@code this.owner}.
      */
     public void add( RoleAssignee ra ) throws GroupException {
         
         if ( ra.equals(this) ) {
             throw new GroupException(this, "A group cannot be added to itself.");
         }
         
         if ( ra instanceof User ) {
             add( (User)ra );
             
         } else {
             if ( ra instanceof ExplicitGroup ) {
                 // validate no circular deps
                 ExplicitGroup g = (ExplicitGroup) ra;
                 if ( g.structuralContains(this) ) {
                     throw new GroupException(this, "A group cannot be added to one of its childs.");
                 }
                 if ( g.owner.isAncestorOf(owner) ) {
                     containedExplicitGroups.add( g );
                 } else {
                     throw new GroupException(this, "Cannot add " + g + ", as it is not defined in " + owner + " or one of its ancestors.");
                 }
             } else {
                 containedRoleAssignees.add( ra.getIdentifier() );
             }
             
         }
         
     }
     
     public void remove(RoleAssignee roleAssignee) {
         removeByRoleAssgineeIdentifier( roleAssignee.getIdentifier() );
     }
     
     /**
      * Returns all the role assignee identifiers in this group. <br>
      * <b>Note</b> some of the identifiers may be stale (i.e. group deleted but 
      * identifiers lingered for a while).
      * 
      * @return A list of the role assignee identifiers.
      */
     public Set<String> getContainedRoleAssgineeIdentifiers() {
         Set<String> retVal = new TreeSet<>();
         retVal.addAll( containedRoleAssignees );
         for ( ExplicitGroup subg : getContainedExplicitGroups() ) {
             retVal.add( subg.getIdentifier() );
         }
         for ( AuthenticatedUser au : containedAuthenticatedUsers ) {
             retVal.add( au.getIdentifier() );
         }
         
         return retVal;
     }
     
     public void removeByRoleAssgineeIdentifier( String idtf ) {
         if ( containedRoleAssignees.contains(idtf) ) {
             containedRoleAssignees.remove(idtf);
         } else {
             for ( AuthenticatedUser au : containedAuthenticatedUsers ) {
                 if ( au.getIdentifier().equals(idtf) ) {
                     containedAuthenticatedUsers.remove(au);
                     return;
                 }
             }
             for ( ExplicitGroup eg : containedExplicitGroups ) {
                 if ( eg.getIdentifier().equals(idtf) ) {
                     containedExplicitGroups.remove(eg);
                     return;
                 }
             }
         }
     }
     
     /**
      * Returns a set of all direct members of the group, including 
      * logical role assignees.
      * @return members of the group.
      */
     public Set<RoleAssignee> getDirectMembers() {
         Set<RoleAssignee> res = new HashSet<>();
         
         res.addAll( getContainedExplicitGroups() );
         res.addAll( containedAuthenticatedUsers );
         for ( String idtf : containedRoleAssignees ) {
             RoleAssignee ra = provider.findRoleAssignee(idtf);
             if ( ra != null ) {
                 res.add(ra);
             }
         }
         
         return res;
     }
     
     @Override
     public String getDescription() {
         return description;
     }
 
     public void setDescription(String description) {
         this.description = description;
     }
 
     @Override
     public boolean contains(DataverseRequest req) {
         return containsDirectly(req) || containsIndirectly(req);
     }
     
 
/** Determines whether or not the structural containment is a part of the groups's strucutre. Returns true if so, false otherwise. */
 public boolean structuralContains(RoleAssignee ra){}

 

}