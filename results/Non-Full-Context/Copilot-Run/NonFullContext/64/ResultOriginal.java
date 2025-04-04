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
 
     /**
      * Method for finding all super classes (but not super interfaces) of given class,
      * starting with the immediate super class and ending in the most distant one.
      * Class itself is included if <code>addClassItself</code> is true.
      *<p>
      * NOTE: mostly/only called to resolve mix-ins as that's where we do not care
      * about fully-resolved types, just associated annotations.
      *
      * @since 2.7
      */
     public static List<Class<?>> findSuperClasses(Class<?> cls, Class<?> endBefore,
             boolean addClassItself) {
         List<Class<?>> result = new ArrayList<Class<?>>(8);
         if ((cls != null) && (cls != endBefore))  {
             if (addClassItself) {
                 result.add(cls);
             }
             while ((cls = cls.getSuperclass()) != null) {
                 if (cls == endBefore) {
                     break;
                 }
                 result.add(cls);
             }
         }
         return result;
     }
 
     @Deprecated // since 2.7
     public static List<Class<?>> findSuperTypes(Class<?> cls, Class<?> endBefore) {
         return findSuperTypes(cls, endBefore, new ArrayList<Class<?>>(8));
     }
 
     @Deprecated // since 2.7
     public static List<Class<?>> findSuperTypes(Class<?> cls, Class<?> endBefore, List<Class<?>> result) {
         _addRawSuperTypes(cls, endBefore, result, false);
         return result;
     }
 
     private static void _addSuperTypes(JavaType type, Class<?> endBefore, Collection<JavaType> result,
             boolean addClassItself)
     {
         if (type == null) {
             return;
         }
         final Class<?> cls = type.getRawClass();
         if (cls == endBefore || cls == Object.class) { return; }
         if (addClassItself) {
             if (result.contains(type)) { // already added, no need to check supers
                 return;
             }
             result.add(type);
         }
         for (JavaType intCls : type.getInterfaces()) {
             _addSuperTypes(intCls, endBefore, result, true);
         }
         _addSuperTypes(type.getSuperClass(), endBefore, result, true);
     }
 
     private static void _addRawSuperTypes(Class<?> cls, Class<?> endBefore, Collection<Class<?>> result, boolean addClassItself) {
         if (cls == endBefore || cls == null || cls == Object.class) { return; }
         if (addClassItself) {
             if (result.contains(cls)) { // already added, no need to check supers
                 return;
             }
             result.add(cls);
         }
         for (Class<?> intCls : _interfaces(cls)) {
             _addRawSuperTypes(intCls, endBefore, result, true);
         }
         _addRawSuperTypes(cls.getSuperclass(), endBefore, result, true);
     }
 
     /*
     /**********************************************************
     /* Class type detection methods
     /**********************************************************
      */
 
     /**
      * @return Null if class might be a bean; type String (that identifies
      *   why it's not a bean) if not
      */
     public static String canBeABeanType(Class<?> type)
     {
         // First: language constructs that ain't beans:
         if (type.isAnnotation()) {
             return "annotation";
         }
         if (type.isArray()) {
             return "array";
         }
         if (Enum.class.isAssignableFrom(type)) {
             return "enum";
         }
         if (type.isPrimitive()) {
             return "primitive";
         }
 
         // Anything else? Seems valid, then
         return null;
     }
     
     public static String isLocalType(Class<?> type, boolean allowNonStatic)
     {
         /* As per [JACKSON-187], GAE seems to throw SecurityExceptions
          * here and there... and GAE itself has a bug, too
          * Bah. So we need to catch some wayward exceptions on GAE
          */
         try {
             final boolean isStatic = Modifier.isStatic(type.getModifiers());
 
             // one more: method locals, anonymous, are not good:
             // 23-Jun-2020, tatu: [databind#2758] With JDK14+ should allow
             //    local Record types, however
             if (!isStatic && hasEnclosingMethod(type)) {
                 return "local/anonymous";
             }
             /* But how about non-static inner classes? Can't construct
              * easily (theoretically, we could try to check if parent
              * happens to be enclosing... but that gets convoluted)
              */
             if (!allowNonStatic) {
                 if (!isStatic && getEnclosingClass(type) != null) {
                     return "non-static member class";
                 }
             }
         }
         catch (SecurityException e) { }
         catch (NullPointerException e) { }
         return null;
     }
 
     /**
      * Method for finding enclosing class for non-static inner classes
      */
     public static Class<?> getOuterClass(Class<?> type)
     {
         // as above, GAE has some issues...
         if (!Modifier.isStatic(type.getModifiers())) {
             try {
                 // one more: method locals, anonymous, are not good:
                 if (hasEnclosingMethod(type)) {
                     return null;
                 }
                 return getEnclosingClass(type);
             } catch (SecurityException e) { }
         }
         return null;
     }
 
 
/** Helper method used to weed out dynamic Proxy types; types that do  not expose concrete method API that we could use to figure out  automatic Bean (property) based serialization. */
 public static boolean isProxyType(Class<?> type){
        if (type == null) {
            return false;
        }
        if (type.isInterface()) {
            return false;
        }
        if (type.isArray()) {
            return false;
        }
        if (type.isAnnotation()) {
            return false;
        }
        if (type.isEnum()) {
            return false;
        }
        if (type.isPrimitive()) {
            return false;
        }
        if (type.isMemberClass()) {
            return false;
        }
        if (type.isLocalClass()) {
            return false;
        }
        if (type.isAnonymousClass()) {
            return false;
        }
        if (type.isSynthetic()) {
            return false;
        }
        if (type.isMemberClass()) {
            return false;
        }
        if (type.isLocalClass()) {
            return false;
        }
        if (type.isAnonymousClass()) {
            return false;
        }
        if (type.isSynthetic()) {
            return false;
        }
        if (type.isMemberClass()) {
            return false;
        }
        if (type.isLocalClass()) {
            return false;
        }
        if (type.isAnonymousClass()) {
            return false;
        }
        if (type.isSynthetic()) {
            return false;
        }
        if (type.isMemberClass()) {
            return false;
        }
        if (type.isLocalClass()) {
            return false;
        }
        if (type.isAnonymousClass()) {
            return false;
        }
        if (type.isSynthetic()) {
            return false;
        }
        if (type.isMemberClass()) {
            return false;
        }
        if (type.isLocalClass()) {
            return false;
        }
        if (type.isAnonymousClass()) {
            return false;
        }
        if (type.isSynthetic()) {
            return false;
        }
        if (type        
 }

 

}