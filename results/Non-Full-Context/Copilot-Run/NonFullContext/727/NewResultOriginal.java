/*
  * Copyright 2013-2021 the original author or authors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package net.logstash.logback.util;
 
 import java.lang.ref.ReferenceQueue;
 import java.lang.ref.WeakReference;
 import java.util.Objects;
 import java.util.concurrent.CopyOnWriteArrayList;
 import java.util.function.Supplier;
 
 
 /**
  * Maintains a per-thread value created by the {@link Supplier} given to the constructor.
  * 
  * <p>A thread obtains the value by calling {@link #acquire()} and must release it after
  * use by calling {@link #release()}. If the value is not released, subsequent calls to
  * {@link #acquire()} will throw an {@link IllegalStateException}.
  * 
  * <p>Instances value may also implement the optional {@link ThreadLocalHolder.Lifecycle}
  * interface if they wish to be notified when they are recycled or disposed.
  * 
  * <p>The holder keeps track of each requesting thread and takes care of disposing the
  * allocated value when it dies.
  * 
  * All allocated values are automatically disposed when {@link ThreadLocalHolder#close()}
  * is called.
  * 
  * <p>Note: This class is for internal use only and subject to backward incompatible change
  * at any time.
  * 
  * @param <T> type of instances returned by this {@link ThreadLocalHolder}.
  *
  * @author brenuart
  */
 public class ThreadLocalHolder<T> {
 
     /**
      * The factory used to create new instances
      */
     private final Supplier<T> factory;
     
     /**
      * ThreadLocal holding per-thread instances
      */
     private final ThreadLocal<Holder<T>> threadLocal = ThreadLocal.withInitial(this::initializeThread);
     
     /**
      * Collection of values assigned to each thread
      */
     protected final CopyOnWriteArrayList<HolderRef> threadValues = new CopyOnWriteArrayList<>(); /* visible for testing */
     
     /**
      * Reference to dead threads
      */
     private final ReferenceQueue<Thread> deadThreads = new ReferenceQueue<>();
     
     /**
      * {@code true} when the {@link ThreadLocalHolder} is closed.
      * When closed, values released by threads will be immediately disposed and the reference cleared.
      */
     private volatile boolean closed = false;
     
     
     /**
      * Create a new instance of the pool.
      * 
      * @param factory the factory used to create new instances.
      */
     public ThreadLocalHolder(Supplier<T> factory) {
         this.factory = Objects.requireNonNull(factory);
     }
     
     
 
/** Get the value assigned to the current thread, creating a new one if none is assigned yet or the  previous has been disposed. */

public final T acquire() {
    if (closed) {
        throw new IllegalStateException("ThreadLocalHolder has been closed");
    }

    Holder<T> holder = threadLocal.get();
    if (holder == null || holder.get() == null) {
        holder = initializeThread();
        threadLocal.set(holder);
    }

    return holder.get();
}
 

}