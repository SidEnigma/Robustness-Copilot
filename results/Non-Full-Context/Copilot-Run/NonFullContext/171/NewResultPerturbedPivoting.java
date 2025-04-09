/*
  * #%L
  * ACS AEM Commons Bundle
  * %%
  * Copyright (C) 2016 Adobe
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
 package com.adobe.acs.commons.redirects.filter;
 
 import com.adobe.acs.commons.redirects.LocationHeaderAdjuster;
 import com.adobe.acs.commons.redirects.models.RedirectMatch;
 import com.adobe.acs.commons.redirects.models.RedirectRule;
 import com.adobe.acs.commons.redirects.models.RedirectConfiguration;
 import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
 import com.day.cq.replication.ReplicationAction;
 import com.day.cq.replication.ReplicationEvent;
 import com.day.cq.wcm.api.WCMMode;
 import com.google.common.cache.Cache;
 import com.google.common.cache.CacheBuilder;
 import org.apache.commons.lang.StringUtils;
 import org.apache.http.Header;
 import org.apache.http.message.BasicHeader;
 import org.apache.sling.api.SlingConstants;
 import org.apache.sling.api.SlingHttpServletRequest;
 import org.apache.sling.api.SlingHttpServletResponse;
 import org.apache.sling.api.request.RequestPathInfo;
 import org.apache.sling.api.resource.LoginException;
 import org.apache.sling.api.resource.Resource;
 import org.apache.sling.api.resource.ResourceResolver;
 import org.apache.sling.api.resource.ResourceResolverFactory;
 import org.apache.sling.api.resource.observation.ResourceChange;
 import org.apache.sling.api.resource.observation.ResourceChangeListener;
 import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
 import org.apache.sling.engine.EngineConstants;
 import org.osgi.framework.BundleContext;
 import org.osgi.framework.ServiceRegistration;
 import org.osgi.service.component.annotations.Component;
 import org.osgi.service.component.annotations.Activate;
 import org.osgi.service.component.annotations.ConfigurationPolicy;
 import org.osgi.service.component.annotations.Deactivate;
 import org.osgi.service.component.annotations.Modified;
 import org.osgi.service.component.annotations.Reference;
 import org.osgi.service.component.annotations.ReferenceCardinality;
 import org.osgi.service.component.annotations.ReferencePolicy;
 import org.osgi.service.component.annotations.ReferencePolicyOption;
 import org.osgi.service.event.Event;
 import org.osgi.service.event.EventConstants;
 import org.osgi.service.event.EventHandler;
 import org.osgi.service.metatype.annotations.AttributeDefinition;
 import org.osgi.service.metatype.annotations.AttributeType;
 import org.osgi.service.metatype.annotations.Designate;
 import org.osgi.service.metatype.annotations.ObjectClassDefinition;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import javax.management.NotCompliantMBeanException;
 import javax.management.openmbean.CompositeDataSupport;
 import javax.management.openmbean.CompositeType;
 import javax.management.openmbean.OpenDataException;
 import javax.management.openmbean.OpenType;
 import javax.management.openmbean.SimpleType;
 import javax.management.openmbean.TabularData;
 import javax.management.openmbean.TabularDataSupport;
 import javax.management.openmbean.TabularType;
 import javax.servlet.Filter;
 import javax.servlet.FilterConfig;
 import javax.servlet.FilterChain;
 import javax.servlet.ServletException;
 import javax.servlet.ServletRequest;
 import javax.servlet.ServletResponse;
 import javax.servlet.http.HttpServletRequest;
 import java.io.IOException;
 import java.lang.invoke.MethodHandles;
 import java.net.URI;
 import java.time.ZonedDateTime;
 import java.util.Map;
 import java.util.List;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.LinkedHashMap;
 import java.util.Hashtable;
 import java.util.Dictionary;
 import java.util.concurrent.ExecutionException;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 import java.util.regex.Pattern;
 import java.util.stream.Collectors;
 
 import static org.apache.sling.engine.EngineConstants.SLING_FILTER_SCOPE;
 import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
 import static org.osgi.framework.Constants.SERVICE_RANKING;
 import static org.osgi.framework.Constants.SERVICE_ID;
 
 /**
  * A request filter that implements support for virtual redirects.
  */
 @Component(service = {Filter.class, RedirectFilterMBean.class, EventHandler.class},
         configurationPolicy = ConfigurationPolicy.REQUIRE, property = {
         SERVICE_DESCRIPTION + "=A request filter implementing support for virtual redirects",
         SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_REQUEST,
         // to correctly work in Author RedirectFilter needs to run after WCMRequestFilter which has rank 2000 in
         // AEM 6.5 and Cloud SDK, see issue 2707
         SERVICE_RANKING + ":Integer=1900",
         "jmx.objectname=" + "com.adobe.acs.commons:type=Redirect Manager",
         EventConstants.EVENT_TOPIC + "=" + ReplicationAction.EVENT_TOPIC,
         EventConstants.EVENT_TOPIC + "=" + ReplicationEvent.EVENT_TOPIC
 
 })
 @Designate(ocd = RedirectFilter.Configuration.class)
 public class RedirectFilter extends AnnotatedStandardMBean
         implements Filter, EventHandler, ResourceChangeListener, RedirectFilterMBean {
 
     public static final String ACS_REDIRECTS_RESOURCE_TYPE = "acs-commons/components/utilities/manage-redirects";
     public static final String REDIRECT_RULE_RESOURCE_TYPE = ACS_REDIRECTS_RESOURCE_TYPE + "/redirect-row";
 
     public static final String DEFAULT_CONFIG_BUCKET = "settings";
     public static final String DEFAULT_CONFIG_NAME = "redirects";
 
     private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
 
     private static final String SERVICE_NAME = "redirect-manager";
 
     @ObjectClassDefinition(name = "ACS Commons Redirect Filter")
     public @interface Configuration {
         @AttributeDefinition(name = "Enable Redirect Filter", description = "Indicates whether the redirect filter is enabled or not.", type = AttributeType.BOOLEAN)
         boolean enabled() default false;
 
         @AttributeDefinition(name = "Rewrite Location Header", description = "Apply Sling Resource Mappings (/etc/map) to Location header. "
                 + "Use if Location header should be rewritten using ResourceResolver#map", type = AttributeType.BOOLEAN)
         boolean mapUrls() default false;
 
         @AttributeDefinition(name = "Request Extensions", description = "List of extensions for which redirection is allowed", type = AttributeType.STRING)
         String[] extensions() default {};
 
         @AttributeDefinition(name = "Request Paths", description = "List of paths for which redirection is allowed", type = AttributeType.STRING)
         String[] paths() default {"/content"};
 
         @AttributeDefinition(name = "Preserve Query String", description = "Preserve query string in redirects", type = AttributeType.BOOLEAN)
         boolean preserveQueryString() default true;
 
         @AttributeDefinition(name = "Additional Response Headers", description = "Optional response headers in the name:value format to apply on delivery,"
                 + " e.g. Cache-Control: max-age=3600", type = AttributeType.STRING)
         String[] additionalHeaders() default {};
 
         @AttributeDefinition(name = "Configuration bucket name", description = "name of the parent folder where to store redirect rules."
                 + " Default is settings. ", type = AttributeType.STRING)
         String bucketName() default DEFAULT_CONFIG_BUCKET;
 
         @AttributeDefinition(name = "Configuration Name", description = "The node name to store redirect configurations. Default is 'redirects' "
                 + " which means the default path to store redirects is /conf/global/settings/redirects "
                 + " where 'settings' is the bucket and 'redirects' is the config name", type = AttributeType.STRING)
         String configName() default  DEFAULT_CONFIG_NAME;
     }
 
     @Reference
     ResourceResolverFactory resourceResolverFactory;
 
     @Reference
     ConfigurationResourceResolver configResolver;
 
     @Reference(
             cardinality = ReferenceCardinality.OPTIONAL,
             policy = ReferencePolicy.STATIC,
             policyOption = ReferencePolicyOption.GREEDY
     )
     LocationHeaderAdjuster urlAdjuster;
 
     private ServiceRegistration<?> listenerRegistration;
     private boolean enabled;
     private boolean mapUrls;
     private boolean preserveQueryString;
     private List<Header> onDeliveryHeaders = Collections.emptyList();
     private Collection<String> methods = Arrays.asList("GET", "HEAD");
     private Collection<String> exts = Collections.emptySet();
     private Collection<String> paths = Collections.emptySet();
     private Configuration config;
     private ExecutorService executor;
     Cache<String, RedirectConfiguration> rulesCache;
 
     public RedirectFilter() throws NotCompliantMBeanException {
         super(RedirectFilterMBean.class);
     }
 
     @Override
     public void init(FilterConfig filterConfig) throws ServletException {
         // no op
     }
 
     @Activate
     @Modified
     protected final void activate(Configuration config, BundleContext context) {
         this.config = config;
         enabled = config.enabled();
 
         if (enabled) {
             Dictionary<String, Object> properties = new Hashtable<>();
             properties.put(ResourceChangeListener.PATHS, "/conf");
             listenerRegistration = context.registerService(ResourceChangeListener.class, this, properties);
             log.debug("Registered {}:{}", SERVICE_ID, listenerRegistration.getReference().getProperty(SERVICE_ID));
 
             exts = config.extensions() == null ? Collections.emptySet()
                     : Arrays.stream(config.extensions()).filter(ext -> !ext.isEmpty()).collect(Collectors.toSet());
             paths = config.paths() == null ? Collections.emptySet() : Arrays.stream(config.paths()).filter(path -> !path.isEmpty()).collect(Collectors.toSet());
             mapUrls = config.mapUrls();
             onDeliveryHeaders = new ArrayList<>();
             for(String kv : config.additionalHeaders()){
                 int idx = kv.indexOf(':');
                 if(idx == -1 || idx > kv.length() - 1) {
                     log.error("invalid on-delivery header: {}", kv);
                     continue;
                 }
                 String name = kv.substring(0, idx).trim();
                 String value = kv.substring(idx + 1).trim();
                 onDeliveryHeaders.add(new BasicHeader(name, value));
             }
             preserveQueryString = config.preserveQueryString();
             log.debug("exts: {}, paths: {}, rewriteUrls: {}",
                     exts, paths, mapUrls);
             executor = Executors.newSingleThreadExecutor();
 
             rulesCache = CacheBuilder.newBuilder().build();
 
         }
     }
 
     @Modified
     protected void modify(BundleContext context, Configuration config) {
         deactivate();
         activate(config, context);
     }
 
 
     @Deactivate
     public void deactivate() {
         if(enabled) {
             executor.shutdown();
         }
         if (listenerRegistration != null) {
             log.debug("unregistering ... ");
             listenerRegistration.unregister();
             listenerRegistration = null;
         }
     }
 
     @Override
     public void handleEvent(Event event) {
         ReplicationEvent replicationEvent = ReplicationEvent.fromEvent(event);
         if(enabled && replicationEvent != null){
             String redirectSubPath = config.bucketName() + "/" + config.configName();
             String[] replicationPaths = replicationEvent.getReplicationAction().getPaths();
             if(replicationPaths != null) {
                 for (String path : replicationPaths) {
                     if (path.contains(redirectSubPath)) {
                         // loading redirect configurations can be expensive and needs to run
                         // asynchronously,
                         // outside of the Sling event processing chain
                         executor.submit(() -> invalidate(path));
                     }
                 }
             }
         }
     }
 
     @Override
     public void onChange(List<ResourceChange> changes) {
         if(!enabled){
             return;
         }
         String redirectSubPath = config.bucketName() + "/" + config.configName();
         for(ResourceChange e : changes){
             String path = e.getPath();
             if(path.contains(redirectSubPath)){
                 executor.submit(() -> invalidate(path));
             }
         }
     }
 
     /**
      * Detect the redirect configuration and invalidate the cached rules
      *
      * Given an even path, e.g. /conf/global/settings/redirects/redirect-rule-2
      * this method will figure out the corresponding configuration (/conf/global/settings/redirects)
      * and invalidate the cached rules
      *
      * @param changePath    the event path
      */
     void invalidate(String changePath) {
         String redirectSubPath = config.bucketName() + "/" + config.configName();
         try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(
                 Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_NAME))) {
             Resource resource = resolver.resolve(changePath);
             while(resource != null){
                 if(resource.getPath().endsWith(redirectSubPath)){
                     log.debug("invalidating {}", resource.getPath());
                     rulesCache.invalidate(resource.getPath());
                     break;
                 }
                 resource = resource.getParent();
             }
         } catch (LoginException e) {
             log.error("Failed to get resolver for {}", SERVICE_NAME, e);
         }
     }
 
     @Override
     public void invalidateAll() {
         rulesCache.invalidateAll();
     }
 
     RedirectConfiguration loadRules(String storagePath) {
         RedirectConfiguration rules = null;
         long t0 = System.currentTimeMillis();
         try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(
                 Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_NAME))) {
             Resource storageResource = resolver.getResource(storagePath);
             if(storageResource != null) {
                 String storageSuffix = getBucket() + "/" + getConfigName();
                 rules = new RedirectConfiguration(storageResource, storageSuffix);
                 log.debug("{} rules loaded from {} in {} ms", rules.getPathRules().size() + rules.getPatternRules().size(),
                         storagePath, System.currentTimeMillis() - t0);
             } else {
                 log.warn("redirects not found in {}", storagePath);
             }
         } catch (LoginException e) {
             log.error("Failed to get resolver for {}", SERVICE_NAME, e);
         }
         return rules;
     }
 
     public static Collection<RedirectRule> getRules(Resource resource) {
         Collection<RedirectRule> rules = new ArrayList<>();
         for (Resource res : resource.getChildren()) {
             if(res.isResourceType(REDIRECT_RULE_RESOURCE_TYPE)){
                 rules.add(RedirectRule.from(res.getValueMap()));
             }
         }
         return rules;
     }
 
     Cache<String, RedirectConfiguration> getRulesCache(){
         return rulesCache;
     }
 
     @Override
     public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
         if (!(request instanceof SlingHttpServletRequest)
                 || !(response instanceof SlingHttpServletResponse)) {
             chain.doFilter(request, response);
             return;
         }
 
         SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
         SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;
 
         if (isEnabled() && doesRequestMatch(slingRequest) && handleRedirect(slingRequest, slingResponse)) {
             return;
         }
         chain.doFilter(request, response);
     }
 
     boolean handleRedirect(SlingHttpServletRequest slingRequest, SlingHttpServletResponse slingResponse) {
         long t0 = System.currentTimeMillis();
         boolean redirected = false;
         RedirectMatch match = match(slingRequest);
         if (match != null) {
             RedirectRule redirectRule = match.getRule();
             ZonedDateTime untilDateTime = redirectRule.getUntilDate();
             if (untilDateTime != null && untilDateTime.isBefore(ZonedDateTime.now())) {
                 log.debug("redirect rule matched, but expired: {}", redirectRule.getUntilDate());
             } else {
                 RequestPathInfo pathInfo = slingRequest.getRequestPathInfo();
                 String resourcePath = pathInfo.getResourcePath();
 
                 String location = evaluate(match, slingRequest);
                 log.trace("matched {} to {} in {} ms", resourcePath, redirectRule.toString(),
                         System.currentTimeMillis() - t0);
 
                 log.debug("Redirecting {} to {}, statusCode: {}",
                         resourcePath, location, redirectRule.getStatusCode());
                 slingResponse.setHeader("Location", location);
                 for(Header header : onDeliveryHeaders){
                     slingResponse.addHeader(header.getName(), header.getValue());
                 }
                 slingResponse.setStatus(redirectRule.getStatusCode());
                 redirected = true;
             }
         }
         return redirected;
     }
 
     /**
      * Evaluate the rule and return the value to put in Location header
      *
      * Depending on the configuration appends query string and rewrites the result using
      * {@link ResourceResolver#map(HttpServletRequest, String)}
      */
     String evaluate(RedirectMatch match, SlingHttpServletRequest slingRequest){
         RequestPathInfo pathInfo = slingRequest.getRequestPathInfo();
         String location = match.getRule().evaluate(match.getMatcher());
         if (StringUtils.startsWith(location, "/") && !StringUtils.startsWith(location, "//")) {
             String ext = pathInfo.getExtension();
             if (ext != null && !location.endsWith(ext)) {
                 location += "." + ext;
             }
             if (mapUrls()) {
                 location = mapUrl(location, slingRequest);
             }
             if(urlAdjuster != null){
                 location = urlAdjuster.adjust(slingRequest, location);
             }
         }
         if (preserveQueryString) {
 
             String queryString = slingRequest.getQueryString();
             if (queryString != null) {
                 location = preserveQueryString(location, queryString);
             }
         }
         return location;
     }
 
     String preserveQueryString(String location, String queryString){
         int idx = location.indexOf('?');
         if (idx == -1) {
             idx = location.indexOf('#');
         }
         if (idx != -1) {
             location = location.substring(0, idx);
         }
 
         location += "?" + queryString;
 
         return location;
     }
 
     String mapUrl(String url, SlingHttpServletRequest slingRequest) {
         return slingRequest.getResourceResolver().map(slingRequest, url);
     }
 
     @Override
     public void destroy() {
         // no op
     }
 
     protected boolean mapUrls() {
         return mapUrls;
     }
 
     /**
      * @return whether redirection is enabled
      */
     protected boolean isEnabled() {
         return enabled;
     }
 
     protected Collection<String> getExtensions() {
         return Collections.unmodifiableCollection(exts);
     }
 
     protected Collection<String> getPaths() {
         return Collections.unmodifiableCollection(paths);
     }
 
     protected Collection<String> getMethods() {
         return Collections.unmodifiableCollection(methods);
     }
 
     protected List<Header> getOnDeliveryHeaders() {
         return Collections.unmodifiableList(onDeliveryHeaders);
     }
 
 
/** Check if redirection for the given request is allowed. */

private boolean doesRequestMatch(SlingHttpServletRequest request) {
    String requestPath = request.getRequestPathInfo().getResourcePath();
    String requestMethod = request.getMethod();
    String requestExtension = request.getRequestPathInfo().getExtension();

    // Check if the request path matches any allowed paths
    if (!getPaths().isEmpty()) {
        boolean pathMatched = getPaths().stream()
                .anyMatch(path -> requestPath.startsWith(path));
        if (!pathMatched) {
            return false;
        }
    }

    // Check if the request extension matches any allowed extensions
    if (!getExtensions().isEmpty() && requestExtension != null) {
        boolean extensionMatched = getExtensions().contains(requestExtension);
        if (!extensionMatched) {
            return false;
        }
    }

    // Check if the request method matches any allowed methods
    if (!getMethods().isEmpty()) {
        boolean methodMatched = getMethods().contains(requestMethod);
        if (!methodMatched) {
            return false;
        }
    }

    return true;
}
 

}