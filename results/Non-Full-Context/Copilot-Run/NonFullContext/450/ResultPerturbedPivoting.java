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
     
 
/** Factory method to build a simple transformer based on a prefix and/or suffix. */
 public static NameTransformer simpleTransformer(final String prefix, final String suffix){
        return new NameTransformer() {
            @Override
            public String transform(String name) {
                return (prefix == null) ? name : (prefix + name);
            }
            @Override
            public String reverse(String transformed) {
                if (prefix == null) {
                    return transformed;
                }
                if (transformed.startsWith(prefix)) {
                    return transformed.substring(prefix.length());
                }
                return transformed;
            }
        };
    }
    
        /**
        * Method called to transform a name to be used for serialization.
        * 
        * @param name Name to transform
        * @return Transformed name
        */
        public abstract String transform(String name);
    
        /**
        * Method called to reverse a name that was transformed using
        * {@link #transform(String)}.
        * 
        * @param transformed Name that was transformed
        * @return Original name
        */
        public abstract String reverse(String transformed);     
 }

 

}