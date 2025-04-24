package org.dcache.services.info.base;
 
 
 import static java.util.Objects.requireNonNull;
 
 import java.util.Collection;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.concurrent.TimeUnit;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  * A StateComposite is an aggregation of zero or more StateComponents.  StateComposites form the
  * branch nodes within the dCache state tree.
  * <p>
  * A Mortal StateComposite has a minimum lifetime when created.  The expiry date will be adjusted to
  * match any added Mortal children: the branch will always persist whilst it contains any children.
  * <p>
  * An Ephemeral StateComposite has no lifetime: it will persist without having fixed lifetime.
  * However an Ephemeral StateComposite will not prevent an ancestor StateComposite that is Mortal
  * from expiring.  In general, a StateComposite that contains <i>only</i> Ephemeral children should
  * also be Ephemeral; all other StateComposites should be Mortal.
  * <p>
  * A StateComposite also maintains a record of the earliest any of its children (or children of
  * children) will expire.  This is an optimisation, allowing a quick determination when a tree
  * should next be purged and, with any subtree, whether it is necessary to purge that subtree.
  *
  * @author Paul Millar <paul.millar@desy.de>
  */
 public class StateComposite implements StateComponent {
 
     private static final Logger LOGGER = LoggerFactory.getLogger(StateComposite.class);
 
     /**
      * Minimum lifetime for on-the-fly created StateComposites, in seconds
      */
     static final long DEFAULT_LIFETIME = 10;
 
     private final Map<String, StateComponent> _children = new HashMap<>();
     private StatePersistentMetadata _metadataRef;
     private Date _earliestChildExpiry;
     private Date _whenIShouldExpire;
     private boolean _isEphemeral;
 
     /**
      * The constructor for public use: a StateComposite with a finite lifetime.
      *
      * @param lifetime the minimum duration, in seconds, that this StateComposite should persist.
      */
     public StateComposite(long lifetime) {
         if (lifetime < 0) {
             lifetime = 0;
         }
 
         becomeMortal(lifetime);
         _metadataRef = null;  // Set when added to state tree
     }
 
 
     /**
      * Create an Ephemeral StateComposite.  These should <i>only</i> be used when they are to
      * contain only Ephemeral children.  Normally StateComposites should be created Mortal.
      */
     public StateComposite() {
         this(false);
     }
 
     /**
      * Create a new Ephemeral or Immortal StateComposite.  Normally StateComposites should be
      * mortal.  (Mortal StateComposites will automatically extend their lives so they don't expire
      * before their children.)
      *
      * @param isImmortal true for an immortal StateComposite, false for an ephemeral one.
      */
     public StateComposite(boolean isImmortal) {
         if (isImmortal) {
             becomeImmortal();
         } else {
             becomeEphemeral();
         }
         _metadataRef = null;
     }
 
     /**
      * Our private usage below: build a new Mortal StateComposite with a link to
      * persistentMetadata.
      *
      * @param persistentMetadata the corresponding StatePersistentMetadata object.
      * @param lifetime           the minimum lifetime of this object, in seconds.
      */
     private StateComposite(StatePersistentMetadata persistentMetadata, long lifetime) {
         becomeMortal(lifetime);
         _metadataRef = persistentMetadata;
     }
 
     /**
      * Build an Immortal StateComposite with specific metadata link. This should only be used by the
      * State singleton.
      *
      * @param persistentMetadata the top-level metadata.
      */
     protected StateComposite(StatePersistentMetadata persistentMetadata) {
         becomeImmortal();
         _metadataRef = persistentMetadata;
     }
 
     /**
      * Possibly update our belief of the earliest time that a Mortal child StateComponent will
      * expire.  It is safe to call this method with all child Dates: it will update the
      * _earliestChildExpiry Date correctly.
      *
      * @param newDate the expiry Date of a Mortal child StateComponent
      */
     private void updateEarliestChildExpiryDate(Date newDate) {
         if (newDate == null) {
             return;
         }
 
         if (_earliestChildExpiry == null || newDate.before(_earliestChildExpiry)) {
             _earliestChildExpiry = newDate;
         }
     }
 
     /**
      * @return the time when the earliest child will expire, or null if we have no Mortal children.
      */
     @Override
     public Date getEarliestChildExpiryDate() {
         return _earliestChildExpiry != null ? new Date(_earliestChildExpiry.getTime()) : null;
     }
 
 
     /**
      * Update our whenIShouldExpire date.  If the new date is before the existing one it is
      * ignored.
      *
      * @param newDate the new whenIShouldExpire date
      */
     private void updateWhenIShouldExpireDate(Date newDate) {
         if (newDate == null) {
             return;
         }
 
         if (_whenIShouldExpire == null || newDate.after(_whenIShouldExpire)) {
             _whenIShouldExpire = newDate;
         }
     }
 
 
     /**
      * Return a cryptic string describing this StateComposite.
      */
     @Override
     public String toString() {
         StringBuilder sb = new StringBuilder();
         sb.append("StateComposite <");
         sb.append(isMortal() ? "+" : isEphemeral() ? "*" : "#");
         sb.append("> {");
         sb.append(_children.size());
         sb.append("}");
 
         return sb.toString();
     }
 
     /**
      * When we should expire.
      */
     @Override
     public Date getExpiryDate() {
         return _whenIShouldExpire != null ? new Date(_whenIShouldExpire.getTime()) : null;
     }
 
     /**
      * This function checks whether our parent should expunge us.
      */
     @Override
     public boolean hasExpired() {
         Date now = new Date();
 
         return _whenIShouldExpire != null ? !now.before(_whenIShouldExpire) : false;
     }
 
     /**
      * Make sure we never expire.
      */
     private void becomeImmortal() {
         _isEphemeral = false;
         _whenIShouldExpire = null;
     }
 
     /**
      * Switch behaviour to be Ephemeral.  That is, don't expire automatically but don't prevent
      * Mortal parent(s) from expiring.
      */
     private void becomeEphemeral() {
         _isEphemeral = true;
         _whenIShouldExpire = null;
     }
 
     /**
      * Initialise our expiry time to some point in the future.
      *
      * @param lifetime the time, in seconds.
      */
     private void becomeMortal(long lifetime) {
         _whenIShouldExpire = new Date(
               System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(lifetime));
     }
 
 
     /**
      * Apply the visitor pattern over our children.
      * <p>
      * Interesting aspects:
      * <ul>
      * <li> Visiting over all child only happens once the <tt>start</tt> path has been
      * exhausted
      * <li> There are five StateVisitor call-backs from this StateComponent
      * </ul>
      * <p>
      * The standard call-backs are:
      * <ul>
      * <li>visitCompositePreDescend() called before visiting children.
      * <li>visitCompositePreLastDescend() called before visiting the last child.
      * <li>visitCompositePostDescend() called after visiting children.
      * </ul>
      * <p>
      * The <tt>start</tt> path allows the client to specify a point within the State tree
      * to start visiting.  Iterations down to that level call a different set of Visitor
      * call-backs: visitCompositePreSkipDescend() and visitCompositePostSkipDescend().
      * These are equivalent to the non-<tt>Skip</tt> versions and allow the StateVisitor
      * to represent the skipping down to the starting point, or not.
      *
      * @param path    the path to the current position in the State.
      * @param visitor the object that implements the StateVisitor class.
      */
     @Override
     public void acceptVisitor(StatePath path, StateVisitor visitor) {
         LOGGER.trace("acceptVisitor({})", path);
         Map<String, String> branchMetadata = getMetadataInfo();
 
         visitor.visitCompositePreDescend(path, branchMetadata);
 
         for (Map.Entry<String, StateComponent> mapEntry : _children.entrySet()) {
             String childName = mapEntry.getKey();
             StateComponent child = mapEntry.getValue();
             StatePath childPath = buildChildPath(path, childName);
             if (visitor.isVisitable(childPath)) {
                 child.acceptVisitor(childPath, visitor);
             }
         }
 
         visitor.visitCompositePostDescend(path, branchMetadata);
     }
 
 
     /**
      * Simulate the effects of the StateTransition, so allowing the StateVisitor to visit the dCache
      * State after the transition has taken effect.
      */
     @Override
     public void acceptVisitor(StateTransition transition, StatePath ourPath, StateVisitor visitor) {
         requireNonNull(transition);
         LOGGER.trace("acceptVisitor; transition={}, path={})", transition, ourPath);
 
         Map<String, String> branchMetadata = getMetadataInfo();
 
         visitor.visitCompositePreDescend(ourPath, branchMetadata);
 
         StateChangeSet changeSet = transition.getStateChangeSet(ourPath);
         Map<String, StateComponent> futureChildren = getFutureChildren(changeSet);
 
         for (Map.Entry<String, StateComponent> mapEntry : futureChildren.entrySet()) {
             String childName = mapEntry.getKey();
             StateComponent child = mapEntry.getValue();
             StatePath childPath = buildChildPath(ourPath, childName);
 
             if (visitor.isVisitable(childPath)) {
                 child.acceptVisitor(transition, childPath, visitor);
             }
         }
 
         visitor.visitCompositePostDescend(ourPath, branchMetadata);
     }
 
 
     /**
      * Return what this._children will look like after a StateChangeSet has been applied.
      */
     private Map<String, StateComponent> getFutureChildren(StateChangeSet changeSet) {
         if (changeSet == null) {
             return _children;
         }
 
         Map<String, StateComponent> futureChildren = new HashMap<>(_children);
 
         for (String childName : changeSet.getNewChildren()) {
             StateComponent childValue = changeSet.getNewChildValue(childName);
             futureChildren.put(childName, childValue);
         }
 
         for (String childName : changeSet.getUpdatedChildren()) {
             StateComponent childValue = changeSet.getUpdatedChildValue(childName);
 
             // When updating a branch (i.e., not a new branch) updates to child
             // StateComposite objects are children of the existing branch, not
             // the future one.
             if (childValue instanceof StateComposite) {
                 continue;
             }
 
             futureChildren.put(childName, childValue);
         }
 
         for (String childName : changeSet.getRemovedChildren()) {
             futureChildren.remove(childName);
         }
 
         return futureChildren;
     }
 
 
     /**
      * Apply a transition to our current state.  Children are added, updated or removed based on the
      * supplied transition.
      *
      * @param ourPath    the path to this within dCache tree, or null for top-most StateComposite
      * @param transition the StateTransition to apply
      */
     @Override
     public void applyTransition(StatePath ourPath, StateTransition transition) {
         StateChangeSet changeSet = transition.getStateChangeSet(ourPath);
 
         if (changeSet == null) {
             LOGGER.warn("cannot find StateChangeSet for path {}", ourPath);
             return;
         }
 
         Date newExpDate = changeSet.getWhenIShouldExpireDate();
         updateWhenIShouldExpireDate(newExpDate);
         if (newExpDate == null) {
             LOGGER.trace("getWhenIShouldExpireDate() returned null: no Mortal children?");
         }
 
         if (changeSet.haveImmortalChild()) {
             becomeImmortal(); // this is currently irreversible
         }
 
         // First, remove those children we should remove.
         for (String childName : changeSet.getRemovedChildren()) {
             LOGGER.trace("removing child {}", childName);
             _children.remove(childName);
         }
 
         // Then update our existing children.
         for (String childName : changeSet.getUpdatedChildren()) {
             StateComponent updatedChildValue = changeSet.getUpdatedChildValue(childName);
 
             if (updatedChildValue == null) {
                 LOGGER.error(
                       "Attempting to update {} in {}, but value is null; wilfully ignoring this.",
                       childName, ourPath);
                 continue;
             }
 
             LOGGER.trace("updating child {}, updated value {}", childName, updatedChildValue);
             addComponent(childName, updatedChildValue);
         }
 
         // Finally, add all new children.
         for (String childName : changeSet.getNewChildren()) {
             StateComponent newChildValue = changeSet.getNewChildValue(childName);
             LOGGER.trace("adding new child {}, new value {}", childName, newChildValue);
             addComponent(childName, newChildValue);
         }
 
         // Now, which children should we iterate into?
         for (String childName : changeSet.getItrChildren()) {
             StateComponent child = _children.get(childName);
 
             if (child == null) {
                 if (!changeSet.getRemovedChildren().contains(childName)) {
                     LOGGER.error("Whilst in {}, avoided attempting to applyTransition()" +
                           " on missing child {}", ourPath, childName);
                 }
                 continue;
             }
 
             child.applyTransition(buildChildPath(ourPath, childName), transition);
         }
 
         recalcEarliestChildExpiry();
     }
 
 
     /**
      * Recalculate _earliestChildExpiryDate() by asking our children for their earliest expiring
      * child.
      * TODO: this isn't always necessary, but it's hard to know when.  Also, it isn't clear that the
      * cost of figuring out when it is necessary is less than the CPU time saved by always recalculating.
      */
     private void recalcEarliestChildExpiry() {
         _earliestChildExpiry = null; // A forceful reset
 
         for (StateComponent child : _children.values()) {
 
             Date earliestExpires = child.getEarliestChildExpiryDate();
 
             if (earliestExpires != null) {
                 updateEarliestChildExpiryDate(earliestExpires);
             }
 
             if (child.isMortal()) {
                 updateEarliestChildExpiryDate(child.getExpiryDate());
             }
         }
     }
 
 
     /**
      * Look up persistent metadata reference for child and return it.  If none is available, null is
      * returned.
      *
      * @param childName the name of the child.
      * @return a StatePersistentMetadata entry, or null if none is appropriate.
      */
     private StatePersistentMetadata getChildMetadata(String childName) {
         return _metadataRef == null ? null : _metadataRef.getChild(childName);
     }
 
     /**
      * @return our metadata info, if there is any, otherwise null.
      */
     private Map<String, String> getMetadataInfo() {
         return _metadataRef == null ? null : _metadataRef.getMetadata();
     }
 
 
 
/** Add a new component to our children's list. */
 private void addComponent(String childName, StateComponent newChild){}

 

}