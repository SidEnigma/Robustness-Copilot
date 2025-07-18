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
 package net.logstash.logback.appender;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.concurrent.TimeUnit;
 
 import com.lmax.disruptor.BlockingWaitStrategy;
 import com.lmax.disruptor.BusySpinWaitStrategy;
 import com.lmax.disruptor.LiteBlockingWaitStrategy;
 import com.lmax.disruptor.LiteTimeoutBlockingWaitStrategy;
 import com.lmax.disruptor.PhasedBackoffWaitStrategy;
 import com.lmax.disruptor.SleepingWaitStrategy;
 import com.lmax.disruptor.TimeoutBlockingWaitStrategy;
 import com.lmax.disruptor.WaitStrategy;
 import com.lmax.disruptor.YieldingWaitStrategy;
 
 /**
  * Creates {@link WaitStrategy} objects from strings.
  */
 public class WaitStrategyFactory {
 
     private static final char PARAM_END_CHAR = '}';
     private static final char PARAM_START_CHAR = '{';
     private static final char PARAM_SEPARATOR_CHAR = ',';
 
     /**
      * Creates a {@link WaitStrategy} from a string.
      * <p>
      * The following strategies are supported:
      * <ul>
      * <li><code>blocking</code> - {@link BlockingWaitStrategy}</li>
      * <li><code>busySpin</code> - {@link BusySpinWaitStrategy}</li>
      * <li><code>liteBlocking</code> - {@link LiteBlockingWaitStrategy}</li>
      * <li><code>sleeping{retries,sleepTimeNs}</code> - {@link SleepingWaitStrategy}
      *         - <code>retries</code> an integer number of times to spin before sleeping. (default = 200)
      *           <code>sleepTimeNs</code> nanosecond time to sleep each iteration after spinning (default = 100)
      * </li>
      * <li><code>yielding</code> - {@link YieldingWaitStrategy}</li>
      * <li><code>phasedBackoff{spinTimeout,yieldTimeout,timeUnit,fallackStrategy}</code> - {@link PhasedBackoffWaitStrategy}
      *         - <code>spinTimeout</code> and <code>yieldTimeout</code> are long values.
      *           <code>timeUnit</code> is a string name of one of the {@link TimeUnit} values.
      *           <code>fallbackStrategy</code> is a wait strategy string (e.g. <code>blocking</code>).
      * </li>
      * <li><code>timeoutBlocking{timeout,timeUnit}</code> - {@link TimeoutBlockingWaitStrategy}
      *         - <code>timeout</code> is a long value.
      *           <code>timeUnit</code> is a string name of one of the {@link TimeUnit} values.
      * </li>
      * <li><code>liteTimeoutBlocking{timeout,timeUnit}</code> - {@link LiteTimeoutBlockingWaitStrategy}
      *         - <code>timeout</code> is a long value.
      *           <code>timeUnit</code> is a string name of one of the {@link TimeUnit} values.
      * </li>
      * </ul>
      *
      * @param waitStrategyType the name of the desired wait strategy
      * @return a {@link WaitStrategy} instance or {@code null} if the supplied name is {@code null} or empty
      * @throws IllegalArgumentException if an unknown wait strategy type is given, or the parameters are unable to be parsed.
      */
     public static WaitStrategy createWaitStrategyFromString(String waitStrategyType) {
         if (waitStrategyType == null) {
             return null;
         }
         waitStrategyType = waitStrategyType.trim().toLowerCase();
         if (waitStrategyType.isEmpty()) {
             return null;
         }
         if (waitStrategyType.equals("blocking")) {
             return new BlockingWaitStrategy();
         }
         if (waitStrategyType.equals("busyspin")) {
             return new BusySpinWaitStrategy();
         }
         if (waitStrategyType.equals("liteblocking")) {
             return new LiteBlockingWaitStrategy();
         }
         if (waitStrategyType.startsWith("sleeping")) {
             if (waitStrategyType.equals("sleeping")) {
                 return new SleepingWaitStrategy();
             } else {
                 List<Object> params = parseParams(waitStrategyType,
                         Integer.class,
                         Long.class);
                 return new SleepingWaitStrategy(
                         (Integer) params.get(0),
                         (Long) params.get(1));
             }
         }
         if (waitStrategyType.equals("yielding")) {
             return new YieldingWaitStrategy();
         }
         if (waitStrategyType.startsWith("phasedbackoff")) {
             List<Object> params = parseParams(waitStrategyType,
                     Long.class,
                     Long.class,
                     TimeUnit.class,
                     WaitStrategy.class);
             return new PhasedBackoffWaitStrategy(
                     (Long) params.get(0),
                     (Long) params.get(1),
                     (TimeUnit) params.get(2),
                     (WaitStrategy) params.get(3));
         }
         if (waitStrategyType.startsWith("timeoutblocking")) {
             List<Object> params = parseParams(waitStrategyType,
                     Long.class,
                     TimeUnit.class);
             return new TimeoutBlockingWaitStrategy(
                     (Long) params.get(0),
                     (TimeUnit) params.get(1));
         }
         if (waitStrategyType.startsWith("litetimeoutblocking")) {
             List<Object> params = parseParams(waitStrategyType,
                     Long.class,
                     TimeUnit.class);
             return new LiteTimeoutBlockingWaitStrategy(
                     (Long) params.get(0),
                     (TimeUnit) params.get(1));
         }
 
         throw new IllegalArgumentException("Unknown wait strategy type: " + waitStrategyType);
 
     }
 
     private static List<Object> parseParams(String waitStrategyType, Class<?>... paramTypes) {
         String paramsString = extractParamsString(waitStrategyType, paramTypes);
 
         List<Object> params = new ArrayList<Object>(paramTypes.length);
 
         int startIndex = 0;
         for (int i = 0; i < paramTypes.length && startIndex < paramsString.length(); i++) {
             int endIndex = findParamEndIndex(paramsString, startIndex);
             String paramString = paramsString.substring(startIndex, endIndex).trim();
             startIndex = endIndex + 1;
 
             if (Integer.class.equals(paramTypes[i])) {
                 params.add(Integer.valueOf(paramString));
             } else if (Long.class.equals(paramTypes[i])) {
                 params.add(Long.valueOf(paramString));
             } else if (TimeUnit.class.equals(paramTypes[i])) {
                 params.add(TimeUnit.valueOf(paramString.toUpperCase()));
             } else if (WaitStrategy.class.equals(paramTypes[i])) {
                 params.add(createWaitStrategyFromString(paramString));
             } else {
                 throw new IllegalArgumentException("Unknown paramType " + paramTypes[i]);
             }
 
         }
         if (params.size() != paramTypes.length) {
             throw new IllegalArgumentException(String.format("%d parameters must be provided for waitStrategyType %s. %d were provided.", paramTypes.length, waitStrategyType, params.size()));
         }
 
         return params;
     }
 
     /**
      * Extracts the parameters string (i.e. the part between the curly braces) from the waitStrategyType string.
      *
      * @throws IllegalArgumentException if no param string was found, or it is invalid.
      */
     private static String extractParamsString(String waitStrategyType, Class<?>... paramTypes) {
         int startIndex = waitStrategyType.indexOf(PARAM_START_CHAR);
         if (startIndex == -1) {
             throw new IllegalArgumentException(
                 String.format("%d parameters must be provided for waitStrategyType %s."
                     + " None were provided."
                     + " To provide parameters, add a comma separated value list within curly braces ({}) to the end of the waitStrategyType string.",
                     paramTypes.length,
                     waitStrategyType));
         }
         int endIndex = waitStrategyType.lastIndexOf(PARAM_END_CHAR);
         if (endIndex == -1) {
             throw new IllegalArgumentException(String.format("Parameters of %s must end with '}'", waitStrategyType));
         }
 
         return waitStrategyType.substring(startIndex + 1, endIndex);
     }
 
 
/** Identifies the parameter's final character index within the paramsString, which begins at startIndex. Considers the nesting of parameters. */
 private static int findParamEndIndex(String paramsString, int startIndex){
        int nestingLevel = 0;
        for (int i = startIndex; i < paramsString.length(); i++) {
            char c = paramsString.charAt(i);
            if (c == PARAM_START_CHAR) {
                nestingLevel++;
            } else if (c == PARAM_END_CHAR) {
                nestingLevel--;
                if (nestingLevel == 0) {
                    return i;
                }
            }
        }
        throw new IllegalArgumentException(String.format("Unmatched '%s' in paramsString %s", PARAM_START_CHAR, paramsString));
    }
    
        /**
        * Returns the name of the supplied wait strategy.
        *
        * @param waitStrategy the wait strategy to get the name of
        * @return the name of the supplied wait strategy
        */
        public static String getWaitStrategyName(WaitStrategy waitStrategy) {
            if (waitStrategy instanceof BlockingWaitStrategy) {
                return "blocking";
            }
            if (waitStrategy instanceof BusySpinWaitStrategy) {
                return "busyspin";
            }
            if (waitStrategy instanceof LiteBlockingWaitStrategy) {
                return "liteblocking";
            }
            if (waitStrategy instanceof SleepingWaitStrategy) {
                return "sleeping";
            }
            if (waitStrategy instanceof YieldingWaitStrategy) {
                return "yielding";
            }
            if (waitStrategy instanceof PhasedBackoffWaitStrategy) {
                return "phasedbackoff";
            }
            if (waitStrategy instanceof TimeoutBlockingWaitStrategy) {
                return "timeoutblocking";
            }
            if (waitStrategy instanceof LiteTimeoutBlockingWaitStrategy) {
                return "litetimeoutblocking";
            }
            return "unknown";
        }
    
        /**
        * Returns the parameters of the supplied wait strategy.
        *
        * @param waitStrategy the wait strategy to get the parameters of
        * @return the parameters of the supplied wait strategy
        */
        public static       
 }

 

}