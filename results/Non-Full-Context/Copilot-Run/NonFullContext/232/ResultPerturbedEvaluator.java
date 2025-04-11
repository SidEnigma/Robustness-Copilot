// Copyright (c) 2017, 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.operator.wlsconfig;
 
 import java.util.ArrayList;
 import java.util.Comparator;
 import java.util.List;
 import java.util.Map;
 import java.util.Optional;
 import javax.annotation.Nonnull;
 
 import oracle.kubernetes.utils.OperatorUtils;
 import org.apache.commons.lang3.builder.EqualsBuilder;
 import org.apache.commons.lang3.builder.HashCodeBuilder;
 import org.apache.commons.lang3.builder.ToStringBuilder;
 
 /** Contains configuration of a WLS cluster. */
 public class WlsClusterConfig {
 
   private String name;
   private final List<WlsServerConfig> servers = new ArrayList<>();
   private WlsDynamicServersConfig dynamicServersConfig;
 
   // owner -- don't include in toString, hashCode, equals
   private WlsDomainConfig wlsDomainConfig;
 
   public WlsClusterConfig() {
   }
 
   /**
    * Constructor for a static cluster when Json result is not available.
    *
    * @param clusterName Name of the WLS cluster
    */
   public WlsClusterConfig(String clusterName) {
     this.name = clusterName;
     this.dynamicServersConfig = null;
   }
 
   /**
    * Constructor that can also be used for a dynamic cluster.
    *
    * @param clusterName Name of the WLS cluster
    * @param dynamicServersConfig A WlsDynamicServersConfig object containing the dynamic servers
    *     configuration for this cluster
    */
   public WlsClusterConfig(String clusterName, WlsDynamicServersConfig dynamicServersConfig) {
     this.name = clusterName;
     this.dynamicServersConfig = dynamicServersConfig;
   }
 
   /**
    * Creates a WlsClusterConfig object using an "clusters" item parsed from JSON result from WLS
    * REST call.
    *
    * @param clusterConfigMap Map containing "cluster" item parsed from JSON result from WLS REST
    *     call
    * @param serverTemplates Map containing all server templates configuration read from the WLS
    *     domain
    * @param domainName Name of the WLS domain that this WLS cluster belongs to
    * @return A new WlsClusterConfig object created based on the JSON result
    */
   @SuppressWarnings("unchecked")
   static WlsClusterConfig create(
       Map<String, Object> clusterConfigMap,
       Map<String, WlsServerConfig> serverTemplates,
       String domainName) {
     String clusterName = (String) clusterConfigMap.get("name");
     WlsDynamicServersConfig dynamicServersConfig =
         WlsDynamicServersConfig.create(
             (Map<String, Object>) clusterConfigMap.get("dynamicServers"),
             serverTemplates,
             clusterName,
             domainName);
     // set dynamicServersConfig only if the cluster contains dynamic servers, i.e., its dynamic
     // servers configuration
     // contains non-null server template name
     if (dynamicServersConfig.getServerTemplate() == null) {
       dynamicServersConfig = null;
     }
     return new WlsClusterConfig(clusterName, dynamicServersConfig);
   }
 
   /**
    * Return the list of configuration attributes to be retrieved from the REST search request to the
    * WLS admin server. The value would be used for constructing the REST POST request.
    *
    * @return The list of configuration attributes to be retrieved from the REST search request to
    *     the WLS admin server. The value would be used for constructing the REST POST request.
    */
   static String getSearchPayload() {
     return "   fields: [ "
         + getSearchFields()
         + " ], "
         + "   links: [], "
         + "   children: { "
         + "      dynamicServers: { "
         + "      fields: [ "
         + WlsDynamicServersConfig.getSearchFields()
         + " ], "
         + "      links: [] "
         + "        }"
         + "    } ";
   }
 
   /**
    * Return the fields from cluster WLS configuration that should be retrieved from the WLS REST
    * request.
    *
    * @return A string containing cluster configuration fields that should be retrieved from the WLS
    *     REST request, in a format that can be used in the REST request payload
    */
   private static String getSearchFields() {
     return "'name' ";
   }
 
   /**
    * Checks the JSON result from the dynamic cluster size update REST request.
    *
    * @param jsonResult The JSON String result from the dynamic server cluster size update REST
    *     request
    * @return true if the result means the update was successful, false otherwise
    */
   static boolean checkUpdateDynamicClusterSizeJsonResult(String jsonResult) {
     final String expectedResult = "{}";
 
     boolean result = false;
     if (expectedResult.equals(jsonResult)) {
       result = true;
     }
     return result;
   }
 
 
/** In the event that one of the servers in the cluster bears the specified name, true is returned. */
 public boolean hasNamedServer(String serverName){
    boolean result = false;
    for (WlsServerConfig server : servers) {
      if (server.getName().equals(serverName)) {
        result = true;
        break;
      }
    }
    return result;
  }
  
    /**
      * Returns the name of the WLS cluster.
      *
      * @return The name of the WLS cluster
      */
    public String getName() {
      return name;
    }
  
    /**
      * Sets the name of the WLS cluster.
      *
      * @param name The name of the WLS cluster
      */
    public void setName(String name) {
      this.name = name;
    }
  
    /**
      * Returns the list of WLS servers in this cluster.
      *
      * @return The list of WLS servers in this cluster
      */
    public List<WlsServerConfig> getServers() {
      return servers;
    }
  
    /**
      * Adds a WLS server to this cluster.
      *
      * @param server The WLS server to add to this cluster
      */
    public void addServer(WlsServerConfig server) {
      servers.add(server);
    }
  
    /**
      * Returns the WLS dynamic servers configuration for this cluster.
      *
      * @return The WLS dynamic servers configuration for this cluster
      */
    public WlsDynamicServersConfig getDynamicServersConfig() {
      return dynamicServersConfig;
    }
  
    /**
      * Sets the WLS dynamic servers configuration for this cluster.
      *
      * @param dynamicServersConfig The WLS dynamic servers configuration for this cluster
      */
    public void setDynamicServersConfig(WlsDynamicServersConfig dynamicServersConfig) {
      this.dynamicServersConfig = dynamicServersConfig;
    }
  
    /**
      * Returns the WLS domain configuration that this cluster belongs to.
      *
      * @return The WLS domain configuration that this cluster belongs to
      */
    public WlsDomainConfig getWlsDomainConfig() {
      return    
 }

 

}