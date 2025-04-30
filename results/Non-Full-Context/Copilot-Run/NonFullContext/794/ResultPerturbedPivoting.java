/*
  * #%L
  * ACS AEM Commons Bundle
  * %%
  * Copyright (C) 2014 Adobe
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
 
 package com.adobe.acs.commons.oak.impl;
 
 import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGenerator;
 import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.CustomChecksumGeneratorOptions;
 import com.adobe.acs.commons.oak.impl.EnsureOakIndex.OakIndexDefinitionException;
 import com.day.cq.commons.jcr.JcrConstants;
 import com.day.cq.commons.jcr.JcrUtil;
 import org.apache.commons.lang.ArrayUtils;
 import org.apache.commons.lang.StringUtils;
 import org.apache.sling.api.resource.LoginException;
 import org.apache.sling.api.resource.ModifiableValueMap;
 import org.apache.sling.api.resource.PersistenceException;
 import org.apache.sling.api.resource.Resource;
 import org.apache.sling.api.resource.ResourceResolver;
 import org.apache.sling.api.resource.ResourceResolverFactory;
 import org.apache.sling.api.resource.ValueMap;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import javax.annotation.Nonnull;
 import javax.annotation.Nullable;
 import javax.jcr.Node;
 import javax.jcr.Property;
 import javax.jcr.PropertyType;
 import javax.jcr.RepositoryException;
 import javax.jcr.Session;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Calendar;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 public class EnsureOakIndexJobHandler implements Runnable {
     //@formatter:off
     static final Logger log = LoggerFactory.getLogger(EnsureOakIndexJobHandler.class);
     static final String PN_FORCE_REINDEX = "forceReindex";
 
     static final String PN_DELETE = "delete";
 
     static final String PN_IGNORE = "ignore";
 
     static final String PN_DISABLE = "disable";
 
     static final String NT_OAK_QUERY_INDEX_DEFINITION = "oak:QueryIndexDefinition";
 
     static final String NT_OAK_UNSTRUCTURED = "oak:Unstructured";
 
     static final String PN_TYPE = "type";
 
     static final String DISABLED = "disabled";
 
     static final String PN_RECREATE_ON_UPDATE = "recreateOnUpdate";
 
     static final String PN_REINDEX_COUNT = "reindexCount";
 
     static final String PN_SEED = "seed";
 
     static final String PN_REINDEX = "reindex";
 
     static final String NN_FACETS = "facets";
 
     static final String ENSURE_OAK_INDEX_USER_NAME = "Ensure Oak Index";
 
     static final String[] MANDATORY_IGNORE_PROPERTIES = {
             // JCR Properties
             JcrConstants.JCR_PRIMARYTYPE,
             JcrConstants.JCR_LASTMODIFIED,
             JcrConstants.JCR_LAST_MODIFIED_BY,
             JcrConstants.JCR_MIXINTYPES,
             JcrConstants.JCR_CREATED,
             JcrConstants.JCR_CREATED_BY,
             // Ensure Oak Index Properties
             PN_RECREATE_ON_UPDATE,
             PN_FORCE_REINDEX,
             PN_DELETE,
             PN_IGNORE,
             PN_DISABLE,
             // Oak Index Properties
             PN_REINDEX,
             PN_REINDEX_COUNT,
             PN_SEED
     };
 
     static final String[] MANDATORY_EXCLUDE_SUB_TREES = {
             // For the real index definition node
             "[" + NT_OAK_QUERY_INDEX_DEFINITION + "]/" + NN_FACETS + "/" + JcrConstants.JCR_CONTENT,
             // For the ensure oak index definition node
             "[" + NT_OAK_UNSTRUCTURED + "]/" + NN_FACETS + "/" + JcrConstants.JCR_CONTENT
     };
 
     static final String[] MANDATORY_EXCLUDE_NODE_NAMES = new String[]{ };
 
     private static final String[] NAME_PROPERTIES = new String[] {"propertyNames", "declaringNodeTypes"} ;
 
     static final String SERVICE_NAME = "ensure-oak-index";
 
     private final EnsureOakIndex ensureOakIndex;
 
     private final List<String> ignoreProperties = new ArrayList<>();
     private final List<String> excludeSubTrees = new ArrayList<>();
     private final List<String> excludeNodeNames = new ArrayList<>();
 
     private String oakIndexesPath;
 
     private String ensureDefinitionsPath;
     //@formatter:on
 
     EnsureOakIndexJobHandler(EnsureOakIndex ensureOakIndex, String oakIndexPath, String ensureDefinitionsPath) {
         this.ensureOakIndex = ensureOakIndex;
         this.oakIndexesPath = oakIndexPath;
         this.ensureDefinitionsPath = ensureDefinitionsPath;
 
         this.ignoreProperties.addAll(Arrays.asList(MANDATORY_IGNORE_PROPERTIES));
         this.excludeSubTrees.addAll(Arrays.asList(MANDATORY_EXCLUDE_SUB_TREES));
         this.excludeNodeNames.addAll(Arrays.asList(MANDATORY_EXCLUDE_NODE_NAMES));
 
         if (ensureOakIndex != null) {
             this.ignoreProperties.addAll(ensureOakIndex.getIgnoreProperties());
         }
     }
 
     @Override
     @SuppressWarnings("squid:S1141")
     public void run() {
         Map<String, Object> authInfo = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
         try (ResourceResolver resourceResolver = this.ensureOakIndex.getResourceResolverFactory().getServiceResourceResolver(authInfo)) {
 
             // we should rethink this nested try here ...
             try {
                 this.ensure(resourceResolver, ensureDefinitionsPath, oakIndexesPath);
             } catch (IOException e) {
                 log.error("Could not ensure management of Oak Index [ {} ]", oakIndexesPath, e);
             }
         } catch (IllegalArgumentException e) {
             log.error("Could not ensure oak indexes due to illegal arguments.",e);
         } catch (LoginException e) {
             log.error("Could not get an admin resource resolver to ensure Oak Indexes", e);
         } catch (Exception e) {
             log.error("Unknown error occurred while ensuring indexes", e);
         }
     }
 
     /**
      * Main work method. Responsible for ensuring the ensure definitions under srcPath are reflected in the real oak
      * index under oakIndexesPath.
      * <p/>
      * The handling is split, so that all re-indexings can be combined into a single commit; this
      * ensures, that a single repository traversal can be used to reindex all affected indexes.
      *
      * @param resourceResolver      the resource resolver (must have permissions to read definitions and change indexes)
      * @param ensureDefinitionsPath the path containing the ensure definitions
      * @param oakIndexesPath        the path of the real oak index
      * @throws RepositoryException
      * @throws IOException
      */
     private void ensure(final ResourceResolver resourceResolver, final String ensureDefinitionsPath,
                         final String oakIndexesPath)
             throws RepositoryException, IOException {
 
         final Resource ensureDefinitions = resourceResolver.getResource(ensureDefinitionsPath);
         final Resource oakIndexes = resourceResolver.getResource(oakIndexesPath);
 
         if (ensureDefinitions == null) {
             throw new IllegalArgumentException("Unable to find Ensure Definitions resource at [ "
                     + ensureDefinitionsPath + " ]");
         } else if (oakIndexes == null) {
             throw new IllegalArgumentException("Unable to find Oak Indexes resource at [ "
                     + oakIndexesPath + " ]");
         }
 
         final Iterator<Resource> ensureDefinitionsIterator = ensureDefinitions.listChildren();
         if (!ensureDefinitionsIterator.hasNext()) {
             log.info("Ensure Definitions path [ {} ] does NOT have children to process", ensureDefinitions.getPath());
         }
 
         final List<Resource> delayedProcessing = new ArrayList<>();
 
         // First, handle all things that may not result in a a collective re-indexing
         // Includes: IGNORES, DELETES, DISABLED ensure definitions
 
         while (ensureDefinitionsIterator.hasNext()) {
             final Resource ensureDefinition = ensureDefinitionsIterator.next();
             final Resource oakIndex = oakIndexes.getChild(ensureDefinition.getName());
 
             log.debug("Ensuring Oak Index [ {} ] ~> [ {}/{} ]",
                     ensureDefinition.getPath(), oakIndexesPath, ensureDefinition.getName());
 
             if (!handleLightWeightIndexOperations(
                     ensureDefinition, oakIndex)) {
                 delayedProcessing.add(ensureDefinition);
             }
         }
 
         if (resourceResolver.hasChanges()) {
             log.info("Saving all DELETES, IGNORES, and DISABLES to [ {} ]", oakIndexesPath);
             resourceResolver.commit();
             log.debug("Commit succeeded");
         }
 
         // Combine the index updates which will potentially result in a repository traversal into a single commit.
         // second iteration: handle CREATE, UPDATE and REINDEXING
         Iterator<Resource> delayedProcessingEnsureDefinitions = delayedProcessing.iterator();
 
         while (delayedProcessingEnsureDefinitions.hasNext()) {
             final Resource ensureDefinition = delayedProcessingEnsureDefinitions.next();
             final Resource oakIndex = oakIndexes.getChild(ensureDefinition.getName());
 
             handleHeavyWeightIndexOperations(oakIndexes, ensureDefinition,
                     oakIndex);
         }
 
         if (resourceResolver.hasChanges()) {
             log.info("Saving all CREATE, UPDATES, and RE-INDEXES, re-indexing may start now.");
             resourceResolver.commit();
             log.debug("Commit succeeded");
         }
     }
 
     /**
      * Handle CREATE and UPDATE operations.
      *
      * @param oakIndexes
      * @param ensureDefinition
      * @param oakIndex
      * @throws RepositoryException
      * @throws PersistenceException
      * @throws IOException
      */
     void handleHeavyWeightIndexOperations(final Resource oakIndexes,
                                           final @Nonnull Resource ensureDefinition, final @Nullable Resource oakIndex)
             throws RepositoryException, IOException {
         final ValueMap ensureDefinitionProperties = ensureDefinition.getValueMap();
 
         try {
             Resource ensuredOakIndex = null;
             validateEnsureDefinition(ensureDefinition);
             if (oakIndex == null) {
                 // CREATE
                 ensuredOakIndex = this.create(ensureDefinition, oakIndexes);
 
                 // Force re-index
                 if (ensureDefinitionProperties.get(PN_FORCE_REINDEX, false)) {
                     this.forceRefresh(ensuredOakIndex);
                 }
             } else {
                 // UPDATE
                 boolean forceReindex = ensureDefinitionProperties.get(PN_FORCE_REINDEX, false);
 
                 if (ensureDefinitionProperties.get(PN_RECREATE_ON_UPDATE, false)) {
                     // Recreate on Update, refresh not required (is implicit)
                     this.delete(oakIndex);
                     this.create(ensureDefinition, oakIndexes);
                 } else {
                     // Normal Update
                     this.update(ensureDefinition, oakIndexes, forceReindex);
                 }
             }
         } catch (OakIndexDefinitionException e) {
             log.error("Skipping processing of {}", ensureDefinition.getPath(), e);
         }
     }
 
     /**
      * handle the operations IGNORE, DELETE and DISABLE
      *
      * @param ensureDefinition
      * @param oakIndex
      * @return true if the definition has been handled; if true the definition needs further processing
      * @throws RepositoryException
      * @throws PersistenceException
      */
     boolean handleLightWeightIndexOperations(
             final @Nonnull Resource ensureDefinition, final @Nullable Resource oakIndex)
             throws RepositoryException, PersistenceException {
 
         final ValueMap ensureDefinitionProperties = ensureDefinition.getValueMap();
         boolean result = true;
 
 
         if (ensureDefinitionProperties.get(PN_IGNORE, false)) {
             // IGNORE
             log.debug("Ignoring index definition at [ {} ]", ensureDefinition.getPath());
         } else if (ensureDefinitionProperties.get(PN_DELETE, false)) {
             // DELETE
             if (oakIndex != null) {
                 this.delete(oakIndex);
             } else {
                 // Oak index does not exist
                 log.info("Requesting deletion of a non-existent Oak Index at [ {}/{} ].\nConsider removing the Ensure Definition at [ {} ] if it is no longer needed.",
                         oakIndexesPath, ensureDefinition.getName(),
                         ensureDefinition.getPath());
             }
         } else if (ensureDefinitionProperties.get(PN_DISABLE, false)) {
             // DISABLE index
             if (oakIndex != null) {
                 this.disableIndex(oakIndex);
             } else {
                 // Oak index does not exist
                 log.info("Requesting disable of a non-existent Oak Index at [ {}/{} ].\nConsider removing the Ensure Definition at [ {} ] if it is no longer needed.",
                         oakIndexesPath, ensureDefinition.getName(), ensureDefinition.getPath());
             }
         } else {
             // handle updates, creates and all reindexing stuff in the second round
             result = false;
         }
         return result;
     }
 
     /**
      * Forces index refresh for create or updates (that require updating).
      *
      * @param oakIndex the index representing the oak index
      * @throws PersistenceException
      */
     public void forceRefresh(final @Nonnull Resource oakIndex) throws PersistenceException {
 
         final ModifiableValueMap mvm = oakIndex.adaptTo(ModifiableValueMap.class);
         if (mvm == null ) {
             String msg = String.format("Cannot adapt %s to a ModifiableValueMap (permissions?)", oakIndex.getPath());
             throw new PersistenceException(msg);
         }
         mvm.put(PN_REINDEX, true);
 
         log.info("Forcing re-index of [ {} ]", oakIndex.getPath());
     }
 
 
