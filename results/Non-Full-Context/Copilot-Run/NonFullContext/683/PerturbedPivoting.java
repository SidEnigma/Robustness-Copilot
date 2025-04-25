package net.glowstone.entity;
 
 import com.flowpowered.network.Message;
 import lombok.Getter;
 import lombok.Setter;
 import net.glowstone.EventFactory;
 import net.glowstone.entity.meta.MetadataIndex;
 import net.glowstone.entity.meta.profile.GlowPlayerProfile;
 import net.glowstone.entity.objects.GlowItem;
 import net.glowstone.inventory.ArmorConstants;
 import net.glowstone.inventory.EquipmentMonitor;
 import net.glowstone.inventory.GlowCraftingInventory;
 import net.glowstone.inventory.GlowEnchantingInventory;
 import net.glowstone.inventory.GlowInventory;
 import net.glowstone.inventory.GlowInventoryView;
 import net.glowstone.inventory.GlowPlayerInventory;
 import net.glowstone.io.entity.EntityStorage;
 import net.glowstone.net.message.play.entity.EntityEquipmentMessage;
 import net.glowstone.net.message.play.entity.EntityHeadRotationMessage;
 import net.glowstone.net.message.play.entity.SpawnPlayerMessage;
 import net.glowstone.util.InventoryUtil;
 import net.glowstone.util.Position;
 import net.glowstone.util.UuidUtils;
 import net.glowstone.util.nbt.CompoundTag;
 import org.bukkit.GameMode;
 import org.bukkit.Location;
 import org.bukkit.Material;
 import org.bukkit.block.Sign;
 import org.bukkit.entity.Entity;
 import org.bukkit.entity.HumanEntity;
 import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
 import org.bukkit.event.inventory.InventoryCloseEvent;
 import org.bukkit.event.inventory.InventoryType;
 import org.bukkit.event.inventory.InventoryType.SlotType;
 import org.bukkit.inventory.EntityEquipment;
 import org.bukkit.inventory.Inventory;
 import org.bukkit.inventory.InventoryView;
 import org.bukkit.inventory.InventoryView.Property;
 import org.bukkit.inventory.ItemStack;
 import org.bukkit.permissions.PermissibleBase;
 import org.bukkit.permissions.Permission;
 import org.bukkit.permissions.PermissionAttachment;
 import org.bukkit.permissions.PermissionAttachmentInfo;
 import org.bukkit.plugin.Plugin;
 import org.bukkit.util.Vector;
 
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Random;
 import java.util.Set;
 import java.util.UUID;
 import java.util.concurrent.ThreadLocalRandom;
 
 import static com.google.common.base.Preconditions.checkArgument;
 import static com.google.common.base.Preconditions.checkNotNull;
 
 /**
  * Represents a human entity, such as an NPC or a player.
  */
 public abstract class GlowHumanEntity extends GlowLivingEntity implements HumanEntity {
 
     /**
      * The player profile with name and UUID information.
      */
     @Getter
     private final GlowPlayerProfile profile;
 
     /**
      * The inventory of this human.
      */
     @Getter
     private final GlowPlayerInventory inventory = new GlowPlayerInventory(this);
 
     /**
      * The ender chest inventory of this human.
      */
     @Getter
     private final GlowInventory enderChest = new GlowInventory(this, InventoryType.ENDER_CHEST);
     /**
      * Whether this human is sleeping or not.
      */
     @Getter
     protected boolean sleeping;
     /**
      * This human's PermissibleBase for permissions.
      */
     protected PermissibleBase permissions;
     /**
      * The item the player has on their cursor.
      */
     @Getter
     @Setter
     private ItemStack itemOnCursor;
     /**
      * How long this human has been sleeping.
      */
     @Getter
     private int sleepTicks;
     /**
      * Whether this human is considered an op.
      */
     @Getter
     private boolean op;
 
     /**
      * The player's active game mode.
      */
     @Getter
     @Setter
     private GameMode gameMode;
 
     /**
      * The player's currently open inventory.
      */
     @Getter
     private InventoryView openInventory;
 
     /**
      * The player's xpSeed. Used for calculation of enchantments.
      */
     @Getter
     @Setter
     private int xpSeed;
 
     /**
      * Whether the client needs to be notified of armor changes (set to true after joining).
      */
     private boolean needsArmorUpdate = false;
 
     /**
      * Creates a human within the specified world and with the specified name.
      *
      * @param location The location.
      * @param profile The human's profile with name and UUID information.
      */
     public GlowHumanEntity(Location location, GlowPlayerProfile profile) {
         super(location);
         this.profile = profile;
         xpSeed = new Random().nextInt(); //TODO: use entity's random instance
         permissions = new PermissibleBase(this);
         gameMode = server.getDefaultGameMode();
 
         openInventory = new GlowInventoryView(this);
         addViewer(openInventory.getTopInventory());
         addViewer(openInventory.getBottomInventory());
     }
 
     ////////////////////////////////////////////////////////////////////////////
     // Internals
 
     @Override
     public List<Message> createSpawnMessage() {
         List<Message> result = new LinkedList<>();
 
         // spawn player
         double x = location.getX();
         double y = location.getY();
         double z = location.getZ();
         int yaw = Position.getIntYaw(location);
         int pitch = Position.getIntPitch(location);
         result.add(new SpawnPlayerMessage(entityId, profile.getId(), x, y, z, yaw, pitch,
                 metadata.getEntryList()));
 
         // head facing
         result.add(new EntityHeadRotationMessage(entityId, yaw));
 
         // equipment
         EntityEquipment equipment = getEquipment();
         result.add(new EntityEquipmentMessage(entityId, EntityEquipmentMessage.HELD_ITEM, equipment
                 .getItemInMainHand()));
         result.add(new EntityEquipmentMessage(entityId, EntityEquipmentMessage.OFF_HAND, equipment
                 .getItemInOffHand()));
         for (int i = 0; i < 4; i++) {
             result.add(new EntityEquipmentMessage(entityId,
                     EntityEquipmentMessage.BOOTS_SLOT + i, equipment.getArmorContents()[i]));
         }
         return result;
     }
 
     @Override
     public void pulse() {
         super.pulse();
         if (sleeping) {
             ++sleepTicks;
         } else {
             sleepTicks = 0;
         }
         processArmorChanges();
     }
 
 
/** Process changes to the human entity's armor and update the entity's armor attributes accordingly. */
 private void processArmorChanges(){}

 

}