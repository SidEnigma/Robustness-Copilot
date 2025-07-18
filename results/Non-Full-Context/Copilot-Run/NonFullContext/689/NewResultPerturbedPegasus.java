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
 
 
/** The object is created using a "clusters" item from the WLS call. */

static WlsClusterConfig create(Map<String, Object> clusterConfigMap, Map<String, WlsServerConfig> serverTemplates, String domainName) {
    WlsClusterConfig clusterConfig = new WlsClusterConfig();

    // Set cluster name
    clusterConfig.name = (String) clusterConfigMap.get("name");

    // Set dynamic servers config if available
    if (clusterConfigMap.containsKey("dynamicServersConfig")) {
        Map<String, Object> dynamicServersConfigMap = (Map<String, Object>) clusterConfigMap.get("dynamicServersConfig");
        clusterConfig.dynamicServersConfig = new WlsDynamicServersConfig(dynamicServersConfigMap);
    }

    // Set WLS domain config
    clusterConfig.wlsDomainConfig = new WlsDomainConfig(domainName);

    // Process server templates
    List<Map<String, Object>> serverTemplateList = (List<Map<String, Object>>) clusterConfigMap.get("serverTemplates");
    for (Map<String, Object> serverTemplateMap : serverTemplateList) {
        String serverTemplateName = (String) serverTemplateMap.get("name");
        WlsServerConfig serverTemplate = serverTemplates.get(serverTemplateName);
        if (serverTemplate != null) {
            clusterConfig.servers.add(serverTemplate);
        }
    }

    return clusterConfig;
}
 

}