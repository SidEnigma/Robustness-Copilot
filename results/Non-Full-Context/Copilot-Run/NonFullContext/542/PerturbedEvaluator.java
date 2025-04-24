package liquibase.change;
 
 import liquibase.change.core.LoadDataColumnConfig;
 import liquibase.database.Database;
 import liquibase.database.DatabaseFactory;
 import liquibase.database.DatabaseList;
 import liquibase.exception.UnexpectedLiquibaseException;
 import liquibase.exception.ValidationErrors;
 import liquibase.serializer.LiquibaseSerializable;
 import liquibase.sqlgenerator.SqlGeneratorFactory;
 import liquibase.statement.DatabaseFunction;
 import liquibase.statement.SequenceNextValueFunction;
 import liquibase.statement.SqlStatement;
 import liquibase.util.ObjectUtil;
 import liquibase.util.StringUtil;
 
 import java.beans.PropertyDescriptor;
 import java.lang.reflect.Method;
 import java.lang.reflect.ParameterizedType;
 import java.lang.reflect.Type;
 import java.math.BigInteger;
 import java.util.*;
 
 /**
  * Static metadata about a {@link Change} parameter.
  * Instances of this class are tracked within {@link ChangeMetaData} and are immutable.
  */
 public class ChangeParameterMetaData {
 
     public static final String COMPUTE = "COMPUTE";
     public static final String ALL = "all";
     public static final String NONE = "none";
 
     private Change change;
     private String parameterName;
     private String description;
     private Map<String, Object> exampleValues;
     private String displayName;
     private String dataType;
     private Class dataTypeClass;
     private Type[] dataTypeClassParameters = new Type[0];
     private String since;
     private Set<String> requiredForDatabase;
     private Set<String> supportedDatabases;
     private String mustEqualExisting;
     private LiquibaseSerializable.SerializationType serializationType;
     private String[] requiredForDatabaseArg;
     private String[] supportedDatabasesArg;
     private Optional<Method> readMethodRef = Optional.empty();
     private Optional<Method> writeMethodRef = Optional.empty();
 
     public ChangeParameterMetaData(Change change, String parameterName, String displayName, String description,
                                    Map<String, Object> exampleValues, String since, Type dataType,
                                    String[] requiredForDatabase, String[] supportedDatabases, String mustEqualExisting,
                                    LiquibaseSerializable.SerializationType serializationType) {
         if (parameterName == null) {
             throw new UnexpectedLiquibaseException("Unexpected null parameterName");
         }
         if (parameterName.contains(" ")) {
             throw new UnexpectedLiquibaseException("Unexpected space in parameterName");
         }
         if (displayName == null) {
             throw new UnexpectedLiquibaseException("Unexpected null displayName");
         }
         if (dataType == null) {
             throw new UnexpectedLiquibaseException("Unexpected null dataType");
         }
 
         this.change = change;
         this.parameterName = parameterName;
         this.displayName = displayName;
         this.description = description;
         this.exampleValues = exampleValues;
         if (dataType instanceof Class) {
             this.dataType = StringUtil.lowerCaseFirst(((Class) dataType).getSimpleName());
             this.dataTypeClass = (Class) dataType;
         } else if (dataType instanceof ParameterizedType) {
             this.dataType = StringUtil.lowerCaseFirst(
                     ((Class) ((ParameterizedType) dataType).getRawType()).getSimpleName() +
                             " of " +
                             StringUtil.lowerCaseFirst(
                                     ((Class) ((ParameterizedType) dataType).getActualTypeArguments()[0]).getSimpleName()
                             )
             );
             this.dataTypeClass = (Class) ((ParameterizedType) dataType).getRawType();
             this.dataTypeClassParameters = ((ParameterizedType) dataType).getActualTypeArguments();
         }
 
         this.mustEqualExisting = mustEqualExisting;
         this.serializationType = serializationType;
         this.since = since;
 
         this.supportedDatabasesArg = supportedDatabases;
         this.requiredForDatabaseArg = requiredForDatabase;
     }
 
     public ChangeParameterMetaData withAccessors(Method readMethod, Method writeMethod) {
         this.readMethodRef = Optional.ofNullable(readMethod);
         this.writeMethodRef = Optional.ofNullable(writeMethod);
         return this;
     }
 
     protected Set<String> analyzeSupportedDatabases(String[] supportedDatabases) {
         if (supportedDatabases == null) {
             supportedDatabases = new String[]{COMPUTE};
         }
 
         Set<String> computedDatabases = new HashSet<>();
 
         if ((supportedDatabases.length == 1)
                 && StringUtil.join(supportedDatabases, ",").equals(COMPUTE)) {
             int validDatabases = 0;
             for (Database database : DatabaseFactory.getInstance().getImplementedDatabases()) {
                 if ((database.getShortName() == null) || "unsupported".equals(database.getShortName())) {
                     continue;
                 }
                 if (!change.supports(database)) {
                     continue;
                 }
                 try {
                     if (!change.generateStatementsVolatile(database)) {
                         Change testChange = change.getClass().getConstructor().newInstance();
                         ValidationErrors originalErrors = getStatementErrors(testChange, database);
                         this.setValue(testChange, this.getExampleValue(database));
                         ValidationErrors finalErrors = getStatementErrors(testChange, database);
                         if (finalErrors.getUnsupportedErrorMessages().isEmpty() || (finalErrors
                                 .getUnsupportedErrorMessages().size() == originalErrors.getUnsupportedErrorMessages()
                                 .size())) {
                             computedDatabases.add(database.getShortName());
                         }
                         validDatabases++;
                     }
                 } catch (Exception ignore) {
                     // Do nothing
                 }
             }
 
             if (validDatabases == 0) {
                 return new HashSet<>(Arrays.asList(ALL));
             } else if (computedDatabases.size() == validDatabases) {
                 computedDatabases = new HashSet<>(Arrays.asList(ALL));
             }
 
             computedDatabases.remove(NONE);
 
             return computedDatabases;
         } else {
             return new HashSet<>(Arrays.asList(supportedDatabases));
         }
     }
 
 
     protected Set<String> analyzeRequiredDatabases(String[] requiredDatabases) {
         if (requiredDatabases == null) {
             requiredDatabases = new String[]{COMPUTE};
         }
 
         Set<String> computedDatabases = new HashSet<>();
 
         if ((requiredDatabases.length == 1)
                 && StringUtil.join(requiredDatabases, ",").equals(COMPUTE)) {
             int validDatabases = 0;
             for (Database database : DatabaseFactory.getInstance().getImplementedDatabases()) {
                 try {
                     if (!change.generateStatementsVolatile(database)) {
                         Change testChange = change.getClass().getConstructor().newInstance();
                         ValidationErrors originalErrors = getStatementErrors(testChange, database);
                         this.setValue(testChange, this.getExampleValue(database));
                         ValidationErrors finalErrors = getStatementErrors(testChange, database);
                         if (!originalErrors.getRequiredErrorMessages().isEmpty() && (finalErrors
                                 .getRequiredErrorMessages().size() < originalErrors.getRequiredErrorMessages().size())
                         ) {
                             computedDatabases.add(database.getShortName());
                         }
                         validDatabases++;
                     }
                 } catch (Exception ignore) {
                     // Do nothing
                 }
             }
 
             if (validDatabases == 0) {
                 return new HashSet<>();
             } else if (computedDatabases.size() == validDatabases) {
                 computedDatabases = new HashSet<>(Arrays.asList(ALL));
             }
 
             computedDatabases.remove(NONE);
         } else {
             computedDatabases = new HashSet<>(Arrays.asList(requiredDatabases));
         }
         computedDatabases.remove(NONE);
         return computedDatabases;
     }
 
     private static ValidationErrors getStatementErrors(Change testChange, Database database) {
         ValidationErrors errors = new ValidationErrors();
         SqlStatement[] statements = testChange.generateStatements(database);
         for (SqlStatement statement : statements) {
             errors.addAll(SqlGeneratorFactory.getInstance().validate(statement, database));
         }
         return errors;
     }
 
     /**
      * Programmatic Name of the parameter. Will not contain spaces so it can be used for XMl tag names etc.
      * By convention, Change names should start be camel case starting with a lower case letter.
      */
     public String getParameterName() {
         return parameterName;
     }
 
     /**
      * A more friendly name of the parameter.
      */
     public String getDisplayName() {
         return displayName;
     }
 
     public String getSince() {
         return since;
     }
 
     /**
      * Return the data type of value stored in this parameter. Used for documentation and integration purposes as well
      * as validation.
      */
     public String getDataType() {
         return dataType;
     }
 
     public Class getDataTypeClass() {
         return dataTypeClass;
     }
 
     public Type[] getDataTypeClassParameters() {
         return dataTypeClassParameters;
     }
 
     /**
      * Return the database types for which this parameter is required. The strings returned correspond to the values
      * returned by {@link liquibase.database.Database#getShortName()}.
      * If the parameter is required for all datatabases, this will return the string "all" as an element.
      * If the parameter is required for no databases, this will return an empty set. Passing the string "none" to the
      * constructor also results in an empty set.
      * This method will never return a null value
      */
     public Set<String> getRequiredForDatabase() {
         if (requiredForDatabase == null) {
             requiredForDatabase = Collections.unmodifiableSet(analyzeRequiredDatabases(requiredForDatabaseArg));
         }
         return requiredForDatabase;
     }
 
     public Set<String> getSupportedDatabases() {
         if (supportedDatabases == null) {
             supportedDatabases = Collections.unmodifiableSet(analyzeSupportedDatabases(supportedDatabasesArg));
         }
         return supportedDatabases;
     }
 
 
/** Return true whether the database contains the string "all" or the getShortName method is contained within it. */
 public boolean isRequiredFor(Database database){}

 

}