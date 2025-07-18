// Copyright (c) 2018, 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.operator.wlsconfig;
 
 import java.util.ArrayList;
 import java.util.List;
 
 /** Contains configuration of a WLS server that belongs to a dynamic cluster. */
 public class WlsDynamicServerConfig extends WlsServerConfig {
 
   // default listen ports per WebLogic DynamicServersMBean
   static final int DEFAULT_LISTEN_PORT_RANGE_BASE = 7100;
   static final int DEFAULT_SSL_LISTEN_PORT_RANGE_BASE = 8100;
   static final int DEFAULT_NAP_LISTEN_PORT_RANGE_BASE = 9100;
 
   /**
    * private constructor. Use {@link #create(String, int, String, String, boolean, WlsServerConfig)}
    * for creating an instance of WlsDynamicServerConfig instead.
    *
    * @param name Name of the dynamic server
    * @param listenPort list port of the dynamic server
    * @param listenAddress listen address of the dynamic server
    * @param sslListenPort SSL listen port of the dynamic server
    * @param machineName machine name of the dynamic server
    * @param adminPort administration port if administration port is enabled
    * @param networkAccessPoints network access points or channels configured for this dynamic server
    */
   private WlsDynamicServerConfig(
       String name,
       Integer listenPort,
       String listenAddress,
       Integer sslListenPort,
       String machineName,
       Integer adminPort,
       List<NetworkAccessPoint> networkAccessPoints) {
     super(
         name,
         listenAddress,
         machineName,
         listenPort,
         sslListenPort,
         adminPort,
         networkAccessPoints);
   }
 
 
/** Create a dynamic server configuration using the server model and index number of that server. */
  static WlsDynamicServerConfig create(String name, int index, String clusterName, String domainName, boolean calculatedListenPorts, WlsServerConfig serverTemplate){
    WlsDynamicServerConfig dynamicServerConfig = new WlsDynamicServerConfig(
        name,
        null,
        null,
        null,
        null,
        null,
        null);
    dynamicServerConfig.setName(name);
    dynamicServerConfig.setClusterName(clusterName);
    dynamicServerConfig.setDomainName(domainName);
    dynamicServerConfig.setCalculatedListenPorts(calculatedListenPorts);
    dynamicServerConfig.setServerTemplate(serverTemplate);
    dynamicServerConfig.setListenPort(serverTemplate.getListenPort());
    dynamicServerConfig.setSslListenPort(serverTemplate.getSslListenPort());
    dynamicServerConfig.setAdminPort(serverTemplate.getAdminPort());
    dynamicServerConfig.setMachineName(serverTemplate.getMachineName());
    dynamicServerConfig.setListenAddress(serverTemplate.getListenAddress());
    dynamicServerConfig.setNetworkAccessPoints(serverTemplate.getNetworkAccessPoints());
    dynamicServerConfig.setListenPort(serverTemplate.getListenPort());
    dynamicServerConfig.setSslListenPort(serverTemplate.getSslListenPort());
    dynamicServerConfig.setAdminPort(serverTemplate.getAdminPort());
    dynamicServerConfig.setListenPort(serverTemplate.getListenPort());
    dynamicServerConfig.setSslListenPort(serverTemplate.getSslListenPort());
    dynamicServerConfig.setAdminPort(serverTemplate.getAdminPort());
    dynamicServerConfig.setListenPort(serverTemplate.getListenPort());
    dynamicServerConfig.setSslListenPort(serverTemplate.getSslListenPort());
    dynamicServerConfig.setAdminPort(serverTemplate.getAdminPort());
    dynamicServerConfig.setListenPort(serverTemplate.getListenPort());
    dynamicServerConfig.setSslListenPort(serverTemplate.getSslListenPort());
    dynamicServerConfig.setAdminPort(serverTemplate.getAdminPort());
    dynamicServerConfig.setListenPort(serverTemplate.getListenPort());
    dynamicServerConfig.setSslListenPort(serverTemplate.getSslListenPort());
    dynamicServerConfig.setAdminPort(serverTemplate.getAdminPort());      
  }

 

}