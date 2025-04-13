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
 
 import com.github.benmanes.caffeine.cache.Cache;
 import com.github.benmanes.caffeine.cache.Caffeine;
 import com.github.benmanes.caffeine.cache.Ticker;
 import com.google.common.base.CaseFormat;
 import com.google.common.collect.ImmutableMap;
 import com.samskivert.mustache.Mustache;
 import com.samskivert.mustache.Mustache.Compiler;
 import com.samskivert.mustache.Mustache.Lambda;
 
 import org.apache.commons.lang3.ObjectUtils;
 import org.apache.commons.lang3.StringEscapeUtils;
 import org.apache.commons.lang3.StringUtils;
 import org.apache.commons.lang3.tuple.Pair;
 import org.openapitools.codegen.CodegenDiscriminator.MappedModel;
 import org.openapitools.codegen.api.TemplatingEngineAdapter;
 import org.openapitools.codegen.config.GlobalSettings;
 import org.openapitools.codegen.examples.ExampleGenerator;
 import org.openapitools.codegen.meta.FeatureSet;
 import org.openapitools.codegen.meta.GeneratorMetadata;
 import org.openapitools.codegen.meta.Stability;
 import org.openapitools.codegen.meta.features.*;
 import org.openapitools.codegen.serializer.SerializerUtils;
 import org.openapitools.codegen.templating.MustacheEngineAdapter;
 import org.openapitools.codegen.templating.mustache.*;
 import org.openapitools.codegen.utils.ModelUtils;
 import org.openapitools.codegen.utils.OneOfImplementorAdditionalData;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.io.File;
 import java.util.*;
 import java.util.Map.Entry;
 import java.util.concurrent.ConcurrentSkipListSet;
 import java.util.concurrent.TimeUnit;
 import java.util.function.Consumer;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 import java.util.stream.Collectors;
 import java.util.stream.Stream;
 
 import io.swagger.v3.core.util.Json;
 import io.swagger.v3.oas.models.OpenAPI;
 import io.swagger.v3.oas.models.Operation;
 import io.swagger.v3.oas.models.PathItem;
 import io.swagger.v3.oas.models.callbacks.Callback;
 import io.swagger.v3.oas.models.examples.Example;
 import io.swagger.v3.oas.models.headers.Header;
 import io.swagger.v3.oas.models.media.*;
 import io.swagger.v3.oas.models.parameters.*;
 import io.swagger.v3.oas.models.responses.ApiResponse;
 import io.swagger.v3.oas.models.responses.ApiResponses;
 import io.swagger.v3.oas.models.security.OAuthFlow;
 import io.swagger.v3.oas.models.security.OAuthFlows;
 import io.swagger.v3.oas.models.security.SecurityScheme;
 import io.swagger.v3.oas.models.servers.Server;
 import io.swagger.v3.oas.models.servers.ServerVariable;
 import io.swagger.v3.parser.util.SchemaTypeUtil;
 
 import static org.openapitools.codegen.utils.OnceLogger.once;
 import static org.openapitools.codegen.utils.StringUtils.*;
 
 public class DefaultCodegen implements CodegenConfig {
     private final Logger LOGGER = LoggerFactory.getLogger(DefaultCodegen.class);
 
     public static FeatureSet DefaultFeatureSet;
 
     // A cache of sanitized words. The sanitizeName() method is invoked many times with the same
     // arguments, this cache is used to optimized performance.
     private static Cache<SanitizeNameOptions, String> sanitizedNameCache;
 
     static {
         DefaultFeatureSet = FeatureSet.newBuilder()
                 .includeDataTypeFeatures(
                         DataTypeFeature.Int32, DataTypeFeature.Int64, DataTypeFeature.Float, DataTypeFeature.Double,
                         DataTypeFeature.Decimal, DataTypeFeature.String, DataTypeFeature.Byte, DataTypeFeature.Binary,
                         DataTypeFeature.Boolean, DataTypeFeature.Date, DataTypeFeature.DateTime, DataTypeFeature.Password,
                         DataTypeFeature.File, DataTypeFeature.Array, DataTypeFeature.Maps, DataTypeFeature.CollectionFormat,
                         DataTypeFeature.CollectionFormatMulti, DataTypeFeature.Enum, DataTypeFeature.ArrayOfEnum, DataTypeFeature.ArrayOfModel,
                         DataTypeFeature.ArrayOfCollectionOfPrimitives, DataTypeFeature.ArrayOfCollectionOfModel, DataTypeFeature.ArrayOfCollectionOfEnum,
                         DataTypeFeature.MapOfEnum, DataTypeFeature.MapOfModel, DataTypeFeature.MapOfCollectionOfPrimitives,
                         DataTypeFeature.MapOfCollectionOfModel, DataTypeFeature.MapOfCollectionOfEnum
                         // Custom types are template specific
                 )
                 .includeDocumentationFeatures(
                         DocumentationFeature.Api, DocumentationFeature.Model
                         // README is template specific
                 )
                 .includeGlobalFeatures(
                         GlobalFeature.Host, GlobalFeature.BasePath, GlobalFeature.Info, GlobalFeature.PartialSchemes,
                         GlobalFeature.Consumes, GlobalFeature.Produces, GlobalFeature.ExternalDocumentation, GlobalFeature.Examples,
                         GlobalFeature.Callbacks
                         // TODO: xml structures, styles, link objects, parameterized servers, full schemes for OAS 2.0
                 )
                 .includeSchemaSupportFeatures(
                         SchemaSupportFeature.Simple, SchemaSupportFeature.Composite,
                         SchemaSupportFeature.Polymorphism
                         // Union (OneOf) not 100% yet.
                 )
                 .includeParameterFeatures(
                         ParameterFeature.Path, ParameterFeature.Query, ParameterFeature.Header, ParameterFeature.Body,
                         ParameterFeature.FormUnencoded, ParameterFeature.FormMultipart, ParameterFeature.Cookie
                 )
                 .includeSecurityFeatures(
                         SecurityFeature.BasicAuth, SecurityFeature.ApiKey, SecurityFeature.BearerToken,
                         SecurityFeature.OAuth2_Implicit, SecurityFeature.OAuth2_Password,
                         SecurityFeature.OAuth2_ClientCredentials, SecurityFeature.OAuth2_AuthorizationCode
                         // OpenIDConnect not yet supported
                 )
                 .includeWireFormatFeatures(
                         WireFormatFeature.JSON, WireFormatFeature.XML
                         // PROTOBUF and Custom are generator specific
                 )
                 .build();
 
         int cacheSize = Integer.parseInt(GlobalSettings.getProperty(NAME_CACHE_SIZE_PROPERTY, "500"));
         int cacheExpiry = Integer.parseInt(GlobalSettings.getProperty(NAME_CACHE_EXPIRY_PROPERTY, "10"));
         sanitizedNameCache = Caffeine.newBuilder()
                 .maximumSize(cacheSize)
                 .expireAfterAccess(cacheExpiry, TimeUnit.SECONDS)
                 .ticker(Ticker.systemTicker())
                 .build();
     }
 
     protected GeneratorMetadata generatorMetadata;
     protected String inputSpec;
     protected String outputFolder = "";
     protected Set<String> defaultIncludes;
     protected Map<String, String> typeMapping;
     protected Map<String, String> instantiationTypes;
     protected Set<String> reservedWords;
     protected Set<String> languageSpecificPrimitives = new HashSet<>();
     protected Map<String, String> importMapping = new HashMap<>();
     protected String modelPackage = "", apiPackage = "", fileSuffix;
     protected String modelNamePrefix = "", modelNameSuffix = "";
     protected String apiNamePrefix = "", apiNameSuffix = "Api";
     protected String testPackage = "";
     protected String filesMetadataFilename = "FILES";
     protected String versionMetadataFilename = "VERSION";
     /*
     apiTemplateFiles are for API outputs only (controllers/handlers).
     API templates may be written multiple times; APIs are grouped by tag and the file is written once per tag group.
     */
     protected Map<String, String> apiTemplateFiles = new HashMap<>();
     protected Map<String, String> modelTemplateFiles = new HashMap<>();
     protected Map<String, String> apiTestTemplateFiles = new HashMap<>();
     protected Map<String, String> modelTestTemplateFiles = new HashMap<>();
     protected Map<String, String> apiDocTemplateFiles = new HashMap<>();
     protected Map<String, String> modelDocTemplateFiles = new HashMap<>();
     protected Map<String, String> reservedWordsMappings = new HashMap<>();
     protected String templateDir;
     protected String embeddedTemplateDir;
     protected Map<String, Object> additionalProperties = new HashMap<>();
     protected Map<String, String> serverVariables = new HashMap<>();
     protected Map<String, Object> vendorExtensions = new HashMap<>();
     /*
     Supporting files are those which aren't models, APIs, or docs.
     These get a different map of data bound to the templates. Supporting files are written once.
     See also 'apiTemplateFiles'.
     */
     protected List<SupportingFile> supportingFiles = new ArrayList<>();
     protected List<CliOption> cliOptions = new ArrayList<>();
     protected boolean skipOverwrite;
     protected boolean removeOperationIdPrefix;
     protected String removeOperationIdPrefixDelimiter = "_";
     protected int removeOperationIdPrefixCount = 1;
     protected boolean skipOperationExample;
 
     protected final static Pattern JSON_MIME_PATTERN = Pattern.compile("(?i)application\\/json(;.*)?");
     protected final static Pattern JSON_VENDOR_MIME_PATTERN = Pattern.compile("(?i)application\\/vnd.(.*)+json(;.*)?");
     private static final Pattern COMMON_PREFIX_ENUM_NAME = Pattern.compile("[a-zA-Z0-9]+\\z");
 
     /**
      * True if the code generator supports multiple class inheritance.
      * This is used to model the parent hierarchy based on the 'allOf' composed schemas.
      */
     protected boolean supportsMultipleInheritance;
     /**
      * True if the code generator supports single class inheritance.
      * This is used to model the parent hierarchy based on the 'allOf' composed schemas.
      * Note: the single-class inheritance technique has inherent limitations because
      * a 'allOf' composed schema may have multiple $ref child schemas, each one
      * potentially representing a "parent" in the class inheritance hierarchy.
      * Some language generators also use class inheritance to implement the `additionalProperties`
      * keyword. For example, the Java code generator may generate 'extends HashMap'.
      */
     protected boolean supportsInheritance;
     /**
      * True if the language generator supports the 'additionalProperties' keyword
      * as sibling of a composed (allOf/anyOf/oneOf) schema.
      * Note: all language generators should support this to comply with the OAS specification.
      */
     protected boolean supportsAdditionalPropertiesWithComposedSchema;
     protected boolean supportsMixins;
     protected Map<String, String> supportedLibraries = new LinkedHashMap<>();
     protected String library;
     protected Boolean sortParamsByRequiredFlag = true;
     protected Boolean sortModelPropertiesByRequiredFlag = false;
     protected Boolean ensureUniqueParams = true;
     protected Boolean allowUnicodeIdentifiers = false;
     protected String gitHost, gitUserId, gitRepoId, releaseNote;
     protected String httpUserAgent;
     protected Boolean hideGenerationTimestamp = true;
     // How to encode special characters like $
     // They are translated to words like "Dollar" and prefixed with '
     // Then translated back during JSON encoding and decoding
     protected Map<String, String> specialCharReplacements = new HashMap<>();
     // When a model is an alias for a simple type
     protected Map<String, String> typeAliases = null;
     protected Boolean prependFormOrBodyParameters = false;
     // The extension of the generated documentation files (defaults to markdown .md)
     protected String docExtension;
     protected String ignoreFilePathOverride;
     // flag to indicate whether to use environment variable to post process file
     protected boolean enablePostProcessFile = false;
     private TemplatingEngineAdapter templatingEngine = new MustacheEngineAdapter();
     // flag to indicate whether to use the utils.OneOfImplementorAdditionalData related logic
     protected boolean useOneOfInterfaces = false;
     // whether or not the oneOf imports machinery should add oneOf interfaces as imports in implementing classes
     protected boolean addOneOfInterfaceImports = false;
     protected List<CodegenModel> addOneOfInterfaces = new ArrayList<>();
 
     // flag to indicate whether to only update files whose contents have changed
     protected boolean enableMinimalUpdate = false;
 
     // acts strictly upon a spec, potentially modifying it to have consistent behavior across generators.
     protected boolean strictSpecBehavior = true;
     // flag to indicate whether enum value prefixes are removed
     protected boolean removeEnumValuePrefix = true;
 
     // Support legacy logic for evaluating discriminators
     protected boolean legacyDiscriminatorBehavior = true;
 
     // Specify what to do if the 'additionalProperties' keyword is not present in a schema.
     // See CodegenConstants.java for more details.
     protected boolean disallowAdditionalPropertiesIfNotPresent = true;
 
     // If the server adds new enum cases, that are unknown by an old spec/client, the client will fail to parse the network response.
     // With this option enabled, each enum will have a new case, 'unknown_default_open_api', so that when the server sends an enum case that is not known by the client/spec, they can safely fallback to this case.
     protected boolean enumUnknownDefaultCase = false;
     protected String enumUnknownDefaultCaseName = "unknown_default_open_api";
 
     // make openapi available to all methods
     protected OpenAPI openAPI;
 
     // A cache to efficiently lookup a Schema instance based on the return value of `toModelName()`.
     private Map<String, Schema> modelNameToSchemaCache;
 
     // A cache to efficiently lookup schema `toModelName()` based on the schema Key
     private final Map<String, String> schemaKeyToModelNameCache = new HashMap<>();
 
     @Override
     public List<CliOption> cliOptions() {
         return cliOptions;
     }
 
     @Override
     public void processOpts() {
         if (additionalProperties.containsKey(CodegenConstants.TEMPLATE_DIR)) {
             this.setTemplateDir((String) additionalProperties.get(CodegenConstants.TEMPLATE_DIR));
         }
 
         if (additionalProperties.containsKey(CodegenConstants.MODEL_PACKAGE)) {
             this.setModelPackage((String) additionalProperties.get(CodegenConstants.MODEL_PACKAGE));
         }
 
         if (additionalProperties.containsKey(CodegenConstants.API_PACKAGE)) {
             this.setApiPackage((String) additionalProperties.get(CodegenConstants.API_PACKAGE));
         }
 
         if (additionalProperties.containsKey(CodegenConstants.HIDE_GENERATION_TIMESTAMP)) {
             setHideGenerationTimestamp(convertPropertyToBooleanAndWriteBack(CodegenConstants.HIDE_GENERATION_TIMESTAMP));
         } else {
             additionalProperties.put(CodegenConstants.HIDE_GENERATION_TIMESTAMP, hideGenerationTimestamp);
         }
 
         if (additionalProperties.containsKey(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG)) {
             this.setSortParamsByRequiredFlag(Boolean.valueOf(additionalProperties
                     .get(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG).toString()));
         }
 
         if (additionalProperties.containsKey(CodegenConstants.SORT_MODEL_PROPERTIES_BY_REQUIRED_FLAG)) {
             this.setSortModelPropertiesByRequiredFlag(Boolean.valueOf(additionalProperties
                     .get(CodegenConstants.SORT_MODEL_PROPERTIES_BY_REQUIRED_FLAG).toString()));
         }
 
         if (additionalProperties.containsKey(CodegenConstants.PREPEND_FORM_OR_BODY_PARAMETERS)) {
             this.setPrependFormOrBodyParameters(Boolean.valueOf(additionalProperties
                     .get(CodegenConstants.PREPEND_FORM_OR_BODY_PARAMETERS).toString()));
         }
 
         if (additionalProperties.containsKey(CodegenConstants.ENSURE_UNIQUE_PARAMS)) {
             this.setEnsureUniqueParams(Boolean.valueOf(additionalProperties
                     .get(CodegenConstants.ENSURE_UNIQUE_PARAMS).toString()));
         }
 
         if (additionalProperties.containsKey(CodegenConstants.ALLOW_UNICODE_IDENTIFIERS)) {
             this.setAllowUnicodeIdentifiers(Boolean.valueOf(additionalProperties
                     .get(CodegenConstants.ALLOW_UNICODE_IDENTIFIERS).toString()));
         }
 
         if (additionalProperties.containsKey(CodegenConstants.API_NAME_PREFIX)) {
             this.setApiNamePrefix((String) additionalProperties.get(CodegenConstants.API_NAME_PREFIX));
         }
 
         if (additionalProperties.containsKey(CodegenConstants.API_NAME_SUFFIX)) {
             this.setApiNameSuffix((String) additionalProperties.get(CodegenConstants.API_NAME_SUFFIX));
         }
 
         if (additionalProperties.containsKey(CodegenConstants.MODEL_NAME_PREFIX)) {
             this.setModelNamePrefix((String) additionalProperties.get(CodegenConstants.MODEL_NAME_PREFIX));
         }
 
         if (additionalProperties.containsKey(CodegenConstants.MODEL_NAME_SUFFIX)) {
             this.setModelNameSuffix((String) additionalProperties.get(CodegenConstants.MODEL_NAME_SUFFIX));
         }
 
         if (additionalProperties.containsKey(CodegenConstants.REMOVE_OPERATION_ID_PREFIX)) {
             this.setRemoveOperationIdPrefix(Boolean.parseBoolean(additionalProperties
                     .get(CodegenConstants.REMOVE_OPERATION_ID_PREFIX).toString()));
         }
 
         if (additionalProperties.containsKey(CodegenConstants.REMOVE_OPERATION_ID_PREFIX_DELIMITER)) {
             this.setRemoveOperationIdPrefixDelimiter(additionalProperties
                     .get(CodegenConstants.REMOVE_OPERATION_ID_PREFIX_DELIMITER).toString());
         }
 
         if (additionalProperties.containsKey(CodegenConstants.REMOVE_OPERATION_ID_PREFIX_COUNT)) {
             this.setRemoveOperationIdPrefixCount(Integer.parseInt(additionalProperties
                     .get(CodegenConstants.REMOVE_OPERATION_ID_PREFIX_COUNT).toString()));
         }
 
         if (additionalProperties.containsKey(CodegenConstants.SKIP_OPERATION_EXAMPLE)) {
             this.setSkipOperationExample(Boolean.parseBoolean(additionalProperties
                     .get(CodegenConstants.SKIP_OPERATION_EXAMPLE).toString()));
         }
 
         if (additionalProperties.containsKey(CodegenConstants.DOCEXTENSION)) {
             this.setDocExtension(String.valueOf(additionalProperties
                     .get(CodegenConstants.DOCEXTENSION).toString()));
         }
 
         if (additionalProperties.containsKey(CodegenConstants.ENABLE_POST_PROCESS_FILE)) {
             this.setEnablePostProcessFile(Boolean.parseBoolean(additionalProperties
                     .get(CodegenConstants.ENABLE_POST_PROCESS_FILE).toString()));
         }
 
         if (additionalProperties.containsKey(CodegenConstants.GENERATE_ALIAS_AS_MODEL)) {
             ModelUtils.setGenerateAliasAsModel(Boolean.parseBoolean(additionalProperties
                     .get(CodegenConstants.GENERATE_ALIAS_AS_MODEL).toString()));
         }
 
         if (additionalProperties.containsKey(CodegenConstants.REMOVE_ENUM_VALUE_PREFIX)) {
             this.setRemoveEnumValuePrefix(Boolean.parseBoolean(additionalProperties
                     .get(CodegenConstants.REMOVE_ENUM_VALUE_PREFIX).toString()));
         }
 
         if (additionalProperties.containsKey(CodegenConstants.LEGACY_DISCRIMINATOR_BEHAVIOR)) {
             this.setLegacyDiscriminatorBehavior(Boolean.parseBoolean(additionalProperties
                     .get(CodegenConstants.LEGACY_DISCRIMINATOR_BEHAVIOR).toString()));
         }
         if (additionalProperties.containsKey(CodegenConstants.DISALLOW_ADDITIONAL_PROPERTIES_IF_NOT_PRESENT)) {
             this.setDisallowAdditionalPropertiesIfNotPresent(Boolean.parseBoolean(additionalProperties
                     .get(CodegenConstants.DISALLOW_ADDITIONAL_PROPERTIES_IF_NOT_PRESENT).toString()));
         }
         if (additionalProperties.containsKey(CodegenConstants.ENUM_UNKNOWN_DEFAULT_CASE)) {
             this.setEnumUnknownDefaultCase(Boolean.parseBoolean(additionalProperties
                     .get(CodegenConstants.ENUM_UNKNOWN_DEFAULT_CASE).toString()));
         }
     }
 
     /***
      * Preset map builder with commonly used Mustache lambdas.
      *
      * To extend the map, override addMustacheLambdas(), call parent method
      * first and then add additional lambdas to the returned builder.
      *
      * If common lambdas are not desired, override addMustacheLambdas() method
      * and return empty builder.
      *
      * @return preinitialized map with common lambdas
      */
     protected ImmutableMap.Builder<String, Lambda> addMustacheLambdas() {
 
         return new ImmutableMap.Builder<String, Mustache.Lambda>()
                 .put("lowercase", new LowercaseLambda().generator(this))
                 .put("uppercase", new UppercaseLambda())
                 .put("snakecase", new SnakecaseLambda())
                 .put("titlecase", new TitlecaseLambda())
                 .put("camelcase", new CamelCaseLambda(true).generator(this))
                 .put("pascalcase", new CamelCaseLambda(false).generator(this))
                 .put("indented", new IndentedLambda())
                 .put("indented_8", new IndentedLambda(8, " "))
                 .put("indented_12", new IndentedLambda(12, " "))
                 .put("indented_16", new IndentedLambda(16, " "));
     }
 
     private void registerMustacheLambdas() {
         ImmutableMap<String, Lambda> lambdas = addMustacheLambdas().build();
 
         if (lambdas.size() == 0) {
             return;
         }
 
         if (additionalProperties.containsKey("lambda")) {
             LOGGER.error("A property called 'lambda' already exists in additionalProperties");
             throw new RuntimeException("A property called 'lambda' already exists in additionalProperties");
         }
         additionalProperties.put("lambda", lambdas);
     }
 
     // override with any special post-processing for all models
     @Override
     @SuppressWarnings({"static-method", "unchecked"})
     public Map<String, Object> postProcessAllModels(Map<String, Object> objs) {
         if (this.useOneOfInterfaces) {
             // First, add newly created oneOf interfaces
             for (CodegenModel cm : addOneOfInterfaces) {
                 Map<String, Object> modelValue = new HashMap<>(additionalProperties());
                 modelValue.put("model", cm);
 
                 List<Map<String, String>> importsValue = new ArrayList<>();
                 Map<String, Object> objsValue = new HashMap<>();
                 objsValue.put("models", Collections.singletonList(modelValue));
                 objsValue.put("package", modelPackage());
                 objsValue.put("imports", importsValue);
                 objsValue.put("classname", cm.classname);
                 objsValue.putAll(additionalProperties);
                 objs.put(cm.name, objsValue);
             }
 
             // Gather data from all the models that contain oneOf into OneOfImplementorAdditionalData classes
             // (see docstring of that class to find out what information is gathered and why)
             Map<String, OneOfImplementorAdditionalData> additionalDataMap = new HashMap<>();
             for (Map.Entry<String, Object> modelsEntry : objs.entrySet()) {
                 Map<String, Object> modelsAttrs = (Map<String, Object>) modelsEntry.getValue();
                 List<Object> models = (List<Object>) modelsAttrs.get("models");
                 List<Map<String, String>> modelsImports = (List<Map<String, String>>) modelsAttrs.getOrDefault("imports", new ArrayList<Map<String, String>>());
                 for (Object _mo : models) {
                     Map<String, Object> mo = (Map<String, Object>) _mo;
                     CodegenModel cm = (CodegenModel) mo.get("model");
                     if (cm.oneOf.size() > 0) {
                         cm.vendorExtensions.put("x-is-one-of-interface", true);
                         for (String one : cm.oneOf) {
                             if (!additionalDataMap.containsKey(one)) {
                                 additionalDataMap.put(one, new OneOfImplementorAdditionalData(one));
                             }
                             additionalDataMap.get(one).addFromInterfaceModel(cm, modelsImports);
                         }
                         // if this is oneOf interface, make sure we include the necessary imports for it
                         addImportsToOneOfInterface(modelsImports);
                     }
                 }
             }
 
             // Add all the data from OneOfImplementorAdditionalData classes to the implementing models
             for (Map.Entry<String, Object> modelsEntry : objs.entrySet()) {
                 Map<String, Object> modelsAttrs = (Map<String, Object>) modelsEntry.getValue();
                 List<Object> models = (List<Object>) modelsAttrs.get("models");
                 List<Map<String, String>> imports = (List<Map<String, String>>) modelsAttrs.get("imports");
                 for (Object _implmo : models) {
                     Map<String, Object> implmo = (Map<String, Object>) _implmo;
                     CodegenModel implcm = (CodegenModel) implmo.get("model");
                     String modelName = toModelName(implcm.name);
                     if (additionalDataMap.containsKey(modelName)) {
                         additionalDataMap.get(modelName).addToImplementor(this, implcm, imports, addOneOfInterfaceImports);
                     }
                 }
             }
         }
 
         return objs;
     }
 
     /**
      * Return a map from model name to Schema for efficient lookup.
      *
      * @return map from model name to Schema.
      */
     protected Map<String, Schema> getModelNameToSchemaCache() {
         if (modelNameToSchemaCache == null) {
             // Create a cache to efficiently lookup schema based on model name.
             Map<String, Schema> m = new HashMap<>();
             ModelUtils.getSchemas(openAPI).forEach((key, schema) -> m.put(toModelName(key), schema));
             modelNameToSchemaCache = Collections.unmodifiableMap(m);
         }
         return modelNameToSchemaCache;
     }
 
     /**
      * Index all CodegenModels by model name.
      *
      * @param objs Map of models
      * @return map of all models indexed by names
      */
     public Map<String, CodegenModel> getAllModels(Map<String, Object> objs) {
         Map<String, CodegenModel> allModels = new HashMap<>();
         for (Entry<String, Object> entry : objs.entrySet()) {
             String modelName = toModelName(entry.getKey());
             Map<String, Object> inner = (Map<String, Object>) entry.getValue();
             List<Map<String, Object>> models = (List<Map<String, Object>>) inner.get("models");
             for (Map<String, Object> mo : models) {
                 CodegenModel cm = (CodegenModel) mo.get("model");
                 allModels.put(modelName, cm);
             }
         }
         return allModels;
     }
 
     /**
      * Loop through all models to update different flags (e.g. isSelfReference), children models, etc
      *
      * @param objs Map of models
      * @return maps of models with various updates
      */
     @Override
     public Map<String, Object> updateAllModels(Map<String, Object> objs) {
         Map<String, CodegenModel> allModels = getAllModels(objs);
 
         // Fix up all parent and interface CodegenModel references.
         for (CodegenModel cm : allModels.values()) {
             if (cm.getParent() != null) {
                 cm.setParentModel(allModels.get(cm.getParent()));
             }
             if (cm.getInterfaces() != null && !cm.getInterfaces().isEmpty()) {
                 cm.setInterfaceModels(new ArrayList<>(cm.getInterfaces().size()));
                 for (String intf : cm.getInterfaces()) {
                     CodegenModel intfModel = allModels.get(intf);
                     if (intfModel != null) {
                         cm.getInterfaceModels().add(intfModel);
                     }
                 }
             }
         }
 
         // Let parent know about all its children
         for (Map.Entry<String, CodegenModel> allModelsEntry : allModels.entrySet()) {
             String name = allModelsEntry.getKey();
             CodegenModel cm = allModelsEntry.getValue();
             CodegenModel parent = allModels.get(cm.getParent());
             // if a discriminator exists on the parent, don't add this child to the inheritance hierarchy
             // TODO Determine what to do if the parent discriminator name == the grandparent discriminator name
             while (parent != null) {
                 if (parent.getChildren() == null) {
                     parent.setChildren(new ArrayList<>());
                 }
                 parent.getChildren().add(cm);
                 parent.hasChildren = true;
                 Schema parentSchema = this.openAPI.getComponents().getSchemas().get(parent.name);
                 if (parentSchema.getDiscriminator() == null) {
                     parent = allModels.get(parent.getParent());
                 } else {
                     parent = null;
                 }
             }
         }
 
         // loop through properties of each model to detect self-reference
         for (Map.Entry<String, Object> entry : objs.entrySet()) {
             Map<String, Object> inner = (Map<String, Object>) entry.getValue();
             List<Map<String, Object>> models = (List<Map<String, Object>>) inner.get("models");
             for (Map<String, Object> mo : models) {
                 CodegenModel cm = (CodegenModel) mo.get("model");
                 removeSelfReferenceImports(cm);
             }
         }
         setCircularReferences(allModels);
 
         return objs;
     }
 
     /**
      * Removes imports from the model that points to itself
      * Marks a self referencing property, if detected
      *
      * @param model Self imports will be removed from this model.imports collection
      */
     protected void removeSelfReferenceImports(CodegenModel model) {
         for (CodegenProperty cp : model.allVars) {
             // detect self import
             if (cp.dataType.equalsIgnoreCase(model.classname) ||
                     (cp.isContainer && cp.items != null && cp.items.dataType.equalsIgnoreCase(model.classname))) {
                 model.imports.remove(model.classname); // remove self import
                 cp.isSelfReference = true;
             }
         }
     }
 
     public void setCircularReferences(Map<String, CodegenModel> models) {
         final Map<String, List<CodegenProperty>> dependencyMap = models.entrySet().stream()
                 .collect(Collectors.toMap(Entry::getKey, entry -> getModelDependencies(entry.getValue())));
 
         models.keySet().forEach(name -> setCircularReferencesOnProperties(name, dependencyMap));
     }
 
     private List<CodegenProperty> getModelDependencies(CodegenModel model) {
         return model.getAllVars().stream()
                 .map(prop -> {
                     if (prop.isContainer) {
                         return prop.items.dataType == null ? null : prop;
                     }
                     return prop.dataType == null ? null : prop;
                 })
                 .filter(Objects::nonNull)
                 .collect(Collectors.toList());
     }
 
     private void setCircularReferencesOnProperties(final String root,
                                                    final Map<String, List<CodegenProperty>> dependencyMap) {
         dependencyMap.getOrDefault(root, new ArrayList<>())
                 .forEach(prop -> {
                     final List<String> unvisited =
                             Collections.singletonList(prop.isContainer ? prop.items.dataType : prop.dataType);
                     prop.isCircularReference = isCircularReference(root,
                             new HashSet<>(),
                             new ArrayList<>(unvisited),
                             dependencyMap);
                 });
     }
 
     private boolean isCircularReference(final String root,
                                         final Set<String> visited,
                                         final List<String> unvisited,
                                         final Map<String, List<CodegenProperty>> dependencyMap) {
         for (int i = 0; i < unvisited.size(); i++) {
             final String next = unvisited.get(i);
             if (!visited.contains(next)) {
                 if (next.equals(root)) {
                     return true;
                 }
                 dependencyMap.getOrDefault(next, new ArrayList<>())
                         .forEach(prop -> unvisited.add(prop.isContainer ? prop.items.dataType : prop.dataType));
                 visited.add(next);
             }
         }
         return false;
     }
 
     // override with any special post-processing
     @Override
     @SuppressWarnings("static-method")
     public Map<String, Object> postProcessModels(Map<String, Object> objs) {
         return objs;
     }
 
     /**
      * post process enum defined in model's properties
      *
      * @param objs Map of models
      * @return maps of models with better enum support
      */
     public Map<String, Object> postProcessModelsEnum(Map<String, Object> objs) {
         List<Object> models = (List<Object>) objs.get("models");
         for (Object _mo : models) {
             Map<String, Object> mo = (Map<String, Object>) _mo;
             CodegenModel cm = (CodegenModel) mo.get("model");
 
             // for enum model
             if (Boolean.TRUE.equals(cm.isEnum) && cm.allowableValues != null) {
                 Map<String, Object> allowableValues = cm.allowableValues;
                 List<Object> values = (List<Object>) allowableValues.get("values");
                 List<Map<String, Object>> enumVars = buildEnumVars(values, cm.dataType);
                 // if "x-enum-varnames" or "x-enum-descriptions" defined, update varnames
                 updateEnumVarsWithExtensions(enumVars, cm.getVendorExtensions(), cm.dataType);
                 cm.allowableValues.put("enumVars", enumVars);
             }
 
             // update codegen property enum with proper naming convention
             // and handling of numbers, special characters
             for (CodegenProperty var : cm.vars) {
                 updateCodegenPropertyEnum(var);
             }
 
             for (CodegenProperty var : cm.allVars) {
                 updateCodegenPropertyEnum(var);
             }
 
             for (CodegenProperty var : cm.requiredVars) {
                 updateCodegenPropertyEnum(var);
             }
 
             for (CodegenProperty var : cm.optionalVars) {
                 updateCodegenPropertyEnum(var);
             }
 
             for (CodegenProperty var : cm.parentVars) {
                 updateCodegenPropertyEnum(var);
             }
 
             for (CodegenProperty var : cm.readOnlyVars) {
                 updateCodegenPropertyEnum(var);
             }
 
             for (CodegenProperty var : cm.readWriteVars) {
                 updateCodegenPropertyEnum(var);
             }
 
         }
         return objs;
     }
 
     /**
      * Returns the common prefix of variables for enum naming if
      * two or more variables are present
      *
      * @param vars List of variable names
      * @return the common prefix for naming
      */
     public String findCommonPrefixOfVars(List<Object> vars) {
         if (vars.size() > 1) {
             try {
                 String[] listStr = vars.toArray(new String[vars.size()]);
                 String prefix = StringUtils.getCommonPrefix(listStr);
                 // exclude trailing characters that should be part of a valid variable
                 // e.g. ["status-on", "status-off"] => "status-" (not "status-o")
                 final Matcher matcher = COMMON_PREFIX_ENUM_NAME.matcher(prefix);
                 return matcher.replaceAll("");
             } catch (ArrayStoreException e) {
                 // do nothing, just return default value
             }
         }
         return "";
     }
 
     /**
      * Return the enum default value in the language specified format
      *
      * @param value    enum variable name
      * @param datatype data type
      * @return the default value for the enum
      */
     public String toEnumDefaultValue(String value, String datatype) {
         return datatype + "." + value;
     }
 
     /**
      * Return the enum value in the language specified format
      * e.g. status becomes "status"
      *
      * @param value    enum variable name
      * @param datatype data type
      * @return the sanitized value for enum
      */
     public String toEnumValue(String value, String datatype) {
         if ("number".equalsIgnoreCase(datatype) || "boolean".equalsIgnoreCase(datatype)) {
             return value;
         } else {
             return "\"" + escapeText(value) + "\"";
         }
     }
 
     /**
      * Return the sanitized variable name for enum
      *
      * @param value    enum variable name
      * @param datatype data type
      * @return the sanitized variable name for enum
      */
     public String toEnumVarName(String value, String datatype) {
         if (value.length() == 0) {
             return "EMPTY";
         }
 
         String var = value.replaceAll("\\W+", "_").toUpperCase(Locale.ROOT);
         if (var.matches("\\d.*")) {
             return "_" + var;
         } else {
             return var;
         }
     }
 
     /**
      * Set the OpenAPI document.
      * This method is invoked when the input OpenAPI document has been parsed and validated.
      */
     @Override
     public void setOpenAPI(OpenAPI openAPI) {
         this.openAPI = openAPI;
         // Set global settings such that helper functions in ModelUtils can lookup the value
         // of the CLI option.
         ModelUtils.setDisallowAdditionalPropertiesIfNotPresent(getDisallowAdditionalPropertiesIfNotPresent());
     }
 
     // override with any message to be shown right before the process finishes
     @Override
     @SuppressWarnings("static-method")
     public void postProcess() {
         System.out.println("################################################################################");
         System.out.println("# Thanks for using OpenAPI Generator.                                          #");
         System.out.println("# Please consider donation to help us maintain this project \uD83D\uDE4F                 #");
         System.out.println("# https://opencollective.com/openapi_generator/donate                          #");
         System.out.println("################################################################################");
     }
 
     // override with any special post-processing
     @Override
     @SuppressWarnings("static-method")
     public Map<String, Object> postProcessOperationsWithModels(Map<String, Object> objs, List<Object> allModels) {
         return objs;
     }
 
     // override with any special post-processing
     @Override
     @SuppressWarnings("static-method")
     public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
         return objs;
     }
 
     // override to post-process any model properties
     @Override
     @SuppressWarnings("unused")
     public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
     }
 
     // override to post-process any parameters
     @Override
     @SuppressWarnings("unused")
     public void postProcessParameter(CodegenParameter parameter) {
     }
 
     //override with any special handling of the entire OpenAPI spec document
     @Override
     @SuppressWarnings("unused")
     public void preprocessOpenAPI(OpenAPI openAPI) {
         if (useOneOfInterfaces) {
             // we process the openapi schema here to find oneOf schemas and create interface models for them
             Map<String, Schema> schemas = new HashMap<>(openAPI.getComponents().getSchemas());
             if (schemas == null) {
                 schemas = new HashMap<>();
             }
             Map<String, PathItem> pathItems = openAPI.getPaths();
 
             // we need to add all request and response bodies to processed schemas
             if (pathItems != null) {
                 for (Map.Entry<String, PathItem> e : pathItems.entrySet()) {
                     for (Map.Entry<PathItem.HttpMethod, Operation> op : e.getValue().readOperationsMap().entrySet()) {
                         String opId = getOrGenerateOperationId(op.getValue(), e.getKey(), op.getKey().toString());
                         // process request body
                         RequestBody b = ModelUtils.getReferencedRequestBody(openAPI, op.getValue().getRequestBody());
                         Schema requestSchema = null;
                         if (b != null) {
                             requestSchema = ModelUtils.getSchemaFromRequestBody(b);
                         }
                         if (requestSchema != null) {
                             schemas.put(opId, requestSchema);
                         }
                         // process all response bodies
                         if (op.getValue().getResponses() != null) {
                             for (Map.Entry<String, ApiResponse> ar : op.getValue().getResponses().entrySet()) {
                                 ApiResponse a = ModelUtils.getReferencedApiResponse(openAPI, ar.getValue());
                                 Schema responseSchema = ModelUtils.getSchemaFromResponse(a);
                                 if (responseSchema != null) {
                                     schemas.put(opId + ar.getKey(), responseSchema);
                                 }
                             }
                         }
                     }
                 }
             }
 
             // also add all properties of all schemas to be checked for oneOf
             Map<String, Schema> propertySchemas = new HashMap<>();
             for (Map.Entry<String, Schema> e : schemas.entrySet()) {
                 Schema s = e.getValue();
                 Map<String, Schema> props = s.getProperties();
                 if (props == null) {
                     props = new HashMap<>();
                 }
                 for (Map.Entry<String, Schema> p : props.entrySet()) {
                     propertySchemas.put(e.getKey() + "/" + p.getKey(), p.getValue());
                 }
             }
             schemas.putAll(propertySchemas);
 
             // go through all gathered schemas and add them as interfaces to be created
             for (Map.Entry<String, Schema> e : schemas.entrySet()) {
                 String n = toModelName(e.getKey());
                 Schema s = e.getValue();
                 String nOneOf = toModelName(n + "OneOf");
                 if (ModelUtils.isComposedSchema(s)) {
                     if (e.getKey().contains("/")) {
                         // if this is property schema, we also need to generate the oneOf interface model
                         addOneOfNameExtension((ComposedSchema) s, nOneOf);
                         addOneOfInterfaceModel((ComposedSchema) s, nOneOf, openAPI);
                     } else {
                         // else this is a component schema, so we will just use that as the oneOf interface model
                         addOneOfNameExtension((ComposedSchema) s, n);
                     }
                 } else if (ModelUtils.isArraySchema(s)) {
                     Schema items = ((ArraySchema) s).getItems();
                     if (ModelUtils.isComposedSchema(items)) {
                         addOneOfNameExtension((ComposedSchema) items, nOneOf);
                         addOneOfInterfaceModel((ComposedSchema) items, nOneOf, openAPI);
                     }
                 } else if (ModelUtils.isMapSchema(s)) {
                     Schema addProps = getAdditionalProperties(s);
                     if (addProps != null && ModelUtils.isComposedSchema(addProps)) {
                         addOneOfNameExtension((ComposedSchema) addProps, nOneOf);
                         addOneOfInterfaceModel((ComposedSchema) addProps, nOneOf, openAPI);
                     }
                 }
             }
         }
     }
 
     // override with any special handling of the entire OpenAPI spec document
     @Override
     @SuppressWarnings("unused")
     public void processOpenAPI(OpenAPI openAPI) {
     }
 
     // override with any special handling of the JMustache compiler
     @Override
     @SuppressWarnings("unused")
     public Compiler processCompiler(Compiler compiler) {
         return compiler;
     }
 
     // override with any special handling for the templating engine
     @Override
     @SuppressWarnings("unused")
     public TemplatingEngineAdapter processTemplatingEngine(TemplatingEngineAdapter templatingEngine) {
         return templatingEngine;
     }
 
     // override with any special text escaping logic
     @Override
     @SuppressWarnings("static-method")
     public String escapeText(String input) {
         if (input == null) {
             return input;
         }
 
         // remove \t, \n, \r
         // replace \ with \\
         // replace " with \"
         // outer unescape to retain the original multi-byte characters
         // finally escalate characters avoiding code injection
         return escapeUnsafeCharacters(
                 StringEscapeUtils.unescapeJava(
                         StringEscapeUtils.escapeJava(input)
                                 .replace("\\/", "/"))
                         .replaceAll("[\\t\\n\\r]", " ")
                         .replace("\\", "\\\\")
                         .replace("\"", "\\\""));
     }
 
     /**
      * Escape characters while allowing new lines
      *
      * @param input String to be escaped
      * @return escaped string
      */
     @Override
     public String escapeTextWhileAllowingNewLines(String input) {
         if (input == null) {
             return input;
         }
 
         // remove \t
         // replace \ with \\
         // replace " with \"
         // outer unescape to retain the original multi-byte characters
         // finally escalate characters avoiding code injection
         return escapeUnsafeCharacters(
                 StringEscapeUtils.unescapeJava(
                         StringEscapeUtils.escapeJava(input)
                                 .replace("\\/", "/"))
                         .replaceAll("[\\t]", " ")
                         .replace("\\", "\\\\")
                         .replace("\"", "\\\""));
     }
 
     // override with any special encoding and escaping logic
     @Override
     @SuppressWarnings("static-method")
     public String encodePath(String input) {
         return escapeText(input);
     }
 
     /**
      * override with any special text escaping logic to handle unsafe
      * characters so as to avoid code injection
      *
      * @param input String to be cleaned up
      * @return string with unsafe characters removed or escaped
      */
     @Override
     public String escapeUnsafeCharacters(String input) {
         LOGGER.warn("escapeUnsafeCharacters should be overridden in the code generator with proper logic to escape " +
                 "unsafe characters");
         // doing nothing by default and code generator should implement
         // the logic to prevent code injection
         // later we'll make this method abstract to make sure
         // code generator implements this method
         return input;
     }
 
     /**
      * Escape single and/or double quote to avoid code injection
      *
      * @param input String to be cleaned up
      * @return string with quotation mark removed or escaped
      */
     @Override
     public String escapeQuotationMark(String input) {
         LOGGER.warn("escapeQuotationMark should be overridden in the code generator with proper logic to escape " +
                 "single/double quote");
         return input.replace("\"", "\\\"");
     }
 
     @Override
     public Set<String> defaultIncludes() {
         return defaultIncludes;
     }
 
     @Override
     public Map<String, String> typeMapping() {
         return typeMapping;
     }
 
     @Override
     public Map<String, String> instantiationTypes() {
         return instantiationTypes;
     }
 
     @Override
     public Set<String> reservedWords() {
         return reservedWords;
     }
 
     @Override
     public Set<String> languageSpecificPrimitives() {
         return languageSpecificPrimitives;
     }
 
     @Override
     public Map<String, String> importMapping() {
         return importMapping;
     }
 
     @Override
     public String testPackage() {
         return testPackage;
     }
 
     @Override
     public String modelPackage() {
         return modelPackage;
     }
 
     @Override
     public String apiPackage() {
         return apiPackage;
     }
 
     @Override
     public String fileSuffix() {
         return fileSuffix;
     }
 
     @Override
     public String templateDir() {
         return templateDir;
     }
 
     @Override
     public String embeddedTemplateDir() {
         if (embeddedTemplateDir != null) {
             return embeddedTemplateDir;
         } else {
             return templateDir;
         }
     }
 
     @Override
     public Map<String, String> apiDocTemplateFiles() {
         return apiDocTemplateFiles;
     }
 
     @Override
     public Map<String, String> modelDocTemplateFiles() {
         return modelDocTemplateFiles;
     }
 
     @Override
     public Map<String, String> reservedWordsMappings() {
         return reservedWordsMappings;
     }
 
     @Override
     public Map<String, String> apiTestTemplateFiles() {
         return apiTestTemplateFiles;
     }
 
     @Override
     public Map<String, String> modelTestTemplateFiles() {
         return modelTestTemplateFiles;
     }
 
     @Override
     public Map<String, String> apiTemplateFiles() {
         return apiTemplateFiles;
     }
 
     @Override
     public Map<String, String> modelTemplateFiles() {
         return modelTemplateFiles;
     }
 
     @Override
     public String apiFileFolder() {
         return outputFolder + File.separator + apiPackage().replace('.', File.separatorChar);
     }
 
     @Override
     public String modelFileFolder() {
         return outputFolder + File.separator + modelPackage().replace('.', File.separatorChar);
     }
 
     @Override
     public String apiTestFileFolder() {
         return outputFolder + File.separator + testPackage().replace('.', File.separatorChar);
     }
 
     @Override
     public String modelTestFileFolder() {
         return outputFolder + File.separator + testPackage().replace('.', File.separatorChar);
     }
 
     @Override
     public String apiDocFileFolder() {
         return outputFolder;
     }
 
     @Override
     public String modelDocFileFolder() {
         return outputFolder;
     }
 
     @Override
     public Map<String, Object> additionalProperties() {
         return additionalProperties;
     }
 
     @Override
     public Map<String, String> serverVariableOverrides() {
         return serverVariables;
     }
 
     @Override
     public Map<String, Object> vendorExtensions() {
         return vendorExtensions;
     }
 
     @Override
     public List<SupportingFile> supportingFiles() {
         return supportingFiles;
     }
 
     @Override
     public String outputFolder() {
         return outputFolder;
     }
 
     @Override
     public void setOutputDir(String dir) {
         this.outputFolder = dir;
     }
 
     @Override
     public String getOutputDir() {
         return outputFolder();
     }
 
     @Override
     public String getInputSpec() {
         return inputSpec;
     }
 
     @Override
     public void setInputSpec(String inputSpec) {
         this.inputSpec = inputSpec;
     }
 
     @Override
     public String getFilesMetadataFilename() {
         return filesMetadataFilename;
     }
 
     public void setFilesMetadataFilename(String filesMetadataFilename) {
         this.filesMetadataFilename = filesMetadataFilename;
     }
 
     @Override
     public String getVersionMetadataFilename() {
         return versionMetadataFilename;
     }
 
     public void setVersionMetadataFilename(String versionMetadataFilename) {
         this.versionMetadataFilename = versionMetadataFilename;
     }
 
     public void setTemplateDir(String templateDir) {
         this.templateDir = templateDir;
     }
 
     public void setModelPackage(String modelPackage) {
         this.modelPackage = modelPackage;
     }
 
     public String getModelNamePrefix() {
         return modelNamePrefix;
     }
 
     public void setModelNamePrefix(String modelNamePrefix) {
         this.modelNamePrefix = modelNamePrefix;
     }
 
     public String getModelNameSuffix() {
         return modelNameSuffix;
     }
 
     public void setModelNameSuffix(String modelNameSuffix) {
         this.modelNameSuffix = modelNameSuffix;
     }
 
     public String getApiNameSuffix() {
         return apiNameSuffix;
     }
 
     public void setApiNameSuffix(String apiNameSuffix) {
         this.apiNameSuffix = apiNameSuffix;
     }
 
     public String getApiNamePrefix() {
         return apiNamePrefix;
     }
 
     public void setApiNamePrefix(String apiNamePrefix) {
         this.apiNamePrefix = apiNamePrefix;
     }
 
     public void setApiPackage(String apiPackage) {
         this.apiPackage = apiPackage;
     }
 
     public Boolean getSortParamsByRequiredFlag() {
         return sortParamsByRequiredFlag;
     }
 
     public void setSortParamsByRequiredFlag(Boolean sortParamsByRequiredFlag) {
         this.sortParamsByRequiredFlag = sortParamsByRequiredFlag;
     }
 
     public Boolean getSortModelPropertiesByRequiredFlag() {
         return sortModelPropertiesByRequiredFlag;
     }
 
     public void setSortModelPropertiesByRequiredFlag(Boolean sortModelPropertiesByRequiredFlag) {
         this.sortModelPropertiesByRequiredFlag = sortModelPropertiesByRequiredFlag;
     }
 
     public Boolean getPrependFormOrBodyParameters() {
         return prependFormOrBodyParameters;
     }
 
     public void setPrependFormOrBodyParameters(Boolean prependFormOrBodyParameters) {
         this.prependFormOrBodyParameters = prependFormOrBodyParameters;
     }
 
     public Boolean getEnsureUniqueParams() {
         return ensureUniqueParams;
     }
 
     public void setEnsureUniqueParams(Boolean ensureUniqueParams) {
         this.ensureUniqueParams = ensureUniqueParams;
     }
 
     public Boolean getLegacyDiscriminatorBehavior() {
         return legacyDiscriminatorBehavior;
     }
 
     public void setLegacyDiscriminatorBehavior(boolean val) {
         this.legacyDiscriminatorBehavior = val;
     }
 
     public Boolean getDisallowAdditionalPropertiesIfNotPresent() {
         return disallowAdditionalPropertiesIfNotPresent;
     }
 
     public void setDisallowAdditionalPropertiesIfNotPresent(boolean val) {
         this.disallowAdditionalPropertiesIfNotPresent = val;
     }
 
     public Boolean getEnumUnknownDefaultCase() {
         return enumUnknownDefaultCase;
     }
 
     public void setEnumUnknownDefaultCase(boolean val) {
         this.enumUnknownDefaultCase = val;
     }
 
     public Boolean getAllowUnicodeIdentifiers() {
         return allowUnicodeIdentifiers;
     }
 
     public void setAllowUnicodeIdentifiers(Boolean allowUnicodeIdentifiers) {
         this.allowUnicodeIdentifiers = allowUnicodeIdentifiers;
     }
 
     public Boolean getUseOneOfInterfaces() {
         return useOneOfInterfaces;
     }
 
     public void setUseOneOfInterfaces(Boolean useOneOfInterfaces) {
         this.useOneOfInterfaces = useOneOfInterfaces;
     }
 
     /**
      * Return the regular expression/JSON schema pattern (http://json-schema.org/latest/json-schema-validation.html#anchor33)
      *
      * @param pattern the pattern (regular expression)
      * @return properly-escaped pattern
      */
     public String toRegularExpression(String pattern) {
         return addRegularExpressionDelimiter(escapeText(pattern));
     }
 
     /**
      * Return the file name of the Api Test
      *
      * @param name the file name of the Api
      * @return the file name of the Api
      */
     @Override
     public String toApiFilename(String name) {
         return toApiName(name);
     }
 
     /**
      * Return the file name of the Api Documentation
      *
      * @param name the file name of the Api
      * @return the file name of the Api
      */
     @Override
     public String toApiDocFilename(String name) {
         return toApiName(name);
     }
 
     /**
      * Return the file name of the Api Test
      *
      * @param name the file name of the Api
      * @return the file name of the Api
      */
     @Override
     public String toApiTestFilename(String name) {
         return toApiName(name) + "Test";
     }
 
     /**
      * Return the variable name in the Api
      *
      * @param name the variable name of the Api
      * @return the snake-cased variable name
      */
     @Override
     public String toApiVarName(String name) {
         return lowerCamelCase(name);
     }
 
     /**
      * Return the capitalized file name of the model
      *
      * @param name the model name
      * @return the file name of the model
      */
     @Override
     public String toModelFilename(String name) {
         return camelize(name);
     }
 
     /**
      * Return the capitalized file name of the model test
      *
      * @param name the model name
      * @return the file name of the model
      */
     @Override
     public String toModelTestFilename(String name) {
         return camelize(name) + "Test";
     }
 
     /**
      * Return the capitalized file name of the model documentation
      *
      * @param name the model name
      * @return the file name of the model
      */
     @Override
     public String toModelDocFilename(String name) {
         return camelize(name);
     }
 
     /**
      * Returns metadata about the generator.
      *
      * @return A provided {@link GeneratorMetadata} instance
      */
     @Override
     public GeneratorMetadata getGeneratorMetadata() {
         return generatorMetadata;
     }
 
     /**
      * Return the operation ID (method name)
      *
      * @param operationId operation ID
      * @return the sanitized method name
      */
     @SuppressWarnings("static-method")
     public String toOperationId(String operationId) {
         // throw exception if method name is empty
         if (StringUtils.isEmpty(operationId)) {
             throw new RuntimeException("Empty method name (operationId) not allowed");
         }
 
         return operationId;
     }
 
 
