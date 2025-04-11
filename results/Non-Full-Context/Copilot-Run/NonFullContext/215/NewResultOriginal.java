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
 
     /**
      * Checks to see if this tag is a strict, deep submap of the given CompoundTag.
      *
      * @param other The CompoundTag that should contain our values.
      */
     public boolean matches(CompoundTag other) {
         for (Entry<String, Tag> entry : value.entrySet()) {
             if (!other.value.containsKey(entry.getKey())) {
                 return false;
             }
             Tag value = entry.getValue();
             Tag otherValue = other.value.get(entry.getKey());
             if ((value == null && otherValue != null) || (value != null && otherValue == null)) {
                 return false;
             }
             if (value != null) {
                 if (value.getClass() != otherValue.getClass()) {
                     return false;
                 }
                 if (value instanceof CompoundTag) {
                     if (!((CompoundTag) value).matches((CompoundTag) otherValue)) {
                         return false;
                     }
                 } else if (value instanceof IntArrayTag) {
                     if (!Arrays.equals(((IntArrayTag) value).getValue(),
                             ((IntArrayTag) otherValue).getValue())) {
                         return false;
                     }
                 } else if (value instanceof ByteArrayTag) {
                     if (!Arrays.equals(((ByteArrayTag) value).getValue(),
                             ((ByteArrayTag) otherValue).getValue())) {
                         return false;
                     }
                 } else if (!value.equals(otherValue)) {
                     // Note: When Mojang actually starts using lists, revisit this.
                     return false;
                 }
             }
         }
         return true;
     }
 
     /**
      * Merges the contents of this compound into the supplied compound.
      *
      * @param other the other compound to merge into.
      * @param overwrite whether keys already set in the other compound should be
      *         overwritten.
      */
     public void mergeInto(CompoundTag other, boolean overwrite) {
         for (String key : value.keySet()) {
             if (!overwrite && other.containsKey(key)) {
                 continue;
             }
             other.put(key, value.get(key));
         }
     }
 
     ////////////////////////////////////////////////////////////////////////////
     // Simple gets
 
 
     /**
      * Returns the value of a numeric subtag.
      *
      * @param key the key to look up
      * @return the numeric tag value
      */
     public Number getNumber(String key) {
         return (Number) get(key, NumericTag.class);
     }
 
     /**
      * Returns the value of a {@code byte} subtag.
      *
      * @param key the key to look up
      * @return the tag value
      */
     public byte getByte(@NonNls String key) {
         if (isInt(key)) {
             return (byte) getInt(key);
         }
         return get(key, ByteTag.class);
     }
 
     /**
      * Returns the value of a {@code short} subtag.
      *
      * @param key the key to look up
      * @return the tag value
      */
     public short getShort(@NonNls String key) {
         if (isInt(key)) {
             return (short) getInt(key);
         }
         return get(key, ShortTag.class);
     }
 
     /**
      * Returns the value of an {@code int} subtag.
      *
      * @param key the key to look up
      * @return the tag value
      */
     public int getInt(@NonNls String key) {
         if (isByte(key)) {
             return (int) getByte(key);
         } else if (isShort(key)) {
             return (int) getShort(key);
         } else if (isLong(key)) {
             return (int) getLong(key);
         }
         return get(key, IntTag.class);
     }
 
     @Override
     public boolean getBoolean(@NonNls String key) {
         return getNumber(key).byteValue() != 0;
     }
 
 
     /**
      * Returns the boolean value of a {@code byte} subtag if present, or a default otherwise.
      *
      * @param key the key to look up
      * @param defaultValue the value to return if the subtag is missing
      * @return the tag value as a boolean, or defaultValue if it's not a byte
      */
     public boolean getBoolean(@NonNls String key, boolean defaultValue) {
         return isNumeric(key) ? getBoolean(key) : defaultValue;
     }
 
     /**
      * Returns the value of a {@code long} subtag.
      *
      * @param key the key to look up
      * @return the tag value
      */
     public long getLong(@NonNls String key) {
         if (isInt(key)) {
             return (long) getInt(key);
         }
         return get(key, LongTag.class);
     }
 
     /**
      * Returns the value of a {@code float} subtag.
      *
      * @param key the key to look up
      * @return the tag value
      */
     public float getFloat(@NonNls String key) {
         if (isDouble(key)) {
             return (float) getDouble(key);
         } else if (isInt(key)) {
             return (float) getInt(key);
         }
         return get(key, FloatTag.class);
     }
 
     /**
      * Returns the value of a {@code double} subtag.
      *
      * @param key the key to look up
      * @return the tag value
      */
     public double getDouble(@NonNls String key) {
         if (isFloat(key)) {
             return (double) getFloat(key);
         } else if (isInt(key)) {
             return (double) getInt(key);
         }
         return get(key, DoubleTag.class);
     }
 
     /**
      * Returns the value of a {@code byte[]} subtag.
      *
      * @param key the key to look up
      * @return the tag value
      */
     public byte[] getByteArray(@NonNls String key) {
         return get(key, ByteArrayTag.class);
     }
 
     /**
      * Returns the value of a {@link String} subtag.
      *
      * @param key the key to look up
      * @return the tag value
      */
     public String getString(@NonNls String key) {
         return get(key, StringTag.class);
     }
 
     /**
      * Returns the value of an {@code int[]} subtag.
      *
      * @param key the key to look up
      * @return the tag value
      */
     public int[] getIntArray(@NonNls String key) {
         return get(key, IntArrayTag.class);
     }
 
     ////////////////////////////////////////////////////////////////////////////
     // Fancy gets
 
     /**
      * Returns the value of a {@link List} subtag.
      *
      * @param key the key to look up
      * @param type the list element tag type
      * @param <V> the list element type
      * @return the tag value
      */
     @SuppressWarnings("unchecked")
     public <V> List<V> getList(@NonNls String key, TagType type) {
         List<? extends Tag> original = getTagList(key, type);
         List<V> result = new ArrayList<>(original.size());
         if (type == TagType.COMPOUND) {
             result.addAll(
                     original.stream().map(
                         item -> (V) new CompoundTag((Map<String, Tag>) item.getValue()))
                             .collect(Collectors.toList()));
         } else {
             result.addAll(
                     original.stream().map(
                         item -> (V) item.getValue()).collect(Collectors.toList()));
         }
 
         return result;
     }
 
     /**
      * Returns the value of a compound subtag.
      *
      * @param key the key to look up
      * @return the tag value
      */
     public CompoundTag getCompound(@NonNls String key) {
         return getTag(key, CompoundTag.class);
     }
 
     /**
      * Returns the value of a compound subtag, if it exists. Multiple strings can be passed in to
      * retrieve a sub-subtag (e.g. {@code tryGetCompound("foo", "bar")} returns a compound subtag
      * called "bar" of a compound subtag called "foo", or null if either of those tags doesn't exist
      * or isn't compound.
      *
      * @param key the key to look up
      * @return the tag value, or an empty optional if the tag doesn't exist or isn't compound
      */
     public Optional<CompoundTag> tryGetCompound(@NonNls String key) {
         if (isCompound(key)) {
             return Optional.of(getCompound(key));
         }
         return Optional.empty();
     }
 
     /**
      * Applies the given function to a compound subtag if it is present. Multiple strings can be
      * passed in to operate on a sub-subtag, as with {@link #tryGetCompound(String)}.
      *
      * @param key the key to look up
      * @param consumer the function to apply
      * @return true if the tag exists and was passed to the consumer; false otherwise
      */
     public boolean readCompound(@NonNls String key, Consumer<? super CompoundTag> consumer) {
         Optional<CompoundTag> tag = tryGetCompound(key);
         tag.ifPresent(consumer);
         return tag.isPresent();
     }
 
     private <V, T extends Tag<V>> boolean readTag(@NonNls String key, Class<T> clazz,
             Consumer<? super V> consumer) {
         if (is(key, clazz)) {
             consumer.accept(get(key, clazz));
             return true;
         }
         return false;
     }
 
     private <T> Optional<T> tryGetTag(@NonNls String key, Class<? extends Tag<T>> clazz) {
         return is(key, clazz) ? Optional.of(get(key, clazz)) : Optional.empty();
     }
 
     /**
      * Applies the given function to a float subtag if it is present.
      *
      * @param key the key to look up
      * @param consumer the function to apply
      * @return true if the tag exists and was passed to the consumer; false otherwise
      */
     public boolean readFloat(@NonNls String key, FloatConsumer consumer) {
         // Avoid boxing by not delegating to readTag
         if (isFloat(key)) {
             consumer.accept(getFloat(key));
             return true;
         }
         return false;
     }
 
     /**
      * Applies the given function to a double subtag if it is present.
      *
      * @param key the key to look up
      * @param consumer the function to apply
      * @return true if the tag exists and was passed to the consumer; false otherwise
      */
     public boolean readDouble(@NonNls String key, DoubleConsumer consumer) {
         // Avoid boxing by not delegating to readTag
         if (isDouble(key)) {
             consumer.accept(getDouble(key));
             return true;
         }
         return false;
     }
 
     /**
      * Applies the given function to an integer subtag if it is present.
      *
      * @param key the key to look up
      * @param consumer the function to apply
      * @return true if the tag exists and was passed to the consumer; false otherwise
      */
     public boolean readInt(@NonNls String key, IntConsumer consumer) {
         // Avoid boxing by not delegating to readTag
         if (isInt(key)) {
             consumer.accept(getInt(key));
             return true;
         }
         return false;
     }
 
     /**
      * Applies the given function to a byte array subtag if it is present.
      *
      * @param key the key to look up
      * @param consumer the function to apply
      * @return true if the tag exists and was passed to the consumer; false otherwise
      */
     public boolean readByteArray(@NonNls String key, Consumer<? super byte[]> consumer) {
         return readTag(key, ByteArrayTag.class, consumer);
     }
 
     /**
      * Applies the given function to an integer array subtag if it is present.
      *
      * @param key the key to look up
      * @param consumer the function to apply
      * @return true if the tag exists and was passed to the consumer; false otherwise
      */
     public boolean readIntArray(@NonNls String key, Consumer<? super int[]> consumer) {
         return readTag(key, IntArrayTag.class, consumer);
     }
 
     /**
      * Applies the given function to a long subtag if it is present.
      *
      * @param key the key to look up
      * @param consumer the function to apply
      * @return true if the tag exists and was passed to the consumer; false otherwise
      */
     public boolean readLong(@NonNls String key, LongConsumer consumer) {
         // Avoid boxing by not delegating to readTag
         if (isLong(key)) {
             consumer.accept(getLong(key));
             return true;
         }
         return false;
     }
 
     /**
      * Returns the value of a long subtag if it is present.
      *
      * @param key the key to look up
      * @return an Optional with the value of that tag if it's present and is a long; an empty
      *         optional otherwise
      */
     public Optional<Long> tryGetLong(@NonNls String key) {
         return tryGetTag(key, LongTag.class);
     }
 
     /**
      * Applies the given function to an integer subtag if it is present.
      *
      * @param key the key to look up
      * @param consumer the function to apply
      * @return true if the tag exists and was passed to the consumer; false otherwise
      */
     public boolean readShort(@NonNls String key, ShortConsumer consumer) {
         // Avoid boxing by not delegating to readTag
         if (isShort(key)) {
             consumer.accept(getShort(key));
             return true;
         }
         return false;
     }
 
 
     /**
      * Applies the given function to a compound subtag if it is present, first converting it to an
      * item using {@link NbtSerialization#readItem(CompoundTag)}.
      *
      * @param key the key to look up
      * @param consumer the function to apply
      * @return true if the tag exists and was passed to the consumer; false otherwise
      */
     public boolean readItem(@NonNls String key, Consumer<? super ItemStack> consumer) {
         return readCompound(key, tag -> consumer.accept(NbtSerialization.readItem(tag)));
     }
 
 
/** Applies the given function to a byte subtag if it is present, converting it to boolean first. */

public boolean readBoolean(@NonNls String key, Consumer<? super Boolean> consumer) {
    if (isByte(key)) {
        consumer.accept(getByte(key) != 0);
        return true;
    }
    return false;
}
 

}