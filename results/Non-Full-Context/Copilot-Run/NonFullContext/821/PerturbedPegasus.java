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
 
 package tech.tablesaw.table;
 
 import com.google.common.base.Preconditions;
 import it.unimi.dsi.fastutil.ints.IntArrays;
 import it.unimi.dsi.fastutil.ints.IntComparator;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Iterator;
 import java.util.List;
 import java.util.PrimitiveIterator;
 import java.util.stream.IntStream;
 import javax.annotation.Nullable;
 import tech.tablesaw.aggregate.NumericAggregateFunction;
 import tech.tablesaw.api.NumericColumn;
 import tech.tablesaw.api.Row;
 import tech.tablesaw.api.Table;
 import tech.tablesaw.columns.Column;
 import tech.tablesaw.selection.Selection;
 import tech.tablesaw.sorting.Sort;
 import tech.tablesaw.sorting.SortUtils;
 import tech.tablesaw.sorting.comparators.IntComparatorChain;
 
 /**
  * A TableSlice is a facade around a Relation that acts as a filter. Requests for data are forwarded
  * to the underlying table. A TableSlice can be sorted independently of the underlying table.
  *
  * <p>A TableSlice is only good until the structure of the underlying table changes.
  */
 public class TableSlice extends Relation {
 
   private final Table table;
   private String name;
   @Nullable private Selection selection;
   @Nullable private int[] sortOrder = null;
 
   /**
    * Returns a new View constructed from the given table, containing only the rows represented by
    * the bitmap
    */
   public TableSlice(Table table, Selection rowSelection) {
     this.name = table.name();
     this.selection = rowSelection;
     this.table = table;
   }
 
   /**
    * Returns a new view constructed from the given table. The view can be sorted independently of
    * the table.
    */
   public TableSlice(Table table) {
     this.name = table.name();
     this.selection = null;
     this.table = table;
   }
 
   @Override
   public Column<?> column(int columnIndex) {
     Column<?> col = table.column(columnIndex);
     if (isSorted()) {
       return col.subset(sortOrder);
     } else if (hasSelection()) {
       return col.where(selection);
     }
     return col;
   }
 
   @Override
   public Column<?> column(String columnName) {
     return column(table.columnIndex(columnName));
   }
 
   @Override
   public int columnCount() {
     return table.columnCount();
   }
 
   @Override
   public int rowCount() {
     if (hasSelection()) {
       return selection.size();
     }
     return table.rowCount();
   }
 
   @Override
   public List<Column<?>> columns() {
     List<Column<?>> columns = new ArrayList<>();
     for (int i = 0; i < columnCount(); i++) {
       columns.add(column(i));
     }
     return columns;
   }
 
   @Override
   public int columnIndex(Column<?> column) {
     return table.columnIndex(column);
   }
 
   @Override
   public Object get(int r, int c) {
     return table.get(mappedRowNumber(r), c);
   }
 
   @Override
   public String name() {
     return name;
   }
 
   public Table getTable() {
     return table;
   }
 
   /** Clears all rows from this View, leaving the structure in place */
   @Override
   public void clear() {
     sortOrder = null;
     selection = Selection.with();
   }
 
   /** Removes the sort from this View. */
   public void removeSort() {
     this.sortOrder = null;
   }
 
   /**
    * Removes the selection from this view, leaving it with the same number of rows as the underlying
    * source table.
    */
   public void removeSelection() {
     this.selection = null;
   }
 
   @Override
   public List<String> columnNames() {
     return table.columnNames();
   }
 
   @Override
   public TableSlice addColumns(Column<?>... column) {
     throw new UnsupportedOperationException(
         "Class TableSlice does not support the addColumns operation");
   }
 
   @Override
   public TableSlice removeColumns(Column<?>... columns) {
     throw new UnsupportedOperationException(
         "Class TableSlice does not support the removeColumns operation");
   }
 
   @Override
   public Table first(int nRows) {
     int count = 0;
     PrimitiveIterator.OfInt it = sourceRowNumberIterator();
     Table copy = table.emptyCopy();
     while (it.hasNext() && count < nRows) {
       int row = it.nextInt();
       copy.addRow(table.row(row));
       count++;
     }
     return copy;
   }
 
   @Override
   public TableSlice setName(String name) {
     this.name = name;
     return this;
   }
 
   public Table asTable() {
     Table table = Table.create(this.name());
     for (Column<?> column : this.columns()) {
       table.addColumns(column);
     }
     return table;
   }
 
 
/** There are source table row numbers in this view. */
 protected PrimitiveIterator.OfInt sourceRowNumberIterator(){}

 

}