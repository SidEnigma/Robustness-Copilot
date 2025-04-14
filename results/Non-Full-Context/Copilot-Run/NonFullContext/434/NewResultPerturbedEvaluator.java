package com.fasterxml.jackson.databind.node;
 
 import java.io.IOException;
 
 import com.fasterxml.jackson.core.*;
 import com.fasterxml.jackson.core.io.CharTypes;
 import com.fasterxml.jackson.core.io.NumberInput;
 import com.fasterxml.jackson.core.util.ByteArrayBuilder;
 
 import com.fasterxml.jackson.databind.SerializerProvider;
 import com.fasterxml.jackson.databind.exc.InvalidFormatException;
 
 /**
  * Value node that contains a text value.
  */
 public class TextNode
     extends ValueNode
 {
     private static final long serialVersionUID = 2L;
 
     final static TextNode EMPTY_STRING_NODE = new TextNode("");
 
     protected final String _value;
 
     public TextNode(String v) { _value = v; }
 
 
/** This factory method is used to create instances, using canonical instances for common cases (only Strings at the moment). If we pass null, it returns null. */

public static TextNode valueOf(String v) {
    if (v == null) {
        return null;
    }
    if (v.isEmpty()) {
        return EMPTY_STRING_NODE;
    }
    return new TextNode(v);
}
 

}