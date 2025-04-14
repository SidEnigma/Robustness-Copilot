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
 
 
/** A collection of listeners that will be notified on health check registration has been added. */

public void addListener(HealthCheckRegistryListener listener) {
    listeners.add(listener);
}
 

}