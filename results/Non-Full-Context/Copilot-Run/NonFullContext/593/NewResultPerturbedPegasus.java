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
 
 
/** If this entity can eat an item while healthy, and if so, how it effects the environment. */

protected boolean tryFeed(Material type, GlowPlayer player) {
    // Check if the entity is healthy and can eat the specified type of food
    if (isHealthy() && DEFAULT_BREEDING_FOODS.contains(type)) {
        // Consume the food item
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && item.getType() == type) {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItemInMainHand(item);

            // Increase the player's food saturation level
            player.setFoodLevel(player.getFoodLevel() + 1);
            player.setSaturation(player.getSaturation() + 0.6f);

            // Increase the player's statistics
            player.incrementStatistic(Statistic.ITEM_USED.get(type));

            // Return true to indicate that the feeding was successful
            return true;
        }
    }

    // Return false if the entity is not healthy or cannot eat the specified type of food
    return false;
}
 

}