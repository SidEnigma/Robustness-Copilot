/*
  *    Copyright 2009-2021 the original author or authors.
  *
  *    Licensed under the Apache License, Version 2.0 (the "License");
  *    you may not use this file except in compliance with the License.
  *    You may obtain a copy of the License at
  *
  *       http://www.apache.org/licenses/LICENSE-2.0
  *
  *    Unless required by applicable law or agreed to in writing, software
  *    distributed under the License is distributed on an "AS IS" BASIS,
  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *    See the License for the specific language governing permissions and
  *    limitations under the License.
  */
 package org.apache.ibatis.io;
 
 import java.io.IOException;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.List;
 
 import org.apache.ibatis.logging.Log;
 import org.apache.ibatis.logging.LogFactory;
 
 /**
  * Provides a very simple API for accessing resources within an application server.
  *
  * @author Ben Gunter
  */
 public abstract class VFS {
   private static final Log log = LogFactory.getLog(VFS.class);
 
   /** The built-in implementations. */
   public static final Class<?>[] IMPLEMENTATIONS = { JBoss6VFS.class, DefaultVFS.class };
 
   /**
    * The list to which implementations are added by {@link #addImplClass(Class)}.
    */
   public static final List<Class<? extends VFS>> USER_IMPLEMENTATIONS = new ArrayList<>();
 
   /** Singleton instance holder. */
   private static class VFSHolder {
     static final VFS INSTANCE = createVFS();
 
     @SuppressWarnings("unchecked")
     static VFS createVFS() {
       // Try the user implementations first, then the built-ins
       List<Class<? extends VFS>> impls = new ArrayList<>();
       impls.addAll(USER_IMPLEMENTATIONS);
       impls.addAll(Arrays.asList((Class<? extends VFS>[]) IMPLEMENTATIONS));
 
       // Try each implementation class until a valid one is found
       VFS vfs = null;
       for (int i = 0; vfs == null || !vfs.isValid(); i++) {
         Class<? extends VFS> impl = impls.get(i);
         try {
           vfs = impl.getDeclaredConstructor().newInstance();
           if (!vfs.isValid() && log.isDebugEnabled()) {
             log.debug("VFS implementation " + impl.getName()
                 + " is not valid in this environment.");
           }
         } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
           log.error("Failed to instantiate " + impl, e);
           return null;
         }
       }
 
       if (log.isDebugEnabled()) {
         log.debug("Using VFS adapter " + vfs.getClass().getName());
       }
 
       return vfs;
     }
   }
 
   /**
    * Get the singleton {@link VFS} instance. If no {@link VFS} implementation can be found for the current environment,
    * then this method returns null.
    *
    * @return single instance of VFS
    */
   public static VFS getInstance() {
     return VFSHolder.INSTANCE;
   }
 
 
/** The specified class is added to the list of VFS implementations. */
 public static void addImplClass(Class<? extends VFS> clazz){
    USER_IMPLEMENTATIONS.add(clazz);    
 }

 

}