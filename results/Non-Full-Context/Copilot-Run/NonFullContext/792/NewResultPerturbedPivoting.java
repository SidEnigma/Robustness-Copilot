package com.fasterxml.jackson.databind;
 
 import java.io.*;
 import java.net.URL;
 import java.util.*;
 import java.util.concurrent.ConcurrentHashMap;
 
 import com.fasterxml.jackson.core.*;
 import com.fasterxml.jackson.core.exc.StreamReadException;
 import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
 import com.fasterxml.jackson.core.filter.JsonPointerBasedFilter;
 import com.fasterxml.jackson.core.filter.TokenFilter;
 import com.fasterxml.jackson.core.filter.TokenFilter.Inclusion;
 import com.fasterxml.jackson.core.type.ResolvedType;
 import com.fasterxml.jackson.core.type.TypeReference;
 
 import com.fasterxml.jackson.databind.cfg.ContextAttributes;
 import com.fasterxml.jackson.databind.deser.DataFormatReaders;
 import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
 import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
 import com.fasterxml.jackson.databind.node.JsonNodeFactory;
 import com.fasterxml.jackson.databind.node.TreeTraversingParser;
 import com.fasterxml.jackson.databind.type.TypeFactory;
 import com.fasterxml.jackson.databind.util.ClassUtil;
 
 /**
  * Builder object that can be used for per-serialization configuration of
  * deserialization parameters, such as root type to use or object
  * to update (instead of constructing new instance).
  *<p>
  * Uses "mutant factory" pattern so that instances are immutable
  * (and thus fully thread-safe with no external synchronization);
  * new instances are constructed for different configurations.
  * Instances are initially constructed by {@link ObjectMapper} and can be
  * reused, shared, cached; both because of thread-safety and because
  * instances are relatively light-weight.
  *<p>
  * NOTE: this class is NOT meant as sub-classable (with Jackson 2.8 and
  * above) by users. It is left as non-final mostly to allow frameworks
  * that require bytecode generation for proxying and similar use cases,
  * but there is no expecation that functionality should be extended
  * by sub-classing.
  */
 public class ObjectReader
     extends ObjectCodec
     implements Versioned, java.io.Serializable // since 2.1
 {
     private static final long serialVersionUID = 2L; // since 2.9
 
     /*
     /**********************************************************
     /* Immutable configuration from ObjectMapper
     /**********************************************************
      */
 
     /**
      * General serialization configuration settings; while immutable,
      * can use copy-constructor to create modified instances as necessary.
      */
     protected final DeserializationConfig _config;
 
     /**
      * Blueprint instance of deserialization context; used for creating
      * actual instance when needed.
      */
     protected final DefaultDeserializationContext _context;
 
     /**
      * Factory used for constructing {@link JsonGenerator}s
      */
     protected final JsonFactory _parserFactory;
 
     /**
      * Flag that indicates whether root values are expected to be unwrapped or not
      */
     protected final boolean _unwrapRoot;
 
     /**
      * Filter to be consider for JsonParser.  
      * Default value to be null as filter not considered.
      */
     private final TokenFilter _filter;
     
     /*
     /**********************************************************
     /* Configuration that can be changed during building
     /**********************************************************
      */
 
     /**
      * Declared type of value to instantiate during deserialization.
      * Defines which deserializer to use; as well as base type of instance
      * to construct if an updatable value is not configured to be used
      * (subject to changes by embedded type information, for polymorphic
      * types). If {@link #_valueToUpdate} is non-null, only used for
      * locating deserializer.
      */
     protected final JavaType _valueType;
 
     /**
      * We may pre-fetch deserializer as soon as {@link #_valueType}
      * is known, and if so, reuse it afterwards.
      * This allows avoiding further deserializer lookups and increases
      * performance a bit on cases where readers are reused.
      * 
      * @since 2.1
      */
     protected final JsonDeserializer<Object> _rootDeserializer;
     
     /**
      * Instance to update with data binding; if any. If null,
      * a new instance is created, if non-null, properties of
      * this value object will be updated instead.
      * Note that value can be of almost any type, except not
      * {@link com.fasterxml.jackson.databind.type.ArrayType}; array
      * types cannot be modified because array size is immutable.
      */
     protected final Object _valueToUpdate;
 
     /**
      * When using data format that uses a schema, schema is passed
      * to parser.
      */
     protected final FormatSchema _schema;
 
     /**
      * Values that can be injected during deserialization, if any.
      */
     protected final InjectableValues _injectableValues;
 
     /**
      * Optional detector used for auto-detecting data format that byte-based
      * input uses.
      *<p>
      * NOTE: If defined non-null, <code>readValue()</code> methods that take
      * {@link Reader} or {@link String} input <b>will fail with exception</b>,
      * because format-detection only works on byte-sources. Also, if format
      * cannot be detect reliably (as per detector settings),
      * a {@link StreamReadException} will be thrown).
      * 
      * @since 2.1
      */
     protected final DataFormatReaders _dataFormatReaders;
 
     /*
     /**********************************************************
     /* Caching
     /**********************************************************
      */
 
     /**
      * Root-level cached deserializers.
      * Passed by {@link ObjectMapper}, shared with it.
      */
     final protected ConcurrentHashMap<JavaType, JsonDeserializer<Object>> _rootDeserializers;
 
     /**
      * Lazily resolved {@link JavaType} for {@link JsonNode}
      */
     protected transient JavaType _jsonNodeType;
     
     /*
     /**********************************************************
     /* Life-cycle, construction
     /**********************************************************
      */
 
     /**
      * Constructor used by {@link ObjectMapper} for initial instantiation
      */
     protected ObjectReader(ObjectMapper mapper, DeserializationConfig config) {
         this(mapper, config, null, null, null, null);
     }
 
     /**
      * Constructor called when a root deserializer should be fetched based
      * on other configuration.
      */
     protected ObjectReader(ObjectMapper mapper, DeserializationConfig config,
             JavaType valueType, Object valueToUpdate,
             FormatSchema schema, InjectableValues injectableValues)
     {
         _config = config;
         _context = mapper._deserializationContext;
         _rootDeserializers = mapper._rootDeserializers;
         _parserFactory = mapper._jsonFactory;
         _valueType = valueType;
         _valueToUpdate = valueToUpdate;
         _schema = schema;
         _injectableValues = injectableValues;
         _unwrapRoot = config.useRootWrapping();
 
         _rootDeserializer = _prefetchRootDeserializer(valueType);
         _dataFormatReaders = null;        
         _filter = null;
     }
     
     /**
      * Copy constructor used for building variations.
      */
     protected ObjectReader(ObjectReader base, DeserializationConfig config,
             JavaType valueType, JsonDeserializer<Object> rootDeser, Object valueToUpdate,
             FormatSchema schema, InjectableValues injectableValues,
             DataFormatReaders dataFormatReaders)
     {
         _config = config;
         _context = base._context;
 
         _rootDeserializers = base._rootDeserializers;
         _parserFactory = base._parserFactory;
 
         _valueType = valueType;
         _rootDeserializer = rootDeser;
         _valueToUpdate = valueToUpdate;
         _schema = schema;
         _injectableValues = injectableValues;
         _unwrapRoot = config.useRootWrapping();
         _dataFormatReaders = dataFormatReaders;
         _filter = base._filter;
     }
 
     /**
      * Copy constructor used when modifying simple feature flags
      */
     protected ObjectReader(ObjectReader base, DeserializationConfig config)
     {
         _config = config;
         _context = base._context;
 
         _rootDeserializers = base._rootDeserializers;
         _parserFactory = base._parserFactory;
 
         _valueType = base._valueType;
         _rootDeserializer = base._rootDeserializer;
         _valueToUpdate = base._valueToUpdate;
         _schema = base._schema;
         _injectableValues = base._injectableValues;
         _unwrapRoot = config.useRootWrapping();
         _dataFormatReaders = base._dataFormatReaders;
         _filter = base._filter;
     }
     
     protected ObjectReader(ObjectReader base, JsonFactory f)
     {
         // may need to override ordering, based on data format capabilities
         _config = base._config
             .with(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, f.requiresPropertyOrdering());
         _context = base._context;
 
         _rootDeserializers = base._rootDeserializers;
         _parserFactory = f;
 
         _valueType = base._valueType;
         _rootDeserializer = base._rootDeserializer;
         _valueToUpdate = base._valueToUpdate;
         _schema = base._schema;
         _injectableValues = base._injectableValues;
         _unwrapRoot = base._unwrapRoot;
         _dataFormatReaders = base._dataFormatReaders;
         _filter = base._filter;
     }
     
     protected ObjectReader(ObjectReader base, TokenFilter filter) {
         _config = base._config;
         _context = base._context;
         _rootDeserializers = base._rootDeserializers;
         _parserFactory = base._parserFactory;
         _valueType = base._valueType;
         _rootDeserializer = base._rootDeserializer;
         _valueToUpdate = base._valueToUpdate;
         _schema = base._schema;
         _injectableValues = base._injectableValues;
         _unwrapRoot = base._unwrapRoot;
         _dataFormatReaders = base._dataFormatReaders;
         _filter = filter;
     }
     
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
     /* Helper methods used internally for invoking constructors
     /* Need to be overridden if sub-classing (not recommended)
     /* is used.
     /**********************************************************
      */
 
     /**
      * Overridable factory method called by various "withXxx()" methods
      * 
      * @since 2.5
      */
     protected ObjectReader _new(ObjectReader base, JsonFactory f) {
         return new ObjectReader(base, f);
     }
 
     /**
      * Overridable factory method called by various "withXxx()" methods
      * 
      * @since 2.5
      */
     protected ObjectReader _new(ObjectReader base, DeserializationConfig config) {
         return new ObjectReader(base, config);
     }
 
     /**
      * Overridable factory method called by various "withXxx()" methods
      * 
      * @since 2.5
      */
     protected ObjectReader _new(ObjectReader base, DeserializationConfig config,
             JavaType valueType, JsonDeserializer<Object> rootDeser, Object valueToUpdate,
             FormatSchema schema, InjectableValues injectableValues,
             DataFormatReaders dataFormatReaders) {
         return new ObjectReader(base, config, valueType, rootDeser,  valueToUpdate,
                  schema,  injectableValues, dataFormatReaders);
     }
 
     /**
      * Factory method used to create {@link MappingIterator} instances;
      * either default, or custom subtype.
      * 
      * @since 2.5
      */
     protected <T> MappingIterator<T> _newIterator(JsonParser p, DeserializationContext ctxt,
             JsonDeserializer<?> deser, boolean parserManaged)
     {
         return new MappingIterator<T>(_valueType, p, ctxt,
                 deser, parserManaged, _valueToUpdate);
     }
 
     /*
     /**********************************************************
     /* Methods for initializing parser instance to use
     /**********************************************************
      */
 
     protected JsonToken _initForReading(DeserializationContext ctxt, JsonParser p)
         throws IOException
     {
         _config.initialize(p, _schema);
 
         /* First: must point to a token; if not pointing to one, advance.
          * This occurs before first read from JsonParser, as well as
          * after clearing of current token.
          */
         JsonToken t = p.currentToken();
         if (t == null) { // and then we must get something...
             t = p.nextToken();
             if (t == null) {
                 // Throw mapping exception, since it's failure to map, not an actual parsing problem
                 ctxt.reportInputMismatch(_valueType,
                         "No content to map due to end-of-input");
             }
         }
         return t;
     }
 
     /**
      * Alternative to {@link #_initForReading} used in cases where reading
      * of multiple values means that we may or may not want to advance the stream,
      * but need to do other initialization.
      *<p>
      * Base implementation only sets configured {@link FormatSchema}, if any, on parser.
      * 
      * @since 2.8
      */
     protected void _initForMultiRead(DeserializationContext ctxt, JsonParser p)
         throws IOException
     {
         _config.initialize(p, _schema);
     }
 
     /*
     /**********************************************************
     /* Life-cycle, fluent factory methods for DeserializationFeatures
     /**********************************************************
      */
 
     /**
      * Method for constructing a new reader instance that is configured
      * with specified feature enabled.
      */
     public ObjectReader with(DeserializationFeature feature) {
         return _with(_config.with(feature));
     }
 
     /**
      * Method for constructing a new reader instance that is configured
      * with specified features enabled.
      */
     public ObjectReader with(DeserializationFeature first,
             DeserializationFeature... other)
     {
         return _with(_config.with(first, other));
     }    
 
     /**
      * Method for constructing a new reader instance that is configured
      * with specified features enabled.
      */
     public ObjectReader withFeatures(DeserializationFeature... features) {
         return _with(_config.withFeatures(features));
     }    
 
     /**
      * Method for constructing a new reader instance that is configured
      * with specified feature disabled.
      */
     public ObjectReader without(DeserializationFeature feature) {
         return _with(_config.without(feature)); 
     }
 
     /**
      * Method for constructing a new reader instance that is configured
      * with specified features disabled.
      */
     public ObjectReader without(DeserializationFeature first,
             DeserializationFeature... other) {
         return _with(_config.without(first, other));
     }    
 
     /**
      * Method for constructing a new reader instance that is configured
      * with specified features disabled.
      */
     public ObjectReader withoutFeatures(DeserializationFeature... features) {
         return _with(_config.withoutFeatures(features));
     }    
 
     /*
     /**********************************************************
     /* Life-cycle, fluent factory methods for JsonParser.Features
     /* (to be deprecated in 2.12?)
     /**********************************************************
      */
 
     /**
      * Method for constructing a new reader instance that is configured
      * with specified feature enabled.
      *
      * @param feature Feature to enable
      *
      * @return Reader instance with specified feature enabled
      */
     public ObjectReader with(JsonParser.Feature feature) {
         return _with(_config.with(feature));
     }
 
     /**
      * Method for constructing a new reader instance that is configured
      * with specified features enabled.
      *
      * @param features Features to enable
      *
      * @return Reader instance with specified features enabled
      */
     public ObjectReader withFeatures(JsonParser.Feature... features) {
         return _with(_config.withFeatures(features));
     }    
 
     /**
      * Method for constructing a new reader instance that is configured
      * with specified feature disabled.
      *
      * @param feature Feature to disable
      *
      * @return Reader instance with specified feature disabled
      */
     public ObjectReader without(JsonParser.Feature feature) {
         return _with(_config.without(feature)); 
     }
 
     /**
      * Method for constructing a new reader instance that is configured
      * with specified features disabled.
      *
      * @param features Features to disable
      *
      * @return Reader instance with specified features disabled
      */
     public ObjectReader withoutFeatures(JsonParser.Feature... features) {
         return _with(_config.withoutFeatures(features));
     }
 
     /*
     /**********************************************************************
     /* Life-cycle, fluent factory methods for StreamReadFeatures (added in 2.11)
     /**********************************************************************
      */
 
     /**
      * Method for constructing a new reader instance that is configured
      * with specified feature enabled.
      *
      * @return Reader instance with specified feature enabled
      *
      * @since 2.11
      */
     public ObjectReader with(StreamReadFeature feature) {
         return _with(_config.with(feature.mappedFeature()));
     }
 
     /**
      * Method for constructing a new reader instance that is configured
      * with specified feature disabled.
      *
      * @return Reader instance with specified feature enabled
      *
      * @since 2.11
      */
     public ObjectReader without(StreamReadFeature feature) {
         return _with(_config.without(feature.mappedFeature()));
     }
 
     /*
     /**********************************************************
     /* Life-cycle, fluent factory methods for FormatFeature (2.7)
     /**********************************************************
      */
 
     /**
      * Method for constructing a new reader instance that is configured
      * with specified feature enabled.
      *
      * @since 2.7
      */
     public ObjectReader with(FormatFeature feature) {
         return _with(_config.with(feature));
     }
 
     /**
      * Method for constructing a new reader instance that is configured
      * with specified features enabled.
      *
      * @since 2.7
      */
     public ObjectReader withFeatures(FormatFeature... features) {
         return _with(_config.withFeatures(features));
     }    
 
     /**
      * Method for constructing a new reader instance that is configured
      * with specified feature disabled.
      *
      * @since 2.7
      */
     public ObjectReader without(FormatFeature feature) {
         return _with(_config.without(feature)); 
     }
 
     /**
      * Method for constructing a new reader instance that is configured
      * with specified features disabled.
      *
      * @since 2.7
      */
     public ObjectReader withoutFeatures(FormatFeature... features) {
         return _with(_config.withoutFeatures(features));
     }
     
     /*
     /**********************************************************
     /* Life-cycle, fluent factory methods, other
     /**********************************************************
      */
 
     /**
      * Convenience method to bind from {@link JsonPointer}.  
      * {@link JsonPointerBasedFilter} is registered and will be used for parsing later. 
      * @since 2.6
      */
     public ObjectReader at(final String pointerExpr) {
         _assertNotNull("pointerExpr", pointerExpr);
         return new ObjectReader(this, new JsonPointerBasedFilter(pointerExpr));
     }
 
     /**
      * Convenience method to bind from {@link JsonPointer}
       * {@link JsonPointerBasedFilter} is registered and will be used for parsing later.
      * @since 2.6
      */
     public ObjectReader at(final JsonPointer pointer) {
         _assertNotNull("pointer", pointer);
         return new ObjectReader(this, new JsonPointerBasedFilter(pointer));
     }
 
     /**
      * Mutant factory method that will construct a new instance that has
      * specified underlying {@link DeserializationConfig}.
      *<p>
      * NOTE: use of this method is not recommended, as there are many other
      * re-configuration methods available.
      */
     public ObjectReader with(DeserializationConfig config) {
         return _with(config);
     }    
 
     /**
      * Method for constructing a new instance with configuration that uses
      * passed {@link InjectableValues} to provide injectable values.
      *<p>
      * Note that the method does NOT change state of this reader, but
      * rather construct and returns a newly configured instance.
      */
     public ObjectReader with(InjectableValues injectableValues)
     {
         if (_injectableValues == injectableValues) {
             return this;
         }
         return _new(this, _config,
                 _valueType, _rootDeserializer, _valueToUpdate,
                 _schema, injectableValues, _dataFormatReaders);
     }
 
     /**
      * Method for constructing a new reader instance with configuration that uses
      * passed {@link JsonNodeFactory} for constructing {@link JsonNode}
      * instances.
      *<p>
      * Note that the method does NOT change state of this reader, but
      * rather construct and returns a newly configured instance.
      */
     public ObjectReader with(JsonNodeFactory f) {
         return _with(_config.with(f));
     }
 
     /**
      * Method for constructing a new reader instance with configuration that uses
      * passed {@link JsonFactory} for constructing underlying Readers.
      *<p>
      * NOTE: only factories that <b>DO NOT REQUIRE SPECIAL MAPPERS</b>
      * (that is, ones that return <code>false</code> for
      * {@link JsonFactory#requiresCustomCodec()}) can be used: trying
      * to use one that requires custom codec will throw exception
      * 
      * @since 2.1
      */
     public ObjectReader with(JsonFactory f) {
         if (f == _parserFactory) {
             return this;
         }
         ObjectReader r = _new(this, f);
         // Also, try re-linking, if possible...
         if (f.getCodec() == null) {
             f.setCodec(r);
         }
         return r;
     }
     
     /**
      * Method for constructing a new instance with configuration that
      * specifies what root name to expect for "root name unwrapping".
      * See {@link DeserializationConfig#withRootName(String)} for
      * details.
      *<p>
      * Note that the method does NOT change state of this reader, but
      * rather construct and returns a newly configured instance.
      */
     public ObjectReader withRootName(String rootName) {
         return _with(_config.withRootName(rootName));
     }
 
     /**
      * @since 2.6
      */
     public ObjectReader withRootName(PropertyName rootName) {
         return _with(_config.withRootName(rootName));
     }
     
     /**
      * Convenience method that is same as calling:
      *<code>
      *   withRootName("")
      *</code>
      * which will forcibly prevent use of root name wrapping when writing
      * values with this {@link ObjectReader}.
      * 
      * @since 2.6
      */
     public ObjectReader withoutRootName() {
         return _with(_config.withRootName(PropertyName.NO_NAME));
     }
     
     /**
      * Method for constructing a new instance with configuration that
      * passes specified {@link FormatSchema} to {@link JsonParser} that
      * is constructed for parsing content.
      *<p>
      * Note that the method does NOT change state of this reader, but
      * rather construct and returns a newly configured instance.
      */
     public ObjectReader with(FormatSchema schema)
     {
         if (_schema == schema) {
             return this;
         }
         _verifySchemaType(schema);
         return _new(this, _config, _valueType, _rootDeserializer, _valueToUpdate,
                 schema, _injectableValues, _dataFormatReaders);
     }
 
     /**
      * Method for constructing a new reader instance that is configured
      * to data bind into specified type.
      *<p>
      * Note that the method does NOT change state of this reader, but
      * rather construct and returns a newly configured instance.
      * 
      * @since 2.5
      */
     public ObjectReader forType(JavaType valueType)
     {
         if (valueType != null && valueType.equals(_valueType)) {
             return this;
         }
         JsonDeserializer<Object> rootDeser = _prefetchRootDeserializer(valueType);
         // type is stored here, no need to make a copy of config
         DataFormatReaders det = _dataFormatReaders;
         if (det != null) {
             det = det.withType(valueType);
         }
         return _new(this, _config, valueType, rootDeser,
                 _valueToUpdate, _schema, _injectableValues, det);
     }    
 
     /**
      * Method for constructing a new reader instance that is configured
      * to data bind into specified type.
      *<p>
      * Note that the method does NOT change state of this reader, but
      * rather construct and returns a newly configured instance.
      *
      * @since 2.5
      */
     public ObjectReader forType(Class<?> valueType) {
         return forType(_config.constructType(valueType));
     }    
 
     /**
      * Method for constructing a new reader instance that is configured
      * to data bind into specified type.
      *<p>
      * Note that the method does NOT change state of this reader, but
      * rather construct and returns a newly configured instance.
      *
      * @since 2.5
      */
     public ObjectReader forType(TypeReference<?> valueTypeRef) {
         return forType(_config.getTypeFactory().constructType(valueTypeRef.getType()));
     }    
 
     /**
      * @deprecated since 2.5 Use {@link #forType(JavaType)} instead
      */
     @Deprecated
     public ObjectReader withType(JavaType valueType) {
         return forType(valueType);
     }
 
     /**
      * @deprecated since 2.5 Use {@link #forType(Class)} instead
      */
     @Deprecated
     public ObjectReader withType(Class<?> valueType) {
         return forType(_config.constructType(valueType));
     }    
 
     /**
      * @deprecated since 2.5 Use {@link #forType(Class)} instead
      */
     @Deprecated
     public ObjectReader withType(java.lang.reflect.Type valueType) {
         return forType(_config.getTypeFactory().constructType(valueType));
     }
 
     /**
      * @deprecated since 2.5 Use {@link #forType(TypeReference)} instead
      */
     @Deprecated
     public ObjectReader withType(TypeReference<?> valueTypeRef) {
         return forType(_config.getTypeFactory().constructType(valueTypeRef.getType()));
     }    
 
 
/** A method of constructing a new instance with a configuration that updates the passed object (as a root value), instead of constructing a new value. */

public ObjectReader withValueToUpdate(Object value) {
    return _new(this, _config, _valueType, _rootDeserializer, value, _schema, _injectableValues, _dataFormatReaders);
}
 

}