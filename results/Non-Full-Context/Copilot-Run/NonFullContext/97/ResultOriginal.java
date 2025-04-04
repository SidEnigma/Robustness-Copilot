package liquibase.integration.commandline;
 
 import liquibase.*;
 import liquibase.change.CheckSum;
 import liquibase.changelog.ChangeLogParameters;
 import liquibase.changelog.visitor.ChangeExecListener;
 import liquibase.command.CommandFailedException;
 import liquibase.command.CommandResults;
 import liquibase.command.CommandScope;
 import liquibase.command.core.*;
 import liquibase.configuration.ConfiguredValue;
 import liquibase.configuration.LiquibaseConfiguration;
 import liquibase.configuration.core.DeprecatedConfigurationValueProvider;
 import liquibase.database.Database;
 import liquibase.diff.compare.CompareControl;
 import liquibase.diff.output.DiffOutputControl;
 import liquibase.diff.output.ObjectChangeFilter;
 import liquibase.diff.output.StandardObjectChangeFilter;
 import liquibase.exception.*;
 import liquibase.hub.HubConfiguration;
 import liquibase.hub.HubServiceFactory;
 import liquibase.integration.IntegrationDetails;
 import liquibase.license.*;
 import liquibase.lockservice.LockService;
 import liquibase.lockservice.LockServiceFactory;
 import liquibase.logging.LogMessageFilter;
 import liquibase.logging.LogService;
 import liquibase.logging.Logger;
 import liquibase.logging.core.JavaLogService;
 import liquibase.resource.ClassLoaderResourceAccessor;
 import liquibase.resource.CompositeResourceAccessor;
 import liquibase.resource.FileSystemResourceAccessor;
 import liquibase.resource.ResourceAccessor;
 import liquibase.ui.ConsoleUIService;
 import liquibase.util.ISODateFormat;
 import liquibase.util.LiquibaseUtil;
 import liquibase.util.StringUtil;
 
 import java.io.*;
 import java.lang.reflect.Field;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.net.URLClassLoader;
 import java.nio.file.Paths;
 import java.security.AccessController;
 import java.security.PrivilegedAction;
 import java.text.MessageFormat;
 import java.text.ParseException;
 import java.util.*;
 import java.util.jar.JarEntry;
 import java.util.jar.JarFile;
 import java.util.logging.*;
 
 import static java.util.ResourceBundle.getBundle;
 
 /**
  * Class for executing Liquibase via the command line.
  *
  * @deprecated use liquibase.integration.commandline.LiquibaseCommandLine.
  */
 public class Main {
 
     //set by new CLI to signify it is handling some of the configuration
     public static boolean runningFromNewCli;
 
     //temporary work-around to pass -D changelog parameters from new CLI to here
     public static Map<String, String> newCliChangelogParameters;
 
     private static PrintStream outputStream = System.out;
 
     private static final String ERRORMSG_UNEXPECTED_PARAMETERS = "unexpected.command.parameters";
     private static final Logger LOG = Scope.getCurrentScope().getLog(Main.class);
     private static ResourceBundle coreBundle = getBundle("liquibase/i18n/liquibase-core");
 
     protected ClassLoader classLoader;
     protected String driver;
     protected String username;
     protected String password;
     protected String url;
     protected String hubConnectionId;
     protected String hubProjectId;
     protected String hubProjectName;
     protected String databaseClass;
     protected String defaultSchemaName;
     protected String outputDefaultSchema;
     protected String outputDefaultCatalog;
     protected String liquibaseCatalogName;
     protected String liquibaseSchemaName;
     protected String databaseChangeLogTableName;
     protected String databaseChangeLogLockTableName;
     protected String databaseChangeLogTablespaceName;
     protected String defaultCatalogName;
     protected String changeLogFile;
     protected String overwriteOutputFile;
     protected String classpath;
     protected String contexts;
     protected String labels;
     protected String driverPropertiesFile;
     protected String propertyProviderClass;
     protected String changeExecListenerClass;
     protected String changeExecListenerPropertiesFile;
     protected Boolean promptForNonLocalDatabase;
     protected Boolean includeSystemClasspath;
     protected String defaultsFile = "liquibase.properties";
     protected String diffTypes;
     protected String changeSetAuthor;
     protected String changeSetContext;
     protected String dataOutputDirectory;
     protected String referenceDriver;
     protected String referenceUrl;
     protected String referenceUsername;
     protected String referencePassword;
     protected String referenceDefaultCatalogName;
     protected String referenceDefaultSchemaName;
     protected String currentDateTimeFunction;
     protected String command;
     protected Set<String> commandParams = new LinkedHashSet<>();
     protected String logLevel;
     protected String logFile;
     protected Map<String, Object> changeLogParameters = new HashMap<>();
     protected String outputFile;
     protected String excludeObjects;
     protected Boolean includeCatalog;
     protected String includeObjects;
     protected Boolean includeSchema;
     protected Boolean includeTablespace;
     protected Boolean deactivate;
     protected String outputSchemasAs;
     protected String referenceSchemas;
     protected String schemas;
     protected String snapshotFormat;
     protected String liquibaseProLicenseKey;
     private boolean liquibaseProLicenseValid = false;
     protected String liquibaseHubApiKey;
     protected String liquibaseHubUrl;
     private Boolean managingLogConfig = null;
     private boolean outputsLogMessages = false;
     protected String sqlFile;
     protected String delimiter;
     protected String rollbackScript;
 
     private static int[] suspiciousCodePoints = {160, 225, 226, 227, 228, 229, 230, 198, 200, 201, 202, 203,
             204, 205, 206, 207, 209, 210, 211, 212, 213, 214, 217, 218, 219,
             220, 222, 223, 232, 233, 234, 235, 236, 237, 238, 239, 241,
             249, 250, 251, 252, 255, 284, 332, 333, 334, 335, 336, 337, 359,
             360, 361, 362, 363, 364, 365, 366, 367, 377, 399,
             8192, 8193, 8194, 8196, 8197, 8199, 8200, 8201, 8202, 8203, 8211, 8287
     };
 
     protected static class CodePointCheck {
         public int position;
         public char ch;
     }
 
     /**
      * Entry point. This is what gets executes when starting this program from the command line. This is actually
      * a simple wrapper so that an errorlevel of != 0 is guaranteed in case of an uncaught exception.
      *
      * @param args the command line arguments
      */
     public static void main(String[] args) {
         int errorLevel = 0;
         try {
             errorLevel = run(args);
         } catch (Throwable e) {
             System.exit(-1);
         }
 
         System.exit(errorLevel);
     }
 
 
     /**
      * Process the command line arguments and perform the appropriate main action (update, rollback etc.)
      *
      * @param args the command line arguments
      * @return the errorlevel to be returned to the operating system, e.g. for further processing by scripts
      * @throws LiquibaseException a runtime exception
      */
     public static int run(String[] args) throws Exception {
         Map<String, Object> scopeObjects = new HashMap<>();
         final IntegrationDetails integrationDetails = new IntegrationDetails();
         integrationDetails.setName("cli");
         final ListIterator<String> argIterator = Arrays.asList(args).listIterator();
         while (argIterator.hasNext()) {
             final String arg = argIterator.next();
             if (arg.startsWith("--")) {
                 if (arg.contains("=")) {
                     String[] splitArg = arg.split("=", 2);
                     String argKey = "argument__" + splitArg[0].replaceFirst("^--", "");
                     if (splitArg.length == 2) {
                         integrationDetails.setParameter(argKey, splitArg[1]);
                     } else {
                         integrationDetails.setParameter(argKey, "true");
                     }
                 } else {
                     String argKey = "argument__" + arg.replaceFirst("^--", "");
                     if (argIterator.hasNext()) {
                         final String next = argIterator.next();
                         if (next.startsWith("--") || isCommand(next)) {
                             integrationDetails.setParameter(argKey, "true");
                             argIterator.previous(); //put value back
                         } else {
                             integrationDetails.setParameter(argKey, next);
                         }
                     } else {
                         integrationDetails.setParameter(argKey, "true");
                     }
                 }
             }
         }
 
         scopeObjects.put("integrationDetails", integrationDetails);
 
         if (!Main.runningFromNewCli) {
             ConsoleUIService ui = new ConsoleUIService();
             ui.setAllowPrompt(true);
             scopeObjects.put(Scope.Attr.ui.name(), ui);
         }
 
         return Scope.child(scopeObjects, new Scope.ScopedRunnerWithReturn<Integer>() {
             @Override
             public Integer run() throws Exception {
                 Main main = new Main();
 
                 try {
                     if ((args.length == 0) || ((args.length == 1) && ("--" + OPTIONS.HELP).equals(args[0]))) {
                         main.printHelp(outputStream);
                         return Integer.valueOf(0);
                     } else if (("--" + OPTIONS.VERSION).equals(args[0])) {
                         main.command = "";
                         main.parseDefaultPropertyFiles();
                         Scope.getCurrentScope().getUI().sendMessage(CommandLineUtils.getBanner());
                         Scope.getCurrentScope().getUI().sendMessage(String.format(coreBundle.getString("version.number"), LiquibaseUtil.getBuildVersionInfo()));
 
                         LicenseService licenseService = Scope.getCurrentScope().getSingleton(LicenseServiceFactory.class).getLicenseService();
                         if (licenseService != null && main.liquibaseProLicenseKey != null) {
                             Location licenseKeyLocation =
                                     new Location("property liquibaseProLicenseKey", LocationType.BASE64_STRING, main.liquibaseProLicenseKey);
                             LicenseInstallResult result = licenseService.installLicense(licenseKeyLocation);
                             if (result.code != 0) {
                                 String allMessages = String.join("\n", result.messages);
                                 Scope.getCurrentScope().getUI().sendErrorMessage(allMessages);
                             }
                         }
                         if (licenseService != null) {
                             Scope.getCurrentScope().getUI().sendMessage(licenseService.getLicenseInfo());
                         }
 
 
                         Scope.getCurrentScope().getUI().sendMessage(String.format("Running Java under %s (Version %s)",
                                 System.getProperties().getProperty("java.home"),
                                 System.getProperty("java.version")
                         ));
                         return Integer.valueOf(0);
                     }
 
                     //
                     // Look for characters which cannot be handled
                     //
                     for (int i = 0; i < args.length; i++) {
                         CodePointCheck codePointCheck = checkArg(args[i]);
                         if (codePointCheck != null) {
                             String message =
                                     "A non-standard character '" + codePointCheck.ch +
                                             "' was detected on the command line at position " +
                                             (codePointCheck.position + 1) + " of argument number " + (i + 1) +
                                             ".\nIf problems occur, please remove the character and try again.";
                             LOG.warning(message);
                             System.err.println(message);
                         }
                     }
 
                     try {
                         main.parseOptions(args);
                         if (main.command == null) {
                             main.printHelp(outputStream);
                             return Integer.valueOf(0);
                         }
                     } catch (CommandLineParsingException e) {
                         Scope.getCurrentScope().getUI().sendMessage(CommandLineUtils.getBanner());
                         Scope.getCurrentScope().getUI().sendMessage(coreBundle.getString("how.to.display.help"));
                         throw e;
                     }
 
                     if (!Main.runningFromNewCli) {
                         final ConsoleUIService ui = (ConsoleUIService) Scope.getCurrentScope().getUI();
                         System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] %4$s [%2$s] %5$s%6$s%n");
 
                         java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
                         java.util.logging.Logger liquibaseLogger = java.util.logging.Logger.getLogger("liquibase");
                         liquibaseLogger.setParent(rootLogger);
 
                         final JavaLogService logService = (JavaLogService) Scope.getCurrentScope().get(Scope.Attr.logService, LogService.class);
                         logService.setParent(liquibaseLogger);
 
                         if (main.logLevel == null) {
                             String defaultLogLevel = System.getProperty("liquibase.log.level");
                             if (defaultLogLevel == null) {
                                 setLogLevel(logService, rootLogger, liquibaseLogger, Level.OFF);
                             } else {
                                 setLogLevel(logService, rootLogger, liquibaseLogger, parseLogLevel(defaultLogLevel, ui));
                             }
                         } else {
                             setLogLevel(logService, rootLogger, liquibaseLogger, parseLogLevel(main.logLevel, ui));
                         }
 
                         if (main.logFile != null) {
                             FileHandler fileHandler = new FileHandler(main.logFile, true);
                             fileHandler.setFormatter(new SimpleFormatter());
                             if (liquibaseLogger.getLevel() == Level.OFF) {
                                 fileHandler.setLevel(Level.FINE);
                             }
 
                             rootLogger.addHandler(fileHandler);
                             for (Handler handler : rootLogger.getHandlers()) {
                                 if (handler instanceof ConsoleHandler) {
                                     handler.setLevel(Level.OFF);
                                 }
                             }
                         }
 
                         if (main.command != null && main.command.toLowerCase().endsWith("sql")) {
                             ui.setOutputStream(System.err);
                         }
                     }
 
                     LicenseService licenseService = Scope.getCurrentScope().getSingleton(LicenseServiceFactory.class).getLicenseService();
                     if (licenseService != null) {
                         if (main.liquibaseProLicenseKey == null) {
                             if (!Main.runningFromNewCli) {
                                 Scope.getCurrentScope().getLog(getClass()).info("No Liquibase Pro license key supplied. Please set liquibaseProLicenseKey on command line or in liquibase.properties to use Liquibase Pro features.");
                             }
                         } else {
                             Location licenseKeyLocation = new Location("property liquibaseProLicenseKey", LocationType.BASE64_STRING, main.liquibaseProLicenseKey);
                             LicenseInstallResult result = licenseService.installLicense(licenseKeyLocation);
                             if (result.code != 0) {
                                 String allMessages = String.join("\n", result.messages);
                                 if (!Main.runningFromNewCli) {
                                     Scope.getCurrentScope().getUI().sendMessage(allMessages);
                                 }
                             } else {
                                 main.liquibaseProLicenseValid = true;
                             }
                         }
 
                         //
                         // Check to see if we have an expired license
                         //
                         if (licenseService.daysTilExpiration() < 0) {
                             main.liquibaseProLicenseValid = false;
                         }
                         if (!Main.runningFromNewCli) {
                             Scope.getCurrentScope().getUI().sendMessage(licenseService.getLicenseInfo());
                         }
                     }
 
                     if (!Main.runningFromNewCli) {
                         Scope.getCurrentScope().getUI().sendMessage(CommandLineUtils.getBanner());
                     }
 
                     if (!LiquibaseCommandLineConfiguration.SHOULD_RUN.getCurrentValue()) {
                         Scope.getCurrentScope().getUI().sendErrorMessage((
                                 String.format(coreBundle.getString("did.not.run.because.param.was.set.to.false"),
                                         LiquibaseCommandLineConfiguration.SHOULD_RUN.getCurrentConfiguredValue().getProvidedValue().getActualKey())));
                         return Integer.valueOf(0);
                     }
 
                     if (setupNeeded(main)) {
                         List<String> setupMessages = main.checkSetup();
                         if (!setupMessages.isEmpty()) {
                             main.printHelp(setupMessages, isStandardOutputRequired(main.command) ? System.err : outputStream);
                             return Integer.valueOf(1);
                         }
                     }
 
                     //
                     // Store the Hub API key for later use
                     //
                     if (StringUtil.isNotEmpty(main.liquibaseHubApiKey)) {
                         DeprecatedConfigurationValueProvider.setData(HubConfiguration.LIQUIBASE_HUB_API_KEY, main.liquibaseHubApiKey);
                     }
 
                     //
                     // Store the Hub URL for later use
                     //
                     if (StringUtil.isNotEmpty(main.liquibaseHubUrl)) {
                         DeprecatedConfigurationValueProvider.setData(HubConfiguration.LIQUIBASE_HUB_URL, main.liquibaseHubUrl);
                     }
 
                     main.applyDefaults();
                     Map<String, Object> innerScopeObjects = new HashMap<>();
                     innerScopeObjects.put("defaultsFile", LiquibaseCommandLineConfiguration.DEFAULTS_FILE.getCurrentValue());
                     if (!Main.runningFromNewCli) {
                         innerScopeObjects.put(Scope.Attr.resourceAccessor.name(), new ClassLoaderResourceAccessor(main.configureClassLoader()));
                     }
                     Scope.child(innerScopeObjects, () -> {
                         main.doMigration();
 
                         if (!Main.runningFromNewCli) {
                             if (COMMANDS.UPDATE.equals(main.command)) {
                                 Scope.getCurrentScope().getUI().sendMessage(coreBundle.getString("update.successful"));
                             } else if (main.command.startsWith(COMMANDS.ROLLBACK)) {
                                 Scope.getCurrentScope().getUI().sendMessage(coreBundle.getString("rollback.successful"));
                             } else {
                                 Scope.getCurrentScope().getUI().sendMessage(String.format(coreBundle.getString("command.successful"), main.command));
                             }
                         }
                     });
                 } catch (Throwable e) {
                     String message = e.getMessage();
                     if (e.getCause() != null) {
                         message = e.getCause().getMessage();
                     }
                     if (message == null) {
                         message = coreBundle.getString("unknown.reason");
                     }
                     // At a minimum, log the message.  We don't need to print the stack
                     // trace because the logger already did that upstream.
                     try {
                         if (e.getCause() instanceof ValidationFailedException) {
                             ((ValidationFailedException) e.getCause()).printDescriptiveError(outputStream);
                         } else {
                             if (!Main.runningFromNewCli) {
                                 if (main.outputsLogMessages) {
                                     Scope.getCurrentScope().getUI().sendErrorMessage((String.format(coreBundle.getString("unexpected.error"), message)), e);
                                 } else {
                                     Scope.getCurrentScope().getUI().sendMessage((String.format(coreBundle.getString("unexpected.error"), message)));
                                     Scope.getCurrentScope().getUI().sendMessage(coreBundle.getString("for.more.information.use.loglevel.flag"));
 
                                     //send it to the LOG in case we're using logFile
                                     Scope.getCurrentScope().getLog(getClass()).severe((String.format(coreBundle.getString("unexpected.error"), message)), e);
                                 }
                             }
                         }
                     } catch (IllegalFormatException e1) {
                         if (Main.runningFromNewCli) {
                             throw e1;
                         }
 
                         e1.printStackTrace();
                     }
                     throw new LiquibaseException(String.format(coreBundle.getString("unexpected.error"), message), e);
                 }
 
                 if (isHubEnabled(main.command) &&
                         HubConfiguration.LIQUIBASE_HUB_API_KEY.getCurrentValue() != null &&
                         !Scope.getCurrentScope().getSingleton(HubServiceFactory.class).isOnline()) {
                     Scope.getCurrentScope().getUI().sendMessage("WARNING: The command " + main.command + " operations were not synced with your Liquibase Hub account because: " + StringUtil.lowerCaseFirst(Scope.getCurrentScope().getSingleton(HubServiceFactory.class).getOfflineReason()));
                 }
 
                 return Integer.valueOf(0);
             }
         });
     }
 
     private static boolean setupNeeded(Main main) throws CommandLineParsingException {
         if (main.command.toLowerCase().startsWith(COMMANDS.REGISTER_CHANGELOG.toLowerCase()) ||
                 main.command.toLowerCase().startsWith(COMMANDS.DEACTIVATE_CHANGELOG.toLowerCase())) {
             return false;
         }
         if (!main.commandParams.contains("--help")) {
             return true;
         }
         return !main.command.toLowerCase().startsWith(COMMANDS.ROLLBACK_ONE_CHANGE_SET.toLowerCase()) &&
                 !main.command.toLowerCase().startsWith(COMMANDS.ROLLBACK_ONE_UPDATE.toLowerCase()) &&
                 (!main.command.toLowerCase().startsWith(COMMANDS.DIFF.toLowerCase()) || !main.isFormattedDiff());
     }
 
     protected static void setLogLevel(LogService logService, java.util.logging.Logger rootLogger, java.util.logging.Logger liquibaseLogger, Level level) {
         if (Main.runningFromNewCli) {
             //new CLI configures logging
             return;
         }
 
         if (level.intValue() < Level.INFO.intValue()) {
             //limit non-liquibase logging to INFO at a minimum to avoid too much logs
             rootLogger.setLevel(Level.INFO);
         } else {
             rootLogger.setLevel(level);
         }
         liquibaseLogger.setLevel(level);
 
         for (Handler handler : rootLogger.getHandlers()) {
             handler.setLevel(level);
             handler.setFilter(new SecureLogFilter(logService.getFilter()));
         }
         //
         // Set the Liquibase Hub log level if logging is not OFF
         //
         if (level != Level.OFF) {
             DeprecatedConfigurationValueProvider.setData(HubConfiguration.LIQUIBASE_HUB_LOGLEVEL, level);
         }
     }
 
     private static Level parseLogLevel(String logLevelName, ConsoleUIService ui) {
         logLevelName = logLevelName.toUpperCase();
         Level logLevel;
         if (logLevelName.equals("DEBUG")) {
             logLevel = Level.FINE;
         } else if (logLevelName.equals("WARN")) {
             logLevel = Level.WARNING;
         } else if (logLevelName.equals("ERROR")) {
             logLevel = Level.SEVERE;
         } else {
             try {
                 logLevel = Level.parse(logLevelName);
             } catch (IllegalArgumentException e) {
                 ui.sendErrorMessage("Unknown log level " + logLevelName);
                 logLevel = Level.OFF;
             }
         }
         return logLevel;
     }
 
     /**
      * Warns the user that some logging was suppressed because the --logLevel command line switch was not set high
      * enough
      *
      * @param outputLoggingEnabled if a warning should be printed
      * @return the warning message (if outputLoggingEnabled==true), an empty String otherwise
      */
     private static String generateLogLevelWarningMessage(boolean outputLoggingEnabled) {
         if (outputLoggingEnabled) {
             return "";
         } else {
             return "\n\n" + coreBundle.getString("for.more.information.use.loglevel.flag");
         }
     }
 
     /**
      * Splits a String of the form "key=value" into the respective parts.
      *
      * @param arg The String expression to split
      * @return An array of exactly 2 entries
      * @throws CommandLineParsingException if the string cannot be split into exactly 2 parts
      */
     // What the number 2 stands for is obvious from the context
     @SuppressWarnings("squid:S109")
     private static String[] splitArg(String arg) throws CommandLineParsingException {
         String[] splitArg = arg.split("=", 2);
         if (splitArg.length < 2) {
             throw new CommandLineParsingException(
                     String.format(coreBundle.getString("could.not.parse.expression"), arg)
             );
         }
 
         splitArg[0] = splitArg[0].replaceFirst("--", "");
         return splitArg;
     }
 
     /**
      * Returns true if the given command is Hub-enabled
      *
      * @param command the command to check
      * @return true if this command has Hub integration false if not
      */
     private static boolean isHubEnabled(String command) {
         return COMMANDS.CHANGELOG_SYNC_SQL.equalsIgnoreCase(command)
                 || COMMANDS.UPDATE_COUNT.equalsIgnoreCase(command)
                 || COMMANDS.UPDATE_TO_TAG.equalsIgnoreCase(command)
                 || COMMANDS.UPDATE.equalsIgnoreCase(command)
                 || COMMANDS.ROLLBACK.equalsIgnoreCase(command)
                 || COMMANDS.ROLLBACK_TO_DATE.equalsIgnoreCase(command)
                 || COMMANDS.ROLLBACK_COUNT.equalsIgnoreCase(command)
                 || COMMANDS.ROLLBACK_ONE_CHANGE_SET.equalsIgnoreCase(command)
                 || COMMANDS.ROLLBACK_ONE_UPDATE.equalsIgnoreCase(command)
                 || COMMANDS.DROP_ALL.equalsIgnoreCase(command);
     }
 
     /**
      * Returns true if the given command requires stdout
      *
      * @param command the command to check
      * @return true if stdout needs for a command, false if not
      */
     private static boolean isStandardOutputRequired(String command) {
         return COMMANDS.SNAPSHOT.equalsIgnoreCase(command)
                 || COMMANDS.SNAPSHOT_REFERENCE.equalsIgnoreCase(command)
                 || COMMANDS.CHANGELOG_SYNC_SQL.equalsIgnoreCase(command)
                 || COMMANDS.CHANGELOG_SYNC_TO_TAG_SQL.equalsIgnoreCase(command)
                 || COMMANDS.MARK_NEXT_CHANGESET_RAN_SQL.equalsIgnoreCase(command)
                 || COMMANDS.UPDATE_COUNT_SQL.equalsIgnoreCase(command)
                 || COMMANDS.UPDATE_TO_TAG_SQL.equalsIgnoreCase(command)
                 || COMMANDS.UPDATE_SQL.equalsIgnoreCase(command)
                 || COMMANDS.ROLLBACK_SQL.equalsIgnoreCase(command)
                 || COMMANDS.ROLLBACK_TO_DATE_SQL.equalsIgnoreCase(command)
                 || COMMANDS.ROLLBACK_COUNT_SQL.equalsIgnoreCase(command)
                 || COMMANDS.FUTURE_ROLLBACK_SQL.equalsIgnoreCase(command)
                 || COMMANDS.FUTURE_ROLLBACK_COUNT_SQL.equalsIgnoreCase(command)
                 || COMMANDS.FUTURE_ROLLBACK_FROM_TAG_SQL.equalsIgnoreCase(command);
     }
 
     /**
      * Returns true if the parameter --changeLogFile is requited for a given command
      *
      * @param command the command to test
      * @return true if a ChangeLog is required, false if not.
      */
     private static boolean isChangeLogRequired(String command) {
         return command.toLowerCase().startsWith(COMMANDS.UPDATE)
                 || (command.toLowerCase().startsWith(COMMANDS.ROLLBACK) &&
                 (!command.equalsIgnoreCase(COMMANDS.ROLLBACK_ONE_CHANGE_SET) &&
                         !command.equalsIgnoreCase(COMMANDS.ROLLBACK_ONE_UPDATE)))
                 || COMMANDS.REGISTER_CHANGELOG.equalsIgnoreCase(command)
                 || COMMANDS.DEACTIVATE_CHANGELOG.equalsIgnoreCase(command)
                 || COMMANDS.CALCULATE_CHECKSUM.equalsIgnoreCase(command)
                 || COMMANDS.STATUS.equalsIgnoreCase(command)
                 || COMMANDS.VALIDATE.equalsIgnoreCase(command)
                 || COMMANDS.CHANGELOG_SYNC.equalsIgnoreCase(command)
                 || COMMANDS.CHANGELOG_SYNC_SQL.equalsIgnoreCase(command)
                 || COMMANDS.CHANGELOG_SYNC_TO_TAG.equalsIgnoreCase(command)
                 || COMMANDS.CHANGELOG_SYNC_TO_TAG_SQL.equalsIgnoreCase(command)
                 || COMMANDS.GENERATE_CHANGELOG.equalsIgnoreCase(command)
                 || COMMANDS.UNEXPECTED_CHANGESETS.equalsIgnoreCase(command)
                 || COMMANDS.DIFF_CHANGELOG.equalsIgnoreCase(command)
                 || COMMANDS.ROLLBACK_ONE_CHANGE_SET.equalsIgnoreCase(command)
                 || COMMANDS.ROLLBACK_ONE_UPDATE.equalsIgnoreCase(command);
     }
 
     /**
      * Returns true if the given arg is a valid main command of Liquibase.
      *
      * @param arg the String to test
      * @return true if it is a valid main command, false if not
      */
     private static boolean isCommand(String arg) {
         return COMMANDS.MIGRATE.equals(arg)
                 || COMMANDS.MIGRATE_SQL.equalsIgnoreCase(arg)
                 || COMMANDS.UPDATE.equalsIgnoreCase(arg)
                 || COMMANDS.UPDATE_SQL.equalsIgnoreCase(arg)
                 || COMMANDS.UPDATE_COUNT.equalsIgnoreCase(arg)
                 || COMMANDS.UPDATE_COUNT_SQL.equalsIgnoreCase(arg)
                 || COMMANDS.UPDATE_TO_TAG.equalsIgnoreCase(arg)
                 || COMMANDS.UPDATE_TO_TAG_SQL.equalsIgnoreCase(arg)
                 || COMMANDS.ROLLBACK.equalsIgnoreCase(arg)
                 || COMMANDS.ROLLBACK_TO_DATE.equalsIgnoreCase(arg)
                 || COMMANDS.ROLLBACK_COUNT.equalsIgnoreCase(arg)
                 || COMMANDS.ROLLBACK_SQL.equalsIgnoreCase(arg)
                 || COMMANDS.ROLLBACK_TO_DATE_SQL.equalsIgnoreCase(arg)
                 || COMMANDS.ROLLBACK_COUNT_SQL.equalsIgnoreCase(arg)
                 || COMMANDS.REGISTER_CHANGELOG.equalsIgnoreCase(arg)
                 || COMMANDS.DEACTIVATE_CHANGELOG.equalsIgnoreCase(arg)
                 || COMMANDS.FUTURE_ROLLBACK_SQL.equalsIgnoreCase(arg)
                 || COMMANDS.FUTURE_ROLLBACK_COUNT_SQL.equalsIgnoreCase(arg)
                 || COMMANDS.FUTURE_ROLLBACK_FROM_TAG_SQL.equalsIgnoreCase(arg)
                 || COMMANDS.UPDATE_TESTING_ROLLBACK.equalsIgnoreCase(arg)
                 || COMMANDS.TAG.equalsIgnoreCase(arg)
                 || COMMANDS.TAG_EXISTS.equalsIgnoreCase(arg)
                 || COMMANDS.LIST_LOCKS.equalsIgnoreCase(arg)
                 || COMMANDS.HISTORY.equalsIgnoreCase(arg)
                 || COMMANDS.DROP_ALL.equalsIgnoreCase(arg)
                 || COMMANDS.RELEASE_LOCKS.equalsIgnoreCase(arg)
                 || COMMANDS.STATUS.equalsIgnoreCase(arg)
                 || COMMANDS.UNEXPECTED_CHANGESETS.equalsIgnoreCase(arg)
                 || COMMANDS.VALIDATE.equalsIgnoreCase(arg)
                 || COMMANDS.HELP.equalsIgnoreCase(arg)
                 || COMMANDS.DIFF.equalsIgnoreCase(arg)
                 || COMMANDS.DIFF_CHANGELOG.equalsIgnoreCase(arg)
                 || COMMANDS.GENERATE_CHANGELOG.equalsIgnoreCase(arg)
                 || COMMANDS.SNAPSHOT.equalsIgnoreCase(arg)
                 || COMMANDS.SNAPSHOT_REFERENCE.equalsIgnoreCase(arg)
                 || COMMANDS.SYNC_HUB.equalsIgnoreCase(arg)
                 || COMMANDS.EXECUTE_SQL.equalsIgnoreCase(arg)
                 || COMMANDS.CALCULATE_CHECKSUM.equalsIgnoreCase(arg)
                 || COMMANDS.CLEAR_CHECKSUMS.equalsIgnoreCase(arg)
                 || COMMANDS.DB_DOC.equalsIgnoreCase(arg)
                 || COMMANDS.CHANGELOG_SYNC.equalsIgnoreCase(arg)
                 || COMMANDS.CHANGELOG_SYNC_SQL.equalsIgnoreCase(arg)
                 || COMMANDS.CHANGELOG_SYNC_TO_TAG.equalsIgnoreCase(arg)
                 || COMMANDS.CHANGELOG_SYNC_TO_TAG_SQL.equalsIgnoreCase(arg)
                 || COMMANDS.MARK_NEXT_CHANGESET_RAN.equalsIgnoreCase(arg)
                 || COMMANDS.MARK_NEXT_CHANGESET_RAN_SQL.equalsIgnoreCase(arg)
                 || COMMANDS.ROLLBACK_ONE_CHANGE_SET.equalsIgnoreCase(arg)
                 || COMMANDS.ROLLBACK_ONE_CHANGE_SET_SQL.equalsIgnoreCase(arg)
                 || COMMANDS.ROLLBACK_ONE_UPDATE.equalsIgnoreCase(arg)
                 || COMMANDS.ROLLBACK_ONE_UPDATE_SQL.equalsIgnoreCase(arg);
     }
 
 
