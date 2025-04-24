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
 
 
/** This function checks whether our parent should expunge us. */
 public boolean hasExpired(){}

 

}