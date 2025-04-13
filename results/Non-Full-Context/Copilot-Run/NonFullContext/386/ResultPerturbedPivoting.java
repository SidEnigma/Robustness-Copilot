/*
  * #%L
  * ACS AEM Commons Bundle
  * %%
  * Copyright (C) 2021 Adobe
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
 
 package com.adobe.acs.commons.ccvar.filter;
 
 import com.adobe.acs.commons.ccvar.PropertyAggregatorService;
 import com.adobe.acs.commons.ccvar.PropertyConfigService;
 import com.adobe.acs.commons.ccvar.util.ContentVariableReplacementUtil;
 import com.fasterxml.jackson.databind.JsonNode;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import com.fasterxml.jackson.databind.node.ArrayNode;
 import com.fasterxml.jackson.databind.node.ObjectNode;
 import org.apache.commons.lang3.StringUtils;
 import org.apache.sling.api.SlingHttpServletRequest;
 import org.apache.sling.api.SlingHttpServletResponse;
 import org.apache.sling.api.request.RequestPathInfo;
 import org.apache.sling.engine.EngineConstants;
 import org.osgi.framework.Constants;
 import org.osgi.service.component.annotations.Activate;
 import org.osgi.service.component.annotations.Component;
 import org.osgi.service.component.annotations.ConfigurationPolicy;
 import org.osgi.service.component.annotations.Reference;
 import org.osgi.service.metatype.annotations.AttributeDefinition;
 import org.osgi.service.metatype.annotations.Designate;
 import org.osgi.service.metatype.annotations.ObjectClassDefinition;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import javax.servlet.Filter;
 import javax.servlet.FilterChain;
 import javax.servlet.FilterConfig;
 import javax.servlet.ServletException;
 import javax.servlet.ServletRequest;
 import javax.servlet.ServletResponse;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.regex.Pattern;
 import java.util.regex.PatternSyntaxException;
 
 /**
  * Filter used to look for and rewrite content variables present in JSON responses. By default only handles .model.json
  * requests.
  */
 @Component(
         service = Filter.class,
         property = {
                 Constants.SERVICE_RANKING + ":Integer=" + Integer.MIN_VALUE,
                 EngineConstants.SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_REQUEST,
                 EngineConstants.SLING_FILTER_PATTERN + "=/content/.*"
         },
         configurationPolicy = ConfigurationPolicy.REQUIRE
 )
 @Designate(ocd = ContentVariableJsonFilter.Config.class)
 public class ContentVariableJsonFilter implements Filter {
 
     private static final Logger LOG = LoggerFactory.getLogger(ContentVariableJsonFilter.class);
 
     @Reference
     private PropertyAggregatorService propertyAggregatorService;
 
     @Reference
     private PropertyConfigService propertyConfigService;
 
     private List<Pattern> includePatterns;
     private List<Pattern> excludePatterns;
     private boolean allInvalidIncludes;
 
     @Override
     public void init(FilterConfig filterConfig) throws ServletException {
         // do nothing
     }
 
     @Override
     public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
         SlingHttpServletRequest slingHttpServletRequest = (SlingHttpServletRequest) servletRequest;
         RequestPathInfo currentPathInfo = slingHttpServletRequest.getRequestPathInfo();
         if (StringUtils.equals(currentPathInfo.getExtension(), "json") && shouldProcess(slingHttpServletRequest.getPathInfo())) {
             CapturingResponseWrapper capturingResponseWrapper = new CapturingResponseWrapper((SlingHttpServletResponse) servletResponse);
             filterChain.doFilter(servletRequest, capturingResponseWrapper);
 
             String currentResponse = capturingResponseWrapper.getCaptureAsString();
             String toReturn = currentResponse;
             try {
                 Map<String, Object> contentVariableReplacements = propertyAggregatorService.getProperties(slingHttpServletRequest);
                 if (contentVariableReplacements.size() > 0) {
                     ObjectMapper objectMapper = new ObjectMapper();
                     JsonNode currentTree = objectMapper.readTree(currentResponse);
                     replaceInElements(currentTree, contentVariableReplacements);
                     toReturn = currentTree.toString();
                 }
             } catch (Exception e) {
                 LOG.error("Exception during JSON property replacement", e);
             } finally {
                 servletResponse.getWriter().write(toReturn);
             }
         } else {
             filterChain.doFilter(servletRequest, servletResponse);
         }
     }
 
 
/** Checks the current URL path against the included and excluded templates. */
 private boolean shouldProcess(String urlPath){
        if (includePatterns == null || excludePatterns == null) {
            return false;
        }
        for (Pattern includePattern : includePatterns) {
            if (includePattern.matcher(urlPath).matches()) {
                for (Pattern excludePattern : excludePatterns) {
                    if (excludePattern.matcher(urlPath).matches()) {
                        return false;
                    }
                }
                return true;
            }
        }
        return allInvalidIncludes;
    }
    
        @Override
        public void destroy() {
            // do nothing
        }
    
        @Activate
        protected void activate(ContentVariableJsonFilter.Config config) {
            includePatterns = new LinkedList<>();
            excludePatterns = new LinkedList<>();
            allInvalidIncludes = false;
            if (config.include() != null) {
                for (String include : config.include()) {
                    try {
                        includePatterns.add(Pattern.compile(include));
                    } catch (PatternSyntaxException e) {
                        allInvalidIncludes = true;
                        LOG.error("Invalid include pattern: " + include, e);
                    }
                }
            }
            if (config.exclude() != null) {
                for (String exclude : config.exclude()) {
                    try {
                        excludePatterns.add(Pattern.compile(exclude));
                    } catch (PatternSyntaxException e) {
                        LOG.error("Invalid exclude pattern: " + exclude, e);
                    }
                }
            }
        }
    
        private void replaceInElements(JsonNode currentTree, Map<String, Object> contentVariableReplacements) {
            if (currentTree.isObject()) {
                ObjectNode currentObject = (ObjectNode) currentTree;
                Iterator<Map.Entry<String, JsonNode>> fields = currentObject.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    replaceInElements(field.getValue(), contentVariableReplacements);
                }
            } else if (current      
 }

 

}