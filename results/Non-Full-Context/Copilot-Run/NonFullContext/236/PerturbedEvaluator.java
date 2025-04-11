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
 
 
/** Method for creating instances of {@link MappingIterator} custom subtype or either default */
 protected MappingIterator<T> _newIterator(JsonParser p, DeserializationContext ctxt, JsonDeserializer<?> deser, boolean parserManaged){}

 

}