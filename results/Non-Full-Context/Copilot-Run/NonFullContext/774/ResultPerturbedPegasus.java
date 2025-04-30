package com.fasterxml.jackson.databind.util;
 
 import java.io.Closeable;
 import java.io.IOException;
 import java.lang.annotation.Annotation;
 import java.lang.reflect.*;
 import java.util.*;
 
 import com.fasterxml.jackson.core.JacksonException;
 import com.fasterxml.jackson.core.JsonGenerator;
 
 import com.fasterxml.jackson.databind.DeserializationContext;
 import com.fasterxml.jackson.databind.JavaType;
 import com.fasterxml.jackson.databind.JsonMappingException;
 import com.fasterxml.jackson.databind.PropertyName;
 import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
 
 public final class ClassUtil
 {
     private final static Class<?> CLS_OBJECT = Object.class;
 
     private final static Annotation[] NO_ANNOTATIONS = new Annotation[0];
     private final static Ctor[] NO_CTORS = new Ctor[0];
 
     private final static Iterator<?> EMPTY_ITERATOR = Collections.emptyIterator();
 
     /*
     /**********************************************************
     /* Simple factory methods
     /**********************************************************
      */
 
     /**
      * @since 2.7
      */
     @SuppressWarnings("unchecked")
     public static <T> Iterator<T> emptyIterator() {
         return (Iterator<T>) EMPTY_ITERATOR;
     }
 
     /*
     /**********************************************************
     /* Methods that deal with inheritance
     /**********************************************************
      */
 
     /**
      * Method that will find all sub-classes and implemented interfaces
      * of a given class or interface. Classes are listed in order of
      * precedence, starting with the immediate super-class, followed by
      * interfaces class directly declares to implemented, and then recursively
      * followed by parent of super-class and so forth.
      * Note that <code>Object.class</code> is not included in the list
      * regardless of whether <code>endBefore</code> argument is defined or not.
      *
      * @param endBefore Super-type to NOT include in results, if any; when
      *    encountered, will be ignored (and no super types are checked).
      *
      * @since 2.7
      */
     public static List<JavaType> findSuperTypes(JavaType type, Class<?> endBefore,
             boolean addClassItself) {
         if ((type == null) || type.hasRawClass(endBefore) || type.hasRawClass(Object.class)) {
             return Collections.emptyList();
         }
         List<JavaType> result = new ArrayList<JavaType>(8);
         _addSuperTypes(type, endBefore, result, addClassItself);
         return result;
     }
 
     /**
      * @since 2.7
      */
     public static List<Class<?>> findRawSuperTypes(Class<?> cls, Class<?> endBefore, boolean addClassItself) {
         if ((cls == null) || (cls == endBefore) || (cls == Object.class)) {
             return Collections.emptyList();
         }
         List<Class<?>> result = new ArrayList<Class<?>>(8);
         _addRawSuperTypes(cls, endBefore, result, addClassItself);
         return result;
     }
 
 
/** The method for finding all super classes in a class is starting with the immediate super class and ending in the most distant one. */
 public static List<Class<?>> findSuperClasses(Class<?> cls, Class<?> endBefore, boolean addClassItself){
        if ((cls == null) || (cls == endBefore) || (cls == Object.class)) {
            return Collections.emptyList();
        }
        List<Class<?>> result = new ArrayList<Class<?>>(8);
        _addSuperClasses(cls, endBefore, result, addClassItself);
        return result;
    }
    
        private static void _addSuperTypes(JavaType type, Class<?> endBefore, List<JavaType> result,
                boolean addClassItself) {
            Class<?> raw = type.getRawClass();
            if (addClassItself) {
                result.add(type);
            }
            if (raw.getSuperclass() != null) {
                _addSuperTypes(type.getSuperType(), endBefore, result, true);
            }
            for (JavaType intf : type.getInterfaces()) {
                _addSuperTypes(intf, endBefore, result, true);
            }
        }
    
        private static void _addRawSuperTypes(Class<?> cls, Class<?> endBefore, List<Class<?>> result,
                boolean addClassItself) {
            if (addClassItself) {
                result.add(cls);
            }
            if (cls.getSuperclass() != null) {
                _addRawSuperTypes(cls.getSuperclass(), endBefore, result, true);
            }
            for (Class<?> intf : cls.getInterfaces()) {
                _addRawSuperTypes(intf, endBefore, result, true);
            }
        }
    
        private static void _addSuperClasses(Class<?> cls, Class<?> endBefore, List<Class<?>> result,
                boolean addClassItself) {
            if (addClassItself) {
                result.add(cls);
            }
            if (cls.getSuperclass() != null) {
                _addSuperClasses(cls.getSuperclass(), endBefore, result, true);
            }
        }
    
        /*
        /**********************************************************
        /* Methods      
 }

 

}