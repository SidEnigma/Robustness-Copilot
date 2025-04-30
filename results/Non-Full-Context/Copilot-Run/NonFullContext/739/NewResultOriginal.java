package com.fasterxml.jackson.databind.introspect;
 
 import java.lang.reflect.Modifier;
 import java.util.*;
 
 import com.fasterxml.jackson.annotation.JacksonInject;
 import com.fasterxml.jackson.annotation.JsonCreator;
 
 import com.fasterxml.jackson.databind.*;
 import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
 import com.fasterxml.jackson.databind.cfg.MapperConfig;
 import com.fasterxml.jackson.databind.util.ClassUtil;
 
 /**
  * Helper class used for aggregating information about all possible
  * properties of a POJO.
  */
 public class POJOPropertiesCollector
 {
     /*
     /**********************************************************
     /* Configuration
     /**********************************************************
      */
 
     /**
      * Configuration settings
      */
     protected final MapperConfig<?> _config;
 
     /**
      * Handler used for name-mangling of getter, mutator (setter/with) methods
      *
      * @since 2.12
      */
     protected final AccessorNamingStrategy _accessorNaming;
 
     /**
      * True if introspection is done for serialization (giving
      * precedence for serialization annotations), or not (false, deserialization)
      */
     protected final boolean _forSerialization;
 
     /**
      * Type of POJO for which properties are being collected.
      */
     protected final JavaType _type;
 
     /**
      * Low-level introspected class information (methods, fields etc)
      */
     protected final AnnotatedClass _classDef;
 
     protected final VisibilityChecker<?> _visibilityChecker;
 
     protected final AnnotationIntrospector _annotationIntrospector;
 
     /**
      * @since 2.9
      */
     protected final boolean _useAnnotations;
 
     /*
     /**********************************************************
     /* Collected property information
     /**********************************************************
      */
 
     /**
      * State flag we keep to indicate whether actual property information
      * has been collected or not.
      */
     protected boolean _collected;
     
     /**
      * Set of logical property information collected so far.
      *<p>
      * Since 2.6, this has been constructed (more) lazily, to defer
      * throwing of exceptions for potential conflicts in cases where
      * this may not be an actual problem.
      */
     protected LinkedHashMap<String, POJOPropertyBuilder> _properties;
 
     protected LinkedList<POJOPropertyBuilder> _creatorProperties;
 
     /**
      * A set of "field renamings" that have been discovered, indicating
      * intended renaming of other accesors: key is the implicit original
      * name and value intended name to use instead.
      *<p>
      * Note that these renamings are applied earlier than "regular" (explicit)
      * renamings and affect implicit name: their effect may be changed by
      * further renaming based on explicit indicators.
      * The main use case is to effectively relink accessors based on fields
      * discovered, and used to sort of correct otherwise missing linkage between
      * fields and other accessors.
      *
      * @since 2.11
      */
     protected Map<PropertyName, PropertyName> _fieldRenameMappings;
     
     protected LinkedList<AnnotatedMember> _anyGetters;
 
     /**
      * @since 2.12
      */
     protected LinkedList<AnnotatedMember> _anyGetterField;
 
     protected LinkedList<AnnotatedMethod> _anySetters;
     
     protected LinkedList<AnnotatedMember> _anySetterField;
 
     /**
      * Accessors (field or "getter" method annotated with
      * {@link com.fasterxml.jackson.annotation.JsonKey}
      *
      * @since 2.12
      */
     protected LinkedList<AnnotatedMember> _jsonKeyAccessors;
 
     /**
      * Accessors (field or "getter" method) annotated with
      * {@link com.fasterxml.jackson.annotation.JsonValue}
      */
     protected LinkedList<AnnotatedMember> _jsonValueAccessors;
 
     /**
      * Lazily collected list of properties that can be implicitly
      * ignored during serialization; only updated when collecting
      * information for deserialization purposes
      */
     protected HashSet<String> _ignoredPropertyNames;
 
     /**
      * Lazily collected list of members that were annotated to
      * indicate that they represent mutators for deserializer
      * value injection.
      */
     protected LinkedHashMap<Object, AnnotatedMember> _injectables;
 
     // // // Deprecated entries to remove from 3.0
     
     /**
      * @deprecated Since 2.12
      */
     @Deprecated
     protected final boolean _stdBeanNaming;
 
     /**
      * @deprecated Since 2.12
      */
     @Deprecated
     protected String _mutatorPrefix = "set";
 
     /*
     /**********************************************************
     /* Life-cycle
     /**********************************************************
      */
 
     /**
      * @since 2.12
      */
     protected POJOPropertiesCollector(MapperConfig<?> config, boolean forSerialization,
             JavaType type, AnnotatedClass classDef,
             AccessorNamingStrategy accessorNaming)
     {
         _config = config;
         _forSerialization = forSerialization;
         _type = type;
         _classDef = classDef;
         if (config.isAnnotationProcessingEnabled()) {
             _useAnnotations = true;
             _annotationIntrospector = _config.getAnnotationIntrospector();
         } else {
             _useAnnotations = false;
             _annotationIntrospector = AnnotationIntrospector.nopInstance();
         }
         _visibilityChecker = _config.getDefaultVisibilityChecker(type.getRawClass(),
                 classDef);
         _accessorNaming = accessorNaming;
 
         // for backwards-compatibility only
         _stdBeanNaming = config.isEnabled(MapperFeature.USE_STD_BEAN_NAMING);
     }
 
     /**
      * @deprecated Since 2.12
      */
     @Deprecated
     protected POJOPropertiesCollector(MapperConfig<?> config, boolean forSerialization,
             JavaType type, AnnotatedClass classDef,
             String mutatorPrefix)
     {
         this(config, forSerialization, type, classDef,
                 _accessorNaming(config, classDef, mutatorPrefix));
         _mutatorPrefix = mutatorPrefix;
     }
 
     private static AccessorNamingStrategy _accessorNaming(MapperConfig<?> config, AnnotatedClass classDef,
             String mutatorPrefix) {
         if (mutatorPrefix == null) {
             mutatorPrefix = "set";
         }
         return new DefaultAccessorNamingStrategy.Provider()
                 .withSetterPrefix(mutatorPrefix).forPOJO(config, classDef);
     }
 
     /*
     /**********************************************************
     /* Public API
     /**********************************************************
      */
 
     public MapperConfig<?> getConfig() {
         return _config;
     }
 
     public JavaType getType() {
         return _type;
     }
     
     public AnnotatedClass getClassDef() {
         return _classDef;
     }
 
     public AnnotationIntrospector getAnnotationIntrospector() {
         return _annotationIntrospector;
     }
     
     public List<BeanPropertyDefinition> getProperties() {
         // make sure we return a copy, so caller can remove entries if need be:
         Map<String, POJOPropertyBuilder> props = getPropertyMap();
         return new ArrayList<BeanPropertyDefinition>(props.values());
     }
 
     public Map<Object, AnnotatedMember> getInjectables() {
         if (!_collected) {
             collectAll();
         }
         return _injectables;
     }
 
     /**
      * @since 2.12
      */
     public AnnotatedMember getJsonKeyAccessor() {
         if (!_collected) {
             collectAll();
         }
         // If @JsonKey defined, must have a single one
         if (_jsonKeyAccessors != null) {
             if (_jsonKeyAccessors.size() > 1) {
                 reportProblem("Multiple 'as-key' properties defined (%s vs %s)",
                         _jsonKeyAccessors.get(0),
                         _jsonKeyAccessors.get(1));
             }
             // otherwise we won't greatly care
             return _jsonKeyAccessors.get(0);
         }
         return null;
     }
 
     /**
      * @since 2.9
      */
     public AnnotatedMember getJsonValueAccessor()
     {
         if (!_collected) {
             collectAll();
         }
         // If @JsonValue defined, must have a single one
         if (_jsonValueAccessors != null) {
             if (_jsonValueAccessors.size() > 1) {
                 reportProblem("Multiple 'as-value' properties defined (%s vs %s)",
                         _jsonValueAccessors.get(0),
                         _jsonValueAccessors.get(1));
             }
             // otherwise we won't greatly care
             return _jsonValueAccessors.get(0);
         }
         return null;
     }
 
     /**
      * Alias for {@link #getAnyGetterMethod()}.
      *
      * @deprecated Since 2.12 use separate {@link #getAnyGetterMethod()} and
      *     {@link #getAnyGetterField()}.
      */
     @Deprecated // since 2.12
     public AnnotatedMember getAnyGetter() {
         return getAnyGetterMethod();
     }
 
     /**
      * @since 2.12 (before only had "getAnyGetter()")
      */
     public AnnotatedMember getAnyGetterField()
     {
         if (!_collected) {
             collectAll();
         }
         if (_anyGetterField != null) {
             if (_anyGetterField.size() > 1) {
                 reportProblem("Multiple 'any-getter' fields defined (%s vs %s)",
                         _anyGetterField.get(0), _anyGetterField.get(1));
             }
             return _anyGetterField.getFirst();
         }
         return null;
     }
 
     /**
      * @since 2.12 (before only had "getAnyGetter()")
      */
     public AnnotatedMember getAnyGetterMethod()
     {
         if (!_collected) {
             collectAll();
         }
         if (_anyGetters != null) {
             if (_anyGetters.size() > 1) {
                 reportProblem("Multiple 'any-getter' methods defined (%s vs %s)",
                         _anyGetters.get(0), _anyGetters.get(1));
             }
             return _anyGetters.getFirst();
         }
         return null;
     }
 
     public AnnotatedMember getAnySetterField()
     {
         if (!_collected) {
             collectAll();
         }
         if (_anySetterField != null) {
             if (_anySetterField.size() > 1) {
                 reportProblem("Multiple 'any-setter' fields defined (%s vs %s)",
                         _anySetterField.get(0), _anySetterField.get(1));
             }
             return _anySetterField.getFirst();
         }
         return null;
     }
 
     public AnnotatedMethod getAnySetterMethod()
     {
         if (!_collected) {
             collectAll();
         }
         if (_anySetters != null) {
             if (_anySetters.size() > 1) {
                 reportProblem("Multiple 'any-setter' methods defined (%s vs %s)",
                         _anySetters.get(0), _anySetters.get(1));
             }
             return _anySetters.getFirst();
         }
         return null;
     }
 
     /**
      * Accessor for set of properties that are explicitly marked to be ignored
      * via per-property markers (but NOT class annotations).
      */
     public Set<String> getIgnoredPropertyNames() {
         return _ignoredPropertyNames;
     }
 
     /**
      * Accessor to find out whether type specified requires inclusion
      * of Object Identifier.
      */
     public ObjectIdInfo getObjectIdInfo()
     {
         ObjectIdInfo info = _annotationIntrospector.findObjectIdInfo(_classDef);
         if (info != null) { // 2.1: may also have different defaults for refs:
             info = _annotationIntrospector.findObjectReferenceInfo(_classDef, info);
         }
         return info;
     }
 
     // for unit tests:
     protected Map<String, POJOPropertyBuilder> getPropertyMap() {
         if (!_collected) {
             collectAll();
         }
         return _properties;
     }
 
     @Deprecated // since 2.9
     public AnnotatedMethod getJsonValueMethod() {
         AnnotatedMember m = getJsonValueAccessor();
         if (m instanceof AnnotatedMethod) {
             return (AnnotatedMethod) m;
         }
         return null;
     }
 
     @Deprecated // since 2.11 (not used by anything at this point)
     public Class<?> findPOJOBuilderClass() {
         return _annotationIntrospector.findPOJOBuilder(_classDef);
     }
 
     /*
     /**********************************************************
     /* Public API: main-level collection
     /**********************************************************
      */
 
     /**
      * Internal method that will collect actual property information.
      *
      * @since 2.6
      */
     protected void collectAll()
     {
         LinkedHashMap<String, POJOPropertyBuilder> props = new LinkedHashMap<String, POJOPropertyBuilder>();
 
         // First: gather basic data
         _addFields(props); // note: populates _fieldRenameMappings
         _addMethods(props);
         // 25-Jan-2016, tatu: Avoid introspecting (constructor-)creators for non-static
         //    inner classes, see [databind#1502]
         if (!_classDef.isNonStaticInnerClass()) {
             _addCreators(props);
         }
 
         // Remove ignored properties, first; this MUST precede annotation merging
         // since logic relies on knowing exactly which accessor has which annotation
         _removeUnwantedProperties(props);
         // and then remove unneeded accessors (wrt read-only, read-write)
         _removeUnwantedAccessor(props);
 
         // Rename remaining properties
         _renameProperties(props);
 
         // and now add injectables, but taking care to avoid overlapping ones
         // via creator and regular properties
         _addInjectables(props);
 
         // then merge annotations, to simplify further processing
         // 26-Sep-2017, tatu: Before 2.9.2 was done earlier but that prevented some of
         //   annotations from getting properly merged
         for (POJOPropertyBuilder property : props.values()) {
             property.mergeAnnotations(_forSerialization);
         }
 
         // Sort by visibility (explicit over implicit); drop all but first of member
         // type (getter, setter etc) if there is visibility difference
         for (POJOPropertyBuilder property : props.values()) {
             property.trimByVisibility();
         }
 
         // And use custom naming strategy, if applicable...
         // As per [databind#2979], should be AFTER trimming
         PropertyNamingStrategy naming = _findNamingStrategy();
         if (naming != null) {
             _renameUsing(props, naming);
         }
 
         // and, if required, apply wrapper name: note, MUST be done after
         // annotations are merged.
         if (_config.isEnabled(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME)) {
             _renameWithWrappers(props);
         }
 
         // well, almost last: there's still ordering...
         _sortProperties(props);
         _properties = props;
         _collected = true;
     }
 
     /*
     /**********************************************************
     /* Overridable internal methods, adding members
     /**********************************************************
      */
     
     /**
      * Method for collecting basic information on all fields found
      */
     protected void _addFields(Map<String, POJOPropertyBuilder> props)
     {
         final AnnotationIntrospector ai = _annotationIntrospector;
         /* 28-Mar-2013, tatu: For deserialization we may also want to remove
          *   final fields, as often they won't make very good mutators...
          *   (although, maybe surprisingly, JVM _can_ force setting of such fields!)
          */
         final boolean pruneFinalFields = !_forSerialization && !_config.isEnabled(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS);
         final boolean transientAsIgnoral = _config.isEnabled(MapperFeature.PROPAGATE_TRANSIENT_MARKER);
 
         for (AnnotatedField f : _classDef.fields()) {
             // @JsonKey?
             if (Boolean.TRUE.equals(ai.hasAsKey(_config, f))) {
                 if (_jsonKeyAccessors == null) {
                     _jsonKeyAccessors = new LinkedList<>();
                 }
                 _jsonKeyAccessors.add(f);
             }
             // @JsonValue?
             if (Boolean.TRUE.equals(ai.hasAsValue(f))) {
                 if (_jsonValueAccessors == null) {
                     _jsonValueAccessors = new LinkedList<>();
                 }
                 _jsonValueAccessors.add(f);
                 continue;
             }
             // 12-October-2020, dominikrebhan: [databind#1458] Support @JsonAnyGetter on
             //   fields and allow @JsonAnySetter to be declared as well.
             boolean anyGetter = Boolean.TRUE.equals(ai.hasAnyGetter(f));
             boolean anySetter = Boolean.TRUE.equals(ai.hasAnySetter(f));
             if (anyGetter || anySetter) {
                 // @JsonAnyGetter?
                 if (anyGetter) {
                     if (_anyGetterField == null) {
                         _anyGetterField = new LinkedList<>();
                     }
                     _anyGetterField.add(f);
                 }
                 // @JsonAnySetter?
                 if (anySetter) {
                     if (_anySetterField == null) {
                         _anySetterField = new LinkedList<>();
                     }
                     _anySetterField.add(f);
                 }
                 continue;
             }
             String implName = ai.findImplicitPropertyName(f);
             if (implName == null) {
                 implName = f.getName();
             }
             // 27-Aug-2020, tatu: [databind#2800] apply naming strategy for
             //   fields too, to allow use of naming conventions.
             implName = _accessorNaming.modifyFieldName(f, implName);
             if (implName == null) {
                 continue;
             }
 
             final PropertyName implNameP = _propNameFromSimple(implName);
             // [databind#2527: Field-based renaming can be applied early (here),
             // or at a later point, but probably must be done before pruning
             // final fields. So let's do it early here
             final PropertyName rename = ai.findRenameByField(_config, f, implNameP);
             if ((rename != null) && !rename.equals(implNameP)) {
                 if (_fieldRenameMappings == null) {
                     _fieldRenameMappings = new HashMap<>();
                 }
                 _fieldRenameMappings.put(rename, implNameP);
             }
 
             PropertyName pn;
 
             if (_forSerialization) {
                 // 18-Aug-2011, tatu: As per existing unit tests, we should only
                 //   use serialization annotation (@JsonSerialize) when serializing
                 //   fields, and similarly for deserialize-only annotations... so
                 //   no fallbacks in this particular case.
                 pn = ai.findNameForSerialization(f);
             } else {
                 pn = ai.findNameForDeserialization(f);
             }
             boolean hasName = (pn != null);
             boolean nameExplicit = hasName;
 
             if (nameExplicit && pn.isEmpty()) { // empty String meaning "use default name", here just means "same as field name"
                 pn = _propNameFromSimple(implName);
                 nameExplicit = false;
             }
             // having explicit name means that field is visible; otherwise need to check the rules
             boolean visible = (pn != null);
             if (!visible) {
                 visible = _visibilityChecker.isFieldVisible(f);
             }
             // and finally, may also have explicit ignoral
             boolean ignored = ai.hasIgnoreMarker(f);
 
             // 13-May-2015, tatu: Moved from earlier place (AnnotatedClass) in 2.6
             if (f.isTransient()) {
                 // 20-May-2016, tatu: as per [databind#1184] explicit annotation should override
                 //    "default" `transient`
                 if (!hasName) {
                     visible = false;
                     if (transientAsIgnoral) {
                         ignored = true;
                     }
                 }
             }
             /* [databind#190]: this is the place to prune final fields, if they are not
              *  to be used as mutators. Must verify they are not explicitly included.
              *  Also: if 'ignored' is set, need to include until a later point, to
              *  avoid losing ignoral information.
              */
             if (pruneFinalFields && (pn == null) && !ignored
                     && Modifier.isFinal(f.getModifiers())) {
                 continue;
             }
             _property(props, implName).addField(f, pn, nameExplicit, visible, ignored);
         }
     }
 
     /**
      * Method for collecting basic information on constructor(s) found
      */
     protected void _addCreators(Map<String, POJOPropertyBuilder> props)
     {
         // can be null if annotation processing is disabled...
         if (!_useAnnotations) {
             return;
         }
         for (AnnotatedConstructor ctor : _classDef.getConstructors()) {
             if (_creatorProperties == null) {
                 _creatorProperties = new LinkedList<POJOPropertyBuilder>();
             }
             for (int i = 0, len = ctor.getParameterCount(); i < len; ++i) {
                 _addCreatorParam(props, ctor.getParameter(i));
             }
         }
         for (AnnotatedMethod factory : _classDef.getFactoryMethods()) {
             if (_creatorProperties == null) {
                 _creatorProperties = new LinkedList<POJOPropertyBuilder>();
             }
             for (int i = 0, len = factory.getParameterCount(); i < len; ++i) {
                 _addCreatorParam(props, factory.getParameter(i));
             }
         }
     }
 
     /**
      * @since 2.4
      */
     protected void _addCreatorParam(Map<String, POJOPropertyBuilder> props,
             AnnotatedParameter param)
     {
         // JDK 8, paranamer, Scala can give implicit name
         String impl = _annotationIntrospector.findImplicitPropertyName(param);
         if (impl == null) {
             impl = "";
         }
         PropertyName pn = _annotationIntrospector.findNameForDeserialization(param);
         boolean expl = (pn != null && !pn.isEmpty());
         if (!expl) {
             if (impl.isEmpty()) {
                 // Important: if neither implicit nor explicit name, cannot make use of
                 // this creator parameter -- may or may not be a problem, verified at a later point.
                 return;
             }
             // Also: if this occurs, there MUST be explicit annotation on creator itself
             JsonCreator.Mode creatorMode = _annotationIntrospector.findCreatorAnnotation(_config,
                     param.getOwner());
             if ((creatorMode == null) || (creatorMode == JsonCreator.Mode.DISABLED)) {
                 return;
             }
             pn = PropertyName.construct(impl);
         }
 
         // 27-Dec-2019, tatu: [databind#2527] may need to rename according to field
         impl = _checkRenameByField(impl);
 
         // shouldn't need to worry about @JsonIgnore, since creators only added
         // if so annotated
         
         /* 13-May-2015, tatu: We should try to start with implicit name, similar to how
          *   fields and methods work; but unlike those, we don't necessarily have
          *   implicit name to use (pre-Java8 at least). So:
          */
         POJOPropertyBuilder prop = (expl && impl.isEmpty())
                 ? _property(props, pn) : _property(props, impl);
         prop.addCtor(param, pn, expl, true, false);
         _creatorProperties.add(prop);
     }
 
     /**
      * Method for collecting basic information on all fields found
      */
     protected void _addMethods(Map<String, POJOPropertyBuilder> props)
     {
         for (AnnotatedMethod m : _classDef.memberMethods()) {
             // For methods, handling differs between getters and setters; and
             // we will also only consider entries that either follow the bean
             // naming convention or are explicitly marked: just being visible
             // is not enough (unlike with fields)
 
             int argCount = m.getParameterCount();
             if (argCount == 0) { // getters (including 'any getter')
                 _addGetterMethod(props, m, _annotationIntrospector);
             } else if (argCount == 1) { // setters
                 _addSetterMethod(props, m, _annotationIntrospector);
             } else if (argCount == 2) { // any getter?
                 if (Boolean.TRUE.equals(_annotationIntrospector.hasAnySetter(m))) {
                     if (_anySetters == null) {
                         _anySetters = new LinkedList<AnnotatedMethod>();
                     }
                     _anySetters.add(m);
                 }
             }
         }
     }
 
     protected void _addGetterMethod(Map<String, POJOPropertyBuilder> props,
             AnnotatedMethod m, AnnotationIntrospector ai)
     {
         // Very first thing: skip if not returning any value
         // 06-May-2020, tatu: [databind#2675] changes handling slightly...
         {
             final Class<?> rt = m.getRawReturnType();
             if ((rt == Void.TYPE) ||
                     ((rt == Void.class) && !_config.isEnabled(MapperFeature.ALLOW_VOID_VALUED_PROPERTIES))) {
                 return;
             }
         }
 
         // any getter?
         // @JsonAnyGetter?
         if (Boolean.TRUE.equals(ai.hasAnyGetter(m))) {
             if (_anyGetters == null) {
                 _anyGetters = new LinkedList<AnnotatedMember>();
             }
             _anyGetters.add(m);
             return;
         }
         // @JsonKey?
         if (Boolean.TRUE.equals(ai.hasAsKey(_config, m))) {
             if (_jsonKeyAccessors == null) {
                 _jsonKeyAccessors = new LinkedList<>();
             }
             _jsonKeyAccessors.add(m);
             return;
         }
         // @JsonValue?
         if (Boolean.TRUE.equals(ai.hasAsValue(m))) {
             if (_jsonValueAccessors == null) {
                 _jsonValueAccessors = new LinkedList<>();
             }
             _jsonValueAccessors.add(m);
             return;
         }
         String implName; // from naming convention
         boolean visible;
 
         PropertyName pn = ai.findNameForSerialization(m);
         boolean nameExplicit = (pn != null);
 
         if (!nameExplicit) { // no explicit name; must consider implicit
             implName = ai.findImplicitPropertyName(m);
             if (implName == null) {
                 implName = _accessorNaming.findNameForRegularGetter(m, m.getName());
             }
             if (implName == null) { // if not, must skip
                 implName = _accessorNaming.findNameForIsGetter(m, m.getName());
                 if (implName == null) {
                     return;
                 }
                 visible = _visibilityChecker.isIsGetterVisible(m);
             } else {
                 visible = _visibilityChecker.isGetterVisible(m);
             }
         } else { // explicit indication of inclusion, but may be empty
             // we still need implicit name to link with other pieces
             implName = ai.findImplicitPropertyName(m);
             if (implName == null) {
                 implName = _accessorNaming.findNameForRegularGetter(m, m.getName());
                 if (implName == null) {
                     implName = _accessorNaming.findNameForIsGetter(m, m.getName());
                 }
             }
             // if not regular getter name, use method name as is
             if (implName == null) {
                 implName = m.getName();
             }
             if (pn.isEmpty()) {
                 // !!! TODO: use PropertyName for implicit names too
                 pn = _propNameFromSimple(implName);
                 nameExplicit = false;
             }
             visible = true;
         }
         // 27-Dec-2019, tatu: [databind#2527] may need to rename according to field
         implName = _checkRenameByField(implName);
         boolean ignore = ai.hasIgnoreMarker(m);
         _property(props, implName).addGetter(m, pn, nameExplicit, visible, ignore);
     }
 
     protected void _addSetterMethod(Map<String, POJOPropertyBuilder> props,
             AnnotatedMethod m, AnnotationIntrospector ai)
     {
         String implName; // from naming convention
         boolean visible;
         PropertyName pn = ai.findNameForDeserialization(m);
         boolean nameExplicit = (pn != null);
         if (!nameExplicit) { // no explicit name; must follow naming convention
             implName = ai.findImplicitPropertyName(m);
             if (implName == null) {
                 implName = _accessorNaming.findNameForMutator(m, m.getName());
             }
             if (implName == null) { // if not, must skip
             	return;
             }
             visible = _visibilityChecker.isSetterVisible(m);
         } else { // explicit indication of inclusion, but may be empty
             // we still need implicit name to link with other pieces
             implName = ai.findImplicitPropertyName(m);
             if (implName == null) {
                 implName = _accessorNaming.findNameForMutator(m, m.getName());
             }
             // if not regular getter name, use method name as is
             if (implName == null) {
                 implName = m.getName();
             }
             if (pn.isEmpty()) {
                 // !!! TODO: use PropertyName for implicit names too
                 pn = _propNameFromSimple(implName);
                 nameExplicit = false;
             }
             visible = true;
         }
         // 27-Dec-2019, tatu: [databind#2527] may need to rename according to field
         implName = _checkRenameByField(implName);
         final boolean ignore = ai.hasIgnoreMarker(m);
         _property(props, implName)
             .addSetter(m, pn, nameExplicit, visible, ignore);
     }
 
     protected void _addInjectables(Map<String, POJOPropertyBuilder> props)
     {
         // first fields, then methods, to allow overriding
         for (AnnotatedField f : _classDef.fields()) {
             _doAddInjectable(_annotationIntrospector.findInjectableValue(f), f);
         }
         
         for (AnnotatedMethod m : _classDef.memberMethods()) {
             // for now, only allow injection of a single arg (to be changed in future?)
             if (m.getParameterCount() != 1) {
                 continue;
             }
             _doAddInjectable(_annotationIntrospector.findInjectableValue(m), m);
         }
     }
 
     protected void _doAddInjectable(JacksonInject.Value injectable, AnnotatedMember m)
     {
         if (injectable == null) {
             return;
         }
         Object id = injectable.getId();
         if (_injectables == null) {
             _injectables = new LinkedHashMap<Object, AnnotatedMember>();
         }
         AnnotatedMember prev = _injectables.put(id, m);
         if (prev != null) {
             // 12-Apr-2017, tatu: Let's allow masking of Field by Method
             if (prev.getClass() == m.getClass()) {
                 String type = id.getClass().getName();
                 throw new IllegalArgumentException("Duplicate injectable value with id '"
                         + id +"' (of type "+type+")");
             }
         }
     }
 
     private PropertyName _propNameFromSimple(String simpleName) {
         return PropertyName.construct(simpleName, null);
     }
 
     // @since 2.11
     private String _checkRenameByField(String implName) {
         if (_fieldRenameMappings != null) {
             PropertyName p = _fieldRenameMappings.get(_propNameFromSimple(implName));
             if (p != null) {
                 implName = p.getSimpleName();
                 return implName;
 
             }
         }
         return implName;
     }
 
     /*
     /**********************************************************
     /* Internal methods; removing ignored properties
     /**********************************************************
      */
 
     /**
      * Method called to get rid of candidate properties that are marked
      * as ignored.
      */
     protected void _removeUnwantedProperties(Map<String, POJOPropertyBuilder> props)
     {
         Iterator<POJOPropertyBuilder> it = props.values().iterator();
         while (it.hasNext()) {
             POJOPropertyBuilder prop = it.next();
 
             // First: if nothing visible, just remove altogether
             if (!prop.anyVisible()) {
                 it.remove();
                 continue;
             }
             // Otherwise, check ignorals
             if (prop.anyIgnorals()) {
                 // first: if one or more ignorals, and no explicit markers, remove the whole thing
                 if (!prop.isExplicitlyIncluded()) {
                     it.remove();
                     _collectIgnorals(prop.getName());
                     continue;
                 }
                 // otherwise just remove ones marked to be ignored
                 prop.removeIgnored();
                 if (!prop.couldDeserialize()) {
                     _collectIgnorals(prop.getName());
                 }
             }
         }
     }
 
     /**
      * Method called to further get rid of unwanted individual accessors,
      * based on read/write settings and rules for "pulling in" accessors
      * (or not).
      */
     protected void _removeUnwantedAccessor(Map<String, POJOPropertyBuilder> props)
     {
         final boolean inferMutators = _config.isEnabled(MapperFeature.INFER_PROPERTY_MUTATORS);
         Iterator<POJOPropertyBuilder> it = props.values().iterator();
 
         while (it.hasNext()) {
             POJOPropertyBuilder prop = it.next();
             // 26-Jan-2017, tatu: [databind#935]: need to denote removal of
             // 16-May-2020, tatu: [databind#2719]: need to pass `this` to allow
             //    addition of ignorals wrt explicit name
             prop.removeNonVisible(inferMutators, _forSerialization ? null : this);
         }
     }
 
 
/** Helper method called to add explicitly ignored properties to a list  of known ignored properties; this helps in proper reporting of  errors. */

protected void _collectIgnorals(String name) {
    // Add the ignored property name to a list of known ignored properties
    // This helps in proper reporting of errors
    // Add your implementation logic here
}
 

}