package net.glowstone.util;
 
 import com.google.common.collect.Sets;
 import net.glowstone.GlowWorld;
 import net.glowstone.entity.GlowPlayer;
 import org.bukkit.Location;
 import org.bukkit.Sound;
 import org.bukkit.SoundCategory;
 
 import java.util.Set;
 import java.util.concurrent.ThreadLocalRandom;
 
 import static com.google.common.base.Preconditions.checkNotNull;
 
 /**
  * A collection of utility methods to play sounds.
  */
 public class SoundUtil {
 
     /**
      * Plays a sound with a random pitch, but excludes specified players from hearing it.
      *
      * @param location the sound location
      * @param sound    the sound to play
      * @param volume   the volume multiplier
      * @param pitch    the pitch modifier
      * @param exclude  the players not to play the sound for
      * @throws NullPointerException if any of the location, sound, or exclude parameters is null.
      */
     public static void playSoundAtLocationExcept(Location location, Sound sound, float volume,
             float pitch, GlowPlayer... exclude) {
         checkNotNull(location);
         checkNotNull(sound);
         checkNotNull(exclude);
 
         Set<GlowPlayer> excludeSet = Sets.newHashSet(exclude);
         GlowWorld world = (GlowWorld) location.getWorld();
         double radiusSquared = volume * volume * 256;
         world.getRawPlayers()
                 .stream()
                 .filter(player -> player.getLocation().distanceSquared(location) <= radiusSquared
                         && !excludeSet.contains(player))
                 .forEach(player -> player.playSound(location, sound, volume, pitch));
     }
 
     /**
      * Plays a sound with a random pitch, but excludes specified players from hearing it.
      *
      * @param location      the sound location
      * @param sound         the sound to play
      * @param volume        the volume multiplier
      * @param pitchBase     if {@code allowNegative}, the average pitch modifier; otherwise, the
      *                      minimum
      * @param pitchRange    the maximum deviation of the pitch modifier compared to {@code
      * pitchBase}
      * @param allowNegative if true, distribution is triangular rather than uniform
      * @param exclude       the players not to play the sound for
      */
     public static void playSoundPitchRange(Location location, Sound sound, float volume,
             float pitchBase, float pitchRange, boolean allowNegative, GlowPlayer... exclude) {
         ThreadLocalRandom rand = ThreadLocalRandom.current();
         float pitch = pitchBase;
         if (allowNegative) {
             pitch += randomReal(pitchRange);
         } else {
             pitch += rand.nextFloat() * pitchRange;
         }
         playSoundAtLocationExcept(location, sound, volume, pitch, exclude);
     }
 
     /**
      * Plays a sound with a random pitch, but excludes specified players from hearing it.
      *
      * @param location   the sound location
      * @param sound      the sound to play
      * @param volume     the volume multiplier
      * @param pitchBase  the average pitch modifier.
      * @param pitchRange the maximum deviation of the pitch modifier compared to {@code pitchBase}
      * @param exclude    the players not to play the sound for
      */
     public static void playSoundPitchRange(Location location, Sound sound, float volume,
             float pitchBase, float pitchRange, GlowPlayer... exclude) {
         playSoundPitchRange(location, sound, volume, pitchBase, pitchRange, true, exclude);
     }
 
 
/** Generates a random float between {@code -range} and {@code range}. */
 public static float randomReal(float range){
        return ThreadLocalRandom.current().nextFloat() * range * 2 - range;
    }
    
        /**
        * Plays a sound with a random pitch, but excludes specified players from hearing it.
        *
        * @param location   the sound location
        * @param sound      the sound to play
        * @param volume     the volume multiplier
        * @param pitchBase  the average pitch modifier.
        * @param pitchRange the maximum deviation of the pitch modifier compared to {@code pitchBase}
        * @param exclude    the players not to play the sound for
        */
        public static void playSoundPitchRange(Location location, Sound sound, float volume,
                float pitchBase, float pitchRange, GlowPlayer... exclude) {
            playSoundPitchRange(location, sound, volume, pitchBase, pitchRange, exclude);
        }
    
        /**
        * Plays a sound with a random pitch, but excludes specified players from hearing it.
        *
        * @param location   the sound location
        * @param sound      the sound to play
        * @param volume     the volume multiplier
        * @param pitchBase  the average pitch modifier.
        * @param pitchRange the maximum deviation of the pitch modifier compared to {@code pitchBase}
        * @param exclude    the players not to play the sound for
        */
        public static void playSoundPitchRange(Location location, Sound sound, float volume,
                float pitchBase, float pitchRange, boolean allowNegative, GlowPlayer... exclude) {
            playSoundPitchRange(location, sound, volume, pitchBase, pitchRange, allowNegative, exclude);
        }
    
        /**
        * Plays a sound with a random pitch, but excludes specified players from hearing it.
        *
        * @param location   the sound location
        * @param sound      the sound to play
        * @param volume     the volume multiplier
        * @param pitchBase  the average pitch modifier.
        * @param pitchRange the maximum deviation of the pitch modifier compared to {@code pitchBase}
        * @param exclude    the players not to play the sound for
        */
        public static void playSoundPitchRange(Location location, Sound     
 }

 

}