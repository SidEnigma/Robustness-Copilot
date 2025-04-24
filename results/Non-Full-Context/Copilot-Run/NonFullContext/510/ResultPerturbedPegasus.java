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
 
 package tech.tablesaw.columns;
 
 import static tech.tablesaw.selection.Selection.selectNRowsAtRandom;
 
 import com.google.common.base.Preconditions;
 import it.unimi.dsi.fastutil.ints.IntComparator;
 import java.util.*;
 import java.util.function.BinaryOperator;
 import java.util.function.Function;
 import java.util.function.Predicate;
 import tech.tablesaw.api.ColumnType;
 import tech.tablesaw.api.StringColumn;
 import tech.tablesaw.api.Table;
 import tech.tablesaw.interpolation.Interpolator;
 import tech.tablesaw.selection.Selection;
 import tech.tablesaw.table.RollingColumn;
 import tech.tablesaw.util.StringUtils;
 
 /**
  * The general interface for columns.
  *
  * <p>Columns can either exist on their own or be a part of a table. All the data in a single column
  * is of a particular type.
  */
 public interface Column<T> extends Iterable<T>, Comparator<T> {
 
   /** Returns the number of elements in this column, including missing values */
   int size();
 
   /** Returns a table containing a ColumnType specific summary of the data in this column */
   Table summary();
 
   /** Returns an array of objects as appropriate for my type of column */
   T[] asObjectArray();
 
   /**
    * Returns the count of missing values in this column.
    *
    * @return missing values as int
    */
   int countMissing();
 
   /**
    * Returns the count of unique values in this column.
    *
    * @return unique values as int
    */
   default int countUnique() {
     return unique().size();
   }
 
   /**
    * Returns the column's name.
    *
    * @return name as String
    */
   String name();
 
   /**
    * Returns this column's ColumnType
    *
    * @return {@link ColumnType}
    */
   ColumnType type();
 
   /**
    * Returns the parser used by {@link #appendCell(String)} ()}.
    *
    * @return {@link AbstractColumnParser}
    */
   AbstractColumnParser<T> parser();
 
   /**
    * Returns a string representation of the value at the given row.
    *
    * @param row The index of the row.
    * @return value as String
    */
   String getString(int row);
 
   /** Returns the value at the given zero-based index */
   T get(int row);
 
   /**
    * Reduction with binary operator and initial value
    *
    * @param initial initial value
    * @param op the operator
    * @return the result of reducing initial value and all rows with operator
    */
   default T reduce(T initial, BinaryOperator<T> op) {
     T acc = initial;
     for (T t : this) {
       acc = op.apply(acc, t);
     }
     return acc;
   }
 
   /**
    * Reduction with binary operator
    *
    * @param op the operator
    * @return Optional with the result of reducing all rows with operator
    */
   default Optional<T> reduce(BinaryOperator<T> op) {
     boolean first = true;
     T acc = null;
     for (T t : this) {
       if (first) {
         acc = t;
         first = false;
       } else {
         acc = op.apply(acc, t);
       }
     }
     return (first ? Optional.empty() : Optional.of(acc));
   }
 
   /** Removes all elements TODO: Make this return this column */
   void clear();
 
   /** Sorts my values in ascending order */
   void sortAscending();
 
   /** Sorts my values in descending order */
   void sortDescending();
 
   /**
    * Returns true if the column has no data
    *
    * @return true if empty, false if not
    */
   boolean isEmpty();
 
   /** Returns an IntComparator for sorting my rows */
   IntComparator rowComparator();
 
   default String title() {
     return "Column: " + name() + System.lineSeparator();
   }
 
   /** Returns a selection containing an index for every missing value in this column */
   Selection isMissing();
 
   /** Returns a selection containing an index for every non-missing value in this column */
   Selection isNotMissing();
 
   /**
    * Returns the width of a cell in this column, in bytes.
    *
    * @return width in bytes
    */
   int byteSize();
 
   /**
    * Returns the contents of the cell at rowNumber as a byte[].
    *
    * @param rowNumber index of the row
    * @return content as byte[]
    */
   byte[] asBytes(int rowNumber);
 
   /** Returns a Set containing all the unique values in this column */
   Set<T> asSet();
 
   /**
    * Returns a {@link RollingColumn} with the given windowSize, which can be used for performing
    * calculations on rolling subsets of my data
    *
    * @param windowSize The number of elements to include in each calculation
    * @return a RollingColumn
    */
   default RollingColumn rolling(final int windowSize) {
     return new RollingColumn(this, windowSize);
   }
 
   /** Returns a String representation of the value at index r, without any formatting applied */
   String getUnformattedString(int r);
 
   /** Returns true if the value at rowNumber is missing */
   boolean isMissing(int rowNumber);
 
   /** TODO(lwhite): Print n from the top and bottom, like a table; */
   default String print() {
     final StringBuilder builder = new StringBuilder();
     builder.append(title());
     for (int i = 0; i < size(); i++) {
       builder.append(getString(i));
       builder.append(System.lineSeparator());
     }
     return builder.toString();
   }
 
   /** Returns the width of the column in characters, for printing */
   default int columnWidth() {
 
     int width = name().length();
     for (int rowNum = 0; rowNum < size(); rowNum++) {
       width = Math.max(width, StringUtils.length(getString(rowNum)));
     }
     return width;
   }
 
   /**
    * Returns a list of all the elements in this column
    *
    * <p>Note, if a value in the column is missing, a {@code null} is added in it's place
    */
   default List<T> asList() {
     List<T> results = new ArrayList<>();
     for (int i = 0; i < this.size(); i++) {
       if (isMissing(i)) {
         results.add(null);
       } else {
         results.add(get(i));
       }
     }
     return results;
   }
 
   /**
    * Returns {@code true} if the given object appears in this column, and false otherwise
    *
    * <p>TODO override in column subtypes for performance
    */
   default boolean contains(T object) {
     for (int i = 0; i < this.size(); i++) {
       if (object != null) {
         if (object.equals(get(i))) {
           return true;
         }
       } else {
         if (get(i) == null) return true;
       }
     }
     return false;
   }
 
   // functional methods corresponding to those in Stream
 
   /**
    * Counts the number of rows satisfying predicate, but only upto the max value
    *
    * @param test the predicate
    * @param max the maximum number of rows to count
    * @return the number of rows satisfying the predicate
    */
   default int count(Predicate<? super T> test, int max) {
     int count = 0;
     for (T t : this) {
       if (test.test(t)) {
         count++;
         if (max > 0 && count >= max) {
           return count;
         }
       }
     }
     return count;
   }
 
   /**
    * Counts the number of rows satisfying predicate
    *
    * @param test the predicate
    * @return the number of rows satisfying the predicate
    */
   default int count(Predicate<? super T> test) {
     return count(test, size());
   }
 
   /**
    * Returns true if all rows satisfy the predicate, false otherwise
    *
    * @param test the predicate
    * @return true if all rows satisfy the predicate, false otherwise
    */
   default boolean allMatch(Predicate<? super T> test) {
     return count(test.negate(), 1) == 0;
   }
 
   /**
    * Returns true if any row satisfies the predicate, false otherwise
    *
    * @param test the predicate
    * @return true if any rows satisfies the predicate, false otherwise
    */
   default boolean anyMatch(Predicate<? super T> test) {
     return count(test, 1) > 0;
   }
 
   /**
    * Returns true if no row satisfies the predicate, false otherwise
    *
    * @param test the predicate
    * @return true if no row satisfies the predicate, false otherwise
    */
   default boolean noneMatch(Predicate<? super T> test) {
     return count(test, 1) == 0;
   }
 
   /**
    * Returns the maximum row according to the provided Comparator
    *
    * @param comp
    * @return the maximum row
    */
   default Optional<T> max(Comparator<? super T> comp) {
     boolean first = true;
     T o1 = null;
     for (T o2 : this) {
       if (first) {
         o1 = o2;
         first = false;
       } else if (comp.compare(o1, o2) < 0) {
         o1 = o2;
       }
     }
     return (first ? Optional.<T>empty() : Optional.<T>of(o1));
   }
 
   /**
    * Returns the minimum value according to the provided Comparator
    *
    * @param comp the Comparator to use
    * @return the minimum value
    */
   default Optional<T> min(Comparator<? super T> comp) {
     boolean first = true;
     T o1 = null;
     for (T o2 : this) {
       if (first) {
         o1 = o2;
         first = false;
       } else if (comp.compare(o1, o2) > 0) {
         o1 = o2;
       }
     }
     return (first ? Optional.<T>empty() : Optional.<T>of(o1));
   }
 
 
/** The function is mapped across all rows and the results are in the Column. */
 C mapInto(Function<? super T, ? extends R> fun, C into){
    for (int i = 0; i < size(); i++) {
      into.add(fun.apply(get(i)));
    }
    return into;
  }
  
    /**
      * Returns a new Column with the results of applying the function to each row
      *
      * @param fun the function to apply
      * @return a new Column with the results of applying the function to each row
      */
    default C map(Function<? super T, ? extends R> fun) {
      C into = createColumn(name());
      return mapInto(fun, into);
    }
  
    /**
      * Returns a new Column with the results of applying the function to each row
      *
      * @param fun the function to apply
      * @return a new Column with the results of applying the function to each row
      */
    default C map(Function<? super T, ? extends R> fun, C into) {
      return mapInto(fun, into);
    }
  
    /**
      * Returns a new Column with the results of applying the function to each row
      *
      * @param fun the function to apply
      * @return a new Column with the results of applying the function to each row
      */
    default C map(Function<? super T, ? extends R> fun, int max) {
      C into = createColumn(name());
      return mapInto(fun, into, max);
    }
  
    /**
      * Returns a new Column with the results of applying the function to each row
      *
      * @param fun the function to apply
      * @return a new Column with the results of applying the function to each row
      */
    default C map(Function<? super T, ? extends R> fun, int max, C into) {
      return mapInto(fun, into, max);
    }
  
    /**
      * Returns a new Column with the results of applying the function to each row
      *
      * @param fun the function to apply
      * @return a new Column with the results of applying the function to each row
      */
    default C map(Function<? super T, ? extends R>    
 }

 

}