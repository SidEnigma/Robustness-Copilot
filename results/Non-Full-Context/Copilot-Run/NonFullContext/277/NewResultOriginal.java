package net.glowstone.entity;
 
 import com.destroystokyo.paper.Title;
 import com.destroystokyo.paper.profile.PlayerProfile;
 import com.flowpowered.network.Message;
 import com.flowpowered.network.util.ByteBufUtils;
 import com.google.common.base.Preconditions;
 import com.google.common.collect.ImmutableList;
 import io.netty.buffer.ByteBuf;
 import io.netty.buffer.ByteBufAllocator;
 import io.netty.buffer.Unpooled;
 import lombok.Getter;
 import lombok.Setter;
 import net.glowstone.EventFactory;
 import net.glowstone.GlowOfflinePlayer;
 import net.glowstone.GlowWorld;
 import net.glowstone.GlowWorldBorder;
 import net.glowstone.block.GlowBlock;
 import net.glowstone.block.ItemTable;
 import net.glowstone.block.blocktype.BlockBed;
 import net.glowstone.block.entity.SignEntity;
 import net.glowstone.block.itemtype.ItemFood;
 import net.glowstone.block.itemtype.ItemType;
 import net.glowstone.chunk.ChunkManager.ChunkLock;
 import net.glowstone.chunk.GlowChunk;
 import net.glowstone.chunk.GlowChunk.Key;
 import net.glowstone.command.LocalizedEnumNames;
 import net.glowstone.constants.GameRules;
 import net.glowstone.constants.GlowAchievement;
 import net.glowstone.constants.GlowBlockEntity;
 import net.glowstone.constants.GlowEffect;
 import net.glowstone.constants.GlowParticle;
 import net.glowstone.constants.GlowSound;
 import net.glowstone.entity.meta.ClientSettings;
 import net.glowstone.entity.meta.MetadataIndex;
 import net.glowstone.entity.meta.MetadataIndex.StatusFlags;
 import net.glowstone.entity.meta.MetadataMap;
 import net.glowstone.entity.meta.profile.GlowPlayerProfile;
 import net.glowstone.entity.monster.GlowBoss;
 import net.glowstone.entity.objects.GlowItem;
 import net.glowstone.entity.passive.GlowFishingHook;
 import net.glowstone.i18n.GlowstoneMessages;
 import net.glowstone.inventory.GlowInventory;
 import net.glowstone.inventory.GlowInventoryView;
 import net.glowstone.inventory.InventoryMonitor;
 import net.glowstone.inventory.ToolType;
 import net.glowstone.inventory.crafting.PlayerRecipeMonitor;
 import net.glowstone.io.PlayerDataService.PlayerReader;
 import net.glowstone.map.GlowMapCanvas;
 import net.glowstone.net.GlowSession;
 import net.glowstone.net.message.play.entity.AnimateEntityMessage;
 import net.glowstone.net.message.play.entity.DestroyEntitiesMessage;
 import net.glowstone.net.message.play.entity.EntityMetadataMessage;
 import net.glowstone.net.message.play.entity.EntityVelocityMessage;
 import net.glowstone.net.message.play.entity.SetPassengerMessage;
 import net.glowstone.net.message.play.game.BlockBreakAnimationMessage;
 import net.glowstone.net.message.play.game.BlockChangeMessage;
 import net.glowstone.net.message.play.game.ChatMessage;
 import net.glowstone.net.message.play.game.ChunkDataMessage;
 import net.glowstone.net.message.play.game.ExperienceMessage;
 import net.glowstone.net.message.play.game.HealthMessage;
 import net.glowstone.net.message.play.game.JoinGameMessage;
 import net.glowstone.net.message.play.game.MapDataMessage;
 import net.glowstone.net.message.play.game.MultiBlockChangeMessage;
 import net.glowstone.net.message.play.game.NamedSoundEffectMessage;
 import net.glowstone.net.message.play.game.PlayEffectMessage;
 import net.glowstone.net.message.play.game.PlayParticleMessage;
 import net.glowstone.net.message.play.game.PluginMessage;
 import net.glowstone.net.message.play.game.PositionRotationMessage;
 import net.glowstone.net.message.play.game.RespawnMessage;
 import net.glowstone.net.message.play.game.SignEditorMessage;
 import net.glowstone.net.message.play.game.SpawnPositionMessage;
 import net.glowstone.net.message.play.game.StateChangeMessage;
 import net.glowstone.net.message.play.game.StateChangeMessage.Reason;
 import net.glowstone.net.message.play.game.StatisticMessage;
 import net.glowstone.net.message.play.game.TimeMessage;
 import net.glowstone.net.message.play.game.TitleMessage;
 import net.glowstone.net.message.play.game.TitleMessage.Action;
 import net.glowstone.net.message.play.game.UnloadChunkMessage;
 import net.glowstone.net.message.play.game.UpdateBlockEntityMessage;
 import net.glowstone.net.message.play.game.UpdateSignMessage;
 import net.glowstone.net.message.play.game.UserListHeaderFooterMessage;
 import net.glowstone.net.message.play.game.UserListItemMessage;
 import net.glowstone.net.message.play.game.UserListItemMessage.Entry;
 import net.glowstone.net.message.play.inv.CloseWindowMessage;
 import net.glowstone.net.message.play.inv.HeldItemMessage;
 import net.glowstone.net.message.play.inv.OpenWindowMessage;
 import net.glowstone.net.message.play.inv.SetWindowContentsMessage;
 import net.glowstone.net.message.play.inv.SetWindowSlotMessage;
 import net.glowstone.net.message.play.inv.WindowPropertyMessage;
 import net.glowstone.net.message.play.player.ResourcePackSendMessage;
 import net.glowstone.net.message.play.player.UseBedMessage;
 import net.glowstone.scoreboard.GlowScoreboard;
 import net.glowstone.scoreboard.GlowTeam;
 import net.glowstone.util.Convert;
 import net.glowstone.util.EntityUtils;
 import net.glowstone.util.InventoryUtil;
 import net.glowstone.util.Position;
 import net.glowstone.util.StatisticMap;
 import net.glowstone.util.TextMessage;
 import net.glowstone.util.TickUtil;
 import net.glowstone.util.nbt.CompoundTag;
 import net.md_5.bungee.api.ChatMessageType;
 import net.md_5.bungee.api.chat.BaseComponent;
 import net.md_5.bungee.chat.ComponentSerializer;
 import org.bukkit.Achievement;
 import org.bukkit.BanList;
 import org.bukkit.Bukkit;
 import org.bukkit.ChatColor;
 import org.bukkit.Difficulty;
 import org.bukkit.Effect;
 import org.bukkit.Effect.Type;
 import org.bukkit.EntityAnimation;
 import org.bukkit.GameMode;
 import org.bukkit.Instrument;
 import org.bukkit.Location;
 import org.bukkit.Material;
 import org.bukkit.Note;
 import org.bukkit.Particle;
 import org.bukkit.Sound;
 import org.bukkit.SoundCategory;
 import org.bukkit.Statistic;
 import org.bukkit.WeatherType;
 import org.bukkit.World;
 import org.bukkit.World.Environment;
 import org.bukkit.advancement.Advancement;
 import org.bukkit.advancement.AdvancementProgress;
 import org.bukkit.block.Block;
 import org.bukkit.block.BlockFace;
 import org.bukkit.boss.BossBar;
 import org.bukkit.configuration.serialization.DelegateDeserialization;
 import org.bukkit.conversations.Conversation;
 import org.bukkit.conversations.ConversationAbandonedEvent;
 import org.bukkit.enchantments.Enchantment;
 import org.bukkit.entity.Entity;
 import org.bukkit.entity.EntityType;
 import org.bukkit.entity.Player;
 import org.bukkit.entity.Projectile;
 import org.bukkit.entity.Villager;
 import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
 import org.bukkit.event.entity.EntityRegainHealthEvent;
 import org.bukkit.event.entity.FoodLevelChangeEvent;
 import org.bukkit.event.inventory.InventoryOpenEvent;
 import org.bukkit.event.player.AsyncPlayerChatEvent;
 import org.bukkit.event.player.PlayerAchievementAwardedEvent;
 import org.bukkit.event.player.PlayerBedEnterEvent;
 import org.bukkit.event.player.PlayerBedLeaveEvent;
 import org.bukkit.event.player.PlayerChangedMainHandEvent;
 import org.bukkit.event.player.PlayerChangedWorldEvent;
 import org.bukkit.event.player.PlayerCommandPreprocessEvent;
 import org.bukkit.event.player.PlayerDropItemEvent;
 import org.bukkit.event.player.PlayerExpChangeEvent;
 import org.bukkit.event.player.PlayerGameModeChangeEvent;
 import org.bukkit.event.player.PlayerLevelChangeEvent;
 import org.bukkit.event.player.PlayerLocaleChangeEvent;
 import org.bukkit.event.player.PlayerPortalEvent;
 import org.bukkit.event.player.PlayerRegisterChannelEvent;
 import org.bukkit.event.player.PlayerResourcePackStatusEvent;
 import org.bukkit.event.player.PlayerRespawnEvent;
 import org.bukkit.event.player.PlayerStatisticIncrementEvent;
 import org.bukkit.event.player.PlayerTeleportEvent;
 import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
 import org.bukkit.event.player.PlayerToggleSneakEvent;
 import org.bukkit.event.player.PlayerToggleSprintEvent;
 import org.bukkit.event.player.PlayerUnregisterChannelEvent;
 import org.bukkit.event.player.PlayerVelocityEvent;
 import org.bukkit.inventory.InventoryView;
 import org.bukkit.inventory.InventoryView.Property;
 import org.bukkit.inventory.ItemStack;
 import org.bukkit.inventory.MainHand;
 import org.bukkit.inventory.Merchant;
 import org.bukkit.inventory.PlayerInventory;
 import org.bukkit.inventory.Recipe;
 import org.bukkit.map.MapView;
 import org.bukkit.material.MaterialData;
 import org.bukkit.plugin.Plugin;
 import org.bukkit.plugin.messaging.StandardMessenger;
 import org.bukkit.scoreboard.Scoreboard;
 import org.bukkit.util.BlockVector;
 import org.bukkit.util.Vector;
 import org.json.simple.JSONObject;
 
 import javax.annotation.Nullable;
 import java.io.IOException;
 import java.net.InetSocketAddress;
 import java.nio.charset.StandardCharsets;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.Objects;
 import java.util.Queue;
 import java.util.Set;
 import java.util.UUID;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ConcurrentLinkedDeque;
 import java.util.concurrent.ThreadLocalRandom;
 import java.util.concurrent.atomic.AtomicReference;
 import java.util.function.Function;
 import java.util.logging.Level;
 import java.util.stream.Collectors;
 
 import static com.google.common.base.Preconditions.checkArgument;
 import static com.google.common.base.Preconditions.checkNotNull;
 import static net.glowstone.GlowServer.logger;
 
 
 /**
  * Represents an in-game player.
  *
  * @author Graham Edgecombe
  */
 @DelegateDeserialization(GlowOfflinePlayer.class)
 public class GlowPlayer extends GlowHumanEntity implements Player {
 
     /**
      * Max distance of a fishing hook.
      */
     public static final int HOOK_MAX_DISTANCE = 32;
 
     private static final Achievement[] ACHIEVEMENT_VALUES = Achievement.values();
     private static final LocalizedEnumNames<Achievement> ACHIEVEMENT_NAMES
             = new LocalizedEnumNames<Achievement>(
                     (Function<String, Achievement>) Achievement::valueOf,
             "glowstone.achievement.unknown",
             null, "maps/achievement", true);
 
     /**
      * The network session attached to this player.
      *
      * @return The GlowSession of the player.
      */
     @Getter
     private final GlowSession session;
 
     /**
      * The entities that the client knows about. Guarded by {@link #worldLock}.
      */
     private final Set<GlowEntity> knownEntities = new HashSet<>();
 
     /**
      * The entities that are hidden from the client.
      */
     private final Set<UUID> hiddenEntities = new HashSet<>();
 
     /**
      * The chunks that the client knows about.
      */
     private final Set<Key> knownChunks = new HashSet<>();
 
     /**
      * A queue of BlockChangeMessages to be sent.
      */
     private final Queue<BlockChangeMessage> blockChanges = new ConcurrentLinkedDeque<>();
 
     /**
      * A queue of messages that should be sent after block changes are processed.
      *
      * <p>Used for sign updates and other situations where the block must be sent first.
      */
     private final List<Message> afterBlockChanges = new LinkedList<>();
 
     /**
      * The set of plugin channels this player is listening on.
      */
     private final Set<String> listeningChannels = new HashSet<>();
 
     /**
      * The player's statistics, achievements, and related data.
      */
     private final StatisticMap stats = new StatisticMap();
 
     /**
      * Whether the player has played before (will be false on first join).
      */
     private final boolean hasPlayedBefore;
 
     /**
      * The time the player first played, or 0 if unknown.
      */
     @Getter
     private final long firstPlayed;
 
     /**
      * The time the player last played, or 0 if unknown.
      */
     @Getter
     private final long lastPlayed;
     @Getter
     private final PlayerRecipeMonitor recipeMonitor;
     public Location teleportedTo = null;
     @Setter
     public boolean affectsSpawning = true;
     /**
      * The time the player joined, in milliseconds, to be saved as last played time.
      *
      * @return The player's join time.
      */
     @Getter
     private long joinTime;
     /**
      * The settings sent by the client.
      */
     private ClientSettings settings = ClientSettings.DEFAULT;
     /**
      * The lock used to prevent chunks from unloading near the player.
      */
     private ChunkLock chunkLock;
     /**
      * The tracker for changes to the currently open inventory.
      */
     private InventoryMonitor invMonitor;
     /**
      * The display name of this player, for chat purposes.
      */
     private String displayName;
     /**
      * The name a player has in the player list.
      */
     private String playerListName;
     /**
      * Cumulative amount of experience points the player has collected.
      */
     @Getter
     private int totalExperience;
     /**
      * The current level (or skill point amount) of the player.
      */
     @Getter
     private int level;
     /**
      * The progress made to the next level, from 0 to 1.
      */
     @Getter
     private float exp;
     /**
      * The human entity's current food level.
      */
     @Getter
     private int foodLevel = 20;
     /**
      * The player's current exhaustion level.
      */
     @Getter
     @Setter
     private float exhaustion;
     /**
      * The player's current saturation level.
      */
     @Getter
     private float saturation;
     /**
      * Whether to perform special scaling of the player's health.
      */
     @Getter
     private boolean healthScaled;
     /**
      * The scale at which to display the player's health.
      */
     @Getter
     private double healthScale = 20;
     /**
      * If this player has seen the end credits.
      */
     @Getter
     @Setter
     private boolean seenCredits;
     /**
      * Recipes this player has unlocked.
      */
     private Collection<Recipe> recipes = new HashSet<>();
     /**
      * This player's current time offset.
      */
     private long timeOffset;
     /**
      * Whether the time offset is relative.
      */
     @Getter
     private boolean playerTimeRelative = true;
     /**
      * The player-specific weather, or null for normal weather.
      */
     private WeatherType playerWeather;
     /**
      * The player's compass target.
      */
     @Getter
     private Location compassTarget;
     /**
      * Whether this player's sleeping state is ignored when changing time.
      */
     private boolean sleepingIgnored;
     /**
      * The bed in which the player currently lies.
      */
     private GlowBlock bed;
     /**
      * The bed spawn location of a player.
      */
     private Location bedSpawn;
     /**
      * Whether to use the bed spawn even if there is no bed block.
      *
      * @return Whether the player is forced to spawn at their bed.
      */
     @Getter
     private boolean bedSpawnForced;
     private final Player.Spigot spigot = new Player.Spigot() {
         @Override
         public void playEffect(Location location, Effect effect, int id, int data, float offsetX,
                 float offsetY, float offsetZ, float speed, int particleCount, int radius) {
             if (effect.getType() == Type.PARTICLE) {
                 MaterialData material = new MaterialData(id, (byte) data);
                 showParticle(location, effect, material, offsetX, offsetY, offsetZ, speed,
                         particleCount);
             } else {
                 GlowPlayer.this.playEffect(location, effect, data);
             }
         }
 
         @Override
         public InetSocketAddress getRawAddress() {
             return session.getAddress();
         }
 
         @Override
         public void respawn() {
             GlowPlayer.this.respawn();
         }
 
         @Override
         public boolean getCollidesWithEntities() {
             return isCollidable();
         }
 
         @Override
         public void setCollidesWithEntities(boolean collides) {
             setCollidable(collides);
         }
 
         @Override
         public Set<Player> getHiddenPlayers() {
             return hiddenEntities.stream().map(Bukkit::getPlayer).filter(Objects::nonNull)
                     .collect(Collectors.toSet());
         }
 
         @Override
         public void sendMessage(ChatMessageType position, BaseComponent... components) {
             GlowPlayer.this.sendMessage(position, components);
         }
 
         @Override
         public void sendMessage(ChatMessageType position, BaseComponent component) {
             GlowPlayer.this.sendMessage(position, component);
         }
 
         @Override
         public void sendMessage(BaseComponent... components) {
             GlowPlayer.this.sendMessage(components);
         }
 
         @Override
         public void sendMessage(BaseComponent component) {
             GlowPlayer.this.sendMessage(component);
         }
 
         @Override
         public String getLocale() {
             return GlowPlayer.this.getLocale();
         }
     };
     /**
      * The location of the sign the player is currently editing, or null.
      */
     private Location signLocation;
     /**
      * Whether the player is permitted to fly.
      */
     private boolean canFly;
     /**
      * Whether the player is currently flying.
      */
     @Getter
     private boolean flying;
     /**
      * The player's base flight speed.
      */
     @Getter
     private float flySpeed = 0.1f;
     /**
      * The player's base walking speed.
      */
     @Getter
     private float walkSpeed = 0.2f;
     /**
      * The scoreboard the player is currently subscribed to.
      */
     private GlowScoreboard scoreboard;
     /**
      * The player's current title, if any.
      */
     private Title.Builder currentTitle = new Title.Builder();
     /**
      * The one block the player is currently digging.
      */
     @Getter
     private GlowBlock digging;
     /**
      * The number of ticks elapsed since the player started digging.
      */
     private long diggingTicks = 0;
     /**
      * The total number of ticks needed to dig the current block.
      */
     private long totalDiggingTicks = Long.MAX_VALUE;
     /**
      * The one itemstack the player is currently usage and associated time.
      */
     @Getter
     @Setter
     private ItemStack usageItem;
     @Getter
     private int usageTime;
     @Getter
     private int startingUsageTime;
     private Entity spectating;
     private HashMap<Advancement, AdvancementProgress> advancements;
     private String resourcePackHash;
     private PlayerResourcePackStatusEvent.Status resourcePackStatus;
     private List<Conversation> conversations = new ArrayList<>();
     private Set<BossBar> bossBars = ConcurrentHashMap.newKeySet();
     /**
      * The player's previous chunk x coordinate.
      */
     private int prevCentralX;
     /**
      * The player's previous chunk x coordinate.
      */
     private int prevCentralZ;
     /**
      * If this is the player's first time getting blocks streamed.
      */
     private boolean firstStream = true;
     /**
      * If we should force block streaming regardless of chunk difference.
      */
     private boolean forceStream = false;
     /**
      * Current casted fishing hook.
      */
     private final AtomicReference<GlowFishingHook> currentFishingHook = new AtomicReference<>(null);
     /**
      * The player's ender pearl cooldown game tick counter.
      * 1 second, or 20 game ticks by default.
      * The player can use ender pearl again if equals 0.
      */
     @Getter
     @Setter
     private int enderPearlCooldown = 0;
 
     /**
      * Returns the current fishing hook.
      *
      * @return the current fishing hook, or null if not fishing
      */
     public GlowFishingHook getCurrentFishingHook() {
         return currentFishingHook.get();
     }
 
     /**
      * Creates a new player and adds it to the world.
      *
      * @param session The player's session.
      * @param profile The player's profile with name and UUID information.
      * @param reader The PlayerReader to be used to initialize the player.
      */
     public GlowPlayer(GlowSession session, GlowPlayerProfile profile, PlayerReader reader) {
         super(initLocation(session, reader), profile);
         setBoundingBox(0.6, 1.8);
         this.session = session;
 
         chunkLock = world.newChunkLock(getName());
 
         // read data from player reader
         hasPlayedBefore = reader.hasPlayedBefore();
         if (hasPlayedBefore) {
             firstPlayed = reader.getFirstPlayed();
             lastPlayed = reader.getLastPlayed();
             bedSpawn = reader.getBedSpawnLocation();
         } else {
             firstPlayed = 0;
             lastPlayed = 0;
         }
 
         //creates InventoryMonitor to avoid NullPointerException
         invMonitor = new InventoryMonitor(getOpenInventory());
         server.getPlayerStatisticIoService().readStatistics(this);
         recipeMonitor = new PlayerRecipeMonitor(this);
 
         updateBossBars();
     }
 
     ////////////////////////////////////////////////////////////////////////////
     // Internals
 
     /**
      * Read the location from a PlayerReader for entity initialization.
      *
      * <p>Will fall back to a reasonable default rather than returning null.
      *
      * @param session The player's session.
      * @param reader The PlayerReader to get the location from.
      * @return The location to spawn the player.
      */
     private static Location initLocation(GlowSession session, PlayerReader reader) {
         if (reader.hasPlayedBefore()) {
             Location loc = reader.getLocation();
             if (loc != null) {
                 return loc;
             }
         }
 
         return findSafeSpawnLocation(session.getServer().getWorlds().get(0).getSpawnLocation());
     }
 
 
/** Find a a Location obove or below the specified Location, which is on ground. */

private static Location findSafeSpawnLocation(Location spawn) {
    World world = spawn.getWorld();
    int x = spawn.getBlockX();
    int y = spawn.getBlockY();
    int z = spawn.getBlockZ();

    // Check if the spawn location is already safe
    if (world.getBlockAt(x, y, z).getType().isSolid() && world.getBlockAt(x, y + 1, z).getType().isSolid()) {
        return spawn;
    }

    // Find a safe location above the spawn location
    for (int i = y + 1; i < world.getMaxHeight(); i++) {
        if (world.getBlockAt(x, i, z).getType().isSolid() && world.getBlockAt(x, i + 1, z).getType().isSolid()) {
            return new Location(world, x, i + 1, z);
        }
    }

    // Find a safe location below the spawn location
    for (int i = y - 1; i > 0; i--) {
        if (world.getBlockAt(x, i, z).getType().isSolid() && world.getBlockAt(x, i + 1, z).getType().isSolid()) {
            return new Location(world, x, i + 1, z);
        }
    }

    // If no safe location is found, return the original spawn location
    return spawn;
}
 

}