package com.fasterxml.jackson.databind.node;
 
 import java.io.*;
 import java.math.BigDecimal;
 import java.math.BigInteger;
 import java.util.*;
 
 import com.fasterxml.jackson.core.*;
 import com.fasterxml.jackson.core.type.WritableTypeId;
 import com.fasterxml.jackson.databind.*;
 import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
 import com.fasterxml.jackson.databind.util.RawValue;
 
 /**
  * Node that maps to JSON Object structures in JSON content.
  *<p>
  * Note: class was <code>final</code> temporarily for Jackson 2.2.
  */
 public class ObjectNode
     extends ContainerNode<ObjectNode>
     implements java.io.Serializable
 {
     private static final long serialVersionUID = 1L; // since 2.10
 
     // Note: LinkedHashMap for backwards compatibility
     protected final Map<String, JsonNode> _children;
 
     public ObjectNode(JsonNodeFactory nc) {
         super(nc);
         _children = new LinkedHashMap<String, JsonNode>();
     }
 
     /**
      * @since 2.4
      */
     public ObjectNode(JsonNodeFactory nc, Map<String, JsonNode> kids) {
         super(nc);
         _children = kids;
     }
 
     @Override
     protected JsonNode _at(JsonPointer ptr) {
         return get(ptr.getMatchingProperty());
     }
 
     /* Question: should this delegate to `JsonNodeFactory`? It does not absolutely
      * have to, as long as sub-types override the method but...
      */
     // note: co-variant for type safety
     @SuppressWarnings("unchecked")
     @Override
     public ObjectNode deepCopy()
     {
         ObjectNode ret = new ObjectNode(_nodeFactory);
 
         for (Map.Entry<String, JsonNode> entry: _children.entrySet())
             ret._children.put(entry.getKey(), entry.getValue().deepCopy());
 
         return ret;
     }
 
     /*
     /**********************************************************
     /* Overrides for JsonSerializable.Base
     /**********************************************************
      */
 
     @Override
     public boolean isEmpty(SerializerProvider serializers) {
         return _children.isEmpty();
     }
 
     /*
     /**********************************************************
     /* Implementation of core JsonNode API
     /**********************************************************
      */
 
     @Override
     public JsonNodeType getNodeType() {
         return JsonNodeType.OBJECT;
     }
 
     @Override
     public final boolean isObject() {
         return true;
     }
     
     @Override public JsonToken asToken() { return JsonToken.START_OBJECT; }
 
     @Override
     public int size() {
         return _children.size();
     }
 
     @Override // since 2.10
     public boolean isEmpty() { return _children.isEmpty(); }
     
     @Override
     public Iterator<JsonNode> elements() {
         return _children.values().iterator();
     }
 
     @Override
     public JsonNode get(int index) { return null; }
 
     @Override
     public JsonNode get(String propertyName) {
         return _children.get(propertyName);
     }
 
     @Override
     public Iterator<String> fieldNames() {
         return _children.keySet().iterator();
     }
 
     @Override
     public JsonNode path(int index) {
         return MissingNode.getInstance();
     }
 
     @Override
     public JsonNode path(String propertyName)
     {
         JsonNode n = _children.get(propertyName);
         if (n != null) {
             return n;
         }
         return MissingNode.getInstance();
     }
 
     @Override
     public JsonNode required(String propertyName) {
         JsonNode n = _children.get(propertyName);
         if (n != null) {
             return n;
         }
         return _reportRequiredViolation("No value for property '%s' of `ObjectNode`", propertyName);
     }
 
     /**
      * Method to use for accessing all properties (with both names
      * and values) of this JSON Object.
      */
     @Override
     public Iterator<Map.Entry<String, JsonNode>> fields() {
         return _children.entrySet().iterator();
     }
 
     @SuppressWarnings("unchecked")
     @Override
     public ObjectNode with(String propertyName) {
         JsonNode n = _children.get(propertyName);
         if (n != null) {
             if (n instanceof ObjectNode) {
                 return (ObjectNode) n;
             }
             throw new UnsupportedOperationException("Property '" + propertyName
                 + "' has value that is not of type ObjectNode (but " + n
                 .getClass().getName() + ")");
         }
         ObjectNode result = objectNode();
         _children.put(propertyName, result);
         return result;
     }
 
     @SuppressWarnings("unchecked")
     @Override
     public ArrayNode withArray(String propertyName)
     {
         JsonNode n = _children.get(propertyName);
         if (n != null) {
             if (n instanceof ArrayNode) {
                 return (ArrayNode) n;
             }
             throw new UnsupportedOperationException("Property '" + propertyName
                 + "' has value that is not of type ArrayNode (but " + n
                 .getClass().getName() + ")");
         }
         ArrayNode result = arrayNode();
         _children.put(propertyName, result);
         return result;
     }
 
     @Override
     public boolean equals(Comparator<JsonNode> comparator, JsonNode o)
     {
         if (!(o instanceof ObjectNode)) {
             return false;
         }
         ObjectNode other = (ObjectNode) o;
         Map<String, JsonNode> m1 = _children;
         Map<String, JsonNode> m2 = other._children;
 
         final int len = m1.size();
         if (m2.size() != len) {
             return false;
         }
 
         for (Map.Entry<String, JsonNode> entry : m1.entrySet()) {
             JsonNode v2 = m2.get(entry.getKey());
             if ((v2 == null) || !entry.getValue().equals(comparator, v2)) {
                 return false;
             }
         }
         return true;
     }
 
     /*
     /**********************************************************
     /* Public API, finding value nodes
     /**********************************************************
      */
     
     @Override
     public JsonNode findValue(String propertyName)
     {
         for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
             if (propertyName.equals(entry.getKey())) {
                 return entry.getValue();
             }
             JsonNode value = entry.getValue().findValue(propertyName);
             if (value != null) {
                 return value;
             }
         }
         return null;
     }
     
     @Override
     public List<JsonNode> findValues(String propertyName, List<JsonNode> foundSoFar)
     {
         for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
             if (propertyName.equals(entry.getKey())) {
                 if (foundSoFar == null) {
                     foundSoFar = new ArrayList<JsonNode>();
                 }
                 foundSoFar.add(entry.getValue());
             } else { // only add children if parent not added
                 foundSoFar = entry.getValue().findValues(propertyName, foundSoFar);
             }
         }
         return foundSoFar;
     }
 
     @Override
     public List<String> findValuesAsText(String propertyName, List<String> foundSoFar)
     {
         for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
             if (propertyName.equals(entry.getKey())) {
                 if (foundSoFar == null) {
                     foundSoFar = new ArrayList<String>();
                 }
                 foundSoFar.add(entry.getValue().asText());
             } else { // only add children if parent not added
                 foundSoFar = entry.getValue().findValuesAsText(propertyName,
                     foundSoFar);
             }
         }
         return foundSoFar;
     }
     
     @Override
     public ObjectNode findParent(String propertyName)
     {
         for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
             if (propertyName.equals(entry.getKey())) {
                 return this;
             }
             JsonNode value = entry.getValue().findParent(propertyName);
             if (value != null) {
                 return (ObjectNode) value;
             }
         }
         return null;
     }
 
     @Override
     public List<JsonNode> findParents(String propertyName, List<JsonNode> foundSoFar)
     {
         for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
             if (propertyName.equals(entry.getKey())) {
                 if (foundSoFar == null) {
                     foundSoFar = new ArrayList<JsonNode>();
                 }
                 foundSoFar.add(this);
             } else { // only add children if parent not added
                 foundSoFar = entry.getValue()
                     .findParents(propertyName, foundSoFar);
             }
         }
         return foundSoFar;
     }
 
     /*
     /**********************************************************
     /* Public API, serialization
     /**********************************************************
      */
 
     /**
      * Method that can be called to serialize this node and
      * all of its descendants using specified JSON generator.
      */
     @Override
     public void serialize(JsonGenerator g, SerializerProvider provider)
         throws IOException
     {
         @SuppressWarnings("deprecation")
         boolean trimEmptyArray = (provider != null) &&
                 !provider.isEnabled(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
         g.writeStartObject(this);
         for (Map.Entry<String, JsonNode> en : _children.entrySet()) {
             /* 17-Feb-2009, tatu: Can we trust that all nodes will always
              *   extend BaseJsonNode? Or if not, at least implement
              *   JsonSerializable? Let's start with former, change if
              *   we must.
              */
             BaseJsonNode value = (BaseJsonNode) en.getValue();
 
             // as per [databind#867], see if WRITE_EMPTY_JSON_ARRAYS feature is disabled,
             // if the feature is disabled, then should not write an empty array
             // to the output, so continue to the next element in the iteration
             if (trimEmptyArray && value.isArray() && value.isEmpty(provider)) {
             	continue;
             }
             g.writeFieldName(en.getKey());
             value.serialize(g, provider);
         }
         g.writeEndObject();
     }
 
     @Override
     public void serializeWithType(JsonGenerator g, SerializerProvider provider,
             TypeSerializer typeSer)
         throws IOException
     {
         @SuppressWarnings("deprecation")
         boolean trimEmptyArray = (provider != null) &&
                 !provider.isEnabled(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
 
         WritableTypeId typeIdDef = typeSer.writeTypePrefix(g,
                 typeSer.typeId(this, JsonToken.START_OBJECT));
         for (Map.Entry<String, JsonNode> en : _children.entrySet()) {
             BaseJsonNode value = (BaseJsonNode) en.getValue();
 
             // check if WRITE_EMPTY_JSON_ARRAYS feature is disabled,
             // if the feature is disabled, then should not write an empty array
             // to the output, so continue to the next element in the iteration
             if (trimEmptyArray && value.isArray() && value.isEmpty(provider)) {
                 continue;
             }
             
             g.writeFieldName(en.getKey());
             value.serialize(g, provider);
         }
         typeSer.writeTypeSuffix(g, typeIdDef);
     }
 
     /*
     /**********************************************************
     /* Extended ObjectNode API, mutators, since 2.1
     /**********************************************************
      */
 
     /**
      * Method that will set specified property, replacing old value, if any.
      * Note that this is identical to {@link #replace(String, JsonNode)},
      * except for return value.
      *<p>
      * NOTE: added to replace those uses of {@link #put(String, JsonNode)}
      * where chaining with 'this' is desired.
      *<p>
      * NOTE: co-variant return type since 2.10
      *
      * @param propertyName Name of property to set
      * @param value Value to set property to; if null, will be converted
      *   to a {@link NullNode} first  (to remove a property, call
      *   {@link #remove} instead)
      *
      * @return This node after adding/replacing property value (to allow chaining)
      *
      * @since 2.1
      */
     @SuppressWarnings("unchecked")
     public <T extends JsonNode> T set(String propertyName, JsonNode value)
     {
         if (value == null) {
             value = nullNode();
         }
         _children.put(propertyName, value);
         return (T) this;
     }
 
     /**
      * Method for adding given properties to this object node, overriding
      * any existing values for those properties.
      *<p>
      * NOTE: co-variant return type since 2.10
      * 
      * @param properties Properties to add
      * 
      * @return This node after adding/replacing property values (to allow chaining)
      *
      * @since 2.1
      */
     @SuppressWarnings("unchecked")
     public <T extends JsonNode> T setAll(Map<String,? extends JsonNode> properties)
     {
         for (Map.Entry<String,? extends JsonNode> en : properties.entrySet()) {
             JsonNode n = en.getValue();
             if (n == null) {
                 n = nullNode();
             }
             _children.put(en.getKey(), n);
         }
         return (T) this;
     }
 
     /**
      * Method for adding all properties of the given Object, overriding
      * any existing values for those properties.
      *<p>
      * NOTE: co-variant return type since 2.10
      * 
      * @param other Object of which properties to add to this object
      *
      * @return This node after addition (to allow chaining)
      *
      * @since 2.1
      */
     @SuppressWarnings("unchecked")
     public <T extends JsonNode> T setAll(ObjectNode other)
     {
         _children.putAll(other._children);
         return (T) this;
     }
 
     /**
      * Method for replacing value of specific property with passed
      * value, and returning value (or null if none).
      *
      * @param propertyName Property of which value to replace
      * @param value Value to set property to, replacing old value if any
      * 
      * @return Old value of the property; null if there was no such property
      *   with value
      * 
      * @since 2.1
      */
     public JsonNode replace(String propertyName, JsonNode value)
     {
         if (value == null) { // let's not store 'raw' nulls but nodes
             value = nullNode();
         }
         return _children.put(propertyName, value);
     }
 
     /**
      * Method for removing property from this ObjectNode, and
      * returning instance after removal.
      *<p>
      * NOTE: co-variant return type since 2.10
      * 
      * @return This node after removing property (if any)
      * 
      * @since 2.1
      */
     @SuppressWarnings("unchecked")
     public <T extends JsonNode> T without(String propertyName)
     {
         _children.remove(propertyName);
         return (T) this;
     }
 
     /**
      * Method for removing specified field properties out of
      * this ObjectNode.
      *<p>
      * NOTE: co-variant return type since 2.10
      * 
      * @param propertyNames Names of properties to remove
      * 
      * @return This node after removing entries
      * 
      * @since 2.1
      */
     @SuppressWarnings("unchecked")
     public <T extends JsonNode> T without(Collection<String> propertyNames)
     {
         _children.keySet().removeAll(propertyNames);
         return (T) this;
     }
     
     /*
     /**********************************************************
     /* Extended ObjectNode API, mutators, generic
     /**********************************************************
      */
 
     /**
      * Method that will set specified property, replacing old value, if any.
      *
      * @param propertyName Name of property to set
      * @param value Value to set to property; if null, will be converted
      *   to a {@link NullNode} first  (to remove a property, call
      *   {@link #remove} instead).
      *   
      * @return Old value of the property, if any; {@code null} if there was no
      *   old value.
      *   
      * @deprecated Since 2.4 use either {@link #set(String,JsonNode)} or {@link #replace(String,JsonNode)},
      */
     @Deprecated
     public JsonNode put(String propertyName, JsonNode value)
     {
         if (value == null) { // let's not store 'raw' nulls but nodes
             value = nullNode();
         }
         return _children.put(propertyName, value);
     }
 
 
/** Method that will set value of specified property if (and only if)  it had no set value previously. */
 public JsonNode putIfAbsent(String propertyName, JsonNode value){}

 

}