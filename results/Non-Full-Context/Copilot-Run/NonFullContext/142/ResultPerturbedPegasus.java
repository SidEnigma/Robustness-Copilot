// Copyright (c) 2017, 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.operator.wlsconfig;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.stream.Collectors;
 import javax.annotation.Nonnull;
 
 import com.fasterxml.jackson.databind.ObjectMapper;
 import oracle.kubernetes.operator.logging.LoggingFacade;
 import oracle.kubernetes.operator.logging.LoggingFactory;
 import oracle.kubernetes.operator.logging.MessageKeys;
 import oracle.kubernetes.weblogic.domain.model.WlsDomain;
 import org.apache.commons.lang3.builder.EqualsBuilder;
 import org.apache.commons.lang3.builder.HashCodeBuilder;
 import org.apache.commons.lang3.builder.ToStringBuilder;
 
 import static oracle.kubernetes.utils.OperatorUtils.isNullOrEmpty;
 
 /** Contains a snapshot of configuration for a WebLogic Domain. */
 public class WlsDomainConfig implements WlsDomain {
   private static final LoggingFacade LOGGER = LoggingFactory.getLogger("Operator", "Operator");
 
   // Name of this WLS domain (This is NOT the domain UID in the WebLogic domain kubernetes CRD)
   private String name;
 
   private String adminServerName;
 
   // Contains all configured WLS clusters in the WLS domain
   private List<WlsClusterConfig> configuredClusters = new ArrayList<>();
   // Contains all statically configured WLS servers in the WLS domain
   private List<WlsServerConfig> servers = new ArrayList<>();
   // Contains all configured server templates in the WLS domain
   private List<WlsServerConfig> serverTemplates = new ArrayList<>();
   // Contains all configured machines in the WLS domain
   private Map<String, WlsMachineConfig> wlsMachineConfigs = new HashMap<>();
 
   public WlsDomainConfig() {
   }
 
   /**
    * Constructor when no JSON response is available.
    *
    * @param name Name of the WLS domain
    */
   public WlsDomainConfig(String name) {
     this.name = name;
   }
 
   /**
    * Constructor.
    *
    * @param name Name of this WLS domain
    * @param adminServerName Name of the admin server in this WLS domain
    * @param wlsClusterConfigs A Map containing clusters configured in this WLS domain
    * @param wlsServerConfigs A Map containing servers configured in the WLS domain
    * @param wlsServerTemplates A Map containing server templates configured in this WLS domain
    * @param wlsMachineConfigs A Map containing machines configured in the WLS domain
    */
   public WlsDomainConfig(
       String name,
       String adminServerName,
       Map<String, WlsClusterConfig> wlsClusterConfigs,
       Map<String, WlsServerConfig> wlsServerConfigs,
       Map<String, WlsServerConfig> wlsServerTemplates,
       Map<String, WlsMachineConfig> wlsMachineConfigs) {
     this.configuredClusters = new ArrayList<>(wlsClusterConfigs.values());
     this.servers =
         wlsServerConfigs != null ? new ArrayList<>(wlsServerConfigs.values()) : new ArrayList<>();
     this.serverTemplates =
         wlsServerTemplates != null ? new ArrayList<>(wlsServerTemplates.values()) : null;
     this.wlsMachineConfigs = wlsMachineConfigs;
     this.name = name;
     this.adminServerName = adminServerName;
     // set domainConfig for each WlsClusterConfig
     if (wlsClusterConfigs != null) {
       for (WlsClusterConfig wlsClusterConfig : this.configuredClusters) {
         wlsClusterConfig.setWlsDomainConfig(this);
       }
     }
   }
 
   /**
    * Create a new WlsDomainConfig object using the json result from the WLS REST call.
    *
    * @param jsonResult A String containing the JSON response from the WLS REST call
    * @return A new WlsDomainConfig object created with information from the JSON response
    */
   public static WlsDomainConfig create(String jsonResult) {
     ParsedJson parsedResult = parseJson(jsonResult);
     return WlsDomainConfig.create(parsedResult);
   }
 
   /**
    * Create a new WlsDomainConfig object based on the parsed JSON result from WLS admin server.
    *
    * @param parsedResult ParsedJson object containing the parsed JSON result
    * @return A new WlsDomainConfig object based on the provided parsed JSON result
    */
   private static WlsDomainConfig create(ParsedJson parsedResult) {
     if (parsedResult == null) {
       // return empty WlsDomainConfig if no parsedResult is provided
       return new WlsDomainConfig(null);
     }
 
     final String name = parsedResult.domainName;
     final String adminServerName = parsedResult.adminServerName;
     Map<String, WlsClusterConfig> wlsClusterConfigs = new HashMap<>();
     Map<String, WlsServerConfig> wlsServerConfigs = new HashMap<>();
     Map<String, WlsServerConfig> wlsServerTemplates = new HashMap<>();
     Map<String, WlsMachineConfig> wlsMachineConfigs = new HashMap<>();
 
     // process list of server templates
     if (parsedResult.serverTemplates != null) {
       for (Map<String, Object> thisServerTemplate : parsedResult.serverTemplates) {
         WlsServerConfig wlsServerTemplate = WlsServerConfig.create(thisServerTemplate);
         wlsServerTemplates.put(wlsServerTemplate.getName(), wlsServerTemplate);
       }
     }
     // process list of clusters (Note: must process server templates before processing clusters)
     if (parsedResult.clusters != null) {
       for (Map<String, Object> clusterConfig : parsedResult.clusters) {
         WlsClusterConfig wlsClusterConfig =
             WlsClusterConfig.create(clusterConfig, wlsServerTemplates, name);
         wlsClusterConfigs.put(wlsClusterConfig.getClusterName(), wlsClusterConfig);
       }
     }
     // process list of statically configured servers
     if (parsedResult.servers != null) {
       for (Map<String, Object> thisServer : parsedResult.servers) {
         WlsServerConfig wlsServerConfig = WlsServerConfig.create(thisServer);
         wlsServerConfigs.put(wlsServerConfig.getName(), wlsServerConfig);
         String clusterName = WlsServerConfig.getClusterNameFromJsonMap(thisServer);
         if (clusterName != null) {
           WlsClusterConfig wlsClusterConfig =
               wlsClusterConfigs.computeIfAbsent(clusterName, WlsClusterConfig::new);
           wlsClusterConfig.addServerConfig(wlsServerConfig);
         }
       }
     }
     // process list of machines
     if (parsedResult.machines != null) {
       for (Map<String, Object> machineConfig : parsedResult.machines) {
         WlsMachineConfig wlsMachineConfig = WlsMachineConfig.create(machineConfig);
         wlsMachineConfigs.put(wlsMachineConfig.getName(), wlsMachineConfig);
       }
     }
     return new WlsDomainConfig(
         name,
         adminServerName,
         wlsClusterConfigs,
         wlsServerConfigs,
         wlsServerTemplates,
         wlsMachineConfigs);
   }
 
   private static String getSearchFields() {
     return "'name' ";
   }
 
 
/** Parse the json string and return a list with a map. */
 private static ParsedJson parseJson(String jsonString){
    ParsedJson parsedResult = new ParsedJson();
    try {
      JsonParser parser = new JsonParser();
      JsonElement jsonElement = parser.parse(jsonString);
      JsonObject jsonObject = jsonElement.getAsJsonObject();
      parsedResult.domainName = jsonObject.get("name").getAsString();
      parsedResult.adminServerName = jsonObject.get("adminServerName").getAsString();
      JsonArray clusters = jsonObject.get("clusters").getAsJsonArray();
      if (clusters != null) {
        for (JsonElement cluster : clusters) {
          JsonObject clusterObject = cluster.getAsJsonObject();
          String clusterName = clusterObject.get("name").getAsString();
          JsonArray servers = clusterObject.get("servers").getAsJsonArray();
          if (servers != null) {
            for (JsonElement server : servers) {
              JsonObject serverObject = server.getAsJsonObject();
              String serverName = serverObject.get("name").getAsString();
              String serverType = serverObject.get("type").getAsString();
              String clusterNameFromJsonMap = WlsServerConfig.getClusterNameFromJsonMap(serverObject);
              if (clusterNameFromJsonMap != null) {
                clusterName = clusterNameFromJsonMap;
              }
              if (serverType.equals("Server")) {
                parsedResult.servers.add(serverObject);
              } else if (serverType.equals("ServerTemplate")) {
                parsedResult.serverTemplates.add(serverObject);
              }
            }
          }
          parsedResult.clusters.add(clusterObject);
        }
      }
      JsonArray machines = jsonObject.get("machines").getAsJsonArray();
      if (machines != null) {
        for (JsonElement machine : machines) {
          JsonObject machineObject = machine.getAsJsonObject();
          parsedResult.machines.add(machineObject);
        }
      }
    } catch   
 }

 

}