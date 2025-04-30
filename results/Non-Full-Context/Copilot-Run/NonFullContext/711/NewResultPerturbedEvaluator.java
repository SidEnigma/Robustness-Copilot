package io.dropwizard.metrics5;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.io.Closeable;
 import java.util.Collections;
 import java.util.Locale;
 import java.util.Set;
 import java.util.SortedMap;
 import java.util.concurrent.Executors;
 import java.util.concurrent.ScheduledExecutorService;
 import java.util.concurrent.ScheduledFuture;
 import java.util.concurrent.ThreadFactory;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicInteger;
 
 /**
  * The abstract base class for all scheduled reporters (i.e., reporters which process a registry's
  * metrics periodically).
  *
  * @see ConsoleReporter
  * @see CsvReporter
  * @see Slf4jReporter
  */
 public abstract class ScheduledReporter implements Closeable, Reporter {
 
     private static final Logger LOG = LoggerFactory.getLogger(ScheduledReporter.class);
 
     /**
      * A simple named thread factory.
      */
     @SuppressWarnings("NullableProblems")
     private static class NamedThreadFactory implements ThreadFactory {
         private final ThreadGroup group;
         private final AtomicInteger threadNumber = new AtomicInteger(1);
         private final String namePrefix;
 
         private NamedThreadFactory(String name) {
             final SecurityManager s = System.getSecurityManager();
             this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
             this.namePrefix = "metrics-" + name + "-thread-";
         }
 
         @Override
         public Thread newThread(Runnable r) {
             final Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
             t.setDaemon(true);
             if (t.getPriority() != Thread.NORM_PRIORITY) {
                 t.setPriority(Thread.NORM_PRIORITY);
             }
             return t;
         }
     }
 
     private static final AtomicInteger FACTORY_ID = new AtomicInteger();
 
     private final MetricRegistry registry;
     private final ScheduledExecutorService executor;
     private final boolean shutdownExecutorOnStop;
     private final Set<MetricAttribute> disabledMetricAttributes;
     private ScheduledFuture<?> scheduledFuture;
     private final MetricFilter filter;
     private final long durationFactor;
     private final String durationUnit;
     private final long rateFactor;
     private final String rateUnit;
 
     /**
      * Creates a new {@link ScheduledReporter} instance.
      *
      * @param registry     the {@link io.dropwizard.metrics5.MetricRegistry} containing the metrics this
      *                     reporter will report
      * @param name         the reporter's name
      * @param filter       the filter for which metrics to report
      * @param rateUnit     a unit of time
      * @param durationUnit a unit of time
      */
     protected ScheduledReporter(MetricRegistry registry,
                                 String name,
                                 MetricFilter filter,
                                 TimeUnit rateUnit,
                                 TimeUnit durationUnit) {
         this(registry, name, filter, rateUnit, durationUnit, createDefaultExecutor(name));
     }
 
     /**
      * Creates a new {@link ScheduledReporter} instance.
      *
      * @param registry the {@link io.dropwizard.metrics5.MetricRegistry} containing the metrics this
      *                 reporter will report
      * @param name     the reporter's name
      * @param filter   the filter for which metrics to report
      * @param executor the executor to use while scheduling reporting of metrics.
      */
     protected ScheduledReporter(MetricRegistry registry,
                                 String name,
                                 MetricFilter filter,
                                 TimeUnit rateUnit,
                                 TimeUnit durationUnit,
                                 ScheduledExecutorService executor) {
         this(registry, name, filter, rateUnit, durationUnit, executor, true);
     }
 
     /**
      * Creates a new {@link ScheduledReporter} instance.
      *
      * @param registry               the {@link io.dropwizard.metrics5.MetricRegistry} containing the metrics this
      *                               reporter will report
      * @param name                   the reporter's name
      * @param filter                 the filter for which metrics to report
      * @param executor               the executor to use while scheduling reporting of metrics.
      * @param shutdownExecutorOnStop if true, then executor will be stopped in same time with this reporter
      */
     protected ScheduledReporter(MetricRegistry registry,
                                 String name,
                                 MetricFilter filter,
                                 TimeUnit rateUnit,
                                 TimeUnit durationUnit,
                                 ScheduledExecutorService executor,
                                 boolean shutdownExecutorOnStop) {
         this(registry, name, filter, rateUnit, durationUnit, executor, shutdownExecutorOnStop, Collections.emptySet());
     }
 
     protected ScheduledReporter(MetricRegistry registry,
                                 String name,
                                 MetricFilter filter,
                                 TimeUnit rateUnit,
                                 TimeUnit durationUnit,
                                 ScheduledExecutorService executor,
                                 boolean shutdownExecutorOnStop,
                                 Set<MetricAttribute> disabledMetricAttributes) {
 
         if (registry == null) {
             throw new NullPointerException("registry == null");
         }
 
         this.registry = registry;
         this.filter = filter;
         this.executor = executor == null ? createDefaultExecutor(name) : executor;
         this.shutdownExecutorOnStop = shutdownExecutorOnStop;
         this.rateFactor = rateUnit.toSeconds(1);
         this.rateUnit = calculateRateUnit(rateUnit);
         this.durationFactor = durationUnit.toNanos(1);
         this.durationUnit = durationUnit.toString().toLowerCase(Locale.US);
         this.disabledMetricAttributes = disabledMetricAttributes != null ? disabledMetricAttributes :
                 Collections.emptySet();
     }
 
     /**
      * Starts the reporter polling at the given period.
      *
      * @param period the amount of time between polls
      * @param unit   the unit for {@code period}
      */
     public void start(long period, TimeUnit unit) {
         start(period, period, unit);
     }
 
     /**
      * Starts the reporter polling at the given period with the specific runnable action.
      * Visible only for testing.
      */
     synchronized void start(long initialDelay, long period, TimeUnit unit, Runnable runnable) {
         if (this.scheduledFuture != null) {
             throw new IllegalArgumentException("Reporter already started");
         }
 
         this.scheduledFuture = executor.scheduleWithFixedDelay(runnable, initialDelay, period, unit);
     }
 
     /**
      * Starts the reporter polling at the given period.
      *
      * @param initialDelay the time to delay the first execution
      * @param period       the amount of time between polls
      * @param unit         the unit for {@code period} and {@code initialDelay}
      */
     synchronized public void start(long initialDelay, long period, TimeUnit unit) {
         start(initialDelay, period, unit, () -> {
             try {
                 report();
             } catch (Throwable ex) {
                 LOG.error("Exception thrown from {}#report. Exception was suppressed.", ScheduledReporter.this.getClass().getSimpleName(), ex);
             }
         });
     }
 
     /**
      * Stops the reporter and if shutdownExecutorOnStop is true then shuts down its thread of execution.
      * <p>
      * Uses the shutdown pattern from http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html
      */
     public void stop() {
         if (shutdownExecutorOnStop) {
             executor.shutdown(); // Disable new tasks from being submitted
         }
 
         try {
             report(); // Report metrics one last time
         } catch (Exception e) {
             LOG.warn("Final reporting of metrics failed.", e);
         }
 
         if (shutdownExecutorOnStop) {
             try {
                 // Wait a while for existing tasks to terminate
                 if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                     executor.shutdownNow(); // Cancel currently executing tasks
                     // Wait a while for tasks to respond to being cancelled
                     if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                         LOG.warn("ScheduledExecutorService did not terminate.");
                     }
                 }
             } catch (InterruptedException ie) {
                 // (Re-)Cancel if current thread also interrupted
                 executor.shutdownNow();
                 // Preserve interrupt status
                 Thread.currentThread().interrupt();
             }
         } else {
             // The external manager (like JEE container) responsible for lifecycle of executor
             cancelScheduledFuture();
         }
     }
 
     private synchronized void cancelScheduledFuture() {
         if (this.scheduledFuture == null) {
             // was never started
             return;
         }
         if (this.scheduledFuture.isCancelled()) {
             // already cancelled
             return;
         }
         // just cancel the scheduledFuture and exit
         this.scheduledFuture.cancel(false);
     }
 
     /**
      * Stops the reporter and shuts down its thread of execution.
      */
     @Override
     public void close() {
         stop();
     }
 
 
