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
 
 import com.adobe.acs.commons.remoteassets.RemoteAssetsBinarySync;
 import com.day.cq.dam.api.DamConstants;
 import org.apache.jackrabbit.JcrConstants;
 import org.apache.jackrabbit.api.security.user.User;
 import org.apache.jackrabbit.api.security.user.UserManager;
 import org.apache.sling.api.resource.Resource;
 import org.apache.sling.api.resource.ResourceDecorator;
 import org.apache.sling.api.resource.ResourceResolver;
 import org.apache.sling.api.resource.ValueMap;
 import org.apache.sling.jcr.base.util.AccessControlUtil;
 import org.osgi.service.component.annotations.Component;
 import org.osgi.service.component.annotations.ConfigurationPolicy;
 import org.osgi.service.component.annotations.Reference;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import javax.jcr.RepositoryException;
 import javax.jcr.Session;
 import javax.servlet.http.HttpServletRequest;
 import java.util.Calendar;
 import java.util.Set;
 import java.util.concurrent.ConcurrentSkipListSet;
 
 /**
  * ResourceDecorator that instruments remote assets to sync binaries as needed.
  *
  * This "decorator" is used to detect the first time a "remote" asset is
  * referenced by the system and sync that asset from the remote server to
  * make it now a "true" asset.
  */
 @Component(
         configurationPolicy = ConfigurationPolicy.REQUIRE,
         service = ResourceDecorator.class
 )
 public class RemoteAssetDecorator implements ResourceDecorator {
 
     private static final Logger LOG = LoggerFactory.getLogger(RemoteAssetDecorator.class);
     private static int SYNC_WAIT_SECONDS = 100;
 
     private static String ADMIN_ID = "admin";
 
     /**
      * This set stores resource paths for remote assets that are in the process
      * of being sync'd from the remote server.  This prevents an infinite loop
      * when the RemoteAssetSync service fetches the asset in order to update it.
      */
     private static Set<String> remoteResourcesSyncing = new ConcurrentSkipListSet<>();
 
     @Reference
     private RemoteAssetsBinarySync assetSync;
 
     @Reference
     private RemoteAssetsConfigImpl config;
 
     /**
      * When resolving a remote asset, first sync the asset from the remote server.
      * @param resource The resource being resolved.
      * @return The current resource.  If the resource is a "remote" asset, it will
      * first be converted to a true local AEM asset by sync'ing in the rendition
      * binaries from the remote server.
      */
     @Override
     public Resource decorate(final Resource resource) {
         try {
             if (!this.accepts(resource)) {
                 return resource;
             }
         } catch (Exception e) {
             // Logging at debug level b/c if this happens it could represent a ton of logging
             LOG.debug("Failed binary sync check for remote asset: {}", resource.getPath());
             return resource;
         }
 
         boolean syncSuccessful = false;
         if (isAlreadySyncing(resource.getPath())) {
             syncSuccessful = waitForSyncInProgress(resource);
         } else {
             syncSuccessful = syncAssetBinaries(resource);
         }
         if (syncSuccessful) {
             LOG.trace("Refreshing resource after binary sync of {}", resource.getPath());
             resource.getResourceResolver().refresh();
             return resource.getResourceResolver().getResource(resource.getPath());
         } else {
             return resource;
         }
     }
 
     /**
      * @deprecated
      * When resolving a remote asset, first sync the asset from the remote server.
      * @param resource The resource being resolved.
      * @param request HttpServletRequest
      * @return The current resource.  If the resource is a "remote" asset, it will
      * first be converted to a true local AEM asset by sync'ing in the rendition
      * binaries from the remote server.
      */
     @Deprecated
     @Override
     public Resource decorate(final Resource resource, final HttpServletRequest request) {
         return this.decorate(resource);
     }
 
     /**
      * Check if this resource is a remote resource.
      * @param resource Resource to check
      * @return true if resource is remote, else false
      */
     protected boolean accepts(final Resource resource) throws RepositoryException {ValueMap props = resource.getValueMap();
         if (!DamConstants.NT_DAM_ASSETCONTENT.equals(props.get(JcrConstants.JCR_PRIMARYTYPE))) {
             return false;
         }
 
         if (!props.get(RemoteAssets.IS_REMOTE_ASSET, false)) {
             return false;
         }
 
         Calendar lastFailure = props.get(RemoteAssets.REMOTE_SYNC_FAILED, (Calendar) null);
         if (lastFailure != null && System.currentTimeMillis() < (lastFailure.getTimeInMillis() + (this.config.getRetryDelay() * 60000))) {
             return false;
         }
 
         boolean matchesSyncPath = false;
         for (String syncPath : this.config.getDamSyncPaths()) {
             if (resource.getPath().startsWith(syncPath)) {
                 matchesSyncPath = true;
             }
         }
 
         return matchesSyncPath && isAllowedUser(resource);
     }
 
 
/** This method makes it possible to check whether the user is authorised to synchronise binaries. Both service users and the administrator user are prevented from synchronising binaries. The configuration of remote assets allows users to be added to a list. */
 private boolean isAllowedUser(Resource resource) throws RepositoryException{}

 

}