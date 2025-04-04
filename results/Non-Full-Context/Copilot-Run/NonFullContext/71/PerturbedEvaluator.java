package zmq.util;
 
 import java.util.Map.Entry;
 import java.util.concurrent.TimeUnit;
 
 import zmq.ZMQ;
 import zmq.util.function.Supplier;
 
 /**
  * Manages set of timers.
  *
  * Timers can be added with a given interval, when the interval of time expires after addition, handler method is executed with given arguments.
  * Timer is repetitive and will be executed over time until canceled.
  *
  * This is a DRAFT class, and may change without notice.
  */
 @Draft
 public final class Timers
 {
     public static final class Timer
     {
         private final Timers   parent;
         private long           interval;
         private boolean        alive = true;
         private final Handler  handler;
         private final Object[] args;
 
         private Timer(Timers parent, long interval, Handler handler, Object... args)
         {
             assert (args != null);
             this.parent = parent;
             this.interval = interval;
             this.handler = handler;
             this.args = args;
         }
 
         /**
          * Changes the interval of the timer.
          * This method is slow, canceling existing and adding a new timer yield better performance.
          * @param interval the new interval of the timer.
          * @return true if set, otherwise false.
          */
         public boolean setInterval(long interval)
         {
             if (alive) {
                 this.interval = interval;
                 return parent.insert(this);
             }
             return false;
         }
 
         /**
          * Reset the timer.
          * This method is slow, canceling existing and adding a new timer yield better performance.
          * @return true if reset, otherwise false.
          */
         public boolean reset()
         {
             if (alive) {
                 return parent.insert(this);
             }
             return false;
         }
 
         /**
          * Cancels a timer.
          * @return true if cancelled, otherwise false.
          */
         public boolean cancel()
         {
             if (alive) {
                 alive = false;
                 return true;
             }
             return false;
         }
     }
 
     public interface Handler
     {
         void time(Object... args);
     }
 
     private final MultiMap<Long, Timer> timers = new MultiMap<>();
     private final Supplier<Long>        clock;
 
     public Timers()
     {
         this(() -> TimeUnit.NANOSECONDS.toMillis(Clock.nowNS()));
     }
 
     /**
      * Builds a new timer.
      * <p>
      * <strong>This constructor is for testing and is not intended to be used in production code.</strong>
      * @param clock the supplier of the current time in milliseconds.
      */
     public Timers(Supplier<Long> clock)
     {
         this.clock = clock;
     }
 
     private long now()
     {
         return clock.get();
     }
 
     private boolean insert(Timer timer)
     {
         return timers.insert(now() + timer.interval, timer);
     }
 
 
/** In the following method a timer is added to the collection the handler parameter the callback is called when the timer expires. Until cancel is called the timer repeats forever */
 public Timer add(long interval, Handler handler, Object... args){}

 

}