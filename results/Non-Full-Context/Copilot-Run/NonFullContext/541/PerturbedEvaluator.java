/*
  * #%L
  * ACS AEM Commons Bundle
  * %%
  * Copyright (C) 2015 Adobe
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
 
 package com.adobe.acs.commons.analysis.jcrchecksum.impl;
 
 import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGenerator;
 import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGeneratorOptions;
 import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.DefaultChecksumGeneratorOptions;
 import org.apache.commons.codec.digest.DigestUtils;
 import org.apache.commons.lang.StringUtils;
 import org.apache.felix.scr.annotations.Component;
 import org.apache.felix.scr.annotations.Service;
 import org.apache.jackrabbit.vault.util.Text;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import javax.jcr.Node;
 import javax.jcr.NodeIterator;
 import javax.jcr.Property;
 import javax.jcr.PropertyIterator;
 import javax.jcr.PropertyType;
 import javax.jcr.RepositoryException;
 import javax.jcr.Session;
 import javax.jcr.Value;
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.SortedMap;
 import java.util.TreeMap;
 
 /**
  * Utility that generates checksums for JCR paths.  The checksum is calculated using a depth first traversal
  * and calculates an aggregate checksum on the nodes with the specified node types
  * (via {@link ChecksumGeneratorOptions}).
  */
 @Component
 @Service
 @SuppressWarnings("squid:S2070") // SHA1 not used cryptographically
 public class ChecksumGeneratorImpl implements ChecksumGenerator {
     private static final Logger log = LoggerFactory.getLogger(ChecksumGeneratorImpl.class);
 
     /**
      * Convenience method for  generateChecksums(session, path, new DefaultChecksumGeneratorOptions()).
      *
      * @param session the session
      * @param path tthe root path to generate checksums for
      * @return the map of abs path ~&gt; checksums
      * @throws RepositoryException
      * @throws IOException
      */
     public Map<String, String> generateChecksums(Session session, String path) throws RepositoryException,
             IOException {
         return generateChecksums(session, path, new DefaultChecksumGeneratorOptions());
     }
 
     /**
      * Traverses the content tree whose root is defined by the path param, respecting the {@link
      * ChecksumGeneratorOptions}.
      * Generates map of checksum hashes in the format [ ABSOLUTE PATH ] : [ CHECKSUM OF NODE SYSTEM ]
      *
      * @param session the session
      * @param path the root path to generate checksums for
      * @param options the {@link ChecksumGeneratorOptions} that define the checksum generation
      * @return the map of abs path ~&gt; checksums
      * @throws RepositoryException
      * @throws IOException
      */
     public Map<String, String> generateChecksums(Session session, String path, ChecksumGeneratorOptions options)
             throws RepositoryException, IOException {
 
         Node node = session.getNode(path);
 
         if (node == null) {
             log.warn("Path [ {} ] not found while generating checksums", path);
             return new LinkedHashMap<>();
         }
 
         return traverseTree(node, options);
     }
 
     /**
      * Traverse the tree for candidate aggregate nodes.
      * @param node the current node being traversed
      * @param options the checksum generator options
      * @return a map of paths and checksums
      * @throws RepositoryException
      * @throws IOException
      */
     private Map<String, String> traverseTree(Node node, ChecksumGeneratorOptions options) throws
             RepositoryException,
             IOException {
 
         final Map<String, String> checksums = new LinkedHashMap<>();
 
         if (isExcludedSubTree(node, options)) {
             return checksums;
         } else if (isChecksumable(node, options) && !isExcludedNodeName(node, options)) {
             // Tree-traversal has found a node to checksum (checksum will include all valid sub-tree nodes)
             final String checksum = generatedNodeChecksum(node.getPath(), node, options);
             if (checksum != null) {
                 checksums.put(node.getPath(), checksum);
                 log.debug("Top Level Node: {} ~> {}", node.getPath(), checksum);
             }
         } else {
             // Traverse the tree for checksum-able node systems
             NodeIterator children = node.getNodes();
 
             while (children.hasNext()) {
                 // Check each child with recursive logic; if child is checksum-able the call into traverseTree will
                 // handle this case
                 checksums.putAll(traverseTree(children.nextNode(), options));
             }
         }
 
         return checksums;
     }
 
 
     /**
      * Ensures the node's primary type is included in the Included Node Types and NOT in the Excluded Node Types and NOT in the Excluded Node Names.
      *
      * @param node    the candidate node
      * @param options the checksum options containing the included and excluded none types
      * @return true if the node represents a checksum-able node system
      * @throws RepositoryException
      */
     private boolean isChecksumable(Node node, ChecksumGeneratorOptions options) throws RepositoryException {
         final Set<String> nodeTypeIncludes = options.getIncludedNodeTypes();
         final Set<String> nodeTypeExcludes = options.getExcludedNodeTypes();
 
         final String primaryNodeType = node.getPrimaryNodeType().getName();
 
         return nodeTypeIncludes.contains(primaryNodeType) && !nodeTypeExcludes.contains(primaryNodeType);
     }
 
     /**
      * Generates a checksum for a single node and its node sub-system, respecting the options.
      * @param aggregateNodePath the absolute path of the node being aggregated into a checksum
      * @param node the node whose subsystem to create a checksum for
      * @param options the {@link ChecksumGeneratorOptions} options
      * @return a map containing 1 entry in the form [ node.getPath() ] : [ CHECKSUM OF NODE SYSTEM ]
      * @throws RepositoryException
      * @throws IOException
      */
     @SuppressWarnings("squid:S3776")
     protected String generatedNodeChecksum(final String aggregateNodePath,
                                                   final Node node,
                                                   final ChecksumGeneratorOptions options)
             throws RepositoryException, IOException {
 
         if (isExcludedSubTree(node, options)) { return ""; }
 
         final Map<String, String> checksums = new LinkedHashMap<>();
 
         if (!isExcludedNodeName(node, options)) {
             /* Create checksums for Node's properties */
             final String checksum = generatePropertyChecksums(aggregateNodePath, node, options);
             if (checksum != null) {
                 checksums.put(getChecksumKey(aggregateNodePath, node.getPath()), checksum);
             }
         }
 
         /* Then process node's children */
 
         final Map<String, String> lexicographicallySortedChecksums = new TreeMap<>();
         final boolean hasOrderedChildren = hasOrderedChildren(node);
         final NodeIterator children = node.getNodes();
 
         while (children.hasNext()) {
             final Node child = children.nextNode();
 
             if (isExcludedSubTree(child, options)) {
                 // Skip this node!
             } else if (!isExcludedNodeType(child, options)) {
                 if (hasOrderedChildren) {
                     // Use the order dictated by the JCR
                     final String checksum = generatedNodeChecksum(aggregateNodePath, child, options);
                     if (checksum != null) {
                         checksums.put(getChecksumKey(aggregateNodePath, child.getPath()), checksum);
 
                         log.debug("Aggregated Ordered Node: {} ~> {}",
                                 getChecksumKey(aggregateNodePath, child.getPath()), checksum);
                     }
 
                 } else {
                     final String checksum = generatedNodeChecksum(aggregateNodePath, child, options);
                     if (checksum != null) {
                         // If order is not dictated by JCR, collect so we can sort later
                         lexicographicallySortedChecksums.put(getChecksumKey(aggregateNodePath, child.getPath()), checksum);
 
                         log.debug("Aggregated Unordered Node: {} ~> {}",
                                 getChecksumKey(aggregateNodePath, child.getPath()), checksum);
                     }
                 }
             }
         }
 
         if (!hasOrderedChildren && lexicographicallySortedChecksums.size() > 0) {
             // Order is not dictated by JCR, so add the lexicographically sorted entries to the checksums string
             checksums.putAll(lexicographicallySortedChecksums);
         }
 
         final String nodeChecksum = aggregateChecksums(checksums);
         log.debug("Node [ {} ] has a aggregated checksum of [ {} ]", getChecksumKey(aggregateNodePath, node.getPath()), nodeChecksum);
 
         return nodeChecksum;
     }
 
 
/** Generates and returns a sorted lexicographically map of (PROPERTY PATH, CHECKSUM OF PROPERTIES). */
 protected String generatePropertyChecksums(final String aggregateNodePath, final Node node, final ChecksumGeneratorOptions options) throws RepositoryException, IOException{}

 

}