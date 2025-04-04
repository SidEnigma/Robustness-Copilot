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
 
 import io.swagger.v3.core.util.Json;
 import io.swagger.v3.oas.models.OpenAPI;
 import io.swagger.v3.oas.models.Operation;
 import io.swagger.v3.oas.models.PathItem;
 import io.swagger.v3.oas.models.Paths;
 import io.swagger.v3.oas.models.info.Contact;
 import io.swagger.v3.oas.models.info.Info;
 import io.swagger.v3.oas.models.info.License;
 import io.swagger.v3.oas.models.media.Schema;
 import io.swagger.v3.oas.models.parameters.Parameter;
 import io.swagger.v3.oas.models.security.*;
 import io.swagger.v3.oas.models.tags.Tag;
 import org.apache.commons.io.FilenameUtils;
 import org.apache.commons.io.comparator.PathFileComparator;
 import org.apache.commons.lang3.ObjectUtils;
 import org.apache.commons.lang3.StringUtils;
 import org.openapitools.codegen.api.TemplateDefinition;
 import org.openapitools.codegen.api.TemplatePathLocator;
 import org.openapitools.codegen.api.TemplateProcessor;
 import org.openapitools.codegen.config.GlobalSettings;
 import org.openapitools.codegen.api.TemplatingEngineAdapter;
 import org.openapitools.codegen.api.TemplateFileType;
 import org.openapitools.codegen.ignore.CodegenIgnoreProcessor;
 import org.openapitools.codegen.languages.PythonClientCodegen;
 import org.openapitools.codegen.languages.PythonExperimentalClientCodegen;
 import org.openapitools.codegen.meta.GeneratorMetadata;
 import org.openapitools.codegen.meta.Stability;
 import org.openapitools.codegen.serializer.SerializerUtils;
 import org.openapitools.codegen.templating.CommonTemplateContentLocator;
 import org.openapitools.codegen.templating.GeneratorTemplateContentLocator;
 import org.openapitools.codegen.templating.MustacheEngineAdapter;
 import org.openapitools.codegen.templating.TemplateManagerOptions;
 import org.openapitools.codegen.utils.ImplementationVersion;
 import org.openapitools.codegen.utils.ModelUtils;
 import org.openapitools.codegen.utils.ProcessUtils;
 import org.openapitools.codegen.utils.URLPathUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.io.*;
 import java.net.URL;
 import java.nio.charset.StandardCharsets;
 import java.nio.file.Path;
 import java.time.ZonedDateTime;
 import java.util.*;
 import java.util.concurrent.ConcurrentSkipListSet;
 import java.util.function.Function;
 import java.util.stream.Collectors;
 
 import static org.apache.commons.lang3.StringUtils.removeStart;
 import static org.openapitools.codegen.utils.OnceLogger.once;
 
 @SuppressWarnings("rawtypes")
 public class DefaultGenerator implements Generator {
     private static final String METADATA_DIR = ".openapi-generator";
     protected final Logger LOGGER = LoggerFactory.getLogger(DefaultGenerator.class);
     private final boolean dryRun;
     protected CodegenConfig config;
     protected ClientOptInput opts;
     protected OpenAPI openAPI;
     protected CodegenIgnoreProcessor ignoreProcessor;
     private Boolean generateApis = null;
     private Boolean generateModels = null;
     private Boolean generateSupportingFiles = null;
     private Boolean generateApiTests = null;
     private Boolean generateApiDocumentation = null;
     private Boolean generateModelTests = null;
     private Boolean generateModelDocumentation = null;
     private Boolean generateMetadata = true;
     private String basePath;
     private String basePathWithoutHost;
     private String contextPath;
     private Map<String, String> generatorPropertyDefaults = new HashMap<>();
     protected TemplateProcessor templateProcessor = null;
 
     private List<TemplateDefinition> userDefinedTemplates = new ArrayList<>();
 
 
     public DefaultGenerator() {
         this(false);
     }
 
     public DefaultGenerator(Boolean dryRun) {
         this.dryRun = Boolean.TRUE.equals(dryRun);
         LOGGER.info("Generating with dryRun={}", this.dryRun);
     }
 
     @SuppressWarnings("deprecation")
     @Override
     public Generator opts(ClientOptInput opts) {
         this.opts = opts;
         this.openAPI = opts.getOpenAPI();
         this.config = opts.getConfig();
         List<TemplateDefinition> userFiles = opts.getUserDefinedTemplates();
         if (userFiles != null) {
             this.userDefinedTemplates = Collections.unmodifiableList(userFiles);
         }
 
         TemplateManagerOptions templateManagerOptions = new TemplateManagerOptions(this.config.isEnableMinimalUpdate(),this.config.isSkipOverwrite());
 
         if (this.dryRun) {
             this.templateProcessor = new DryRunTemplateManager(templateManagerOptions);
         } else {
             TemplatingEngineAdapter templatingEngine = this.config.getTemplatingEngine();
 
             if (templatingEngine instanceof MustacheEngineAdapter) {
                 MustacheEngineAdapter mustacheEngineAdapter = (MustacheEngineAdapter) templatingEngine;
                 mustacheEngineAdapter.setCompiler(this.config.processCompiler(mustacheEngineAdapter.getCompiler()));
             }
 
             TemplatePathLocator commonTemplateLocator = new CommonTemplateContentLocator();
             TemplatePathLocator generatorTemplateLocator = new GeneratorTemplateContentLocator(this.config);
             this.templateProcessor = new TemplateManager(
                     templateManagerOptions,
                     templatingEngine,
                     new TemplatePathLocator[]{generatorTemplateLocator, commonTemplateLocator}
             );
         }
 
         String ignoreFileLocation = this.config.getIgnoreFilePathOverride();
         if (ignoreFileLocation != null) {
             final File ignoreFile = new File(ignoreFileLocation);
             if (ignoreFile.exists() && ignoreFile.canRead()) {
                 this.ignoreProcessor = new CodegenIgnoreProcessor(ignoreFile);
             } else {
                 LOGGER.warn("Ignore file specified at {} is not valid. This will fall back to an existing ignore file if present in the output directory.", ignoreFileLocation);
             }
         }
 
         if (this.ignoreProcessor == null) {
             this.ignoreProcessor = new CodegenIgnoreProcessor(this.config.getOutputDir());
         }
 
         return this;
     }
 
     /**
      * Retrieves an instance to the configured template processor, available after user-defined options are
      * applied via {@link DefaultGenerator#opts(ClientOptInput)}.
      *
      * @return A configured {@link TemplateProcessor}, or null.
      */
     public TemplateProcessor getTemplateProcessor() {
         return templateProcessor;
     }
 
     /**
      * Programmatically disable the output of .openapi-generator/VERSION, .openapi-generator-ignore,
      * or other metadata files used by OpenAPI Generator.
      *
      * @param generateMetadata true: enable outputs, false: disable outputs
      */
     @SuppressWarnings("WeakerAccess")
     public void setGenerateMetadata(Boolean generateMetadata) {
         this.generateMetadata = generateMetadata;
     }
 
     /**
      * Set generator properties otherwise pulled from system properties.
      * Useful for running tests in parallel without relying on System.properties.
      *
      * @param key   The system property key
      * @param value The system property value
      */
     @SuppressWarnings("WeakerAccess")
     public void setGeneratorPropertyDefault(final String key, final String value) {
         this.generatorPropertyDefaults.put(key, value);
     }
 
     private Boolean getGeneratorPropertyDefaultSwitch(final String key, final Boolean defaultValue) {
         String result = null;
         if (this.generatorPropertyDefaults.containsKey(key)) {
             result = this.generatorPropertyDefaults.get(key);
         }
         if (result != null) {
             return Boolean.valueOf(result);
         }
         return defaultValue;
     }
 
     void configureGeneratorProperties() {
         // allows generating only models by specifying a CSV of models to generate, or empty for all
         // NOTE: Boolean.TRUE is required below rather than `true` because of JVM boxing constraints and type inference.
         generateApis = GlobalSettings.getProperty(CodegenConstants.APIS) != null ? Boolean.TRUE : getGeneratorPropertyDefaultSwitch(CodegenConstants.APIS, null);
         generateModels = GlobalSettings.getProperty(CodegenConstants.MODELS) != null ? Boolean.TRUE : getGeneratorPropertyDefaultSwitch(CodegenConstants.MODELS, null);
         generateSupportingFiles = GlobalSettings.getProperty(CodegenConstants.SUPPORTING_FILES) != null ? Boolean.TRUE : getGeneratorPropertyDefaultSwitch(CodegenConstants.SUPPORTING_FILES, null);
 
         if (generateApis == null && generateModels == null && generateSupportingFiles == null) {
             // no specifics are set, generate everything
             generateApis = generateModels = generateSupportingFiles = true;
         } else {
             if (generateApis == null) {
                 generateApis = false;
             }
             if (generateModels == null) {
                 generateModels = false;
             }
             if (generateSupportingFiles == null) {
                 generateSupportingFiles = false;
             }
         }
         // model/api tests and documentation options rely on parent generate options (api or model) and no other options.
         // They default to true in all scenarios and can only be marked false explicitly
         generateModelTests = GlobalSettings.getProperty(CodegenConstants.MODEL_TESTS) != null ? Boolean.valueOf(GlobalSettings.getProperty(CodegenConstants.MODEL_TESTS)) : getGeneratorPropertyDefaultSwitch(CodegenConstants.MODEL_TESTS, true);
         generateModelDocumentation = GlobalSettings.getProperty(CodegenConstants.MODEL_DOCS) != null ? Boolean.valueOf(GlobalSettings.getProperty(CodegenConstants.MODEL_DOCS)) : getGeneratorPropertyDefaultSwitch(CodegenConstants.MODEL_DOCS, true);
         generateApiTests = GlobalSettings.getProperty(CodegenConstants.API_TESTS) != null ? Boolean.valueOf(GlobalSettings.getProperty(CodegenConstants.API_TESTS)) : getGeneratorPropertyDefaultSwitch(CodegenConstants.API_TESTS, true);
         generateApiDocumentation = GlobalSettings.getProperty(CodegenConstants.API_DOCS) != null ? Boolean.valueOf(GlobalSettings.getProperty(CodegenConstants.API_DOCS)) : getGeneratorPropertyDefaultSwitch(CodegenConstants.API_DOCS, true);
 
         // Additional properties added for tests to exclude references in project related files
         config.additionalProperties().put(CodegenConstants.GENERATE_API_TESTS, generateApiTests);
         config.additionalProperties().put(CodegenConstants.GENERATE_MODEL_TESTS, generateModelTests);
 
         config.additionalProperties().put(CodegenConstants.GENERATE_API_DOCS, generateApiDocumentation);
         config.additionalProperties().put(CodegenConstants.GENERATE_MODEL_DOCS, generateModelDocumentation);
 
         config.additionalProperties().put(CodegenConstants.GENERATE_APIS, generateApis);
         config.additionalProperties().put(CodegenConstants.GENERATE_MODELS, generateModels);
 
         if (!generateApiTests && !generateModelTests) {
             config.additionalProperties().put(CodegenConstants.EXCLUDE_TESTS, true);
         }
 
         if (GlobalSettings.getProperty("debugOpenAPI") != null) {
             System.out.println(SerializerUtils.toJsonString(openAPI));
         } else if (GlobalSettings.getProperty("debugSwagger") != null) {
             // This exists for backward compatibility
             // We fall to this block only if debugOpenAPI is null. No need to dump this twice.
             LOGGER.info("Please use system property 'debugOpenAPI' instead of 'debugSwagger'.");
             System.out.println(SerializerUtils.toJsonString(openAPI));
         }
 
         config.processOpts();
         config.preprocessOpenAPI(openAPI);
 
         // set OpenAPI to make these available to all methods
         config.setOpenAPI(openAPI);
 
         config.additionalProperties().put("generatorVersion", ImplementationVersion.read());
         config.additionalProperties().put("generatedDate", ZonedDateTime.now().toString());
         config.additionalProperties().put("generatedYear", String.valueOf(ZonedDateTime.now().getYear()));
         config.additionalProperties().put("generatorClass", config.getClass().getName());
         config.additionalProperties().put("inputSpec", config.getInputSpec());
 
         if (openAPI.getExtensions() != null) {
             config.vendorExtensions().putAll(openAPI.getExtensions());
         }
 
         // TODO: Allow user to define _which_ servers object in the array to target.
         // Configures contextPath/basePath according to api document's servers
         URL url = URLPathUtils.getServerURL(openAPI, config.serverVariableOverrides());
         contextPath = removeTrailingSlash(config.escapeText(url.getPath())); // for backward compatibility
         basePathWithoutHost = contextPath;
         if (URLPathUtils.isRelativeUrl(openAPI.getServers())) {
             basePath = removeTrailingSlash(basePathWithoutHost);
         } else {
             basePath = removeTrailingSlash(config.escapeText(URLPathUtils.getHost(openAPI, config.serverVariableOverrides())));
         }
     }
 
     private void configureOpenAPIInfo() {
         Info info = this.openAPI.getInfo();
         if (info == null) {
             return;
         }
         if (info.getTitle() != null) {
             config.additionalProperties().put("appName", config.escapeText(info.getTitle()));
         }
         if (info.getVersion() != null) {
             config.additionalProperties().put("appVersion", config.escapeText(info.getVersion()));
         } else {
             LOGGER.error("Missing required field info version. Default appVersion set to 1.0.0");
             config.additionalProperties().put("appVersion", "1.0.0");
         }
 
         if (StringUtils.isEmpty(info.getDescription())) {
             // set a default description if none if provided
             config.additionalProperties().put("appDescription",
                     "No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)");
             config.additionalProperties().put("appDescriptionWithNewLines", config.additionalProperties().get("appDescription"));
             config.additionalProperties().put("unescapedAppDescription", "No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)");
         } else {
             config.additionalProperties().put("appDescription", config.escapeText(info.getDescription()));
             config.additionalProperties().put("appDescriptionWithNewLines", config.escapeTextWhileAllowingNewLines(info.getDescription()));
             config.additionalProperties().put("unescapedAppDescription", info.getDescription());
         }
 
         if (info.getContact() != null) {
             Contact contact = info.getContact();
             if (contact.getEmail() != null) {
                 config.additionalProperties().put("infoEmail", config.escapeText(contact.getEmail()));
             }
             if (contact.getName() != null) {
                 config.additionalProperties().put("infoName", config.escapeText(contact.getName()));
             }
             if (contact.getUrl() != null) {
                 config.additionalProperties().put("infoUrl", config.escapeText(contact.getUrl()));
             }
         }
 
         if (info.getLicense() != null) {
             License license = info.getLicense();
             if (license.getName() != null) {
                 config.additionalProperties().put("licenseInfo", config.escapeText(license.getName()));
             }
             if (license.getUrl() != null) {
                 config.additionalProperties().put("licenseUrl", config.escapeText(license.getUrl()));
             }
         }
 
         if (info.getVersion() != null) {
             config.additionalProperties().put("version", config.escapeText(info.getVersion()));
         } else {
             LOGGER.error("Missing required field info version. Default version set to 1.0.0");
             config.additionalProperties().put("version", "1.0.0");
         }
 
         if (info.getTermsOfService() != null) {
             config.additionalProperties().put("termsOfService", config.escapeText(info.getTermsOfService()));
         }
     }
 
     private void generateModelTests(List<File> files, Map<String, Object> models, String modelName) throws IOException {
         // to generate model test files
         for (Map.Entry<String, String> configModelTestTemplateFilesEntry : config.modelTestTemplateFiles().entrySet()) {
             String templateName = configModelTestTemplateFilesEntry.getKey();
             String suffix = configModelTestTemplateFilesEntry.getValue();
             String filename = config.modelTestFileFolder() + File.separator + config.toModelTestFilename(modelName) + suffix;
 
             if (generateModelTests) {
                 // do not overwrite test file that already exists (regardless of config's skipOverwrite setting)
                 File modelTestFile = new File(filename);
                 if (modelTestFile.exists()) {
                     this.templateProcessor.skip(modelTestFile.toPath(), "Test files never overwrite an existing file of the same name.");
                 } else {
                     File written = processTemplateToFile(models, templateName, filename, generateModelTests, CodegenConstants.MODEL_TESTS, config.modelTestFileFolder());
                     if (written != null) {
                         files.add(written);
                         if (config.isEnablePostProcessFile() && !dryRun) {
                             config.postProcessFile(written, "model-test");
                         }
                     }
                 }
             } else if (dryRun) {
                 Path skippedPath = java.nio.file.Paths.get(filename);
                 this.templateProcessor.skip(skippedPath, "Skipped by modelTests option supplied by user.");
             }
         }
     }
 
     private void generateModelDocumentation(List<File> files, Map<String, Object> models, String modelName) throws IOException {
         for (String templateName : config.modelDocTemplateFiles().keySet()) {
             String docExtension = config.getDocExtension();
             String suffix = docExtension != null ? docExtension : config.modelDocTemplateFiles().get(templateName);
             String filename = config.modelDocFileFolder() + File.separator + config.toModelDocFilename(modelName) + suffix;
 
             File written = processTemplateToFile(models, templateName, filename, generateModelDocumentation, CodegenConstants.MODEL_DOCS);
             if (written != null) {
                 files.add(written);
                 if (config.isEnablePostProcessFile() && !dryRun) {
                     config.postProcessFile(written, "model-doc");
                 }
             }
         }
     }
 
     private void generateModel(List<File> files, Map<String, Object> models, String modelName) throws IOException {
         for (String templateName : config.modelTemplateFiles().keySet()) {
             String filename = config.modelFilename(templateName, modelName);
             File written = processTemplateToFile(models, templateName, filename, generateModels, CodegenConstants.MODELS);
             if (written != null) {
                 files.add(written);
                 if (config.isEnablePostProcessFile() && !dryRun) {
                     config.postProcessFile(written, "model");
                 }
             }
         }
     }
 
     @SuppressWarnings("unchecked")
     void generateModels(List<File> files, List<Object> allModels, List<String> unusedModels) {
         if (!generateModels) {
             // TODO: Process these anyway and add to dryRun info
             LOGGER.info("Skipping generation of models.");
             return;
         }
 
         final Map<String, Schema> schemas = ModelUtils.getSchemas(this.openAPI);
         if (schemas == null) {
             LOGGER.warn("Skipping generation of models because specification document has no schemas.");
             return;
         }
 
         String modelNames = GlobalSettings.getProperty("models");
         Set<String> modelsToGenerate = null;
         if (modelNames != null && !modelNames.isEmpty()) {
             modelsToGenerate = new HashSet<>(Arrays.asList(modelNames.split(",")));
         }
 
         Set<String> modelKeys = schemas.keySet();
         if (modelsToGenerate != null && !modelsToGenerate.isEmpty()) {
             Set<String> updatedKeys = new HashSet<>();
             for (String m : modelKeys) {
                 if (modelsToGenerate.contains(m)) {
                     updatedKeys.add(m);
                 }
             }
 
             modelKeys = updatedKeys;
         }
 
         // store all processed models
         Map<String, Object> allProcessedModels = new TreeMap<>((o1, o2) -> ObjectUtils.compare(config.toModelName(o1), config.toModelName(o2)));
 
         Boolean skipFormModel = GlobalSettings.getProperty(CodegenConstants.SKIP_FORM_MODEL) != null ?
                 Boolean.valueOf(GlobalSettings.getProperty(CodegenConstants.SKIP_FORM_MODEL)) :
                 getGeneratorPropertyDefaultSwitch(CodegenConstants.SKIP_FORM_MODEL, true);
 
         // process models only
         for (String name : modelKeys) {
             try {
                 // don't generate models that are not used as object (e.g. form parameters)
                 if (unusedModels.contains(name)) {
                     if (Boolean.FALSE.equals(skipFormModel)) {
                         // if skipFormModel sets to true, still generate the model and log the result
                         LOGGER.info("Model {} (marked as unused due to form parameters) is generated due to the global property `skipFormModel` set to false", name);
                     } else {
                         LOGGER.info("Model {} not generated since it's marked as unused (due to form parameters) and `skipFormModel` (global property) set to true (default)", name);
                         // TODO: Should this be added to dryRun? If not, this seems like a weird place to return early from processing.
                         continue;
                     }
                 }
 
                 Schema schema = schemas.get(name);
 
                 if (ModelUtils.isFreeFormObject(this.openAPI, schema)) { // check to see if it's a free-form object
                     // there are 3 free form use cases
                     // 1. free form with no validation that is not allOf included in any composed schemas
                     // 2. free form with validation
                     // 3. free form that is allOf included in any composed schemas
                     //      this use case arises when using interface schemas
                     // generators may choose to make models for use case 2 + 3
                     Schema refSchema = new Schema();
                     refSchema.set$ref("#/components/schemas/"+name);
                     Schema unaliasedSchema = config.unaliasSchema(refSchema, config.importMapping());
                     if (unaliasedSchema.get$ref() == null) {
                         LOGGER.info("Model {} not generated since it's a free-form object", name);
                         continue;
                     }
                 } else if (ModelUtils.isMapSchema(schema)) { // check to see if it's a "map" model
                     // A composed schema (allOf, oneOf, anyOf) is considered a Map schema if the additionalproperties attribute is set
                     // for that composed schema. However, in the case of a composed schema, the properties are defined or referenced
                     // in the inner schemas, and the outer schema does not have properties.
                     if (!ModelUtils.isGenerateAliasAsModel(schema) && !ModelUtils.isComposedSchema(schema) && (schema.getProperties() == null || schema.getProperties().isEmpty())) {
                         // schema without property, i.e. alias to map
                         LOGGER.info("Model {} not generated since it's an alias to map (without property) and `generateAliasAsModel` is set to false (default)", name);
                         continue;
                     }
                 } else if (ModelUtils.isArraySchema(schema)) { // check to see if it's an "array" model
                     if (!ModelUtils.isGenerateAliasAsModel(schema) && (schema.getProperties() == null || schema.getProperties().isEmpty())) {
                         // schema without property, i.e. alias to array
                         LOGGER.info("Model {} not generated since it's an alias to array (without property) and `generateAliasAsModel` is set to false (default)", name);
                         continue;
                     }
                 }
 
                 Map<String, Schema> schemaMap = new HashMap<>();
                 schemaMap.put(name, schema);
                 Map<String, Object> models = processModels(config, schemaMap);
                 models.put("classname", config.toModelName(name));
                 models.putAll(config.additionalProperties());
                 allProcessedModels.put(name, models);
             } catch (Exception e) {
                 throw new RuntimeException("Could not process model '" + name + "'" + ".Please make sure that your schema is correct!", e);
             }
         }
 
         // loop through all models to update children models, isSelfReference, isCircularReference, etc
         allProcessedModels = config.updateAllModels(allProcessedModels);
 
         // post process all processed models
         allProcessedModels = config.postProcessAllModels(allProcessedModels);
 
         // generate files based on processed models
         for (String modelName : allProcessedModels.keySet()) {
             Map<String, Object> models = (Map<String, Object>) allProcessedModels.get(modelName);
             models.put("modelPackage", config.modelPackage());
             try {
                 // TODO revise below as we've already performed unaliasing so that the isAlias check may be removed
                 List<Object> modelList = (List<Object>) models.get("models");
                 if (modelList != null && !modelList.isEmpty()) {
                     Map<String, Object> modelTemplate = (Map<String, Object>) modelList.get(0);
                     if (modelTemplate != null && modelTemplate.containsKey("model")) {
                         CodegenModel m = (CodegenModel) modelTemplate.get("model");
                         if (m.isAlias && !((config instanceof PythonClientCodegen) || (config instanceof PythonExperimentalClientCodegen)))  {
                             // alias to number, string, enum, etc, which should not be generated as model
                             // for PythonClientCodegen, all aliases are generated as models
                             continue;  // Don't create user-defined classes for aliases
                         }
                     }
                     allModels.add(modelTemplate);
                 }
 
                 // to generate model files
                 generateModel(files, models, modelName);
 
                 // to generate model test files
                 generateModelTests(files, models, modelName);
 
                 // to generate model documentation files
                 generateModelDocumentation(files, models, modelName);
 
             } catch (Exception e) {
                 throw new RuntimeException("Could not generate model '" + modelName + "'", e);
             }
         }
         if (GlobalSettings.getProperty("debugModels") != null) {
             LOGGER.info("############ Model info ############");
             Json.prettyPrint(allModels);
         }
 
     }
 
     @SuppressWarnings("unchecked")
     void generateApis(List<File> files, List<Object> allOperations, List<Object> allModels) {
         if (!generateApis) {
             // TODO: Process these anyway and present info via dryRun?
             LOGGER.info("Skipping generation of APIs.");
             return;
         }
         Map<String, List<CodegenOperation>> paths = processPaths(this.openAPI.getPaths());
         Set<String> apisToGenerate = null;
         String apiNames = GlobalSettings.getProperty("apis");
         if (apiNames != null && !apiNames.isEmpty()) {
             apisToGenerate = new HashSet<>(Arrays.asList(apiNames.split(",")));
         }
         if (apisToGenerate != null && !apisToGenerate.isEmpty()) {
             Map<String, List<CodegenOperation>> updatedPaths = new TreeMap<>();
             for (String m : paths.keySet()) {
                 if (apisToGenerate.contains(m)) {
                     updatedPaths.put(m, paths.get(m));
                 }
             }
             paths = updatedPaths;
         }
         for (String tag : paths.keySet()) {
             try {
                 List<CodegenOperation> ops = paths.get(tag);
                 ops.sort((one, another) -> ObjectUtils.compare(one.operationId, another.operationId));
                 Map<String, Object> operation = processOperations(config, tag, ops, allModels);
                 URL url = URLPathUtils.getServerURL(openAPI, config.serverVariableOverrides());
                 operation.put("basePath", basePath);
                 operation.put("basePathWithoutHost", removeTrailingSlash(config.encodePath(url.getPath())));
                 operation.put("contextPath", contextPath);
                 operation.put("baseName", tag);
                 operation.put("apiPackage", config.apiPackage());
                 operation.put("modelPackage", config.modelPackage());
                 operation.putAll(config.additionalProperties());
                 operation.put("classname", config.toApiName(tag));
                 operation.put("classVarName", config.toApiVarName(tag));
                 operation.put("importPath", config.toApiImport(tag));
                 operation.put("classFilename", config.toApiFilename(tag));
                 operation.put("strictSpecBehavior", config.isStrictSpecBehavior());
 
                 if (allModels == null || allModels.isEmpty()) {
                     operation.put("hasModel", false);
                 } else {
                     operation.put("hasModel", true);
                 }
 
                 if (!config.vendorExtensions().isEmpty()) {
                     operation.put("vendorExtensions", config.vendorExtensions());
                 }
 
                 // process top-level x-group-parameters
                 if (config.vendorExtensions().containsKey("x-group-parameters")) {
                     boolean isGroupParameters = Boolean.parseBoolean(config.vendorExtensions().get("x-group-parameters").toString());
 
                     Map<String, Object> objectMap = (Map<String, Object>) operation.get("operations");
                     @SuppressWarnings("unchecked")
                     List<CodegenOperation> operations = (List<CodegenOperation>) objectMap.get("operation");
                     for (CodegenOperation op : operations) {
                         if (isGroupParameters && !op.vendorExtensions.containsKey("x-group-parameters")) {
                             op.vendorExtensions.put("x-group-parameters", Boolean.TRUE);
                         }
                     }
                 }
 
                 // Pass sortParamsByRequiredFlag through to the Mustache template...
                 boolean sortParamsByRequiredFlag = true;
                 if (this.config.additionalProperties().containsKey(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG)) {
                     sortParamsByRequiredFlag = Boolean.parseBoolean(this.config.additionalProperties().get(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG).toString());
                 }
                 operation.put("sortParamsByRequiredFlag", sortParamsByRequiredFlag);
 
                 /* consumes, produces are no longer defined in OAS3.0
                 processMimeTypes(swagger.getConsumes(), operation, "consumes");
                 processMimeTypes(swagger.getProduces(), operation, "produces");
                 */
 
                 allOperations.add(new HashMap<>(operation));
 
                 addAuthenticationSwitches(operation);
 
                 for (String templateName : config.apiTemplateFiles().keySet()) {
                     String filename = config.apiFilename(templateName, tag);
                     File written = processTemplateToFile(operation, templateName, filename, generateApis, CodegenConstants.APIS);
                     if (written != null) {
                         files.add(written);
                         if (config.isEnablePostProcessFile() && !dryRun) {
                             config.postProcessFile(written, "api");
                         }
                     }
                 }
 
                 // to generate api test files
                 for (String templateName : config.apiTestTemplateFiles().keySet()) {
                     String filename = config.apiTestFilename(templateName, tag);
                     File apiTestFile = new File(filename);
                     // do not overwrite test file that already exists
                     if (apiTestFile.exists()) {
                         this.templateProcessor.skip(apiTestFile.toPath(), "Test files never overwrite an existing file of the same name.");
                     } else {
                         File written = processTemplateToFile(operation, templateName, filename, generateApiTests, CodegenConstants.API_TESTS, config.apiTestFileFolder());
                         if (written != null) {
                             files.add(written);
                             if (config.isEnablePostProcessFile() && !dryRun) {
                                 config.postProcessFile(written, "api-test");
                             }
                         }
                     }
                 }
 
                 // to generate api documentation files
                 for (String templateName : config.apiDocTemplateFiles().keySet()) {
                     String filename = config.apiDocFilename(templateName, tag);
                     File written = processTemplateToFile(operation, templateName, filename, generateApiDocumentation, CodegenConstants.API_DOCS);
                     if (written != null) {
                         files.add(written);
                         if (config.isEnablePostProcessFile() && !dryRun) {
                             config.postProcessFile(written, "api-doc");
                         }
                     }
                 }
 
             } catch (Exception e) {
                 throw new RuntimeException("Could not generate api file for '" + tag + "'", e);
             }
         }
         if (GlobalSettings.getProperty("debugOperations") != null) {
             LOGGER.info("############ Operation info ############");
             Json.prettyPrint(allOperations);
         }
 
     }
 
     private void generateSupportingFiles(List<File> files, Map<String, Object> bundle) {
         if (!generateSupportingFiles) {
             // TODO: process these anyway and report via dryRun?
             LOGGER.info("Skipping generation of supporting files.");
             return;
         }
         Set<String> supportingFilesToGenerate = null;
         String supportingFiles = GlobalSettings.getProperty(CodegenConstants.SUPPORTING_FILES);
         if (supportingFiles != null && !supportingFiles.isEmpty()) {
             supportingFilesToGenerate = new HashSet<>(Arrays.asList(supportingFiles.split(",")));
         }
 
         for (SupportingFile support : config.supportingFiles()) {
             try {
                 String outputFolder = config.outputFolder();
                 if (StringUtils.isNotEmpty(support.getFolder())) {
                     outputFolder += File.separator + support.getFolder();
                 }
                 File of = new File(outputFolder);
                 if (!of.isDirectory()) {
                     if(!dryRun && !of.mkdirs()) {
                         once(LOGGER).debug("Output directory {} not created. It {}.", outputFolder, of.exists() ? "already exists." : "may not have appropriate permissions.");
                     }
                 }
                 String outputFilename = new File(support.getDestinationFilename()).isAbsolute() // split
                         ? support.getDestinationFilename()
                         : outputFolder + File.separator + support.getDestinationFilename().replace('/', File.separatorChar);
 
                 boolean shouldGenerate = true;
                 if (supportingFilesToGenerate != null && !supportingFilesToGenerate.isEmpty()) {
                     shouldGenerate = supportingFilesToGenerate.contains(support.getDestinationFilename());
                 }
 
                 File written = processTemplateToFile(bundle, support.getTemplateFile(), outputFilename, shouldGenerate, CodegenConstants.SUPPORTING_FILES);
                 if (written != null) {
                     files.add(written);
                     if (config.isEnablePostProcessFile() && !dryRun) {
                         config.postProcessFile(written, "supporting-file");
                     }
                 }
             } catch (Exception e) {
                 throw new RuntimeException("Could not generate supporting file '" + support + "'", e);
             }
         }
 
         // Consider .openapi-generator-ignore a supporting file
         // Output .openapi-generator-ignore if it doesn't exist and wasn't explicitly created by a generator
         final String openapiGeneratorIgnore = ".openapi-generator-ignore";
         String ignoreFileNameTarget = config.outputFolder() + File.separator + openapiGeneratorIgnore;
         File ignoreFile = new File(ignoreFileNameTarget);
         if (generateMetadata) {
             try {
                 boolean shouldGenerate = !ignoreFile.exists();
                 if (shouldGenerate && supportingFilesToGenerate != null && !supportingFilesToGenerate.isEmpty()) {
                     shouldGenerate = supportingFilesToGenerate.contains(openapiGeneratorIgnore);
                 }
                 File written = processTemplateToFile(bundle, openapiGeneratorIgnore, ignoreFileNameTarget, shouldGenerate, CodegenConstants.SUPPORTING_FILES);
                 if (written != null) {
                     files.add(written);
                     if (config.isEnablePostProcessFile() && !dryRun) {
                         config.postProcessFile(written, "openapi-generator-ignore");
                     }
                 }
             } catch (Exception e) {
                 throw new RuntimeException("Could not generate supporting file '" + ignoreFileNameTarget + "'", e);
             }
         } else {
             this.templateProcessor.skip(ignoreFile.toPath(), "Skipped by generateMetadata option supplied by user.");
         }
 
         generateVersionMetadata(files);
     }
 
     @SuppressWarnings("unchecked")
     Map<String, Object> buildSupportFileBundle(List<Object> allOperations, List<Object> allModels) {
 
         Map<String, Object> bundle = new HashMap<>(config.additionalProperties());
         bundle.put("apiPackage", config.apiPackage());
 
         Map<String, Object> apis = new HashMap<>();
         apis.put("apis", allOperations);
 
         URL url = URLPathUtils.getServerURL(openAPI, config.serverVariableOverrides());
 
         bundle.put("openAPI", openAPI);
         bundle.put("basePath", basePath);
         bundle.put("basePathWithoutHost", basePathWithoutHost);
         bundle.put("scheme", URLPathUtils.getScheme(url, config));
         bundle.put("host", url.getHost());
         if (url.getPort() != 80 && url.getPort() != 443 && url.getPort() != -1) {
             bundle.put("port", url.getPort());
         }
         bundle.put("contextPath", contextPath);
         bundle.put("apiInfo", apis);
         bundle.put("models", allModels);
         bundle.put("apiFolder", config.apiPackage().replace('.', File.separatorChar));
         bundle.put("modelPackage", config.modelPackage());
         bundle.put("library", config.getLibrary());
         bundle.put("generatorLanguageVersion", config.generatorLanguageVersion());
         // todo verify support and operation bundles have access to the common variables
 
         addAuthenticationSwitches(bundle);
 
         List<CodegenServer> servers = config.fromServers(openAPI.getServers());
         if (servers != null && !servers.isEmpty()) {
             servers.forEach(server -> server.url = removeTrailingSlash(server.url));
             bundle.put("servers", servers);
             bundle.put("hasServers", true);
         }
 
         if (openAPI.getExternalDocs() != null) {
             bundle.put("externalDocs", openAPI.getExternalDocs());
         }
 
         for (int i = 0; i < allModels.size() - 1; i++) {
             HashMap<String, CodegenModel> cm = (HashMap<String, CodegenModel>) allModels.get(i);
             CodegenModel m = cm.get("model");
             m.hasMoreModels = true;
         }
 
         config.postProcessSupportingFileData(bundle);
 
         if (GlobalSettings.getProperty("debugSupportingFiles") != null) {
             LOGGER.info("############ Supporting file info ############");
             Json.prettyPrint(bundle);
         }
         return bundle;
     }
 
 
/** This adds a collection and a boolean for each type ofAuthentication to the map. */
  void addAuthenticationSwitches(Map<String, Object> bundle){}

 

}