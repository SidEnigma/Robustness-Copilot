package com.fasterxml.jackson.databind.deser.impl;
 
 import java.io.IOException;
 import java.lang.reflect.InvocationTargetException;
 import java.util.*;
 
 import com.fasterxml.jackson.core.JacksonException;
 import com.fasterxml.jackson.core.JsonParser;
 
 import com.fasterxml.jackson.databind.DeserializationContext;
 import com.fasterxml.jackson.databind.DeserializationFeature;
 import com.fasterxml.jackson.databind.JsonDeserializer;
 import com.fasterxml.jackson.databind.JsonMappingException;
 import com.fasterxml.jackson.databind.MapperFeature;
 import com.fasterxml.jackson.databind.PropertyName;
 import com.fasterxml.jackson.databind.cfg.MapperConfig;
 import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
 import com.fasterxml.jackson.databind.util.ClassUtil;
 import com.fasterxml.jackson.databind.util.IgnorePropertiesUtil;
 import com.fasterxml.jackson.databind.util.NameTransformer;
 
 /**
  * Helper class used for storing mapping from property name to
  * {@link SettableBeanProperty} instances.
  *<p>
  * Note that this class is used instead of generic {@link java.util.HashMap}
  * for bit of performance gain (and some memory savings): although default
  * implementation is very good for generic use cases, it can be streamlined
  * a bit for specific use case we have. Even relatively small improvements
  * matter since this is directly on the critical path during deserialization,
  * as it is done for each and every POJO property deserialized.
  */
 public class BeanPropertyMap
     implements Iterable<SettableBeanProperty>,
         java.io.Serializable
 {
     private static final long serialVersionUID = 2L;
 
     /**
      * @since 2.5
      */
     protected final boolean _caseInsensitive;
 
     private int _hashMask;
 
     /**
      * Number of entries stored in the hash area.
      */
     private int _size;
 
     private int _spillCount;
 
     /**
      * Hash area that contains key/property pairs in adjacent elements.
      */
     private Object[] _hashArea;
 
     /**
      * Array of properties in the exact order they were handed in. This is
      * used by as-array serialization, deserialization.
      */
     private final SettableBeanProperty[] _propsInOrder;
 
     /**
      * Configuration of alias mappings, indexed by unmodified property name
      * to unmodified aliases, if any; entries only included for properties
      * that do have aliases.
      * This is is used for constructing actual reverse lookup mapping, if
      * needed, taking into account possible case-insensitivity, as well
      * as possibility of name prefixes.
      *
      * @since 2.9
      */
     private final Map<String,List<PropertyName>> _aliasDefs;
 
     /**
      * Mapping from secondary names (aliases) to primary names.
      *
      * @since 2.9
      */
     private final Map<String,String> _aliasMapping;
 
     /**
      * We require {@link Locale} since case changes are locale-sensitive in certain
      * cases (see <a href="https://en.wikipedia.org/wiki/Dotted_and_dotless_I">Turkish I</a>
      * for example)
      *
      * @since 2.11
      */
     private final Locale _locale;
 
     /**
      * @since 2.11
      */
     public BeanPropertyMap(boolean caseInsensitive, Collection<SettableBeanProperty> props,
             Map<String,List<PropertyName>> aliasDefs,
             Locale locale)
     {
         _caseInsensitive = caseInsensitive;
         _propsInOrder = props.toArray(new SettableBeanProperty[props.size()]);
         _aliasDefs = aliasDefs;
         _locale = locale;
         _aliasMapping = _buildAliasMapping(aliasDefs, caseInsensitive, locale);
         init(props);
 
     }
 
     /**
      * @deprecated since 2.11
      */
     @Deprecated
     public BeanPropertyMap(boolean caseInsensitive, Collection<SettableBeanProperty> props,
             Map<String,List<PropertyName>> aliasDefs) {
         this(caseInsensitive, props, aliasDefs, Locale.getDefault());
     }
 
     /* Copy constructors used when a property can replace existing one
      *
      * @since 2.9.6
      */
     private BeanPropertyMap(BeanPropertyMap src,
             SettableBeanProperty newProp, int hashIndex, int orderedIndex)
     {
         // First, copy most fields as is:
         _caseInsensitive = src._caseInsensitive;
         _locale = src._locale;
         _hashMask = src._hashMask;
         _size = src._size;
         _spillCount = src._spillCount;
         _aliasDefs = src._aliasDefs;
         _aliasMapping = src._aliasMapping;
 
         // but then make deep copy of arrays to modify
         _hashArea = Arrays.copyOf(src._hashArea, src._hashArea.length);
         _propsInOrder = Arrays.copyOf(src._propsInOrder, src._propsInOrder.length);
         _hashArea[hashIndex] = newProp;
         _propsInOrder[orderedIndex] = newProp;
     }
 
     /* Copy constructors used when a property needs to be appended (can't replace)
      *
      * @since 2.9.6
      */
     private BeanPropertyMap(BeanPropertyMap src,
             SettableBeanProperty newProp, String key, int slot)
     {
         // First, copy most fields as is:
         _caseInsensitive = src._caseInsensitive;
         _locale = src._locale;
         _hashMask = src._hashMask;
         _size = src._size;
         _spillCount = src._spillCount;
         _aliasDefs = src._aliasDefs;
         _aliasMapping = src._aliasMapping;
 
         // but then make deep copy of arrays to modify
         _hashArea = Arrays.copyOf(src._hashArea, src._hashArea.length);
         int last = src._propsInOrder.length;
         // and append property at the end of ordering
         _propsInOrder = Arrays.copyOf(src._propsInOrder, last+1);
         _propsInOrder[last] = newProp;
 
         final int hashSize = _hashMask+1;
         int ix = (slot<<1);
 
         // primary slot not free?
         if (_hashArea[ix] != null) {
             // secondary?
             ix = (hashSize + (slot >> 1)) << 1;
             if (_hashArea[ix] != null) {
                 // ok, spill over.
                 ix = ((hashSize + (hashSize >> 1) ) << 1) + _spillCount;
                 _spillCount += 2;
                 if (ix >= _hashArea.length) {
                     _hashArea = Arrays.copyOf(_hashArea, _hashArea.length + 4);
                 }
             }
         }
         _hashArea[ix] = key;
         _hashArea[ix+1] = newProp;
     }
 
     /**
      * @since 2.8
      */
     protected BeanPropertyMap(BeanPropertyMap base, boolean caseInsensitive)
     {
         _caseInsensitive = caseInsensitive;
         _locale = base._locale;
         _aliasDefs = base._aliasDefs;
         _aliasMapping = base._aliasMapping;
 
         // 16-May-2016, tatu: Alas, not enough to just change flag, need to re-init as well.
         _propsInOrder = Arrays.copyOf(base._propsInOrder, base._propsInOrder.length);
         init(Arrays.asList(_propsInOrder));
     }
 
     /**
      * Mutant factory method that constructs a new instance if desired case-insensitivity
      * state differs from the state of this instance; if states are the same, returns
      * <code>this</code>.
      *
      * @since 2.8
      */
     public BeanPropertyMap withCaseInsensitivity(boolean state) {
         if (_caseInsensitive == state) {
             return this;
         }
         return new BeanPropertyMap(this, state);
     }
 
     protected void init(Collection<SettableBeanProperty> props)
     {
         _size = props.size();
 
         // First: calculate size of primary hash area
         final int hashSize = findSize(_size);
         _hashMask = hashSize-1;
 
         // and allocate enough to contain primary/secondary, expand for spillovers as need be
         int alloc = (hashSize + (hashSize>>1)) * 2;
         Object[] hashed = new Object[alloc];
         int spillCount = 0;
 
         for (SettableBeanProperty prop : props) {
             // Due to removal, renaming, theoretically possible we'll have "holes" so:
             if (prop == null) {
                 continue;
             }
 
             String key = getPropertyName(prop);
             int slot = _hashCode(key);
             int ix = (slot<<1);
 
             // primary slot not free?
             if (hashed[ix] != null) {
                 // secondary?
                 ix = (hashSize + (slot >> 1)) << 1;
                 if (hashed[ix] != null) {
                     // ok, spill over.
                     ix = ((hashSize + (hashSize >> 1) ) << 1) + spillCount;
                     spillCount += 2;
                     if (ix >= hashed.length) {
                         hashed = Arrays.copyOf(hashed, hashed.length + 4);
                     }
                 }
             }
             hashed[ix] = key;
             hashed[ix+1] = prop;
 
             // and aliases
         }
         _hashArea = hashed;
         _spillCount = spillCount;
     }
 
     private final static int findSize(int size)
     {
         if (size <= 5) {
             return 8;
         }
         if (size <= 12) {
             return 16;
         }
         int needed = size + (size >> 2); // at most 80% full
         int result = 32;
         while (result < needed) {
             result += result;
         }
         return result;
     }
 
     /**
      * @since 2.12
      */
     public static BeanPropertyMap construct(MapperConfig<?> config,
             Collection<SettableBeanProperty> props,
             Map<String,List<PropertyName>> aliasMapping,
             boolean caseInsensitive) {
         return new BeanPropertyMap(caseInsensitive,
                 props, aliasMapping,
                 config.getLocale());
     }
 
     /**
      * @since 2.11
      * @deprecated since 2.12
      */
     @Deprecated
     public static BeanPropertyMap construct(MapperConfig<?> config,
             Collection<SettableBeanProperty> props,
             Map<String,List<PropertyName>> aliasMapping) {
         return new BeanPropertyMap(config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES),
                 props, aliasMapping,
                 config.getLocale());
     }
 
     /**
      * @deprecated since 2.11
      */
     @Deprecated
     public static BeanPropertyMap construct(Collection<SettableBeanProperty> props,
             boolean caseInsensitive, Map<String,List<PropertyName>> aliasMapping) {
         return new BeanPropertyMap(caseInsensitive, props, aliasMapping);
     }
 
 
/** Creates a new instance representing a copy of the instance but for one additional property that is passed as a parameter to the method. */

public BeanPropertyMap withProperty(SettableBeanProperty newProp) {
    int hashIndex = _hashCode(getPropertyName(newProp)) << 1;
    int orderedIndex = _propsInOrder.length;
    BeanPropertyMap newMap = new BeanPropertyMap(this, newProp, hashIndex, orderedIndex);
    return newMap;
}
 

}