/*
  * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
  * Copyright 2018 SmartBear Software
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     https://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.openapitools.codegen;
 
 import com.fasterxml.jackson.core.JsonProcessingException;
 import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
 import com.fasterxml.jackson.databind.MapperFeature;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import io.swagger.v3.core.util.Json;
 import io.swagger.v3.oas.models.*;
 import io.swagger.v3.oas.models.callbacks.Callback;
 import io.swagger.v3.oas.models.media.*;
 import io.swagger.v3.oas.models.parameters.Parameter;
 import io.swagger.v3.oas.models.parameters.RequestBody;
 import io.swagger.v3.oas.models.responses.ApiResponse;
 import io.swagger.v3.oas.models.responses.ApiResponses;
 import org.openapitools.codegen.utils.ModelUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.util.*;
 import java.util.stream.Collectors;
 
 public class InlineModelResolver {
     private OpenAPI openapi;
     private Map<String, Schema> addedModels = new HashMap<String, Schema>();
     private Map<String, String> generatedSignature = new HashMap<String, String>();
 
     // structure mapper sorts properties alphabetically on write to ensure models are
     // serialized consistently for lookup of existing models
     private static ObjectMapper structureMapper;
 
     static {
         structureMapper = Json.mapper().copy();
         structureMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
         structureMapper.writer(new DefaultPrettyPrinter());
     }
 
      final Logger LOGGER = LoggerFactory.getLogger(InlineModelResolver.class);
 
     void flatten(OpenAPI openapi) {
         this.openapi = openapi;
 
         if (openapi.getComponents() == null) {
             openapi.setComponents(new Components());
         }
 
         if (openapi.getComponents().getSchemas() == null) {
             openapi.getComponents().setSchemas(new HashMap<String, Schema>());
         }
 
         flattenPaths(openapi);
         flattenComponents(openapi);
     }
 
     /**
      * Flatten inline models in Paths
      *
      * @param openAPI target spec
      */
     private void flattenPaths(OpenAPI openAPI) {
         Paths paths = openAPI.getPaths();
         if (paths == null) {
             return;
         }
 
         for (Map.Entry<String, PathItem> pathsEntry : paths.entrySet()) {
             String pathname = pathsEntry.getKey();
             PathItem path = pathsEntry.getValue();
             List<Operation> operations = new ArrayList<>(path.readOperations());
 
             // Include callback operation as well
             for (Operation operation : path.readOperations()) {
                 Map<String, Callback> callbacks = operation.getCallbacks();
                 if (callbacks != null) {
                     operations.addAll(callbacks.values().stream()
                             .flatMap(callback -> callback.values().stream())
                             .flatMap(pathItem -> pathItem.readOperations().stream())
                             .collect(Collectors.toList()));
                 }
             }
 
             for (Operation operation : operations) {
                 flattenRequestBody(openAPI, pathname, operation);
                 flattenParameters(openAPI, pathname, operation);
                 flattenResponses(openAPI, pathname, operation);
             }
         }
     }
 
     /**
      * Flatten inline models in RequestBody
      *
      * @param openAPI target spec
      * @param pathname target pathname
      * @param operation target operation
      */
     private void flattenRequestBody(OpenAPI openAPI, String pathname, Operation operation) {
         RequestBody requestBody = operation.getRequestBody();
         if (requestBody == null) {
             return;
         }
 
         Schema model = ModelUtils.getSchemaFromRequestBody(requestBody);
         if (model instanceof ObjectSchema) {
             Schema obj = model;
             if (obj.getType() == null || "object".equals(obj.getType())) {
                 if (obj.getProperties() != null && obj.getProperties().size() > 0) {
                     flattenProperties(openAPI, obj.getProperties(), pathname);
                     // for model name, use "title" if defined, otherwise default to 'inline_object'
                     String modelName = resolveModelName(obj.getTitle(), "inline_object");
                     addGenerated(modelName, model);
                     openAPI.getComponents().addSchemas(modelName, model);
 
                     // create request body
                     RequestBody rb = new RequestBody();
                     rb.setRequired(requestBody.getRequired());
                     Content content = new Content();
                     MediaType mt = new MediaType();
                     Schema schema = new Schema();
                     schema.set$ref(modelName);
                     mt.setSchema(schema);
 
                     // get "consumes", e.g. application/xml, application/json
                     Set<String> consumes;
                     if (requestBody == null || requestBody.getContent() == null || requestBody.getContent().isEmpty()) {
                         consumes = new HashSet<>();
                         consumes.add("application/json"); // default to application/json
                         LOGGER.info("Default to application/json for inline body schema");
                     } else {
                         consumes = requestBody.getContent().keySet();
                     }
 
                     for (String consume : consumes) {
                         content.addMediaType(consume, mt);
                     }
 
                     rb.setContent(content);
 
                     // add to openapi "components"
                     if (openAPI.getComponents().getRequestBodies() == null) {
                         Map<String, RequestBody> requestBodies = new HashMap<String, RequestBody>();
                         requestBodies.put(modelName, rb);
                         openAPI.getComponents().setRequestBodies(requestBodies);
                     } else {
                         openAPI.getComponents().getRequestBodies().put(modelName, rb);
                     }
 
                     // update requestBody to use $ref instead of inline def
                     requestBody.set$ref(modelName);
 
                 }
             }
         } else if (model instanceof ArraySchema) {
             ArraySchema am = (ArraySchema) model;
             Schema inner = am.getItems();
             if (inner instanceof ObjectSchema) {
                 ObjectSchema op = (ObjectSchema) inner;
                 if (op.getProperties() != null && op.getProperties().size() > 0) {
                     flattenProperties(openAPI, op.getProperties(), pathname);
                     // Generate a unique model name based on the title.
                     String modelName = resolveModelName(op.getTitle(), null);
                     Schema innerModel = modelFromProperty(openAPI, op, modelName);
                     String existing = matchGenerated(innerModel);
                     if (existing != null) {
                         Schema schema = new Schema().$ref(existing);
                         schema.setRequired(op.getRequired());
                         am.setItems(schema);
                     } else {
                         Schema schema = new Schema().$ref(modelName);
                         schema.setRequired(op.getRequired());
                         am.setItems(schema);
                         addGenerated(modelName, innerModel);
                         openAPI.getComponents().addSchemas(modelName, innerModel);
                     }
                 }
             }
         }
     }
 
     /**
      * Flatten inline models in parameters
      *
      * @param openAPI target spec
      * @param pathname target pathname
      * @param operation target operation
      */
     private void flattenParameters(OpenAPI openAPI, String pathname, Operation operation) {
         List<Parameter> parameters = operation.getParameters();
         if (parameters == null) {
             return;
         }
 
         for (Parameter parameter : parameters) {
             if (parameter.getSchema() == null) {
                 continue;
             }
 
             Schema model = parameter.getSchema();
             if (model instanceof ObjectSchema) {
                 Schema obj = model;
                 if (obj.getType() == null || "object".equals(obj.getType())) {
                     if (obj.getProperties() != null && obj.getProperties().size() > 0) {
                         flattenProperties(openAPI, obj.getProperties(), pathname);
                         String modelName = resolveModelName(obj.getTitle(), parameter.getName());
 
                         parameter.$ref(modelName);
                         addGenerated(modelName, model);
                         openAPI.getComponents().addSchemas(modelName, model);
                     }
                 }
             } else if (model instanceof ArraySchema) {
                 ArraySchema am = (ArraySchema) model;
                 Schema inner = am.getItems();
                 if (inner instanceof ObjectSchema) {
                     ObjectSchema op = (ObjectSchema) inner;
                     if (op.getProperties() != null && op.getProperties().size() > 0) {
                         flattenProperties(openAPI, op.getProperties(), pathname);
                         String modelName = resolveModelName(op.getTitle(), parameter.getName());
                         Schema innerModel = modelFromProperty(openAPI, op, modelName);
                         String existing = matchGenerated(innerModel);
                         if (existing != null) {
                             Schema schema = new Schema().$ref(existing);
                             schema.setRequired(op.getRequired());
                             am.setItems(schema);
                         } else {
                             Schema schema = new Schema().$ref(modelName);
                             schema.setRequired(op.getRequired());
                             am.setItems(schema);
                             addGenerated(modelName, innerModel);
                             openAPI.getComponents().addSchemas(modelName, innerModel);
                         }
                     }
                 }
             }
         }
     }
 
     /**
      * Flatten inline models in ApiResponses
      *
      * @param openAPI target spec
      * @param pathname target pathname
      * @param operation target operation
      */
     private void flattenResponses(OpenAPI openAPI, String pathname, Operation operation) {
         ApiResponses responses = operation.getResponses();
         if (responses == null) {
             return;
         }
 
         for (Map.Entry<String, ApiResponse> responsesEntry : responses.entrySet()) {
             String key = responsesEntry.getKey();
             ApiResponse response = responsesEntry.getValue();
             if (ModelUtils.getSchemaFromResponse(response) == null) {
                 continue;
             }
 
             Schema property = ModelUtils.getSchemaFromResponse(response);
             if (property instanceof ObjectSchema) {
                 ObjectSchema op = (ObjectSchema) property;
                 if (op.getProperties() != null && op.getProperties().size() > 0) {
                     String modelName = resolveModelName(op.getTitle(), "inline_response_" + key);
                     Schema model = modelFromProperty(openAPI, op, modelName);
                     String existing = matchGenerated(model);
                     Content content = response.getContent();
                     for (MediaType mediaType : content.values()) {
                         if (existing != null) {
                             Schema schema = this.makeSchema(existing, property);
                             schema.setRequired(op.getRequired());
                             mediaType.setSchema(schema);
                         } else {
                             Schema schema = this.makeSchema(modelName, property);
                             schema.setRequired(op.getRequired());
                             mediaType.setSchema(schema);
                             addGenerated(modelName, model);
                             openAPI.getComponents().addSchemas(modelName, model);
                         }
                     }
                 }
             } else if (property instanceof ArraySchema) {
                 ArraySchema ap = (ArraySchema) property;
                 Schema inner = ap.getItems();
                 if (inner instanceof ObjectSchema) {
                     ObjectSchema op = (ObjectSchema) inner;
                     if (op.getProperties() != null && op.getProperties().size() > 0) {
                         flattenProperties(openAPI, op.getProperties(), pathname);
                         String modelName = resolveModelName(op.getTitle(),
                                 "inline_response_" + key);
                         Schema innerModel = modelFromProperty(openAPI, op, modelName);
                         String existing = matchGenerated(innerModel);
                         if (existing != null) {
                             Schema schema = this.makeSchema(existing, op);
                             schema.setRequired(op.getRequired());
                             ap.setItems(schema);
                         } else {
                             Schema schema = this.makeSchema(modelName, op);
                             schema.setRequired(op.getRequired());
                             ap.setItems(schema);
                             addGenerated(modelName, innerModel);
                             openAPI.getComponents().addSchemas(modelName, innerModel);
                         }
                     }
                 }
             } else if (property instanceof MapSchema) {
                 MapSchema mp = (MapSchema) property;
                 Schema innerProperty = ModelUtils.getAdditionalProperties(openAPI, mp);
                 if (innerProperty instanceof ObjectSchema) {
                     ObjectSchema op = (ObjectSchema) innerProperty;
                     if (op.getProperties() != null && op.getProperties().size() > 0) {
                         flattenProperties(openAPI, op.getProperties(), pathname);
                         String modelName = resolveModelName(op.getTitle(),
                                 "inline_response_" + key);
                         Schema innerModel = modelFromProperty(openAPI, op, modelName);
                         String existing = matchGenerated(innerModel);
                         if (existing != null) {
                             Schema schema = new Schema().$ref(existing);
                             schema.setRequired(op.getRequired());
                             mp.setAdditionalProperties(schema);
                         } else {
                             Schema schema = new Schema().$ref(modelName);
                             schema.setRequired(op.getRequired());
                             mp.setAdditionalProperties(schema);
                             addGenerated(modelName, innerModel);
                             openAPI.getComponents().addSchemas(modelName, innerModel);
                         }
                     }
                 }
             }
         }
     }
 
 
/** Flattens the properties of inline object schemas that belong to a schema composed into a single flat list of properties. */

private void flattenComposedChildren(OpenAPI openAPI, String key, List<Schema> children) {
    // Implementation logic goes here
}
 

}