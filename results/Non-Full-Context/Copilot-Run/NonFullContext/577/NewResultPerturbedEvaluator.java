package liquibase.database.core;
 
 import liquibase.CatalogAndSchema;
 import liquibase.GlobalConfiguration;
 import liquibase.Scope;
 import liquibase.changelog.column.LiquibaseColumn;
 import liquibase.database.AbstractJdbcDatabase;
 import liquibase.database.DatabaseConnection;
 import liquibase.database.ObjectQuotingStrategy;
 import liquibase.database.jvm.JdbcConnection;
 import liquibase.exception.DatabaseException;
 import liquibase.executor.ExecutorService;
 import liquibase.logging.Logger;
 import liquibase.statement.SqlStatement;
 import liquibase.statement.core.RawCallStatement;
 import liquibase.statement.core.RawSqlStatement;
 import liquibase.structure.DatabaseObject;
 import liquibase.structure.core.Table;
 import liquibase.util.JdbcUtil;
 import liquibase.util.StringUtil;
 
 import java.math.BigInteger;
 import java.nio.charset.Charset;
 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.sql.Statement;
 import java.util.*;
 
 /**
  * Encapsulates PostgreSQL database support.
  */
 public class PostgresDatabase extends AbstractJdbcDatabase {
     private String dbFullVersion=null;
     public static final String PRODUCT_NAME = "PostgreSQL";
     public static final int MINIMUM_DBMS_MAJOR_VERSION = 9;
     public static final int MINIMUM_DBMS_MINOR_VERSION = 2;
 
     /**
      * Maximum length of PostgresSQL identifier.
      * For details see https://www.postgresql.org/docs/current/sql-syntax-lexical.html#SQL-SYNTAX-IDENTIFIERS.
      */
     static final int PGSQL_PK_BYTES_LIMIT = 63;
     static final String PGSQL_PK_SUFFIX = "_pkey";
 
     private static final int PGSQL_DEFAULT_TCP_PORT_NUMBER = 5432;
     private static final Logger LOG = Scope.getCurrentScope().getLog(PostgresDatabase.class);
 
     private Set<String> systemTablesAndViews = new HashSet<>();
 
     private Set<String> reservedWords = new HashSet<>();
 
     public PostgresDatabase() {
         super.setCurrentDateTimeFunction("NOW()");
         // "Reserved" or "reserved (can be function or type)" in PostgreSQL
         // from https://www.postgresql.org/docs/9.6/static/sql-keywords-appendix.html
         reservedWords.addAll(Arrays.asList("ALL", "ANALYSE", "ANALYZE", "AND", "ANY", "ARRAY", "AS", "ASC",
                 "ASYMMETRIC", "AUTHORIZATION", "BINARY", "BOTH", "CASE", "CAST", "CHECK", "COLLATE", "COLLATION",
                 "COLUMN", "CONCURRENTLY", "CONSTRAINT", "CREATE", "CROSS", "CURRENT_CATALOG", "CURRENT_DATE",
                 "CURRENT_ROLE", "CURRENT_SCHEMA", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "DEFAULT",
                 "DEFERRABLE", "DESC", "DISTINCT", "DO", "ELSE", "END", "EXCEPT", "FALSE", "FETCH", "FOR", "FOREIGN",
                 "FREEZE", "FROM", "FULL", "GRANT", "GROUP", "HAVING", "ILIKE", "IN", "INITIALLY", "INNER", "INTERSECT",
                 "INTO", "IS", "ISNULL", "JOIN", "LATERAL", "LEADING", "LEFT", "LIKE", "LIMIT", "LOCALTIME",
                 "LOCALTIMESTAMP", "NATURAL", "NOT", "NOTNULL", "NULL", "OFFSET", "ON", "ONLY", "OR", "ORDER", "OUTER",
                 "OVERLAPS", "PLACING", "PRIMARY", "REFERENCES", "RETURNING", "RIGHT", "SELECT", "SESSION_USER",
                 "SIMILAR", "SOME", "SYMMETRIC", "TABLE", "TABLESAMPLE", "THEN", "TO", "TRAILING", "TRUE", "UNION",
                 "UNIQUE", "USER", "USING", "VARIADIC", "VERBOSE", "WHEN", "WHERE", "WINDOW", "WITH"));
         super.sequenceNextValueFunction = "nextval('%s')";
         super.sequenceCurrentValueFunction = "currval('%s')";
         super.unmodifiableDataTypes.addAll(Arrays.asList("bool", "int4", "int8", "float4", "float8", "bigserial", "serial", "oid", "bytea", "date", "timestamptz", "text", "int2[]", "int4[]", "int8[]", "float4[]", "float8[]", "bool[]", "varchar[]", "text[]", "numeric[]"));
         super.unquotedObjectsAreUppercased=false;
     }
 
     @Override
     public boolean equals(Object o) {
         // Actually, we don't need and more specific checks than the base method. This exists just to make SONAR happy.
         return super.equals(o);
     }
 
     @Override
     public int hashCode() {
         // Actually, we don't need and more specific hashing than the base method. This exists just to make SONAR happy.
         return super.hashCode();
     }
 
     @Override
     public String getShortName() {
         return "postgresql";
     }
 
     @Override
     protected String getDefaultDatabaseProductName() {
         return "PostgreSQL";
     }
 
     @Override
     public Integer getDefaultPort() {
         return PGSQL_DEFAULT_TCP_PORT_NUMBER;
     }
 
     @Override
     public Set<String> getSystemViews() {
         return systemTablesAndViews;
     }
 
     @Override
     public int getPriority() {
         return PRIORITY_DEFAULT;
     }
 
     @Override
     public boolean supportsInitiallyDeferrableColumns() {
         return true;
     }
 
     @Override
     public boolean isCorrectDatabaseImplementation(DatabaseConnection conn) throws DatabaseException {
         if (!PRODUCT_NAME.equalsIgnoreCase(conn.getDatabaseProductName())) {
             return false;
         }
 
         int majorVersion = conn.getDatabaseMajorVersion();
         int minorVersion = conn.getDatabaseMinorVersion();
 
         if ((majorVersion < MINIMUM_DBMS_MAJOR_VERSION) || ((majorVersion == MINIMUM_DBMS_MAJOR_VERSION) &&
             (minorVersion < MINIMUM_DBMS_MINOR_VERSION))) {
             LOG.warning(
                 String.format(
                     "Your PostgreSQL software version (%d.%d) seems to indicate that your software is " +
                         "older than %d.%d. This means that you might encounter strange behaviour and " +
                         "incorrect error messages.", majorVersion, minorVersion, MINIMUM_DBMS_MAJOR_VERSION, MINIMUM_DBMS_MINOR_VERSION));
             return true;
         }
 
         return true;
     }
 
     @Override
     public String getDefaultDriver(String url) {
         if (url.startsWith("jdbc:postgresql:")) {
             return "org.postgresql.Driver";
         }
         return null;
     }
 
     @Override
     public boolean supportsCatalogInObjectName(Class<? extends DatabaseObject> type) {
         return false;
     }
 
     @Override
     public boolean supportsSequences() {
         return true;
     }
 
     @Override
     public String getDatabaseChangeLogTableName() {
         return super.getDatabaseChangeLogTableName().toLowerCase(Locale.US);
     }
 
     @Override
     public String getDatabaseChangeLogLockTableName() {
         return super.getDatabaseChangeLogLockTableName().toLowerCase(Locale.US);
     }
 
     @Override
     public void setConnection(DatabaseConnection conn) {
         super.setConnection(conn);
 
 
 
         if (conn instanceof JdbcConnection) {
             Statement statement = null;
             ResultSet resultSet = null;
             try {
                 statement = ((JdbcConnection) conn).createStatement();
                 resultSet = statement.executeQuery("select setting from pg_settings where name = 'edb_redwood_date'");
                 if (resultSet.next()) {
                     String setting = resultSet.getString(1);
                     if ((setting != null) && "on".equals(setting)) {
                         LOG.warning("EnterpriseDB " + conn.getURL() + " does not store DATE columns. Instead, it auto-converts " +
                                 "them " +
                                 "to TIMESTAMPs. (edb_redwood_date=true)");
                     }
                 }
             } catch (SQLException | DatabaseException e) {
                 LOG.info("Cannot check pg_settings", e);
             } finally {
                 JdbcUtil.close(resultSet, statement);
             }
         }
 
     }
 
     @Override
     public String unescapeDataTypeName(String dataTypeName) {
         return dataTypeName.replace("\"", "");
     }
 
     @Override
     public boolean isSystemObject(DatabaseObject example) {
         // All tables in the schemas pg_catalog and pg_toast are definitely system tables.
         if
                 (
                 (example instanceof Table)
                         && (example.getSchema() != null)
                         && (
                         ("pg_catalog".equals(example.getSchema().getName()))
                                 || ("pg_toast".equals(example.getSchema().getName()))
                 )
                 ) {
             return true;
         }
 
         return super.isSystemObject(example);
     }
 
     @Override
     public boolean supportsTablespaces() {
         return true;
     }
 
     @Override
     public String getAutoIncrementClause() {
         try {
             if (getDatabaseMajorVersion() < 10) {
                 return "";
             }
         } catch (DatabaseException e) {
             return "";
         }
         return super.getAutoIncrementClause();
     }
 
     @Override
     protected String getAutoIncrementClause(final String generationType, final Boolean defaultOnNull) {
         try {
             if (getDatabaseMajorVersion() < 10) {
                 return "";
             }
         } catch (DatabaseException e) {
             return "";
         }
 
         if (StringUtil.isEmpty(generationType)) {
             return super.getAutoIncrementClause();
         }
 
         String autoIncrementClause = "GENERATED %s AS IDENTITY"; // %s -- [ ALWAYS | BY DEFAULT ]
         return String.format(autoIncrementClause, generationType);
     }
 
     @Override
     public boolean generateAutoIncrementStartWith(BigInteger startWith) {
         try {
             if (getDatabaseMajorVersion() < 10) {
                 return false;
             }
         } catch (DatabaseException e) {
             return false;
         }
         return super.generateAutoIncrementStartWith(startWith);
     }
 
     @Override
     public boolean generateAutoIncrementBy(BigInteger incrementBy) {
         try {
             if (getDatabaseMajorVersion() < 10) {
                 return false;
             }
         } catch (DatabaseException e) {
             return false;
         }
         return super.generateAutoIncrementBy(incrementBy);
     }
 
 
/** Skips quoting the columns name of type "LiquibaseColumn" */

public String escapeObjectName(String objectName, Class<? extends DatabaseObject> objectType) {
    if (objectType == LiquibaseColumn.class) {
        return objectName;
    } else {
        return super.escapeObjectName(objectName, objectType);
    }
}
 

}