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
 package com.adobe.acs.commons.exporters.impl.users;
 
 import static com.adobe.acs.commons.exporters.impl.users.Constants.CUSTOM_PROPERTIES;
 import static com.adobe.acs.commons.exporters.impl.users.Constants.GROUPS;
 import static com.adobe.acs.commons.exporters.impl.users.Constants.GROUP_FILTER;
 
 import java.io.IOException;
 
 import javax.servlet.ServletException;
 
 import org.apache.felix.scr.annotations.sling.SlingServlet;
 import org.apache.sling.api.SlingHttpServletRequest;
 import org.apache.sling.api.SlingHttpServletResponse;
 import org.apache.sling.api.resource.ModifiableValueMap;
 import org.apache.sling.api.resource.ValueMap;
 import org.apache.sling.api.servlets.SlingAllMethodsServlet;
 
 @SlingServlet(
         methods = {"POST"},
         resourceTypes = {"acs-commons/components/utilities/exporters/users-to-csv"},
         selectors = {"save"},
         extensions = {"json"}
         )
 public class UsersSaveServlet extends SlingAllMethodsServlet {
 
 
/** Add the Users to CSV form data to the underlying jcr */
 public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException{
         
             ValueMap properties = request.getResource().adaptTo(ValueMap.class);
             String groupFilter = properties.get(GROUP_FILTER, "");
             String groups = properties.get(GROUPS, "");
             String customProperties = properties.get(CUSTOM_PROPERTIES, "");
         
             String[] groupArray = groups.split(",");
             String[] customPropertyArray = customProperties.split(",");
         
             for (String group : groupArray) {
          String[] groupProperties = group.split(":");
          String groupName = groupProperties[0];
          String groupPath = groupProperties[1];
          String groupType = groupProperties[2];
         
          String[] customPropertyProperties = customPropertyArray[0].split(":");
          String customPropertyName = customPropertyProperties[0];
          String customPropertyPath = customPropertyProperties[1];
          String customPropertyType = customPropertyProperties[2];
         
          String[] customPropertyProperties2 = customPropertyArray[1].split(":");
          String customPropertyName2 = customPropertyProperties2[0];
          String customPropertyPath2 = customPropertyProperties2[1];
          String customPropertyType2 = customPropertyProperties2[2];
         
          String[] customPropertyProperties3 = customPropertyArray[2].split(":");
          String customPropertyName3 = customPropertyProperties3[0];
          String customPropertyPath3 = customPropertyProperties3[1];
          String customPropertyType3 = customPropertyProperties3[2];
         
          String[] customPropertyProperties4 = customPropertyArray[3].split(":");
          String customPropertyName4 = customPropertyProperties4[0];
          String customPropertyPath4 = customPropertyProperties4[1];
          String customPropertyType4 = customPropertyProperties4[2];
         
          String[] customPropertyProperties5 = customPropertyArray[4].split(":");
          String customPropertyName5 = customPropertyProperties5[0];
          String customPropertyPath5 = customPropertyProperties5[1];
          String                        
 }

 

}