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
 
 
/** Return a new table that contains all the columns in the argument. */
 public Table reorderColumns(String... columnNames){}

 

}