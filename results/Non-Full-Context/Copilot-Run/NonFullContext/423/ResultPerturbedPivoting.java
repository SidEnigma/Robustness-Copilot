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
 
 
 package org.activiti.engine.impl.persistence.entity;
 
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.Collection;
 import java.util.Date;
 import java.util.GregorianCalendar;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import org.activiti.bpmn.model.SequenceFlow;
 import org.activiti.bpmn.model.UserTask;
 import org.activiti.engine.ActivitiObjectNotFoundException;
 import org.activiti.engine.delegate.event.ActivitiEventType;
 import org.activiti.engine.delegate.event.ActivitiProcessCancelledEvent;
 import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
 import org.activiti.engine.history.DeleteReason;
 import org.activiti.engine.impl.ExecutionQueryImpl;
 import org.activiti.engine.impl.Page;
 import org.activiti.engine.impl.ProcessInstanceQueryImpl;
 import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
 import org.activiti.engine.impl.context.Context;
 import org.activiti.engine.impl.identity.Authentication;
 import org.activiti.engine.impl.persistence.CountingExecutionEntity;
 import org.activiti.engine.impl.persistence.entity.data.DataManager;
 import org.activiti.engine.impl.persistence.entity.data.ExecutionDataManager;
 import org.activiti.engine.repository.ProcessDefinition;
 import org.activiti.engine.runtime.Execution;
 import org.activiti.engine.runtime.ProcessInstance;
 import org.activiti.engine.task.IdentityLinkType;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
 
 
  */
 public class ExecutionEntityManagerImpl extends AbstractEntityManager<ExecutionEntity> implements ExecutionEntityManager {
 
   private static final Logger logger = LoggerFactory.getLogger(ExecutionEntityManagerImpl.class);
 
   protected ExecutionDataManager executionDataManager;
 
   public ExecutionEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, ExecutionDataManager executionDataManager) {
     super(processEngineConfiguration);
     this.executionDataManager = executionDataManager;
   }
 
   @Override
   protected DataManager<ExecutionEntity> getDataManager() {
     return executionDataManager;
   }
 
   // Overriding the default delete methods to set the 'isDeleted' flag
 
   @Override
   public void delete(ExecutionEntity entity) {
     delete(entity, true);
   }
 
   @Override
   public void delete(ExecutionEntity entity, boolean fireDeleteEvent) {
     super.delete(entity, fireDeleteEvent);
     entity.setDeleted(true);
   }
 
   // FIND METHODS
 
   @Override
   public ExecutionEntity findSubProcessInstanceBySuperExecutionId(String superExecutionId) {
     return executionDataManager.findSubProcessInstanceBySuperExecutionId(superExecutionId);
   }
 
   @Override
   public List<ExecutionEntity> findChildExecutionsByParentExecutionId(String parentExecutionId) {
     return executionDataManager.findChildExecutionsByParentExecutionId(parentExecutionId);
   }
 
   @Override
   public List<ExecutionEntity> findChildExecutionsByProcessInstanceId(String processInstanceId) {
     return executionDataManager.findChildExecutionsByProcessInstanceId(processInstanceId);
   }
 
   @Override
   public List<ExecutionEntity> findExecutionsByParentExecutionAndActivityIds(final String parentExecutionId, final Collection<String> activityIds) {
     return executionDataManager.findExecutionsByParentExecutionAndActivityIds(parentExecutionId, activityIds);
   }
 
   @Override
   public long findExecutionCountByQueryCriteria(ExecutionQueryImpl executionQuery) {
     return executionDataManager.findExecutionCountByQueryCriteria(executionQuery);
   }
 
   @Override
   public List<ExecutionEntity> findExecutionsByQueryCriteria(ExecutionQueryImpl executionQuery, Page page) {
     return executionDataManager.findExecutionsByQueryCriteria(executionQuery, page);
   }
 
   @Override
   public long findProcessInstanceCountByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
     return executionDataManager.findProcessInstanceCountByQueryCriteria(executionQuery);
   }
 
   @Override
   public List<ProcessInstance> findProcessInstanceByQueryCriteria(ProcessInstanceQueryImpl executionQuery) {
     return executionDataManager.findProcessInstanceByQueryCriteria(executionQuery);
   }
 
   @Override
   public ExecutionEntity findByRootProcessInstanceId(String rootProcessInstanceId) {
     List<ExecutionEntity> executions = executionDataManager.findExecutionsByRootProcessInstanceId(rootProcessInstanceId);
     return processExecutionTree(rootProcessInstanceId, executions);
 
   }
 
 
/** Processes a collection of {@link ExecutionEntity} instances, which form in the runtime tree. */
 protected ExecutionEntity processExecutionTree(String rootProcessInstanceId, List<ExecutionEntity> executions){
    ExecutionEntity rootExecution = null;
    for (ExecutionEntity execution : executions) {
      if (execution.getProcessInstanceId().equals(rootProcessInstanceId)) {
        rootExecution = execution;
        break;
      }
    }
    if (rootExecution == null) {
      throw new ActivitiObjectNotFoundException("No execution found for root process instance id: " + rootProcessInstanceId, ProcessInstance.class);
    }
    return rootExecution;
  }
  
    @Override
    public ExecutionEntity findExecutionById(String executionId) {
      return executionDataManager.findExecutionById(executionId);
    }
  
    @Override
    public ExecutionEntity findExecutionById(String executionId, boolean includeProcessInstance) {
      return executionDataManager.findExecutionById(executionId   
 }

 

}