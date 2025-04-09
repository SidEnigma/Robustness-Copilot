/*
  * #%L
  * ACS AEM Commons Bundle
  * %%
  * Copyright (C) 2013 Adobe
  * %%
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  * #L%
  */
 
 package com.adobe.acs.commons.replication.dispatcher.impl;
 
 import com.adobe.acs.commons.replication.dispatcher.DispatcherFlushFilter;
 import com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher;
 import com.adobe.acs.commons.replication.dispatcher.DispatcherFlushFilter.FlushType;
 import com.adobe.acs.commons.util.ParameterUtil;
 import com.day.cq.replication.AgentManager;
 import com.day.cq.replication.Preprocessor;
 import com.day.cq.replication.ReplicationAction;
 import com.day.cq.replication.ReplicationActionType;
 import com.day.cq.replication.ReplicationException;
 import com.day.cq.replication.ReplicationOptions;
 
 import org.apache.commons.lang.StringUtils;
 import org.apache.felix.scr.annotations.Activate;
 import org.apache.felix.scr.annotations.Component;
 import org.apache.felix.scr.annotations.ConfigurationPolicy;
 import org.apache.felix.scr.annotations.Deactivate;
 import org.apache.felix.scr.annotations.Properties;
 import org.apache.felix.scr.annotations.Property;
 import org.apache.felix.scr.annotations.PropertyOption;
 import org.apache.felix.scr.annotations.Reference;
 import org.apache.felix.scr.annotations.ReferencePolicyOption;
 import org.apache.felix.scr.annotations.Service;
 import org.apache.sling.api.resource.LoginException;
 import org.apache.sling.api.resource.ResourceResolver;
 import org.apache.sling.api.resource.ResourceResolverFactory;
 import org.apache.sling.commons.osgi.PropertiesUtil;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.LinkedHashMap;
 import java.util.Map;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 @Component(
         label = "ACS AEM Commons - Dispatcher Flush Rules",
         description = "Facilitates the flushing of associated paths based on resources being replicated. "
                 + "All flushes use the AEM Replication APIs and support queuing on the Replication Agent."
                 + "ResourceOnly flushes require Replication Flush Agents with the HTTP Header of "
                 + "'CQ-Action-Scope: ResourceOnly'."
                 + "Neither rule sets supports chaining; { /a/.*=/b/c -> /b/.*=/d/e }, "
                 + "due to dangerous cyclic conditions.",
         metatype = true,
         configurationFactory = true,
         policy = ConfigurationPolicy.REQUIRE)
 @Service
 @Properties({
         @Property(
                 name = "webconsole.configurationFactory.nameHint",
                 value = "Rule: {prop.replication-action-type}, for Hierarchy: [{prop.rules.hierarchical}] or Resources: [{prop.rules.resource-only}]")
 })
 public class DispatcherFlushRulesImpl implements Preprocessor {
     private static final Logger log = LoggerFactory.getLogger(DispatcherFlushRulesImpl.class);
 
     private static final String OPTION_INHERIT = "INHERIT";
     private static final String OPTION_ACTIVATE = "ACTIVATE";
     private static final String OPTION_DELETE = "DELETE";
 
 
     private static final DispatcherFlushFilter HIERARCHICAL_FILTER =
             new DispatcherFlushRulesFilter(FlushType.Hierarchical);
     private static final DispatcherFlushFilter RESOURCE_ONLY_FILTER =
             new DispatcherFlushRulesFilter(FlushType.ResourceOnly);
 
     /* Replication Action Type Property */
 
     private static final String DEFAULT_REPLICATION_ACTION_TYPE_NAME = OPTION_INHERIT;
     @Property(label = "Replication Action Type",
             description = "The Replication Action Type to use when issuing the flush cmd to the associated paths. "
                     + "If 'Inherit' is selected, the Replication Action Type of the observed Replication Action "
                     + "will be used.",
             options = {
                     @PropertyOption(name = OPTION_INHERIT, value = "Inherit"),
                     @PropertyOption(name = OPTION_ACTIVATE, value = "Invalidate Cache"),
                     @PropertyOption(name = OPTION_DELETE, value = "Delete Cache")
             })
     private static final String PROP_REPLICATION_ACTION_TYPE_NAME = "prop.replication-action-type";
 
 
     /* Flush Rules */
     private static final String[] DEFAULT_HIERARCHICAL_FLUSH_RULES = {};
 
     @Property(label = "Flush Rules (Hierarchical)",
             description = "Pattern to Path associations for flush rules."
                     + "Format: <pattern-of-trigger-content>=<path-to-flush>",
             cardinality = Integer.MAX_VALUE,
             value = { })
     private static final String PROP_FLUSH_RULES = "prop.rules.hierarchical";
 
 
     /* Flush Rules */
     private static final String[] DEFAULT_RESOURCE_ONLY_FLUSH_RULES = {};
 
     @Property(label = "Flush Rules (ResourceOnly)",
             description = "Pattern to Path associations for flush rules. "
                     + "Format: <pattern-of-trigger-content>=<path-to-flush>",
             cardinality = Integer.MAX_VALUE,
             value = { })
     private static final String PROP_RESOURCE_ONLY_FLUSH_RULES = "prop.rules.resource-only";
 
     private static final String SERVICE_NAME = "dispatcher-flush";
     protected static final Map<String, Object> AUTH_INFO;
 
     static {
         AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
     }
 
     @Reference(policyOption = ReferencePolicyOption.GREEDY)
     private DispatcherFlusher dispatcherFlusher;
 
     @Reference
     private AgentManager agentManager;
 
     @Reference
     private ResourceResolverFactory resourceResolverFactory;
 
     private Map<Pattern, String[]> hierarchicalFlushRules = new LinkedHashMap<Pattern, String[]>();
     private Map<Pattern, String[]> resourceOnlyFlushRules = new LinkedHashMap<Pattern, String[]>();
     private ReplicationActionType replicationActionType = null;
 
     /**
      * {@inheritDoc}
      */
     @Override
     @SuppressWarnings("squid:S3776")
     public final void preprocess(final ReplicationAction replicationAction,
                                  final ReplicationOptions replicationOptions) throws ReplicationException {
         if (!this.accepts(replicationAction, replicationOptions)) {
             return;
         }
 
         // Path being replicated
         final String path = replicationAction.getPath();
 
         // Replication action type occurring
         final ReplicationActionType flushActionType =
                 replicationActionType == null ? replicationAction.getType() : replicationActionType;
 
         try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)){
 
             // Flush full content hierarchies
             for (final Map.Entry<Pattern, String[]> entry : this.hierarchicalFlushRules.entrySet()) {
                 final Pattern pattern = entry.getKey();
                 final Matcher m = pattern.matcher(path);
 
                 if (m.matches()) {
                     for (final String value : entry.getValue()) {
                         final String flushPath = m.replaceAll(value);
     
                         log.debug("Requesting hierarchical flush of associated path: {} ~> {}", path,
                                 flushPath);
                         dispatcherFlusher.flush(resourceResolver, flushActionType, false,
                                 HIERARCHICAL_FILTER,
                                 flushPath);
                     }
                 }
             }
 
             // Flush explicit resources using the CQ-Action-Scope ResourceOnly header
             for (final Map.Entry<Pattern, String[]> entry : this.resourceOnlyFlushRules.entrySet()) {
                 final Pattern pattern = entry.getKey();
                 final Matcher m = pattern.matcher(path);
 
                 if (m.matches()) {
                     for (final String value : entry.getValue()) {
                         final String flushPath = m.replaceAll(value);
     
                         log.debug("Requesting ResourceOnly flush of associated path: {} ~> {}", path, entry.getValue());
                         dispatcherFlusher.flush(resourceResolver, flushActionType, false,
                                 RESOURCE_ONLY_FILTER,
                                 flushPath);
                     }
                 }
             }
 
         } catch (ReplicationException e) {
             // ReplicationException must be caught here, as otherwise this will prevent the replication at all
             log.error("Error issuing dispatcher flush rules, some downstream replication exception occurred: {}", e.getMessage(), e);
         } catch (LoginException e) {
             log.error("Error issuing dispatcher flush rules due to a repository login exception: {}", e.getMessage(), e);
         }
     }
 
 
/** This service is checked to see if it should respond to this action. */

private boolean accepts(final ReplicationAction replicationAction, final ReplicationOptions replicationOptions) {
    // Add your implementation logic here
    
    // Example implementation:
    if (replicationAction.getType() == ReplicationActionType.ACTIVATE) {
        return true;
    } else {
        return false;
    }
}
 

}