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
 
 
/** The server template and index number are used to create a dynamic server config. */

static WlsDynamicServerConfig create(String name, int index, String clusterName, String domainName, boolean calculatedListenPorts, WlsServerConfig serverTemplate) {
    int listenPort = calculatedListenPorts ? (serverTemplate.getListenPort() + index) : serverTemplate.getListenPort();
    int sslListenPort = calculatedListenPorts ? (serverTemplate.getSslListenPort() + index) : serverTemplate.getSslListenPort();
    int napListenPort = calculatedListenPorts ? (serverTemplate.getNapListenPort() + index) : serverTemplate.getNapListenPort();

    List<NetworkAccessPoint> networkAccessPoints = new ArrayList<>();
    for (NetworkAccessPoint nap : serverTemplate.getNetworkAccessPoints()) {
        int napPort = calculatedListenPorts ? (nap.getListenPort() + index) : nap.getListenPort();
        NetworkAccessPoint newNap = new NetworkAccessPoint(nap.getName(), nap.getProtocol(), napPort);
        networkAccessPoints.add(newNap);
    }

    String machineName = serverTemplate.getMachineName();
    if (machineName != null) {
        machineName = machineName + "-" + index;
    }

    return new WlsDynamicServerConfig(name, listenPort, serverTemplate.getListenAddress(), sslListenPort, machineName, serverTemplate.getAdminPort(), networkAccessPoints);
}
 

}