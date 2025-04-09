package liquibase.change;
 
 import liquibase.change.core.RawSQLChange;
 import liquibase.Scope;
 import liquibase.GlobalConfiguration;
 import liquibase.database.Database;
 import liquibase.database.core.MSSQLDatabase;
 import liquibase.database.core.PostgresDatabase;
 import liquibase.exception.DatabaseException;
 import liquibase.exception.UnexpectedLiquibaseException;
 import liquibase.exception.ValidationErrors;
 import liquibase.exception.Warnings;
 import liquibase.statement.SqlStatement;
 import liquibase.statement.core.RawSqlStatement;
 import liquibase.util.StringUtil;
 
 import java.io.*;
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * A common parent for all raw SQL related changes regardless of where the sql was sourced from.
  * 
  * Implements the necessary logic to choose how the SQL string should be parsed to generate the statements.
  *
  */
 @SuppressWarnings("java:S5998")
 public abstract class AbstractSQLChange extends AbstractChange implements DbmsTargetedChange {
 
     private boolean stripComments;
     private boolean splitStatements;
     /**
      *
      * @deprecated  To be removed when splitStatements is changed to be type Boolean
      *
      */
     private boolean splitStatementsSet;
 
     private String endDelimiter;
     private String sql;
     private String dbms;
 
     protected String encoding;
 
 
     protected AbstractSQLChange() {
         setStripComments(null);
         setSplitStatements(null);
     }
 
     public InputStream openSqlStream() throws IOException {
         return null;
     }
 
     @Override
     @DatabaseChangeProperty(since = "3.0", exampleValue = "h2, oracle")
     public String getDbms() {
         return dbms;
     }
 
     @Override
     public void setDbms(final String dbms) {
         this.dbms = dbms;
     }
 
     /**
      * {@inheritDoc}
      * @param database
      * @return always true (in AbstractSQLChange)
      */
     @Override
     public boolean supports(Database database) {
         return true;
     }
 
     @Override
     public Warnings warn(Database database) {
         return new Warnings();
     }
 
     @Override
     public ValidationErrors validate(Database database) {
         ValidationErrors validationErrors = new ValidationErrors();
         if (StringUtil.trimToNull(sql) == null) {
             validationErrors.addError("'sql' is required");
         }
         return validationErrors;
 
     }
 
     /**
      * Return if comments should be stripped from the SQL before passing it to the database.
      * <p></p>
      * This will always return a non-null value and should be a boolean rather than a Boolean, but that breaks the Bean Standard.
      */
     @DatabaseChangeProperty(description = "Set to true to remove any comments in the SQL before executing, otherwise false. Defaults to false if not set")
     public Boolean isStripComments() {
         return stripComments;
     }
 
 
     /**
      * Return true if comments should be stripped from the SQL before passing it to the database.
      * Passing null sets stripComments to the default value (false).
      */
     public void setStripComments(Boolean stripComments) {
         if (stripComments == null) {
             this.stripComments = false;
         } else {
             this.stripComments = stripComments;
         }
     }
 
     /**
      * Return if the SQL should be split into multiple statements before passing it to the database.
      * By default, statements are split around ";" and "go" delimiters.
      * <p></p>
      * This will always return a non-null value and should be a boolean rather than a Boolean, but that breaks the Bean Standard.
      */
     @DatabaseChangeProperty(description = "Set to false to not have liquibase split statements on ;'s and GO's. Defaults to true if not set")
     public Boolean isSplitStatements() {
         return splitStatements;
     }
 
     /**
      * Set whether SQL should be split into multiple statements.
      * Passing null sets stripComments to the default value (true).
      */
     public void setSplitStatements(Boolean splitStatements) {
         if (splitStatements == null) {
             this.splitStatements = true;
         } else {
             this.splitStatements = splitStatements;
             splitStatementsSet = true;
         }
     }
 
     /**
      * @deprecated  To be removed when splitStatements is changed to be Boolean type
      * @return
      */
     @Deprecated
     public boolean isSplitStatementsSet() {
         return splitStatementsSet;
     }
 
     /**
      * Return the raw SQL managed by this Change
      */
     @DatabaseChangeProperty(serializationType = SerializationType.DIRECT_VALUE)
     public String getSql() {
         return sql;
     }
 
     /**
      * Set the raw SQL managed by this Change. The passed sql is trimmed and set to null if an empty string is passed.
      */
     public void setSql(String sql) {
        this.sql = StringUtil.trimToNull(sql);
     }
 
     /**
      * Set the end delimiter used to split statements. Will return null if the default delimiter should be used.
      *
      * @see #splitStatements
      */
     @DatabaseChangeProperty(description = "Delimiter to apply to the end of the statement. Defaults to ';', may be set to ''.", exampleValue = "\\nGO")
     public String getEndDelimiter() {
         return endDelimiter;
     }
 
     /**
      * Set the end delimiter for splitting SQL statements. Set to null to use the default delimiter.
      * @param endDelimiter
      */
     public void setEndDelimiter(String endDelimiter) {
         this.endDelimiter = endDelimiter;
     }
 
     /**
      * Calculates the checksum based on the contained SQL.
      *
      * @see liquibase.change.AbstractChange#generateCheckSum()
      */
     @Override
     public CheckSum generateCheckSum() {
         InputStream stream = null;
         try {
             stream = openSqlStream();
 
             String sql = this.sql;
             if ((stream == null) && (sql == null)) {
                 sql = "";
             }
 
             if (sql != null) {
                 stream = new ByteArrayInputStream(sql.getBytes(GlobalConfiguration.OUTPUT_FILE_ENCODING.getCurrentValue())
                 );
             }
 
             return CheckSum.compute(new NormalizingStream(this.getEndDelimiter(), this.isSplitStatements(), this.isStripComments(), stream), false);
         } catch (IOException e) {
             throw new UnexpectedLiquibaseException(e);
         } finally {
             if (stream != null) {
                 try {
                     stream.close();
                 } catch (IOException e) {
                     Scope.getCurrentScope().getLog(getClass()).fine("Error closing stream", e);
                 }
             }
         }
     }
 
 
 
/** Depending on how the SQL is to be analysed, it generates one or more SqlStatements. SQL is split and each command is turned into a separate SqlStatement if split statements is set to true.  Comments are removed before the split statement is executed if the stripping of comments is true. If a connection is available the SQL set is passed through the method {@link java.sql.Connection#nativeSQL} */
 public SqlStatement[] generateStatements(Database database){
        if (sql == null) {
            return new SqlStatement[0];
        }
    
        String[] statements = sql.split(endDelimiter);
        SqlStatement[] sqlStatements = new SqlStatement[statements.length];
        for (int i = 0; i < statements.length; i++) {
            String statement = statements[i];
            if (statement.trim().length() == 0) {
                continue;
            }
    
            if (isStripComments()) {
                statement = StripComments.stripComments(statement);
            }
    
            if (isSplitStatements()) {
                statement = SplitStatements.splitStatements(statement);
            }
    
            sqlStatements[i] = new RawSqlStatement(statement);
        }
    
        return sqlStatements;
    }
    
        @Override
        public String getConfirmationMessage() {
            return "SQL executed";
        }
    
        @Override
        public String getSerializedObjectNamespace() {
            return STANDARD_CHANGE_NAMESPACE;
        }
    
        @Override
        public String getSerializedObjectName() {
            return "sql";
        }
    
        @Override
        public String getSerializedObjectNamespace() {
            return STANDARD_CHANGE_NAMESPACE;
        }
    
        @Override
        public String getSerializedObjectName() {
            return "sql";
        }
    
        @Override
        public String getSerializedObjectNamespace() {
            return STANDARD_CHANGE_NAMESPACE;
        }
    
        @Override
        public String getSerializedObjectName() {
            return "sql";
        }
    
        @Override
        public String getSerializedObjectNamespace() {
            return STANDARD_CHANGE_NAMESPACE;
        }
    
        @Override
        public String getSerializedObjectName() {
            return "sql";
        }
    
        @Override
        public String getSerializedObjectNamespace() {
            return STANDARD_CHANGE_NAMESPACE;       
 }

 

}