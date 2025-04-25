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
 
 package org.apache.shardingsphere.elasticjob.lite.internal.guarantee;
 
 import org.apache.shardingsphere.elasticjob.lite.internal.config.ConfigurationService;
 import org.apache.shardingsphere.elasticjob.lite.internal.storage.JobNodeStorage;
 import org.apache.shardingsphere.elasticjob.reg.base.CoordinatorRegistryCenter;
 
 import java.util.Collection;
 
 /**
  * Guarantee service.
  */
 public final class GuaranteeService {
     
     private final JobNodeStorage jobNodeStorage;
     
     private final ConfigurationService configService;
     
     public GuaranteeService(final CoordinatorRegistryCenter regCenter, final String jobName) {
         jobNodeStorage = new JobNodeStorage(regCenter, jobName);
         configService = new ConfigurationService(regCenter, jobName);
     }
     
     /**
      * Register start.
      *
      * @param shardingItems to be registered sharding items
      */
     public void registerStart(final Collection<Integer> shardingItems) {
         for (int each : shardingItems) {
             jobNodeStorage.createJobNodeIfNeeded(GuaranteeNode.getStartedNode(each));
         }
     }
     
     /**
      * Judge whether current sharding items are all register start success.
      *
      * @param shardingItems current sharding items
      * @return current sharding items are all start success or not
      */
     public boolean isRegisterStartSuccess(final Collection<Integer> shardingItems) {
         for (int each : shardingItems) {
             if (!jobNodeStorage.isJobNodeExisted(GuaranteeNode.getStartedNode(each))) {
                 return false;
             }
         }
         return true;
     }
     
 
/** Checks if the job's sharding items have been started. */
 public boolean isAllStarted(){
        return jobNodeStorage.isJobNodeExisted(GuaranteeNode.getAllStartedNode());
    }
    
        /**
        * Register success.
        *
        * @param shardingItems to be registered sharding items
        */
        public void registerSuccess(final Collection<Integer> shardingItems) {
            for (int each : shardingItems) {
                jobNodeStorage.createJobNodeIfNeeded(GuaranteeNode.getSuccessNode(each));
            }
        }
        
        /**
        * Judge whether current sharding items are all register success.
        *
        * @param shardingItems current sharding items
        * @return current sharding items are all success or not
        */
        public boolean isRegisterSuccess(final Collection<Integer> shardingItems) {
            for (int each : shardingItems) {
                if (!jobNodeStorage.isJobNodeExisted(GuaranteeNode.getSuccessNode(each))) {
                    return false;
                }
            }
            return true;
        }
        
        /**
        * Register fail.
        *
        * @param shardingItems to be registered sharding items
        */
        public void registerFail(final Collection<Integer> shardingItems) {
            for (int each : shardingItems) {
                jobNodeStorage.createJobNodeIfNeeded(GuaranteeNode.getFailNode(each));
            }
        }
        
        /**
        * Judge whether current sharding items are all register fail.
        *
        * @param shardingItems current sharding items
        * @return current sharding items are all fail or not
        */
        public boolean isRegisterFail(final Collection<Integer> shardingItems) {
            for (int each : shardingItems) {
                if (!jobNodeStorage.isJobNodeExisted(GuaranteeNode.getFailNode(each))) {
                    return false;
                }
            }
            return true;
        }
        
        /**
        * Register complete.
        *
        * @param shardingItems to be registered sharding items
        */
        public void registerComplete(final Collection<Integer       
 }

 

}