/** Create the oak index based on the warranty definition. */
 public Resource create(@Nonnull final Resource ensuredDefinition, @Nonnull final Resource oakIndexes) throws RepositoryException{
        final ValueMap ensuredDefinitionProperties = ensuredDefinition.getValueMap();
        final String ensuredDefinitionName = ensuredDefinition.getName();
        final String ensuredDefinitionPath = ensuredDefinition.getPath();
        final String oakIndexesPath = oakIndexes.getPath();
    
        final String oakIndexName = ensuredDefinitionProperties.get(PN_NAME, ensuredDefinitionName);
        final String oakIndexPath = oakIndexesPath + "/" + oakIndexName;
    
        final Resource oakIndex = resourceResolver.getResource(oakIndexPath);
        if (oakIndex != null) {
            log.info("Skipping creation of existing Oak Index at [ {} ]", oakIndexPath);
            return oakIndex;
        }
    
        final String oakIndexType = ensuredDefinitionProperties.get(PN_TYPE, DEFAULT_TYPE);
        final String oakIndexPaths = ensuredDefinitionProperties.get(PN_PATHS, "");
        final String oakIndexQuery = ensuredDefinitionProperties.get(PN_QUERY, "");
        final String oakIndexQueryLanguage = ensuredDefinitionProperties.get(PN_QUERY_LANGUAGE, DEFAULT_QUERY_LANGUAGE);
        final String oakIndexQueryType = ensuredDefinitionProperties.get(PN_QUERY_TYPE, DEFAULT_QUERY_TYPE);
        final String oakIndexQueryAnalyzer = ensuredDefinitionProperties.get(PN_QUERY_ANALYZER, DEFAULT_QUERY_ANALYZER);
        final String oakIndexQueryAnalyzerType = ensuredDefinitionProperties.get(PN_QUERY_ANALYZER_TYPE, DEFAULT_QUERY_ANALYZER_TYPE);
        final String oakIndexQueryAnalyzerVersion = ensuredDefinitionProperties.get(PN_QUERY_ANALYZER_VERSION, DEFAULT_QUERY_ANALYZER_VERSION);
        final String oakIndexQueryAnalyzerLanguage = ensuredDefinitionProperties.get(PN_QUERY_ANALYZER_LANGUAGE, DEFAULT_QUERY_ANALYZER_LANGUAGE);
        final String oakIndexQueryAnalyzerLanguageVersion       
 }

 

}