package net.glowstone.util.nbt;
 
 import lombok.Getter;
 import net.glowstone.constants.ItemIds;
 import net.glowstone.io.nbt.NbtSerialization;
 import net.glowstone.util.DynamicallyTypedMapWithDoubles;
 import net.glowstone.util.FloatConsumer;
 import net.glowstone.util.ShortConsumer;
 import org.bukkit.Material;
 import org.bukkit.inventory.ItemStack;
 import org.jetbrains.annotations.NonNls;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Optional;
 import java.util.UUID;
 import java.util.function.Consumer;
 import java.util.function.DoubleConsumer;
 import java.util.function.Function;
 import java.util.function.IntConsumer;
 import java.util.function.LongConsumer;
 import java.util.stream.Collectors;
 
 import static com.google.common.base.Preconditions.checkNotNull;
 
 /**
  * The {@code TAG_Compound} tag.
  */
 public class CompoundTag extends Tag<Map<String, Tag>>
         implements DynamicallyTypedMapWithDoubles<String> {
 
     /**
      * The value.
      */
     @Getter
     private final Map<String, Tag> value = new LinkedHashMap<>();
 
     /**
      * Creates a new, empty CompoundTag.
      */
     public CompoundTag() {
         super(TagType.COMPOUND);
     }
 
     public CompoundTag(Map<String, Tag> value) {
         this();
         this.value.putAll(value);
     }
 
     @Override
     protected void valueToString(StringBuilder builder) {
         builder.append(value.size()).append(" entries\n{\n"); // NON-NLS
         for (Entry<String, Tag> entry : value.entrySet()) {
             builder.append("    ").append(entry.getKey()).append(": ")
                     .append(entry.getValue().toString().replaceAll("\n", "\n    ")).append("\n");
         }
         builder.append("}");
     }
 
     ////////////////////////////////////////////////////////////////////////////
     // Helper stuff
 
     public boolean isEmpty() {
         return value.isEmpty();
     }
 
     /**
      * Check if the compound contains the given key.
      *
      * @param key The key.
      * @return True if the key is in the map.
      */
     public boolean containsKey(@NonNls String key) {
         return value.containsKey(key);
     }
 
     public void remove(@NonNls String key) {
         value.remove(key);
     }
 
 
/** Checks if this tag is a strict and deep submap of the given CompoundTag, which contains our values. */
 public boolean matches(CompoundTag other){
        for (Entry<String, Tag> entry : value.entrySet()) {
            Tag otherTag = other.value.get(entry.getKey());
            if (otherTag == null || !otherTag.equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }
    
        ////////////////////////////////////////////////////////////////////////////
        // NBT serialization
    
        @Override
        public void write(NbtSerialization stream) {
            stream.writeTag(this);
        }
    
        @Override
        public void read(NbtSerialization stream) {
            value.clear();
            int size = stream.readInt();
            for (int i = 0; i < size; i++) {
                String key = stream.readString();
                Tag tag = stream.readTag();
                value.put(key, tag);
            }
        }
    
        ////////////////////////////////////////////////////////////////////////////
        // ItemStack serialization
    
        public static CompoundTag fromItemStack(ItemStack itemStack) {
            CompoundTag tag = new CompoundTag();
            tag.put("id", new ShortTag(itemStack.getType().getId()));
            tag.put("Damage", new ShortTag(itemStack.getDurability()));
            tag.put("Count", new ByteTag((byte) itemStack.getAmount()));
            tag.put("tag", itemStack.getItemMeta().toTag());
            return tag;
        }
    
        public static ItemStack toItemStack(CompoundTag tag) {
            short id = tag.getShort("id");
            short damage = tag.getShort("Damage");
            byte count = tag.getByte("Count");
            ItemStack itemStack = new ItemStack(Material.getMaterial(id), count, damage);
            itemStack.setItemMeta(ItemMetaTag.toItemMeta(tag.getCompound("tag")));
            return itemStack;
        }
    
        ////////////////////////////////////////////////////////////////////////////
        // NBT serialization
    
        @Override
        public void write(NbtSerialization stream) {
            stream.writeTag(this        
 }

 

}