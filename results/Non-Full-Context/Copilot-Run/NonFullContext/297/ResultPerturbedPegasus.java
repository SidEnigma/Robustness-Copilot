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
 
   /**
    * Returns a table consisting of randomly selected records from this table. The sample size is
    * based on the given proportion
    *
    * @param proportion The proportion to go in the sample
    */
   public Table sampleX(double proportion) {
     Preconditions.checkArgument(
         proportion <= 1 && proportion >= 0, "The sample proportion must be between 0 and 1");
 
     int tableSize = (int) Math.round(rowCount() * proportion);
     return where(selectNRowsAtRandom(tableSize, rowCount()));
   }
 
   /**
    * Returns a table consisting of randomly selected records from this table
    *
    * @param nRows The number of rows to go in the sample
    */
   public Table sampleN(int nRows) {
     Preconditions.checkArgument(
         nRows > 0 && nRows < rowCount(),
         "The number of rows sampled must be greater than 0 and less than the number of rows in the table.");
     return where(selectNRowsAtRandom(nRows, rowCount()));
   }
 
   /** Clears all the data from this table */
   @Override
   public void clear() {
     columnList.forEach(Column::clear);
   }
 
   /** Returns a new table containing the first {@code nrows} of data in this table */
   public Table first(int nRows) {
     int newRowCount = Math.min(nRows, rowCount());
     return inRange(0, newRowCount);
   }
 
   /** Returns a new table containing the last {@code nrows} of data in this table */
   public Table last(int nRows) {
     int newRowCount = Math.min(nRows, rowCount());
     return inRange(rowCount() - newRowCount, rowCount());
   }
 
   /**
    * Sorts this table into a new table on the columns indexed
    *
    * <p>if index is negative then sort that column in descending order otherwise sort ascending
    */
   public Table sortOn(int... columnIndexes) {
     List<String> names = new ArrayList<>();
     for (int i : columnIndexes) {
       if (i >= 0) {
         names.add(columnList.get(i).name());
       } else {
         names.add("-" + columnList.get(-i).name());
       }
     }
     return sortOn(names.toArray(new String[names.size()]));
   }
 
   /**
    * Returns a copy of this table sorted on the given column names, applied in order,
    *
    * <p>if column name starts with - then sort that column descending otherwise sort ascending
    */
   public Table sortOn(String... columnNames) {
     return this.sortOn(Sort.create(this, columnNames));
   }
 
   /**
    * Returns a copy of this table sorted in the order of the given column names, in ascending order
    */
   public Table sortAscendingOn(String... columnNames) {
     return this.sortOn(columnNames);
   }
 
   /**
    * Returns a copy of this table sorted on the given column names, applied in order, descending
    * TODO: Provide equivalent methods naming columns by index
    */
   public Table sortDescendingOn(String... columnNames) {
     Sort key = getSort(columnNames);
     return sortOn(key);
   }
 
   /**
    * Returns a copy of this table sorted using the given sort key.
    *
    * @param key to sort on.
    * @return a sorted copy of this table.
    */
   public Table sortOn(Sort key) {
     Preconditions.checkArgument(!key.isEmpty());
     if (key.size() == 1) {
       IntComparator comparator = SortUtils.getComparator(this, key);
       return sortOn(comparator);
     }
     IntComparatorChain chain = SortUtils.getChain(this, key);
     return sortOn(chain);
   }
 
   /** Returns a copy of this table sorted using the given comparator */
   private Table sortOn(IntComparator rowComparator) {
     Table newTable = emptyCopy(rowCount());
 
     int[] newRows = rows();
     IntArrays.mergeSort(newRows, rowComparator);
 
     Rows.copyRowsToTable(newRows, this, newTable);
     return newTable;
   }
 
   /** Returns a copy of this table sorted using the given comparator */
   public Table sortOn(Comparator<Row> rowComparator) {
     Row row1 = new Row(this);
     Row row2 = new Row(this);
     return sortOn(
         (IntComparator)
             (k1, k2) -> {
               row1.at(k1);
               row2.at(k2);
               return rowComparator.compare(row1, row2);
             });
   }
 
   /** Returns an array of ints of the same number of rows as the table */
   private int[] rows() {
     int[] rowIndexes = new int[rowCount()];
     for (int i = 0; i < rowCount(); i++) {
       rowIndexes[i] = i;
     }
     return rowIndexes;
   }
 
   /**
    * Adds a single row to this table from sourceTable, copying every column in sourceTable
    *
    * @param rowIndex The row in sourceTable to add to this table
    * @param sourceTable A table with the same column structure as this table
    */
   public void addRow(int rowIndex, Table sourceTable) {
     for (int i = 0; i < columnCount(); i++) {
       column(i).appendObj(sourceTable.column(i).get(rowIndex));
     }
   }
 
   /**
    * Adds the given row to this table
    *
    * @deprecated Use {@link #append(Row)} instead.
    */
   @Deprecated
   public void addRow(Row row) {
     for (int i = 0; i < row.columnCount(); i++) {
       column(i).appendObj(row.getObject(i));
     }
   }
 
   /** Returns a new Row object with its position set to the given zero-based row index. */
   public Row row(int rowIndex) {
     Row row = new Row(Table.this);
     row.at(rowIndex);
     return row;
   }
 
   /** Returns a table containing the rows contained in the given array of row indices */
   public Table rows(int... rowNumbers) {
     Preconditions.checkArgument(Ints.max(rowNumbers) <= rowCount());
     return where(Selection.with(rowNumbers));
   }
 
   /** Returns a table EXCLUDING the rows contained in the given array of row indices */
   public Table dropRows(int... rowNumbers) {
     Preconditions.checkArgument(Ints.max(rowNumbers) <= rowCount());
     Selection selection = Selection.withRange(0, rowCount()).andNot(Selection.with(rowNumbers));
     return where(selection);
   }
 
   /**
    * Returns a new table containing the first rowCount rows if rowCount positive. Returns the last
    * rowCount rows if rowCount negative.
    */
   public Table inRange(int rowCount) {
     Preconditions.checkArgument(rowCount <= rowCount());
     int rowStart = rowCount >= 0 ? 0 : rowCount() + rowCount;
     int rowEnd = rowCount >= 0 ? rowCount : rowCount();
     return where(Selection.withRange(rowStart, rowEnd));
   }
 
   /**
    * Returns a new table containing the rows contained in the range from rowStart inclusive to
    * rowEnd exclusive
    */
   public Table inRange(int rowStart, int rowEnd) {
     Preconditions.checkArgument(rowEnd <= rowCount());
     return where(Selection.withRange(rowStart, rowEnd));
   }
 
   /**
    * Returns a new table EXCLUDING the first rowCount rows if rowCount positive. Drops the last
    * rowCount rows if rowCount negative.
    */
   public Table dropRange(int rowCount) {
     Preconditions.checkArgument(rowCount <= rowCount());
     int rowStart = rowCount >= 0 ? rowCount : 0;
     int rowEnd = rowCount >= 0 ? rowCount() : rowCount() + rowCount;
     return where(Selection.withRange(rowStart, rowEnd));
   }
 
   /**
    * Returns a table EXCLUDING the rows contained in the range from rowStart inclusive to rowEnd
    * exclusive
    */
   public Table dropRange(int rowStart, int rowEnd) {
     Preconditions.checkArgument(rowEnd <= rowCount());
     return where(Selection.withoutRange(0, rowCount(), rowStart, rowEnd));
   }
 
   /** Returns a table containing the rows contained in the given Selection */
   public Table where(Selection selection) {
     Table newTable = this.emptyCopy(selection.size());
     Rows.copyRowsToTable(selection, this, newTable);
     return newTable;
   }
 
   /** Returns a new Table made by applying the given function to this table */
   public Table where(Function<Table, Selection> selection) {
     return where(selection.apply(this));
   }
 
   /**
    * Returns a new Table made by EXCLUDING any rows returned when the given function is applied to
    * this table
    */
   public Table dropWhere(Function<Table, Selection> selection) {
     return where(not(selection));
   }
 
   /** Returns a table EXCLUDING the rows contained in the given Selection */
   public Table dropWhere(Selection selection) {
     Selection opposite = new BitmapBackedSelection();
     opposite.addRange(0, rowCount());
     opposite.andNot(selection);
     Table newTable = this.emptyCopy(opposite.size());
     Rows.copyRowsToTable(opposite, this, newTable);
     return newTable;
   }
 
   /**
    * Returns a pivot on this table, where: The first column contains unique values from the index
    * column1 There are n additional columns, one for each unique value in column2 The values in each
    * of the cells in these new columns are the result of applying the given AggregateFunction to the
    * data in column3, grouped by the values of column1 and column2
    */
   public Table pivot(
       CategoricalColumn<?> column1,
       CategoricalColumn<?> column2,
       NumericColumn<?> column3,
       AggregateFunction<?, ?> aggregateFunction) {
     return PivotTable.pivot(this, column1, column2, column3, aggregateFunction);
   }
 
   /**
    * Returns a pivot on this table, where: The first column contains unique values from the index
    * column1 There are n additional columns, one for each unique value in column2 The values in each
    * of the cells in these new columns are the result of applying the given AggregateFunction to the
    * data in column3, grouped by the values of column1 and column2
    */
   public Table pivot(
       String column1Name,
       String column2Name,
       String column3Name,
       AggregateFunction<?, ?> aggregateFunction) {
     return pivot(
         categoricalColumn(column1Name),
         categoricalColumn(column2Name),
         numberColumn(column3Name),
         aggregateFunction);
   }
 
   /**
    * Returns a non-overlapping and exhaustive collection of "slices" over this table. Each slice is
    * like a virtual table containing a subset of the records in this table
    *
    * <p>This method is intended for advanced or unusual operations on the subtables. If you want to
    * calculate summary statistics for each subtable, the summarize methods (e.g)
    *
    * <p>table.summarize(myColumn, mean, median).by(columns)
    *
    * <p>are preferred
    */
   public TableSliceGroup splitOn(String... columns) {
     return splitOn(categoricalColumns(columns).toArray(new CategoricalColumn<?>[columns.length]));
   }
 
   /**
    * Returns a non-overlapping and exhaustive collection of "slices" over this table. Each slice is
    * like a virtual table containing a subset of the records in this table
    *
    * <p>This method is intended for advanced or unusual operations on the subtables. If you want to
    * calculate summary statistics for each subtable, the summarize methods (e.g)
    *
    * <p>table.summarize(myColumn, mean, median).by(columns)
    *
    * <p>are preferred
    */
   public TableSliceGroup splitOn(CategoricalColumn<?>... columns) {
     return StandardTableSliceGroup.create(this, columns);
   }
 
   /** Returns the unique records in this table Note: Uses a lot of memory for a sort */
   public Table dropDuplicateRows() {
 
     Table sorted = this.sortOn(columnNames().toArray(new String[columns().size()]));
     Table temp = emptyCopy();
 
     for (int row = 0; row < rowCount(); row++) {
       if (temp.isEmpty() || !Rows.compareRows(row, sorted, temp)) {
         Rows.appendRowToTable(row, sorted, temp);
       }
     }
     return temp;
   }
 
   /** Returns only those records in this table that have no columns with missing values */
   public Table dropRowsWithMissingValues() {
 
     Selection missing = new BitmapBackedSelection();
 
     for (int row = 0; row < rowCount(); row++) {
       for (int col = 0; col < columnCount(); col++) {
         Column<?> c = column(col);
         if (c.isMissing(row)) {
           missing.add(row);
           break;
         }
       }
     }
     Selection notMissing = Selection.withRange(0, rowCount());
     notMissing.andNot(missing);
     Table temp = emptyCopy(notMissing.size());
     Rows.copyRowsToTable(notMissing, this, temp);
     return temp;
   }
 
   /**
    * Returns a new table containing copies of the selected columns from this table
    *
    * @param columns The columns to copy into the new table
    * @see #retainColumns(Column[])
    */
   public Table selectColumns(Column<?>... columns) {
     Table t = Table.create(this.name);
     for (Column<?> c : columns) {
       t.addColumns(c.copy());
     }
     return t;
   }
 
   /**
    * Returns a new table containing copies of the selected columns from this table
    *
    * @param columns The columns to copy into the new table
    * @see #retainColumns(Column[])
    * @deprecated Use {@link #selectColumns(Column[])} instead
    */
   public Table select(Column<?>... columns) {
     return selectColumns(columns);
   }
 
   /**
    * Returns a new table containing copies of the selected columns from this table
    *
    * @param columnNames The names of the columns to include
    * @see #retainColumns(String[])
    */
   public Table selectColumns(String... columnNames) {
     Table t = Table.create(this.name);
     for (String s : columnNames) {
       t.addColumns(column(s).copy());
     }
     return t;
   }
 
   /**
    * Returns a new table containing copies of the selected columns from this table
    *
    * @param columnNames The names of the columns to include
    * @see #retainColumns(String[])
    * @deprecated Use {@link #selectColumns(String[])} instead
    */
   public Table select(String... columnNames) {
     return selectColumns(columnNames);
   }
 
   /**
    * Returns a new table containing copies of all the columns from this table, except those at the
    * given indexes
    *
    * @param columnIndexes The indexes of the columns to exclude
    * @see #removeColumns(int[])
    */
   public Table rejectColumns(int... columnIndexes) {
     Table t = Table.create(this.name);
     RoaringBitmap bm = new RoaringBitmap();
     bm.add((long) 0, columnCount());
     RoaringBitmap excluded = new RoaringBitmap();
     excluded.add(columnIndexes);
     bm.andNot(excluded);
     for (int i : bm) {
       t.addColumns(column(i).copy());
     }
     return t;
   }
 
   /**
    * Returns a new table containing copies of all the columns from this table, except those named in
    * the argument
    *
    * @param columnNames The names of the columns to exclude
    * @see #removeColumns(int[])
    */
   public Table rejectColumns(String... columnNames) {
     IntArrayList indices = new IntArrayList();
     for (String s : columnNames) {
       indices.add(columnIndex(s));
     }
     return rejectColumns(indices.toIntArray());
   }
 
   /**
    * Returns a new table containing copies of all the columns from this table, except those named in
    * the argument
    *
    * @param columns The names of the columns to exclude
    * @see #removeColumns(int[])
    */
   public Table rejectColumns(Column<?>... columns) {
     IntArrayList indices = new IntArrayList();
     for (Column<?> c : columns) {
       indices.add(columnIndex(c));
     }
     return rejectColumns(indices.toIntArray());
   }
 
   /**
    * Returns a new table containing copies of the columns at the given indexes
    *
    * @param columnIndexes The indexes of the columns to include
    * @see #retainColumns(int[])
    */
   public Table selectColumns(int... columnIndexes) {
     Table t = Table.create(this.name);
     RoaringBitmap bm = new RoaringBitmap();
     bm.add((long) 0, columnCount());
     RoaringBitmap excluded = new RoaringBitmap();
     excluded.add(columnIndexes);
     bm.andNot(excluded);
     for (int i : bm) {
       t.addColumns(column(i).copy());
     }
     return t;
   }
 
   /** Removes the given columns from this table and returns this table */
   @Override
   public Table removeColumns(Column<?>... columns) {
     columnList.removeAll(Arrays.asList(columns));
     return this;
   }
 
   /** Removes all columns with missing values from this table, and returns this table. */
   public Table removeColumnsWithMissingValues() {
     removeColumns(columnList.stream().filter(x -> x.countMissing() > 0).toArray(Column<?>[]::new));
     return this;
   }
 
   /**
    * Removes all columns except for those given in the argument from this table and returns this
    * table
    */
   public Table retainColumns(Column<?>... columns) {
     List<Column<?>> retained = Arrays.asList(columns);
     columnList.clear();
     columnList.addAll(retained);
     return this;
   }
 
   /**
    * Removes all columns except for those given in the argument from this table and returns this
    * table
    */
   public Table retainColumns(int... columnIndexes) {
     List<Column<?>> retained = columns(columnIndexes);
     columnList.clear();
     columnList.addAll(retained);
     return this;
   }
 
   /**
    * Removes all columns except for those given in the argument from this table and returns this
    * table
    */
   public Table retainColumns(String... columnNames) {
     List<Column<?>> retained = columns(columnNames);
     columnList.clear();
     columnList.addAll(retained);
     return this;
   }
 
   /** Returns this table after adding the data from the argument */
   @SuppressWarnings({"rawtypes", "unchecked"})
   public Table append(Relation tableToAppend) {
     for (final Column column : columnList) {
       final Column columnToAppend = tableToAppend.column(column.name());
       column.append(columnToAppend);
     }
     return this;
   }
 
   /**
    * Appends the given row to this table and returns the table.
    *
    * <p>Note: The table is modified in-place TODO: Performance
    */
   public Table append(Row row) {
     for (int i = 0; i < row.columnCount(); i++) {
       column(i).appendObj(row.getObject(i));
     }
     return this;
   }
 
   /** Removes the columns with the given names from this table and returns this table */
   @Override
   public Table removeColumns(String... columns) {
     return (Table) super.removeColumns(columns);
   }
 
   /** Removes the columns at the given indices from this table and returns this table */
   @Override
   public Table removeColumns(int... columnIndexes) {
     return (Table) super.removeColumns(columnIndexes);
   }
 
 
