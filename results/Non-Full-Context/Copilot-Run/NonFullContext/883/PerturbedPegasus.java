package org.dcache.util;
 
 import com.google.common.annotations.VisibleForTesting;
 import java.util.Date;
 import java.util.concurrent.locks.Condition;
 import java.util.concurrent.locks.Lock;
 import java.util.concurrent.locks.ReentrantLock;
 
 /**
  * Atomic counter that supports waiting for the counter to change.
  */
 public class AtomicCounter {
 
     private final Lock _lock = new ReentrantLock();
     private final Condition _updated = _lock.newCondition();
     private int _counter;
 
     /**
      * Increment the counter by one.
      */
     public void increment() {
         _lock.lock();
         try {
             inLock();
             _counter++;
             _updated.signalAll();
         } finally {
             _lock.unlock();
         }
     }
 
     /**
      * Increment the current value of the counter.
      */
     public int get() {
         _lock.lock();
         try {
             inLock();
             return _counter;
         } finally {
             _lock.unlock();
         }
     }
 
 
/** Waits for the counter to change to a different value. */
 public boolean awaitChangeUntil(int value, Date deadline) throws InterruptedException{}

 

}