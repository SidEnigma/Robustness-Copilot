// Copyright (c) 2018, 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.operator.work;
 
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.concurrent.locks.Condition;
 import java.util.concurrent.locks.ReentrantLock;
 import java.util.function.Consumer;
 
 import oracle.kubernetes.operator.logging.LoggingFacade;
 import oracle.kubernetes.operator.logging.LoggingFactory;
 import oracle.kubernetes.operator.logging.MessageKeys;
 import oracle.kubernetes.operator.work.NextAction.Kind;
 
 import static oracle.kubernetes.operator.logging.MessageKeys.CURRENT_STEPS;
 
 /**
  * User-level thread&#x2E; Represents the execution of one processing flow. The {@link Engine} is
  * capable of running a large number of flows concurrently by using a relatively small number of
  * threads. This is made possible by utilizing a {@link Fiber} &mdash; a user-level thread that gets
  * created for each processing flow. A fiber remembers where in the pipeline the processing is at
  * and other additional information specific to the execution of a particular flow.
  *
  * <h2>Suspend/Resume</h2>
  *
  * <p>Fiber can be {@link NextAction#suspend(Consumer) suspended} by a {@link Step}. When a fiber is
  * suspended, it will be kept on the side until it is {@link #resume(Packet) resumed}. This allows
  * threads to go execute other runnable fibers, allowing efficient utilization of smaller number of
  * threads.
  *
  * <h2>Context ClassLoader</h2>
  *
  * <p>Just like thread, a fiber has a context class loader (CCL.) A fiber's CCL becomes the thread's
  * CCL when it's executing the fiber. The original CCL of the thread will be restored when the
  * thread leaves the fiber execution.
  *
  * <h2>Debugging Aid</h2>
  *
  * <p>Setting the {@link #LOGGER} for FINE would give you basic start/stop/resume/suspend level
  * logging. Using FINER would cause more detailed logging, which includes what steps are executed in
  * what order and how they behaved.
  */
 public final class Fiber implements Runnable, ComponentRegistry, AsyncFiber {
   private static final LoggingFacade LOGGER = LoggingFactory.getLogger("Operator", "Operator");
   private static final int NOT_COMPLETE = 0;
   private static final int DONE = 1;
   private static final int CANCELLED = 2;
   private static final ThreadLocal<Fiber> CURRENT_FIBER = new ThreadLocal<>();
   /** Used to allocate unique number for each fiber. */
   private static final AtomicInteger iotaGen = new AtomicInteger();
   public final Engine owner;
   private final Fiber parent;
   private final int id;
   /**
    * Replace uses of synchronized(this) with this lock so that we can control unlocking for resume
    * use cases.
    */
   private final ReentrantLock lock = new ReentrantLock();
   private final Condition condition = lock.newCondition();
   private final AtomicInteger status = new AtomicInteger(NOT_COMPLETE);
   private final Map<String, Component> components = new ConcurrentHashMap<>();
   /** The next action for this Fiber. */
   private NextAction na;
   private NextAction last;
   private final ClassLoader contextClassLoader;
   private CompletionCallback completionCallback;
   /** The thread on which this Fiber is currently executing, if applicable. */
   private volatile Thread currentThread;
   private ExitCallback exitCallback;
   private Collection<Fiber> children = null;
   // Will only be populated if log level is at least FINE
   private List<BreadCrumb> breadCrumbs = null;
 
   Fiber(Engine engine) {
     this(engine, null);
   }
 
   Fiber(Engine engine, Fiber parent) {
     this.owner = engine;
     this.parent = parent;
     id = (parent == null) ? iotaGen.incrementAndGet() : (parent.children.size() + 1);
 
     // if this is run from another fiber, then we naturally inherit its context
     // classloader,
     // so this code works for fiber->fiber inheritance just fine.
     contextClassLoader = Thread.currentThread().getContextClassLoader();
   }
 
   /**
    * Gets the current fiber that's running, if set.
    *
    * @return Current fiber
    */
   public static Fiber getCurrentIfSet() {
     return CURRENT_FIBER.get();
   }
 
 
/** Use the executor of this fiber to schedule an operation for a certain time in the future. */
 public void scheduleOnce(long timeout, TimeUnit unit, Runnable runnable){
    owner.getExecutor().schedule(runnable, timeout, unit);
  }
  
    /**
      * Gets the unique id of this fiber.
      *
      * @return Unique id of this fiber
      */
    public int getId() {
      return id;
    }
  
    /**
      * Gets the parent fiber of this fiber.
      *
      * @return Parent fiber of this fiber
      */
    public Fiber getParent() {
      return parent;
    }
  
    /**
      * Gets the owner of this fiber.
      *
      * @return Owner of this fiber
      */
    public Engine getOwner() {
      return owner;
    }
  
    /**
      * Gets the current thread that's executing this fiber.
      *
      * @return Current thread that's executing this fiber
      */
    public Thread getCurrentThread() {
      return currentThread;
    }
  
    /**
      * Gets the current thread that's executing this fiber.
      *
      * @return Current thread that's executing this fiber
      */
    public void setCurrentThread(Thread currentThread) {
      this.currentThread = currentThread;
    }
  
    /**
      * Gets the context class loader of this fiber.
      *
      * @return Context class loader of this fiber
      */
    public ClassLoader getContextClassLoader() {
      return contextClassLoader;
    }
  
    /**
      * Gets the status of this fiber.
      *
      * @return Status of this fiber
      */
    public int getStatus() {
      return status.get();
    }
  
    /**
      * Sets the status of this fiber.
      *
      * @param status Status of this fiber
      */
    public void setStatus(int status) {
      this.status.set(status);
    }
  
    /**
      * Gets the next action of this fiber.
      *
      * @return Next action of this fiber
      */
    public NextAction getNextAction() {
      return na;
    }
  
    /**
      * Sets the next action of   
 }

 

}