package io.dropwizard.metrics5;
 
 import java.util.Collections;
 import java.util.List;
 import java.util.Map;
 import java.util.SortedMap;
 import java.util.SortedSet;
 import java.util.TreeMap;
 import java.util.TreeSet;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ConcurrentMap;
 import java.util.concurrent.CopyOnWriteArrayList;
 
 /**
  * A registry of metric instances.
  */
 public class MetricRegistry implements MetricSet {
 
     /**
      * Concatenates elements to form a dotted name, eliding any null values or empty strings.
      *
      * @param name  the first element of the name
      * @param names the remaining elements of the name
      * @return {@code name} and {@code names} concatenated by periods
      */
     public static MetricName name(String name, String... names) {
         if (names == null) {
             return MetricName.build(name);
         }
 
         final String[] parts = new String[names.length + 1];
         parts[0] = name;
         System.arraycopy(names, 0, parts, 1, names.length);
         return MetricName.build(parts);
     }
 
     /**
      * Concatenates a class name and elements to form a dotted name, eliding any null values or
      * empty strings.
      *
      * @param klass the first element of the name
      * @param names the remaining elements of the name
      * @return {@code klass} and {@code names} concatenated by periods
      */
     public static MetricName name(Class<?> klass, String... names) {
         return name(klass.getName(), names);
     }
 
     private final ConcurrentMap<MetricName, Metric> metrics;
     private final List<MetricRegistryListener> listeners;
 
     /**
      * Creates a new {@link MetricRegistry}.
      */
     public MetricRegistry() {
         this.metrics = buildMap();
         this.listeners = new CopyOnWriteArrayList<>();
     }
 
     /**
      * Creates a new {@link ConcurrentMap} implementation for use inside the registry. Override this
      * to create a {@link MetricRegistry} with space- or time-bounded metric lifecycles, for
      * example.
      *
      * @return a new {@link ConcurrentMap}
      */
     protected ConcurrentMap<MetricName, Metric> buildMap() {
         return new ConcurrentHashMap<>();
     }
 
     /**
      * See {@link #register(MetricName, Metric)}
      */
     public <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {
         return register(MetricName.build(name), metric);
     }
 
     /**
      * Given a {@link Metric}, registers it under the given name.
      *
      * @param name   the name of the metric
      * @param metric the metric
      * @param <T>    the type of the metric
      * @return {@code metric}
      * @throws IllegalArgumentException if the name is already registered or metric variable is null
      */
     @SuppressWarnings("unchecked")
     public <T extends Metric> T register(MetricName name, T metric) throws IllegalArgumentException {
         if (metric == null) {
             throw new NullPointerException("metric == null");
         }
 
         if (metric instanceof MetricRegistry) {
             final MetricRegistry childRegistry = (MetricRegistry)metric;
             final MetricName childName = name;
             childRegistry.addListener(new MetricRegistryListener() {
                 @Override
                 public void onGaugeAdded(MetricName name, Gauge<?> gauge) {
                     register(childName.append(name), gauge);
                 }
 
                 @Override
                 public void onGaugeRemoved(MetricName name) {
                     remove(childName.append(name));
                 }
 
                 @Override
                 public void onCounterAdded(MetricName name, Counter counter) {
                     register(childName.append(name), counter);
                 }
 
                 @Override
                 public void onCounterRemoved(MetricName name) {
                     remove(childName.append(name));
                 }
 
                 @Override
                 public void onHistogramAdded(MetricName name, Histogram histogram) {
                     register(childName.append(name), histogram);
                 }
 
                 @Override
                 public void onHistogramRemoved(MetricName name) {
                     remove(childName.append(name));
                 }
 
                 @Override
                 public void onMeterAdded(MetricName name, Meter meter) {
                     register(childName.append(name), meter);
                 }
 
                 @Override
                 public void onMeterRemoved(MetricName name) {
                     remove(childName.append(name));
                 }
 
                 @Override
                 public void onTimerAdded(MetricName name, Timer timer) {
                     register(childName.append(name), timer);
                 }
 
                 @Override
                 public void onTimerRemoved(MetricName name) {
                     remove(childName.append(name));
                 }
             });
         } else if (metric instanceof MetricSet) {
             registerAll(name, (MetricSet) metric);
         } else {
             final Metric existing = metrics.putIfAbsent(name, metric);
             if (existing == null) {
                 onMetricAdded(name, metric);
             } else {
                 throw new IllegalArgumentException("A metric named " + name + " already exists");
             }
         }
         return metric;
     }
 
     /**
      * Given a metric set, registers them.
      *
      * @param metrics a set of metrics
      * @throws IllegalArgumentException if any of the names are already registered
      */
     public void registerAll(MetricSet metrics) throws IllegalArgumentException {
         registerAll(null, metrics);
     }
 
     /**
      * See {@link #counter(MetricName)}
      */
     public Counter counter(String name) {
         return getOrAdd(MetricName.build(name), MetricBuilder.COUNTERS);
     }
 
     /**
      * Return the {@link Counter} registered under this name; or create and register
      * a new {@link Counter} if none is registered.
      *
      * @param name the name of the metric
      * @return a new or pre-existing {@link Counter}
      */
     public Counter counter(MetricName name) {
         return getOrAdd(name, MetricBuilder.COUNTERS);
     }
 
     /**
      * See {@link #histogram(MetricName)}
      */
     public Histogram histogram(String name) {
         return getOrAdd(MetricName.build(name), MetricBuilder.HISTOGRAMS);
     }
 
     /**
      * Return the {@link Counter} registered under this name; or create and register
      * a new {@link Counter} using the provided MetricSupplier if none is registered.
      *
      * @param name     the name of the metric
      * @param supplier a MetricSupplier that can be used to manufacture a counter.
      * @return a new or pre-existing {@link Counter}
      */
     public <T extends Counter> T counter(MetricName name, final MetricSupplier<T> supplier) {
         return getOrAdd(name, new MetricBuilder<T>() {
             @Override
             public T newMetric() {
                 return supplier.newMetric();
             }
 
             @Override
             public boolean isInstance(Metric metric) {
                 return Counter.class.isInstance(metric);
             }
         });
     }
 
     /**
      * Return the {@link Histogram} registered under this name; or create and register
      * a new {@link Histogram} if none is registered.
      *
      * @param name the name of the metric
      * @return a new or pre-existing {@link Histogram}
      */
     public Histogram histogram(MetricName name) {
         return getOrAdd(name, MetricBuilder.HISTOGRAMS);
     }
 
     /**
      * See {@link #meter(MetricName)}
      */
     public Meter meter(String name) {
         return getOrAdd(MetricName.build(name), MetricBuilder.METERS);
     }
 
     /**
      * Return the {@link Histogram} registered under this name; or create and register
      * a new {@link Histogram} using the provided MetricSupplier if none is registered.
      *
      * @param name     the name of the metric
      * @param supplier a MetricSupplier that can be used to manufacture a histogram
      * @return a new or pre-existing {@link Histogram}
      */
     public Histogram histogram(MetricName name, final MetricSupplier<Histogram> supplier) {
         return getOrAdd(name, new MetricBuilder<Histogram>() {
             @Override
             public Histogram newMetric() {
                 return supplier.newMetric();
             }
 
             @Override
             public boolean isInstance(Metric metric) {
                 return Histogram.class.isInstance(metric);
             }
         });
     }
 
     /**
      * Return the {@link Meter} registered under this name; or create and register
      * a new {@link Meter} if none is registered.
      *
      * @param name the name of the metric
      * @return a new or pre-existing {@link Meter}
      */
     public Meter meter(MetricName name) {
         return getOrAdd(name, MetricBuilder.METERS);
     }
 
     /**
      * See {@link #timer(MetricName)}
      */
     public Timer timer(String name) {
         return getOrAdd(MetricName.build(name), MetricBuilder.TIMERS);
     }
 
     /**
      * Return the {@link Meter} registered under this name; or create and register
      * a new {@link Meter} using the provided MetricSupplier if none is registered.
      *
      * @param name     the name of the metric
      * @param supplier a MetricSupplier that can be used to manufacture a Meter
      * @return a new or pre-existing {@link Meter}
      */
     public Meter meter(MetricName name, final MetricSupplier<Meter> supplier) {
         return getOrAdd(name, new MetricBuilder<Meter>() {
             @Override
             public Meter newMetric() {
                 return supplier.newMetric();
             }
 
             @Override
             public boolean isInstance(Metric metric) {
                 return Meter.class.isInstance(metric);
             }
         });
     }
 
     /**
      * Return the {@link Timer} registered under this name; or create and register
      * a new {@link Timer} if none is registered.
      *
      * @param name the name of the metric
      * @return a new or pre-existing {@link Timer}
      */
     public Timer timer(MetricName name) {
         return getOrAdd(name, MetricBuilder.TIMERS);
     }
 
     /**
      * Return the {@link Timer} registered under this name; or create and register
      * a new {@link Timer} using the provided MetricSupplier if none is registered.
      *
      * @param name     the name of the metric
      * @param supplier a MetricSupplier that can be used to manufacture a Timer
      * @return a new or pre-existing {@link Timer}
      */
     public Timer timer(MetricName name, final MetricSupplier<Timer> supplier) {
         return getOrAdd(name, new MetricBuilder<Timer>() {
             @Override
             public Timer newMetric() {
                 return supplier.newMetric();
             }
 
             @Override
             public boolean isInstance(Metric metric) {
                 return Timer.class.isInstance(metric);
             }
         });
     }
 
     /**
      * Return the {@link Gauge} registered under this name; or create and register
      * a new {@link SettableGauge} if none is registered.
      *
      * @param name the name of the metric
      * @return a pre-existing {@link Gauge} or a new {@link SettableGauge}
      * @since 4.2
      */
     @SuppressWarnings({"rawtypes", "unchecked"})
     public <T extends Gauge> T gauge(MetricName name) {
         return (T) getOrAdd(name, MetricBuilder.GAUGES);
     }
 
     /**
      * Return the {@link Gauge} registered under this name; or create and register
      * a new {@link Gauge} using the provided MetricSupplier if none is registered.
      *
      * @param name     the name of the metric
      * @param supplier a MetricSupplier that can be used to manufacture a Gauge
      * @return a new or pre-existing {@link Gauge}
      */
     @SuppressWarnings("rawtypes")
     public <T extends Gauge> T gauge(MetricName name, final MetricSupplier<T> supplier) {
         return getOrAdd(name, new MetricBuilder<T>() {
             @Override
             public T newMetric() {
                 return supplier.newMetric();
             }
 
             @Override
             public boolean isInstance(Metric metric) {
                 return Gauge.class.isInstance(metric);
             }
         });
     }
 
 
     /**
      * Removes the metric with the given name.
      *
      * @param name the name of the metric
      * @return whether or not the metric was removed
      */
     public boolean remove(MetricName name) {
         final Metric metric = metrics.remove(name);
         if (metric != null) {
             onMetricRemoved(name, metric);
             return true;
         }
         return false;
     }
 
     /**
      * Removes all metrics which match the given filter.
      *
      * @param filter a filter
      */
     public void removeMatching(MetricFilter filter) {
         for (Map.Entry<MetricName, Metric> entry : metrics.entrySet()) {
             if (filter.matches(entry.getKey(), entry.getValue())) {
                 remove(entry.getKey());
             }
         }
     }
 
 
/** Adds a {@link MetricRegistryListener} to a collection of listeners that will be notified when the metric is created. */
 public void addListener(MetricRegistryListener listener){}

 

}