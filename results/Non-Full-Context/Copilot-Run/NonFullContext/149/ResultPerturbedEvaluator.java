package liquibase.diff.output.changelog;
 
 import liquibase.GlobalConfiguration;
 import liquibase.Scope;
 import liquibase.change.Change;
 import liquibase.change.core.*;
 import liquibase.changelog.ChangeSet;
 import liquibase.configuration.core.DeprecatedConfigurationValueProvider;
 import liquibase.database.*;
 import liquibase.database.core.*;
 import liquibase.diff.DiffResult;
 import liquibase.diff.ObjectDifferences;
 import liquibase.diff.compare.CompareControl;
 import liquibase.diff.output.DiffOutputControl;
 import liquibase.exception.DatabaseException;
 import liquibase.exception.UnexpectedLiquibaseException;
 import liquibase.executor.Executor;
 import liquibase.executor.ExecutorService;
 import liquibase.serializer.ChangeLogSerializer;
 import liquibase.serializer.ChangeLogSerializerFactory;
 import liquibase.snapshot.DatabaseSnapshot;
 import liquibase.snapshot.EmptyDatabaseSnapshot;
 import liquibase.statement.core.RawSqlStatement;
 import liquibase.structure.DatabaseObject;
 import liquibase.structure.DatabaseObjectComparator;
 import liquibase.structure.core.Column;
 import liquibase.structure.core.StoredDatabaseLogic;
 import liquibase.util.DependencyUtil;
 import liquibase.util.StringUtil;
 
 import javax.xml.parsers.ParserConfigurationException;
 import java.io.*;
 import java.text.SimpleDateFormat;
 import java.util.*;
 
 public class DiffToChangeLog {
 
     public static final String ORDER_ATTRIBUTE = "order";
     public static final String DATABASE_CHANGE_LOG_CLOSING_XML_TAG = "</databaseChangeLog>";
     public static final String EXTERNAL_FILE_DIR_SCOPE_KEY = "DiffToChangeLog.externalFilesDir";
     public static final String DIFF_OUTPUT_CONTROL_SCOPE_KEY = "diffOutputControl";
     public static final String DIFF_SNAPSHOT_DATABASE = "snapshotDatabase";
 
     private String idRoot = String.valueOf(new Date().getTime());
     private boolean overriddenIdRoot;
 
     private int changeNumber = 1;
 
     private String changeSetContext;
     private String changeSetAuthor;
     private String changeSetPath;
     private DiffResult diffResult;
     private DiffOutputControl diffOutputControl;
     private boolean tryDbaDependencies = true;
 
     private static Set<Class> loggedOrderFor = new HashSet<>();
 
     public DiffToChangeLog(DiffResult diffResult, DiffOutputControl diffOutputControl) {
         this.diffResult = diffResult;
         this.diffOutputControl = diffOutputControl;
         respectSchemaAndCatalogCaseIfNeeded(diffOutputControl);
     }
 
     private void respectSchemaAndCatalogCaseIfNeeded(DiffOutputControl diffOutputControl) {
         if (this.diffResult.getComparisonSnapshot().getDatabase() instanceof AbstractDb2Database) {
             diffOutputControl.setRespectSchemaAndCatalogCase(true);
         }
     }
 
     public DiffToChangeLog(DiffOutputControl diffOutputControl) {
         this.diffOutputControl = diffOutputControl;
     }
 
     public void setDiffResult(DiffResult diffResult) {
         this.diffResult = diffResult;
     }
 
     public void setChangeSetContext(String changeSetContext) {
         this.changeSetContext = changeSetContext;
     }
 
     public void print(String changeLogFile) throws ParserConfigurationException, IOException, DatabaseException {
         this.changeSetPath = changeLogFile;
         ChangeLogSerializer changeLogSerializer = ChangeLogSerializerFactory.getInstance().getSerializer(changeLogFile);
         this.print(changeLogFile, changeLogSerializer);
     }
 
     public void print(PrintStream out) throws ParserConfigurationException, IOException, DatabaseException {
         this.print(out, ChangeLogSerializerFactory.getInstance().getSerializer("xml"));
     }
 
     public void print(String changeLogFile, ChangeLogSerializer changeLogSerializer) throws ParserConfigurationException, IOException, DatabaseException {
         this.changeSetPath = changeLogFile;
         File file = new File(changeLogFile);
 
         final Map<String, Object> newScopeObjects = new HashMap<>();
 
         File objectsDir = null;
         if (changeLogFile.toLowerCase().endsWith("sql")) {
             DeprecatedConfigurationValueProvider.setData("liquibase.pro.sql.inline", "true");
         } else if (this.diffResult.getComparisonSnapshot() instanceof EmptyDatabaseSnapshot) {
             objectsDir = new File(file.getParentFile(), "objects");
         } else {
             objectsDir = new File(file.getParentFile(), "objects-" + new Date().getTime());
         }
 
         if (objectsDir != null) {
             if (objectsDir.exists()) {
                 throw new UnexpectedLiquibaseException("The generatechangelog command would overwrite your existing stored logic files. To run this command please remove or rename the '"+objectsDir.getCanonicalPath()+"' dir in your local project directory");
             }
             newScopeObjects.put(EXTERNAL_FILE_DIR_SCOPE_KEY, objectsDir);
         }
 
 
         newScopeObjects.put(DIFF_OUTPUT_CONTROL_SCOPE_KEY, diffOutputControl);
 
         try {
             //
             // Get a Database instance and save it in the scope for later use
             //
             DatabaseSnapshot snapshot = diffResult.getReferenceSnapshot();
             Database database = determineDatabase(diffResult.getReferenceSnapshot());
             if (database == null) {
                 database = determineDatabase(diffResult.getComparisonSnapshot());
             }
             newScopeObjects.put(DIFF_SNAPSHOT_DATABASE, database);
             Scope.child(newScopeObjects, new Scope.ScopedRunner() {
                 @Override
                 public void run() {
                     try {
                         if (!file.exists()) {
                             //print changeLog only if there are available changeSets to print instead of printing it always
                             printNew(changeLogSerializer, file);
                         } else {
                             Scope.getCurrentScope().getLog(getClass()).info(file + " exists, appending");
                             ByteArrayOutputStream out = new ByteArrayOutputStream();
                             print(new PrintStream(out, true, GlobalConfiguration.OUTPUT_FILE_ENCODING.getCurrentValue()), changeLogSerializer);
 
                             String xml = new String(out.toByteArray(), GlobalConfiguration.OUTPUT_FILE_ENCODING.getCurrentValue());
                             String innerXml = xml.replaceFirst("(?ms).*<databaseChangeLog[^>]*>", "");
 
                             innerXml = innerXml.replaceFirst(DATABASE_CHANGE_LOG_CLOSING_XML_TAG, "");
                             innerXml = innerXml.trim();
                             if ("".equals(innerXml)) {
                                 Scope.getCurrentScope().getLog(getClass()).info("No changes found, nothing to do");
                                 return;
                             }
 
                             try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
 
                                 String line;
                                 long offset = 0;
                                 boolean foundEndTag = false;
                                 while ((line = randomAccessFile.readLine()) != null) {
                                     int index = line.indexOf(DATABASE_CHANGE_LOG_CLOSING_XML_TAG);
                                     if (index >= 0) {
                                         foundEndTag = true;
                                         break;
                                     } else {
                                         offset = randomAccessFile.getFilePointer();
                                     }
                                 }
 
                                 String lineSeparator = GlobalConfiguration.OUTPUT_LINE_SEPARATOR.getCurrentValue();
 
                                 if (foundEndTag) {
                                     randomAccessFile.seek(offset);
                                     randomAccessFile.writeBytes("    ");
                                     randomAccessFile.write(innerXml.getBytes(GlobalConfiguration.OUTPUT_FILE_ENCODING.getCurrentValue()));
                                     randomAccessFile.writeBytes(lineSeparator);
                                     randomAccessFile.writeBytes(DATABASE_CHANGE_LOG_CLOSING_XML_TAG + lineSeparator);
                                 } else {
                                     randomAccessFile.seek(0);
                                     long length = randomAccessFile.length();
                                     randomAccessFile.seek(length);
                                     randomAccessFile.write(xml.getBytes(GlobalConfiguration.OUTPUT_FILE_ENCODING.getCurrentValue()));
                                 }
                             }
 
                         }
                     } catch (Exception e) {
                         throw new RuntimeException(e);
                     }
                 }
             });
         } catch (Exception e) {
             //rethrow known exceptions. TODO: Fix this up with final Scope API
             final Throwable cause = e.getCause();
             if (cause instanceof ParserConfigurationException) {
                 throw (ParserConfigurationException) cause;
             }
             if (cause instanceof IOException) {
                 throw (IOException) cause;
             }
             if (cause instanceof DatabaseException) {
                 throw (DatabaseException) cause;
             }
 
             throw new RuntimeException(e);
         }
     }
 
     //
     // Return the Database from this snapshot
     // if it is not offline
     //
     private Database determineDatabase(DatabaseSnapshot snapshot) {
         Database database = snapshot.getDatabase();
         DatabaseConnection connection = database.getConnection();
         if (! (connection instanceof OfflineConnection) && database instanceof PostgresDatabase) {
             return database;
         }
         return null;
     }
 
     /**
      * Prints changeLog that would bring the target database to be the same as
      * the reference database
      */
     public void printNew(ChangeLogSerializer changeLogSerializer, File file) throws ParserConfigurationException, IOException, DatabaseException {
 
         List<ChangeSet> changeSets = generateChangeSets();
 
         Scope.getCurrentScope().getLog(getClass()).info("changeSets count: " + changeSets.size());
         if (changeSets.isEmpty()) {
             Scope.getCurrentScope().getLog(getClass()).info("No changesets to add.");
         } else {
             Scope.getCurrentScope().getLog(getClass()).info(file + " does not exist, creating and adding " + changeSets.size() + " changesets.");
         }
 
         try (FileOutputStream stream = new FileOutputStream(file);
              PrintStream out = new PrintStream(stream, true, GlobalConfiguration.OUTPUT_FILE_ENCODING.getCurrentValue())) {
             changeLogSerializer.write(changeSets, out);
         }
     }
 
     /**
      * Prints changeLog that would bring the target database to be the same as
      * the reference database
      */
     public void print(final PrintStream out, final ChangeLogSerializer changeLogSerializer) throws ParserConfigurationException, IOException, DatabaseException {
         List<ChangeSet> changeSets = generateChangeSets();
 
         changeLogSerializer.write(changeSets, out);
 
         out.flush();
     }
 
     public List<ChangeSet> generateChangeSets() {
         final ChangeGeneratorFactory changeGeneratorFactory = ChangeGeneratorFactory.getInstance();
         DatabaseObjectComparator comparator = new DatabaseObjectComparator();
 
         String created = null;
         if (GlobalConfiguration.GENERATE_CHANGESET_CREATED_VALUES.getCurrentValue()) {
             created = new SimpleDateFormat("yyyy-MM-dd HH:mmZ").format(new Date());
         }
 
         List<Class<? extends DatabaseObject>> types = getOrderedOutputTypes(ChangedObjectChangeGenerator.class);
         List<ChangeSet> updateChangeSets = new ArrayList<ChangeSet>();
 
         // Keep a reference to DiffResult in the comparision database so that it can be retrieved later
         // This is to avoid changing the MissingObjectChangeGenerator API and still be able to pass the
         // initial DiffResult Object which can be used to check for the objects available in the database
         // without doing any expensive db calls. Example usage is in MissingUniqueConstraintChangeGenerator#alreadyExists()
         Database comparisionDatabase = diffResult.getComparisonSnapshot().getDatabase();
         if (comparisionDatabase instanceof AbstractJdbcDatabase) {
             ((AbstractJdbcDatabase) comparisionDatabase).set("diffResult", diffResult);
         }
 
         for (Class<? extends DatabaseObject> type : types) {
             ObjectQuotingStrategy quotingStrategy = diffOutputControl.getObjectQuotingStrategy();
             for (Map.Entry<? extends DatabaseObject, ObjectDifferences> entry : diffResult.getChangedObjects(type, comparator).entrySet()) {
                 if (!diffResult.getReferenceSnapshot().getDatabase().isLiquibaseObject(entry.getKey()) && !diffResult.getReferenceSnapshot().getDatabase().isSystemObject(entry.getKey())) {
                     Change[] changes = changeGeneratorFactory.fixChanged(entry.getKey(), entry.getValue(), diffOutputControl, diffResult.getReferenceSnapshot().getDatabase(), diffResult.getComparisonSnapshot().getDatabase());
                     addToChangeSets(changes, updateChangeSets, quotingStrategy, created);
                 }
             }
         }
 
         types = getOrderedOutputTypes(MissingObjectChangeGenerator.class);
         List<DatabaseObject> missingObjects = new ArrayList<DatabaseObject>();
         for (Class<? extends DatabaseObject> type : types) {
             for (DatabaseObject object : diffResult.getMissingObjects(type, getDbObjectComparator())) {
                 if (object == null) {
                     continue;
                 }
                 if (!diffResult.getReferenceSnapshot().getDatabase().isLiquibaseObject(object) && !diffResult.getReferenceSnapshot().getDatabase().isSystemObject(object)) {
                     missingObjects.add(object);
                 }
             }
         }
 
         List<ChangeSet> createChangeSets = new ArrayList<ChangeSet>();
 
         for (DatabaseObject object : sortMissingObjects(missingObjects, diffResult.getReferenceSnapshot().getDatabase())) {
             ObjectQuotingStrategy quotingStrategy = diffOutputControl.getObjectQuotingStrategy();
 
             Change[] changes = changeGeneratorFactory.fixMissing(object, diffOutputControl, diffResult.getReferenceSnapshot().getDatabase(), diffResult.getComparisonSnapshot().getDatabase());
             addToChangeSets(changes, createChangeSets, quotingStrategy, created);
         }
 
         List<ChangeSet> deleteChangeSets = new ArrayList<ChangeSet>();
 
         types = getOrderedOutputTypes(UnexpectedObjectChangeGenerator.class);
         for (Class<? extends DatabaseObject> type : types) {
             ObjectQuotingStrategy quotingStrategy = diffOutputControl.getObjectQuotingStrategy();
             for (DatabaseObject object : sortUnexpectedObjects(diffResult.getUnexpectedObjects(type, comparator), diffResult.getReferenceSnapshot().getDatabase())) {
                 if (!diffResult.getComparisonSnapshot().getDatabase().isLiquibaseObject(object) && !diffResult.getComparisonSnapshot().getDatabase().isSystemObject(object)) {
                     Change[] changes = changeGeneratorFactory.fixUnexpected(object, diffOutputControl, diffResult.getReferenceSnapshot().getDatabase(), diffResult.getComparisonSnapshot().getDatabase());
                     addToChangeSets(changes, deleteChangeSets, quotingStrategy, created);
                 }
             }
         }
         // remove the diffResult from the database object
         if (comparisionDatabase instanceof AbstractJdbcDatabase) {
             ((AbstractJdbcDatabase) comparisionDatabase).set("diffResult", null);
         }
 
 
         List<ChangeSet> changeSets = new ArrayList<ChangeSet>();
         changeSets.addAll(createChangeSets);
         changeSets.addAll(deleteChangeSets);
         changeSets.addAll(updateChangeSets);
         return changeSets;
     }
 
     private DatabaseObjectComparator getDbObjectComparator() {
         return new DatabaseObjectComparator() {
             @Override
             public int compare(DatabaseObject o1, DatabaseObject o2) {
                 if (o1 instanceof Column && o1.getAttribute(ORDER_ATTRIBUTE, Integer.class) != null && o2.getAttribute(ORDER_ATTRIBUTE, Integer.class) != null) {
                     int i = o1.getAttribute(ORDER_ATTRIBUTE, Integer.class).compareTo(o2.getAttribute(ORDER_ATTRIBUTE, Integer.class));
                     if (i != 0) {
                         return i;
                     }
                 } else if (o1 instanceof StoredDatabaseLogic && o1.getAttribute(ORDER_ATTRIBUTE, Integer.class) != null
                         && o2.getAttribute(ORDER_ATTRIBUTE, Integer.class) != null) {
                     int order = o1.getAttribute(ORDER_ATTRIBUTE, Long.class).compareTo(o2.getAttribute(ORDER_ATTRIBUTE, Long.class));
                     if (order != 0) {
                         return order;
                     }
                 }
                 return super.compare(o1, o2);
 
             }
         };
     }
 
     private List<DatabaseObject> sortUnexpectedObjects(Collection<? extends DatabaseObject> unexpectedObjects, Database database) {
         return sortObjects("unexpected", (Collection<DatabaseObject>) unexpectedObjects, database);
     }
 
     private List<DatabaseObject> sortMissingObjects(Collection<DatabaseObject> missingObjects, Database database) {
         return sortObjects("missing", missingObjects, database);
     }
 
     private List<DatabaseObject> sortObjects(final String type, Collection<DatabaseObject> objects, Database database) {
 
         if (!objects.isEmpty() && supportsSortingObjects(database) && (database.getConnection() != null) && !(database.getConnection() instanceof OfflineConnection)) {
             List<String> schemas = new ArrayList<>();
             CompareControl.SchemaComparison[] schemaComparisons = this.diffOutputControl.getSchemaComparisons();
             if (schemaComparisons != null) {
                 for (CompareControl.SchemaComparison comparison : schemaComparisons) {
                     String schemaName = comparison.getReferenceSchema().getSchemaName();
                     if (schemaName == null) {
                         schemaName = database.getDefaultSchemaName();
                     }
                     schemas.add(schemaName);
                 }
             }
 
             if (schemas.isEmpty()) {
                 schemas.add(database.getDefaultSchemaName());
             }
 
             try {
                 final List<String> dependencyOrder = new ArrayList<>();
                 DependencyUtil.NodeValueListener<String> nameListener = new DependencyUtil.NodeValueListener<String>() {
                     @Override
                     public void evaluating(String nodeValue) {
                         dependencyOrder.add(nodeValue);
                     }
                 };
 
                 DependencyUtil.DependencyGraph<String> graph = new DependencyUtil.DependencyGraph<String>(nameListener);
                 addDependencies(graph, schemas, database);
                 graph.computeDependencies();
 
                 if (!dependencyOrder.isEmpty()) {
 
                     final List<DatabaseObject> toSort = new ArrayList<>();
                     final List<DatabaseObject> toNotSort = new ArrayList<>();
 
                     for (DatabaseObject obj : objects) {
                         if (!(obj instanceof Column)) {
                             String schemaName = null;
                             if (obj.getSchema() != null) {
                                 schemaName = obj.getSchema().getName();
                             }
 
                             String name = schemaName + "." + obj.getName();
                             if (dependencyOrder.contains(name)) {
                                 toSort.add(obj);
                             } else {
                                 toNotSort.add(obj);
                             }
                         } else {
                             toNotSort.add(obj);
                         }
                     }
 
                     Collections.sort(toSort, new Comparator<DatabaseObject>() {
                         @Override
                         public int compare(DatabaseObject o1, DatabaseObject o2) {
                             String o1Schema = null;
                             if (o1.getSchema() != null) {
                                 o1Schema = o1.getSchema().getName();
                             }
 
                             String o2Schema = null;
                             if (o2.getSchema() != null) {
                                 o2Schema = o2.getSchema().getName();
                             }
 
                             Integer o1Order = dependencyOrder.indexOf(o1Schema + "." + o1.getName());
                             int o2Order = dependencyOrder.indexOf(o2Schema + "." + o2.getName());
 
                             int order = o1Order.compareTo(o2Order);
                             if ("unexpected".equals(type)) {
                                 order = order * -1;
                             }
                             return order;
                         }
                     });
 
                     toSort.addAll(toNotSort);
                     return toSort;
                 }
             } catch (DatabaseException e) {
                 Scope.getCurrentScope().getLog(getClass()).fine("Cannot get object dependencies: " + e.getMessage());
             }
         }
         return new ArrayList<>(objects);
     }
 
     private List<Map<String, ?>> queryForDependenciesOracle(Executor executor, List<String> schemas)
             throws DatabaseException {
         List<Map<String, ?>> rs = null;
         try {
             if (tryDbaDependencies) {
                 rs = executor.queryForList(new RawSqlStatement("select OWNER, NAME, REFERENCED_OWNER, REFERENCED_NAME from DBA_DEPENDENCIES where REFERENCED_OWNER != 'SYS' AND NOT(NAME LIKE 'BIN$%') AND NOT(OWNER = REFERENCED_OWNER AND NAME = REFERENCED_NAME) AND (" + StringUtil.join(schemas, " OR ", new StringUtil.StringUtilFormatter<String>() {
                             @Override
                             public String toString(String obj) {
                                 return "OWNER='" + obj + "'";
                             }
                         }
                 ) + ")"));
             } else {
                 rs = executor.queryForList(new RawSqlStatement("select NAME, REFERENCED_OWNER, REFERENCED_NAME from USER_DEPENDENCIES where REFERENCED_OWNER != 'SYS' AND NOT(NAME LIKE 'BIN$%') AND NOT(NAME = REFERENCED_NAME) AND (" + StringUtil.join(schemas, " OR ", new StringUtil.StringUtilFormatter<String>() {
                             @Override
                             public String toString(String obj) {
                                 return "REFERENCED_OWNER='" + obj + "'";
                             }
                         }
                 ) + ")"));
             }
         } catch (DatabaseException dbe) {
             //
             // If our exception is for something other than a missing table/view
             // then we just re-throw the exception
             // else if we can't see USER_DEPENDENCIES then we also re-throw
             //   to stop the recursion
             //
             String message = dbe.getMessage();
             if (!message.contains("ORA-00942: table or view does not exist")) {
                 throw new DatabaseException(dbe);
             } else if (!tryDbaDependencies) {
                 throw new DatabaseException(dbe);
             }
             Scope.getCurrentScope().getLog(getClass()).warning("Unable to query DBA_DEPENDENCIES table. Switching to USER_DEPENDENCIES");
             tryDbaDependencies = false;
             return queryForDependenciesOracle(executor, schemas);
         }
         return rs;
     }
 
 
