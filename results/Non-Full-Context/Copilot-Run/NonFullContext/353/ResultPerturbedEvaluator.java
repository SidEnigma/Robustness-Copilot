package io.dropwizard.metrics5.health;
 
 import io.dropwizard.metrics5.health.annotation.Async;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.NoSuchElementException;
 import java.util.SortedMap;
 import java.util.SortedSet;
 import java.util.TreeMap;
 import java.util.TreeSet;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ConcurrentMap;
 import java.util.concurrent.CopyOnWriteArrayList;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Future;
 import java.util.concurrent.ScheduledExecutorService;
 import java.util.concurrent.ScheduledThreadPoolExecutor;
 import java.util.concurrent.ThreadFactory;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicInteger;
 
 import static io.dropwizard.metrics5.health.HealthCheck.Result;
 
 /**
  * A registry for health checks.
  */
 public class HealthCheckRegistry {
     private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckRegistry.class);
     private static final int ASYNC_EXECUTOR_POOL_SIZE = 2;
 
     private final ConcurrentMap<String, HealthCheck> healthChecks;
     private final List<HealthCheckRegistryListener> listeners;
     private final ScheduledExecutorService asyncExecutorService;
     private final Object lock = new Object();
 
     /**
      * Creates a new {@link HealthCheckRegistry}.
      */
     public HealthCheckRegistry() {
         this(ASYNC_EXECUTOR_POOL_SIZE);
     }
 
     /**
      * Creates a new {@link HealthCheckRegistry}.
      *
      * @param asyncExecutorPoolSize core pool size for async health check executions
      */
     public HealthCheckRegistry(int asyncExecutorPoolSize) {
         this(createExecutorService(asyncExecutorPoolSize));
     }
 
     /**
      * Creates a new {@link HealthCheckRegistry}.
      *
      * @param asyncExecutorService executor service for async health check executions
      */
     public HealthCheckRegistry(ScheduledExecutorService asyncExecutorService) {
         this.healthChecks = new ConcurrentHashMap<>();
         this.listeners = new CopyOnWriteArrayList<>();
         this.asyncExecutorService = asyncExecutorService;
     }
 
     /**
      * Adds a {@link HealthCheckRegistryListener} to a collection of listeners that will be notified on health check
      * registration. Listeners will be notified in the order in which they are added. The listener will be notified of all
      * existing health checks when it first registers.
      *
      * @param listener listener to add
      */
     public void addListener(HealthCheckRegistryListener listener) {
         listeners.add(listener);
         for (Map.Entry<String, HealthCheck> entry : healthChecks.entrySet()) {
             listener.onHealthCheckAdded(entry.getKey(), entry.getValue());
         }
     }
 
     /**
      * Removes a {@link HealthCheckRegistryListener} from this registry's collection of listeners.
      *
      * @param listener listener to remove
      */
     public void removeListener(HealthCheckRegistryListener listener) {
         listeners.remove(listener);
     }
 
     /**
      * Registers an application {@link HealthCheck}.
      *
      * @param name        the name of the health check
      * @param healthCheck the {@link HealthCheck} instance
      */
     public void register(String name, HealthCheck healthCheck) {
         HealthCheck registered;
         synchronized (lock) {
             if (healthChecks.containsKey(name)) {
                 throw new IllegalArgumentException("A health check named " + name + " already exists");
             }
             registered = healthCheck;
             if (healthCheck.getClass().isAnnotationPresent(Async.class)) {
                 registered = new AsyncHealthCheckDecorator(healthCheck, asyncExecutorService);
             }
             healthChecks.put(name, registered);
         }
         onHealthCheckAdded(name, registered);
     }
 
 
/** Given a name, unregisters the application HealthCheck that has that name. */
 public void unregister(String name){
        HealthCheck registered;
        synchronized (lock) {
            registered = healthChecks.remove(name);
        }
        if (registered != null) {
            onHealthCheckRemoved(name, registered);
        }
    }
    
        /**
        * Unregisters all health checks.
        */
        public void unregisterAll() {
            synchronized (lock) {
                for (Map.Entry<String, HealthCheck> entry : healthChecks.entrySet()) {
                    onHealthCheckRemoved(entry.getKey(), entry.getValue());
                }
                healthChecks.clear();
            }
        }
    
        /**
        * Returns the {@link HealthCheck} with the given name.
        *
        * @param name the name of the health check
        * @return the health check
        * @throws NoSuchElementException if no health check with the given name exists
        */
        public HealthCheck getHealthCheck(String name) {
            return healthChecks.get(name);
        }
    
        /**
        * Returns the names of all registered health checks.
        *
        * @return the names of all registered health checks
        */
        public SortedSet<String> getNames() {
            return new TreeSet<>(healthChecks.keySet());
        }
    
        /**
        * Returns the names of all registered health checks.
        *
        * @return the names of all registered health checks
        */
        public List<HealthCheck> getHealthChecks() {
            return new CopyOnWriteArrayList<>(healthChecks.values());
        }
    
        /**
        * Returns the names of all registered health checks.
        *
        * @return the names of all registered health checks
        */
        public SortedMap<String, HealthCheck> getHealthChecksMap() {
            return new TreeMap<>(healthChecks);
        }
    
        /**
        * Returns the names of all registered health checks.
        *
        * @return the names of all registered health checks
        */
        public SortedMap<String, HealthCheck> getHealthChecksMapSorted() {
            return      
 }

 

}