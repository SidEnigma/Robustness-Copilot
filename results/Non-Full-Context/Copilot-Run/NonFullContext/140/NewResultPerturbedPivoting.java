package com.fasterxml.jackson.databind.node;
 
 import java.io.IOException;
 
 import com.fasterxml.jackson.core.*;
 import com.fasterxml.jackson.databind.JsonSerializable;
 import com.fasterxml.jackson.databind.SerializerProvider;
 
 /**
  * Value node that contains a wrapped POJO, to be serialized as
  * a JSON constructed through data mapping (usually done by
  * calling {@link com.fasterxml.jackson.databind.ObjectMapper}).
  */
 public class POJONode
     extends ValueNode
 {
     private static final long serialVersionUID = 2L;
 
     protected final Object _value;
 
     public POJONode(Object v) { _value = v; }
 
     /*
     /**********************************************************
     /* Base class overrides
     /**********************************************************
      */
 
     @Override
     public JsonNodeType getNodeType() {
         return JsonNodeType.POJO;
     }
 
     @Override public JsonToken asToken() { return JsonToken.VALUE_EMBEDDED_OBJECT; }
 
 
/** As it is possible that some implementations integrate byte[] as POJONode (although the optimal is {@link BinaryNode}), let's add here also support for binary data exposure. */

public byte[] binaryValue() throws IOException {
    if (_value instanceof byte[]) {
        return (byte[]) _value;
    } else {
        throw new IOException("Cannot convert POJONode to byte[]");
    }
}
}