/** Returns true if the given main command arg needs no special parameters. */
 private static boolean isNoArgCommand(String arg){
        return COMMANDS.HELP.equalsIgnoreCase(arg)
                || COMMANDS.DB_DOC.equalsIgnoreCase(arg)
                || COMMANDS.LIST_LOCKS.equalsIgnoreCase(arg)
                || COMMANDS.RELEASE_LOCKS.equalsIgnoreCase(arg)
                || COMMANDS.STATUS.equalsIgnoreCase(arg)
                || COMMANDS.UNEXPECTED_CHANGESETS.equalsIgnoreCase(arg)
                || COMMANDS.VALIDATE.equalsIgnoreCase(arg)
                || COMMANDS.DIFF.equalsIgnoreCase(arg)
                || COMMANDS.DIFF_CHANGELOG.equalsIgnoreCase(arg)
                || COMMANDS.GENERATE_CHANGELOG.equalsIgnoreCase(arg)
                || COMMANDS.SNAPSHOT.equalsIgnoreCase(arg)
                || COMMANDS.SNAPSHOT_REFERENCE.equalsIgnoreCase(arg)
                || COMMANDS.SYNC_HUB.equalsIgnoreCase(arg)
                || COMMANDS.EXECUTE_SQL.equalsIgnoreCase(arg)
                || COMMANDS.CALCULATE_CHECKSUM.equalsIgnoreCase(arg)
                || COMMANDS.CLEAR_CHECKSUMS.equalsIgnoreCase(arg)
                || COMMANDS.CHANGELOG_SYNC.equalsIgnoreCase(arg)
                || COMMANDS.CHANGELOG_SYNC_SQL.equalsIgnoreCase(arg)
                || COMMANDS.CHANGELOG_SYNC_TO_TAG.equalsIgnoreCase(arg)
                || COMMANDS.CHANGELOG_SYNC_TO_TAG_SQL.equalsIgnoreCase(arg)
                || COMMANDS.MARK_NEXT_CHANGESET_RAN.equalsIgnoreCase(arg)
                || COMMANDS.MARK_NEXT_CHANGESET_RAN_SQL.equalsIgnoreCase        
 }

 

}