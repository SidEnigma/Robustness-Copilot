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
 package com.adobe.acs.commons.remoteassets.impl;
 
 import com.adobe.acs.commons.assets.FileExtensionMimeTypeConstants;
 import com.adobe.acs.commons.remoteassets.RemoteAssetsNodeSync;
 import com.day.cq.commons.jcr.JcrUtil;
 import com.day.cq.dam.api.Asset;
 import com.day.cq.dam.api.DamConstants;
 import com.day.cq.dam.commons.util.DamUtil;
 import com.day.cq.tagging.Tag;
 import com.day.cq.tagging.TagManager;
 import com.day.cq.wcm.api.NameConstants;
 import com.google.gson.JsonArray;
 import com.google.gson.JsonElement;
 import com.google.gson.JsonObject;
 import com.google.gson.JsonParser;
 import com.google.gson.JsonPrimitive;
 import com.google.gson.JsonSyntaxException;
 import org.apache.commons.io.FilenameUtils;
 import org.apache.commons.lang3.StringUtils;
 import org.apache.http.client.fluent.Executor;
 import org.apache.http.client.fluent.Request;
 import org.apache.jackrabbit.vault.util.JcrConstants;
 import org.apache.sling.api.resource.ModifiableValueMap;
 import org.apache.sling.api.resource.Resource;
 import org.apache.sling.api.resource.ResourceResolver;
 import org.apache.sling.api.resource.ValueMap;
 import org.osgi.service.component.annotations.Component;
 import org.osgi.service.component.annotations.Reference;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import javax.jcr.Node;
 import javax.jcr.RepositoryException;
 import javax.jcr.Session;
 import javax.jcr.ValueFactory;
 import java.io.IOException;
 import java.io.InputStream;
 import java.math.BigDecimal;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.time.ZonedDateTime;
 import java.time.format.DateTimeFormatter;
 import java.time.format.DateTimeParseException;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Calendar;
 import java.util.GregorianCalendar;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.Set;
 import java.util.regex.Pattern;
 
 /**
  * Remote Assets service to sync a node tree from a remote server.
  */
 @Component(
         immediate = true,
         service = RemoteAssetsNodeSync.class
 )
 public class RemoteAssetsNodeSyncImpl implements RemoteAssetsNodeSync {
 
   private static final String REP_POLICY = "rep:policy";
 
     private static final Logger LOG = LoggerFactory.getLogger(RemoteAssetsNodeSyncImpl.class);
     private static final Pattern DATE_REGEX = Pattern
             .compile("[A-Za-z]{3}\\s[A-Za-z]{3}\\s\\d\\d\\s\\d\\d\\d\\d\\s\\d\\d:\\d\\d:\\d\\d\\sGMT[-+]\\d\\d\\d\\d");
     private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US);
     private static final Pattern DECIMAL_REGEX = Pattern.compile("-?\\d+\\.\\d+");
     private static final String ASSET_FILE_PREFIX = "remoteassets/remote_asset";
     private static final Set<String> PROTECTED_PROPERTIES = new HashSet<>(Arrays.asList(
             JcrConstants.JCR_CREATED, JcrConstants.JCR_CREATED_BY, JcrConstants.JCR_VERSIONHISTORY, JcrConstants.JCR_BASEVERSION,
             JcrConstants.JCR_ISCHECKEDOUT, JcrConstants.JCR_UUID, JcrConstants.JCR_PREDECESSORS
     ));
     private static final Set<String> PROTECTED_NODES = new HashSet<>(Arrays.asList(
             DamConstants.THUMBNAIL_NODE, REP_POLICY
     ));
 
     @Reference
     private RemoteAssetsConfigImpl remoteAssetsConfig;
 
     private int saveRefreshCount = 0;
 
     /**
      * @see RemoteAssetsNodeSync#syncAssetNodes()
      */
     @Override
     public void syncAssetNodes() {
         
         try (ResourceResolver remoteAssetsResolver = this.remoteAssetsConfig.getResourceResolver();) {
             List<String> syncPaths = new ArrayList<>();
             syncPaths.addAll(this.remoteAssetsConfig.getTagSyncPaths());
             syncPaths.addAll(this.remoteAssetsConfig.getDamSyncPaths());
             for (String syncPath : syncPaths) {
                 LOG.info("Starting sync of nodes for {}", syncPath);
                 remoteAssetsResolver.refresh();
                 JsonObject topLevelJsonWithChildren = getJsonFromUri(syncPath);
                 String resourcePrimaryType = topLevelJsonWithChildren.getAsJsonPrimitive(JcrConstants.JCR_PRIMARYTYPE).getAsString();
                 Resource topLevelSyncResource = getOrCreateNode(remoteAssetsResolver, syncPath, resourcePrimaryType);
                 createOrUpdateNodes(remoteAssetsResolver, topLevelJsonWithChildren, topLevelSyncResource);
                 remoteAssetsResolver.commit();
                 LOG.info("Completed sync of nodes for {}", syncPath);
             }
         } catch (Exception e) {
             LOG.error("Unexpected error sync'ing remote asset nodes", e);
         } 
     }
 
     /**
      * Retrieve or create a node in the JCR.
      *
      * @param nextPath String
      * @param primaryType String
      * @return Resource
      * @throws RepositoryException exception
      */
     private Resource getOrCreateNode(final ResourceResolver remoteAssetsResolver, final String nextPath, final String primaryType) throws RepositoryException {
         Resource resource;
 
         try {
             resource = remoteAssetsResolver.getResource(nextPath);
             if (resource == null) {
                 Node node = JcrUtil.createPath(nextPath, primaryType, remoteAssetsResolver.adaptTo(Session.class));
                 resource = remoteAssetsResolver.getResource(node.getPath());
                 LOG.debug("New resource '{}' created.", resource.getPath());
             } else {
                 LOG.debug("Resource '{}' retrieved from JCR.", resource.getPath());
             }
         } catch (RepositoryException re) {
             LOG.error("Repository Exception. Unable to get or create resource '{}'", nextPath, re);
             throw re;
         }
 
         return resource;
     }
 
     /**
      * Get {@link JsonObject} from URL response.
      *
      * @param path String
      * @return JsonObject
      * @throws IOException exception
      */
     private JsonObject getJsonFromUri(final String path) throws IOException {
         URI pathUri;
         try {
             pathUri = new URI(null, null, path, null);
         } catch (URISyntaxException e) {
             LOG.error("URI Syntax Exception", e);
             throw new IOException("Invalid URI", e);
         }
 
         // we want to traverse the JCR one level at a time, hence the '1' selector.
         String url = this.remoteAssetsConfig.getServer() + pathUri.toString() + ".1.json";
         Executor executor = this.remoteAssetsConfig.getRemoteAssetsHttpExecutor();
         String responseString = executor.execute(Request.Get(url)).returnContent().asString();
 
         try {
             JsonObject responseJson = new JsonParser().parse(responseString).getAsJsonObject();
             LOG.debug("JSON successfully fetched for URL '{}'.", url);
             return responseJson;
         } catch (JsonSyntaxException | IllegalStateException e) {
             LOG.error("Unable to grab JSON Object. Please ensure URL {} is valid. \nRaw Response: {}", url, responseString);
             throw new IOException("Invalid JSON response", e);
         }
     }
 
     /**
      * Create or update resources from remote JSON.
      *
      * @param json JsonObject
      * @param resource Resource
      * @throws IOException exception
      * @throws RepositoryException exception
      */
     private void createOrUpdateNodes(final ResourceResolver remoteAssetsResolver, final JsonObject json, final Resource resource) throws IOException, RepositoryException {
         for (Map.Entry<String, JsonElement> jsonEntry : json.entrySet()) {
             JsonElement jsonElement = jsonEntry.getValue();
             if (jsonElement.isJsonObject()) {
                 createOrUpdateNodesForJsonObject(remoteAssetsResolver, jsonEntry.getKey(), resource);
             } else if (jsonElement.isJsonArray()) {
                 setNodeArrayProperty(remoteAssetsResolver, jsonEntry.getKey(), jsonElement.getAsJsonArray(), resource);
             } else {
                 setNodeProperty(remoteAssetsResolver, jsonEntry.getKey(), json, resource);
             }
         }
     }
 
 
/** Handler for when a JSON element is an Object, representing a resource. */

private void createOrUpdateNodesForJsonObject(final ResourceResolver remoteAssetsResolver, final String key, final Resource parentResource) throws IOException, RepositoryException {
    JsonObject jsonObject = json.getAsJsonObject(key);
    String resourcePrimaryType = jsonObject.getAsJsonPrimitive(JcrConstants.JCR_PRIMARYTYPE).getAsString();
    String resourcePath = parentResource.getPath() + "/" + key;
    Resource resource = getOrCreateNode(remoteAssetsResolver, resourcePath, resourcePrimaryType);
    createOrUpdateNodes(remoteAssetsResolver, jsonObject, resource);
}
 

}