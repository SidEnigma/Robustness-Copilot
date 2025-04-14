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
 
 package org.openapitools.codegen.utils;
 
 import com.fasterxml.jackson.databind.JsonNode;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import io.swagger.v3.oas.models.OpenAPI;
 import io.swagger.v3.oas.models.Operation;
 import io.swagger.v3.oas.models.PathItem;
 import io.swagger.v3.oas.models.callbacks.Callback;
 import io.swagger.v3.oas.models.headers.Header;
 import io.swagger.v3.oas.models.media.*;
 import io.swagger.v3.oas.models.parameters.Parameter;
 import io.swagger.v3.oas.models.parameters.RequestBody;
 import io.swagger.v3.oas.models.responses.ApiResponse;
 import io.swagger.v3.parser.core.models.AuthorizationValue;
 import io.swagger.v3.parser.util.ClasspathHelper;
 import io.swagger.v3.parser.ObjectMapperFactory;
 import io.swagger.v3.parser.util.RemoteUrl;
 import io.swagger.v3.parser.util.SchemaTypeUtil;
 import org.apache.commons.lang3.StringUtils;
 import org.openapitools.codegen.CodegenModel;
 import org.openapitools.codegen.IJsonSchemaValidationProperties;
 import org.openapitools.codegen.config.GlobalSettings;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.apache.commons.io.FileUtils;
 
 import java.io.UnsupportedEncodingException;
 import java.math.BigDecimal;
 import java.net.URI;
 import java.net.URLDecoder;
 import java.util.*;
 import java.util.Map.Entry;
 import java.util.stream.Collectors;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.nio.file.Paths;
 
 import static org.openapitools.codegen.utils.OnceLogger.once;
 
 public class ModelUtils {
     private static final Logger LOGGER = LoggerFactory.getLogger(ModelUtils.class);
 
     private static final String URI_FORMAT = "uri";
 
     private static final String generateAliasAsModelKey = "generateAliasAsModel";
 
     // A vendor extension to track the value of the 'swagger' field in a 2.0 doc, if applicable.
     private static final String openapiDocVersion = "x-original-swagger-version";
 
     // A vendor extension to track the value of the 'disallowAdditionalPropertiesIfNotPresent' CLI
     private static final String disallowAdditionalPropertiesIfNotPresent = "x-disallow-additional-properties-if-not-present";
 
     private static final String freeFormExplicit = "x-is-free-form";
 
     private static ObjectMapper JSON_MAPPER, YAML_MAPPER;
 
     static {
         JSON_MAPPER = ObjectMapperFactory.createJson();
         YAML_MAPPER = ObjectMapperFactory.createYaml();
     }
 
     public static void setDisallowAdditionalPropertiesIfNotPresent(boolean value) {
         GlobalSettings.setProperty(disallowAdditionalPropertiesIfNotPresent, Boolean.toString(value));
     }
 
     public static boolean isDisallowAdditionalPropertiesIfNotPresent() {
         return Boolean.parseBoolean(GlobalSettings.getProperty(disallowAdditionalPropertiesIfNotPresent, "true"));
     }
 
     public static void setGenerateAliasAsModel(boolean value) {
         GlobalSettings.setProperty(generateAliasAsModelKey, Boolean.toString(value));
     }
 
     public static boolean isGenerateAliasAsModel() {
         return Boolean.parseBoolean(GlobalSettings.getProperty(generateAliasAsModelKey, "false"));
     }
 
     public static boolean isGenerateAliasAsModel(Schema schema) {
         return isGenerateAliasAsModel() || (schema.getExtensions() != null && schema.getExtensions().getOrDefault("x-generate-alias-as-model", false).equals(true));
     }
 
     /**
      * Searches for the model by name in the map of models and returns it
      *
      * @param name   Name of the model
      * @param models Map of models
      * @return model
      */
     public static CodegenModel getModelByName(final String name, final Map<String, Object> models) {
         final Object data = models.get(name);
         if (data instanceof Map) {
             final Map<?, ?> dataMap = (Map<?, ?>) data;
             final Object dataModels = dataMap.get("models");
             if (dataModels instanceof List) {
                 final List<?> dataModelsList = (List<?>) dataModels;
                 for (final Object entry : dataModelsList) {
                     if (entry instanceof Map) {
                         final Map<?, ?> entryMap = (Map<?, ?>) entry;
                         final Object model = entryMap.get("model");
                         if (model instanceof CodegenModel) {
                             return (CodegenModel) model;
                         }
                     }
                 }
             }
         }
         return null;
     }
 
     /**
      * Return the list of all schemas in the 'components/schemas' section used in the openAPI specification
      *
      * @param openAPI specification
      * @return schemas a list of used schemas
      */
     public static List<String> getAllUsedSchemas(OpenAPI openAPI) {
         Map<String, List<String>> childrenMap = getChildrenMap(openAPI);
         List<String> allUsedSchemas = new ArrayList<String>();
         visitOpenAPI(openAPI, (s, t) -> {
             if (s.get$ref() != null) {
                 String ref = getSimpleRef(s.get$ref());
                 if (!allUsedSchemas.contains(ref)) {
                     allUsedSchemas.add(ref);
                 }
                 if (childrenMap.containsKey(ref)) {
                     for (String child : childrenMap.get(ref)) {
                         if (!allUsedSchemas.contains(child)) {
                             allUsedSchemas.add(child);
                         }
                     }
                 }
             }
         });
         return allUsedSchemas;
     }
 
     /**
      * Return the list of unused schemas in the 'components/schemas' section of an openAPI specification
      *
      * @param openAPI specification
      * @return schemas a list of unused schemas
      */
     public static List<String> getUnusedSchemas(OpenAPI openAPI) {
         final Map<String, List<String>> childrenMap;
         Map<String, List<String>> tmpChildrenMap;
         try {
             tmpChildrenMap = getChildrenMap(openAPI);
         } catch (NullPointerException npe) {
             // in rare cases, such as a spec document with only one top-level oneOf schema and multiple referenced schemas,
             // the stream used in getChildrenMap will raise an NPE. Rather than modify getChildrenMap which is used by getAllUsedSchemas,
             // we'll catch here as a workaround for this edge case.
             tmpChildrenMap = new HashMap<>();
         }
 
         childrenMap = tmpChildrenMap;
         List<String> unusedSchemas = new ArrayList<String>();
 
         if (openAPI != null) {
             Map<String, Schema> schemas = getSchemas(openAPI);
             unusedSchemas.addAll(schemas.keySet());
 
             visitOpenAPI(openAPI, (s, t) -> {
                 if (s.get$ref() != null) {
                     String ref = getSimpleRef(s.get$ref());
                     unusedSchemas.remove(ref);
                     if (childrenMap.containsKey(ref)) {
                         unusedSchemas.removeAll(childrenMap.get(ref));
                     }
                 }
             });
         }
         return unusedSchemas;
     }
 
     /**
      * Return the list of schemas in the 'components/schemas' used only in a 'application/x-www-form-urlencoded' or 'multipart/form-data' mime time
      *
      * @param openAPI specification
      * @return schemas a list of schemas
      */
     public static List<String> getSchemasUsedOnlyInFormParam(OpenAPI openAPI) {
         List<String> schemasUsedInFormParam = new ArrayList<String>();
         List<String> schemasUsedInOtherCases = new ArrayList<String>();
 
         visitOpenAPI(openAPI, (s, t) -> {
             if (s.get$ref() != null) {
                 String ref = getSimpleRef(s.get$ref());
                 if ("application/x-www-form-urlencoded".equalsIgnoreCase(t) ||
                         "multipart/form-data".equalsIgnoreCase(t)) {
                     schemasUsedInFormParam.add(ref);
                 } else {
                     schemasUsedInOtherCases.add(ref);
                 }
             }
         });
         return schemasUsedInFormParam.stream().filter(n -> !schemasUsedInOtherCases.contains(n)).collect(Collectors.toList());
     }
 
     /**
      * Private method used by several methods ({@link #getAllUsedSchemas(OpenAPI)},
      * {@link #getUnusedSchemas(OpenAPI)},
      * {@link #getSchemasUsedOnlyInFormParam(OpenAPI)}, ...) to traverse all paths of an
      * OpenAPI instance and call the visitor functional interface when a schema is found.
      *
      * @param openAPI specification
      * @param visitor functional interface (can be defined as a lambda) called each time a schema is found.
      */
     private static void visitOpenAPI(OpenAPI openAPI, OpenAPISchemaVisitor visitor) {
         Map<String, PathItem> paths = openAPI.getPaths();
         List<String> visitedSchemas = new ArrayList<>();
 
         if (paths != null) {
             for (PathItem path : paths.values()) {
                 visitPathItem(path, openAPI, visitor, visitedSchemas);
             }
         }
     }
 
     private static void visitPathItem(PathItem pathItem, OpenAPI openAPI, OpenAPISchemaVisitor visitor, List<String> visitedSchemas) {
         List<Operation> allOperations = pathItem.readOperations();
         if (allOperations != null) {
             for (Operation operation : allOperations) {
                 //Params:
                 visitParameters(openAPI, operation.getParameters(), visitor, visitedSchemas);
 
                 //RequestBody:
                 RequestBody requestBody = getReferencedRequestBody(openAPI, operation.getRequestBody());
                 if (requestBody != null) {
                     visitContent(openAPI, requestBody.getContent(), visitor, visitedSchemas);
                 }
 
                 //Responses:
                 if (operation.getResponses() != null) {
                     for (ApiResponse r : operation.getResponses().values()) {
                         ApiResponse apiResponse = getReferencedApiResponse(openAPI, r);
                         if (apiResponse != null) {
                             visitContent(openAPI, apiResponse.getContent(), visitor, visitedSchemas);
                             if (apiResponse.getHeaders() != null) {
                                 for (Entry<String, Header> e : apiResponse.getHeaders().entrySet()) {
                                     Header header = getReferencedHeader(openAPI, e.getValue());
                                     if (header.getSchema() != null) {
                                         visitSchema(openAPI, header.getSchema(), e.getKey(), visitedSchemas, visitor);
                                     }
                                     visitContent(openAPI, header.getContent(), visitor, visitedSchemas);
                                 }
                             }
                         }
                     }
                 }
 
                 //Callbacks:
                 if (operation.getCallbacks() != null) {
                     for (Callback c : operation.getCallbacks().values()) {
                         Callback callback = getReferencedCallback(openAPI, c);
                         if (callback != null) {
                             for (PathItem p : callback.values()) {
                                 visitPathItem(p, openAPI, visitor, visitedSchemas);
                             }
                         }
                     }
                 }
             }
         }
         //Params:
         visitParameters(openAPI, pathItem.getParameters(), visitor, visitedSchemas);
     }
 
     private static void visitParameters(OpenAPI openAPI, List<Parameter> parameters, OpenAPISchemaVisitor visitor,
                                         List<String> visitedSchemas) {
         if (parameters != null) {
             for (Parameter p : parameters) {
                 Parameter parameter = getReferencedParameter(openAPI, p);
                 if (parameter != null) {
                     if (parameter.getSchema() != null) {
                         visitSchema(openAPI, parameter.getSchema(), null, visitedSchemas, visitor);
                     }
                     visitContent(openAPI, parameter.getContent(), visitor, visitedSchemas);
                 } else {
                     once(LOGGER).warn("Unreferenced parameter(s) found.");
                 }
             }
         }
     }
 
     private static void visitContent(OpenAPI openAPI, Content content, OpenAPISchemaVisitor visitor, List<String> visitedSchemas) {
         if (content != null) {
             for (Entry<String, MediaType> e : content.entrySet()) {
                 if (e.getValue().getSchema() != null) {
                     visitSchema(openAPI, e.getValue().getSchema(), e.getKey(), visitedSchemas, visitor);
                 }
             }
         }
     }
 
 
/** If a schema matchs mimeType in the OpenAPI document, the method invokes the visitor function and is added to visitedAchemas . References schema are visited only one single time to avoid infiite recursion */

private static void visitSchema(OpenAPI openAPI, Schema schema, String mimeType, List<String> visitedSchemas, OpenAPISchemaVisitor visitor) {
    if (schema != null) {
        String ref = getSimpleRef(schema.get$ref());
        if (!visitedSchemas.contains(ref)) {
            visitedSchemas.add(ref);
            visitor.visit(schema, mimeType);
            if (schema.getProperties() != null) {
                for (Map.Entry<String, Schema> entry : schema.getProperties().entrySet()) {
                    visitSchema(openAPI, entry.getValue(), mimeType, visitedSchemas, visitor);
                }
            }
            if (schema.getAdditionalProperties() instanceof Schema) {
                visitSchema(openAPI, (Schema) schema.getAdditionalProperties(), mimeType, visitedSchemas, visitor);
            }
            if (schema.getItems() instanceof Schema) {
                visitSchema(openAPI, (Schema) schema.getItems(), mimeType, visitedSchemas, visitor);
            }
            if (schema.getAllOf() != null) {
                for (Schema s : schema.getAllOf()) {
                    visitSchema(openAPI, s, mimeType, visitedSchemas, visitor);
                }
            }
            if (schema.getOneOf() != null) {
                for (Schema s : schema.getOneOf()) {
                    visitSchema(openAPI, s, mimeType, visitedSchemas, visitor);
                }
            }
            if (schema.getAnyOf() != null) {
                for (Schema s : schema.getAnyOf()) {
                    visitSchema(openAPI, s, mimeType, visitedSchemas, visitor);
                }
            }
            if (schema.getNot() != null) {
                visitSchema(openAPI, schema.getNot(), mimeType, visitedSchemas, visitor);
            }
        }
    }
}
 

}