// Copyright (c) 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.weblogic.domain.model;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.LinkedHashMap;
 import java.util.List;
 import javax.annotation.Nullable;
 
 import com.google.gson.Gson;
 import com.google.gson.JsonParser;
 import com.google.gson.TypeAdapter;
 import com.google.gson.annotations.JsonAdapter;
 import com.google.gson.stream.JsonReader;
 import com.google.gson.stream.JsonToken;
 import com.google.gson.stream.JsonWriter;
 import oracle.kubernetes.json.Description;
 import org.apache.commons.lang3.builder.EqualsBuilder;
 import org.apache.commons.lang3.builder.HashCodeBuilder;
 import org.yaml.snakeyaml.Yaml;
 
 /**
  * The configuration to be applied to the WebLogic Monitoring Exporter sidecars in the domain.
  * Note that the operator does not access any of the fields in the configuration; this definition
  * exists simply to define the CRD and to control serialization.
  */
 @JsonAdapter(MonitoringExporterConfiguration.ConfigurationTypeAdapter.class)
 public class MonitoringExporterConfiguration {
   @Description("If true, metrics names will be constructed with underscores between words (snake case). "
              + "By default, metrics names will be constructed with capital letters separating words "
              + "(camel case).")
   private Boolean metricsNameSnakeCase;
 
   @Description("If true, metrics qualifiers will include the operator domain. Defaults to false.")
   private Boolean domainQualifier;
 
   private ExporterQuery[] queries;
 
   public static MonitoringExporterConfiguration createFromYaml(String yaml) {
     return new Gson().fromJson(JsonParser.parseString(convertToJson(yaml)), MonitoringExporterConfiguration.class);
   }
 
   public static String convertToJson(String yaml) {
     final Object loadedYaml = new Yaml().load(yaml);
     return new Gson().toJson(loadedYaml, LinkedHashMap.class);
   }
 
   /**
    * Returns the configuration in a form that can be send to the exporter sidecar.
    */
   public String asJsonString() {
     return toJson();
   }
 
 
/** Returns true if the specified YAML string matches this configuration, ignoring unknown fields and field order. */

public boolean matchesYaml(String yaml) {
  MonitoringExporterConfiguration otherConfig = MonitoringExporterConfiguration.createFromYaml(yaml);

  // Check if metricsNameSnakeCase matches
  if (metricsNameSnakeCase != null && !metricsNameSnakeCase.equals(otherConfig.metricsNameSnakeCase)) {
    return false;
  }

  // Check if domainQualifier matches
  if (domainQualifier != null && !domainQualifier.equals(otherConfig.domainQualifier)) {
    return false;
  }

  // Check if queries match
  if (queries != null && otherConfig.queries != null) {
    if (queries.length != otherConfig.queries.length) {
      return false;
    }

    for (int i = 0; i < queries.length; i++) {
      if (!queries[i].equals(otherConfig.queries[i])) {
        return false;
      }
    }
  } else if (queries != null || otherConfig.queries != null) {
    return false;
  }

  return true;
}
 

}