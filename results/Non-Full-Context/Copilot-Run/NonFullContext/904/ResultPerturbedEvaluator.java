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
 package com.adobe.acs.commons.ondeploy.scripts;
 
 import com.adobe.acs.commons.cqsearch.QueryUtil;
 import com.day.cq.search.Query;
 import com.day.cq.search.QueryBuilder;
 import org.osgi.annotation.versioning.ConsumerType;
 import com.day.cq.commons.jcr.JcrConstants;
 import com.day.cq.commons.jcr.JcrUtil;
 import com.day.cq.search.PredicateGroup;
 import com.day.cq.search.result.SearchResult;
 import com.day.cq.wcm.api.PageManager;
 import org.apache.sling.api.resource.ModifiableValueMap;
 import org.apache.sling.api.resource.Resource;
 import org.apache.sling.api.resource.ResourceResolver;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import javax.jcr.Node;
 import javax.jcr.PathNotFoundException;
 import javax.jcr.RepositoryException;
 import javax.jcr.Session;
 import javax.jcr.Workspace;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 
 /**
  * Base on-deploy script implementation.
  */
 @ConsumerType
 public abstract class OnDeployScriptBase implements OnDeployScript {
     private static final String SLING_RESOURCE_TYPE = "sling:resourceType";
 
     @SuppressWarnings("PMD.LoggerIsNotStaticFinal")
     protected final Logger logger = LoggerFactory.getLogger(this.getClass());
 
     private PageManager pageManager;
     private ResourceResolver resourceResolver;
     private Session session;
     private Workspace workspace;
 
     /**
      * @see OnDeployScript#execute(ResourceResolver)
      */
     public final void execute(ResourceResolver resourceResolver) {
         this.resourceResolver = resourceResolver;
 
         this.pageManager = resourceResolver.adaptTo(PageManager.class);
         this.session = resourceResolver.adaptTo(Session.class);
         if (session != null) {
             this.workspace = session.getWorkspace();
         }
         try {
             execute();
             resourceResolver.commit();
         } catch (Exception e) {
             throw new OnDeployScriptException(e);
         }
     }
 
     /**
      * Execute the script.
      * <p>
      * This function must be implemented by all scripts.
      */
     protected abstract void execute() throws Exception;
 
     /**
      * Retrieve a node, or create it if not present.
      * <p>
      * The node, as well as non-existent parent nodes, are created as type
      * nt:unstructured.
      *
      * @param absolutePath Path to fetch or create.
      * @return The fetched or created node.
      */
     protected final Node getOrCreateNode(String absolutePath) throws RepositoryException {
         return getOrCreateNode(absolutePath, JcrConstants.NT_UNSTRUCTURED, JcrConstants.NT_UNSTRUCTURED);
     }
 
     /**
      * Retrieve a node, or create it if not present.
      * <p>
      * If the node does not exist, it is created as the specified nodeType.
      * <p>
      * Non-existent parent nodes are created as type nt:unstructured.
      *
      * @param absolutePath Path to fetch or create.
      * @param nodeType     The type of node to create.
      * @return The fetched or created node.
      */
     protected final Node getOrCreateNode(String absolutePath, String nodeType) throws RepositoryException {
         return getOrCreateNode(absolutePath, JcrConstants.NT_UNSTRUCTURED, nodeType);
     }
 
     /**
      * Retrieve a node, or create it if not present.
      * <p>
      * If the node does not exist, it is created as the specified nodeType.
      * <p>
      * Non-existent parent nodes are created as the type specified by
      * intermediateNodeType.
      *
      * @param absolutePath         Path to fetch or create.
      * @param intermediateNodeType The type of intermediate nodes to create.
      * @param nodeType             The type of node to create.
      * @return The fetched or created node.
      */
     protected final Node getOrCreateNode(String absolutePath, String intermediateNodeType, String nodeType) throws RepositoryException {
         try {
             return session.getNode(absolutePath);
         } catch (PathNotFoundException e) {
             logger.info("Creating node (and missing parents): {}", absolutePath);
             return JcrUtil.createPath(absolutePath, intermediateNodeType, nodeType, session, false);
         }
     }
 
     /**
      * Rename a property on a node.
      *
      * @param node            Node to update the property name on.
      * @param oldPropertyName Old property name.
      * @param newPropertyName New property name.
      */
     protected final void renameProperty(Node node, String oldPropertyName, String newPropertyName) throws RepositoryException {
         Resource resource = resourceResolver.getResource(node.getPath());
         renameProperty(resource, oldPropertyName, newPropertyName);
     }
 
     /**
      * Rename a property on a resource.
      *
      * @param resource        Resource to update the property name on.
      * @param oldPropertyName Old property name.
      * @param newPropertyName New property name.
      */
     protected final void renameProperty(Resource resource, String oldPropertyName, String newPropertyName) {
         ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);
         if (properties.containsKey(oldPropertyName)) {
             logger.info("Renaming property '{}' to '{}' on resource: {}", oldPropertyName, newPropertyName, resource.getPath());
             properties.put(newPropertyName, properties.get(oldPropertyName));
             properties.remove(oldPropertyName);
         } else {
             logger.debug("Property '{}' does not exist on resource: {}", oldPropertyName, resource.getPath());
         }
     }
 
     /**
      * Remove node at a given path.
      *
      * @param path Path to the node to remove.
      */
     protected final void removeResource(String path) throws RepositoryException {
         Resource resource = resourceResolver.getResource(path);
         if (resource != null) {
             logger.info("Deleting node at {}", path);
             session.removeItem(path);
         } else {
             logger.info("Node at {} has already been removed", path);
         }
     }
 
 
/** /content is searched for the current sling:resourceType, and any nodes it finds are changed with the newResourceType. */
 protected final void searchAndUpdateResourceType(String oldResourceType, String newResourceType) throws RepositoryException{
        logger.info("Searching for {} and updating to {}", oldResourceType, newResourceType);
        String searchPath = "/content";
        Node searchNode = session.getRootNode().getNode(searchPath);
        NodeIterator nodeIterator = searchNode.getNodes();
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.nextNode();
            if (node.hasProperty(SLING_RESOURCE_TYPE)) {
                String resourceType = node.getProperty(SLING_RESOURCE_TYPE).getString();
                if (resourceType.equals(oldResourceType)) {
                    logger.info("Updating resource type on {} to {}", node.getPath(), newResourceType);
                    node.setProperty(SLING_RESOURCE_TYPE, newResourceType);
                }
            }
        }
    }
    
        /**
        * Remove a property from a node.
        *
        * @param node            Node to update the property name on.
        * @param propertyName    Property name to remove.
        */
        protected final void removeProperty(Node node, String propertyName) throws RepositoryException {
            Resource resource = resourceResolver.getResource(node.getPath());
            removeProperty(resource, propertyName);
        }
    
        /**
        * Remove a property from a resource.
        *
        * @param resource        Resource to update the property name on.
        * @param propertyName    Property name to remove.
        */
        protected final void removeProperty(Resource resource, String propertyName) {
            ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);
            if (properties.containsKey(propertyName)) {
                logger.info("Removing property '{}' from resource: {}", propertyName, resource.getPath());
                properties.remove(propertyName);
            } else {
                logger.debug("Property '{}' does not exist on resource: {}", propertyName, resource.getPath());
            }
        }
    
        /**
        * Set a property on a node.
        *
        * @param node            Node to update the property name on.       
 }

 

}