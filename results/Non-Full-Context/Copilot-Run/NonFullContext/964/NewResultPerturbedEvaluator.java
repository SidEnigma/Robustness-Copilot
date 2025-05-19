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
     
     
     /**
      * Get the value assigned to the current thread, creating a new one if none is assigned yet or the
      * previous has been disposed.
      * 
      * The value must be {@link #release()} to ensure proper life cycle before it can be {@link #acquire()}
      * again.
      * 
      * @return the value assigned to this thread
      * @throws IllegalStateException if the value is already in use and {@link #release()} was not yet invoked.
      */
     public final T acquire() {
         Holder<T> holder = this.threadLocal.get();
         
         if (holder.leased) {
             throw new IllegalStateException("ThreadLocal value is already in use and not yet released.");
         }
         
         if (holder.value == null) {
             holder.value = Objects.requireNonNull(createInstance());
         }
         
         holder.leased = true;
         return holder.value;
     }
     
     
     /**
      * Release the value and recycle it if possible.
      * 
      * @throws IllegalStateException if the value was not previously {@link #acquire()}.
      */
     public final void release() {
         Holder<T> holder = this.threadLocal.get();
         
         if (!holder.leased) {
             throw new IllegalStateException("Invalid attempt at releasing a value that was not previously acquired.");
         }
         holder.leased = false;
         
         /*
          * Dispose value if it cannot be recycled
          */
         if (this.closed || !safelyRecycleInstance(holder.value)) {
             disposeHolder(holder);
         }
               
         /*
          * Dispose values assigned to threads that just died
          */
         processDeadThreads();
     }
     
       
     /**
      * Close the holder and dispose all values.
      * Threads are still able to {@link #acquire()} values after the holder is closed, but they will be disposed
      * immediately when {@link #release()} instead of recycled.
      */
     public void close() {
         /*
          * Indicate the holder so values released by running threads will be disposed
          * immediately instead of being recycled.
          */
         this.closed = true;
         
         /*
          * Dispose value assigned to running threads.
          * "inuse" values will be disposed by the owning thread when it releases it.
          */
         for (HolderRef holderRef: this.threadValues) {
             Holder<T> holder = holderRef.getHolder();
             if (!holder.leased) {
                 disposeHolder(holder);
             }
         }
         this.threadValues.clear();
      
         /*
          * Dispose values assigned to threads that just died
          */
         processDeadThreads();
     }
     
     
     /**
      * Create a new {@link Holder} and keep track of the asking thread for clearing when the thread
      * is gone.
      * 
      * @return a {@link Holder} assigned to the current thread.
      */
     private Holder<T> initializeThread() {
         Holder<T> holder = new Holder<>();
         threadValues.add(new HolderRef(Thread.currentThread(), holder, deadThreads));
         return holder;
     }
 
     
     /**
      * Dispose values of dead threads
      */
     @SuppressWarnings("unchecked")
     private void processDeadThreads() {
         // Note:
         //   ReferenceQueue#poll is thread safe and doesn't block when empty.
 
         HolderRef ref = (HolderRef) deadThreads.poll();
         while (ref != null) {
             Holder<T> holder = ref.getHolder();
             disposeHolder(holder);
             threadValues.remove(ref);
             
             ref = (HolderRef) deadThreads.poll();
         }
     }
     
    
     private void disposeHolder(Holder<T> holder) {
         safelyDisposeInstance(holder.value);
         holder.value = null;
     }
     
     
     /**
      * Create a new object instance (must be non-null).
      * Sub-classes may override this method to implement their own custom logic if needed.
      * 
      * @return a new object instance
      */
     protected T createInstance() {
         return this.factory.get();
     }
     
     
     /**
      * Dispose the object instance by calling its life cycle methods.
      * Sub-classes may override this method if they wish to implement their own custom logic.
      * 
      * @param instance the instance to dispose
      */
     protected void disposeInstance(T instance) {
         if (instance instanceof Lifecycle) {
             ((Lifecycle) instance).dispose();
         }
     }
     
     
     /**
      * Safely dispose the given instance, ignoring any exception that may be thrown.
      * 
      * @param instance the instance to dispose
      */
     private void safelyDisposeInstance(T instance) {
         try {
             disposeInstance(instance);
         } catch (Exception e) {
             // ignore
         }
     }
     
     
 
/** Before putting the instance back in the pool, recycle it. If a subclass wants to add their own custom logic, they can override this function. */

protected boolean recycleInstance(T instance) {
    // Add your custom logic here
    // Return true if the instance was successfully recycled, false otherwise
    return true;
}
 

}