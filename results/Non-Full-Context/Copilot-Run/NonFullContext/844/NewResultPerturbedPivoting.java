package com.fasterxml.jackson.databind;
 
 import java.io.*;
 import java.text.*;
 import java.util.Locale;
 import java.util.Map;
 import java.util.TimeZone;
 import java.util.concurrent.atomic.AtomicReference;
 
 import com.fasterxml.jackson.core.*;
 import com.fasterxml.jackson.core.exc.StreamWriteException;
 import com.fasterxml.jackson.core.io.CharacterEscapes;
 import com.fasterxml.jackson.core.io.SegmentedStringWriter;
 import com.fasterxml.jackson.core.io.SerializedString;
 import com.fasterxml.jackson.core.type.TypeReference;
 import com.fasterxml.jackson.core.util.*;
 import com.fasterxml.jackson.databind.cfg.ContextAttributes;
 import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
 import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
 import com.fasterxml.jackson.databind.ser.*;
 import com.fasterxml.jackson.databind.ser.impl.TypeWrappedSerializer;
 import com.fasterxml.jackson.databind.type.TypeFactory;
 import com.fasterxml.jackson.databind.util.ClassUtil;
 
 /**
  * Builder object that can be used for per-serialization configuration of
  * serialization parameters, such as JSON View and root type to use.
  * (and thus fully thread-safe with no external synchronization);
  * new instances are constructed for different configurations.
  * Instances are initially constructed by {@link ObjectMapper} and can be
  * reused in completely thread-safe manner with no explicit synchronization
  */
 public class ObjectWriter
     implements Versioned,
         java.io.Serializable // since 2.1
 {
     private static final long serialVersionUID = 1; // since 2.5
 
     /**
      * We need to keep track of explicit disabling of pretty printing;
      * easiest to do by a token value.
      */
     protected final static PrettyPrinter NULL_PRETTY_PRINTER = new MinimalPrettyPrinter();
 
     /*
     /**********************************************************
     /* Immutable configuration from ObjectMapper
     /**********************************************************
      */
 
     /**
      * General serialization configuration settings
      */
     protected final SerializationConfig _config;
 
     protected final DefaultSerializerProvider _serializerProvider;
 
     protected final SerializerFactory _serializerFactory;
 
     /**
      * Factory used for constructing {@link JsonGenerator}s
      */
     protected final JsonFactory _generatorFactory;
 
     /*
     /**********************************************************
     /* Configuration that can be changed via mutant factories
     /**********************************************************
      */
 
     /**
      * Container for settings that need to be passed to {@link JsonGenerator}
      * constructed for serializing values.
      *
      * @since 2.5
      */
     protected final GeneratorSettings _generatorSettings;
 
     /**
      * We may pre-fetch serializer if root type
      * is known (has been explicitly declared), and if so, reuse it afterwards.
      * This allows avoiding further serializer lookups and increases
      * performance a bit on cases where readers are reused.
      *
      * @since 2.5
      */
     protected final Prefetch _prefetch;
     
     /*
     /**********************************************************
     /* Life-cycle, constructors
     /**********************************************************
      */
 
     /**
      * Constructor used by {@link ObjectMapper} for initial instantiation
      */
     protected ObjectWriter(ObjectMapper mapper, SerializationConfig config,
             JavaType rootType, PrettyPrinter pp)
     {
         _config = config;
         _serializerProvider = mapper._serializerProvider;
         _serializerFactory = mapper._serializerFactory;
         _generatorFactory = mapper._jsonFactory;
         _generatorSettings = (pp == null) ? GeneratorSettings.empty
                 : new GeneratorSettings(pp, null, null, null);
 
         if (rootType == null) {
             _prefetch = Prefetch.empty;
         } else if (rootType.hasRawClass(Object.class)) {
             // 15-Sep-2019, tatu: There is no "untyped serializer", but...
             //     as per [databind#1093] we do need `TypeSerializer`
             _prefetch = Prefetch.empty.forRootType(this, rootType);
         } else {
             _prefetch = Prefetch.empty.forRootType(this, rootType.withStaticTyping());
         }
     }
 
     /**
      * Alternative constructor for initial instantiation by {@link ObjectMapper}
      */
     protected ObjectWriter(ObjectMapper mapper, SerializationConfig config)
     {
         _config = config;
         _serializerProvider = mapper._serializerProvider;
         _serializerFactory = mapper._serializerFactory;
         _generatorFactory = mapper._jsonFactory;
 
         _generatorSettings = GeneratorSettings.empty;
         _prefetch = Prefetch.empty;
     }
 
     /**
      * Alternative constructor for initial instantiation by {@link ObjectMapper}
      */
     protected ObjectWriter(ObjectMapper mapper, SerializationConfig config,
             FormatSchema s)
     {
         _config = config;
 
         _serializerProvider = mapper._serializerProvider;
         _serializerFactory = mapper._serializerFactory;
         _generatorFactory = mapper._jsonFactory;
 
         _generatorSettings = (s == null) ? GeneratorSettings.empty
                 : new GeneratorSettings(null, s, null, null);
         _prefetch = Prefetch.empty;
     }
     
     /**
      * Copy constructor used for building variations.
      */
     protected ObjectWriter(ObjectWriter base, SerializationConfig config,
             GeneratorSettings genSettings, Prefetch prefetch)
     {
         _config = config;
 
         _serializerProvider = base._serializerProvider;
         _serializerFactory = base._serializerFactory;
         _generatorFactory = base._generatorFactory;
 
         _generatorSettings = genSettings;
         _prefetch = prefetch;
     }
 
     /**
      * Copy constructor used for building variations.
      */
     protected ObjectWriter(ObjectWriter base, SerializationConfig config)
     {
         _config = config;
 
         _serializerProvider = base._serializerProvider;
         _serializerFactory = base._serializerFactory;
         _generatorFactory = base._generatorFactory;
 
         _generatorSettings = base._generatorSettings;
         _prefetch = base._prefetch;
     }
 
     /**
      * @since 2.3
      */
     protected ObjectWriter(ObjectWriter base, JsonFactory f)
     {
         // may need to override ordering, based on data format capabilities
         _config = base._config
             .with(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, f.requiresPropertyOrdering());
 
         _serializerProvider = base._serializerProvider;
         _serializerFactory = base._serializerFactory;
         _generatorFactory = f;
 
         _generatorSettings = base._generatorSettings;
         _prefetch = base._prefetch;
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
     /**********************************************************************
     /* Internal factory methods, for convenience
     /**********************************************************************
      */
 
     /**
      * Overridable factory method called by various "withXxx()" methods
      * 
      * @since 2.5
      */
     protected ObjectWriter _new(ObjectWriter base, JsonFactory f) {
         return new ObjectWriter(base, f);
     }
 
     /**
      * Overridable factory method called by various "withXxx()" methods
      * 
      * @since 2.5
      */
     protected ObjectWriter _new(ObjectWriter base, SerializationConfig config) {
         if (config == _config) {
             return this;
         }
         return new ObjectWriter(base, config);
     }
 
     /**
      * Overridable factory method called by various "withXxx()" methods.
      * It assumes `this` as base for settings other than those directly
      * passed in.
      * 
      * @since 2.5
      */
     protected ObjectWriter _new(GeneratorSettings genSettings, Prefetch prefetch) {
         if ((_generatorSettings == genSettings) && (_prefetch == prefetch)) {
             return this;
         }
         return new ObjectWriter(this, _config, genSettings, prefetch);
     }
 
 
/** A substitutable factory method called by the {@link #writeValues(OutputStream)} method (and its various substitutions) and initializes if necessary. */

protected SequenceWriter _newSequenceWriter(boolean wrapInArray, JsonGenerator gen, boolean managedInput) throws IOException {
    if (wrapInArray) {
        gen.writeStartArray();
    }
    return new SequenceWriter(_serializerProvider(), gen, managedInput, _prefetch);
}
 

}