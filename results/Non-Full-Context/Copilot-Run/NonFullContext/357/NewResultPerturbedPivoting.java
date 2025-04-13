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
 
     /**
      * Helper method used to weed out dynamic Proxy types; types that do
      * not expose concrete method API that we could use to figure out
      * automatic Bean (property) based serialization.
      */
     public static boolean isProxyType(Class<?> type)
     {
         // As per [databind#57], should NOT disqualify JDK proxy:
         /*
         // Then: well-known proxy (etc) classes
         if (Proxy.isProxyClass(type)) {
             return true;
         }
         */
         String name = type.getName();
         // Hibernate uses proxies heavily as well:
         if (name.startsWith("net.sf.cglib.proxy.")
             || name.startsWith("org.hibernate.proxy.")) {
             return true;
         }
         // Not one of known proxies, nope:
         return false;
     }
 
     /**
      * Helper method that checks if given class is a concrete one;
      * that is, not an interface or abstract class.
      */
     public static boolean isConcrete(Class<?> type)
     {
         int mod = type.getModifiers();
         return (mod & (Modifier.INTERFACE | Modifier.ABSTRACT)) == 0;
     }
 
     public static boolean isConcrete(Member member)
     {
         int mod = member.getModifiers();
         return (mod & (Modifier.INTERFACE | Modifier.ABSTRACT)) == 0;
     }
     
     public static boolean isCollectionMapOrArray(Class<?> type)
     {
         if (type.isArray()) return true;
         if (Collection.class.isAssignableFrom(type)) return true;
         if (Map.class.isAssignableFrom(type)) return true;
         return false;
     }
 
     public static boolean isBogusClass(Class<?> cls) {
         return (cls == Void.class || cls == Void.TYPE
                 || cls == com.fasterxml.jackson.databind.annotation.NoClass.class);
     }
 
     /**
      * Helper method for detecting Java14-added new {@code Record} types
      *
      * @since 2.12
      */
     public static boolean isRecordType(Class<?> cls) {
         Class<?> parent = cls.getSuperclass();
         return (parent != null) && "java.lang.Record".equals(parent.getName());
     }
 
     /**
      * @since 2.7
      */
     public static boolean isObjectOrPrimitive(Class<?> cls) {
         return (cls == CLS_OBJECT) || cls.isPrimitive();
     }
 
     /**
      * @since 2.9
      */
     public static boolean hasClass(Object inst, Class<?> raw) {
         // 10-Nov-2016, tatu: Could use `Class.isInstance()` if we didn't care
         //    about being exactly that type
         return (inst != null) && (inst.getClass() == raw);
     }
 
     /**
      * @since 2.9
      */
     public static void verifyMustOverride(Class<?> expType, Object instance,
             String method)
     {
         if (instance.getClass() != expType) {
             throw new IllegalStateException(String.format(
                     "Sub-class %s (of class %s) must override method '%s'",
                 instance.getClass().getName(), expType.getName(), method));
         }
     }
 
     /*
     /**********************************************************
     /* Method type detection methods
     /**********************************************************
      */
 
     /**
      * @deprecated Since 2.6 not used; may be removed before 3.x
      */
     @Deprecated // since 2.6
     public static boolean hasGetterSignature(Method m)
     {
         // First: static methods can't be getters
         if (Modifier.isStatic(m.getModifiers())) {
             return false;
         }
         // Must take no args
         Class<?>[] pts = m.getParameterTypes();
         if (pts != null && pts.length != 0) {
             return false;
         }
         // Can't be a void method
         if (Void.TYPE == m.getReturnType()) {
             return false;
         }
         // Otherwise looks ok:
         return true;
     }
 
     /*
     /**********************************************************
     /* Exception handling; simple re-throw
     /**********************************************************
      */
 
     /**
      * Helper method that will check if argument is an {@link Error},
      * and if so, (re)throw it; otherwise just return
      *
      * @since 2.9
      */
     public static Throwable throwIfError(Throwable t) {
         if (t instanceof Error) {
             throw (Error) t;
         }
         return t;
     }
 
     /**
      * Helper method that will check if argument is an {@link RuntimeException},
      * and if so, (re)throw it; otherwise just return
      *
      * @since 2.9
      */
     public static Throwable throwIfRTE(Throwable t) {
         if (t instanceof RuntimeException) {
             throw (RuntimeException) t;
         }
         return t;
     }
 
     /**
      * Helper method that will check if argument is an {@link IOException},
      * and if so, (re)throw it; otherwise just return
      *
      * @since 2.9
      */
     public static Throwable throwIfIOE(Throwable t) throws IOException {
         if (t instanceof IOException) {
             throw (IOException) t;
         }
         return t;
     }
 
     /*
     /**********************************************************
     /* Exception handling; other
     /**********************************************************
      */
     
     /**
      * Method that can be used to find the "root cause", innermost
      * of chained (wrapped) exceptions.
      */
     public static Throwable getRootCause(Throwable t)
     {
         while (t.getCause() != null) {
             t = t.getCause();
         }
         return t;
     }
 
     /**
      * Method that works like by calling {@link #getRootCause} and then
      * either throwing it (if instanceof {@link IOException}), or
      * return.
      *
      * @since 2.8
      */
     public static Throwable throwRootCauseIfIOE(Throwable t) throws IOException {
         return throwIfIOE(getRootCause(t));
     }
 
     /**
      * Method that will wrap 't' as an {@link IllegalArgumentException} if it
      * is a checked exception; otherwise (runtime exception or error) throw as is
      */
     public static void throwAsIAE(Throwable t) {
         throwAsIAE(t, t.getMessage());
     }
 
     /**
      * Method that will wrap 't' as an {@link IllegalArgumentException} (and with
      * specified message) if it
      * is a checked exception; otherwise (runtime exception or error) throw as is
      */
     public static void throwAsIAE(Throwable t, String msg)
     {
         throwIfRTE(t);
         throwIfError(t);
         throw new IllegalArgumentException(msg, t);
     }
 
     /**
      * @since 2.9
      */
     public static <T> T throwAsMappingException(DeserializationContext ctxt,
             IOException e0) throws JsonMappingException {
         if (e0 instanceof JsonMappingException) {
             throw (JsonMappingException) e0;
         }
         throw JsonMappingException.from(ctxt, e0.getMessage())
             .withCause(e0);
     }
 
     /**
      * Method that will locate the innermost exception for given Throwable;
      * and then wrap it as an {@link IllegalArgumentException} if it
      * is a checked exception; otherwise (runtime exception or error) throw as is
      */
     public static void unwrapAndThrowAsIAE(Throwable t)
     {
         throwAsIAE(getRootCause(t));
     }
 
     /**
      * Method that will locate the innermost exception for given Throwable;
      * and then wrap it as an {@link IllegalArgumentException} if it
      * is a checked exception; otherwise (runtime exception or error) throw as is
      */
     public static void unwrapAndThrowAsIAE(Throwable t, String msg)
     {
         throwAsIAE(getRootCause(t), msg);
     }
 
 
/** Assistive method that encapsulates the logic by trying to close the output generator in case of failure; useful mainly for forcing rinsing, because otherwise error conditions tend to be difficult to diagnose. */

public static void closeOnFailAndThrowAsIOE(JsonGenerator g, Exception fail) throws IOException {
    try {
        g.close();
    } catch (IOException e) {
        // Ignore the exception from closing the generator
    }
    throwIfIOE(fail);
}
 

}