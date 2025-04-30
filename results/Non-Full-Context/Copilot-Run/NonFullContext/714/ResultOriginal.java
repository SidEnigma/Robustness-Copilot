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
 
 
/** Creates a {@link WaitStrategy} from a string. */
 public static WaitStrategy createWaitStrategyFromString(String waitStrategyType){
      if(waitStrategyType == null){
          return new BusySpinWaitStrategy();
      }
      if(waitStrategyType.equals("busySpin")){
          return new BusySpinWaitStrategy();
      }
      if(waitStrategyType.equals("sleeping")){
          return new SleepingWaitStrategy();
      }
      if(waitStrategyType.equals("yielding")){
          return new YieldingWaitStrategy();
      }
      if(waitStrategyType.equals("liteBlocking")){
          return new LiteBlockingWaitStrategy();
      }
      if(waitStrategyType.equals("liteTimeoutBlocking")){
          return new LiteTimeoutBlockingWaitStrategy();
      }
      if(waitStrategyType.equals("timeoutBlocking")){
          return new TimeoutBlockingWaitStrategy();
      }
      if(waitStrategyType.equals("blocking")){
          return new BlockingWaitStrategy();
      }
      if(waitStrategyType.equals("phasedBackoff")){
          return new PhasedBackoffWaitStrategy();
      }
      throw new IllegalArgumentException("Unknown wait strategy type: " + waitStrategyType);  
 }

 

}