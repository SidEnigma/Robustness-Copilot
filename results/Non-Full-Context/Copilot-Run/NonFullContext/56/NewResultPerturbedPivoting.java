/* Copyright (C) 2002-2003  Christoph Steinbeck <steinbeck@users.sf.net>
  *               2002-2008  Egon Willighagen <egonw@users.sf.net>
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public License
  * as published by the Free Software Foundation; either version 2.1
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 package org.openscience.cdk.tools;
 
 import org.apache.logging.log4j.Level;
 import org.apache.logging.log4j.LogManager;
 import org.apache.logging.log4j.Logger;
 import org.apache.logging.log4j.core.LoggerContext;
 import org.apache.logging.log4j.core.config.ConfigurationSource;
 import org.apache.logging.log4j.core.config.Configurator;
 import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
 import org.apache.logging.log4j.internal.LogManagerStatus;
 
 import java.io.BufferedReader;
 import java.io.InputStream;
 import java.io.PrintWriter;
 import java.io.StringReader;
 import java.net.URI;
 import java.util.HashMap;
 import java.util.Map;
 
 /**
  * Useful for logging messages. Often used as a class static variable instantiated like:
  * <pre>
  * public class SomeClass {
  *     private static ILoggingTool logger =
  *         LoggingToolFactory.createLoggingTool(SomeClass.class);
  * }
  * </pre>
  * There is no special reason not to make the logger private and static, as the logging
  * information is closely bound to one specific Class, not subclasses and not instances.
  *
  * <p>The logger has five logging levels:
  * <dl>
  *  <dt>DEBUG
  *  <dd>Default mode. Used for information you might need to track down the cause of a
  *      bug in the source code, or to understand how an algorithm works.
  *  <dt>WARNING
  *  <dd>This indicates a special situation which is unlike to happen, but for which no
  *      special actions need to be taken. E.g. missing information in files, or an
  *      unknown atom type. The action is normally something user friendly.
  *  <dt>INFO
  *  <dd>For reporting informative information to the user that he might easily disregard.
  *      Real important information should be given to the user using a GUI element.
  *  <dt>FATAL
  *  <dd>This level is used for situations that should not have happened *and* that
  *      lead to a situation where this program can no longer function (rare in Java).
  *  <dt>ERROR
  *  <dd>This level is used for situations that should not have happened *and* thus
  *      indicate a bug.
  * </dl>
  *
  * <p>Consider that the debugging will not always be turned on. Therefore, it is better
  * not to concatenate string in the logger.debug() call, but have the LoggingTool do
  * this when appropriate. In other words, use:
  * <pre>
  * logger.debug("The String X has this value: ", someString);
  * logger.debug("The int Y has this value: ", y);
  * </pre>
  * instead of:
  * <pre>
  * logger.debug("The String X has this value: " + someString);
  * logger.debug("The int Y has this value: " + y);
  * </pre>
  *
  * <p>For logging calls that require even more computation you can use the
  * <code>isDebugEnabled()</code> method:
  * <pre>
  * if (logger.isDebugEnabled()) {
  *   logger.info("The 1056389822th prime that is used is: ",
  *     calculatePrime(1056389822));
  * }
  * </pre>
  *
  * <p>The class uses log4j as a backend if available, and {@link System#err} otherwise.
  *
  * @cdk.module log4j
  * @cdk.githash
  */
 public class LoggingTool implements ILoggingTool {
 
     private boolean             toSTDOUT             = false;
 
     private Logger log4jLogger;
     private static ILoggingTool logger;
     private String              classname;
 
     private int                 stackLength;                 // NOPMD
 
     /** Default number of StackTraceElements to be printed by debug(Exception). */
     public final int            DEFAULT_STACK_LENGTH = 5;
 
     /** Log4J2 has customer levels and no longer has "TRACE_INT" etc so we can't know the values at compile
      *  time and therefore it's not possible use a switch. */
     private static final Map<Level, Integer> LOG4J2_LEVEL_TO_CDK_LEVEL = new HashMap<>();
 
     static {
         LOG4J2_LEVEL_TO_CDK_LEVEL.put(Level.TRACE, TRACE);
         LOG4J2_LEVEL_TO_CDK_LEVEL.put(Level.DEBUG, DEBUG);
         LOG4J2_LEVEL_TO_CDK_LEVEL.put(Level.INFO, INFO);
         LOG4J2_LEVEL_TO_CDK_LEVEL.put(Level.WARN, WARN);
         LOG4J2_LEVEL_TO_CDK_LEVEL.put(Level.ERROR, ERROR);
         LOG4J2_LEVEL_TO_CDK_LEVEL.put(Level.FATAL, FATAL);
     }
 
     /**
      * Constructs a LoggingTool which produces log lines without any special
      * indication which class the message originates from.
      */
     public LoggingTool() {
         this(LoggingTool.class);
     }
 
     /**
      * Constructs a LoggingTool which produces log lines indicating them to be
      * for the Class of the <code>Object</code>.
      *
      * @param object Object from which the log messages originate
      */
     public LoggingTool(Object object) {
         this(object.getClass());
     }
 
     /**
      * Constructs a LoggingTool which produces log lines indicating them to be
      * for the given Class.
      *
      * @param classInst Class from which the log messages originate
      */
     public LoggingTool(Class<?> classInst) {
         LoggingTool.logger = this;
         stackLength = DEFAULT_STACK_LENGTH;
         this.classname = classInst.getName();
         try {
             log4jLogger = LogManager.getLogger(classname);
         } catch (NoClassDefFoundError e) {
             toSTDOUT = true;
             logger.debug("Log4J class not found!");
         } catch (NullPointerException e) { // NOPMD
             toSTDOUT = true;
             logger.debug("Properties file not found!");
         } catch (Exception e) {
             toSTDOUT = true;
             logger.debug("Unknown error occurred: ", e.getMessage());
         }
         /* **************************************************************
          * but some JVMs (i.e. MSFT) won't pass the SecurityException to this
          * exception handler. So we are going to check the JVM version first
          * **************************************************************
          */
         String strJvmVersion = System.getProperty("java.version");
         if (strJvmVersion.compareTo("1.2") >= 0) {
             // Use a try {} to catch SecurityExceptions when used in applets
             try {
                 // by default debugging is set off, but it can be turned on
                 // with starting java like "java -Dcdk.debugging=true"
                 if (System.getProperty("cdk.debugging", "false").equals("true")) {
                     Configurator.setLevel(log4jLogger.getName(), Level.DEBUG);
                 } else {
                     Configurator.setLevel(log4jLogger.getName(), Level.WARN);
                 }
                 if (System.getProperty("cdk.debug.stdout", "false").equals("true")) {
                     toSTDOUT = true;
                 }
             } catch (Exception e) {
                 System.err.println("Could not read the System property used to determine "
                         + "if logging should be turned on. So continuing without logging.");
             }
         }
     }
 
     /**
      * Forces the <code>LoggingTool</code> to configure the Log4J toolkit.
      * Normally this should be done by the application that uses the CDK library,
      * but is available for convenience.
      */
     public static void configureLog4j() {
         LoggingTool localLogger = new LoggingTool(LoggingTool.class);
         try {
             try (InputStream resourceAsStream = LoggingTool.class.getResourceAsStream("cdk-log4j2.xml")) {
                 if (resourceAsStream != null) {
                     XmlConfiguration config = new XmlConfiguration(
                             LoggerContext.getContext(true),
                             new ConfigurationSource(resourceAsStream));
                     Configurator.reconfigure(config);
                 }
             }
         } catch (NullPointerException e) { // NOPMD
             localLogger.error("Properties file not found: ", e.getMessage());
             localLogger.debug(e);
         } catch (Exception e) {
             e.printStackTrace();
             localLogger.error("Unknown error occurred: ", e.getMessage());
             localLogger.debug(e);
         }
     }
 
 
/** Generates the system properties of the operating system and Java version. */

public void dumpSystemProperties() {
    Properties properties = System.getProperties();
    for (String key : properties.stringPropertyNames()) {
        String value = properties.getProperty(key);
        logger.debug(key + ": " + value);
    }
}
 

}