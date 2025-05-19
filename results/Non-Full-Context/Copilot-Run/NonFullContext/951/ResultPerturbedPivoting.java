package liquibase.structure.core;
 
 import liquibase.structure.AbstractDatabaseObject;
 import liquibase.structure.DatabaseObject;
 import liquibase.util.StringUtil;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 
 public class PrimaryKey extends AbstractDatabaseObject {
 
     public PrimaryKey() {
         setAttribute("columns", new ArrayList());
     }
 
     public PrimaryKey(String name, String tableCatalogName, String tableSchemaName, String tableName, Column... columns) {
         this();
         setName(name);
         if (tableName != null) {
             Table table = new Table(tableCatalogName, tableSchemaName, tableName);
 
             if (columns != null) {
                 setAttribute("columns", Arrays.asList(columns));
                 for (Column column : getColumns()) {
                     column.setRelation(table);
                 }
             }
 
             setTable(table);
         }
     }
 
     @Override
     public DatabaseObject[] getContainingObjects() {
         return new DatabaseObject[] {
                 getTable()
         };
     }
 
     @Override
     public String getName() {
         return getAttribute("name", String.class);
     }
 
     @Override
     public PrimaryKey setName(String name) {
         this.setAttribute("name", name);
         return this;
     }
 
     @Override
     public Schema getSchema() {
         if (getTable() == null) {
             return null;
         }
         return getTable().getSchema();
     }
 
     public String getColumnNames() {
         return StringUtil.join(getColumns(), ", ", new StringUtil.StringUtilFormatter() {
             @Override
             public String toString(Object obj) {
                 return ((Column) obj).toString(false);
             }
         });
     }
 
 
/** Adds a new column to the list of columns for this primary key. */
 public PrimaryKey addColumn(int position, Column column){
        List<Column> columns = getColumns();
        columns.add(position, column);
        setAttribute("columns", columns);
        return this;        
 }

 

}