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
 
     /**
      * Return the variable name by removing invalid characters and proper escaping if
      * it's a reserved word.
      *
      * @param name the variable name
      * @return the sanitized variable name
      */
     public String toVarName(final String name) {
         if (reservedWords.contains(name)) {
             return escapeReservedWord(name);
         } else if (name.chars().anyMatch(character -> specialCharReplacements.containsKey(String.valueOf((char) character)))) {
             return escape(name, specialCharReplacements, null, null);
         }
         return name;
     }
 
     /**
      * Return the parameter name by removing invalid characters and proper escaping if
      * it's a reserved word.
      *
      * @param name Codegen property object
      * @return the sanitized parameter name
      */
     @Override
     public String toParamName(String name) {
         name = removeNonNameElementToCamelCase(name); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.
         if (reservedWords.contains(name)) {
             return escapeReservedWord(name);
         } else if (name.chars().anyMatch(character -> specialCharReplacements.containsKey(String.valueOf((char) character)))) {
             return escape(name, specialCharReplacements, null, null);
         }
         return name;
 
     }
 
     /**
      * Return the parameter name of array of model
      *
      * @param name name of the array model
      * @return the sanitized parameter name
      */
     public String toArrayModelParamName(String name) {
         return toParamName(name);
     }
 
     /**
      * Return the Enum name (e.g. StatusEnum given 'status')
      *
      * @param property Codegen property
      * @return the Enum name
      */
     @SuppressWarnings("static-method")
     public String toEnumName(CodegenProperty property) {
         return StringUtils.capitalize(property.name) + "Enum";
     }
 
     /**
      * Return the escaped name of the reserved word
      *
      * @param name the name to be escaped
      * @return the escaped reserved word
      * <p>
      * throws Runtime exception as reserved word is not allowed (default behavior)
      */
     @Override
     @SuppressWarnings("static-method")
     public String escapeReservedWord(String name) {
         throw new RuntimeException("reserved word " + name + " not allowed");
     }
 
     /**
      * Return the fully-qualified "Model" name for import
      *
      * @param name the name of the "Model"
      * @return the fully-qualified "Model" name for import
      */
     @Override
     public String toModelImport(String name) {
         if ("".equals(modelPackage())) {
             return name;
         } else {
             return modelPackage() + "." + name;
         }
     }
 
     /**
      * Returns the same content as [[toModelImport]] with key the fully-qualified Model name and value the initial input.
      * In case of union types this method has a key for each separate model and import.
      * @param name the name of the "Model"
      * @return Map of fully-qualified models.
      */
     @Override
     public Map<String,String> toModelImportMap(String name){
         return Collections.singletonMap(this.toModelImport(name),name);
     }
 
     /**
      * Return the fully-qualified "Api" name for import
      *
      * @param name the name of the "Api"
      * @return the fully-qualified "Api" name for import
      */
     @Override
     public String toApiImport(String name) {
         return apiPackage() + "." + name;
     }
 
     /**
      * Default constructor.
      * This method will map between OAS type and language-specified type, as well as mapping
      * between OAS type and the corresponding import statement for the language. This will
      * also add some language specified CLI options, if any.
      * returns string presentation of the example path (it's a constructor)
      */
     public DefaultCodegen() {
         CodegenType codegenType = getTag();
         if (codegenType == null) {
             codegenType = CodegenType.OTHER;
         }
 
         generatorMetadata = GeneratorMetadata.newBuilder()
                 .stability(Stability.STABLE)
                 .featureSet(DefaultFeatureSet)
                 .generationMessage(String.format(Locale.ROOT, "OpenAPI Generator: %s (%s)", getName(), codegenType.toValue()))
                 .build();
 
         defaultIncludes = new HashSet<>(
                 Arrays.asList("double",
                         "int",
                         "long",
                         "short",
                         "char",
                         "float",
                         "String",
                         "boolean",
                         "Boolean",
                         "Double",
                         "Void",
                         "Integer",
                         "Long",
                         "Float")
         );
 
         typeMapping = new HashMap<>();
         typeMapping.put("array", "List");
         typeMapping.put("set", "Set");
         typeMapping.put("map", "Map");
         typeMapping.put("boolean", "Boolean");
         typeMapping.put("string", "String");
         typeMapping.put("int", "Integer");
         typeMapping.put("float", "Float");
         typeMapping.put("double", "Double");
         typeMapping.put("number", "BigDecimal");
         typeMapping.put("decimal", "BigDecimal");
         typeMapping.put("DateTime", "Date");
         typeMapping.put("long", "Long");
         typeMapping.put("short", "Short");
         typeMapping.put("char", "String");
         typeMapping.put("object", "Object");
         typeMapping.put("integer", "Integer");
         // FIXME: java specific type should be in Java Based Abstract Impl's
         typeMapping.put("ByteArray", "byte[]");
         typeMapping.put("binary", "File");
         typeMapping.put("file", "File");
         typeMapping.put("UUID", "UUID");
         typeMapping.put("URI", "URI");
         typeMapping.put("AnyType", "oas_any_type_not_mapped");
 
         instantiationTypes = new HashMap<>();
 
         reservedWords = new HashSet<>();
 
         cliOptions.add(CliOption.newBoolean(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG,
                 CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG_DESC).defaultValue(Boolean.TRUE.toString()));
         cliOptions.add(CliOption.newBoolean(CodegenConstants.SORT_MODEL_PROPERTIES_BY_REQUIRED_FLAG,
                 CodegenConstants.SORT_MODEL_PROPERTIES_BY_REQUIRED_FLAG_DESC).defaultValue(Boolean.TRUE.toString()));
         cliOptions.add(CliOption.newBoolean(CodegenConstants.ENSURE_UNIQUE_PARAMS, CodegenConstants
                 .ENSURE_UNIQUE_PARAMS_DESC).defaultValue(Boolean.TRUE.toString()));
         // name formatting options
         cliOptions.add(CliOption.newBoolean(CodegenConstants.ALLOW_UNICODE_IDENTIFIERS, CodegenConstants
                 .ALLOW_UNICODE_IDENTIFIERS_DESC).defaultValue(Boolean.FALSE.toString()));
         // option to change the order of form/body parameter
         cliOptions.add(CliOption.newBoolean(CodegenConstants.PREPEND_FORM_OR_BODY_PARAMETERS,
                 CodegenConstants.PREPEND_FORM_OR_BODY_PARAMETERS_DESC).defaultValue(Boolean.FALSE.toString()));
 
         // option to change how we process + set the data in the discriminator mapping
         CliOption legacyDiscriminatorBehaviorOpt = CliOption.newBoolean(CodegenConstants.LEGACY_DISCRIMINATOR_BEHAVIOR, CodegenConstants.LEGACY_DISCRIMINATOR_BEHAVIOR_DESC).defaultValue(Boolean.TRUE.toString());
         Map<String, String> legacyDiscriminatorBehaviorOpts = new HashMap<>();
         legacyDiscriminatorBehaviorOpts.put("true", "The mapping in the discriminator includes descendent schemas that allOf inherit from self and the discriminator mapping schemas in the OAS document.");
         legacyDiscriminatorBehaviorOpts.put("false", "The mapping in the discriminator includes any descendent schemas that allOf inherit from self, any oneOf schemas, any anyOf schemas, any x-discriminator-values, and the discriminator mapping schemas in the OAS document AND Codegen validates that oneOf and anyOf schemas contain the required discriminator and throws an error if the discriminator is missing.");
         legacyDiscriminatorBehaviorOpt.setEnum(legacyDiscriminatorBehaviorOpts);
         cliOptions.add(legacyDiscriminatorBehaviorOpt);
 
         // option to change how we process + set the data in the 'additionalProperties' keyword.
         CliOption disallowAdditionalPropertiesIfNotPresentOpt = CliOption.newBoolean(
                 CodegenConstants.DISALLOW_ADDITIONAL_PROPERTIES_IF_NOT_PRESENT,
                 CodegenConstants.DISALLOW_ADDITIONAL_PROPERTIES_IF_NOT_PRESENT_DESC).defaultValue(Boolean.TRUE.toString());
         Map<String, String> disallowAdditionalPropertiesIfNotPresentOpts = new HashMap<>();
         disallowAdditionalPropertiesIfNotPresentOpts.put("false",
                 "The 'additionalProperties' implementation is compliant with the OAS and JSON schema specifications.");
         disallowAdditionalPropertiesIfNotPresentOpts.put("true",
                 "Keep the old (incorrect) behaviour that 'additionalProperties' is set to false by default.");
         disallowAdditionalPropertiesIfNotPresentOpt.setEnum(disallowAdditionalPropertiesIfNotPresentOpts);
         cliOptions.add(disallowAdditionalPropertiesIfNotPresentOpt);
         this.setDisallowAdditionalPropertiesIfNotPresent(true);
 
         CliOption enumUnknownDefaultCaseOpt = CliOption.newBoolean(
                 CodegenConstants.ENUM_UNKNOWN_DEFAULT_CASE,
                 CodegenConstants.ENUM_UNKNOWN_DEFAULT_CASE_DESC).defaultValue(Boolean.FALSE.toString());
         Map<String, String> enumUnknownDefaultCaseOpts = new HashMap<>();
         enumUnknownDefaultCaseOpts.put("false",
                 "No changes to the enum's are made, this is the default option.");
         enumUnknownDefaultCaseOpts.put("true",
                 "With this option enabled, each enum will have a new case, 'unknown_default_open_api', so that when the enum case sent by the server is not known by the client/spec, can safely be decoded to this case.");
         enumUnknownDefaultCaseOpt.setEnum(enumUnknownDefaultCaseOpts);
         cliOptions.add(enumUnknownDefaultCaseOpt);
         this.setEnumUnknownDefaultCase(false);
 
         // initialize special character mapping
         initializeSpecialCharacterMapping();
 
         // Register common Mustache lambdas.
         registerMustacheLambdas();
     }
 
     /**
      * Initialize special character mapping
      */
     protected void initializeSpecialCharacterMapping() {
         // Initialize special characters
         specialCharReplacements.put("$", "Dollar");
         specialCharReplacements.put("^", "Caret");
         specialCharReplacements.put("|", "Pipe");
         specialCharReplacements.put("=", "Equal");
         specialCharReplacements.put("*", "Star");
         specialCharReplacements.put("-", "Minus");
         specialCharReplacements.put("&", "Ampersand");
         specialCharReplacements.put("%", "Percent");
         specialCharReplacements.put("#", "Hash");
         specialCharReplacements.put("@", "At");
         specialCharReplacements.put("!", "Exclamation");
         specialCharReplacements.put("+", "Plus");
         specialCharReplacements.put(":", "Colon");
         specialCharReplacements.put(";", "Semicolon");
         specialCharReplacements.put(">", "Greater_Than");
         specialCharReplacements.put("<", "Less_Than");
         specialCharReplacements.put(".", "Period");
         specialCharReplacements.put("_", "Underscore");
         specialCharReplacements.put("?", "Question_Mark");
         specialCharReplacements.put(",", "Comma");
         specialCharReplacements.put("'", "Quote");
         specialCharReplacements.put("\"", "Double_Quote");
         specialCharReplacements.put("/", "Slash");
         specialCharReplacements.put("\\", "Back_Slash");
         specialCharReplacements.put("(", "Left_Parenthesis");
         specialCharReplacements.put(")", "Right_Parenthesis");
         specialCharReplacements.put("{", "Left_Curly_Bracket");
         specialCharReplacements.put("}", "Right_Curly_Bracket");
         specialCharReplacements.put("[", "Left_Square_Bracket");
         specialCharReplacements.put("]", "Right_Square_Bracket");
         specialCharReplacements.put("~", "Tilde");
         specialCharReplacements.put("`", "Backtick");
 
         specialCharReplacements.put("<=", "Less_Than_Or_Equal_To");
         specialCharReplacements.put(">=", "Greater_Than_Or_Equal_To");
         specialCharReplacements.put("!=", "Not_Equal");
         specialCharReplacements.put("~=", "Tilde_Equal");
     }
 
     /**
      * Return the symbol name of a symbol
      *
      * @param input Symbol (e.g. $)
      * @return Symbol name (e.g. Dollar)
      */
     protected String getSymbolName(String input) {
         return specialCharReplacements.get(input);
     }
 
     /**
      * Return the example path
      *
      * @param path      the path of the operation
      * @param operation OAS operation object
      * @return string presentation of the example path
      */
     @Override
     @SuppressWarnings("static-method")
     public String generateExamplePath(String path, Operation operation) {
         StringBuilder sb = new StringBuilder();
         sb.append(path);
 
         if (operation.getParameters() != null) {
             int count = 0;
 
             for (Parameter param : operation.getParameters()) {
                 if (param instanceof QueryParameter) {
                     StringBuilder paramPart = new StringBuilder();
                     QueryParameter qp = (QueryParameter) param;
 
                     if (count == 0) {
                         paramPart.append("?");
                     } else {
                         paramPart.append(",");
                     }
                     count += 1;
                     if (!param.getRequired()) {
                         paramPart.append("[");
                     }
                     paramPart.append(param.getName()).append("=");
                     paramPart.append("{");
 
                     // TODO support for multi, tsv?
                     if (qp.getStyle() != null) {
                         paramPart.append(param.getName()).append("1");
                         if (Parameter.StyleEnum.FORM.equals(qp.getStyle())) {
                             if (qp.getExplode() != null && qp.getExplode()) {
                                 paramPart.append(",");
                             } else {
                                 paramPart.append("&").append(param.getName()).append("=");
                                 paramPart.append(param.getName()).append("2");
                             }
                         } else if (Parameter.StyleEnum.PIPEDELIMITED.equals(qp.getStyle())) {
                             paramPart.append("|");
                         } else if (Parameter.StyleEnum.SPACEDELIMITED.equals(qp.getStyle())) {
                             paramPart.append("%20");
                         } else {
                             LOGGER.warn("query parameter '{}' style not support: {}", param.getName(), qp.getStyle());
                         }
                     } else {
                         paramPart.append(param.getName());
                     }
 
                     paramPart.append("}");
                     if (!param.getRequired()) {
                         paramPart.append("]");
                     }
                     sb.append(paramPart);
                 }
             }
         }
 
         return sb.toString();
     }
 
     /**
      * Return the instantiation type of the property, especially for map and array
      *
      * @param schema property schema
      * @return string presentation of the instantiation type of the property
      */
     public String toInstantiationType(Schema schema) {
         if (ModelUtils.isMapSchema(schema)) {
             Schema additionalProperties = getAdditionalProperties(schema);
             String inner = getSchemaType(additionalProperties);
             return instantiationTypes.get("map") + "<String, " + inner + ">";
         } else if (ModelUtils.isArraySchema(schema)) {
             ArraySchema arraySchema = (ArraySchema) schema;
             String inner = getSchemaType(getSchemaItems(arraySchema));
             String parentType;
             if (ModelUtils.isSet(schema)) {
                 parentType = "set";
             } else {
                 parentType = "array";
             }
             return instantiationTypes.get(parentType) + "<" + inner + ">";
         } else {
             return null;
         }
     }
 
     /**
      * Return the example value of the parameter.
      *
      * @param codegenParameter Codegen parameter
      */
     public void setParameterExampleValue(CodegenParameter codegenParameter) {
 
         // set the example value
         // if not specified in x-example, generate a default value
         // TODO need to revise how to obtain the example value
         if (codegenParameter.vendorExtensions != null && codegenParameter.vendorExtensions.containsKey("x-example")) {
             codegenParameter.example = Json.pretty(codegenParameter.vendorExtensions.get("x-example"));
         } else if (Boolean.TRUE.equals(codegenParameter.isBoolean)) {
             codegenParameter.example = "true";
         } else if (Boolean.TRUE.equals(codegenParameter.isLong)) {
             codegenParameter.example = "789";
         } else if (Boolean.TRUE.equals(codegenParameter.isInteger)) {
             codegenParameter.example = "56";
         } else if (Boolean.TRUE.equals(codegenParameter.isFloat)) {
             codegenParameter.example = "3.4";
         } else if (Boolean.TRUE.equals(codegenParameter.isDouble)) {
             codegenParameter.example = "1.2";
         } else if (Boolean.TRUE.equals(codegenParameter.isNumber)) {
             codegenParameter.example = "8.14";
         } else if (Boolean.TRUE.equals(codegenParameter.isBinary)) {
             codegenParameter.example = "BINARY_DATA_HERE";
         } else if (Boolean.TRUE.equals(codegenParameter.isByteArray)) {
             codegenParameter.example = "BYTE_ARRAY_DATA_HERE";
         } else if (Boolean.TRUE.equals(codegenParameter.isFile)) {
             codegenParameter.example = "/path/to/file.txt";
         } else if (Boolean.TRUE.equals(codegenParameter.isDate)) {
             codegenParameter.example = "2013-10-20";
         } else if (Boolean.TRUE.equals(codegenParameter.isDateTime)) {
             codegenParameter.example = "2013-10-20T19:20:30+01:00";
         } else if (Boolean.TRUE.equals(codegenParameter.isUuid)) {
             codegenParameter.example = "38400000-8cf0-11bd-b23e-10b96e4ef00d";
         } else if (Boolean.TRUE.equals(codegenParameter.isUri)) {
             codegenParameter.example = "https://openapi-generator.tech";
         } else if (Boolean.TRUE.equals(codegenParameter.isString)) {
             codegenParameter.example = codegenParameter.paramName + "_example";
         } else if (Boolean.TRUE.equals(codegenParameter.isFreeFormObject)) {
             codegenParameter.example = "Object";
         }
 
     }
 
     /**
      * Return the example value of the parameter.
      *
      * @param codegenParameter Codegen parameter
      * @param parameter        Parameter
      */
     public void setParameterExampleValue(CodegenParameter codegenParameter, Parameter parameter) {
         if (parameter.getExample() != null) {
             codegenParameter.example = parameter.getExample().toString();
             return;
         }
 
         if (parameter.getExamples() != null && !parameter.getExamples().isEmpty()) {
             Example example = parameter.getExamples().values().iterator().next();
             if (example.getValue() != null) {
                 codegenParameter.example = example.getValue().toString();
                 return;
             }
         }
 
         Schema schema = parameter.getSchema();
         if (schema != null && schema.getExample() != null) {
             codegenParameter.example = schema.getExample().toString();
             return;
         }
 
         setParameterExampleValue(codegenParameter);
     }
 
     /**
      * Return the example value of the parameter.
      *
      * @param codegenParameter Codegen parameter
      * @param requestBody      Request body
      */
     public void setParameterExampleValue(CodegenParameter codegenParameter, RequestBody requestBody) {
         Content content = requestBody.getContent();
 
         if (content.size() > 1) {
             // @see ModelUtils.getSchemaFromContent()
             once(LOGGER).warn("Multiple MediaTypes found, using only the first one");
         }
 
         MediaType mediaType = content.values().iterator().next();
         if (mediaType.getExample() != null) {
             codegenParameter.example = mediaType.getExample().toString();
             return;
         }
 
         if (mediaType.getExamples() != null && !mediaType.getExamples().isEmpty()) {
             Example example = mediaType.getExamples().values().iterator().next();
             if (example.getValue() != null) {
                 codegenParameter.example = example.getValue().toString();
                 return;
             }
         }
 
         setParameterExampleValue(codegenParameter);
     }
 
     /**
      * Sets the content type of the parameter based on the encoding specified in the request body.
      *
      * @param codegenParameter Codegen parameter
      * @param mediaType        MediaType from the request body
      */
     public void setParameterContentType(CodegenParameter codegenParameter, MediaType mediaType) {
         if (mediaType != null && mediaType.getEncoding() != null) {
             Encoding encoding = mediaType.getEncoding().get(codegenParameter.baseName);
             if (encoding != null) {
                 codegenParameter.contentType = encoding.getContentType();
             } else {
                 LOGGER.debug("encoding not specified for {}", codegenParameter.baseName);
             }
         }
     }
 
     /**
      * Return the example value of the property
      *
      * @param schema Property schema
      * @return string presentation of the example value of the property
      */
     public String toExampleValue(Schema schema) {
         if (schema.getExample() != null) {
             return schema.getExample().toString();
         }
 
         return getPropertyDefaultValue(schema);
     }
 
     /**
      * Return the default value of the property
      *
      * Return null if you do NOT want a default value.
      * Any non-null value will cause {{#defaultValue} check to pass.
      *
      * @param schema Property schema
      * @return string presentation of the default value of the property
      */
     @SuppressWarnings("static-method")
     public String toDefaultValue(Schema schema) {
         if (schema.getDefault() != null) {
             return schema.getDefault().toString();
         }
 
         return getPropertyDefaultValue(schema);
     }
 
     /**
      * Return the default value of the parameter
      *
      * Return null if you do NOT want a default value.
      * Any non-null value will cause {{#defaultValue} check to pass.
      *
      * @param schema Parameter schema
      * @return string presentation of the default value of the parameter
      */
     public String toDefaultParameterValue(Schema<?> schema) {
         // by default works as original method to be backward compatible
         return toDefaultValue(schema);
     }
 
     /**
      * Return property value depending on property type.
      *
      * @param schema property type
      * @return property value
      */
     @SuppressWarnings("squid:S3923")
     private String getPropertyDefaultValue(Schema schema) {
         /*
          * Although all branches return null, this is left intentionally as examples for new contributors
          */
         if (ModelUtils.isBooleanSchema(schema)) {
             return "null";
         } else if (ModelUtils.isDateSchema(schema)) {
             return "null";
         } else if (ModelUtils.isDateTimeSchema(schema)) {
             return "null";
         } else if (ModelUtils.isNumberSchema(schema)) {
             return "null";
         } else if (ModelUtils.isIntegerSchema(schema)) {
             return "null";
         } else if (ModelUtils.isStringSchema(schema)) {
             return "null";
         } else if (ModelUtils.isObjectSchema(schema)) {
             return "null";
         } else {
             return "null";
         }
     }
 
     /**
      * Return the property initialized from a data object
      * Useful for initialization with a plain object in Javascript
      *
      * @param name   Name of the property object
      * @param schema Property schema
      * @return string presentation of the default value of the property
      */
     @SuppressWarnings("static-method")
     public String toDefaultValueWithParam(String name, Schema schema) {
         return " = data." + name + ";";
     }
 
     /**
      * returns the OpenAPI type for the property. Use getAlias to handle $ref of primitive type
      *
      * @param schema property schema
      * @return string presentation of the type
      **/
     @SuppressWarnings("static-method")
     public String getSchemaType(Schema schema) {
         if (schema instanceof ComposedSchema) { // composed schema
             ComposedSchema cs = (ComposedSchema) schema;
             // Get the interfaces, i.e. the set of elements under 'allOf', 'anyOf' or 'oneOf'.
             List<Schema> schemas = ModelUtils.getInterfaces(cs);
 
             List<String> names = new ArrayList<>();
             // Build a list of the schema types under each interface.
             // For example, if a 'allOf' composed schema has $ref children,
             // add the type of each child to the list of names.
             for (Schema s : schemas) {
                 names.add(getSingleSchemaType(s));
             }
 
             if (cs.getAllOf() != null) {
                 return toAllOfName(names, cs);
             } else if (cs.getAnyOf() != null) { // anyOf
                 return toAnyOfName(names, cs);
             } else if (cs.getOneOf() != null) { // oneOf
                 return toOneOfName(names, cs);
             }
         }
 
         return getSingleSchemaType(schema);
 
     }
 
     protected Schema<?> getSchemaItems(ArraySchema schema) {
         Schema<?> items = schema.getItems();
         if (items == null) {
             LOGGER.error("Undefined array inner type for `{}`. Default to String.", schema.getName());
             items = new StringSchema().description("TODO default missing array inner type to string");
             schema.setItems(items);
         }
         return items;
     }
 
     protected Schema<?> getSchemaAdditionalProperties(Schema schema) {
         Schema<?> inner = getAdditionalProperties(schema);
         if (inner == null) {
             LOGGER.error("`{}` (map property) does not have a proper inner type defined. Default to type:string", schema.getName());
             inner = new StringSchema().description("TODO default missing map inner type to string");
             schema.setAdditionalProperties(inner);
         }
         return inner;
     }
 
     /**
      * Return the name of the 'allOf' composed schema.
      *
      * @param names          List of names
      * @param composedSchema composed schema
      * @return name of the allOf schema
      */
     @SuppressWarnings("static-method")
     public String toAllOfName(List<String> names, ComposedSchema composedSchema) {
         Map<String, Object> exts = composedSchema.getExtensions();
         if (exts != null && exts.containsKey("x-all-of-name")) {
             return (String) exts.get("x-all-of-name");
         }
         if (names.size() == 0) {
             LOGGER.error("allOf has no member defined: {}. Default to ERROR_ALLOF_SCHEMA", composedSchema);
             return "ERROR_ALLOF_SCHEMA";
         } else if (names.size() == 1) {
             return names.get(0);
         } else {
             LOGGER.warn("allOf with multiple schemas defined. Using only the first one: {}", names.get(0));
             return names.get(0);
         }
     }
 
     /**
      * Return the name of the anyOf schema
      *
      * @param names          List of names
      * @param composedSchema composed schema
      * @return name of the anyOf schema
      */
     @SuppressWarnings("static-method")
     public String toAnyOfName(List<String> names, ComposedSchema composedSchema) {
         return "anyOf<" + String.join(",", names) + ">";
     }
 
     /**
      * Return the name of the oneOf schema.
      * <p>
      * This name is used to set the value of CodegenProperty.openApiType.
      * <p>
      * If the 'x-one-of-name' extension is specified in the OAS document, return that value.
      * Otherwise, a name is constructed by creating a comma-separated list of all the names
      * of the oneOf schemas.
      *
      * @param names          List of names
      * @param composedSchema composed schema
      * @return name of the oneOf schema
      */
     @SuppressWarnings("static-method")
     public String toOneOfName(List<String> names, ComposedSchema composedSchema) {
         Map<String, Object> exts = composedSchema.getExtensions();
         if (exts != null && exts.containsKey("x-one-of-name")) {
             return (String) exts.get("x-one-of-name");
         }
         return "oneOf<" + String.join(",", names) + ">";
     }
 
     @Override
     public Schema unaliasSchema(Schema schema, Map<String, String> usedImportMappings) {
         return ModelUtils.unaliasSchema(this.openAPI, schema, usedImportMappings);
     }
 
     /**
      * Return a string representation of the schema type, resolving aliasing and references if necessary.
      *
      * @param schema input
      * @return the string representation of the schema type.
      */
     protected String getSingleSchemaType(Schema schema) {
         Schema unaliasSchema = unaliasSchema(schema, importMapping);
 
         if (StringUtils.isNotBlank(unaliasSchema.get$ref())) { // reference to another definition/schema
             // get the schema/model name from $ref
             String schemaName = ModelUtils.getSimpleRef(unaliasSchema.get$ref());
             if (StringUtils.isNotEmpty(schemaName)) {
                 if (importMapping.containsKey(schemaName)) {
                     return schemaName;
                 }
                 return getAlias(schemaName);
             } else {
                 LOGGER.warn("Error obtaining the datatype from ref: {}. Default to 'object'", unaliasSchema.get$ref());
                 return "object";
             }
         } else { // primitive type or model
             return getAlias(getPrimitiveType(unaliasSchema));
         }
     }
 
     /**
      * Return the OAI type (e.g. integer, long, etc) corresponding to a schema.
      * <pre>$ref</pre> is not taken into account by this method.
      * <p>
      * If the schema is free-form (i.e. 'type: object' with no properties) or inline
      * schema, the returned OAI type is 'object'.
      *
      * @param schema
      * @return type
      */
     private String getPrimitiveType(Schema schema) {
         if (schema == null) {
             throw new RuntimeException("schema cannot be null in getPrimitiveType");
         } else if (typeMapping.containsKey(schema.getType() + "+" + schema.getFormat())) {
             // allows custom type_format mapping.
             // use {type}+{format}
             return typeMapping.get(schema.getType() + "+" + schema.getFormat());
         } else if (ModelUtils.isNullType(schema)) {
             // The 'null' type is allowed in OAS 3.1 and above. It is not supported by OAS 3.0.x,
             // though this tooling supports it.
             return "null";
         } else if (ModelUtils.isDecimalSchema(schema)) {
             // special handle of type: string, format: number
             return "decimal";
         } else if (ModelUtils.isByteArraySchema(schema)) {
             return "ByteArray";
         } else if (ModelUtils.isFileSchema(schema)) {
             return "file";
         } else if (ModelUtils.isBinarySchema(schema)) {
             return SchemaTypeUtil.BINARY_FORMAT;
         } else if (ModelUtils.isBooleanSchema(schema)) {
             return SchemaTypeUtil.BOOLEAN_TYPE;
         } else if (ModelUtils.isDateSchema(schema)) {
             return SchemaTypeUtil.DATE_FORMAT;
         } else if (ModelUtils.isDateTimeSchema(schema)) {
             return "DateTime";
         } else if (ModelUtils.isNumberSchema(schema)) {
             if (schema.getFormat() == null) { // no format defined
                 return "number";
             } else if (ModelUtils.isFloatSchema(schema)) {
                 return SchemaTypeUtil.FLOAT_FORMAT;
             } else if (ModelUtils.isDoubleSchema(schema)) {
                 return SchemaTypeUtil.DOUBLE_FORMAT;
             } else {
                 LOGGER.warn("Unknown `format` {} detected for type `number`. Defaulting to `number`", schema.getFormat());
                 return "number";
             }
         } else if (ModelUtils.isIntegerSchema(schema)) {
             if (ModelUtils.isLongSchema(schema)) {
                 return "long";
             } else {
                 return schema.getType(); // integer
             }
         } else if (ModelUtils.isMapSchema(schema)) {
             return "map";
         } else if (ModelUtils.isArraySchema(schema)) {
             if (ModelUtils.isSet(schema)) {
                 return "set";
             } else {
                 return "array";
             }
         } else if (ModelUtils.isUUIDSchema(schema)) {
             return "UUID";
         } else if (ModelUtils.isURISchema(schema)) {
             return "URI";
         } else if (ModelUtils.isStringSchema(schema)) {
             if (typeMapping.containsKey(schema.getFormat())) {
                 // If the format matches a typeMapping (supplied with the --typeMappings flag)
                 // then treat the format as a primitive type.
                 // This allows the typeMapping flag to add a new custom type which can then
                 // be used in the format field.
                 return schema.getFormat();
             }
             return "string";
         } else if (isFreeFormObject(schema)) {
             // Note: the value of a free-form object cannot be an arbitrary type. Per OAS specification,
             // it must be a map of string to values.
             return "object";
         } else if (schema.getProperties() != null && !schema.getProperties().isEmpty()) { // having property implies it's a model
             return "object";
         } else if (ModelUtils.isAnyType(schema)) {
             return "AnyType";
         } else if (StringUtils.isNotEmpty(schema.getType())) {
             if (!importMapping.containsKey(schema.getType())) {
                 LOGGER.warn("Unknown type found in the schema: {}", schema.getType());
             }
             return schema.getType();
         }
         // The 'type' attribute has not been set in the OAS schema, which means the value
         // can be an arbitrary type, e.g. integer, string, object, array, number...
         // TODO: we should return a different value to distinguish between free-form object
         // and arbitrary type.
         return "object";
     }
 
     /**
      * Return the lowerCamelCase of the string
      *
      * @param name string to be lowerCamelCased
      * @return lowerCamelCase string
      */
     @SuppressWarnings("static-method")
     public String lowerCamelCase(String name) {
         return (name.length() > 0) ? (Character.toLowerCase(name.charAt(0)) + name.substring(1)) : "";
     }
 
     /**
      * Output the language-specific type declaration of a given OAS name.
      *
      * @param name name
      * @return a string presentation of the type
      */
     @Override
     @SuppressWarnings("static-method")
     public String getTypeDeclaration(String name) {
         return name;
     }
 
     /**
      * Output the language-specific type declaration of the property.
      *
      * @param schema property schema
      * @return a string presentation of the property type
      */
     @Override
     public String getTypeDeclaration(Schema schema) {
         if (schema == null) {
             LOGGER.warn("Null schema found. Default type to `NULL_SCHEMA_ERR`");
             return "NULL_SCHEMA_ERR";
         }
 
         String oasType = getSchemaType(schema);
         if (typeMapping.containsKey(oasType)) {
             return typeMapping.get(oasType);
         }
 
         return oasType;
     }
 
     /**
      * Determine the type alias for the given type if it exists. This feature
      * was originally developed for Java because the language does not have an aliasing
      * mechanism of its own but later extends to handle other languages
      *
      * @param name The type name.
      * @return The alias of the given type, if it exists. If there is no alias
      * for this type, then returns the input type name.
      */
     public String getAlias(String name) {
         if (typeAliases != null && typeAliases.containsKey(name)) {
             return typeAliases.get(name);
         }
         return name;
     }
 
     /**
      * Output the Getter name for boolean property, e.g. getActive
      *
      * @param name the name of the property
      * @return getter name based on naming convention
      */
     @Override
     public String toBooleanGetter(String name) {
         return "get" + getterAndSetterCapitalize(name);
     }
 
     /**
      * Output the Getter name, e.g. getSize
      *
      * @param name the name of the property
      * @return getter name based on naming convention
      */
     @Override
     public String toGetter(String name) {
         return "get" + getterAndSetterCapitalize(name);
     }
 
     /**
      * Output the Setter name, e.g. setSize
      *
      * @param name the name of the property
      * @return setter name based on naming convention
      */
     @Override
     public String toSetter(String name) {
         return "set" + getterAndSetterCapitalize(name);
     }
 
     /**
      * Output the API (class) name (capitalized) ending with the specified or default suffix
      * Return DefaultApi if name is empty
      *
      * @param name the name of the Api
      * @return capitalized Api name
      */
     @Override
     public String toApiName(String name) {
         if (name.length() == 0) {
             return "DefaultApi";
         }
         return camelize(apiNamePrefix + "_" + name + "_" + apiNameSuffix);
     }
 
 
/** Converts the OpenAPI schema name to a model name suitable for the current code generator. */
 public String toModelName(final String name){}

 

}