/** Reports the values of all metrics in the registry */

public void report() {
    SortedMap<String, Gauge> gauges = registry.getGauges(filter);
    SortedMap<String, Counter> counters = registry.getCounters(filter);
    SortedMap<String, Histogram> histograms = registry.getHistograms(filter);
    SortedMap<String, Meter> meters = registry.getMeters(filter);
    SortedMap<String, Timer> timers = registry.getTimers(filter);

    // Report gauges
    for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
        String name = entry.getKey();
        Gauge gauge = entry.getValue();
        reportGauge(name, gauge);
    }

    // Report counters
    for (Map.Entry<String, Counter> entry : counters.entrySet()) {
        String name = entry.getKey();
        Counter counter = entry.getValue();
        reportCounter(name, counter);
    }

    // Report histograms
    for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
        String name = entry.getKey();
        Histogram histogram = entry.getValue();
        reportHistogram(name, histogram);
    }

    // Report meters
    for (Map.Entry<String, Meter> entry : meters.entrySet()) {
        String name = entry.getKey();
        Meter meter = entry.getValue();
        reportMeter(name, meter);
    }

    // Report timers
    for (Map.Entry<String, Timer> entry : timers.entrySet()) {
        String name = entry.getKey();
        Timer timer = entry.getValue();
        reportTimer(name, timer);
    }
}
 

}