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
 
 package org.apache.shardingsphere.elasticjob.lite.lifecycle.internal.reg;
 
 import com.google.common.base.Strings;
 import com.google.common.hash.HashCode;
 import com.google.common.hash.Hasher;
 import com.google.common.hash.Hashing;
 import lombok.AccessLevel;
 import lombok.NoArgsConstructor;
 import org.apache.shardingsphere.elasticjob.reg.base.CoordinatorRegistryCenter;
 import org.apache.shardingsphere.elasticjob.reg.zookeeper.ZookeeperConfiguration;
 import org.apache.shardingsphere.elasticjob.reg.zookeeper.ZookeeperRegistryCenter;
 
 import java.nio.charset.StandardCharsets;
 import java.util.Map;
 import java.util.concurrent.ConcurrentHashMap;
 
 /**
  * Registry center factory.
  */
 @SuppressWarnings("UnstableApiUsage")
 @NoArgsConstructor(access = AccessLevel.PRIVATE)
 public final class RegistryCenterFactory {
     
     private static final Map<HashCode, CoordinatorRegistryCenter> REG_CENTER_REGISTRY = new ConcurrentHashMap<>();
     
 
/** Build a CoordinatorRegistryCenter or return the existing one if there is one set up with the given connection string */
 public static CoordinatorRegistryCenter createCoordinatorRegistryCenter(final String connectString, final String namespace, final String digest){
      if (Strings.isNullOrEmpty(connectString)) {
          throw new IllegalArgumentException("Connect string can not be null or empty.");
      }
      if (Strings.isNullOrEmpty(namespace)) {
          throw new IllegalArgumentException("Namespace can not be null or empty.");
      }
      if (Strings.isNullOrEmpty(digest)) {
          throw new IllegalArgumentException("Digest can not be null or empty.");
      }
      final HashCode hashCode = Hashing.sha256().hashString(connectString + namespace + digest, StandardCharsets.UTF_8);
      CoordinatorRegistryCenter regCenter = REG_CENTER_REGISTRY.get(hashCode);
      if (null == regCenter) {
          regCenter = new ZookeeperRegistryCenter(new ZookeeperConfiguration(connectString, namespace));
          REG_CENTER_REGISTRY.put(hashCode, regCenter);
      }
      return regCenter;
  }
  } 
 }

 

}