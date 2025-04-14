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
 public static TextNode valueOf(String v){
        if(v == null) return null;
        if(v.length() == 0) return EMPTY_STRING_NODE;
        return new TextNode(v);
    }
    
        public TextNode(char[] buf, int start, int len) {
            _value = new String(buf, start, len);
        }
    
        public TextNode(char[] buf) {
            _value = new String(buf);
        }
    
        public TextNode(char c) {
            _value = String.valueOf(c);
        }
    
        public TextNode(int i) {
            _value = String.valueOf(i);
        }
    
        public TextNode(long l) {
            _value = String.valueOf(l);
        }
    
        public TextNode(double d) {
            _value = String.valueOf(d);
        }
    
        public TextNode(boolean b) {
            _value = String.valueOf(b);
        }
    
        public TextNode(BigInteger v) {
            _value = v.toString();
        }
    
        public TextNode(BigDecimal v) {
            _value = v.toString();
        }
    
        public TextNode(byte[] v) {
            _value = Base64Variants.getDefaultVariant().encode(v, false);
        }
    
        public TextNode(byte[] v, int offset, int len) {
            _value = Base64Variants.getDefaultVariant().encode(v, offset, len, false);
        }
    
        public TextNode(byte[] v, Base64Variant bv) {
            _value = bv.encode(v, false);
        }
    
        public TextNode(byte[] v, int offset, int len, Base64Variant bv) {
            _value = bv.encode(v, offset, len, false);
        }
    
        public TextNode(byte[] v, int offset, int len, Base64Variant bv, boolean wrap) {
            _value =        
 }

 

}