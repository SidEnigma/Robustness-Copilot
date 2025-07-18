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
 
 
/** Keeps users' CSV form data in the underlying jcr:content node. */
 public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException{
                                        
 }

 

}