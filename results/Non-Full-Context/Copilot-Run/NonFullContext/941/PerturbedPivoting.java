package org.matsim.contrib.sumo;
 
 import org.matsim.api.core.v01.Coord;
 import org.matsim.core.utils.geometry.CoordinateTransformation;
 import org.xml.sax.Attributes;
 import org.xml.sax.SAXException;
 import org.xml.sax.helpers.DefaultHandler;
 
 import javax.annotation.Nullable;
 import javax.xml.parsers.ParserConfigurationException;
 import javax.xml.parsers.SAXParser;
 import javax.xml.parsers.SAXParserFactory;
 import java.io.File;
 import java.io.IOException;
 import java.util.*;
 import java.util.stream.Collectors;
 
 /**
  * Parses and holds specific information from sumo network xml file.
  */
 public class SumoNetworkHandler extends DefaultHandler {
 
     final double[] netOffset = new double[2];
 
     /**
      * All junctions.
      */
     final Map<String, Junction> junctions = new HashMap<>();
 
     /**
      * Edges mapped by id.
      */
     final Map<String, Edge> edges = new HashMap<>();
 
     /**
      * Map lane id to their edge.
      */
     final Map<String, Edge> lanes = new HashMap<>();
 
     /**
      * All connections mapped by the origin (from).
      */
     final Map<String, List<Connection>> connections = new HashMap<>();
 
     /**
      * Parsed link types.
      */
     final Map<String, Type> types = new HashMap<>();
 
     /**
      * Stores current parsed edge.
      */
     private Edge tmpEdge = null;
 
     private SumoNetworkHandler() {
     }
 
     public Map<String, Junction> getJunctions() {
         return junctions;
     }
 
     public Map<String, Edge> getEdges() {
         return edges;
     }
 
     public Map<String, Edge> getLanes() {
         return lanes;
     }
 
     public Map<String, List<Connection>> getConnections() {
         return connections;
     }
 
     public Map<String, Type> getTypes() {
         return types;
     }
 
 
/** Creates a sumo handler by reading the data in the xml file. */
  static SumoNetworkHandler read(File file) throws ParserConfigurationException, SAXException, IOException{}

 

}