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
 package net.logstash.logback.appender.destination;
 
 import java.net.InetSocketAddress;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import ch.qos.logback.core.CoreConstants;
 
 /**
  * Constructs {@link InetSocketAddress}es by parsing {@link String} values.
  */
 public class DestinationParser {
 
     private static final Pattern DESTINATION_PATTERN = Pattern.compile("^\\s*(\\S+?)\\s*(:\\s*(\\S+)\\s*)?$");
     private static final int HOSTNAME_GROUP = 1;
     private static final int PORT_GROUP = 3;
 
     private DestinationParser() {
         // utility class
     }
     
 
/** The given @link String value is used to construct @link InetSocketAddresses. */
 public static List<InetSocketAddress> parse(String destinations, int defaultPort){}

                                                       
 }
