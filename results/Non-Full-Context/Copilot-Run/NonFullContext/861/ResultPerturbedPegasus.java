/*
  * #%L
  * ACS AEM Commons Bundle
  * %%
  * Copyright (C) 2013 Adobe
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
 package com.adobe.acs.commons.util;
 
 import java.util.concurrent.Callable;
 
 import org.osgi.annotation.versioning.ProviderType;
 
 /**
  * Utility class for executing a particular task with a set Thread Context Class Loader.
  */
 @ProviderType
 public final class ThreadContextClassLoaderTaskExecutor {
     
     private ThreadContextClassLoaderTaskExecutor() {
     }
 
 
/** The Thread Context Class Loader is set to execute the task. */
 public static V doWithTccl(ClassLoader classLoader, Callable<V> task) throws Exception{
      Thread currentThread = Thread.currentThread();
      ClassLoader oldClassLoader = currentThread.getContextClassLoader();
      currentThread.setContextClassLoader(classLoader);
      try {
          return task.call();
      } finally {
          currentThread.setContextClassLoader(oldClassLoader);
      }
  } 
 }

 

}