package net.glowstone.util;
 
 import com.google.common.collect.ImmutableList;
 import org.bukkit.Location;
 import org.bukkit.block.BlockFace;
 import org.bukkit.util.BlockVector;
 
 import java.util.List;
 
 import static org.bukkit.block.BlockFace.EAST;
 import static org.bukkit.block.BlockFace.EAST_NORTH_EAST;
 import static org.bukkit.block.BlockFace.EAST_SOUTH_EAST;
 import static org.bukkit.block.BlockFace.NORTH;
 import static org.bukkit.block.BlockFace.NORTH_EAST;
 import static org.bukkit.block.BlockFace.NORTH_NORTH_EAST;
 import static org.bukkit.block.BlockFace.NORTH_NORTH_WEST;
 import static org.bukkit.block.BlockFace.NORTH_WEST;
 import static org.bukkit.block.BlockFace.SOUTH;
 import static org.bukkit.block.BlockFace.SOUTH_EAST;
 import static org.bukkit.block.BlockFace.SOUTH_SOUTH_EAST;
 import static org.bukkit.block.BlockFace.SOUTH_SOUTH_WEST;
 import static org.bukkit.block.BlockFace.SOUTH_WEST;
 import static org.bukkit.block.BlockFace.WEST;
 import static org.bukkit.block.BlockFace.WEST_NORTH_WEST;
 import static org.bukkit.block.BlockFace.WEST_SOUTH_WEST;
 
 /**
  * A static class housing position-related utilities and constants.
  *
  * @author Graham Edgecombe
  */
 public final class Position {
 
     /**
      * Common Rotation values used blocks such as Signs, Skulls, and Banners. The order relates to
      * the data/tag that is applied to the block on placing.
      */
     public static final List<BlockFace> ROTATIONS = ImmutableList
             .of(NORTH, NORTH_NORTH_EAST, NORTH_EAST, EAST_NORTH_EAST, EAST, EAST_SOUTH_EAST,
                     SOUTH_EAST, SOUTH_SOUTH_EAST, SOUTH, SOUTH_SOUTH_WEST, SOUTH_WEST,
                     WEST_SOUTH_WEST, WEST, WEST_NORTH_WEST, NORTH_WEST, NORTH_NORTH_WEST);
 
     private Position() {
     }
 
     /**
      * Gets an integer approximation of the yaw between 0 and 255.
      *
      * @param loc The location to get the value from.
      * @return An integer approximation of the yaw.
      */
     public static int getIntYaw(Location loc) {
         return (int) (loc.getYaw() % 360 / 360 * 256);
     }
 
     /**
      * Gets an integer approximation of the pitch between 0 and 255.
      *
      * @param loc The location to get the value from.
      * @return An integer approximation of the yaw.
      */
     public static int getIntPitch(Location loc) {
         return (int) (loc.getPitch() % 360 / 360 * 256);
     }
 
     /**
      * Gets an integer approximation of the head-yaw rotation between 0 and 255.
      *
      * @param headYaw the head-yaw rotation value.
      * @return An integer approximation of the head-yaw rotation value.
      */
     public static int getIntHeadYaw(float headYaw) {
         return (int) (headYaw % 360 / 360 * 256);
     }
 
 
/** Return true if between two locations there has been a change in the position. */
 public static boolean hasMoved(Location first, Location second){
        return first.getX() != second.getX() || first.getY() != second.getY() || first.getZ() != second.getZ();
    }
    
        /**
        * Gets the block vector representing the location.
        *
        * @param loc The location to get the value from.
        * @return The block vector representing the location.
        */
        public static BlockVector getBlockVector(Location loc) {
            return new BlockVector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }
    
        /**
        * Gets the block vector representing the location.
        *
        * @param loc The location to get the value from.
        * @return The block vector representing the location.
        */
        public static BlockVector getBlockVector(org.bukkit.Location loc) {
            return new BlockVector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }
    
        /**
        * Gets the block vector representing the location.
        *
        * @param loc The location to get the value from.
        * @return The block vector representing the location.
        */
        public static BlockVector getBlockVector(net.glowstone.entity.GlowEntity loc) {
            return new BlockVector(loc.getLocation().getBlockX(), loc.getLocation().getBlockY(), loc.getLocation().getBlockZ());
        }
    
        /**
        * Gets the block vector representing the location.
        *
        * @param loc The location to get the value from.
        * @return The block vector representing the location.
        */
        public static BlockVector getBlockVector(net.glowstone.entity.GlowPlayer loc) {
            return new BlockVector(loc.getLocation().getBlockX(), loc.getLocation().getBlockY(), loc.getLocation().getBlockZ());
        }
    
        /**
        * Gets the block vector representing the location.
        *
        * @param loc The location to get the value from.
        * @return The block vector representing the location.
        */
        public static BlockVector getBlock      
 }

 

}