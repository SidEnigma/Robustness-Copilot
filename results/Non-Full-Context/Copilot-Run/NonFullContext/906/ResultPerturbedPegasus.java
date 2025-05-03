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
 
 
/** If this tag is a deep submap of the CompoundTag, we need to check it. */
 public boolean matches(CompoundTag other){
        if(other.value.size() != value.size()) return false;
        for(Entry<String, Tag> entry : value.entrySet()){
            if(!other.value.containsKey(entry.getKey())) return false;
            if(!entry.getValue().matches(other.value.get(entry.getKey()))) return false;
        }
        return true;
    }
    
        ////////////////////////////////////////////////////////////////////////////
        // Tag stuff
    
        @Override
        public CompoundTag copy() {
            return new CompoundTag(value);
        }
    
        @Override
        public CompoundTag clone() {
            return new CompoundTag(value);
        }
    
        @Override
        public CompoundTag deepCopy() {
            return new CompoundTag(value.entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> e.getValue().deepCopy())));
        }
    
        @Override
        public CompoundTag deepClone() {
            return new CompoundTag(value.entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> e.getValue().deepClone())));
        }
    
        @Override
        public CompoundTag deepClone(Function<Tag, Tag> replacer) {
            return new CompoundTag(value.entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> e.getValue().deepClone(replacer))));
        }
    
        @Override
        public CompoundTag deepClone(Function<Tag, Tag> replacer, Consumer<Tag> remover) {
            return new CompoundTag(value.entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> e.getValue().deepClone(replacer, remover))));
        }
    
        @Override
        public CompoundTag deepClone(Function<Tag, Tag> replacer, Consumer<Tag> remover, Consumer<Tag> adder) {
            return new CompoundTag(value.entrySet().stream().collect(Collectors.        
 }

 

}