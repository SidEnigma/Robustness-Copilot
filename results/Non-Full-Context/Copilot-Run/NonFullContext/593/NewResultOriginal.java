package net.glowstone.entity;
 
 import com.google.common.collect.Sets;
 import net.glowstone.entity.ai.EntityDirector;
 import net.glowstone.entity.ai.MobState;
 import net.glowstone.net.message.play.player.InteractEntityMessage;
 import net.glowstone.util.InventoryUtil;
 import org.bukkit.GameMode;
 import org.bukkit.Location;
 import org.bukkit.Material;
 import org.bukkit.Statistic;
 import org.bukkit.entity.Animals;
 import org.bukkit.entity.EntityType;
 import org.bukkit.inventory.ItemStack;
 import org.bukkit.util.Vector;
 
 import java.util.EnumSet;
 import java.util.Set;
 
 /**
  * Represents an Animal, such as a Cow.
  */
 public class GlowAnimal extends GlowAgeable implements Animals {
 
     private static final Set<Material> DEFAULT_BREEDING_FOODS =
             Sets.immutableEnumSet(EnumSet.noneOf(Material.class));
 
     private static final double VERTICAL_GRAVITY_ACCEL = -0.04;
 
     /**
      * Creates a new ageable animal.
      *
      * @param location The location of the animal.
      * @param type The type of animal.
      * @param maxHealth The max health of this animal.
      */
     public GlowAnimal(Location location, EntityType type, double maxHealth) {
         super(location, type, maxHealth);
         if (type != null) {
             EntityDirector.registerEntityMobState(type, MobState.IDLE, "look_around");
             EntityDirector.registerEntityMobState(type, MobState.IDLE, "look_player");
         }
         setState(MobState.IDLE);
 
         setGravityAccel(new Vector(0, VERTICAL_GRAVITY_ACCEL, 0));
     }
 
     @Override
     protected int getAmbientDelay() {
         return 120;
     }
 
 
/** Determines whether this entity can eat an item while healthy, and if so, applies the effects  of eating it. */

protected boolean tryFeed(Material type, GlowPlayer player) {
    // Check if the entity is healthy and can eat the item
    if (isHealthy() && canEat(type)) {
        // Apply the effects of eating the item
        applyEatingEffects(type, player);
        return true;
    }
    return false;
}
 

}