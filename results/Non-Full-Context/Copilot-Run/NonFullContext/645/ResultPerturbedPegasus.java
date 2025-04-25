/*
  * Copyright 2010-2020 Alfresco Software, Ltd.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.activiti.engine.impl.bpmn.deployer;
 
 import java.util.Collection;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 
 import com.fasterxml.jackson.databind.JsonNode;
 import com.fasterxml.jackson.databind.node.ObjectNode;
 import org.activiti.bpmn.constants.BpmnXMLConstants;
 import org.activiti.bpmn.model.BpmnModel;
 import org.activiti.bpmn.model.ExtensionElement;
 import org.activiti.bpmn.model.FlowElement;
 import org.activiti.bpmn.model.Process;
 import org.activiti.bpmn.model.SubProcess;
 import org.activiti.bpmn.model.UserTask;
 import org.activiti.bpmn.model.ValuedDataObject;
 import org.activiti.engine.DynamicBpmnConstants;
 import org.activiti.engine.DynamicBpmnService;
 import org.activiti.engine.delegate.event.ActivitiEventType;
 import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
 import org.activiti.engine.impl.cfg.IdGenerator;
 import org.activiti.engine.impl.context.Context;
 import org.activiti.engine.impl.interceptor.CommandContext;
 import org.activiti.engine.impl.persistence.deploy.Deployer;
 import org.activiti.engine.impl.persistence.entity.DeploymentEntity;
 import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
 import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
 import org.activiti.engine.impl.persistence.entity.ResourceEntity;
 import org.apache.commons.lang3.StringUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public class BpmnDeployer implements Deployer {
 
     private static final Logger log = LoggerFactory.getLogger(BpmnDeployer.class);
 
     protected IdGenerator idGenerator;
     protected ParsedDeploymentBuilderFactory parsedDeploymentBuilderFactory;
     protected BpmnDeploymentHelper bpmnDeploymentHelper;
     protected CachingAndArtifactsManager cachingAndArtifactsManager;
 
     @Override
     public void deploy(DeploymentEntity deployment,
                        Map<String, Object> deploymentSettings) {
         log.debug("Processing deployment {}",
                   deployment.getName());
 
         // The ParsedDeployment represents the deployment, the process definitions, and the BPMN
         // resource, parse, and model associated with each process definition.
         ParsedDeployment parsedDeployment = parsedDeploymentBuilderFactory
                 .getBuilderForDeploymentAndSettings(deployment,
                                                     deploymentSettings)
                 .build();
 
         bpmnDeploymentHelper.verifyProcessDefinitionsDoNotShareKeys(parsedDeployment.getAllProcessDefinitions());
 
         bpmnDeploymentHelper.copyDeploymentValuesToProcessDefinitions(
                 parsedDeployment.getDeployment(),
                 parsedDeployment.getAllProcessDefinitions());
         bpmnDeploymentHelper.setResourceNamesOnProcessDefinitions(parsedDeployment);
 
 //    createAndPersistNewDiagramsIfNeeded(parsedDeployment);
         setProcessDefinitionDiagramNames(parsedDeployment);
 
         if (deployment.isNew()) {
             Map<ProcessDefinitionEntity, ProcessDefinitionEntity> mapOfNewProcessDefinitionToPreviousVersion =
                     getPreviousVersionsOfProcessDefinitions(parsedDeployment);
             setProcessDefinitionVersionsAndIds(parsedDeployment,
                                                mapOfNewProcessDefinitionToPreviousVersion);
             setProcessDefinitionAppVersion(parsedDeployment);
 
             persistProcessDefinitionsAndAuthorizations(parsedDeployment);
             updateTimersAndEvents(parsedDeployment,
                                   mapOfNewProcessDefinitionToPreviousVersion);
             dispatchProcessDefinitionEntityInitializedEvent(parsedDeployment);
         } else {
             makeProcessDefinitionsConsistentWithPersistedVersions(parsedDeployment);
         }
 
         cachingAndArtifactsManager.updateCachingAndArtifacts(parsedDeployment);
 
         for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
             BpmnModel bpmnModel = parsedDeployment.getBpmnModelForProcessDefinition(processDefinition);
             createLocalizationValues(processDefinition.getId(),
                                      bpmnModel.getProcessById(processDefinition.getKey()));
         }
     }
 //
 //  /**
 //   * Creates new diagrams for process definitions if the deployment is new, the process definition in
 //   * question supports it, and the engine is configured to make new diagrams.
 //   *
 //   * When this method creates a new diagram, it also persists it via the ResourceEntityManager
 //   * and adds it to the resources of the deployment.
 //   */
 //  protected void createAndPersistNewDiagramsIfNeeded(ParsedDeployment parsedDeployment) {
 //
 //    final ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
 //    final DeploymentEntity deploymentEntity = parsedDeployment.getDeployment();
 //
 //    final ResourceEntityManager resourceEntityManager = processEngineConfiguration.getResourceEntityManager();
 //
 //    for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
 //      if (processDefinitionDiagramHelper.shouldCreateDiagram(processDefinition, deploymentEntity)) {
 //        ResourceEntity resource = processDefinitionDiagramHelper.createDiagramForProcessDefinition(
 //            processDefinition, parsedDeployment.getBpmnParseForProcessDefinition(processDefinition));
 //        if (resource != null) {
 //          resourceEntityManager.insert(resource, false);
 //          deploymentEntity.addResource(resource);  // now we'll find it if we look for the diagram name later.
 //        }
 //      }
 //    }
 //  }
 
     /**
      * Updates all the process definition entities to have the correct diagram resource name.  Must
      * be called after createAndPersistNewDiagramsAsNeeded to ensure that any newly-created diagrams
      * already have their resources attached to the deployment.
      */
     protected void setProcessDefinitionDiagramNames(ParsedDeployment parsedDeployment) {
         Map<String, ResourceEntity> resources = parsedDeployment.getDeployment().getResources();
 
         for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
             String diagramResourceName = ResourceNameUtil.getProcessDiagramResourceNameFromDeployment(processDefinition,
                                                                                                       resources);
             processDefinition.setDiagramResourceName(diagramResourceName);
         }
     }
 
     /**
      * Constructs a map from new ProcessDefinitionEntities to the previous version by key and tenant.
      * If no previous version exists, no map entry is created.
      */
     protected Map<ProcessDefinitionEntity, ProcessDefinitionEntity> getPreviousVersionsOfProcessDefinitions(
             ParsedDeployment parsedDeployment) {
 
         Map<ProcessDefinitionEntity, ProcessDefinitionEntity> result = new LinkedHashMap<ProcessDefinitionEntity, ProcessDefinitionEntity>();
 
         for (ProcessDefinitionEntity newDefinition : parsedDeployment.getAllProcessDefinitions()) {
             ProcessDefinitionEntity existingDefinition = bpmnDeploymentHelper.getMostRecentVersionOfProcessDefinition(newDefinition);
 
             if (existingDefinition != null) {
                 result.put(newDefinition,
                            existingDefinition);
             }
         }
 
         return result;
     }
 
     /**
      * Sets the version on each process definition entity, and the identifier.  If the map contains
      * an older version for a process definition, then the version is set to that older entity's
      * version plus one; otherwise it is set to 1.  Also dispatches an ENTITY_CREATED event.
      */
     protected void setProcessDefinitionVersionsAndIds(ParsedDeployment parsedDeployment,
                                                       Map<ProcessDefinitionEntity, ProcessDefinitionEntity> mapNewToOldProcessDefinitions) {
         CommandContext commandContext = Context.getCommandContext();
 
         if(parsedDeployment.getDeployment().getProjectReleaseVersion() != null){
             Integer version = parsedDeployment.getDeployment().getVersion();
             for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
                 processDefinition.setVersion(version);
                 processDefinition.setId(getIdForNewProcessDefinition(processDefinition));
 
                 if (commandContext.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
                     commandContext.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_CREATED,
                                                                                                                                              processDefinition));
                 }
             }
         }else{
 
             for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
                 int version = 1;
 
                 ProcessDefinitionEntity latest = mapNewToOldProcessDefinitions.get(processDefinition);
                 if (latest != null) {
                     version = latest.getVersion() + 1;
                 }
 
                 processDefinition.setVersion(version);
                 processDefinition.setId(getIdForNewProcessDefinition(processDefinition));
 
                 if (commandContext.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
                     commandContext.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_CREATED,
                                                                                                                                              processDefinition));
                 }
             }
         }
 
 
     }
 
     /**
      * Saves each process definition.  It is assumed that the deployment is new, the definitions
      * have never been saved before, and that they have all their values properly set up.
      */
     protected void persistProcessDefinitionsAndAuthorizations(ParsedDeployment parsedDeployment) {
         CommandContext commandContext = Context.getCommandContext();
         ProcessDefinitionEntityManager processDefinitionManager = commandContext.getProcessDefinitionEntityManager();
 
         for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
             processDefinitionManager.insert(processDefinition,
                                             false);
             bpmnDeploymentHelper.addAuthorizationsForNewProcessDefinition(parsedDeployment.getProcessModelForProcessDefinition(processDefinition),
                                                                           processDefinition);
         }
     }
 
     protected void updateTimersAndEvents(ParsedDeployment parsedDeployment,
                                          Map<ProcessDefinitionEntity, ProcessDefinitionEntity> mapNewToOldProcessDefinitions) {
 
         for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
             bpmnDeploymentHelper.updateTimersAndEvents(processDefinition,
                                                        mapNewToOldProcessDefinitions.get(processDefinition),
                                                        parsedDeployment);
         }
     }
 
     protected void dispatchProcessDefinitionEntityInitializedEvent(ParsedDeployment parsedDeployment) {
         CommandContext commandContext = Context.getCommandContext();
         for (ProcessDefinitionEntity processDefinitionEntity : parsedDeployment.getAllProcessDefinitions()) {
             log.info("Process deployed: {id: " + processDefinitionEntity.getId() +
                 ", key: " + processDefinitionEntity.getKey() + ", name: " + processDefinitionEntity.getName() +" }");
             if (commandContext.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
                 commandContext.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
                         ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_INITIALIZED,
                                                                processDefinitionEntity));
             }
         }
     }
 
     /**
      * Returns the ID to use for a new process definition; subclasses may override this to provide
      * their own identification scheme.
      * <p>
      * Process definition ids NEED to be unique accross the whole engine!
      */
     protected String getIdForNewProcessDefinition(ProcessDefinitionEntity processDefinition) {
         String nextId = idGenerator.getNextId();
 
         String result = processDefinition.getKey() + ":" + processDefinition.getVersion() + ":" + nextId; // ACT-505
         // ACT-115: maximum id length is 64 characters
         if (result.length() > 64) {
             result = nextId;
         }
 
         return result;
     }
 
 