/** Appends an empty row and returns a Row object that is related to the new row. */
 public Row appendRow(){
    Row r = new Row(columnCount());
    for (int i = 0; i < columnCount(); i++) {
      r.set(i, column(i).appendObj());
    }
    return r;
  }
  
    /**
      * Returns a new table containing copies of all the columns from this table, except those at the
      * given indexes
      *
      * @param columnIndexes The indexes of the columns to exclude
      * @see #removeColumns(int[])
      */
    public Table rejectColumns(String... columnNames) {
      IntArrayList indices = new IntArrayList();
      for (String s : columnNames) {
        indices.add(columnIndex(s));
      }
      return rejectColumns(indices.toIntArray());
    }
  
    /**
      * Returns a new table containing copies of all the columns from this table, except those at the
      * given indexes
      *
      * @param columnIndexes The indexes of the columns to exclude
      * @see #removeColumns(int[])
      */
    public Table rejectColumns(Column<?>... columns) {
      IntArrayList indices = new IntArrayList();
      for (Column<?> c : columns) {
        indices.add(columnIndex(c));
      }
      return rejectColumns(indices.toIntArray());
    }
  
    /**
      * Returns a new table containing copies of all the columns from this table, except those named in
      * the argument
      *
      * @param columnNames The names of the columns to exclude
      * @see #removeColumns(int[])
      */
    public Table rejectColumns(Column<?>... columns) {
      IntArrayList indices = new IntArrayList();
      for (Column<?> c : columns) {
        indices.add(columnIndex(c));
      }
      return rejectColumns(indices.toIntArray());
    }
  
    /**
      * Returns a new table containing copies of all the columns from this table, except those named in
      * the argument
      *
      * @param columnNames The names of the   
 }

 

}