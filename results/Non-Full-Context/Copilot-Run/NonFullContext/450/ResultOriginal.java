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
 public static NameTransformer simpleTransformer(final String prefix, final String suffix){
        if (prefix == null && suffix == null) {
            return NOP;
        }
        return new SimpleTransformer(prefix, suffix);
    }
    
        protected abstract String transform(String name);
        protected abstract String reverse(String transformed);
    
        /**
        * Simple implementation of name transformer that adds prefix and/or suffix
        * to given name.
        */
        protected final static class SimpleTransformer
            extends NameTransformer
            implements java.io.Serializable
        {
            private static final long serialVersionUID = 1L;
    
            private final String _prefix;
            private final String _suffix;
    
            public SimpleTransformer(String prefix, String suffix) {
                _prefix = prefix;
                _suffix = suffix;
            }
    
            @Override
            public String transform(String name) {
                if (_prefix == null) {
                    return _suffix == null ? name : name + _suffix;
                }
                if (_suffix == null) {
                    return _prefix + name;
                }
                return _prefix + name + _suffix;
            }
    
            @Override
            public String reverse(String transformed) {
                if (_prefix == null) {
                    if (_suffix == null) {
                        return transformed;
                    }
                    if (transformed.endsWith(_suffix)) {
                        return transformed.substring(0, transformed.length() - _suffix.length());
                    }
                    return transformed;
                }
                if (_suffix == null) {
                    if (transformed.startsWith(_prefix)) {
                        return transformed.substring(_prefix.length());
                    }
                    return transformed;
                }
                if (transformed.startsWith(_prefix) && transformed.endsWith(_suffix)) {
                    return transformed.substring(_prefix.length(), transformed.length() - _suffix.length());
                }
                return transformed;
            }
        }       
 }

 

}