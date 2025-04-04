package com.fasterxml.jackson.databind;
 
 import java.io.*;
 import java.lang.reflect.Type;
 import java.net.URL;
 import java.security.AccessController;
 import java.security.PrivilegedAction;
 import java.text.DateFormat;
 import java.util.*;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.atomic.AtomicReference;
 
 import com.fasterxml.jackson.annotation.*;
 import com.fasterxml.jackson.core.*;
 import com.fasterxml.jackson.core.exc.StreamReadException;
 import com.fasterxml.jackson.core.exc.StreamWriteException;
 import com.fasterxml.jackson.core.io.CharacterEscapes;
 import com.fasterxml.jackson.core.io.SegmentedStringWriter;
 import com.fasterxml.jackson.core.type.ResolvedType;
 import com.fasterxml.jackson.core.type.TypeReference;
 import com.fasterxml.jackson.core.util.*;
 import com.fasterxml.jackson.databind.cfg.*;
 import com.fasterxml.jackson.databind.deser.*;
 import com.fasterxml.jackson.databind.exc.MismatchedInputException;
 import com.fasterxml.jackson.databind.introspect.*;
 import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
 import com.fasterxml.jackson.databind.jsontype.*;
 import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
 import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;
 import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
 import com.fasterxml.jackson.databind.node.*;
 import com.fasterxml.jackson.databind.ser.*;
 import com.fasterxml.jackson.databind.type.*;
 import com.fasterxml.jackson.databind.util.ClassUtil;
 import com.fasterxml.jackson.databind.util.RootNameLookup;
 import com.fasterxml.jackson.databind.util.StdDateFormat;
 import com.fasterxml.jackson.databind.util.TokenBuffer;
 
 /**
  * ObjectMapper provides functionality for reading and writing JSON,
  * either to and from basic POJOs (Plain Old Java Objects), or to and from
  * a general-purpose JSON Tree Model ({@link JsonNode}), as well as
  * related functionality for performing conversions.
  * It is also highly customizable to work both with different styles of JSON
  * content, and to support more advanced Object concepts such as
  * polymorphism and Object identity.
  * <code>ObjectMapper</code> also acts as a factory for more advanced {@link ObjectReader}
  * and {@link ObjectWriter} classes.
  * Mapper (and {@link ObjectReader}s, {@link ObjectWriter}s it constructs) will
  * use instances of {@link JsonParser} and {@link JsonGenerator}
  * for implementing actual reading/writing of JSON.
  * Note that although most read and write methods are exposed through this class,
  * some of the functionality is only exposed via {@link ObjectReader} and
  * {@link ObjectWriter}: specifically, reading/writing of longer sequences of
  * values is only available through {@link ObjectReader#readValues(InputStream)}
  * and {@link ObjectWriter#writeValues(OutputStream)}.
  *<p>
 Simplest usage is of form:
 <pre>
   final ObjectMapper mapper = new ObjectMapper(); // can use static singleton, inject: just make sure to reuse!
   MyValue value = new MyValue();
   // ... and configure
   File newState = new File("my-stuff.json");
   mapper.writeValue(newState, value); // writes JSON serialization of MyValue instance
   // or, read
   MyValue older = mapper.readValue(new File("my-older-stuff.json"), MyValue.class);
 
   // Or if you prefer JSON Tree representation:
   JsonNode root = mapper.readTree(newState);
   // and find values by, for example, using a {@link com.fasterxml.jackson.core.JsonPointer} expression:
   int age = root.at("/personal/age").getValueAsInt(); 
 </pre>
  *<p>
  * The main conversion API is defined in {@link ObjectCodec}, so that
  * implementation details of this class need not be exposed to
  * streaming parser and generator classes. Usage via {@link ObjectCodec} is,
  * however, usually only for cases where dependency to {@link ObjectMapper} is
  * either not possible (from Streaming API), or undesireable (when only relying
  * on Streaming API).
  *<p> 
  * Mapper instances are fully thread-safe provided that ALL configuration of the
  * instance occurs before ANY read or write calls. If configuration of a mapper instance
  * is modified after first usage, changes may or may not take effect, and configuration
  * calls themselves may fail.
  * If you need to use different configuration, you have two main possibilities:
  *<ul>
  * <li>Construct and use {@link ObjectReader} for reading, {@link ObjectWriter} for writing.
  *    Both types are fully immutable and you can freely create new instances with different
  *    configuration using either factory methods of {@link ObjectMapper}, or readers/writers
  *    themselves. Construction of new {@link ObjectReader}s and {@link ObjectWriter}s is
  *    a very light-weight operation so it is usually appropriate to create these on per-call
  *    basis, as needed, for configuring things like optional indentation of JSON.
  *  </li>
  * <li>If the specific kind of configurability is not available via {@link ObjectReader} and
  *   {@link ObjectWriter}, you may need to use multiple {@link ObjectMapper} instead (for example:
  *   you cannot change mix-in annotations on-the-fly; or, set of custom (de)serializers).
  *   To help with this usage, you may want to use method {@link #copy()} which creates a clone
  *   of the mapper with specific configuration, and allows configuration of the copied instance
  *   before it gets used. Note that {@link #copy} operation is as expensive as constructing
  *   a new {@link ObjectMapper} instance: if possible, you should still pool and reuse mappers
  *   if you intend to use them for multiple operations.
  *  </li>
  * </ul>
  *<p>
  * Note on caching: root-level deserializers are always cached, and accessed
  * using full (generics-aware) type information. This is different from
  * caching of referenced types, which is more limited and is done only
  * for a subset of all deserializer types. The main reason for difference
  * is that at root-level there is no incoming reference (and hence no
  * referencing property, no referral information or annotations to
  * produce differing deserializers), and that the performance impact
  * greatest at root level (since it'll essentially cache the full
  * graph of deserializers involved).
  *<p>
  * Notes on security: use "default typing" feature (see {@link #enableDefaultTyping()})
  * is a potential security risk, if used with untrusted content (content generated by
  * untrusted external parties). If so, you may want to construct a custom 
  * {@link TypeResolverBuilder} implementation to limit possible types to instantiate,
  * (using {@link #setDefaultTyping}).
  */
 public class ObjectMapper
     extends ObjectCodec
     implements Versioned,
         java.io.Serializable // as of 2.1
 {
     private static final long serialVersionUID = 2L; // as of 2.9
 
     /*
     /**********************************************************
     /* Helper classes, enums
     /**********************************************************
      */
 
     /**
      * Enumeration used with {@link ObjectMapper#activateDefaultTyping(PolymorphicTypeValidator)}
      * to specify what kind of types (classes) default typing should
      * be used for. It will only be used if no explicit type information
      * is found, but this enumeration further limits subset of those types.
      *<p>
      * Since 2.4 there are special exceptions for JSON Tree model
      * types (sub-types of {@link TreeNode}: default typing is never
      * applied to them.
      * Since 2.8(.4) additional checks are made to avoid attempts at default
      * typing primitive-valued properties.
      *<p>
      * NOTE: use of Default Typing can be a potential security risk if incoming
      * content comes from untrusted sources, and it is recommended that this
      * is either not done, or, if enabled, make sure to {@code activateDefaultTyping(...)}
      * methods that take {@link PolymorphicTypeValidator} that limits applicability
      * to known trusted types.
      */
     public enum DefaultTyping {
         /**
          * This value means that only properties that have
          * {@link java.lang.Object} as declared type (including
          * generic types without explicit type) will use default
          * typing.
          */
         JAVA_LANG_OBJECT,
 
         /**
          * Value that means that default typing will be used for
          * properties with declared type of {@link java.lang.Object}
          * or an abstract type (abstract class or interface).
          * Note that this does <b>not</b> include array types.
          *<p>
          * Since 2.4, this does NOT apply to {@link TreeNode} and its subtypes.
          */
         OBJECT_AND_NON_CONCRETE,
 
         /**
          * Value that means that default typing will be used for
          * all types covered by {@link #OBJECT_AND_NON_CONCRETE}
          * plus all array types for them.
          *<p>
          * Since 2.4, this does NOT apply to {@link TreeNode} and its subtypes.
          */
         NON_CONCRETE_AND_ARRAYS,
 
         /**
          * Value that means that default typing will be used for
          * all non-final types, with exception of small number of
          * "natural" types (String, Boolean, Integer, Double), which
          * can be correctly inferred from JSON; as well as for
          * all arrays of non-final types.
          *<p>
          * Since 2.4, this does NOT apply to {@link TreeNode} and its subtypes.
          */
         NON_FINAL,
 
         /**
          * Value that means that default typing will be used for
          * all types, with exception of small number of
          * "natural" types (String, Boolean, Integer, Double) that
          * can be correctly inferred from JSON, and primitives (which
          * can not be polymorphic either).
          * Typing is also enabled for all array types.
          *<p>
          * WARNING: most of the time this is <b>NOT</b> the setting you want
          * as it tends to add Type Ids everywhere, even in cases
          * where type can not be anything other than declared (for example
          * if declared value type of a property is {@code final} -- for example,
          * properties of type {@code long} (or wrapper {@code Long}).
          *<p>
          * Note that this is rarely the option you should use as it results
          * in adding type information in many places where it should not be needed:
          * make sure you understand its behavior.
          * The only known use case for this setting is for serialization
          * when passing instances of final class, and base type is not
          * separately specified.
          *
          * @since 2.10
          */
         EVERYTHING
     }
 
     /**
      * Customized {@link TypeResolverBuilder} that provides type resolver builders
      * used with so-called "default typing"
      * (see {@link ObjectMapper#activateDefaultTyping(PolymorphicTypeValidator)} for details).
      *<p>
      * Type resolver construction is based on configuration: implementation takes care
      * of only providing builders in cases where type information should be applied.
      * This is important since build calls may be sent for any and all types, and
      * type information should NOT be applied to all of them.
      */
     public static class DefaultTypeResolverBuilder
         extends StdTypeResolverBuilder
         implements java.io.Serializable
     {
         private static final long serialVersionUID = 1L;
 
         /**
          * Definition of what types is this default typer valid for.
          */
         protected final DefaultTyping _appliesFor;
 
         /**
          * {@link PolymorphicTypeValidator} top use for validating that the subtypes
          * resolved are valid for use (usually to protect against possible
          * security issues)
          *
          * @since 2.10
          */
         protected final PolymorphicTypeValidator _subtypeValidator;
 
         /**
          * @deprecated Since 2.10
          */
         @Deprecated // since 2.10
         public DefaultTypeResolverBuilder(DefaultTyping t) {
             this(t, LaissezFaireSubTypeValidator.instance);
         }
 
         /**
          * @since 2.10
          */
         public DefaultTypeResolverBuilder(DefaultTyping t, PolymorphicTypeValidator ptv) {
             _appliesFor = _requireNonNull(t, "Can not pass `null` DefaultTyping");
             _subtypeValidator = _requireNonNull(ptv, "Can not pass `null` PolymorphicTypeValidator");
         }
 
         // @since 2.13
         protected DefaultTypeResolverBuilder(DefaultTypeResolverBuilder base, Class<?> defaultImpl) {
             super(base, defaultImpl);
             _appliesFor = base._appliesFor;
             _subtypeValidator = base._subtypeValidator;
         }
 
         // 20-Jan-2020: as per [databind#2599] Objects.requireNonNull() from JDK7 not in all Android so
         private static <T> T _requireNonNull(T value, String msg) {
             // Replacement for: return Objects.requireNonNull(t, msg);
             if (value == null) {
                 throw new NullPointerException(msg);
             }
             return value;
         }
 
         /**
          * @since 2.10
          */
         public static DefaultTypeResolverBuilder construct(DefaultTyping t,
                 PolymorphicTypeValidator ptv) {
             return new DefaultTypeResolverBuilder(t, ptv);
         }
 
         @Override // since 2.13
         public DefaultTypeResolverBuilder withDefaultImpl(Class<?> defaultImpl) {
             if (_defaultImpl == defaultImpl) {
                 return this;
             }
             ClassUtil.verifyMustOverride(DefaultTypeResolverBuilder.class, this, "withDefaultImpl");
             return new DefaultTypeResolverBuilder(this, defaultImpl);
         }
 
         @Override // since 2.10
         public PolymorphicTypeValidator subTypeValidator(MapperConfig<?> config) {
             return _subtypeValidator;
         }
 
         @Override
         public TypeDeserializer buildTypeDeserializer(DeserializationConfig config,
                 JavaType baseType, Collection<NamedType> subtypes)
         {
             return useForType(baseType) ? super.buildTypeDeserializer(config, baseType, subtypes) : null;
         }
 
         @Override
         public TypeSerializer buildTypeSerializer(SerializationConfig config,
                 JavaType baseType, Collection<NamedType> subtypes)
         {
             return useForType(baseType) ? super.buildTypeSerializer(config, baseType, subtypes) : null;            
         }
 
         /**
          * Method called to check if the default type handler should be
          * used for given type.
          * Note: "natural types" (String, Boolean, Integer, Double) will never
          * use typing; that is both due to them being concrete and final,
          * and since actual serializers and deserializers will also ignore any
          * attempts to enforce typing.
          */
         public boolean useForType(JavaType t)
         {
             // 03-Oct-2016, tatu: As per [databind#1395], need to skip
             //  primitive types too, regardless
             if (t.isPrimitive()) {
                 return false;
             }
 
             switch (_appliesFor) {
             case NON_CONCRETE_AND_ARRAYS:
                 while (t.isArrayType()) {
                     t = t.getContentType();
                 }
                 // fall through
             case OBJECT_AND_NON_CONCRETE:
                 // 19-Apr-2016, tatu: ReferenceType like Optional also requires similar handling:
                 while (t.isReferenceType()) {
                     t = t.getReferencedType();
                 }
                 return t.isJavaLangObject()
                         || (!t.isConcrete()
                                 // [databind#88] Should not apply to JSON tree models:
                                 && !TreeNode.class.isAssignableFrom(t.getRawClass()));
 
             case NON_FINAL:
                 while (t.isArrayType()) {
                     t = t.getContentType();
                 }
                 // 19-Apr-2016, tatu: ReferenceType like Optional also requires similar handling:
                 while (t.isReferenceType()) {
                     t = t.getReferencedType();
                 }
                 // [databind#88] Should not apply to JSON tree models:
                 return !t.isFinal() && !TreeNode.class.isAssignableFrom(t.getRawClass());
             case EVERYTHING:
                 // So, excluding primitives (handled earlier) and "Natural types" (handled
                 // before this method is called), applied to everything
                 return true;
             default:
             case JAVA_LANG_OBJECT:
                 return t.isJavaLangObject();
             }
         }
     }
 
     /*
     /**********************************************************
     /* Internal constants, singletons
     /**********************************************************
      */
 
     // 16-May-2009, tatu: Ditto ^^^
     protected final static AnnotationIntrospector DEFAULT_ANNOTATION_INTROSPECTOR = new JacksonAnnotationIntrospector();
 
     /**
      * Base settings contain defaults used for all {@link ObjectMapper}
      * instances.
      */
     protected final static BaseSettings DEFAULT_BASE = new BaseSettings(
             null, // cannot share global ClassIntrospector any more (2.5+)
             DEFAULT_ANNOTATION_INTROSPECTOR,
              null, TypeFactory.defaultInstance(),
             null, StdDateFormat.instance, null,
             Locale.getDefault(),
             null, // to indicate "use Jackson default TimeZone" (UTC since Jackson 2.7)
             Base64Variants.getDefaultVariant(),
             // Only for 2.x; 3.x will use more restrictive default
             LaissezFaireSubTypeValidator.instance,
             // Since 2.12:
             new DefaultAccessorNamingStrategy.Provider()
     );
 
     /*
     /**********************************************************
     /* Configuration settings, shared
     /**********************************************************
      */
 
     /**
      * Factory used to create {@link JsonParser} and {@link JsonGenerator}
      * instances as necessary.
      */
     protected final JsonFactory _jsonFactory;
 
     /**
      * Specific factory used for creating {@link JavaType} instances;
      * needed to allow modules to add more custom type handling
      * (mostly to support types of non-Java JVM languages)
      */
     protected TypeFactory _typeFactory;
 
     /**
      * Provider for values to inject in deserialized POJOs.
      */
     protected InjectableValues _injectableValues;
 
     /**
      * Thing used for registering sub-types, resolving them to
      * super/sub-types as needed.
      */
     protected SubtypeResolver _subtypeResolver;
 
     /**
      * Currently active per-type configuration overrides, accessed by
      * declared type of property.
      *
      * @since 2.9
      */
     protected final ConfigOverrides _configOverrides;
 
     /**
      * Current set of coercion configuration definitions that define allowed
      * (and not allowed) coercions from secondary shapes.
      *
      * @since 2.12
      */
     protected final CoercionConfigs _coercionConfigs;
 
     /*
     /**********************************************************
     /* Configuration settings: mix-in annotations
     /**********************************************************
      */
 
     /**
      * Mapping that defines how to apply mix-in annotations: key is
      * the type to received additional annotations, and value is the
      * type that has annotations to "mix in".
      *<p>
      * Annotations associated with the value classes will be used to
      * override annotations of the key class, associated with the
      * same field or method. They can be further masked by sub-classes:
      * you can think of it as injecting annotations between the target
      * class and its sub-classes (or interfaces)
      * 
      * @since 2.6 (earlier was a simple {@link java.util.Map}
      */
     protected SimpleMixInResolver _mixIns;
 
     /*
     /**********************************************************
     /* Configuration settings, serialization
     /**********************************************************
      */
 
     /**
      * Configuration object that defines basic global
      * settings for the serialization process
      */
     protected SerializationConfig _serializationConfig;
 
     /**
      * Object that manages access to serializers used for serialization,
      * including caching.
      * It is configured with {@link #_serializerFactory} to allow
      * for constructing custom serializers.
      *<p>
      * Note: while serializers are only exposed {@link SerializerProvider},
      * mappers and readers need to access additional API defined by
      * {@link DefaultSerializerProvider}
      */
     protected DefaultSerializerProvider _serializerProvider;
 
     /**
      * Serializer factory used for constructing serializers.
      */
     protected SerializerFactory _serializerFactory;
 
     /*
     /**********************************************************
     /* Configuration settings, deserialization
     /**********************************************************
      */
 
     /**
      * Configuration object that defines basic global
      * settings for the serialization process
      */
     protected DeserializationConfig _deserializationConfig;
 
     /**
      * Blueprint context object; stored here to allow custom
      * sub-classes. Contains references to objects needed for
      * deserialization construction (cache, factory).
      */
     protected DefaultDeserializationContext _deserializationContext;
 
     /*
     /**********************************************************
     /* Module-related
     /**********************************************************
      */
 
     /**
      * Set of module types (as per {@link Module#getTypeId()} that have been
      * registered; kept track of iff {@link MapperFeature#IGNORE_DUPLICATE_MODULE_REGISTRATIONS}
      * is enabled, so that duplicate registration calls can be ignored
      * (to avoid adding same handlers multiple times, mostly).
      * 
      * @since 2.5
      */
     protected Set<Object> _registeredModuleTypes;
 
     /*
     /**********************************************************
     /* Caching
     /**********************************************************
      */
 
     /* Note: handling of serializers and deserializers is not symmetric;
      * and as a result, only root-level deserializers can be cached here.
      * This is mostly because typing and resolution for deserializers is
      * fully static; whereas it is quite dynamic for serialization.
      */
 
     /**
      * We will use a separate main-level Map for keeping track
      * of root-level deserializers. This is where most successful
      * cache lookups get resolved.
      * Map will contain resolvers for all kinds of types, including
      * container types: this is different from the component cache
      * which will only cache bean deserializers.
      *<p>
      * Given that we don't expect much concurrency for additions
      * (should very quickly converge to zero after startup), let's
      * explicitly define a low concurrency setting.
      *<p>
      * These may are either "raw" deserializers (when
      * no type information is needed for base type), or type-wrapped
      * deserializers (if it is needed)
      */
     final protected ConcurrentHashMap<JavaType, JsonDeserializer<Object>> _rootDeserializers
         = new ConcurrentHashMap<JavaType, JsonDeserializer<Object>>(64, 0.6f, 2);
 
     /*
     /**********************************************************
     /* Life-cycle: constructing instance
     /**********************************************************
      */
 
     /**
      * Default constructor, which will construct the default
      * {@link JsonFactory} as necessary, use
      * {@link SerializerProvider} as its
      * {@link SerializerProvider}, and
      * {@link BeanSerializerFactory} as its
      * {@link SerializerFactory}.
      * This means that it
      * can serialize all standard JDK types, as well as regular
      * Java Beans (based on method names and Jackson-specific annotations),
      * but does not support JAXB annotations.
      */
     public ObjectMapper() {
         this(null, null, null);
     }
 
     /**
      * Constructs instance that uses specified {@link JsonFactory}
      * for constructing necessary {@link JsonParser}s and/or
      * {@link JsonGenerator}s.
      */
     public ObjectMapper(JsonFactory jf) {
         this(jf, null, null);
     }
 
     /**
      * Copy-constructor, mostly used to support {@link #copy}.
      * 
      * @since 2.1
      */
     protected ObjectMapper(ObjectMapper src)
     {
         _jsonFactory = src._jsonFactory.copy();
         _jsonFactory.setCodec(this);
         _subtypeResolver = src._subtypeResolver.copy();
         _typeFactory = src._typeFactory;
         _injectableValues = src._injectableValues;
         _configOverrides = src._configOverrides.copy();
         _coercionConfigs = src._coercionConfigs.copy();
         _mixIns = src._mixIns.copy();
 
         RootNameLookup rootNames = new RootNameLookup();
         _serializationConfig = new SerializationConfig(src._serializationConfig,
                 _subtypeResolver, _mixIns, rootNames, _configOverrides);
         _deserializationConfig = new DeserializationConfig(src._deserializationConfig,
                 _subtypeResolver, _mixIns, rootNames, _configOverrides,
                 _coercionConfigs);
         _serializerProvider = src._serializerProvider.copy();
         _deserializationContext = src._deserializationContext.copy();
 
         // Default serializer factory is stateless, can just assign
         _serializerFactory = src._serializerFactory;
 
         // as per [databind#922], [databind#1078] make sure to copy registered modules as appropriate
         Set<Object> reg = src._registeredModuleTypes;
         if (reg == null) {
             _registeredModuleTypes = null;
         } else {
             _registeredModuleTypes = new LinkedHashSet<Object>(reg);
         }
     }
 
     /**
      * Constructs instance that uses specified {@link JsonFactory}
      * for constructing necessary {@link JsonParser}s and/or
      * {@link JsonGenerator}s, and uses given providers for accessing
      * serializers and deserializers.
      * 
      * @param jf JsonFactory to use: if null, a new {@link MappingJsonFactory} will be constructed
      * @param sp SerializerProvider to use: if null, a {@link SerializerProvider} will be constructed
      * @param dc Blueprint deserialization context instance to use for creating
      *    actual context objects; if null, will construct standard
      *    {@link DeserializationContext}
      */
     public ObjectMapper(JsonFactory jf,
             DefaultSerializerProvider sp, DefaultDeserializationContext dc)
     {
         // 02-Mar-2009, tatu: Important: we MUST default to using the mapping factory,
         //  otherwise tree serialization will have problems with POJONodes.
         if (jf == null) {
             _jsonFactory = new MappingJsonFactory(this);
         } else {
             _jsonFactory = jf;
             if (jf.getCodec() == null) { // as per [JACKSON-741]
                 _jsonFactory.setCodec(this);
             }
         }
         _subtypeResolver = new StdSubtypeResolver();
         RootNameLookup rootNames = new RootNameLookup();
         // and default type factory is shared one
         _typeFactory = TypeFactory.defaultInstance();
 
         SimpleMixInResolver mixins = new SimpleMixInResolver(null);
         _mixIns = mixins;
         BaseSettings base = DEFAULT_BASE.withClassIntrospector(defaultClassIntrospector());
         _configOverrides = new ConfigOverrides();
         _coercionConfigs = new CoercionConfigs();
         _serializationConfig = new SerializationConfig(base,
                     _subtypeResolver, mixins, rootNames, _configOverrides);
         _deserializationConfig = new DeserializationConfig(base,
                     _subtypeResolver, mixins, rootNames, _configOverrides,
                     _coercionConfigs);
 
         // Some overrides we may need
         final boolean needOrder = _jsonFactory.requiresPropertyOrdering();
         if (needOrder ^ _serializationConfig.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)) {
             configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, needOrder);
         }
         
         _serializerProvider = (sp == null) ? new DefaultSerializerProvider.Impl() : sp;
         _deserializationContext = (dc == null) ?
                 new DefaultDeserializationContext.Impl(BeanDeserializerFactory.instance) : dc;
 
         // Default serializer factory is stateless, can just assign
         _serializerFactory = BeanSerializerFactory.instance;
     }
 
     /**
      * Overridable helper method used to construct default {@link ClassIntrospector}
      * to use.
      * 
      * @since 2.5
      */
     protected ClassIntrospector defaultClassIntrospector() {
         return new BasicClassIntrospector();
     }
 
     /*
     /**********************************************************
     /* Methods sub-classes MUST override
     /**********************************************************
      */
     
     /**
      * Method for creating a new {@link ObjectMapper} instance that
      * has same initial configuration as this instance. Note that this
      * also requires making a copy of the underlying {@link JsonFactory}
      * instance.
      *<p>
      * Method is typically
      * used when multiple, differently configured mappers are needed.
      * Although configuration is shared, cached serializers and deserializers
      * are NOT shared, which means that the new instance may be re-configured
      * before use; meaning that it behaves the same way as if an instance
      * was constructed from scratch.
      * 
      * @since 2.1
      */
     public ObjectMapper copy() {
         _checkInvalidCopy(ObjectMapper.class);
         return new ObjectMapper(this);
     }
 
     /**
      * @since 2.1
      */
     protected void _checkInvalidCopy(Class<?> exp)
     {
         if (getClass() != exp) {
             // 10-Nov-2016, tatu: could almost use `ClassUtil.verifyMustOverride()` but not quite
             throw new IllegalStateException("Failed copy(): "+getClass().getName()
                     +" (version: "+version()+") does not override copy(); it has to");
         }
     }
 
     /*
     /**********************************************************
     /* Methods sub-classes MUST override if providing custom
     /* ObjectReader/ObjectWriter implementations
     /**********************************************************
      */
     
     /**
      * Factory method sub-classes must override, to produce {@link ObjectReader}
      * instances of proper sub-type
      * 
      * @since 2.5
      */
     protected ObjectReader _newReader(DeserializationConfig config) {
         return new ObjectReader(this, config);
     }
 
     /**
      * Factory method sub-classes must override, to produce {@link ObjectReader}
      * instances of proper sub-type
      * 
      * @since 2.5
      */
     protected ObjectReader _newReader(DeserializationConfig config,
             JavaType valueType, Object valueToUpdate,
             FormatSchema schema, InjectableValues injectableValues) {
         return new ObjectReader(this, config, valueType, valueToUpdate, schema, injectableValues);
     }
 
     /**
      * Factory method sub-classes must override, to produce {@link ObjectWriter}
      * instances of proper sub-type
      * 
      * @since 2.5
      */
     protected ObjectWriter _newWriter(SerializationConfig config) {
         return new ObjectWriter(this, config);
     }
 
     /**
      * Factory method sub-classes must override, to produce {@link ObjectWriter}
      * instances of proper sub-type
      * 
      * @since 2.5
      */
     protected ObjectWriter _newWriter(SerializationConfig config, FormatSchema schema) {
         return new ObjectWriter(this, config, schema);
     }
 
     /**
      * Factory method sub-classes must override, to produce {@link ObjectWriter}
      * instances of proper sub-type
      * 
      * @since 2.5
      */
     protected ObjectWriter _newWriter(SerializationConfig config,
             JavaType rootType, PrettyPrinter pp) {
         return new ObjectWriter(this, config, rootType, pp);
     }
 
     /*
     /**********************************************************
     /* Versioned impl
     /**********************************************************
      */
 
     /**
      * Method that will return version information stored in and read from jar
      * that contains this class.
      */
     @Override
     public Version version() {
         return com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION;
     }
 
     /*
     /**********************************************************
     /* Module registration, discovery
     /**********************************************************
      */
 
 
/** Method for registering a module that can extend functionality  provided by this mapper; for example, by adding providers for  custom serializers and deserializers. */

public ObjectMapper registerModule(Module module) {
    _checkInvalidCopy(ObjectMapper.class);
    if (_registeredModuleTypes == null) {
        _registeredModuleTypes = new LinkedHashSet<>();
    }
    _registeredModuleTypes.add(module);
    return this;
}
 

}