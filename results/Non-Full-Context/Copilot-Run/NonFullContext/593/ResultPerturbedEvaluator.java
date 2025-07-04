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
 
 
/** Returns true if the current entity can eat an item. If so, applies the effects of eating it, otherwise returns false. */
 protected boolean tryFeed(Material type, GlowPlayer player){
        if (DEFAULT_BREEDING_FOODS.contains(type)) {
            if (player.getGameMode() == GameMode.SURVIVAL) {
                player.getStatistic(Statistic.ANIMALS_BRED);
            }
            return true;
        }
        return false;
    }
    
        @Override
        public void onInteract(GlowPlayer player, InteractEntityMessage message) {
            if (message.getAction() == InteractEntityMessage.Action.INTERACT) {
                if (tryFeed(player.getItemInHand().getType(), player)) {
                    InventoryUtil.consumeItem(player.getItemInHand());
                }
            }
        }
    
        @Override
        public Set<Material> getBreedingFoods() {
            return DEFAULT_BREEDING_FOODS;
        }
    
        @Override
        public boolean isBaby() {
            return getAge() < 0;
        }
    
        @Override
        public void setBaby(boolean baby) {
            if (baby) {
                setAge(-1);
            } else {
                setAge(0);
            }
        }
    
        @Override
        public boolean isAdult() {
            return getAge() >= 0;
        }
    
        @Override
        public void setAdult(boolean adult) {
            if (adult) {
                setAge(0);
            } else {
                setAge(-1);
            }
        }
    
        @Override
        public boolean isAdultOrBaby() {
            return getAge() >= -1;
        }
    
        @Override
        public void setAdultOrBaby(boolean adultOrBaby) {
            if (adultOrBaby) {
                setAge(0);
            } else {
                setAge(-1);
            }
        }
    
        @Override
        public boolean isAdultOrOlder() {
            return getAge() >= 0;
        }
    
        @Override
        public void setAdultOrOlder(boolean adultOrOlder) {     
 }

 

}