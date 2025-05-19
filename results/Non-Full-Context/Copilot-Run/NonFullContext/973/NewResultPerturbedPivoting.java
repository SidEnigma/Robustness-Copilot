/*
  * #%L
  * ACS AEM Commons Bundle
  * %%
  * Copyright (C) 2017 Adobe
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
 
 package com.adobe.acs.commons.dam.impl;
 
 import org.apache.commons.lang3.StringUtils;
 import org.apache.felix.scr.annotations.Component;
 import org.apache.felix.scr.annotations.ConfigurationPolicy;
 import org.apache.felix.scr.annotations.Properties;
 import org.apache.felix.scr.annotations.Property;
 import org.apache.felix.scr.annotations.Reference;
 import org.apache.felix.scr.annotations.Service;
 import org.apache.sling.api.SlingHttpServletRequest;
 import org.apache.sling.api.SlingHttpServletResponse;
 import org.apache.sling.api.resource.Resource;
 import org.apache.sling.api.resource.ValueMap;
 import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
 import org.apache.sling.api.wrappers.CompositeValueMap;
 import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
 import org.apache.sling.api.wrappers.ValueMapDecorator;
 import org.apache.sling.jcr.resource.api.JcrResourceConstants;
 import org.apache.sling.servlets.post.AbstractPostResponse;
 import org.apache.sling.servlets.post.Modification;
 import org.apache.sling.servlets.post.PostOperation;
 import org.apache.sling.servlets.post.SlingPostProcessor;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import javax.servlet.Filter;
 import javax.servlet.FilterChain;
 import javax.servlet.FilterConfig;
 import javax.servlet.ServletException;
 import javax.servlet.ServletRequest;
 import javax.servlet.ServletResponse;
 import javax.servlet.http.HttpServletResponse;
 import java.io.IOException;
 import java.util.HashMap;
 import java.util.List;
 
 @Component(
         policy = ConfigurationPolicy.REQUIRE,
         immediate = true
 )
 @Properties({
         @Property(
                 name = "service.ranking",
                 intValue = -2000
         ),
         @Property(
                 name = "sling.filter.scope",
                 value = "REQUEST"
         ),
         @Property(
                 name = "sling.filter.pattern",
                 value = "/content/dam/.*"
         ),
         @Property(
                 name = "sling.servlet.methods",
                 value = "GET"
         ),
         @Property(
                 name = "sling.servlet.resourceTypes",
                 value = "acs-commons/touchui-widgets/asset-folder-properties-support"
         )
 })
 @Service
 public class AssetsFolderPropertiesSupport extends SlingSafeMethodsServlet implements Filter, SlingPostProcessor {
     private static final Logger log = LoggerFactory.getLogger(AssetsFolderPropertiesSupport.class);
 
     private static final String DAM_PATH_PREFIX = "/content/dam";
     private static final String POST_METHOD = "post";
     private static final String OPERATION = ":operation";
     private static final String DAM_FOLDER_SHARE_OPERATION = "dam.share.folder";
     private static final String GRANITE_UI_FORM_VALUES = "granite.ui.form.values";
 
     /**
      * The is a reference to the OOTB AEM PostOperation that handles updates for Folder Properties; This is used below in process(..) to ensure that all OOTB behaviors are executed.
      */
     @Reference(target="&(sling.post.operation=dam.share.folder)(sling.servlet.methods=POST)")
     private transient PostOperation folderShareHandler;
 
     /**
      * This method is responsible for post processing POSTs to the FolderShareHandler PostOperation (:operation = dam.share.folder).
      * This method will store a whitelisted set of request parameters to their relative location off of the [sling:*Folder] node.
      *
      * Note, this is executed AFTER the OOTB FolderShareHandler PostOperation.
      *
      * At this time this method only supports single-value Strings and ignores all @typeHints.
      *
      * This method must fail fast via the accepts(...) method.
      *
      * @param servletRequest the request object
      * @param servletResponse the response object
      * @param chain the filter chain
      * @throws IOException
      * @throws ServletException
      */
     public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
         final SlingHttpServletRequest request = (SlingHttpServletRequest) servletRequest;
         final SlingHttpServletResponse response = (SlingHttpServletResponse) servletResponse;
 
         if (!accepts(request)) {
             chain.doFilter(request, response);
             return;
         }
 
         log.trace("ACS AEM Commons Assets Folder Properties Support applied to POST Request");
         chain.doFilter(new AssetsFolderPropertiesSupportRequest(request, null), response);
     }
 
     public void init(FilterConfig filterConfig) throws ServletException {
         // Do Nothing
     }
 
     public void destroy() {
         // Do Nothing
     }
 
     public void process(SlingHttpServletRequest request, List<Modification> changes) throws Exception {
         if (AssetsFolderPropertiesSupportRequest.isMarked(request)) {
             log.trace("Sending the the wrapped dam.folder.share request to the AEM Assets dam.folder.share PostOperation for final processing");
 
             final AssetsFolderPropertiesSupportRequest wrappedRequest = new AssetsFolderPropertiesSupportRequest(request, DAM_FOLDER_SHARE_OPERATION);
 
             folderShareHandler.run(wrappedRequest, new DummyPostResponse(), new SlingPostProcessor[]{});
 
             log.trace("Processed the the wrapped dam.folder.share request with the AEM Assets dam.folder.share PostOperation");
         }
     }
 
 
/** The gateway method used by the filter to determine whether the request is likely to be processed by supporting the properties of the Assets folder. */

protected boolean accepts(SlingHttpServletRequest request) {
    String method = request.getMethod();
    String operation = request.getParameter(OPERATION);

    // Check if the request method is GET and the operation is dam.share.folder
    if ("GET".equalsIgnoreCase(method) && DAM_FOLDER_SHARE_OPERATION.equalsIgnoreCase(operation)) {
        return true;
    }

    // Check if the request method is POST and the request path starts with /content/dam
    if ("POST".equalsIgnoreCase(method) && request.getRequestURI().startsWith(DAM_PATH_PREFIX)) {
        return true;
    }

    return false;
}
 

}