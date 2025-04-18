// Copyright (c) 2018, 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.json;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.lang.reflect.Field;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.lang.reflect.Modifier;
 import java.lang.reflect.ParameterizedType;
 import java.lang.reflect.Type;
 import java.net.URL;
 import java.time.OffsetDateTime;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.LinkedHashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Optional;
 import java.util.Set;
 import java.util.TreeMap;
 import javax.annotation.Nonnull;
 import javax.annotation.Nullable;
 
 import com.google.gson.Gson;
 import com.google.gson.GsonBuilder;
 import com.google.gson.annotations.SerializedName;
 import io.swagger.annotations.ApiModel;
 import io.swagger.annotations.ApiModelProperty;
 
 public class SchemaGenerator {
 
   private static final String EXTERNAL_CLASS = "external";
 
   private static final List<Class<?>> PRIMITIVE_NUMBERS =
       Arrays.asList(byte.class, short.class, int.class, long.class, float.class, double.class);
 
   private static final String JSON_SCHEMA_REFERENCE = "http://json-schema.org/draft-04/schema#";
 
   // A map of classes to their $ref values
   private final Map<Class<?>, String> references = new HashMap<>();
 
   // A map of found classes to their definitions or the constant EXTERNAL_CLASS.
   private final Map<Class<?>, Object> definedObjects = new HashMap<>();
 
   // a map of external class names to the external schema that defines them
   private final Map<String, String> schemaUrls = new HashMap<>();
 
   // if true generate the additionalProperties field to forbid fields not in the object's schema. Defaults to true.
   private boolean forbidAdditionalProperties = true;
 
   // if true, the object fields are implemented as references to definitions
   private boolean supportObjectReferences = true;
 
   // if true, generate the top-level schema version reference
   private boolean includeSchemaReference = true;
 
   // suppress descriptions for any contained packages
   private final Collection<String> suppressDescriptionForPackages = new ArrayList<>();
   private final Map<Class<?>, String> additionalPropertiesTypes = new HashMap<>();
 
   private final Collection<String> enabledFeatures = new ArrayList<>();
 
   /**
    * Returns a pretty-printed string corresponding to a generated schema.
    *
    * @param schema a schema generated by a call to #generate
    * @return a string version of the schema
    */
   public static String prettyPrint(Object schema) {
     return new GsonBuilder().setPrettyPrinting().create().toJson(schema);
   }
 
   static <T, S> Map<T, S> loadCachedSchema(URL cacheUrl) throws IOException {
     StringBuilder sb = new StringBuilder();
     try (BufferedReader schemaReader =
         new BufferedReader(new InputStreamReader(cacheUrl.openStream()))) {
       String inputLine;
       while ((inputLine = schemaReader.readLine()) != null) {
         sb.append(inputLine).append('\n');
       }
     }
 
     return fromJson(sb.toString());
   }
 
   @SuppressWarnings("unchecked")
   private static <T, S> Map<T, S> fromJson(String json) {
     return new Gson().fromJson(json, HashMap.class);
   }
 
 
/** Specifies the version of the Kubernetes schema to use. */
 public void useKubernetesVersion(String version) throws IOException{}

 

}