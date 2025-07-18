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
 
 package com.adobe.acs.commons.mcp.impl.processes;
 
 import com.adobe.acs.commons.mcp.form.NumberfieldComponent;
 import com.adobe.acs.commons.workflow.bulk.removal.WorkflowRemovalConfig;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.Collections;
 import java.util.Date;
 import java.util.EnumMap;
 import java.util.List;
 import java.util.regex.Pattern;
 import java.util.regex.PatternSyntaxException;
 import java.util.stream.Collectors;
 
 import javax.jcr.RepositoryException;
 
 import org.apache.commons.lang.StringUtils;
 import org.apache.sling.api.resource.LoginException;
 import org.apache.sling.api.resource.PersistenceException;
 import org.apache.sling.api.resource.ResourceResolver;
 
 import com.adobe.acs.commons.fam.ActionManager;
 import com.adobe.acs.commons.mcp.ProcessDefinition;
 import com.adobe.acs.commons.mcp.ProcessInstance;
 import com.adobe.acs.commons.mcp.form.DatePickerComponent;
 import com.adobe.acs.commons.mcp.form.FormField;
 import com.adobe.acs.commons.mcp.form.MultifieldComponent;
 import com.adobe.acs.commons.mcp.model.GenericBlobReport;
 import com.adobe.acs.commons.workflow.bulk.removal.WorkflowInstanceRemover;
 import com.adobe.acs.commons.workflow.bulk.removal.WorkflowRemovalStatus;
 
 /**
  * Removes workflow process instances based on specified conditions.
  */
 public class WorkflowRemover extends ProcessDefinition {
 
     private static final int BATCH_SIZE = 1000;
     // don't constrain the runtime
     private static final int MAX_DURATION_MINS = 0;
 
     private final WorkflowInstanceRemover workflowInstanceRemover;
 
     private final transient GenericBlobReport report = new GenericBlobReport();
     private final transient List<EnumMap<ReportColumns, Object>> reportRows = new ArrayList<>();
 
     @FormField(name = "Workflow Payload Paths", description = "Payload path regex", hint = "/content/dam/.*",
             component = MultifieldComponent.class)
     public List<String> payloadPaths;
 
     @FormField(name = "Workflows Older Than", description = "only remove workflows older than the specified date",
             component = DatePickerComponent.class)
     public String olderThanVal;
 
     @FormField(name = "Workflows Older Than Milliseconds", description = "only remove workflows that were started longer than the specified milliseconds ago",
             component = NumberfieldComponent.class)
     public long olderThanMillis;
 
     @FormField(
             name = "Workflow Models",
             description = "If no Workflow Models are selected, Workflow Instances will not be filtered by Workflow Model.",
             component = MultifieldComponent.class, options = { MultifieldComponent.USE_CLASS
                     + "=com.adobe.acs.commons.mcp.form.workflow.WorkflowModelSelector" })
     public List<String> modelIds = new ArrayList<>();
 
     private List<Pattern> payloads = new ArrayList<>();
     private Calendar olderThan;
 
     @FormField(name = "Workflow Statuses", component = MultifieldComponent.class, required = true,
             options = { MultifieldComponent.USE_CLASS
                     + "=com.adobe.acs.commons.mcp.form.workflow.WorkflowStatusSelector" })
     public List<String> statuses = new ArrayList<>();
 
     private WorkflowRemovalConfig workflowRemovalConfig;
 
     public WorkflowRemover(WorkflowInstanceRemover workflowInstanceRemover) {
         super();
         this.workflowInstanceRemover = workflowInstanceRemover;
     }
 
     @Override
     public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
         instance.defineCriticalAction("Seek and Destroy Workflows", rr, this::performCleanupActivity);
 
         // TODO I'd eventually like to refactor this as follows, but that requires some significant change to the
         // underlying service, which I'm not ready to take on
         // criticalAction - find workflows to remove
         // if !dryRun
         // action - remove workflows
         // action - remove empty folders
         // end if
     }
 
     @Override
     public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException,
             PersistenceException {
         report.setRows(reportRows, ReportColumns.class);
         report.persist(rr, instance.getPath() + "/jcr:content/report");
     }
 
     @Override
     public void init() throws RepositoryException {
         // No init needed, do nothing.
     }
 
     /**
      * Cleanup the old workflows.  Package scoped for unit test purposes.
      * @param manager the action manager to handle removal.
      * @throws Exception if an error occurs
      */
     void performCleanupActivity(ActionManager manager) throws Exception {
         manager.withResolver(rr -> {
 
             parseParameters();
 
             workflowInstanceRemover.removeWorkflowInstances(rr, workflowRemovalConfig);
 
             WorkflowRemovalStatus status = workflowInstanceRemover.getStatus();
             EnumMap<ReportColumns, Object> reportRow = report(status);
             reportRows.add(reportRow);
         });
     }
 
 
/** Collect and return a report row for the workflow status. */
  EnumMap<ReportColumns, Object> report(WorkflowRemovalStatus status){}

 

}