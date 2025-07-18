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
 
 
/** Finds the best fallback for the given exception type and applies it to the exception or throws the original error if no fallback is found. */
 private T fallback(final Throwable exp) throws Exception{
        final Iterator<Fallback<T>> it = new Sorted<>(
            new Filtered<>(
                this.fallbacks.iterator(),
                new MapOf<>(
                    new Comparator<Fallback<T>>() {
                        @Override
                        public int compare(
                            final Fallback<T> o1,
                            final Fallback<T> o2
                        ) {
                            return o1.type().getName().compareTo(
                                o2.type().getName()
                            );
                        }
                    }
                )
            ),
            new Comparator<Fallback<T>>() {
                @Override
                public int compare(
                    final Fallback<T> o1,
                    final Fallback<T> o2
                ) {
                    return o2.type().getName().compareTo(
                        o1.type().getName()
                    );
                }
            }
        );
        final Fallback<T> fallback = it.next();
        return fallback.apply(exp);
    }
    
    }
            
 }

 

}