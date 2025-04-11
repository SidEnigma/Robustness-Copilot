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
 
 package org.apache.shardingsphere.elasticjob.lite.internal.config;
 
 import org.apache.shardingsphere.elasticjob.api.JobConfiguration;
 import org.apache.shardingsphere.elasticjob.infra.exception.JobConfigurationException;
 import org.apache.shardingsphere.elasticjob.infra.exception.JobExecutionEnvironmentException;
 import org.apache.shardingsphere.elasticjob.infra.pojo.JobConfigurationPOJO;
 import org.apache.shardingsphere.elasticjob.lite.internal.storage.JobNodeStorage;
 import org.apache.shardingsphere.elasticjob.reg.base.CoordinatorRegistryCenter;
 import org.apache.shardingsphere.elasticjob.infra.env.TimeService;
 import org.apache.shardingsphere.elasticjob.infra.yaml.YamlEngine;
 
 /**
  * Configuration service.
  */
 public final class ConfigurationService {
     
     private final TimeService timeService;
     
     private final JobNodeStorage jobNodeStorage;
     
     public ConfigurationService(final CoordinatorRegistryCenter regCenter, final String jobName) {
         jobNodeStorage = new JobNodeStorage(regCenter, jobName);
         timeService = new TimeService();
     }
     
     /**
      * Load job configuration.
      * 
      * @param fromCache load from cache or not
      * @return job configuration
      */
     public JobConfiguration load(final boolean fromCache) {
         String result;
         if (fromCache) {
             result = jobNodeStorage.getJobNodeData(ConfigurationNode.ROOT);
             if (null == result) {
                 result = jobNodeStorage.getJobNodeDataDirectly(ConfigurationNode.ROOT);
             }
         } else {
             result = jobNodeStorage.getJobNodeDataDirectly(ConfigurationNode.ROOT);
         }
         return YamlEngine.unmarshal(result, JobConfigurationPOJO.class).toJobConfiguration();
     }
     
     /**
      * Set up job configuration.
      * 
      * @param jobClassName job class name
      * @param jobConfig job configuration to be updated
      * @return accepted job configuration
      */
     public JobConfiguration setUpJobConfiguration(final String jobClassName, final JobConfiguration jobConfig) {
         checkConflictJob(jobClassName, jobConfig);
         if (!jobNodeStorage.isJobNodeExisted(ConfigurationNode.ROOT) || jobConfig.isOverwrite()) {
             jobNodeStorage.replaceJobNode(ConfigurationNode.ROOT, YamlEngine.marshal(JobConfigurationPOJO.fromJobConfiguration(jobConfig)));
             jobNodeStorage.replaceJobRootNode(jobClassName);
             return jobConfig;
         }
         return load(false);
     }
     
     private void checkConflictJob(final String newJobClassName, final JobConfiguration jobConfig) {
         if (!jobNodeStorage.isJobRootNodeExisted()) {
             return;
         }
         String originalJobClassName = jobNodeStorage.getJobRootNodeData();
         if (null != originalJobClassName && !originalJobClassName.equals(newJobClassName)) {
             throw new JobConfigurationException(
                     "Job conflict with register center. The job '%s' in register center's class is '%s', your job class is '%s'", jobConfig.getJobName(), originalJobClassName, newJobClassName);
         }
     }
     
 
/** Between the job server and the registration centre, the maximum time of several seconds can be controlled */

public void checkMaxTimeDiffSecondsTolerable() throws JobExecutionEnvironmentException {
    long maxTimeDiffSeconds = 5; // Maximum time difference in seconds
    long timeDiff = timeService.getCurrentMillis() - jobNodeStorage.getRegistryCenterTime(ConfigurationNode.ROOT);
    if (timeDiff > maxTimeDiffSeconds * 1000 || timeDiff < -maxTimeDiffSeconds * 1000) {
        throw new JobExecutionEnvironmentException("Time difference between job server and registration center exceeds %d seconds", maxTimeDiffSeconds);
    }
}
 

}