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
 
 
/** This method can be used toserialize this and all of its descendants. */
 public void serialize(JsonGenerator g, SerializerProvider provider) throws IOException{
        if (_children.size() == 0) {
            g.writeStartObject();
            g.writeEndObject();
        } else {
            g.writeStartObject();
            for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
                g.writeFieldName(entry.getKey());
                entry.getValue().serialize(g, provider);
            }
            g.writeEndObject();
        }
    }
    
        @Override
        public void serializeWithType(JsonGenerator g, SerializerProvider provider,
                TypeSerializer typeSer) throws IOException
        {
            typeSer.writeTypePrefixForObject(this, g);
            serialize(g, provider);
            typeSer.writeTypeSuffixForObject(this, g);
        }
    
        @Override
        public void serialize(JsonGenerator g, SerializerProvider provider,
                JsonSerializer<Object> serializer) throws IOException
        {
            if (_children.size() == 0) {
                serializer.serialize(null, g, provider);
            } else {
                for (Map.Entry<String, JsonNode> entry : _children.entrySet()) {
                    g.writeFieldName(entry.getKey());
                    serializer.serialize(entry.getValue(), g, provider);
                }
            }
        }
    
        /*
        /**********************************************************
        /* Public API, deserialization
        /**********************************************************
        */
    
        @Override
        public JsonNode deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonNode n = deserialize(jp, ctxt, null);
            if (n != null) {
                return n;
            }
            return MissingNode.getInstance();
        }
    
        @Override
        public JsonNode deserialize(JsonParser jp, DeserializationContext ctxt,
                JsonNode n) throws IOException, JsonProcessingException
        {
            if (        
 }

 

}