package dmg.util.logback;
 
 import static com.google.common.base.Preconditions.checkArgument;
 import static java.util.Objects.requireNonNull;
 
 import ch.qos.logback.classic.Level;
 import com.google.common.base.Throwables;
 import com.google.common.cache.CacheBuilder;
 import com.google.common.cache.CacheLoader;
 import com.google.common.cache.LoadingCache;
 import com.google.common.collect.HashBasedTable;
 import com.google.common.collect.Lists;
 import com.google.common.collect.Maps;
 import com.google.common.collect.Sets;
 import com.google.common.collect.Table;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Optional;
 import java.util.Set;
 import java.util.concurrent.ExecutionException;
 import org.slf4j.Logger;
 
 /**
  * This class maintains a set of filter thresholds. Two types of inheritance are supported:
  * <p>
  * - Inheritance from a parent FilterThresholds - Inheritance from parent loggers
  * <p>
  * If a threshold is not defined for a given combination of logger and appender, then the parent
  * FilterThresholds is consulted recursively. If not defined in the parent, the threshold of the
  * parent logger is used recursively.
  * <p>
  * The thus calculated effective log level is cached. The cache is invalidated if the set of
  * thresholds is modified, but it is not invalidated if the thresholds in the parent set are
  * modified. Thus once cached, only newly created cells will inherit the thresholds from its parent
  * cell (this is mostly to simplify the design).
  */
 public class FilterThresholdSet {
 
     private final FilterThresholdSet _parent;
 
     private final Set<String> _appenders = Sets.newHashSet();
 
     private final Set<LoggerName> _roots = new HashSet<>();
 
     /* Logger x Appender -> Level */
     private final Table<LoggerName, String, Level> _rules = HashBasedTable.create();
 
     /* Logger -> (Appender -> Level) */
     private final LoadingCache<String, Map<String, Level>> _effectiveMaps =
           CacheBuilder.newBuilder().build(CacheLoader.from(
                 logger -> computeEffectiveMap(LoggerName.getInstance(logger))));
 
     /* Logger -> Level */
     private final LoadingCache<Logger, Optional<Level>> _effectiveLevels =
           CacheBuilder.newBuilder().build(CacheLoader.from(
                 logger -> {
                     try {
                         Map<String, Level> map = _effectiveMaps.get(logger.getName());
                         return map.isEmpty()
                               ? Optional.empty()
                               : Optional.of(Collections.min(map.values(), LEVEL_ORDER));
                     } catch (ExecutionException e) {
                         Throwables.throwIfUnchecked(e.getCause());
                         throw new RuntimeException(e.getCause());
                     }
                 }));
 
     private static final Comparator<Level> LEVEL_ORDER =
           (o1, o2) -> Integer.compare(o1.toInt(), o2.toInt());
 
     public FilterThresholdSet() {
         this(null);
     }
 
     public FilterThresholdSet(FilterThresholdSet parent) {
         _parent = parent;
     }
 
     /**
      * Adds an appender, which will become available for threshold definitions.
      */
     public synchronized void addAppender(String name) {
         requireNonNull(name);
         _appenders.add(name);
     }
 
     /**
      * Returns the list of appenders available for threshold definitions. This is the union of the
      * appenders of the parents thresholds and the appenders of these thresholds.
      */
     public synchronized Collection<String> getAppenders() {
         if (_parent == null) {
             return Lists.newArrayList(_appenders);
         } else {
             Collection<String> appenders = _parent.getAppenders();
             appenders.addAll(_appenders);
             return appenders;
         }
     }
 
 
/** Indicates whether the appendix is valid and valid for use in a threshold definition. */
 public synchronized boolean hasAppender(String appender){
        return _appenders.contains(appender);
    }
    
        /**
        * Adds a logger, which will become available for threshold definitions.
        */
        public synchronized void addRoot(String logger) {
            requireNonNull(logger);
            _roots.add(LoggerName.getInstance(logger));
        }
    
        /**
        * Returns the list of loggers available for threshold definitions. This is the union of the
        * loggers of the parents thresholds and the loggers of these thresholds.
        */
        public synchronized Collection<String> getRoots() {
            if (_parent == null) {
                return Lists.newArrayList(_roots.stream().map(LoggerName::getName).collect(Collectors.toList()));
            } else {
                Collection<String> roots = _parent.getRoots();
                roots.addAll(_roots.stream().map(LoggerName::getName).collect(Collectors.toList()));
                return roots;
            }
        }
    
        /**
        * Adds a threshold rule.
        * <p>
        * The rule is added to the set of rules. If the rule is already defined, it is overwritten.
        * <p>
        * The rule is also added to the cache of effective rules.
        */
        public synchronized void addRule(String logger, String appender, Level level) {
            requireNonNull(logger);
            requireNonNull(appender);
            requireNonNull(level);
            _rules.put(LoggerName.getInstance(logger), appender, level);
            _effectiveMaps.invalidate(logger);
        }
    
        /**
        * Adds a threshold rule.
        * <p>
        * The rule is added to the set of rules. If the rule is already defined, it is overwritten.
        * <p>
        * The rule is also added to the cache of effective rules.
        */
        public synchronized void addRule(String logger, String appender, String level) {
            requireNonNull(logger);
            requireNonNull(appender);
            requireNonNull(level        
 }

 

}