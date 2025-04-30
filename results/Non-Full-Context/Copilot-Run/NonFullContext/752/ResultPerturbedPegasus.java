/*
  * The MIT License (MIT)
  *
  * Copyright (c) 2017-2020 Yegor Bugayenko
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included
  * in all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */
 package org.cactoos.scalar;
 
 import java.util.Comparator;
 import java.util.Iterator;
 import java.util.Map;
 import org.cactoos.Fallback;
 import org.cactoos.Scalar;
 import org.cactoos.func.FuncWithFallback;
 import org.cactoos.iterable.IterableOf;
 import org.cactoos.iterator.Filtered;
 import org.cactoos.iterator.Sorted;
 import org.cactoos.map.MapOf;
 
 /**
  * Scalar with fallbacks that enable it to recover from errors.
  *
  * <p>There is no thread-safety guarantee.
  *
  * @param <T> Type of result
  * @see FuncWithFallback
  * @since 0.31
  * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
  */
 public final class ScalarWithFallback<T> implements Scalar<T> {
 
     /**
      * The origin scalar.
      */
     private final Scalar<T> origin;
 
     /**
      * The fallback.
      */
     private final Iterable<Fallback<T>> fallbacks;
 
     /**
      * Ctor.
      * @param origin Original scalar
      * @param fbks The fallbacks
      */
     @SafeVarargs
     public ScalarWithFallback(
         final Scalar<T> origin,
         final Fallback<T>... fbks
     ) {
         this(origin, new IterableOf<>(fbks));
     }
 
     /**
      * Ctor.
      * @param origin Original scalar
      * @param fbks Fallbacks
      */
     public ScalarWithFallback(final Scalar<T> origin,
         final Iterable<Fallback<T>> fbks) {
         this.origin = origin;
         this.fallbacks = fbks;
     }
 
     @Override
     @SuppressWarnings("PMD.AvoidCatchingThrowable")
     public T value() throws Exception {
         T result;
         try {
             result = this.origin.value();
         } catch (final InterruptedException ex) {
             Thread.currentThread().interrupt();
             result = this.fallback(ex);
             // @checkstyle IllegalCatchCheck (1 line)
         } catch (final Throwable ex) {
             result = this.fallback(ex);
         }
         return result;
     }
 
 
/** If no fallback is found, apply it to the exception or throw the original error. */
 private T fallback(final Throwable exp) throws Exception{
        T result = null;
        for (final Fallback<T> fallback : this.fallbacks) {
            try {
                result = fallback.value();
                break;
            } catch (final Throwable ex) {
                // @checkstyle IllegalCatchCheck (1 line)
            }
        }
        if (result == null) {
            throw exp;
        }
        return result;
    }
    
        @Override
        public String toString() {
            return new MapOf<>(
                "origin", this.origin,
                "fallbacks", this.fallbacks
            ).toString();
        }
    
        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ScalarWithFallback<T> other = (ScalarWithFallback<T>) obj;
            return this.origin.equals(other.origin)
                && this.fallbacks.equals(other.fallbacks);
        }
    
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 31 * hash + this.origin.hashCode();
            hash = 31 * hash + this.fallbacks.hashCode();
            return hash;
        }
    
        @Override
        public Scalar<T> memoize() {
            return new Memoized<>(this);
        }
    
        @Override
        public Scalar<T> memoize(final long ttl) {
            return new Memoized<>(this, ttl);
        }
    
        @Override
        public Scalar<T> memoize(final long ttl, final long period) {
            return new Memoized<>(this, ttl, period);
        }
    
        @Override
        public Scalar<T> memoize(final long ttl, final long period, final long init) {
            return new Memoized<>(this, ttl, period, init);
        }
    
        @Override       
 }

 

}