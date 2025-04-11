package tech.tablesaw.joining;
 
 import com.google.common.collect.Streams;
 import com.google.common.primitives.Ints;
 import java.util.Arrays;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.stream.Collectors;
 import tech.tablesaw.api.*;
 import tech.tablesaw.columns.Column;
 import tech.tablesaw.columns.booleans.BooleanColumnType;
 import tech.tablesaw.columns.dates.DateColumnType;
 import tech.tablesaw.columns.datetimes.DateTimeColumnType;
 import tech.tablesaw.columns.instant.InstantColumnType;
 import tech.tablesaw.columns.numbers.*;
 import tech.tablesaw.columns.strings.StringColumnType;
 import tech.tablesaw.columns.strings.TextColumnType;
 import tech.tablesaw.columns.times.TimeColumnType;
 import tech.tablesaw.index.*;
 import tech.tablesaw.selection.Selection;
 
 /** Implements joins between two or more Tables */
 public class DataFrameJoiner {
 
   /** The types of joins that are supported */
   private enum JoinType {
     INNER,
     LEFT_OUTER,
     RIGHT_OUTER,
     FULL_OUTER
   }
 
   private static final String TABLE_ALIAS = "T";
 
   private final Table table;
   private final String[] joinColumnNames;
   private final List<Integer> joinColumnIndexes;
   private final AtomicInteger joinTableId = new AtomicInteger(2);
 
   /**
    * Constructor.
    *
    * @param table The table to join on.
    * @param joinColumnNames The join column names to join on.
    */
   public DataFrameJoiner(Table table, String... joinColumnNames) {
     this.table = table;
     this.joinColumnNames = joinColumnNames;
     this.joinColumnIndexes = getJoinIndexes(table, joinColumnNames);
   }
 
   /**
    * Finds the index of the columns corresponding to the columnNames. E.G. The column named "ID" is
    * located at index 5 in table.
    *
    * @param table the table that contains the columns.
    * @param columnNames the column names to find indexes of.
    * @return a list of column indexes within the table.
    */
   private List<Integer> getJoinIndexes(Table table, String[] columnNames) {
     return Arrays.stream(columnNames).map(table::columnIndex).collect(Collectors.toList());
   }
 
   /**
    * Joins to the given tables assuming that they have a column of the name we're joining on
    *
    * @param tables The tables to join with
    */
   public Table inner(Table... tables) {
     return inner(false, tables);
   }
 
   /**
    * Joins to the given tables assuming that they have a column of the name we're joining on
    *
    * @param allowDuplicateColumnNames if {@code false} the join will fail if any columns other than
    *     the join column have the same name if {@code true} the join will succeed and duplicate
    *     columns are renamed*
    * @param tables The tables to join with
    */
   public Table inner(boolean allowDuplicateColumnNames, Table... tables) {
     Table joined = table;
     for (Table currT : tables) {
       joined =
           joinInternal(
               joined, currT, JoinType.INNER, allowDuplicateColumnNames, false, joinColumnNames);
     }
     return joined;
   }
 
   /**
    * Joins the joiner to the table2, using the given column for the second table and returns the
    * resulting table
    *
    * @param table2 The table to join with
    * @param col2Name The column to join on. If col2Name refers to a double column, the join is
    *     performed after rounding to integers.
    * @return The resulting table
    */
   public Table inner(Table table2, String col2Name) {
     return inner(table2, false, col2Name);
   }
 
   /**
    * Joins the joiner to the table2, using the given columns for the second table and returns the
    * resulting table
    *
    * @param table2 The table to join with
    * @param col2Names The columns to join on. If a name refers to a double column, the join is
    *     performed after rounding to integers.
    * @return The resulting table
    */
   public Table inner(Table table2, String[] col2Names) {
     return inner(table2, false, col2Names);
   }
 
   /**
    * Joins the joiner to the table2, using the given column for the second table and returns the
    * resulting table
    *
    * @param table2 The table to join with
    * @param col2Name The column to join on. If col2Name refers to a double column, the join is
    *     performed after rounding to integers.
    * @param allowDuplicateColumnNames if {@code false} the join will fail if any columns other than
    *     the join column have the same name if {@code true} the join will succeed and duplicate
    *     columns are renamed*
    * @return The resulting table
    */
   public Table inner(Table table2, String col2Name, boolean allowDuplicateColumnNames) {
     return inner(table2, allowDuplicateColumnNames, col2Name);
   }
 
   /**
    * Joins the joiner to the table2, using the given columns for the second table and returns the
    * resulting table
    *
    * @param table2 The table to join with
    * @param allowDuplicateColumnNames if {@code false} the join will fail if any columns other than
    *     the join column have the same name if {@code true} the join will succeed and duplicate
    *     columns are renamed*
    * @param col2Names The columns to join on. If a name refers to a double column, the join is
    *     performed after rounding to integers.
    * @return The resulting table
    */
   public Table inner(Table table2, boolean allowDuplicateColumnNames, String... col2Names) {
     Table joinedTable;
     joinedTable =
         joinInternal(table, table2, JoinType.INNER, allowDuplicateColumnNames, false, col2Names);
     return joinedTable;
   }
 
   /**
    * Joins the joiner to the table2, using the given columns for the second table and returns the
    * resulting table
    *
    * @param table2 The table to join with
    * @param allowDuplicateColumnNames if {@code false} the join will fail if any columns other than
    *     the join column have the same name if {@code true} the join will succeed and duplicate
    *     columns are renamed*
    * @param keepAllJoinKeyColumns if {@code false} the join will only keep join key columns in
    *     table1 if {@code true} the join will return all join key columns in both table, which may
    *     have difference when there are null values
    * @param col2Names The columns to join on. If a name refers to a double column, the join is
    *     performed after rounding to integers.
    * @return The resulting table
    */
   public Table inner(
       Table table2,
       boolean allowDuplicateColumnNames,
       boolean keepAllJoinKeyColumns,
       String... col2Names) {
     return joinInternal(
         table, table2, JoinType.INNER, allowDuplicateColumnNames, keepAllJoinKeyColumns, col2Names);
   }
 
   /**
    * Joins two tables.
    *
    * @param table1 the table on the left side of the join.
    * @param table2 the table on the right side of the join.
    * @param joinType the type of join.
    * @param allowDuplicates if {@code false} the join will fail if any columns other than the join
    *     column have the same name if {@code true} the join will succeed and duplicate columns are
    *     renamed
    * @param keepAllJoinKeyColumns if {@code false} the join will only keep join key columns in
    *     table1 if {@code true} the join will return all join key columns in both table, which may
    *     have difference when there are null values
    * @param table2JoinColumnNames The names of the columns in table2 to join on.
    * @return the joined table
    */
   private Table joinInternal(
       Table table1,
       Table table2,
       JoinType joinType,
       boolean allowDuplicates,
       boolean keepAllJoinKeyColumns,
       String... table2JoinColumnNames) {
 
     List<Integer> table2JoinColumnIndexes = getJoinIndexes(table2, table2JoinColumnNames);
     List<Index> table1Indexes = buildIndexesForJoinColumns(joinColumnIndexes, table1);
     List<Index> table2Indexes = buildIndexesForJoinColumns(table2JoinColumnIndexes, table2);
 
     Table result = Table.create(table1.name());
     // A set of column indexes in the result table that can be ignored. They are duplicate join
     // keys.
     Set<Integer> resultIgnoreColIndexes =
         emptyTableFromColumns(
             result,
             table1,
             table2,
             joinType,
             allowDuplicates,
             table2JoinColumnIndexes,
             keepAllJoinKeyColumns);
 
     validateIndexes(table1Indexes, table2Indexes);
     if (table1.rowCount() == 0 && (joinType == JoinType.LEFT_OUTER || joinType == JoinType.INNER)) {
       // Handle special case of empty table here so it doesn't fall through to the behavior
       // that adds rows for full outer and right outer joins
       if (!keepAllJoinKeyColumns) {
         result.removeColumns(Ints.toArray(resultIgnoreColIndexes));
       }
       return result;
     }
 
     Selection table1DoneRows = Selection.with();
     Selection table2DoneRows = Selection.with();
     // use table 2 for row iteration, which can significantly increase performance
     if (table1.rowCount() > table2.rowCount() && joinType == JoinType.INNER) {
       for (Row row : table2) {
         int ri = row.getRowNumber();
         if (table2DoneRows.contains(ri)) {
           // Already processed a selection of table1 that contained this row.
           continue;
         }
         Selection table1Rows =
             createMultiColSelection(
                 table2, ri, table1Indexes, table1.rowCount(), table2JoinColumnIndexes);
         Selection table2Rows =
             createMultiColSelection(
                 table2, ri, table2Indexes, table2.rowCount(), table2JoinColumnIndexes);
         crossProduct(
             result,
             table1,
             table2,
             table1Rows,
             table2Rows,
             resultIgnoreColIndexes,
             keepAllJoinKeyColumns);
 
         table2DoneRows = table2DoneRows.or(table2Rows);
         if (table2DoneRows.size() == table2.rowCount()) {
           // Processed all the rows in table1 exit early.
           if (!keepAllJoinKeyColumns) {
             result.removeColumns(Ints.toArray(resultIgnoreColIndexes));
           }
           return result;
         }
       }
     } else {
       for (Row row : table1) {
         int ri = row.getRowNumber();
         if (table1DoneRows.contains(ri)) {
           // Already processed a selection of table1 that contained this row.
           continue;
         }
         Selection table1Rows =
             createMultiColSelection(
                 table1, ri, table1Indexes, table1.rowCount(), joinColumnIndexes);
         Selection table2Rows =
             createMultiColSelection(
                 table1, ri, table2Indexes, table2.rowCount(), joinColumnIndexes);
         if ((joinType == JoinType.LEFT_OUTER || joinType == JoinType.FULL_OUTER)
             && table2Rows.isEmpty()) {
           withMissingLeftJoin(
               result, table1, table1Rows, resultIgnoreColIndexes, keepAllJoinKeyColumns);
         } else {
           crossProduct(
               result,
               table1,
               table2,
               table1Rows,
               table2Rows,
               resultIgnoreColIndexes,
               keepAllJoinKeyColumns);
         }
         table1DoneRows = table1DoneRows.or(table1Rows);
         if (joinType == JoinType.FULL_OUTER || joinType == JoinType.RIGHT_OUTER) {
           // Update done rows in table2 for full Outer.
           table2DoneRows = table2DoneRows.or(table2Rows);
         } else if (table1DoneRows.size() == table1.rowCount()) {
           // Processed all the rows in table1 exit early.
           if (!keepAllJoinKeyColumns) {
             result.removeColumns(Ints.toArray(resultIgnoreColIndexes));
           }
           return result;
         }
       }
     }
 
     // Add all rows from table2 that were not handled already.
     Selection table2Rows = table2DoneRows.flip(0, table2.rowCount());
     withMissingRight(
         result,
         table1.columnCount(),
         table2,
         table2Rows,
         joinType,
         table2JoinColumnIndexes,
         resultIgnoreColIndexes,
         keepAllJoinKeyColumns);
     if (!keepAllJoinKeyColumns) {
       result.removeColumns(Ints.toArray(resultIgnoreColIndexes));
     }
     return result;
   }
 
   private void validateIndexes(List<Index> table1Indexes, List<Index> table2Indexes) {
     if (table1Indexes.size() != table2Indexes.size()) {
       throw new IllegalArgumentException(
           "Cannot join using a different number of indices on each table: "
               + table1Indexes
               + " and "
               + table2Indexes);
     }
     for (int i = 0; i < table1Indexes.size(); i++) {
       if (!table1Indexes.get(i).getClass().equals(table2Indexes.get(i).getClass())) {
         throw new IllegalArgumentException(
             "Cannot join using different index types: " + table1Indexes + " and " + table2Indexes);
       }
     }
   }
 
 
/** Build a reverse index for every join column in the table. */
 private List<Index> buildIndexesForJoinColumns(List<Integer> joinColumnIndexes, Table table){}

 

}