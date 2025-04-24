/**
  * Jooby https://jooby.io
  * Apache License Version 2.0 https://jooby.io/LICENSE.txt
  * Copyright 2014 Edgar Espina
  */
 package io.jooby.openapi;
 
 import io.jooby.Router;
 import io.jooby.SneakyThrows;
 import io.jooby.internal.openapi.ClassSource;
 import io.jooby.internal.openapi.ContextPathParser;
 import io.jooby.internal.openapi.OpenAPIExt;
 import io.jooby.internal.openapi.OpenAPIParser;
 import io.jooby.internal.openapi.ParserContext;
 import io.jooby.internal.openapi.OperationExt;
 import io.jooby.internal.openapi.RouteParser;
 import io.jooby.internal.openapi.TypeFactory;
 import io.swagger.v3.core.util.Json;
 import io.swagger.v3.core.util.Yaml;
 import io.swagger.v3.oas.models.OpenAPI;
 import io.swagger.v3.oas.models.PathItem;
 import io.swagger.v3.oas.models.Paths;
 import io.swagger.v3.oas.models.info.Info;
 import io.swagger.v3.oas.models.servers.Server;
 import io.swagger.v3.oas.models.tags.Tag;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import javax.annotation.Nonnull;
 import javax.annotation.Nullable;
 import java.io.IOException;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Optional;
 import java.util.Set;
 import java.util.regex.Pattern;
 import java.util.stream.Stream;
 
 /**
  * Generate an {@link OpenAPI} model from a Jooby application.
  *
  * Optionally exports an {@link OpenAPI} model to a json or yaml file.
  *
  * Usage: https://jooby.io/modules/openapi
  *
  * @author edgar
  */
 public class OpenAPIGenerator {
 
   /**
    * Supported formats.
    */
   public enum Format {
     /**
      * JSON.
      */
     JSON {
       @Override public String toString(OpenAPIGenerator tool, OpenAPI result) {
         return tool.toJson(result);
       }
     },
 
     /**
      * YAML.
      */
     YAML {
       @Override public String toString(OpenAPIGenerator tool, OpenAPI result) {
         return tool.toYaml(result);
       }
     };
 
     /**
      * File extension.
      *
      * @return File extension.
      */
     public @Nonnull String extension() {
       return name().toLowerCase();
     }
 
     /**
      * Convert an {@link OpenAPI} model to the current format.
      *
      * @param tool Generator.
      * @param result Model.
      * @return String (json or yaml content).
      */
     public abstract @Nonnull String toString(@Nonnull OpenAPIGenerator tool,
         @Nonnull OpenAPI result);
 
   }
 
   private Logger log = LoggerFactory.getLogger(getClass());
 
   private Set<DebugOption> debug;
 
   private ClassLoader classLoader;
 
   private Path basedir = java.nio.file.Paths.get(System.getProperty("user.dir"));
 
   private Path outputDir = basedir;
 
   private String templateName = "openapi.yaml";
 
   private String includes;
 
   private String excludes;
 
   private String metaInf;
 
   /**
    * Test Only.
    *
    * @param metaInf Location of meta-inf directory.
    */
   public OpenAPIGenerator(String metaInf) {
     this.metaInf = metaInf;
   }
 
   /**
    * Creates a new instance.
    */
   public OpenAPIGenerator() {
     this("META-INF/services/io.jooby.MvcFactory");
   }
 
   /**
    * Export an {@link OpenAPI} model to the given format.
    *
    * @param openAPI Model.
    * @param format Format.
    * @throws IOException
    * @return Output file.
    */
   public @Nonnull Path export(@Nonnull OpenAPI openAPI, @Nonnull Format format) throws IOException {
     Path output;
     if (openAPI instanceof OpenAPIExt) {
       String source = ((OpenAPIExt) openAPI).getSource();
       String[] names = source.split("\\.");
       output = Stream.of(names).limit(names.length - 1)
           .reduce(outputDir, Path::resolve, Path::resolve);
       String appname = names[names.length - 1];
       if (appname.endsWith("Kt")) {
         appname = appname.substring(0, appname.length() - 2);
       }
       output = output.resolve(appname + "." + format.extension());
     } else {
       output = outputDir.resolve("openapi." + format.extension());
     }
 
     if (!Files.exists(output.getParent())) {
       Files.createDirectories(output.getParent());
     }
 
     String content = format.toString(this, openAPI);
     Files.write(output, Collections.singleton(content));
     return output;
   }
 
 
/** Returns an OpenAPI object from Jooby Class by parsing the class byte code. */

public OpenAPI generate(@Nonnull String classname) {
  OpenAPI openAPI = new OpenAPI();

  try {
    Class<?> clazz = Class.forName(classname);
    Router router = (Router) clazz.getDeclaredConstructor().newInstance();

    OpenAPIParser parser = new OpenAPIParser();
    ParserContext parserContext = new ParserContext(openAPI);
    parserContext.setRouter(router);

    // Parse routes
    RouteParser routeParser = new RouteParser(parserContext);
    routeParser.parseRoutes();

    // Parse classes
    ClassSource classSource = new ClassSource(parserContext);
    classSource.parseClasses();

    // Parse operations
    OperationExt operationExt = new OperationExt(parserContext);
    operationExt.parseOperations();

    // Set paths
    Paths paths = new Paths();
    for (io.jooby.internal.openapi.PathItemExt pathItemExt : parserContext.getPathItems()) {
      PathItem pathItem = pathItemExt.getPathItem();
      paths.addPathItem(pathItemExt.getPath(), pathItem);
    }
    openAPI.setPaths(paths);

    // Set info
    Info info = new Info();
    info.setTitle("Jooby API");
    info.setVersion("1.0.0");
    openAPI.setInfo(info);

    // Set servers
    Server server = new Server();
    server.setUrl("http://localhost:8080");
    openAPI.setServers(Collections.singletonList(server));

    // Set tags
    Tag tag = new Tag();
    tag.setName("Jooby");
    openAPI.setTags(Collections.singletonList(tag));

    return openAPI;
  } catch (Exception e) {
    log.error("Error generating OpenAPI from Jooby Class", e);
    return null;
  }
}
 

}