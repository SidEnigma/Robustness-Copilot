// Copyright (c) 2017, 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.operator.logging;
 
 import java.text.MessageFormat;
 import java.util.ResourceBundle;
 import java.util.logging.ConsoleHandler;
 import java.util.logging.Handler;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 /** Centralized logging for the operator. */
 public class LoggingFacade {
 
   public static final String TRACE = "OWLS-KO-TRACE: ";
   protected static final String CLASS = LoggingFacade.class.getName();
   private final Logger logger;
 
   /**
    * Construct logging facade.
    * @param logger logger
    */
   public LoggingFacade(Logger logger) {
     this.logger = logger;
 
     final Logger parentLogger = Logger.getAnonymousLogger().getParent();
     final Handler[] handlers = parentLogger.getHandlers();
     for (final Handler handler : handlers) {
       if (handler instanceof ConsoleHandler) {
         parentLogger.removeHandler(handler);
       }
     }
 
     ConsoleHandler handler = new ConsoleHandler();
     handler.setFormatter(new LoggingFormatter());
     logger.addHandler(handler);
   }
 
   /**
    * Logs a message at the CONFIG level.
    *
    * @param msg message to log
    */
   public void config(String msg) {
     if (isConfigEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.CONFIG, details.clazz, details.method, msg);
     }
   }
 
   /**
    * Logs a message which requires parameters at the CONFIG level.
    *
    * @param msg message to log
    * @param params vararg list of parameters to use when logging the message
    */
   public void config(String msg, Object... params) {
     if (isConfigEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.CONFIG, details.clazz, details.method, msg, params);
     }
   }
 
   /**
    * Logs a message which accompanies a Throwable at the CONFIG level.
    *
    * @param msg the message to log
    * @param thrown an Exception to include in the logged message
    */
   public void config(String msg, Throwable thrown) {
     if (isConfigEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.CONFIG, details.clazz, details.method, msg, thrown);
     }
   }
 
   /** Logs a method entry. The calling class and method names will be inferred. */
   public void entering() {
     if (isFinerEnabled()) {
       CallerDetails details = inferCaller();
       logger.entering(details.clazz, details.method);
     }
   }
 
   /**
    * Logs a method entry, with a list of arguments of interest. The calling class and method names
    * will be inferred. Warning: Depending on the nature of the arguments, it may be required to cast
    * those of type String to Object, to ensure that this variant is called as expected, instead of
    * one of those referenced below.
    *
    * @param params varargs list of objects to include in the log message
    */
   public void entering(Object... params) {
     if (isFinerEnabled()) {
       CallerDetails details = inferCaller();
       logger.entering(details.clazz, details.method, params);
     }
   }
 
   /** Logs a method exit. The calling class and method names will be inferred. */
   public void exiting() {
     if (isFinerEnabled()) {
       CallerDetails details = inferCaller();
       logger.exiting(details.clazz, details.method);
     }
   }
 
   /**
    * Logs a method exit, with a result object. The calling class and method names will be inferred.
    *
    * @param result object to log which is the result of the method call
    */
   public void exiting(Object result) {
     if (isFinerEnabled()) {
       CallerDetails details = inferCaller();
       logger.exiting(details.clazz, details.method, result);
     }
   }
 
   /**
    * Logs a message at the FINE level.
    *
    * @param msg the message to log
    */
   public void fine(String msg) {
     if (isFineEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.FINE, details.clazz, details.method, msg);
     }
   }
 
   /**
    * Logs a message which requires parameters at the FINE level.
    *
    * @param msg the message to log
    * @param params varargs list of objects to include in the log message
    */
   public void fine(String msg, Object... params) {
     if (isFineEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.FINE, details.clazz, details.method, msg, params);
     }
   }
 
   /**
    * Logs a message which accompanies a Throwable at the FINE level.
    *
    * @param msg the message to log
    * @param thrown an Exception to include in the logged message
    */
   public void fine(String msg, Throwable thrown) {
     if (isFineEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.FINE, details.clazz, details.method, msg, thrown);
     }
   }
 
   /**
    * Logs a message at the FINER level.
    *
    * @param msg the message to log
    */
   public void finer(String msg) {
     if (isFinerEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.FINER, details.clazz, details.method, msg);
     }
   }
 
   /**
    * Logs a message which requires parameters at the FINER level.
    *
    * @param msg the message to log
    * @param params varargs list of objects to include in the log message
    */
   public void finer(String msg, Object... params) {
     if (isFinerEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.FINER, details.clazz, details.method, msg, params);
     }
   }
 
   /**
    * Logs a message which accompanies a Throwable at the FINER level.
    *
    * @param msg the message to log
    * @param thrown an Exception to include in the logged message
    */
   public void finer(String msg, Throwable thrown) {
     if (isFinerEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.FINER, details.clazz, details.method, msg, thrown);
     }
   }
 
   /**
    * Logs a message at the FINEST level.
    *
    * @param msg the message to log
    */
   public void finest(String msg) {
     if (isFinestEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.FINEST, details.clazz, details.method, msg);
     }
   }
 
   /**
    * Logs a message which requires parameters at the FINEST level.
    *
    * @param msg the message to log
    * @param params varargs list of objects to include in the log message
    */
   public void finest(String msg, Object... params) {
     if (isFinestEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.FINEST, details.clazz, details.method, msg, params);
     }
   }
 
   /**
    * Logs a message which accompanies a Throwable at the FINEST level.
    *
    * @param msg the message to log
    * @param thrown an Exception to include in the logged message
    */
   public void finest(String msg, Throwable thrown) {
     if (isFinestEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.FINEST, details.clazz, details.method, msg, thrown);
     }
   }
 
   /**
    * Returns the level at which the underlying logger operates.
    *
    * @return a Level object at which logger is operating
    */
   public Level getLevel() {
     return logger.getLevel();
   }
 
   /**
    * Sets the level at which the underlying Logger operates. This should not be called in the
    * general case; levels should be set via OOB configuration (a configuration file exposed by the
    * logging implementation, management API, etc).
    *
    * @param newLevel Level to set
    */
   public void setLevel(Level newLevel) {
     logger.setLevel(newLevel);
   }
 
   /**
    * Returns the name of the underlying logger.
    *
    * @return a String with the name of the logger
    */
   public String getName() {
     return logger.getName();
   }
 
   /**
    * Returns the underlying logger. This should only be used when component code calls others' code,
    * and that code requires that we provide it with a Logger.
    *
    * @return the underlying Logger object
    */
   public Logger getUnderlyingLogger() {
     return logger;
   }
 
   /**
    * Logs a message at the INFO level.
    *
    * @param msg the message to log
    */
   public void info(String msg) {
     if (isInfoEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.INFO, details.clazz, details.method, msg);
     }
   }
 
   /**
    * Logs a message which requires parameters at the INFO level.
    *
    * @param msg the message to log
    * @param params varargs list of objects to include in the log message
    */
   public void info(String msg, Object... params) {
     if (isInfoEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.INFO, details.clazz, details.method, msg, params);
     }
   }
 
   /**
    * Logs a message which requires parameters at the INFO level with a logging filter applied.
    *
    * @param loggingFilter LoggingFilter to be applied, can be null
    * @param msg the message to log
    * @param params varargs list of objects to include in the log message
    */
   public void info(LoggingFilter loggingFilter, String msg, Object... params) {
     if (isInfoEnabled() && LoggingFilter.canLog(loggingFilter, msg)) {
       CallerDetails details = inferCaller();
       logger.logp(Level.INFO, details.clazz, details.method, msg, params);
     }
   }
 
   /**
    * Logs a message which accompanies a Throwable at the INFO level.
    *
    * @param msg the message to log
    * @param thrown an Exception to include in the logged message
    */
   public void info(String msg, Throwable thrown) {
     if (isInfoEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.INFO, details.clazz, details.method, msg, thrown);
     }
   }
 
   /**
    * Checks if a message at CONFIG level would actually be logged.
    *
    * @return <code>true</code> if logging at the CONFIG level
    */
   public boolean isConfigEnabled() {
     return logger.isLoggable(Level.CONFIG);
   }
 
   /**
    * Checks if a message at FINE level would actually be logged.
    *
    * @return <code>true</code> if logging at the FINE level
    */
   public boolean isFineEnabled() {
     return logger.isLoggable(Level.FINE);
   }
 
   /**
    * Checks if a message at FINER level would actually be logged.
    *
    * @return <code>true</code> if logging at the FINER level
    */
   public boolean isFinerEnabled() {
     return logger.isLoggable(Level.FINER);
   }
 
   /**
    * Checks if a message at FINEST level would actually be logged.
    *
    * @return <code>true</code> if logging at the FINEST level
    */
   public boolean isFinestEnabled() {
     return logger.isLoggable(Level.FINEST);
   }
 
   /**
    * Checks if a message at INFO level would actually be logged.
    *
    * @return <code>true</code> if logging at the INFO level
    */
   public boolean isInfoEnabled() {
     return logger.isLoggable(Level.INFO);
   }
 
   /**
    * Checks if a message at the provided level would actually be logged.
    *
    * @param level a Level object to check against
    * @return <code>true</code> if logging at the level specified
    */
   public boolean isLoggable(Level level) {
     return logger.isLoggable(level);
   }
 
   /**
    * Checks if a message at SEVERE level would actually be logged.
    *
    * @return <code>true</code> if logging at the SEVERE level
    */
   public boolean isSevereEnabled() {
     return logger.isLoggable(Level.SEVERE);
   }
 
   /**
    * Checks if a message at WARNING level would actually be logged.
    *
    * @return <code>true</code> if logging at the WARNING level
    */
   public boolean isWarningEnabled() {
     return logger.isLoggable(Level.WARNING);
   }
 
   /**
    * Logs a message at the requested level. Normally, one of the level-specific methods should be
    * used instead.
    *
    * @param level Level at which log log the message
    * @param msg the message to log
    */
   public void log(Level level, String msg) {
     if (isLoggable(level)) {
       CallerDetails details = inferCaller();
       logger.logp(level, details.clazz, details.method, msg);
     }
   }
 
   /**
    * Logs a message which requires parameters. This replaces the Logger equivalents taking a single
    * param or an Object array, and is backward-compatible with them. Calling the per-Level methods
    * is preferred, but this is present for completeness.
    *
    * @param level Level at which log log the message
    * @param msg the message to log
    * @param params varargs list of objects to include in the log message
    * @see Logger#log(java.util.logging.Level, String, Object[])
    */
   public void log(Level level, String msg, Object... params) {
     if (isLoggable(level)) {
       CallerDetails details = inferCaller();
       logger.logp(level, details.clazz, details.method, msg, params);
     }
   }
 
   /**
    * Logs a message which accompanies a Throwable. Calling equivalent per-Level method is preferred,
    * but this is present for completeness.
    *
    * @param level Level at which log log the message
    * @param msg the message to log
    * @param thrown an Exception to include in the logged message
    */
   public void log(Level level, String msg, Throwable thrown) {
     if (isLoggable(level)) {
       CallerDetails details = inferCaller();
       logger.logp(level, details.clazz, details.method, msg, thrown);
     }
   }
 
   /**
    * Logs a message at the SEVERE level.
    *
    * @param msg the message to log
    */
   public void severe(String msg) {
     if (isSevereEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.SEVERE, details.clazz, details.method, msg);
     }
   }
 
   /**
    * Logs a message which requires parameters at the SEVERE level.
    *
    * @param msg the message to log
    * @param params varargs list of objects to include in the log message
    */
   public void severe(String msg, Object... params) {
     if (isSevereEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.SEVERE, details.clazz, details.method, msg, params);
     }
   }
 
   /**
    * Logs a message which requires parameters at the SEVERE level with a logging filter applied.
    *
    * @param loggingFilter LoggingFilter to be applied, can be null
    * @param msg the message to log
    * @param params varargs list of objects to include in the log message
    */
   public void severe(LoggingFilter loggingFilter, String msg, Object... params) {
     if (isSevereEnabled() && LoggingFilter.canLog(loggingFilter, msg)) {
       CallerDetails details = inferCaller();
       logger.logp(Level.SEVERE, details.clazz, details.method, msg, params);
     }
   }
 
   /**
    * Logs a message which accompanies a Throwable at the SEVERE level.
    *
    * @param msg the message to log
    * @param thrown an Exception to include in the logged message
    */
   public void severe(String msg, Throwable thrown) {
     if (isSevereEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.SEVERE, details.clazz, details.method, msg, thrown);
     }
   }
 
   /**
    * Logs a message which accompanies a Throwable at the SEVERE level with a logging filter applied.
    *
    * @param loggingFilter LoggingFilter to be applied, can be null
    * @param msg the message to log
    * @param thrown an Exception to include in the logged message
    */
   public void severe(LoggingFilter loggingFilter, String msg, Throwable thrown) {
     if (isSevereEnabled() && LoggingFilter.canLog(loggingFilter, msg)) {
       CallerDetails details = inferCaller();
       logger.logp(Level.SEVERE, details.clazz, details.method, msg, thrown);
     }
   }
 
   /**
    * Logs that an exception will be thrown. The calling class and method names will be inferred.
    *
    * @param pending an Exception to include in the logged message
    */
   public void throwing(Throwable pending) {
     if (isFinerEnabled()) {
       CallerDetails details = inferCaller();
       logger.throwing(details.clazz, details.method, pending);
     }
   }
 
   /**
    * Logs a message at the WARNING level.
    *
    * @param msg the message to log
    */
   public void warning(String msg) {
     if (isWarningEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.WARNING, details.clazz, details.method, msg);
     }
   }
 
   /**
    * Logs a message which requires parameters at the WARNING level.
    *
    * @param msg the message to log
    * @param params varargs list of objects to include in the log message
    */
   public void warning(String msg, Object... params) {
     if (isWarningEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.WARNING, details.clazz, details.method, msg, params);
     }
   }
 
   /**
    * Logs a message which requires parameters at the WARNING level with a logging filter applied.
    *
    * @param loggingFilter LoggingFilter to be applied, can be null
    * @param msg the message to log
    * @param params varargs list of objects to include in the log message
    */
   public void warning(LoggingFilter loggingFilter, String msg, Object... params) {
     if (isWarningEnabled() && LoggingFilter.canLog(loggingFilter, msg)) {
       CallerDetails details = inferCaller();
       logger.logp(Level.WARNING, details.clazz, details.method, msg, params);
     }
   }
 
   /**
    * Logs a message which accompanies a Throwable at the WARNING level.
    *
    * @param msg the message to log
    * @param thrown an Exception to include in the logged message
    */
   public void warning(String msg, Throwable thrown) {
     if (isWarningEnabled()) {
       CallerDetails details = inferCaller();
       logger.logp(Level.WARNING, details.clazz, details.method, msg, thrown);
     }
   }
 
   /**
    * Logs a trace message with the ID FMW-TRACE at the FINER level.
    *
    * @param msg the message to log
    */
   public void trace(String msg) {
     finer(TRACE + msg);
   }
 
   /**
    * Logs a trace message with the ID FMW-TRACE at the FINER level.
    *
    * @param msg the message to log
    * @param args parameters to the trace message
    */
   public void trace(String msg, Object... args) {
     finer(TRACE + msg, args);
   }
 
   /**
    * Accessor for the resource bundle backing this logger.
    * @return the bundle
    */
   public ResourceBundle getResourceBundle() {
     for (Logger l = getUnderlyingLogger(); l != null; l = l.getParent()) {
       ResourceBundle rb = l.getResourceBundle();
       if (rb != null) {
         return rb;
       }
     }
     throw new AssertionError(formatMessage(MessageKeys.RESOURCE_BUNDLE_NOT_FOUND));
   }
 
   /**
    * Formats message based on string loaded from the resource bundle backing this logger.
    * @param msgId Message id
    * @param params Parameters to message formatting
    * @return Formatted message
    */
   public String formatMessage(String msgId, Object... params) {
     if (params == null || params.length == 0) {
       return getResourceBundle().getString(msgId);
     }
 
     String msg = getResourceBundle().getString(msgId);
     MessageFormat formatter = new MessageFormat(msg);
     return formatter.format(params);
   }
 
 
/** Obtains caller details, class name and method, to be provided to the actual Logger. */

  private CallerDetails inferCaller() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    String className = null;
    String methodName = null;

    // Iterate through the stack trace to find the first class and method that is not part of the LoggingFacade class
    for (StackTraceElement element : stackTrace) {
      String currentClassName = element.getClassName();
      if (!currentClassName.equals(CLASS)) {
        className = currentClassName;
        methodName = element.getMethodName();
        break;
      }
    }

    return new CallerDetails(className, methodName);
  }

  // ...
}
