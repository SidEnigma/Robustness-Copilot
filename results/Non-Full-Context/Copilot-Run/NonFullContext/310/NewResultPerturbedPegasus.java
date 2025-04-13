/*
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.apache.shardingsphere.elasticjob.cloud.console.controller;
 
 import com.google.common.base.Preconditions;
 import com.google.common.base.Strings;
 import lombok.extern.slf4j.Slf4j;
 import org.apache.shardingsphere.elasticjob.cloud.config.CloudJobExecutionType;
 import org.apache.shardingsphere.elasticjob.cloud.config.pojo.CloudJobConfigurationPOJO;
 import org.apache.shardingsphere.elasticjob.cloud.console.controller.search.JobEventRdbSearch;
 import org.apache.shardingsphere.elasticjob.cloud.scheduler.config.job.CloudJobConfigurationService;
 import org.apache.shardingsphere.elasticjob.cloud.scheduler.env.BootstrapEnvironment;
 import org.apache.shardingsphere.elasticjob.cloud.scheduler.mesos.FacadeService;
 import org.apache.shardingsphere.elasticjob.cloud.scheduler.producer.ProducerManager;
 import org.apache.shardingsphere.elasticjob.cloud.scheduler.state.failover.FailoverTaskInfo;
 import org.apache.shardingsphere.elasticjob.cloud.scheduler.statistics.StatisticManager;
 import org.apache.shardingsphere.elasticjob.cloud.statistics.StatisticInterval;
 import org.apache.shardingsphere.elasticjob.cloud.statistics.type.job.JobExecutionTypeStatistics;
 import org.apache.shardingsphere.elasticjob.cloud.statistics.type.job.JobRegisterStatistics;
 import org.apache.shardingsphere.elasticjob.cloud.statistics.type.job.JobRunningStatistics;
 import org.apache.shardingsphere.elasticjob.cloud.statistics.type.task.TaskResultStatistics;
 import org.apache.shardingsphere.elasticjob.cloud.statistics.type.task.TaskRunningStatistics;
 import org.apache.shardingsphere.elasticjob.infra.context.TaskContext;
 import org.apache.shardingsphere.elasticjob.infra.exception.JobSystemException;
 import org.apache.shardingsphere.elasticjob.reg.base.CoordinatorRegistryCenter;
 import org.apache.shardingsphere.elasticjob.restful.Http;
 import org.apache.shardingsphere.elasticjob.restful.wrapper.QueryParameterMap;
 import org.apache.shardingsphere.elasticjob.restful.annotation.ParamSource;
 import org.apache.shardingsphere.elasticjob.restful.RestfulController;
 import org.apache.shardingsphere.elasticjob.restful.annotation.ContextPath;
 import org.apache.shardingsphere.elasticjob.restful.annotation.Mapping;
 import org.apache.shardingsphere.elasticjob.restful.annotation.Param;
 import org.apache.shardingsphere.elasticjob.restful.annotation.RequestBody;
 import org.apache.shardingsphere.elasticjob.tracing.api.TracingConfiguration;
 import org.apache.shardingsphere.elasticjob.tracing.event.JobExecutionEvent;
 import org.apache.shardingsphere.elasticjob.tracing.event.JobStatusTraceEvent;
 
 import javax.sql.DataSource;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Optional;
 import java.util.Set;
 
 /**
  * Cloud job restful api.
  */
 @Slf4j
 @ContextPath("/api/job")
 public final class CloudJobController implements RestfulController {
     
     private static CoordinatorRegistryCenter regCenter;
     
     private static JobEventRdbSearch jobEventRdbSearch;
     
     private static ProducerManager producerManager;
     
     private final CloudJobConfigurationService configService;
     
     private final FacadeService facadeService;
     
     private final StatisticManager statisticManager;
     
     public CloudJobController() {
         Preconditions.checkNotNull(regCenter);
         configService = new CloudJobConfigurationService(regCenter);
         facadeService = new FacadeService(regCenter);
         statisticManager = StatisticManager.getInstance(regCenter, null);
     }
     
     /**
      * Init.
      * @param regCenter       registry center
      * @param producerManager producer manager
      */
     public static void init(final CoordinatorRegistryCenter regCenter, final ProducerManager producerManager) {
         CloudJobController.regCenter = regCenter;
         CloudJobController.producerManager = producerManager;
         Optional<TracingConfiguration<?>> tracingConfiguration = BootstrapEnvironment.getINSTANCE().getTracingConfiguration();
         jobEventRdbSearch = tracingConfiguration.map(tracingConfiguration1 -> new JobEventRdbSearch((DataSource) tracingConfiguration1.getTracingStorageConfiguration().getStorage())).orElse(null);
     }
     
     /**
      * Register cloud job.
      *
      * @param cloudJobConfig cloud job configuration
      * @return <em>true</em> for operation finished.
      */
     @Mapping(method = Http.POST, path = "/register")
     public boolean register(@RequestBody final CloudJobConfigurationPOJO cloudJobConfig) {
         producerManager.register(cloudJobConfig);
         return true;
     }
     
     /**
      * Update cloud job.
      *
      * @param cloudJobConfig cloud job configuration
      * @return <em>true</em> for operation finished.
      */
     @Mapping(method = Http.PUT, path = "/update")
     public boolean update(@RequestBody final CloudJobConfigurationPOJO cloudJobConfig) {
         producerManager.update(cloudJobConfig);
         return true;
     }
     
     /**
      * Deregister cloud job.
      *
      * @param jobName job name
      * @return <em>true</em> for operation finished.
      */
     @Mapping(method = Http.DELETE, path = "/{jobName}/deregister")
     public boolean deregister(@Param(name = "jobName", source = ParamSource.PATH) final String jobName) {
         producerManager.deregister(jobName);
         return true;
     }
     
 
/** Check to see if the cloud job is disabled. */
@Mapping(method = Http.GET, path = "/{jobName}/disabled")
public boolean isDisabled(@Param(name = "jobName", source = ParamSource.PATH) final String jobName) {
    CloudJobConfigurationPOJO jobConfig = configService.load(jobName);
    return jobConfig != null && jobConfig.isDisabled();
}

}