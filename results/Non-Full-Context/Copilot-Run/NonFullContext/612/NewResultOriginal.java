// Copyright (c) 2017, 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.weblogic.domain.model;
 
 import java.time.OffsetDateTime;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Objects;
 import java.util.function.Predicate;
 import java.util.stream.Collectors;
 import javax.annotation.Nonnull;
 import javax.annotation.Nullable;
 
 import jakarta.json.JsonPatchBuilder;
 import jakarta.validation.Valid;
 import oracle.kubernetes.json.Description;
 import oracle.kubernetes.json.Range;
 import oracle.kubernetes.utils.SystemClock;
 import org.apache.commons.lang3.builder.EqualsBuilder;
 import org.apache.commons.lang3.builder.HashCodeBuilder;
 import org.apache.commons.lang3.builder.ToStringBuilder;
 
 import static oracle.kubernetes.operator.WebLogicConstants.SHUTDOWN_STATE;
 import static oracle.kubernetes.weblogic.domain.model.ObjectPatch.createObjectPatch;
 
 /**
  * DomainStatus represents information about the status of a domain. Status may trail the actual
  * state of a system.
  */
 @Description("The current status of the operation of the WebLogic domain. Updated automatically by the operator.")
 public class DomainStatus {
 
   @Description("Current service state of the domain.")
   @Valid
   private List<DomainCondition> conditions = new ArrayList<>();
 
   @Description(
       "A human readable message indicating details about why the domain is in this condition.")
   private String message;
 
   @Description(
       "A brief CamelCase message indicating details about why the domain is in this state.")
   private String reason;
 
   @Description(
       "Non-zero if the introspector job fails for any reason. "
           + "You can configure an introspector job retry limit for jobs that log script failures using "
           + "the Operator tuning parameter 'domainPresenceFailureRetryMaxCount' (default 5). "
           + "You cannot configure a limit for other types of failures, such as a Domain resource reference "
           + "to an unknown secret name; in which case, the retries are unlimited.")
   @Range(minimum = 0)
   private Integer introspectJobFailureCount = 0;
 
   @Description(
           "Unique id of the last introspector job that was processed for this domain.")
   private String lastIntrospectJobProcessedUid;
 
   @Description("Status of WebLogic Servers in this domain.")
   @Valid
   // sorted list of ServerStatus
   private final List<ServerStatus> servers;
 
   @Description("Status of WebLogic clusters in this domain.")
   @Valid
   // sorted list of ClusterStatus
   private List<ClusterStatus> clusters = new ArrayList<>();
 
   @Description(
       "RFC 3339 date and time at which the operator started the domain. This will be when "
           + "the operator begins processing and will precede when the various servers "
           + "or clusters are available.")
   private OffsetDateTime startTime = SystemClock.now();
 
   @Description(
       "The number of running cluster member Managed Servers in the WebLogic cluster if there is "
       + "exactly one cluster defined in the domain configuration and where the `replicas` field is set at the `spec` "
       + "level rather than for the specific cluster under `clusters`. This field is provided to support use of "
       + "Kubernetes scaling for this limited use case.")
   @Range(minimum = 0)
   private Integer replicas;
 
   public DomainStatus() {
     servers = new ArrayList<>();
   }
 
   /**
    * A copy constructor that creates a deep copy.
    * @param that the object to copy
    */
   public DomainStatus(DomainStatus that) {
     message = that.message;
     reason = that.reason;
     conditions = that.conditions.stream().map(DomainCondition::new).collect(Collectors.toList());
     servers = that.servers.stream().map(ServerStatus::new).collect(Collectors.toList());
     clusters = that.clusters.stream().map(ClusterStatus::new).collect(Collectors.toList());
     startTime = that.startTime;
     replicas = that.replicas;
     introspectJobFailureCount = that.introspectJobFailureCount;
     lastIntrospectJobProcessedUid = that.lastIntrospectJobProcessedUid;
   }
 
   /**
    * Current service state of domain.
    *
    * @return conditions
    */
   public @Nonnull List<DomainCondition> getConditions() {
     return conditions;
   }
 
 
/** Adds a condition to the status, replacing any existing conditions with the same type, and removing other  conditions according to the domain rules. */

public DomainStatus addCondition(DomainCondition newCondition) {
    // Remove any existing conditions with the same type
    conditions.removeIf(condition -> Objects.equals(condition.getType(), newCondition.getType()));

    // Add the new condition
    conditions.add(newCondition);

    // Remove other conditions according to the domain rules
    conditions.removeIf(condition -> {
        // Remove conditions with type SHUTDOWN_STATE if there is a condition with type SHUTDOWN_STATE
        if (Objects.equals(condition.getType(), SHUTDOWN_STATE)) {
            return true;
        }
        
        // Add more domain rules here
        
        return false;
    });

    return this;
}
 

}