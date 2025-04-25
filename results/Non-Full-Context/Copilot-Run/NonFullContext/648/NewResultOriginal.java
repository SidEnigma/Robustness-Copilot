/**
  * Jooby https://jooby.io
  * Apache License Version 2.0 https://jooby.io/LICENSE.txt
  * Copyright 2014 Edgar Espina
  */
 package io.jooby;
 
 import io.jooby.exception.MissingValueException;
 import io.jooby.exception.TypeMismatchException;
 import io.jooby.internal.ArrayValue;
 import io.jooby.internal.HashValue;
 import io.jooby.internal.MissingValue;
 import io.jooby.internal.SingleValue;
 
 import java.util.TreeMap;
 import javax.annotation.Nonnull;
 import javax.annotation.Nullable;
 import java.time.Instant;
 import java.time.LocalDateTime;
 import java.time.ZoneOffset;
 import java.time.format.DateTimeParseException;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Optional;
 import java.util.Set;
 import java.util.function.Function;
 
 /**
  * Unified API for HTTP value. This API plays two role:
  *
  * - unify access to HTTP values, like query, path, form and header parameter
  * - works as validation API, because it is able to check for required and type-safe values
  *
  * The value API is composed by three types:
  *
  * - Single value
  * - Object value
  * - Sequence of values (array)
  *
  * Single value are can be converted to string, int, boolean, enum like values.
  * Object value is a key:value structure (like a hash).
  * Sequence of values are index based structure.
  *
  * All these 3 types are modeled into a single Value class. At any time you can treat a value as
  * 1) single, 2) hash or 3) array of them.
  *
  * @since 2.0.0
  * @author edgar
  */
 public interface Value {
   /**
    * Convert this value to long (if possible).
    *
    * @return Long value.
    */
   default long longValue() {
     try {
       return Long.parseLong(value());
     } catch (NumberFormatException x) {
       try {
         LocalDateTime date = LocalDateTime.parse(value(), Context.RFC1123);
         Instant instant = date.toInstant(ZoneOffset.UTC);
         return instant.toEpochMilli();
       } catch (DateTimeParseException expected) {
       }
       throw new TypeMismatchException(name(), long.class, x);
     }
   }
 
   /**
    * Convert this value to long (if possible) or fallback to given value when missing.
    *
    * @param defaultValue Default value.
    * @return Convert this value to long (if possible) or fallback to given value when missing.
    */
   default long longValue(long defaultValue) {
     try {
       return longValue();
     } catch (MissingValueException x) {
       return defaultValue;
     }
   }
 
 
/** Convert this value to int (if possible). */

  default int intValue() {
    try {
      return Integer.parseInt(value());
    } catch (NumberFormatException x) {
      try {
        LocalDateTime date = LocalDateTime.parse(value(), Context.RFC1123);
        Instant instant = date.toInstant(ZoneOffset.UTC);
        return (int) instant.toEpochMilli();
      } catch (DateTimeParseException expected) {
      }
      throw new TypeMismatchException(name(), int.class, x);
    }
  }
 

}