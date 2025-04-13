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
 
 
/** The convenience method for generating checks is DefaultChecksumGeneratorOptions. */
 public Map<String, String> generateChecksums(Session session, String path) throws RepositoryException, IOException{}

 

}