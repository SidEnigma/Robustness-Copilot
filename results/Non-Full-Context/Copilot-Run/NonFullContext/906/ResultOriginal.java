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
 
 
/** Checks to see if this tag is a strict, deep submap of the given CompoundTag. */
 public boolean matches(CompoundTag other){
        if(other.value.size() != value.size()) return false;
        for(Entry<String, Tag> entry : value.entrySet()){
            if(!other.value.containsKey(entry.getKey())) return false;
            if(!entry.getValue().matches(other.value.get(entry.getKey()))) return false;
        }
        return true;
    }
    
        ////////////////////////////////////////////////////////////////////////////
        // NBT serialization
    
        @Override
        public void write(NbtSerialization.NbtOutputStream output) {
            output.writeTag(this);
        }
    
        @Override
        public void read(NbtSerialization.NbtInputStream input) {
            input.readTag(this);
        }
    
        ////////////////////////////////////////////////////////////////////////////
        // ItemStack serialization
    
        @Override
        public ItemStack toItemStack() {
            ItemStack item = new ItemStack(ItemIds.AIR);
            for (Entry<String, Tag> entry : value.entrySet()) {
                if (entry.getKey().equals("id")) {
                    item.setType(Material.getMaterial(entry.getValue().asString()));
                } else if (entry.getKey().equals("Damage")) {
                    item.setDurability(entry.getValue().asShort());
                } else if (entry.getKey().equals("Count")) {
                    item.setAmount(entry.getValue().asShort());
                } else if (entry.getKey().equals("tag")) {
                    item.setItemMeta(entry.getValue().toItemMeta());
                }
            }
            return item;
        }
    
        @Override
        public void fromItemStack(ItemStack item) {
            value.clear();
            value.put("id", new StringTag(item.getType().name()));
            value.put("Damage", new ShortTag(item.getDurability()));
            value.put("Count", new ShortTag(item.getAmount()));
            value.put("tag", new CompoundTag(item.getItemMeta().        
 }

 

}