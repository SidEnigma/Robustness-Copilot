package org.matsim.contrib.osm.networkReader;
 
 import org.apache.log4j.Logger;
 import org.matsim.api.core.v01.Coord;
 import org.matsim.api.core.v01.Id;
 import org.matsim.api.core.v01.network.Link;
 import org.matsim.api.core.v01.network.Network;
 import org.matsim.api.core.v01.network.NetworkFactory;
 import org.matsim.api.core.v01.network.Node;
 import org.matsim.core.network.NetworkUtils;
 import org.matsim.core.utils.geometry.CoordUtils;
 import org.matsim.core.utils.geometry.CoordinateTransformation;
 
 import java.nio.file.Path;
 import java.nio.file.Paths;
 import java.util.*;
 import java.util.concurrent.ConcurrentMap;
 import java.util.concurrent.Executors;
 import java.util.function.BiPredicate;
 import java.util.function.Consumer;
 import java.util.function.Predicate;
 
 /**
  * Class for converting osm-networks into matsim-networks. This class uses the binary osm.pbf format as an input. Suitable
  * input files can be found at https://download.geofabrik.de
  * <p>
  * Examples on how to use the reader can be found in {@link org.matsim.contrib.osm.examples}
  * <p>
  * For the most common highway tags the {@link LinkProperties} class contains default properties for the
  * corresponding links in the matsim-network (e.g. speed, number of lanes). Those default properties may be overridden
  * with custom link properties using the {@link SupersonicOsmNetworkReader.Builder#addOverridingLinkProperties(String, LinkProperties)}
  * method of the Builder.
  */
 public class SupersonicOsmNetworkReader {
 
     private static final Logger log = Logger.getLogger(SupersonicOsmNetworkReader.class);
 
     private static final Set<String> reverseTags = new HashSet<>(Arrays.asList("-1", "reverse"));
     private static final Set<String> oneWayTags = new HashSet<>(Arrays.asList("yes", "true", "1"));
     private static final Set<String> notOneWayTags = new HashSet<>(Arrays.asList("no", "false", "0"));
 
     private final Predicate<Long> preserveNodeWithId;
     private final AfterLinkCreated afterLinkCreated;
     private final double freeSpeedFactor;
     private final double adjustCapacityLength;
     private final BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy;
     final OsmNetworkParser parser;
 
     private Network network;
 
     SupersonicOsmNetworkReader(OsmNetworkParser parser,
                                Predicate<Long> preserveNodeWithId,
                                BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy,
                                AfterLinkCreated afterLinkCreated,
                                double freeSpeedFactor, double adjustCapacityLength) {
         this.parser = parser;
         this.preserveNodeWithId = preserveNodeWithId;
         this.includeLinkAtCoordWithHierarchy = includeLinkAtCoordWithHierarchy;
         this.afterLinkCreated = afterLinkCreated;
         this.freeSpeedFactor = freeSpeedFactor;
         this.adjustCapacityLength = adjustCapacityLength;
     }
 
 
/** Creates a function to adjust the free speed for urban connections. */

static AfterLinkCreated adjustFreespeed(final double factor) {
    return (link, osmTags) -> {
        if (osmTags.containsKey("highway")) {
            String highwayType = osmTags.get("highway");
            if (highwayType.equals("residential") || highwayType.equals("living_street")) {
                link.setFreespeed(link.getFreespeed() * factor);
            }
        }
    };
}
 

}