package org.dcache.util;
 
 import static com.google.common.base.Preconditions.checkArgument;
 import static java.util.Objects.requireNonNull;
 import static java.util.concurrent.TimeUnit.DAYS;
 import static java.util.concurrent.TimeUnit.HOURS;
 import static java.util.concurrent.TimeUnit.MICROSECONDS;
 import static java.util.concurrent.TimeUnit.MILLISECONDS;
 import static java.util.concurrent.TimeUnit.MINUTES;
 import static java.util.concurrent.TimeUnit.NANOSECONDS;
 import static java.util.concurrent.TimeUnit.SECONDS;
 import static org.dcache.util.Strings.toThreeSigFig;
 
 import com.google.common.collect.ImmutableMap;
 import java.text.SimpleDateFormat;
 import java.time.Duration;
 import java.time.Instant;
 import java.time.temporal.ChronoUnit;
 import java.util.Arrays;
 import java.util.Comparator;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Optional;
 import java.util.Properties;
 import java.util.Set;
 import java.util.concurrent.TimeUnit;
 import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
 
 /**
  * Utility classes for dealing with time and durations, mostly with pretty- printing them for human
  * consumption.
  */
 public class TimeUtils {
 
     public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd' 'HH:mm:ss.SSS";
 
     /**
      * <p>Compares time units such that the larger unit is
      * ordered before the smaller.</p>
      */
     public static class DecreasingTimeUnitComparator
           implements Comparator<TimeUnit> {
 
         @Override
         public int compare(TimeUnit unit1, TimeUnit unit2) {
             if (unit1 == null) {
                 return -1;
             }
 
             if (unit2 == null) {
                 return 1;
             }
 
             long nanos1 = unit1.toNanos(1);
             long nanos2 = unit2.toNanos(1);
 
             if (nanos1 == nanos2) {
                 return 0;
             }
 
             if (nanos1 < nanos2) {
                 return 1;
             }
 
             return -1;
         }
     }
 
     /**
      * <p>Parses a duration of a given dimension into other dimensions.</p>
      *
      * <p>The parsed out dimensions are held in an internal map.</p>
      *
      * <p>There can be gaps between the various time dimensions, but it
      * is understood that successive calls to parse should be strictly decreasing. Calls to compute
      * a larger unit than the current unit will throw an exception.</p>
      *
      * <p>The dimensions can be recomputed by calling clear(), but
      * the original duration given to the parser is fixed.</p>
      */
     public static class DurationParser {
 
         private final Map<TimeUnit, Long> durations;
         private final Long duration;
         private final TimeUnit durationUnit;
 
         private TimeUnit current;
         private long remainder;
 
         public DurationParser(Long duration, TimeUnit durationUnit) {
             this.duration = requireNonNull(duration,
                   "duration was null");
             this.durationUnit = requireNonNull(durationUnit,
                   "durationUnit was null");
             durations = new HashMap<>();
             remainder = durationUnit.toNanos(duration);
         }
 
         public DurationParser parseAll() {
             parse(TimeUnit.DAYS);
             parse(TimeUnit.HOURS);
             parse(TimeUnit.MINUTES);
             parse(TimeUnit.SECONDS);
             parse(TimeUnit.MILLISECONDS);
             parse(TimeUnit.MICROSECONDS);
             parse(TimeUnit.NANOSECONDS);
             return this;
         }
 
         public void parse(TimeUnit unit) throws IllegalStateException {
             checkStrictlyDecreasing(unit);
             long durationForUnit = unit.convert(remainder, TimeUnit.NANOSECONDS);
             long durationInNanos = unit.toNanos(durationForUnit);
             remainder = remainder - durationInNanos;
             durations.put(unit, durationForUnit);
             current = unit;
         }
 
         public void clear() {
             durations.clear();
             current = null;
             remainder = durationUnit.toNanos(duration);
         }
 
         public long get(TimeUnit unit) {
             Long duration = durations.get(unit);
             return duration == null ? 0L : duration;
         }
 
         private void checkStrictlyDecreasing(TimeUnit next)
               throws IllegalStateException {
             /*
              * Because this is a decreasing order comparator,
              * it returns -1 when the first element is larger than the
              * second.
              */
             if (comparator.compare(next, current) <= 0) {
                 throw new IllegalStateException(next + " is not strictly "
                       + "smaller than " + current);
             }
         }
     }
 
     /**
      * <p>Returns a given duration broken down into constituent units of all
      * dimensions and formatted according to the duration format string.</p>
      *
      * <p>The format markers are:</p>
      *
      * <table>
      *     <tr><td>%D</td><td>days</td></tr>
      *     <tr><td>%H</td><td>hours (no leading zero)</td></tr>
      *     <tr><td>%HH</td><td>hours with one leading zero</td></tr>
      *     <tr><td>%m</td><td>minutes (no leading zero)</td></tr>
      *     <tr><td>%mm</td><td>minutes with one leading zero</td></tr>
      *     <tr><td>%s</td><td>seconds (no leading zero)</td></tr>
      *     <tr><td>%ss</td><td>seconds with one leading zero</td></tr>
      *     <tr><td>%S</td><td>milliseconds</td></tr>
      *     <tr><td>%N</td><td>milliseconds+nanoseconds</td></tr>
      * </table>
      *
      * <p>Note that '%' is used here as a reserved symbol as in String formatting.
      *    %s, however, has a different meaning.  This formatting string should
      *    not contain any other placeholders than the ones above and cannot
      *    be combined with normal string formatting.</p>
      */
     public static class DurationFormatter {
 
         private final String format;
         private DurationParser durations;
 
         public DurationFormatter(String format) {
             requireNonNull(format,
                   "Format string must be specified.");
             this.format = format;
         }
 
         public String format(long duration, TimeUnit unit) {
             requireNonNull(unit,
                   "Duration time unit must be specified.");
             TimeUnit[] sortedDimensions = getSortedDimensions();
             durations = new DurationParser(duration, unit);
             for (TimeUnit dimension : sortedDimensions) {
                 durations.parse(dimension);
             }
             StringBuilder builder = new StringBuilder();
             replace(builder);
             return builder.toString();
         }
 
         private TimeUnit[] getSortedDimensions() {
             Set<TimeUnit> units = new HashSet<>();
             char[] sequence = format.toCharArray();
             for (int c = 0; c < sequence.length; c++) {
                 switch (sequence[c]) {
                     case '%':
                         ++c;
                         switch (sequence[c]) {
                             case 'D':
                                 units.add(TimeUnit.DAYS);
                                 break;
                             case 'H':
                                 units.add(TimeUnit.HOURS);
                                 break;
                             case 'm':
                                 units.add(TimeUnit.MINUTES);
                                 break;
                             case 's':
                                 units.add(TimeUnit.SECONDS);
                                 break;
                             case 'S':
                                 units.add(TimeUnit.MILLISECONDS);
                                 break;
                             case 'N':
                                 units.add(TimeUnit.MILLISECONDS);
                                 units.add(TimeUnit.MICROSECONDS);
                                 units.add(TimeUnit.NANOSECONDS);
                                 break;
                             default:
                                 throw new IllegalArgumentException(
                                       "No such formatting symbol " + c);
                         }
                         break;
                     default:
                 }
             }
 
             TimeUnit[] sorted = units.toArray(TimeUnit[]::new);
             Arrays.sort(sorted, comparator);
             return sorted;
         }
 
         private void replace(StringBuilder builder) {
             char[] sequence = format.toCharArray();
             for (int c = 0; c < sequence.length; c++) {
                 switch (sequence[c]) {
                     case '%':
                         c = handlePlaceholder(++c,
                               sequence,
                               builder);
                         break;
                     default:
                         builder.append(sequence[c]);
                 }
             }
         }
 
         private int handlePlaceholder(int c, char[] sequence, StringBuilder builder) {
             switch (sequence[c]) {
                 case 'D':
                     builder.append(durations.get(TimeUnit.DAYS));
                     break;
                 case 'H':
                     if (sequence[c + 1] == 'H') {
                         ++c;
                         builder.append(leadingZero(durations.get(TimeUnit.HOURS)));
                     } else {
                         builder.append(durations.get(TimeUnit.HOURS));
                     }
                     break;
                 case 'm':
                     if (sequence[c + 1] == 'm') {
                         ++c;
                         builder.append(leadingZero(durations.get(TimeUnit.MINUTES)));
                     } else {
                         builder.append(durations.get(TimeUnit.MINUTES));
                     }
                     break;
                 case 's':
                     if (sequence[c + 1] == 's') {
                         ++c;
                         builder.append(leadingZero(durations.get(TimeUnit.SECONDS)));
                     } else {
                         builder.append(durations.get(TimeUnit.SECONDS));
                     }
                     break;
                 case 'S':
                     builder.append(durations.get(TimeUnit.MILLISECONDS));
                     break;
                 case 'N':
                     builder.append(durations.get(TimeUnit.MILLISECONDS))
                           .append(durations.get(TimeUnit.MICROSECONDS))
                           .append(durations.get(TimeUnit.NANOSECONDS));
                     break;
                 default:
                     throw new IllegalArgumentException
                           ("No such formatting symbol " + c);
             }
 
             return c;
         }
 
         private String leadingZero(Long value) {
             String valueString = String.valueOf(value);
             if (valueString.length() < 2) {
                 return '0' + valueString;
             }
             return valueString;
         }
     }
 
     public enum TimeUnitFormat {
         /**
          * Display time-units in a short format.
          */
         SHORT,
 
         /**
          * Display time-units as whole words.
          */
         LONG
     }
 
     private static final ImmutableMap<TimeUnit, String> SHORT_TIMEUNIT_NAMES =
           ImmutableMap.<TimeUnit, String>builder().
                 put(NANOSECONDS, "ns").
                 put(MICROSECONDS, "\u00B5s"). // U+00B5 is Unicode for microsymbol
                 put(MILLISECONDS, "ms").
                 put(SECONDS, "s").
                 put(MINUTES, "min").
                 put(HOURS, "hours").
                 put(DAYS, "days").
                 build();
 
     private static final ImmutableMap<TimeUnit, String> LONG_TIMEUNIT_NAMES =
           ImmutableMap.<TimeUnit, String>builder().
                 put(NANOSECONDS, "nanoseconds").
                 put(MICROSECONDS, "microseconds").
                 put(MILLISECONDS, "milliseconds").
                 put(SECONDS, "seconds").
                 put(MINUTES, "minutes").
                 put(HOURS, "hours").
                 put(DAYS, "days").
                 build();
 
     private static final DecreasingTimeUnitComparator comparator
           = new DecreasingTimeUnitComparator();
 
     private TimeUtils() {
         // Prevent instantiation.
     }
 
     public static CharSequence duration(long duration, TimeUnit units, TimeUnitFormat unitFormat) {
         return appendDuration(new StringBuilder(), duration, units, unitFormat);
     }
 
     /**
      * @param duration to be expressed
      * @param unit     of the duration
      * @param format   as specified above.
      * @return formatted string
      * @see DurationFormatter
      */
     public static String getFormattedDuration(long duration,
           TimeUnit unit,
           String format) {
         return new DurationFormatter(format).format(duration, unit);
     }
 
     /**
      * Returns short sting form for given {@ link TimeUnit}.
      */
     public static String unitStringOf(TimeUnit unit) {
         return SHORT_TIMEUNIT_NAMES.get(unit);
     }
 
 
/** Return a compact human understandable string describing the supplied duration */

public static StringBuilder appendDuration(StringBuilder sb, Duration duration, TimeUnitFormat unitFormat) {
    long durationInNanos = duration.toNanos();
    TimeUnit unit = chooseTimeUnit(durationInNanos);
    long durationInUnit = unit.convert(durationInNanos, TimeUnit.NANOSECONDS);
    String unitString = getUnitString(unit, unitFormat);
    
    sb.append(durationInUnit).append(" ").append(unitString);
    return sb;
}
 

}