/** Set values on the in-memory version of the persisted version of the process definition to make sure it is consistent. */
 protected void makeProcessDefinitionsConsistentWithPersistedVersions(ParsedDeployment parsedDeployment){
        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            ProcessDefinitionEntity persistedProcessDefinition = bpmnDeploymentHelper.getMostRecentVersionOfProcessDefinition(processDefinition);
            if (persistedProcessDefinition != null) {
                persistedProcessDefinition.setResourceName(processDefinition.getResourceName());
                persistedProcessDefinition.setDeploymentId(processDefinition.getDeploymentId());
                persistedProcessDefinition.setTenantId(processDefinition.getTenantId());
                persistedProcessDefinition.setVersionTag(processDefinition.getVersionTag());
                persistedProcessDefinition.setHistoryLevel(processDefinition.getHistoryLevel());
                persistedProcessDefinition.setSuspensionState(processDefinition.getSuspensionState());
                persistedProcessDefinition.setGraphicalNotationDefined(processDefinition.isGraphicalNotationDefined());
                persistedProcessDefinition.setSuspensionState(processDefinition.getSuspensionState());
                persistedProcessDefinition.setSuspensionDate(processDefinition.getSuspensionDate());
                persistedProcessDefinition.setSuspensionUser(processDefinition.getSuspensionUser());
                persistedProcessDefinition.setDeploymentId(processDefinition.getDeploymentId());
                persistedProcessDefinition.setDeploymentDate(processDefinition.getDeploymentDate());
                persistedProcessDefinition.setDeploymentCategory(processDefinition.getDeploymentCategory());
                persistedProcessDefinition.setDeploymentName(processDefinition.getDeploymentName());
                persistedProcessDefinition.setDeploymentSource(processDefinition.getDeploymentSource());
                persistedProcessDefinition.setDeploymentTenantId(processDefinition.getDeploymentTenantId());
                persistedProcessDefinition.setResourceName(processDefinition.getResourceName());
                persistedProcessDefinition.setResourceType(processDefinition.getResourceType());
                persistedProcessDefinition.setDiagram(processDefinition.getDiagram());
                persistedProcessDefinition.setGraphicalNotationDefined(processDefinition.isGraphicalNotationDefined());
                persistedProcessDefinition.setSuspensionState(processDefinition.getSuspensionState());
                persistedProcessDefinition.setSuspensionDate(processDefinition.getSuspensionDate());
                persistedProcessDefinition      
 }

 

}