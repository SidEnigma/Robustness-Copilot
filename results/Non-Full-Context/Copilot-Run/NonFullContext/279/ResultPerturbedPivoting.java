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
 package com.adobe.acs.commons.synth.children;
 
 import com.adobe.acs.commons.json.JsonObjectUtil;
 import com.google.gson.Gson;
 import com.google.gson.JsonElement;
 import com.google.gson.JsonObject;
 import org.apache.commons.collections.IteratorUtils;
 import org.apache.jackrabbit.JcrConstants;
 import org.apache.sling.api.resource.ModifiableValueMap;
 import org.apache.sling.api.resource.Resource;
 import org.apache.sling.api.resource.ResourceWrapper;
 import org.apache.sling.api.resource.ValueMap;
 import org.apache.sling.api.wrappers.ValueMapDecorator;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import javax.jcr.RepositoryException;
 import java.io.Serializable;
 import java.lang.reflect.InvocationTargetException;
 import java.time.Instant;
 import java.time.LocalDateTime;
 import java.time.ZoneId;
 import java.time.format.DateTimeFormatter;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.LinkedHashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
 import java.util.TreeSet;
 
 /**
  * Class to wrapper a real resource to facilitate the persistence of children resources in a property (serialized as
  * JSON).
  *
  * Can be used as follows...
  *
  * To write data:
  *
  * Resource real = resolve.getResource("/content/real");
  * ChildrenAsPropertyResource wrapper = new ChildrenAsPropertyResource(real);
  * Resource child = wrapper.create("child-1", "nt:unstructured");
  * ModifiableValueMap mvm = child.adaptTo(ModifiableValueMap.class);
  * mvm.put("prop-1", "some data");
  * mvm.put("prop-2", Calendar.getInstance());
  * wrapper.persist();
  * resolver.commit();
  *
  * To read data:
  *
  * Resource real = resolve.getResource("/content/real");
  * ChildrenAsPropertyResource wrapper = new ChildrenAsPropertyResource(real);
  * for(Resource child : wrapper.getChildren()) {
  *     child.getValueMap().get("prop-1", String.class);
  * }
  *
  */
 public class ChildrenAsPropertyResource extends ResourceWrapper {
     private static final Logger log = LoggerFactory.getLogger(ChildrenAsPropertyResource.class);
 
     private static final String EMPTY_JSON = "{}";
 
     private static final String DEFAULT_PROPERTY_NAME = "children";
 
     private final Resource resource;
 
     private final String propertyName;
 
     private Map<String, Resource> lookupCache = null;
 
     private Set<Resource> orderedCache = null;
 
     private Comparator<Resource> comparator = null;
 
     public static final Comparator<Resource> RESOURCE_NAME_COMPARATOR = new ResourceNameComparator();
 
     /**
      * ResourceWrapper that allows resource children to be modeled in data stored into a property using the default
      * property name of "children".
      *
      * @param resource     the resource to store the children as properties on
      * @throws InvalidDataFormatException
      */
     public ChildrenAsPropertyResource(Resource resource) throws InvalidDataFormatException {
         this(resource, DEFAULT_PROPERTY_NAME, null);
     }
 
     /**
      * ResourceWrapper that allows resource children to be modeled in data stored into a property.
      *
      * @param resource     the resource to store the children as properties on
      * @param propertyName the property name to store the children as properties in
      */
     public ChildrenAsPropertyResource(Resource resource, String propertyName) throws InvalidDataFormatException {
         this(resource, propertyName, null);
     }
 
     /**
      * ResourceWrapper that allows resource children to be modeled in data stored into a property.
      *
      * @param resource     the resource to store the children as properties on
      * @param propertyName the property name to store the children as properties in
      * @param comparator   the comparator used to order the serialized children
      * @throws InvalidDataFormatException
      */
     public ChildrenAsPropertyResource(Resource resource, String propertyName, Comparator<Resource> comparator)
             throws InvalidDataFormatException {
         super(resource);
 
         this.resource = resource;
         this.propertyName = propertyName;
         this.comparator = comparator;
 
         if (this.comparator == null) {
             this.orderedCache = new LinkedHashSet<Resource>();
         } else {
             this.orderedCache = new TreeSet<Resource>(this.comparator);
         }
 
         this.lookupCache = new HashMap<String, Resource>();
 
         for (SyntheticChildAsPropertyResource r : this.deserialize()) {
             this.orderedCache.add(r);
             this.lookupCache.put(r.getName(), r);
         }
     }
 
     /**
      * {@inheritDoc}
      **/
     @Override
     public final Iterator<Resource> listChildren() {
         return IteratorUtils.getIterator(this.orderedCache);
     }
 
     /**
      * {@inheritDoc}
      **/
     @Override
     public final Iterable<Resource> getChildren() {
         return Collections.unmodifiableSet(this.orderedCache);
     }
 
     /**
      * {@inheritDoc}
      **/
     @Override
     public final Resource getChild(String name) {
         return this.lookupCache.get(name);
     }
 
     /**
      * {@inheritDoc}
      **/
     @Override
     public final Resource getParent() {
         return this.resource;
     }
 
 
     public final Resource create(String name, String primaryType) throws RepositoryException {
         return create(name, primaryType, null);
     }
 
     public final Resource create(String name, String primaryType, Map<String, Object> data) throws RepositoryException {
         if (data == null) {
             data = new HashMap<String, Object>();
         }
 
         if (data.containsKey(JcrConstants.JCR_PRIMARYTYPE) && primaryType != null) {
             data.put(JcrConstants.JCR_PRIMARYTYPE, primaryType);
         }
 
         final SyntheticChildAsPropertyResource child =
                 new SyntheticChildAsPropertyResource(this.resource, name, data);
 
         if (this.lookupCache.containsKey(child.getName())) {
             log.info("Existing synthetic child [ {} ] overwritten", name);
         }
 
         this.lookupCache.put(child.getName(), child);
         this.orderedCache.add(child);
 
         return child;
     }
 
     /**
      * Deletes the named child.
      *
      * Requires subsequent call to persist().
      *
      * @param name the child node name to delete
      * @throws RepositoryException
      */
     public final void delete(String name) throws RepositoryException {
         if (this.lookupCache.containsKey(name)) {
             Resource tmp = this.lookupCache.get(name);
             this.orderedCache.remove(tmp);
             this.lookupCache.remove(name);
         }
     }
 
     /**
      * Delete all children.
      *
      * Requires subsequent call to persist().
      *
      * @throws InvalidDataFormatException
      */
     public final void deleteAll() throws InvalidDataFormatException {
         // Clear the caches; requires serialize
         if (this.comparator == null) {
             this.orderedCache = new LinkedHashSet<Resource>();
         } else {
             this.orderedCache = new TreeSet<Resource>(this.comparator);
         }
 
         this.lookupCache = new HashMap<String, Resource>();
     }
 
     /**
      * Persist changes to the underlying valuemap so they are available for persisting to the JCR.
      *
      * @throws RepositoryException
      */
     public final void persist() throws RepositoryException {
         this.serialize();
     }
 
 
     /**
      * Serializes all children data as JSON to the resource's propertyName.
      *
      * @throws InvalidDataFormatException
      */
     private void serialize() throws InvalidDataFormatException {
         final long start = System.currentTimeMillis();
 
         final ModifiableValueMap modifiableValueMap = this.resource.adaptTo(ModifiableValueMap.class);
         JsonObject childrenJSON = new JsonObject();
 
         try {
             // Add the new entries to the JSON
             for (Resource childResource : this.orderedCache) {
                 childrenJSON.add(childResource.getName(), this.serializeToJSON(childResource));
             }
 
             if (childrenJSON.entrySet().size() > 0) {
                 // Persist the JSON back to the Node
                 modifiableValueMap.put(this.propertyName, childrenJSON.toString());
             } else {
                 // Nothing to persist; delete the property
                 modifiableValueMap.remove(this.propertyName);
             }
 
             log.debug("Persist operation for [ {} ] in [ {} ms ]",
                     this.resource.getPath() + "/" + this.propertyName,
                     System.currentTimeMillis() - start);
 
         } catch (NoSuchMethodException e) {
             throw new InvalidDataFormatException(this.resource, this.propertyName, childrenJSON.toString());
         } catch (IllegalAccessException e) {
             throw new InvalidDataFormatException(this.resource, this.propertyName, childrenJSON.toString());
         } catch (InvocationTargetException e) {
             throw new InvalidDataFormatException(this.resource, this.propertyName, childrenJSON.toString());
         }
     }
 
 
/** Convert the serialized JSON data found in the node property to Resources. */
 private List<SyntheticChildAsPropertyResource> deserialize() throws InvalidDataFormatException{
        final long start = System.currentTimeMillis();
    
        final ValueMap valueMap = this.resource.adaptTo(ValueMap.class);
        final String json = valueMap.get(this.propertyName, String.class);
    
        if (json == null) {
            return Collections.emptyList();
        }
    
        final JsonObject jsonObject = new JsonObject(json);
    
        final List<SyntheticChildAsPropertyResource> children = new ArrayList<SyntheticChildAsPropertyResource>();
    
        for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
            final JsonObject childJSON = (JsonObject) entry.getValue();
            final SyntheticChildAsPropertyResource child =
                    new SyntheticChildAsPropertyResource(this.resource, entry.getKey(), childJSON);
            children.add(child);
        }
    
        log.debug("Deserialize operation for [ {} ] in [ {} ms ]",
                this.resource.getPath() + "/" + this.propertyName,
                System.currentTimeMillis() - start);
    
        return children;
    }
    
        /**
        * Serializes a Resource to JSON.
        *
        * @param resource the resource to serialize
        * @return the JSON representation of the resource
        * @throws NoSuchMethodException
        * @throws IllegalAccessException
        * @throws InvocationTargetException
        */
        private JsonObject serializeToJSON(Resource resource) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
            final JsonObject jsonObject = new JsonObject();
    
            for (String propertyName : resource.getPropertyNames()) {
                final Value[] values = resource.getProperty(propertyName);
    
                for (Value value : values) {
                    jsonObject.add(propertyName, value.getString());
                }
            }
    
            return jsonObject;
        }
    
        /**
        * {@inheritDoc}
        **/
        @Override
        public final String toString() {
            return      
 }

 

}