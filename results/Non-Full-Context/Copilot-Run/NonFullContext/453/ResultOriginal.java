/*
  * Anserini: A Lucene toolkit for reproducible information retrieval research
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package io.anserini.search.topicreader;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.nio.file.Path;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.SortedMap;
 import java.util.TreeMap;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 public class MicroblogTopicReader extends TopicReader<Integer> {
   public MicroblogTopicReader(Path topicFile) {
     super(topicFile);
   }
 
   private static final Pattern TOP_PATTERN =
       Pattern.compile("<top(.*?)</top>", Pattern.DOTALL);
   private static final Pattern NUM_PATTERN =
       Pattern.compile("<num> Number: MB(\\d+) </num>", Pattern.DOTALL);
   // TREC 2011 topics uses <title> tag
   private static final Pattern TITLE_PATTERN =
       Pattern.compile("<title>\\s*(.*?)\\s*</title>", Pattern.DOTALL);
   // TREC 2012 topics use <query> tag
   private static final Pattern TITLE_PATTERN2 =
       Pattern.compile("<query>\\s*(.*?)\\s*</query>", Pattern.DOTALL);
   private static final Pattern TWEETTIME_PATTERN =
       Pattern.compile("<querytweettime>\\s*(\\d+)\\s*</querytweettime>", Pattern.DOTALL);
 
 
/** Read topics of TREC Microblog Tracks from 2011 to 2014 including:  topics. */
 public SortedMap<Integer, Map<String, String>> read(BufferedReader bRdr) throws IOException{
    SortedMap<Integer, Map<String, String>> topics = new TreeMap<>();
    String line;
    while ((line = bRdr.readLine()) != null) {
      Matcher topMatcher = TOP_PATTERN.matcher(line);
      if (topMatcher.find()) {
         String top = topMatcher.group(1);
         Matcher numMatcher = NUM_PATTERN.matcher(top);
         if (numMatcher.find()) {
            String num = numMatcher.group(1);
            Map<String, String> topic = new HashMap<>();
            Matcher titleMatcher = TITLE_PATTERN.matcher(top);
            if (titleMatcher.find()) {
              topic.put("title", titleMatcher.group(1));
            } else {
              titleMatcher = TITLE_PATTERN2.matcher(top);
              if (titleMatcher.find()) {
                 topic.put("title", titleMatcher.group(1));
              }
            }
            Matcher tweetTimeMatcher = TWEETTIME_PATTERN.matcher(top);
            if (tweetTimeMatcher.find()) {
              topic.put("tweetTime", tweetTimeMatcher.group(1));
            }
            topics.put(Integer.parseInt(num), topic);
         }
      }
    }
    return topics;
     }          
 }

 

}