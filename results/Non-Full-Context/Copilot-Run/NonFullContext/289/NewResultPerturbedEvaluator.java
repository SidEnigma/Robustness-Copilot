/*
  * #%L
  * ACS AEM Commons Bundle
  * %%
  * Copyright (C) 2018 Adobe
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
 package com.adobe.acs.commons.ondeploy.impl;
 
 import com.adobe.acs.commons.ondeploy.OnDeployExecutor;
 import com.adobe.acs.commons.ondeploy.OnDeployScriptProvider;
 import com.adobe.acs.commons.ondeploy.scripts.OnDeployScript;
 import com.adobe.acs.commons.util.RequireAem;
 import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
 import com.day.cq.commons.jcr.JcrConstants;
 import org.apache.commons.lang3.exception.ExceptionUtils;
 import org.apache.felix.scr.annotations.Component;
 import org.apache.felix.scr.annotations.ConfigurationPolicy;
 import org.apache.felix.scr.annotations.Properties;
 import org.apache.felix.scr.annotations.Property;
 import org.apache.felix.scr.annotations.Reference;
 import org.apache.felix.scr.annotations.ReferenceCardinality;
 import org.apache.felix.scr.annotations.ReferencePolicy;
 import org.apache.felix.scr.annotations.Service;
 import org.apache.sling.api.resource.LoginException;
 import org.apache.sling.api.resource.ModifiableValueMap;
 import org.apache.sling.api.resource.PersistenceException;
 import org.apache.sling.api.resource.Resource;
 import org.apache.sling.api.resource.ResourceResolver;
 import org.apache.sling.api.resource.ResourceResolverFactory;
 import org.apache.sling.api.resource.ValueMap;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import javax.management.DynamicMBean;
 import javax.management.NotCompliantMBeanException;
 import javax.management.openmbean.CompositeDataSupport;
 import javax.management.openmbean.CompositeType;
 import javax.management.openmbean.OpenDataException;
 import javax.management.openmbean.OpenType;
 import javax.management.openmbean.SimpleType;
 import javax.management.openmbean.TabularDataSupport;
 import javax.management.openmbean.TabularType;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Date;
 import java.util.Calendar;
 import java.util.concurrent.CopyOnWriteArrayList;
 import java.util.concurrent.atomic.AtomicBoolean;
 
 /**
  * A service that triggers scripts on deployment to an AEM server.
  * <p>
  * This class manages scripts so that they only run once (unless the script
  * fails).  Script execution statuses are stored in the JCR @
  * /var/acs-commons/on-deploy-scripts-status.
  * <p>
  * Scripts are specified via OSGi config, are are run in the order specified.
  * <p>
  * NOTE: Since it's always a possibility that
  * /var/acs-commons/on-deploy-scripts-status will be deleted in the JCR,
  * scripts should be written defensively in case they are actually run more
  * than once.  This also covers the scenario where a script is run a second
  * time after failing the first time.
  */
 @Component(metatype = false, policy = ConfigurationPolicy.REQUIRE)
 @Properties({ @Property(label = "MBean Name", name = "jmx.objectname",
         value = "com.adobe.acs.commons:type=On-Deploy Scripts") })
 @Service(value = {DynamicMBean.class, OnDeployExecutorMBean.class, OnDeployExecutor.class})
 public class OnDeployExecutorImpl extends AnnotatedStandardMBean implements OnDeployExecutorMBean, OnDeployExecutor {
     static final String SCRIPT_STATUS_JCR_FOLDER = "/var/acs-commons/on-deploy-scripts-status";
 
     private static final String SCRIPT_DATE_END = "endDate";
     private static final String SCRIPT_DATE_START = "startDate";
     private static final String SCRIPT_OUTPUT = "output";
     private static final String SCRIPT_STATUS = "status";
     private static final String SCRIPT_STATUS_FAIL = "fail";
     private static final String SCRIPT_STATUS_RUNNING = "running";
     private static final String SCRIPT_STATUS_SUCCESS = "success";
 
     private static final String SERVICE_NAME = "on-deploy-scripts";
 
     private static final Logger logger = LoggerFactory.getLogger(OnDeployExecutorImpl.class);
 
     @Reference
     private ResourceResolverFactory resourceResolverFactory;
     
     @Reference(target="(distribution=classic)")
     RequireAem requireAem;
 
     @Reference(name = "scriptProvider", referenceInterface = OnDeployScriptProvider.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
     private List<OnDeployScriptProvider> scriptProviders = new CopyOnWriteArrayList<>();
 
     private static transient String[] scriptsItemNames;
     private static transient CompositeType scriptsCompositeType;
     private static transient TabularType scriptsTabularType;
 
     static {
         try {
             scriptsItemNames = new String[] { "_provider", "_script", SCRIPT_DATE_START, SCRIPT_DATE_END, SCRIPT_STATUS };
             scriptsCompositeType =
                     new CompositeType("Script Row", "single script status row", scriptsItemNames, new String[] {
                             "Provider", "Script", "Start Date", "End Date", "Status" },
                             new OpenType[] { SimpleType.STRING, SimpleType.STRING, SimpleType.DATE, SimpleType.DATE,
                                     SimpleType.STRING });
             scriptsTabularType =
                     new TabularType("Scripts", "On-Deploy Scripts", scriptsCompositeType, new String[] { "_provider",
                             "_script" });
 
         } catch (OpenDataException ex) {
             logger.error("Unable to build MBean composite types", ex);
         }
     }
 
     public OnDeployExecutorImpl() throws NotCompliantMBeanException {
         super(OnDeployExecutorMBean.class);
     }
 
     //TODO: Is this really necessary?? This is default behavior, no need to specify explicitly here
     protected void bindResourceResolverFactory(ResourceResolverFactory resourceResolverFactory) {
         this.resourceResolverFactory = resourceResolverFactory;
     }
 
     /**
      * Executes all on-deploy scripts on bind of a script provider.
      */
     protected void bindScriptProvider(OnDeployScriptProvider scriptProvider) {
         logger.info("Executing on-deploy scripts from scriptProvider: {}", scriptProvider.getClass().getName());
         scriptProviders.add(scriptProvider);
 
         List<OnDeployScript> scripts = scriptProvider.getScripts();
         if (scripts.size() == 0) {
             logger.debug("No on-deploy scripts found.");
             return;
         }
         
         try (ResourceResolver resourceResolver = logIn()) {
             runScripts(resourceResolver, scripts);
         }
     }
 
     protected void unbindScriptProvider(OnDeployScriptProvider scriptProvider) {
        scriptProviders.remove(scriptProvider);
     }
 
     protected Resource getOrCreateStatusTrackingResource(ResourceResolver resourceResolver, Class<?> scriptClass) {
         String scriptClassName = scriptClass.getName();
         Resource resource = resourceResolver.getResource(SCRIPT_STATUS_JCR_FOLDER + "/" + scriptClassName);
         if (resource == null) {
             Resource folder = resourceResolver.getResource(SCRIPT_STATUS_JCR_FOLDER);
             try {
                 resource = resourceResolver.create(folder, scriptClassName, Collections.singletonMap(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED));
             } catch (PersistenceException re) {
                 logger.error("On-deploy script cannot be run because the system could not find or create the script status node: {}/{}", SCRIPT_STATUS_JCR_FOLDER, scriptClassName);
                 throw new OnDeployEarlyTerminationException(re);
             }
         }
         return resource;
 
     }
 
     protected String getScriptStatus(Resource statusResource) {
         return statusResource.getValueMap().get(SCRIPT_STATUS, (String) null);
     }
 
     protected ResourceResolver logIn() {
         try {
             Map<String, Object> userParams = new HashMap<>();
             userParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_NAME);
             return resourceResolverFactory.getServiceResourceResolver(userParams);
         } catch (LoginException le2) {
             logger.error("On-deploy scripts cannot be run because the system cannot log in with the appropriate service user");
             throw new OnDeployEarlyTerminationException(le2);
         }
     }
 
 
/** Run the OnDeployScript only if the previous run were not successfully. */

protected boolean runScript(ResourceResolver resourceResolver, OnDeployScript script) {
    Resource statusResource = getOrCreateStatusTrackingResource(resourceResolver, script.getClass());
    String scriptStatus = getScriptStatus(statusResource);
    
    if (scriptStatus == null || !scriptStatus.equals(SCRIPT_STATUS_SUCCESS)) {
        ModifiableValueMap valueMap = statusResource.adaptTo(ModifiableValueMap.class);
        valueMap.put(SCRIPT_STATUS, SCRIPT_STATUS_RUNNING);
        valueMap.put(SCRIPT_DATE_START, Calendar.getInstance());
        
        try {
            script.execute(resourceResolver);
            valueMap.put(SCRIPT_STATUS, SCRIPT_STATUS_SUCCESS);
        } catch (Exception e) {
            logger.error("Error executing on-deploy script: {}", ExceptionUtils.getStackTrace(e));
            valueMap.put(SCRIPT_STATUS, SCRIPT_STATUS_FAIL);
            valueMap.put(SCRIPT_OUTPUT, ExceptionUtils.getStackTrace(e));
            return false;
        } finally {
            valueMap.put(SCRIPT_DATE_END, Calendar.getInstance());
        }
    }
    
    return true;
}
 

}