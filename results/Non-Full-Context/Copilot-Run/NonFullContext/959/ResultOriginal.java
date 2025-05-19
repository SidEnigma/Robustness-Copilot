// Copyright (c) 2017, 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.operator.wlsconfig;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 
 import com.fasterxml.jackson.annotation.JsonIgnore;
 import oracle.kubernetes.operator.helpers.LegalNames;
 import org.apache.commons.lang3.builder.EqualsBuilder;
 import org.apache.commons.lang3.builder.HashCodeBuilder;
 import org.apache.commons.lang3.builder.ToStringBuilder;
 
 /** Contains configuration of a WebLogic server. */
 public class WlsServerConfig {
   private String name;
   private Integer listenPort;
   private String listenAddress;
   private String clusterName;
   private Integer sslListenPort;
   private String machineName;
   private Integer adminPort;
   private List<NetworkAccessPoint> networkAccessPoints;
 
   public WlsServerConfig() {
   }
 
   /**
    * Creates a server configuration.
    *
    * @param name the server name
    * @param listenAddress the listen address
    * @param listenPort the listen port
    */
   public WlsServerConfig(String name, String listenAddress, int listenPort) {
     this.name = name;
     this.listenAddress = listenAddress;
     this.listenPort = listenPort;
   }
 
   /**
    * Construct a WlsServerConfig object using values provided.
    *
    * @param name Name of the WLS server
    * @param listenAddress Configured listen address for this WLS server
    * @param machineName Configured machine name for this WLS server
    * @param listenPort Configured listen port for this WLS server
    * @param sslListenPort Configured SSL listen port for this WLS server
    * @param adminPort Configured domain wide administration port
    * @param networkAccessPoints List of NetworkAccessPoint containing channels configured for this
    */
   public WlsServerConfig(
       String name,
       String listenAddress,
       String machineName,
       Integer listenPort,
       Integer sslListenPort,
       Integer adminPort,
       List<NetworkAccessPoint> networkAccessPoints) {
     this.name = name;
     this.listenAddress = listenAddress;
     this.machineName = machineName;
     this.listenPort = listenPort;
     this.sslListenPort = sslListenPort;
     this.adminPort = adminPort;
     this.networkAccessPoints = networkAccessPoints;
   }
 
 
/** Creates a WLSServerConfig object using an "servers" or "serverTemplates" item parsed from JSON  result from WLS REST call. */
 static WlsServerConfig create(Map<String, Object> serverConfigMap){
    String name = (String) serverConfigMap.get("name");
    String listenAddress = (String) serverConfigMap.get("listenAddress");
    String machineName = (String) serverConfigMap.get("machineName");
    Integer listenPort = (Integer) serverConfigMap.get("listenPort");
    Integer sslListenPort = (Integer) serverConfigMap.get("sslListenPort");
    Integer adminPort = (Integer) serverConfigMap.get("adminPort");
    List<NetworkAccessPoint> networkAccessPoints = new ArrayList<>();
    List<Map<String, Object>> networkAccessPointList = (List<Map<String, Object>>) serverConfigMap.get("networkAccessPoints");
    if (networkAccessPointList != null) {
      for (Map<String, Object> networkAccessPointMap : networkAccessPointList) {
        networkAccessPoints.add(NetworkAccessPoint.create(networkAccessPointMap));
      }
    }
    return new WlsServerConfig(name, listenAddress, machineName, listenPort, sslListenPort, adminPort, networkAccessPoints);
  }
  
    public String getName() {
      return name;
    }
  
    public void setName(String name) {
      this.name = name;
    }
  
    public Integer getListenPort() {
      return listenPort;
    }
  
    public void setListenPort(Integer listenPort) {
      this.listenPort = listenPort;
    }
  
    public String getListenAddress() {
      return listenAddress;
    }
  
    public void setListenAddress(String listenAddress) {
      this.listenAddress = listenAddress;
    }
  
    public String getClusterName() {
      return clusterName;
    }
  
    public void setClusterName(String clusterName) {
      this.clusterName = clusterName;
    }
  
    public Integer getSslListenPort() {
      return sslListenPort;
    }
  
    public void setSslListenPort(Integer sslListenPort) {
      this.sslListenPort = ssl    
 }

 

}