/*
  * #%L
  * ACS AEM Commons Bundle
  * %%
  * Copyright (C) 2016 Adobe
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
 
 package com.adobe.acs.commons.util.impl;
 
 import com.adobe.acs.commons.cqsearch.QueryUtil;
 import com.adobe.acs.commons.util.ParameterUtil;
 import com.adobe.acs.commons.util.QueryHelper;
 import com.day.cq.search.PredicateGroup;
 import com.day.cq.search.QueryBuilder;
 import com.day.cq.search.result.Hit;
 import org.apache.commons.lang.StringUtils;
 import org.apache.felix.scr.annotations.Component;
 import org.apache.felix.scr.annotations.Reference;
 import org.apache.felix.scr.annotations.Service;
 import org.apache.sling.api.resource.Resource;
 import org.apache.sling.api.resource.ResourceResolver;
 
 import javax.jcr.NodeIterator;
 import javax.jcr.RepositoryException;
 import javax.jcr.Session;
 import javax.jcr.query.Query;
 import javax.jcr.query.QueryManager;
 import javax.jcr.query.QueryResult;
 import javax.jcr.query.Row;
 import javax.jcr.query.RowIterator;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
 import java.util.Map;
 
 @Component
 @Service
 public class QueryHelperImpl implements QueryHelper {
 
     @Reference
     private QueryBuilder queryBuilder;
 
     public static final String QUERY_BUILDER = "queryBuilder";
 
     public static final String LIST = "list";
 
 
/** Collect all the resources for the definition of the package. */
 public List<Resource> findResources(final ResourceResolver resourceResolver, final String language, final String statement, final String relPath) throws RepositoryException{
      final List<Resource> resources = new ArrayList<>();
      final Session session = resourceResolver.adaptTo(Session.class);
      final QueryManager queryManager = session.getWorkspace().getQueryManager();
      final Query query = queryManager.createQuery(statement, Query.JCR_SQL2);
      final QueryResult queryResult = query.execute();
      final RowIterator rowIterator = queryResult.getRows();
      while (rowIterator.hasNext()) {
          final Row row = rowIterator.nextRow();
          final String path = row.getValue("path").getString();
          final Resource resource = resourceResolver.getResource(path);
          if (resource != null) {
              resources.add(resource);
          }
      }
      return resources; 
 }

 

}