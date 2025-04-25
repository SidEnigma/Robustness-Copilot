/*
  * #%L
  * ACS AEM Commons Bundle
  * %%
  * Copyright (C) 2020 Adobe
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
 
 import com.adobe.acs.commons.util.ParameterUtil;
 import com.day.cq.replication.ContentBuilder;
 import com.day.cq.replication.ReplicationAction;
 import com.day.cq.replication.ReplicationActionType;
 import com.day.cq.replication.ReplicationContent;
 import com.day.cq.replication.ReplicationContentFactory;
 import com.day.cq.replication.ReplicationException;
 import com.day.cq.replication.ReplicationLog;
 import org.apache.commons.collections.MapUtils;
 import org.apache.commons.io.FilenameUtils;
 import org.apache.commons.io.IOUtils;
 import org.apache.commons.lang3.ArrayUtils;
 import org.apache.commons.lang3.StringUtils;
 import org.osgi.framework.Constants;
 import org.osgi.service.component.annotations.Activate;
 import org.osgi.service.component.annotations.Component;
 import org.osgi.service.component.annotations.ConfigurationPolicy;
 import org.osgi.service.metatype.annotations.AttributeDefinition;
 import org.osgi.service.metatype.annotations.Designate;
 import org.osgi.service.metatype.annotations.ObjectClassDefinition;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import javax.jcr.Session;
 import java.io.BufferedWriter;
 import java.io.IOException;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.LinkedHashMap;
 import java.util.Map;
 import java.util.Optional;
 import java.util.regex.PatternSyntaxException;
 
 /**
  * Custom dispatcher flush content builder that sends a list of URIs to be re-fetched immediately upon flushing a page.
  */
 @Designate(ocd = RefetchFlushContentBuilderImpl.Config.class)
 @Component(
         service = { ContentBuilder.class },
         configurationPolicy = ConfigurationPolicy.OPTIONAL,
         property = {
             Constants.SERVICE_DESCRIPTION + "=ACS Commons Re-fetch Flush Content Builder",
             "webconsole.configurationFactory.nameHint=Extension Mapping: [{extension.pairs}] Match: [{match.paths}]",
             "name=" + RefetchFlushContentBuilderImpl.SERVICE_NAME
         },
         immediate = true
 )
 public class RefetchFlushContentBuilderImpl implements ContentBuilder {
     private static final Logger log = LoggerFactory.getLogger(RefetchFlushContentBuilderImpl.class);
 
     private ReplicationLog replicationLog;
 
     private static final String CONTENT_BUILDER_NAME = "flush_refetch";
     public static final String TITLE = "Dispatcher Flush Re-fetch";
     public static final String SERVICE_NAME = CONTENT_BUILDER_NAME;
     private static final String[] DEFAULT_MATCH_PATH = {"*"};
     private String[] pathMatches = DEFAULT_MATCH_PATH;
     private Map<String, String[]> extensionPairs = new LinkedHashMap<>();
 
     @ObjectClassDefinition(
         name = "ACS AEM Commons - Dispatcher Flush with Re-fetch",
         description = "Instead of deleting pages from the Dispatcher cache, update the last modified time (.stat) of the targeted file, and trigger an immediate request of the page."
     )
     @interface Config {
         @AttributeDefinition(
                 name = "Path Pattern",
                 description = "Specify a regex to match paths to be included in the re-fetch flush (i.e. * for all paths, /content/.* for all paths under /content, .*.html for all paths with html as its extension)",
                 cardinality = Integer.MAX_VALUE
         )
         String[] match_paths() default {"*"}; // DEFAULT_MATCH_PATH
 
         @AttributeDefinition(
             name = "Extension Pairs",
             description = "To activate paired pages with re-fetch, specify the original extension (i.e. html) and map it to any other extensions (i.e. header_include.html)",
             cardinality = Integer.MAX_VALUE
         )
         String[] extension_pairs() default {}; // DEFAULT_EXTENSION_PAIRS
     }
 
     @Activate
     protected void activate(Config config) {
         this.extensionPairs = this.formatExtensions(ParameterUtil.toMap(config.extension_pairs(), "=", false, null, false));
 
         logInfoMessage("Extension Pairs [" +  mapToString(this.extensionPairs) + "]");
 
         ArrayList<String> validMatches = new ArrayList<>();
 
         String[] matchProps = config.match_paths();
 
         if (matchProps.length > 0) {
             for (String match : matchProps) {
                 if (StringUtils.isNotEmpty(match)) {
                     validMatches.add(match);
                 }
             }
         }
 
         this.pathMatches = validMatches.toArray(new String[0]);
 
         logInfoMessage("Match Path Patterns [" +  String.join(",", this.pathMatches) + "]");
     }
 
 
/** Given as input a configuredExtension object, unfold it by individual extension. */
 private Map<String, String[]> formatExtensions(final Map<String, String> configuredExtensions){
        Map<String, String[]> extensionPairs = new LinkedHashMap<>();
    
        for (Map.Entry<String, String> entry : configuredExtensions.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
    
            if (StringUtils.isNotEmpty(key) && StringUtils.isNotEmpty(value)) {
                String[] values = value.split(",");
                if (values.length > 0) {
                    extensionPairs.put(key, values);
                }
            }
        }
    
        return extensionPairs;
    }
    
        @Override
        public String getName() {
            return CONTENT_BUILDER_NAME;
        }
    
        @Override
        public String getTitle() {
            return TITLE;
        }
    
        @Override
        public ReplicationAction[] build(ReplicationContent content, Session session) throws ReplicationException {
            if (content == null) {
                throw new ReplicationException("Content is null");
            }
    
            if (session == null) {
                throw new ReplicationException("Session is null");
            }
    
            if (content.getPath() == null) {
                throw new ReplicationException("Path is null");
            }
    
            if (content.getAction() == null) {
                throw new ReplicationException("Action is null");
            }
    
            if (content.getAction().getType() != ReplicationActionType.FLUSH) {
                throw new ReplicationException("Action type is not FLUSH");
            }
    
            if (content.getAction().getType() == ReplicationActionType.FLUSH) {
                if (content.getAction().getType() == ReplicationActionType.FLUSH) {
                    if (content.getAction().getType() == ReplicationActionType.FLUSH) {
                        if (content.getAction().getType() == ReplicationActionType.FLUSH) {
                            if (content.getAction().getType() == ReplicationActionType.FLUSH) {
                                if (content.get     
 }

 

}