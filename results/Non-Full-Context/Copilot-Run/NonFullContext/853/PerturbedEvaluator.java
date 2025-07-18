/*
  * Copyright 2013-2021 the original author or authors.
  *
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
  */
 package net.logstash.logback.stacktrace;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Deque;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.regex.Pattern;
 
 import net.logstash.logback.CachingAbbreviator;
 import net.logstash.logback.NullAbbreviator;
 
 import ch.qos.logback.access.PatternLayout;
 import ch.qos.logback.classic.pattern.Abbreviator;
 import ch.qos.logback.classic.pattern.TargetLengthBasedClassNameAbbreviator;
 import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
 import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
 import ch.qos.logback.classic.spi.ILoggingEvent;
 import ch.qos.logback.classic.spi.IThrowableProxy;
 import ch.qos.logback.classic.spi.StackTraceElementProxy;
 import ch.qos.logback.classic.spi.ThrowableProxy;
 import ch.qos.logback.classic.spi.ThrowableProxyUtil;
 import ch.qos.logback.core.CoreConstants;
 import ch.qos.logback.core.boolex.EvaluationException;
 import ch.qos.logback.core.boolex.EventEvaluator;
 import ch.qos.logback.core.status.ErrorStatus;
 
 /**
  * A {@link ThrowableHandlingConverter} (similar to logback's {@link ThrowableProxyConverter})
  * that formats stacktraces by doing the following:
  *
  * <ul>
  * <li>Limits the number of stackTraceElements per throwable
  *     (applies to each individual throwable.  e.g. caused-bys and suppressed).
  *     See {@link #maxDepthPerThrowable}.</li>
  * <li>Limits the total length in characters of the trace.
  *     See {@link #maxLength}.</li>
  * <li>Abbreviates class names based on the {@link #shortenedClassNameLength}.
  *     See {@link #shortenedClassNameLength}.</li>
  * <li>Filters out consecutive unwanted stackTraceElements based on regular expressions.
  *     See {@link #excludes}.</li>
  * <li>Uses evaluators to determine if the stacktrace should be logged.
  *     See {@link #evaluators}.</li>
  * <li>Outputs in either 'normal' order (root-cause-last), or root-cause-first.
  *     See {@link #rootCauseFirst}.</li>
  * </ul>
  *
  * To use this with a {@link PatternLayout}, you must configure {@code conversionRule}
  * as described <a href="http://logback.qos.ch/manual/layouts.html#customConversionSpecifier">here</a>.
  * Options can be specified in the pattern in the following order:
  * <ol>
  * <li>maxDepthPerThrowable = "full" or "short" or an integer value</li>
  * <li>shortenedClassNameLength = "full" or "short" or an integer value</li>
  * <li>maxLength = "full" or "short" or an integer value</li>
  * </ol>
  * 
  * If any other remaining options are "rootFirst",
  * then the converter awill be configured as root-cause-first.
  * If any other remaining options equal to an evaluator name,
  * then the evaluator will be used to determine if the stacktrace should be printed.
  * Other options will be interpreted as exclusion regexes.
  * <p>
  * For example,
  * <pre>
  * {@code
  *     <conversionRule conversionWord="stack"
  *                   converterClass="net.logstash.logback.stacktrace.ShortenedThrowableConverter" />
  *
  *     <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
  *         <encoder>
  *             <pattern>[%thread] - %msg%n%stack{5,1024,10,rootFirst,regex1,regex2,evaluatorName}</pattern>
  *         </encoder>
  *     </appender>
  * }
  * </pre>
  */
 public class ShortenedThrowableConverter extends ThrowableHandlingConverter {
 
     public static final int FULL_MAX_DEPTH_PER_THROWABLE = Integer.MAX_VALUE;
     public static final int SHORT_MAX_DEPTH_PER_THROWABLE = 3;
     public static final int DEFAULT_MAX_DEPTH_PER_THROWABLE = FULL_MAX_DEPTH_PER_THROWABLE;
 
     public static final int FULL_MAX_LENGTH = Integer.MAX_VALUE;
     public static final int SHORT_MAX_LENGTH = 1024;
     public static final int DEFAULT_MAX_LENGTH = FULL_MAX_LENGTH;
 
     public static final int FULL_CLASS_NAME_LENGTH = Integer.MAX_VALUE;
     public static final int SHORT_CLASS_NAME_LENGTH = 10;
     public static final int DEFAULT_CLASS_NAME_LENGTH = FULL_CLASS_NAME_LENGTH;
 
     private static final String ELLIPSIS = "...";
     private static final int BUFFER_INITIAL_CAPACITY = 4096;
 
     private static final String OPTION_VALUE_FULL = "full";
     private static final String OPTION_VALUE_SHORT = "short";
     private static final String OPTION_VALUE_ROOT_FIRST = "rootFirst";
     private static final String OPTION_VALUE_INLINE_HASH = "inlineHash";
 
     private static final int OPTION_INDEX_MAX_DEPTH = 0;
     private static final int OPTION_INDEX_SHORTENED_CLASS_NAME = 1;
     private static final int OPTION_INDEX_MAX_LENGTH = 2;
 
     private AtomicInteger errorCount = new AtomicInteger();
 
     /**
      * Maximum number of stackTraceElements printed per throwable.
      */
     private int maxDepthPerThrowable = DEFAULT_MAX_DEPTH_PER_THROWABLE;
 
     /**
      * Maximum number of characters in the entire stacktrace.
      */
     private int maxLength = DEFAULT_MAX_LENGTH;
 
     /**
      * Will try to shorten class name lengths to less than this value
      */
     private int shortenedClassNameLength = DEFAULT_CLASS_NAME_LENGTH;
 
     /**
      * Abbreviator that will shorten the classnames if {@link #shortenedClassNameLength}
      * is set less than {@link #FULL_CLASS_NAME_LENGTH}
      */
     private Abbreviator abbreviator = NullAbbreviator.INSTANCE;
 
     /**
      * Patterns used to determine which stacktrace elements to exclude.
      *
      * The strings being matched against are in the form "fullyQualifiedClassName.methodName"
      * (e.g. "java.lang.Object.toString").
      *
      * Note that these elements will only be excluded if and only if
      * more than one consecutive line matches an exclusion pattern.
      */
     private List<Pattern> excludes = new ArrayList<Pattern>(5);
 
     /**
      * True to print the root cause first.  False to print exceptions normally (root cause last).
      */
     private boolean rootCauseFirst;
 
     /**
      * True to compute and inline stack hashes.
      */
     private boolean inlineHash;
 
     private StackElementFilter stackElementFilter;
 
     private StackHasher stackHasher;
 
     /**
      * Evaluators that determine if the stacktrace should be logged.
      */
     private List<EventEvaluator<ILoggingEvent>> evaluators = new ArrayList<EventEvaluator<ILoggingEvent>>(1);
 
     @Override
     public void start() {
         parseOptions();
         // instantiate stack element filter
         if (excludes == null || excludes.isEmpty()) {
             if (inlineHash) {
                 // filter out elements with no source info
                 addInfo("[inlineHash] is active with no exclusion pattern: use non null source info filter to exclude generated classnames (see doc)");
                 stackElementFilter = StackElementFilter.withSourceInfo();
             } else {
                 // use any filter
                 stackElementFilter = StackElementFilter.any();
             }
         } else {
             // use patterns filter
             stackElementFilter = StackElementFilter.byPattern(excludes);
         }
         // instantiate stack hasher if "inline hash" is active
         if (inlineHash) {
             stackHasher = new StackHasher(stackElementFilter);
         }
         super.start();
     }
 
     private void parseOptions() {
         List<String> optionList = getOptionList();
 
         if (optionList == null) {
             return;
         }
         final int optionListSize = optionList.size();
         for (int i = 0; i < optionListSize; i++) {
             String option = optionList.get(i);
             switch (i) {
                 case OPTION_INDEX_MAX_DEPTH:
                     setMaxDepthPerThrowable(parseIntegerOptionValue(option, FULL_MAX_DEPTH_PER_THROWABLE, SHORT_MAX_DEPTH_PER_THROWABLE, DEFAULT_MAX_DEPTH_PER_THROWABLE));
                     break;
                 case OPTION_INDEX_SHORTENED_CLASS_NAME:
                     setShortenedClassNameLength(parseIntegerOptionValue(option, FULL_CLASS_NAME_LENGTH, SHORT_CLASS_NAME_LENGTH, DEFAULT_CLASS_NAME_LENGTH));
                     break;
                 case OPTION_INDEX_MAX_LENGTH:
                     setMaxLength(parseIntegerOptionValue(option, FULL_MAX_LENGTH, SHORT_MAX_LENGTH, DEFAULT_MAX_LENGTH));
                     break;
                 default:
                     /*
                      * Remaining options are either
                      *     - "rootFirst" - indicating that stacks should be printed root-cause first
                      *     - "inlineHash" - indicating that hexadecimal error hashes should be computed and inlined
                      *     - evaluator name - name of evaluators that will determine if the stacktrace is ignored
                      *     - exclusion pattern - pattern for stack trace elements to exclude
                      */
                     if (OPTION_VALUE_ROOT_FIRST.equals(option)) {
                         setRootCauseFirst(true);
                     } else if (OPTION_VALUE_INLINE_HASH.equals(option)) {
                         setInlineHash(true);
                     } else {
                         @SuppressWarnings("rawtypes")
                         Map evaluatorMap = (Map) getContext().getObject(CoreConstants.EVALUATOR_MAP);
                         @SuppressWarnings("unchecked")
                         EventEvaluator<ILoggingEvent> evaluator = (evaluatorMap != null)
                             ? (EventEvaluator<ILoggingEvent>) evaluatorMap.get(option)
                             : null;
 
                         if (evaluator != null) {
                             addEvaluator(evaluator);
                         } else {
                             addExclude(option);
                         }
                     }
                     break;
             }
         }
     }
 
     private int parseIntegerOptionValue(String option, int valueIfFull, int valueIfShort, int valueIfNonParsable) {
         if (OPTION_VALUE_FULL.equals(option)) {
             return valueIfFull;
         } else if (OPTION_VALUE_SHORT.equals(option)) {
             return valueIfShort;
         } else {
             try {
                 return Integer.parseInt(option);
             } catch (NumberFormatException nfe) {
                 addError("Could not parse [" + option + "] as an integer");
                 return valueIfNonParsable;
             }
         }
     }
 
     @Override
     public String convert(ILoggingEvent event) {
         IThrowableProxy throwableProxy = event.getThrowableProxy();
         if (throwableProxy == null || isExcludedByEvaluator(event)) {
             return CoreConstants.EMPTY_STRING;
         }
 
         // compute stack trace hashes
         Deque<String> stackHashes = null;
         if (inlineHash && (throwableProxy instanceof ThrowableProxy)) {
             stackHashes = stackHasher.hexHashes(((ThrowableProxy) throwableProxy).getThrowable());
         }
 
         /*
          * The extra 100 gives a little more buffer room since we actually
          * go over the maxLength before detecting it and truncating.
          */
         StringBuilder builder = new StringBuilder(Math.min(BUFFER_INITIAL_CAPACITY, this.maxLength + 100 > 0 ? this.maxLength + 100 : this.maxLength));
         if (rootCauseFirst) {
             appendRootCauseFirst(builder, null, ThrowableProxyUtil.REGULAR_EXCEPTION_INDENT, throwableProxy, stackHashes);
         } else {
             appendRootCauseLast(builder, null, ThrowableProxyUtil.REGULAR_EXCEPTION_INDENT, throwableProxy, stackHashes);
         }
         if (builder.length() > maxLength) {
             builder.setLength(maxLength - ELLIPSIS.length() - CoreConstants.LINE_SEPARATOR.length());
             builder.append(ELLIPSIS).append(CoreConstants.LINE_SEPARATOR);
         }
         return builder.toString();
     }
 
     /**
      * Return true if any evaluator returns true, indicating that
      * the stack trace should not be logged.
      */
     private boolean isExcludedByEvaluator(ILoggingEvent event) {
         for (int i = 0; i < evaluators.size(); i++) {
             EventEvaluator<ILoggingEvent> evaluator = evaluators.get(i);
             try {
                 if (evaluator.evaluate(event)) {
                     return true;
                 }
             } catch (EvaluationException eex) {
                 int errors = errorCount.incrementAndGet();
                 if (errors < CoreConstants.MAX_ERROR_COUNT) {
                     addError(String.format("Exception thrown for evaluator named [%s]", evaluator.getName()), eex);
                 } else if (errors == CoreConstants.MAX_ERROR_COUNT) {
                     ErrorStatus errorStatus = new ErrorStatus(
                         String.format("Exception thrown for evaluator named [%s]", evaluator.getName()), this, eex);
                     errorStatus.add(new ErrorStatus(
                         "This was the last warning about this evaluator's errors. "
                             + "We don't want the StatusManager to get flooded.",
                         this));
                     addStatus(errorStatus);
                 }
             }
         }
         return false;
     }
 
 
/** Appends recursively the root cause */
 private void appendRootCauseLast(StringBuilder builder, String prefix, int indent, IThrowableProxy throwableProxy, Deque<String> stackHashes){}

 

}