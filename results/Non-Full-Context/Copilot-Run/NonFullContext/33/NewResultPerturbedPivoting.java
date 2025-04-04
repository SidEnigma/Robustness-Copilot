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
 
 
/** When resolving a remote resource, first synchronize the resource from the remote server. */
@Override
public Resource decorate(final Resource resource) {
    // Check if the resource is a remote resource
    if (isRemoteResource(resource)) {
        // Get the path of the remote resource
        String remotePath = getRemotePath(resource);

        // Synchronize the remote resource
        synchronizeRemoteResource(remotePath);

        // Get the synchronized resource
        Resource synchronizedResource = getSynchronizedResource(resource.getResourceResolver(), remotePath);

        // Return the synchronized resource
        return synchronizedResource;
    }

    // Return the original resource if it is not a remote resource
    return resource;
}
 

}