/** Return the variable name by removing invalid characters and proper escaping if  it's a reserved word. */
 public String toVarName(final String name){
        return underscore(name);
    }
    
        /**
        * Return the variable name by removing invalid characters and proper escaping if  it's a reserved word.
        *
        * @param name the variable name
        * @return the variable name
        */
        @Override
        public String toVarName(String name, boolean isBoolean) {
            return underscore(name);
        }
    
        /**
        * Return the variable name by removing invalid characters and proper escaping if  it's a reserved word.
        *
        * @param name the variable name
        * @return the variable name
        */
        @Override
        public String toVarName(String name, boolean isBoolean, boolean isList) {
            return underscore(name);
        }
    
        /**
        * Return the variable name by removing invalid characters and proper escaping if  it's a reserved word.
        *
        * @param name the variable name
        * @return the variable name
        */
        @Override
        public String toVarName(String name, boolean isBoolean, boolean isList, boolean isMap) {
            return underscore(name);
        }
    
        /**
        * Return the variable name by removing invalid characters and proper escaping if  it's a reserved word.
        *
        * @param name the variable name
        * @return the variable name
        */
        @Override
        public String toVarName(String name, boolean isBoolean, boolean isList, boolean isMap, boolean isDate) {
            return underscore(name);
        }
    
        /**
        * Return the variable name by removing invalid characters and proper escaping if  it's a reserved word.
        *
        * @param name the variable name
        * @return the variable name
        */
        @Override
        public String toVarName(String name, boolean isBoolean, boolean isList, boolean isMap, boolean isDate, boolean isByteArray) {
            return underscore(name);
        }
    
        /**
        * Return the variable name by removing invalid characters and proper escaping if  it's a reserved word.
        *
        * @param name the variable name
        * @return the       
 }

 

}