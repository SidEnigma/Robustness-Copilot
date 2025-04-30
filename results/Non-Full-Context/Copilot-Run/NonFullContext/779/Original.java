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
 package net.logstash.logback.composite;
 
 import java.io.IOException;
 import java.util.Iterator;
 import java.util.Map.Entry;
 import java.util.Objects;
 
 import ch.qos.logback.core.spi.DeferredProcessingAware;
 import com.fasterxml.jackson.core.JsonFactory;
 import com.fasterxml.jackson.core.JsonGenerator;
 import com.fasterxml.jackson.databind.JsonNode;
 import com.fasterxml.jackson.databind.node.ObjectNode;
 
 public class GlobalCustomFieldsJsonProvider<Event extends DeferredProcessingAware> extends AbstractJsonProvider<Event> implements JsonFactoryAware {
     
     /**
      * The un-parsed custom fields string to use to initialize customFields
      * when the formatter is started.
      */
     private String customFields;
 
     /**
      * When non-null, the fields in this JsonNode will be embedded in the logstash json.
      */
     private ObjectNode customFieldsNode;
     
     /**
      * The factory used to convert the JSON string into a valid {@link ObjectNode} when custom
      * fields are set as text instead of a pre-parsed Jackson ObjectNode.
      */
     private JsonFactory jsonFactory;
 
     @Override
     public void writeTo(JsonGenerator generator, Event event) throws IOException {
         writeFieldsOfNode(generator, customFieldsNode);
     }
 
 
/** Writes the fields of the given node into the generator. */
 private void writeFieldsOfNode(JsonGenerator generator, JsonNode node) throws IOException{}

 

}