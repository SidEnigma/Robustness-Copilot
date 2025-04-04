package org.matsim.contrib.sumo;
 
 import com.google.common.collect.Sets;
 import org.apache.commons.csv.CSVFormat;
 import org.apache.commons.csv.CSVPrinter;
 import org.apache.logging.log4j.LogManager;
 import org.apache.logging.log4j.Logger;
 import org.locationtech.jts.geom.Geometry;
 import org.matsim.api.core.v01.Coord;
 import org.matsim.api.core.v01.Id;
 import org.matsim.api.core.v01.TransportMode;
 import org.matsim.api.core.v01.network.*;
 import org.matsim.contrib.osm.networkReader.LinkProperties;
 import org.matsim.core.network.NetworkUtils;
 import org.matsim.core.network.algorithms.NetworkCleaner;
 import org.matsim.core.utils.geometry.CoordinateTransformation;
 import org.matsim.core.utils.geometry.geotools.MGC;
 import org.matsim.core.utils.geometry.transformations.TransformationFactory;
 import org.matsim.core.utils.gis.ShapeFileReader;
 import org.matsim.core.utils.io.IOUtils;
 import org.matsim.lanes.*;
 import org.xml.sax.SAXException;
 import picocli.CommandLine;
 
 import javax.xml.parsers.ParserConfigurationException;
 import java.io.BufferedWriter;
 import java.io.File;
 import java.io.IOException;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.util.*;
 import java.util.concurrent.Callable;
 import java.util.stream.Collectors;
 
 import static org.matsim.lanes.LanesUtils.calculateAndSetCapacity;
 
 /**
  * Converter for sumo networks
  *
  * @author rakow
  */
 public class SumoNetworkConverter implements Callable<Integer> {
 
     private static final Logger log = LogManager.getLogger(SumoNetworkConverter.class);
 
     @CommandLine.Parameters(arity = "1..*", paramLabel = "INPUT", description = "Input file(s)")
     private List<Path> input;
 
     @CommandLine.Option(names = "--output", description = "Output xml file", required = true)
     private Path output;
 
     @CommandLine.Option(names = "--shp", description = "Optional shape file used for filtering")
     private Path shapeFile;
 
     @CommandLine.Option(names = "--from-crs", description = "Coordinate system of input data", required = true)
     private String fromCRS;
 
     @CommandLine.Option(names = "--to-crs", description = "Desired output coordinate system", required = true)
     private String toCRS;
 
     private SumoNetworkConverter(List<Path> input, Path output, Path shapeFile, String fromCRS, String toCRS) {
         this.input = input;
         this.output = output;
         this.shapeFile = shapeFile;
         this.fromCRS = fromCRS;
         this.toCRS = toCRS;
     }
 
     private SumoNetworkConverter() {
     }
 
     /**
      * Creates a new converter instance.
      *
      * @param input   List of input files, if multiple they will be merged
      * @param output  output path
      * @param fromCRS coordinate system of input data
      * @param toCRS   desired coordinate system of network
      */
     public static SumoNetworkConverter newInstance(List<Path> input, Path output, String fromCRS, String toCRS) {
         return new SumoNetworkConverter(input, output, null, fromCRS, toCRS);
     }
 
     /**
      * Creates a new converter instance, with a shape file for filtering.
      *
      * @param shapeFile only include links in this shape file.
      * @see #newInstance(List, Path, String, String)
      */
     public static SumoNetworkConverter newInstance(List<Path> input, Path output, Path shapeFile, String fromCRS, String toCRS) {
         return new SumoNetworkConverter(input, output, shapeFile, fromCRS, toCRS);
     }
 
     /**
      * Reads network from input file.
      */
     public static SumoNetworkHandler readNetwork(File input) throws IOException, SAXException, ParserConfigurationException {
         return SumoNetworkHandler.read(input);
     }
 
     public static void main(String[] args) {
         System.exit(new CommandLine(new SumoNetworkConverter()).execute(args));
     }
 
     /**
      * Network area based on the cut-out.
      */
     private static Geometry calculateNetworkArea(Path shapeFile) {
         // only the first feature is used
         return ((Geometry) ShapeFileReader.getAllFeatures(shapeFile.toString()).iterator().next().getDefaultGeometry());
     }
 
     /**
      * Execute the converter, which includes conversion and writing the files
      *
      * @see #convert(Network, Lanes) .
      */
     @Override
     public Integer call() throws Exception {
 
 
         Network network = NetworkUtils.createNetwork();
         Lanes lanes = LanesUtils.createLanesContainer();
 
         SumoNetworkHandler handler = convert(network, lanes);
 
         calculateLaneCapacities(network, lanes);
 
         // This needs to run without errors, otherwise network is broken
         network.getLinks().values().forEach(link -> {
             LanesToLinkAssignment l2l = lanes.getLanesToLinkAssignments().get(link.getId());
             if (l2l != null)
                 LanesUtils.createLanes(link, l2l);
         });
 
         new NetworkWriter(network).write(output.toAbsolutePath().toString());
         new LanesWriter(lanes).write(output.toAbsolutePath().toString().replace(".xml", "-lanes.xml"));
 
         writeGeometry(handler, output.toAbsolutePath().toString().replace(".xml", "-linkGeometries.csv"));
 
         return 0;
     }
 
 
/** Based on {@link LanesUtils} calculate the capacity of lanes */
 public void calculateLaneCapacities(Network network, Lanes lanes){
        network.getLinks().values().forEach(link -> {
            LanesToLinkAssignment l2l = lanes.getLanesToLinkAssignments().get(link.getId());
            if (l2l != null)
                calculateAndSetCapacity(link, l2l);
        });
    }
    
        /**
        * Converts the network and lanes.
        *
        * @param network the network to convert
        * @param lanes   the lanes to convert
        * @return the handler containing the converted network and lanes
        */
        public SumoNetworkHandler convert(Network network, Lanes lanes) {
    
            SumoNetworkHandler handler = new SumoNetworkHandler(network, lanes);
    
            for (Path input : input) {
                SumoNetworkHandler.read(input, handler);
            }
    
            if (shapeFile != null) {
                Geometry networkArea = calculateNetworkArea(shapeFile);
                handler.filter(networkArea);
            }
    
            handler.convert(fromCRS, toCRS);
    
            return handler;
        }
    
        /**
        * Writes the geometry of the links to a csv file.
        *
        * @param handler the handler containing the converted network and lanes
        * @param output  the output file
        */
        public void writeGeometry(SumoNetworkHandler handler, String output) {
            try {
                BufferedWriter writer = IOUtils.getBufferedWriter(output);
                writer.write("linkId,x,y\n");
                handler.getNetwork().getLinks().values().forEach(link -> {
                    Coord coord = link.getCoord();
                    writer.write(link.getId() + "," + coord.getX() + "," + coord.getY() + "\n");
                });
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    
        /**
        * Converts the network and lanes.
        *
        * @param network  the network to convert
        * @param lanes   the        
 }

 

}