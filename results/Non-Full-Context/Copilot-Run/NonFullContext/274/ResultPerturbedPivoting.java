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
 package com.adobe.acs.commons.httpcache.store.jcr.impl.writer;
 
 import static com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreConstants.OAK_UNSTRUCTURED;
 
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;
 import java.io.ObjectOutputStream;
 import java.time.Clock;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 
 import javax.jcr.Binary;
 import javax.jcr.Node;
 import javax.jcr.RepositoryException;
 import javax.jcr.Session;
 
 import org.apache.jackrabbit.commons.JcrUtils;
 
 import com.adobe.acs.commons.httpcache.engine.CacheContent;
 import com.adobe.acs.commons.httpcache.keys.CacheKey;
 import com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreConstants;
 import com.day.cq.commons.jcr.JcrConstants;
 
 public class EntryNodeWriter
 {
 
     private final Session session;
     private final Node entryNode;
     private final CacheKey cacheKey;
     private final CacheContent cacheContent;
     private final long expireTimeInMilliSeconds;
     private final Clock clock;
 
     public EntryNodeWriter(Session session, Node entryNode, CacheKey cacheKey, CacheContent cacheContent, long expireTimeInMilliSeconds, Clock clock){
         this.session = session;
         this.entryNode = entryNode;
         this.cacheKey = cacheKey;
         this.cacheContent = cacheContent;
         this.expireTimeInMilliSeconds = expireTimeInMilliSeconds;
         this.clock = clock;
     }
 
     /**
      * Populate the entry node with values
      * @throws RepositoryException
      */
     public void write() throws RepositoryException, IOException
     {
         entryNode.setProperty(JCRHttpCacheStoreConstants.PN_ISCACHEENTRYNODE, true);
 
         populateMetaData();
         populateHeaders();
         populateBinaryContent();
 
         //if we the expire time is set, set it on the node
         if(expireTimeInMilliSeconds > 0) {
             setExpireTime();
         }
 
         if(!entryNode.hasProperty(JCRHttpCacheStoreConstants.PN_CACHEKEY)) {
             populateCacheKey();
         }
     }
 
     private void setExpireTime() throws RepositoryException
     {
         entryNode.setProperty(JCRHttpCacheStoreConstants.PN_EXPIRES_ON,  expireTimeInMilliSeconds + clock.instant().toEpochMilli() );
     }
 
     private void populateMetaData() throws RepositoryException
     {
         entryNode.setProperty(JCRHttpCacheStoreConstants.PN_STATUS, cacheContent.getStatus());
         entryNode.setProperty(JCRHttpCacheStoreConstants.PN_CHAR_ENCODING, cacheContent.getCharEncoding());
         entryNode.setProperty(JCRHttpCacheStoreConstants.PN_CONTENT_TYPE, cacheContent.getContentType());
         entryNode.setProperty(JCRHttpCacheStoreConstants.PN_WRITEMETHOD, cacheContent.getWriteMethod().name());
     }
 
 
/** Save the input stream to a binary property under the cache input node. */
 private void populateBinaryContent() throws RepositoryException{
        Binary binary = session.getValueFactory().createBinary(new ByteArrayInputStream(cacheContent.getContent()));
        entryNode.setProperty(JCRHttpCacheStoreConstants.PN_BINARY_CONTENT, binary);
    }
    
        private void populateHeaders() throws RepositoryException
        {
            Map<String, List<String>> headers = cacheContent.getHeaders();
            if(headers != null) {
                for(Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    String key = entry.getKey();
                    List<String> values = entry.getValue();
                    if(values != null) {
                        for(String value : values) {
                            entryNode.setProperty(key, value);
                        }
                    }
                }
            }
        }
    
        private void populateCacheKey() throws RepositoryException
        {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(cacheKey);
            oos.close();
            Binary binary = session.getValueFactory().createBinary(new ByteArrayInputStream(bos.toByteArray()));
            entryNode.setProperty(JCRHttpCacheStoreConstants.PN_CACHEKEY, binary);
        }
    
        public void remove() throws RepositoryException
        {
            JcrUtils.remove(entryNode);
        }
    
        public void removeAll() throws RepositoryException
        {
            Iterator<Node> iterator = entryNode.getNodes(OAK_UNSTRUCTURED).iterator();
            while(iterator.hasNext()) {
                JcrUtils.remove(iterator.next());
            }
        }
    }
            
 }

 

}