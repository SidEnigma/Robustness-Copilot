package com.fasterxml.jackson.databind.util;
 
 /**
  * Helper class used to encapsulate details of name mangling, transforming
  * of names using different strategies (prefixes, suffixes).
  * Default implementation is "no-operation" (aka identity transformation).
  */
 public abstract class NameTransformer
 {
     /**
      * Singleton "no-operation" transformer which simply returns given
      * name as is. Used commonly as placeholder or marker.
      */
     public final static NameTransformer NOP = new NopTransformer();
     
     protected final static class NopTransformer
         extends NameTransformer
         implements java.io.Serializable
     {
         private static final long serialVersionUID = 1L;
 
         @Override
         public String transform(String name) {
             return name;
         }
         @Override
         public String reverse(String transformed) {
             // identity transformation is always reversible:
             return transformed;
         }
     }
 
     protected NameTransformer() { }
     
 
/** Factory method for constructing a simple transformer based on  prefix and/or suffix. */

public static NameTransformer simpleTransformer(final String prefix, final String suffix) {
    return new NameTransformer() {
        private static final long serialVersionUID = 1L;

        @Override
        public String transform(String name) {
            return prefix + name + suffix;
        }

        @Override
        public String reverse(String transformed) {
            if (prefix != null && transformed.startsWith(prefix)) {
                transformed = transformed.substring(prefix.length());
            }
            if (suffix != null && transformed.endsWith(suffix)) {
                transformed = transformed.substring(0, transformed.length() - suffix.length());
            }
            return transformed;
        }
    };
}
 

}