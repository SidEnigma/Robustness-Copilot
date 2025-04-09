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
 
   /**
    * Returns true if one of the servers in the cluster has the specified name.
    *
    * @param serverName the name to look for
    * @return true or false
    */
   public boolean hasNamedServer(String serverName) {
     return getServerConfigs().stream().anyMatch(c -> serverName.equals(c.getName()));
   }
 
   /**
    * Add a statically configured WLS server to this cluster.
    *
    * @param wlsServerConfig A WlsServerConfig object containing the configuration of the statically
    *     configured WLS server that belongs to this cluster
    * @return Cluster configuration
    */
   public synchronized WlsClusterConfig addServerConfig(WlsServerConfig wlsServerConfig) {
     servers.add(wlsServerConfig);
     return this;
   }
 
   /**
    * Returns the number of servers that are statically configured in this cluster.
    *
    * @return The number of servers that are statically configured in this cluster
    */
   public synchronized int getClusterSize() {
     return servers.size();
   }
 
   public synchronized int getMaxClusterSize() {
     return hasDynamicServers() ? getClusterSize() + getMaxDynamicClusterSize() : getClusterSize();
   }
 
   /**
    * Returns the minimum size of the cluster.
    * @return  For static clusters, the minimum size can be 0.  Dynamic servers will return the configured value.
    */
   public int getMinClusterSize() {
     return hasDynamicServers() ? getMinDynamicClusterSize() : 0;
   }
 
   /**
    * Returns the name of the cluster that this WlsClusterConfig is created for.
    *
    * @return the name of the cluster that this WlsClusterConfig is created for
    */
   public String getClusterName() {
     return name;
   }
 
   /**
    * Returns the name of the cluster that this WlsClusterConfig is created for.
    *
    * @return the name of the cluster that this WlsClusterConfig is created for
    */
   public String getName() {
     return name;
   }
 
   public WlsDynamicServersConfig getDynamicServersConfig() {
     return this.dynamicServersConfig;
   }
 
   /**
    * Returns the WlsDomainConfig object for the WLS domain that this cluster belongs to.
    *
    * @return the WlsDomainConfig object for the WLS domain that this cluster belongs to
    */
   public WlsDomainConfig getWlsDomainConfig() {
     return wlsDomainConfig;
   }
 
   /**
    * Associate this cluster to the WlsDomainConfig object for the WLS domain that this cluster
    * belongs to.
    *
    * @param wlsDomainConfig the WlsDomainConfig object for the WLS domain that this cluster belongs
    *     to
    */
   public void setWlsDomainConfig(WlsDomainConfig wlsDomainConfig) {
     this.wlsDomainConfig = wlsDomainConfig;
   }
 
   /**
    * Returns a sorted list of server configurations for servers that belong to this cluster,
    * which includes both statically configured servers and dynamic servers.
    *
    * @return A sorted list of WlsServerConfig containing configurations of servers that belong to
    *     this cluster
    */
   public synchronized List<WlsServerConfig> getServerConfigs() {
     int dcsize = dynamicServersConfig == null ? 0 : dynamicServersConfig.getDynamicClusterSize();
     List<WlsServerConfig> result = new ArrayList<>(dcsize + servers.size());
     Optional.ofNullable(dynamicServersConfig).map(WlsDynamicServersConfig::getServerConfigs)
         .ifPresent(dynamicServers -> dynamicServers.forEach(item -> result.add(item)));
     result.addAll(servers);
     result.sort(Comparator.comparing((WlsServerConfig sc) -> OperatorUtils.getSortingString(sc.getName())));
     return result;
   }
 
   public List<WlsServerConfig> getServers() {
     return this.servers;
   }
 
   /**
    * Whether the cluster contains any statically configured servers.
    *
    * @return True if the cluster contains any statically configured servers
    */
   public synchronized boolean hasStaticServers() {
     return !servers.isEmpty();
   }
 
   /**
    * Whether the cluster contains any dynamic servers.
    *
    * @return True if the cluster contains any dynamic servers
    */
   public boolean hasDynamicServers() {
     return dynamicServersConfig != null;
   }
 
   /**
    * Returns the current size of the dynamic cluster (the number of dynamic server instances allowed
    * to be created).
    *
    * @return the current size of the dynamic cluster, or -1 if there is no dynamic servers in this
    *     cluster
    */
   public int getDynamicClusterSize() {
     return dynamicServersConfig != null ? dynamicServersConfig.getDynamicClusterSize() : -1;
   }
 
   /**
    * Returns the maximum size of the dynamic cluster.
    *
    * @return the maximum size of the dynamic cluster, or -1 if there is no dynamic servers in this
    *     cluster
    */
   public int getMaxDynamicClusterSize() {
     return dynamicServersConfig != null ? dynamicServersConfig.getMaxDynamicClusterSize() : -1;
   }
 
   /**
    * Returns the minimum size of the dynamic cluster.
    *
    * @return the minimum size of the dynamic cluster, or -1 if there is no dynamic servers in this
    *     cluster
    */
   public int getMinDynamicClusterSize() {
     return dynamicServersConfig != null ? dynamicServersConfig.getMinDynamicClusterSize() : -1;
   }
 
 
/** Verify whether the WebLogic domain already has all the machines configured for use by the  dynamic cluster. */

public boolean verifyMachinesConfigured(String machineNamePrefix, int numMachinesNeeded) {
    int numMachinesConfigured = 0;
    
    // Iterate through all the server configurations
    for (WlsServerConfig serverConfig : servers) {
        String machineName = serverConfig.getMachineName();
        
        // Check if the machine name matches the prefix
        if (machineName != null && machineName.startsWith(machineNamePrefix)) {
            numMachinesConfigured++;
        }
        
        // Break the loop if the required number of machines are already configured
        if (numMachinesConfigured >= numMachinesNeeded) {
            break;
        }
    }
    
    // Check if the required number of machines are already configured
    return numMachinesConfigured >= numMachinesNeeded;
}
 

}