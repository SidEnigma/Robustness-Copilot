/*
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package tech.tablesaw.api;
 
 import static java.util.stream.Collectors.toList;
 import static tech.tablesaw.aggregate.AggregateFunctions.count;
 import static tech.tablesaw.aggregate.AggregateFunctions.countMissing;
 import static tech.tablesaw.api.QuerySupport.not;
 import static tech.tablesaw.selection.Selection.selectNRowsAtRandom;
 
 import com.google.common.base.Preconditions;
 import com.google.common.collect.Streams;
 import com.google.common.primitives.Ints;
 import io.github.classgraph.ClassGraph;
 import io.github.classgraph.ScanResult;
 import it.unimi.dsi.fastutil.ints.IntArrayList;
 import it.unimi.dsi.fastutil.ints.IntArrays;
 import it.unimi.dsi.fastutil.ints.IntComparator;
 import java.util.*;
 import java.util.function.Consumer;
 import java.util.function.Function;
 import java.util.function.IntFunction;
 import java.util.function.Predicate;
 import java.util.stream.Collectors;
 import java.util.stream.Stream;
 import org.roaringbitmap.RoaringBitmap;
 import tech.tablesaw.aggregate.AggregateFunction;
 import tech.tablesaw.aggregate.CrossTab;
 import tech.tablesaw.aggregate.PivotTable;
 import tech.tablesaw.aggregate.Summarizer;
 import tech.tablesaw.columns.Column;
 import tech.tablesaw.io.DataFrameReader;
 import tech.tablesaw.io.DataFrameWriter;
 import tech.tablesaw.io.DataReader;
 import tech.tablesaw.io.DataWriter;
 import tech.tablesaw.io.ReaderRegistry;
 import tech.tablesaw.io.WriterRegistry;
 import tech.tablesaw.joining.DataFrameJoiner;
 import tech.tablesaw.selection.BitmapBackedSelection;
 import tech.tablesaw.selection.Selection;
 import tech.tablesaw.sorting.Sort;
 import tech.tablesaw.sorting.SortUtils;
 import tech.tablesaw.sorting.comparators.IntComparatorChain;
 import tech.tablesaw.table.*;
 
 /**
  * A table of data, consisting of some number of columns, each of which has the same number of rows.
  * All the data in a column has the same type: integer, float, category, etc., but a table may
  * contain an arbitrary number of columns of any type.
  *
  * <p>Tables are the main data-type and primary focus of Tablesaw.
  */
 public class Table extends Relation implements Iterable<Row> {
 
   public static final ReaderRegistry defaultReaderRegistry = new ReaderRegistry();
   public static final WriterRegistry defaultWriterRegistry = new WriterRegistry();
 
   static {
     autoRegisterReadersAndWriters();
   }
 
   /** The columns that hold the data in this table */
   private final List<Column<?>> columnList = new ArrayList<>();
   /** The name of the table */
   private String name;
 
   // standard column names for melt and cast operations
   public static final String MELT_VARIABLE_COLUMN_NAME = "variable";
   public static final String MELT_VALUE_COLUMN_NAME = "value";
 
   /** Returns a new table */
   private Table() {}
 
   /** Returns a new table initialized with the given name */
   private Table(String name) {
     this.name = name;
   }
 
   /**
    * Returns a new Table initialized with the given names and columns
    *
    * @param name The name of the table
    * @param columns One or more columns, all of which must have either the same length or size 0
    */
   protected Table(String name, Column<?>... columns) {
     this(name);
     for (final Column<?> column : columns) {
       this.addColumns(column);
     }
   }
 
   /**
    * Returns a new Table initialized with the given names and columns
    *
    * @param name The name of the table
    * @param columns One or more columns, all of which must have either the same length or size 0
    */
   protected Table(String name, Collection<Column<?>> columns) {
     this(name);
     for (final Column<?> column : columns) {
       this.addColumns(column);
     }
   }
 
   /** TODO: Add documentation */
   private static void autoRegisterReadersAndWriters() {
     try (ScanResult scanResult =
         new ClassGraph().enableAllInfo().whitelistPackages("tech.tablesaw.io").scan()) {
       List<String> classes = new ArrayList<>();
       classes.addAll(scanResult.getClassesImplementing(DataWriter.class.getName()).getNames());
       classes.addAll(scanResult.getClassesImplementing(DataReader.class.getName()).getNames());
       for (String clazz : classes) {
         try {
           Class.forName(clazz);
         } catch (ClassNotFoundException e) {
           throw new IllegalStateException(e);
         }
       }
     }
   }
 
   /** Returns a new, empty table (without rows or columns) */
   public static Table create() {
     return new Table();
   }
 
   /** Returns a new, empty table (without rows or columns) with the given name */
   public static Table create(String tableName) {
     return new Table(tableName);
   }
 
   /**
    * Returns a new table with the given columns
    *
    * @param columns one or more columns, all of the same @code{column.size()}
    */
   public static Table create(Column<?>... columns) {
     return new Table(null, columns);
   }
 
   /**
    * Returns a new table with the given columns
    *
    * @param columns one or more columns, all of the same @code{column.size()}
    */
   public static Table create(Collection<Column<?>> columns) {
     return new Table(null, columns);
   }
 
   /**
    * Returns a new table with the given columns
    *
    * @param columns one or more columns, all of the same @code{column.size()}
    */
   public static Table create(Stream<Column<?>> columns) {
     return new Table(null, columns.collect(Collectors.toList()));
   }
 
   /**
    * Returns a new table with the given columns and given name
    *
    * @param name the name for this table
    * @param columns one or more columns, all of the same @code{column.size()}
    */
   public static Table create(String name, Column<?>... columns) {
     return new Table(name, columns);
   }
 
   /**
    * Returns a new table with the given columns and given name
    *
    * @param name the name for this table
    * @param columns one or more columns, all of the same @code{column.size()}
    */
   public static Table create(String name, Collection<Column<?>> columns) {
     return new Table(name, columns);
   }
 
   /**
    * Returns a new table with the given columns and given name
    *
    * @param name the name for this table
    * @param columns one or more columns, all of the same @code{column.size()}
    */
   public static Table create(String name, Stream<Column<?>> columns) {
     return new Table(name, columns.collect(Collectors.toList()));
   }
 
   /**
    * Returns a sort Key that can be used for simple or chained comparator sorting
    *
    * <p>You can extend the sort key by using .next() to fill more columns to the sort order
    */
   private static Sort first(String columnName, Sort.Order order) {
     return Sort.on(columnName, order);
   }
 
   /**
    * Returns an object that can be used to sort this table in the order specified for by the given
    * column names
    */
   private static Sort getSort(String... columnNames) {
     Sort key = null;
     for (String s : columnNames) {
       if (key == null) {
         key = first(s, Sort.Order.DESCEND);
       } else {
         key.next(s, Sort.Order.DESCEND);
       }
     }
     return key;
   }
 
   /** Returns an object that an be used to read data from a file into a new Table */
   public static DataFrameReader read() {
     return new DataFrameReader(defaultReaderRegistry);
   }
 
   /**
    * Returns an object that an be used to write data from a Table into a file. If the file exists,
    * it is over-written
    */
   public DataFrameWriter write() {
     return new DataFrameWriter(defaultWriterRegistry, this);
   }
 
   /** Adds the given column to this table */
   @Override
   public Table addColumns(final Column<?>... cols) {
     for (final Column<?> c : cols) {
       validateColumn(c);
       columnList.add(c);
     }
     return this;
   }
 
   /**
    * For internal Tablesaw use only
    *
    * <p>Adds the given column to this table without performing duplicate-name or column size checks
    */
   public void internalAddWithoutValidation(final Column<?> c) {
     columnList.add(c);
   }
 
   /**
    * Throws an IllegalArgumentException if a column with the given name is already in the table, or
    * if the number of rows in the column does not match the number of rows in the table
    */
   private void validateColumn(final Column<?> newColumn) {
     Preconditions.checkNotNull(
         newColumn, "Attempted to add a null to the columns in table " + name);
     List<String> stringList = new ArrayList<>();
     for (String name : columnNames()) {
       stringList.add(name.toLowerCase());
     }
     if (stringList.contains(newColumn.name().toLowerCase())) {
       String message =
           String.format(
               "Cannot add column with duplicate name %s to table %s", newColumn.name(), name);
       throw new IllegalArgumentException(message);
     }
 
     checkColumnSize(newColumn);
   }
 
   /**
    * Throws an IllegalArgumentException if the column size doesn't match the rowCount() for the
    * table
    */
   private void checkColumnSize(Column<?> newColumn) {
     if (columnCount() != 0) {
       Preconditions.checkArgument(
           newColumn.size() == rowCount(),
           "Column "
               + newColumn.name()
               + " does not have the same number of rows as the other columns in the table.");
     }
   }
 
   /**
    * Adds the given column to this table at the given position in the column list
    *
    * @param index Zero-based index into the column list
    * @param column Column to be added
    */
   public Table insertColumn(int index, Column<?> column) {
     validateColumn(column);
     columnList.add(index, column);
     return this;
   }
 
   /**
    * Return a new table (shallow copy) that contains all the columns in this table, in the order
    * given in the argument. Throw an IllegalArgument exception if the number of names given does not
    * match the number of columns in this table. NOTE: This does not make a copy of the columns, so
    * they are shared between the two tables.
    *
    * @param columnNames a column name or array of names
    */
   public Table reorderColumns(String... columnNames) {
     Preconditions.checkArgument(columnNames.length == columnCount());
     Table table = Table.create(name);
     for (String name : columnNames) {
       table.addColumns(column(name));
     }
     return table;
   }
 
   /**
    * Replaces an existing column (by index) in this table with the given new column
    *
    * @param colIndex Zero-based index of the column to be replaced
    * @param newColumn Column to be added
    */
   public Table replaceColumn(final int colIndex, final Column<?> newColumn) {
     removeColumns(column(colIndex));
     return insertColumn(colIndex, newColumn);
   }
 
   /**
    * Replaces an existing column (by name) in this table with the given new column
    *
    * @param columnName String name of the column to be replaced
    * @param newColumn Column to be added
    */
   public Table replaceColumn(final String columnName, final Column<?> newColumn) {
     int colIndex = columnIndex(columnName);
     return replaceColumn(colIndex, newColumn);
   }
 
   /**
    * Replaces an existing column having the same name of the given column with the given column
    *
    * @param newColumn Column to be added
    */
   public Table replaceColumn(Column<?> newColumn) {
     return replaceColumn(newColumn.name(), newColumn);
   }
 
   /** Sets the name of the table */
   @Override
   public Table setName(String name) {
     this.name = name;
     return this;
   }
 
   /**
    * Returns the column at the given index in the column list
    *
    * @param columnIndex an integer at least 0 and less than number of columns in the table
    */
   @Override
   public Column<?> column(int columnIndex) {
     return columnList.get(columnIndex);
   }
 
   /** Returns the number of columns in the table */
   @Override
   public int columnCount() {
     return columnList.size();
   }
 
   /** Returns the number of rows in the table */
   @Override
   public int rowCount() {
     int result = 0;
     if (!columnList.isEmpty()) {
       // all the columns have the same number of elements, so we can check any of them
       result = columnList.get(0).size();
     }
     return result;
   }
 
   /** Returns the list of columns */
   @Override
   public List<Column<?>> columns() {
     return columnList;
   }
 
   /** Returns the columns in this table as an array */
   public Column<?>[] columnArray() {
     return columnList.toArray(new Column<?>[columnCount()]);
   }
 
   /** Returns only the columns whose names are given in the input array */
   @Override
   public List<CategoricalColumn<?>> categoricalColumns(String... columnNames) {
     List<CategoricalColumn<?>> columns = new ArrayList<>();
     for (String columnName : columnNames) {
       columns.add(categoricalColumn(columnName));
     }
     return columns;
   }
 
   /**
    * Returns the index of the column with the given name
    *
    * @throws IllegalArgumentException if the input string is not the name of any column in the table
    */
   @Override
   public int columnIndex(String columnName) {
     int columnIndex = -1;
     for (int i = 0; i < columnList.size(); i++) {
       if (columnList.get(i).name().equalsIgnoreCase(columnName)) {
         columnIndex = i;
         break;
       }
     }
     if (columnIndex == -1) {
       throw new IllegalArgumentException(
           String.format("Column %s is not present in table %s", columnName, name));
     }
     return columnIndex;
   }
 
   /**
    * Returns the index of the given column (its position in the list of columns)
    *
    * @throws IllegalArgumentException if the column is not present in this table
    */
   public int columnIndex(Column<?> column) {
     int columnIndex = -1;
     for (int i = 0; i < columnList.size(); i++) {
       if (columnList.get(i).equals(column)) {
         columnIndex = i;
         break;
       }
     }
     if (columnIndex == -1) {
       throw new IllegalArgumentException(
           String.format("Column %s is not present in table %s", column.name(), name));
     }
     return columnIndex;
   }
 
   /** Returns the name of the table */
   @Override
   public String name() {
     return name;
   }
 
   /** Returns a List of the names of all the columns in this table */
   public List<String> columnNames() {
     return columnList.stream().map(Column::name).collect(toList());
   }
 
   /** Returns a table with the same columns as this table */
   public Table copy() {
     Table copy = new Table(name);
     for (Column<?> column : columnList) {
       copy.addColumns(column.emptyCopy(rowCount()));
     }
 
     int[] rows = new int[rowCount()];
     for (int i = 0; i < rowCount(); i++) {
       rows[i] = i;
     }
     Rows.copyRowsToTable(rows, this, copy);
     return copy;
   }
 
   /** Returns a table with the same columns as this table, but no data */
   public Table emptyCopy() {
     Table copy = new Table(name);
     for (Column<?> column : columnList) {
       copy.addColumns(column.emptyCopy());
     }
     return copy;
   }
 
   /**
    * Returns a table with the same columns as this table, but no data, initialized to the given row
    * size
    */
   public Table emptyCopy(int rowSize) {
     Table copy = new Table(name);
     for (Column<?> column : columnList) {
       copy.addColumns(column.emptyCopy(rowSize));
     }
     return copy;
   }
 
   /**
    * Splits the table into two, randomly assigning records to each according to the proportion given
    * in trainingProportion
    *
    * @param table1Proportion The proportion to go in the first table
    * @return An array two tables, with the first table having the proportion specified in the method
    *     parameter, and the second table having the balance of the rows
    */
   public Table[] sampleSplit(double table1Proportion) {
     Table[] tables = new Table[2];
     int table1Count = (int) Math.round(rowCount() * table1Proportion);
 
     Selection table2Selection = new BitmapBackedSelection();
     for (int i = 0; i < rowCount(); i++) {
       table2Selection.add(i);
     }
     Selection table1Selection = new BitmapBackedSelection();
 
     Selection table1Records = selectNRowsAtRandom(table1Count, rowCount());
     for (int table1Record : table1Records) {
       table1Selection.add(table1Record);
     }
     table2Selection.andNot(table1Selection);
     tables[0] = where(table1Selection);
     tables[1] = where(table2Selection);
     return tables;
   }
 
   /**
    * Splits the table into two stratified samples, this uses the specified column to divide the
    * table into groups, randomly assigning records to each according to the proportion given in
    * trainingProportion.
    *
    * @param column the column to be used for the stratified sampling
    * @param table1Proportion The proportion to go in the first table
    * @return An array two tables, with the first table having the proportion specified in the method
    *     parameter, and the second table having the balance of the rows
    */
   public Table[] stratifiedSampleSplit(CategoricalColumn<?> column, double table1Proportion) {
     Preconditions.checkArgument(
         containsColumn(column),
         "The categorical column must be part of the table, you can create a string column and add it to this table before sampling.");
     final Table first = emptyCopy();
     final Table second = emptyCopy();
 
     splitOn(column)
         .asTableList()
         .forEach(
             tab -> {
               Table[] splits = tab.sampleSplit(table1Proportion);
               first.append(splits[0]);
               second.append(splits[1]);
             });
 
     return new Table[] {first, second};
   }
 
 
/** Returns a table consisting of randomly selected records from that table. */
 public Table sampleX(double proportion){
    int sampleSize = (int) Math.round(rowCount() * proportion);
    Selection sampleSelection = new BitmapBackedSelection();
    for (int i = 0; i < rowCount(); i++) {
      sampleSelection.add(i);
    }
    Selection sampleRecords = selectNRowsAtRandom(sampleSize, rowCount());
    for (int sampleRecord : sampleRecords) {
      sampleSelection.add(sampleRecord);
    }
    return where(sampleSelection);
  }
  
    /**
      * Returns a table consisting of randomly selected records from that table.
      *
      * @param proportion The proportion of records to be selected
      * @return A table with the proportion of records specified in the method parameter
      */
    public Table sample(double proportion) {
      int sampleSize = (int) Math.round(rowCount() * proportion);
      Selection sampleSelection = new BitmapBackedSelection();
      for (int i = 0; i < rowCount(); i++) {
        sampleSelection.add(i);
      }
      Selection sampleRecords = selectNRowsAtRandom(sampleSize, rowCount());
      for (int sampleRecord : sampleRecords) {
        sampleSelection.add(sampleRecord);
      }
      return where(sampleSelection);
    }
  
    /**
      * Returns a table consisting of randomly selected records from that table.
      *
      * @param proportion The proportion of records to be selected
      * @return A table with the proportion of records specified in the method parameter
      */
    public Table sample(double proportion, int seed) {
      int sampleSize = (int) Math.round(rowCount() * proportion);
      Selection sampleSelection = new BitmapBackedSelection();
      for (int i = 0; i < rowCount(); i++) {
        sampleSelection.add(i);
      }
      Selection sampleRecords = selectNRowsAtRandom(sampleSize, rowCount(), seed);
      for (int sampleRecord : sampleRecords) {
        sampleSelection.add(sampleRecord);
      }
      return where(sampleSelection);    
 }

 

}