/** To determine whether to go into the sorting logic used by {@link #sortMissingObjects(Collection, Database)} */
 protected boolean supportsSortingObjects(Database database){
        return true;
    }
    
        private void addDependencies(DependencyUtil.DependencyGraph<String> graph, List<String> schemas, Database database)
                throws DatabaseException {
            for (String schema : schemas) {
                graph.addNode(schema);
            }
    
            List<Map<String, ?>> rs = queryForDependenciesOracle(database.getDefaultExecutor(), schemas);
            for (Map<String, ?> row : rs) {
                String owner = (String) row.get("OWNER");
                String name = (String) row.get("NAME");
                String referencedOwner = (String) row.get("REFERENCED_OWNER");
                String referencedName = (String) row.get("REFERENCED_NAME");
    
                if (owner.equals(referencedOwner)) {
                    graph.addDependency(owner + "." + name, referencedOwner + "." + referencedName);
                } else {
                    graph.addDependency(owner + "." + name, referencedOwner + "." + referencedName);
                    graph.addDependency(referencedOwner + "." + referencedName, owner + "." + name);
                }
            }
        }
    
        private void addDependencies(DependencyUtil.DependencyGraph<String> graph, List<String> schemas, Database database,
                List<String> tables, List<String> views) throws DatabaseException {
            for (String schema : schemas) {
                graph.addNode(schema);
            }
    
            List<Map<String, ?>> rs = queryForDependenciesOracle(database.getDefaultExecutor(), schemas);
            for (Map<String, ?> row : rs) {
                String owner = (String) row.get("OWNER");
                String name = (String) row.get("NAME");
                String referencedOwner = (String) row.get("REFERENCED_OWNER");
                String referencedName = (String) row.get("REFERENCED_NAME");
    
                if (owner.equ       
 }

 

}