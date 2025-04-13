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
 
 
/** Returns true if the update to the REST request, done usiing a JSON file from a dynamic cluster, was successful, false otherwise */
  static boolean checkUpdateDynamicClusterSizeJsonResult(String jsonResult){
    return jsonResult.contains("\"status\":\"success\"");     
  }

 

}