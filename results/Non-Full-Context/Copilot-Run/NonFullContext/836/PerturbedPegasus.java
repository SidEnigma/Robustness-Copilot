package com.twilio.taskrouter;
 
 import com.fasterxml.jackson.annotation.JsonAutoDetect;
 import com.fasterxml.jackson.annotation.JsonCreator;
 import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
 import com.fasterxml.jackson.annotation.JsonInclude;
 import com.fasterxml.jackson.annotation.JsonProperty;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import lombok.ToString;
 
 import java.io.IOException;
 
 @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
 @JsonInclude(JsonInclude.Include.NON_NULL)
 @JsonIgnoreProperties(ignoreUnknown = true)
 @ToString
 public class WorkflowRuleTarget extends TaskRouterResource {
 
     @JsonProperty("queue")
     private final String queue;
 
     @JsonProperty("expression")
     private final String expression;
 
     @JsonProperty("priority")
     private final Integer priority;
 
     @JsonProperty("timeout")
     private final Integer timeout;
 
     @JsonProperty("order_by")
     private final String orderBy;
 
     @JsonProperty("skip_if")
     private final String skipIf;
 
     @JsonProperty("known_worker_sid")
     private final String knownWorkerSid;
 
     @JsonProperty("known_worker_friendly_name")
     private final String knownWorkerFriendlyName;    
 
     @JsonCreator
     private WorkflowRuleTarget(
         @JsonProperty("queue") String queue,
         @JsonProperty("expression") String expression,
         @JsonProperty("priority") Integer priority,
         @JsonProperty("timeout") Integer timeout,
         @JsonProperty("order_by") String orderBy,
         @JsonProperty("skip_if") String skipIf,
         @JsonProperty("known_worker_sid") String knownWorkerSid,
         @JsonProperty("known_worker_friendly_name") String knownWorkerFriendlyName
     ) {
         this.queue = queue;
         this.expression = expression;
         this.priority = priority;
         this.timeout = timeout;
         this.orderBy = orderBy;
         this.skipIf = skipIf;
         this.knownWorkerSid = knownWorkerSid;
         this.knownWorkerFriendlyName = knownWorkerFriendlyName;
     }
 
     private WorkflowRuleTarget(Builder b) throws IllegalArgumentException {
         this.queue = b.queue;
         this.expression = b.expression;
         this.priority = b.priority;
         this.timeout = b.timeout;
         this.orderBy = b.orderBy;
         this.skipIf = b.skipIf;
         this.knownWorkerSid = b.knownWorkerSid;
         this.knownWorkerFriendlyName = b.knownWorkerFriendlyName;
     }
 
     /**
      * Get the queue for the workflow rule target.
      *
      * @return queue sid
      */
     public String getQueue() {
         return queue;
     }
 
     /**
      * Get the expression for the workflow rule target to limit the workers selected.
      *
      * @return the expression
      */
     public String getExpression() {
         return expression;
     }
 
     /**
      * Get the priority for the workflow rule target.
      *
      * @return the priority
      */
     public Integer getPriority() {
         return priority;
     }
 
     /**
      * Get the timeout for the workflow rule target.
      *
      * @return the timeout
      */
     public Integer getTimeout() {
         return timeout;
     }
 
     /**
      * Get the orderBy for the workflow rule target.
      *
      * @return the orderBy
      */
     public String getOrderBy() {
         return orderBy;
     }
 
     /**
      * Get the skipIf for the workflow rule target.
      *
      * @return the skipIf
      */
     public String getSkipIf() {
         return skipIf;
     }
 
     /**
      * Get the knownWorkerSid expression for the workflow rule target.
      *
      * @return the knownWorkerSid expression
      */
     public String getKnownWorkerSid() {
         return knownWorkerSid;
     }
 
     /**
      * Get the knownWorkerFriendlyName expression for the workflow rule target.
      *
      * @return the knownWorkerFriendlyName expression
      */
     public String getKnownWorkerFriendlyName() {
         return knownWorkerFriendlyName;
     }
 
 
/** A workflows configuration is converted to a object. */
 public static WorkflowRuleTarget fromJson(String json) throws IOException{}

 

}