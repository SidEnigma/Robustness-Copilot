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
 
 import static tech.tablesaw.columns.DateAndTimePredicates.isMissing;
 import static tech.tablesaw.columns.DateAndTimePredicates.isNotMissing;
 
 import com.google.common.base.Preconditions;
 import it.unimi.dsi.fastutil.ints.IntArrayList;
 import it.unimi.dsi.fastutil.ints.IntArrays;
 import it.unimi.dsi.fastutil.ints.IntComparator;
 import it.unimi.dsi.fastutil.ints.IntComparators;
 import it.unimi.dsi.fastutil.ints.IntIterator;
 import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
 import it.unimi.dsi.fastutil.ints.IntSet;
 import java.nio.ByteBuffer;
 import java.sql.Time;
 import java.time.LocalTime;
 import java.time.format.DateTimeFormatter;
 import java.util.*;
 import java.util.function.Consumer;
 import java.util.function.Supplier;
 import java.util.stream.Stream;
 import tech.tablesaw.columns.AbstractColumn;
 import tech.tablesaw.columns.AbstractColumnParser;
 import tech.tablesaw.columns.Column;
 import tech.tablesaw.columns.times.PackedLocalTime;
 import tech.tablesaw.columns.times.TimeColumnFormatter;
 import tech.tablesaw.columns.times.TimeColumnType;
 import tech.tablesaw.columns.times.TimeFillers;
 import tech.tablesaw.columns.times.TimeFilters;
 import tech.tablesaw.columns.times.TimeMapFunctions;
 import tech.tablesaw.selection.Selection;
 
 /** A column that contains local time values */
 public class TimeColumn extends AbstractColumn<TimeColumn, LocalTime>
     implements CategoricalColumn<LocalTime>,
         TimeFilters,
         TimeFillers<TimeColumn>,
         TimeMapFunctions {
 
   private TimeColumnFormatter printFormatter = new TimeColumnFormatter();
 
   protected IntArrayList data;
 
   private final IntComparator comparator =
       (r1, r2) -> {
         int f1 = getIntInternal(r1);
         int f2 = getIntInternal(r2);
         return Integer.compare(f1, f2);
       };
 
   private TimeColumn(String name, IntArrayList times) {
     super(TimeColumnType.instance(), name, TimeColumnType.DEFAULT_PARSER);
     data = times;
   }
 
   private TimeColumn(String name) {
     super(TimeColumnType.instance(), name, TimeColumnType.DEFAULT_PARSER);
     data = new IntArrayList(DEFAULT_ARRAY_SIZE);
   }
 
   public static TimeColumn createInternal(String name, int[] data) {
     return new TimeColumn(name, new IntArrayList(data));
   }
 
   public static boolean valueIsMissing(int i) {
     return TimeColumnType.valueIsMissing(i);
   }
 
   public static TimeColumn create(String name) {
     return new TimeColumn(name);
   }
 
   public static TimeColumn create(String name, AbstractColumnParser<LocalTime> parser) {
     TimeColumn column = new TimeColumn(name);
     column.setParser(parser);
     return column;
   }
 
   public static TimeColumn create(String name, Collection<LocalTime> data) {
     TimeColumn column = new TimeColumn(name, new IntArrayList(data.size()));
     for (LocalTime time : data) {
       column.append(time);
     }
     return column;
   }
 
   public static TimeColumn create(String name, LocalTime... data) {
     TimeColumn column = new TimeColumn(name, new IntArrayList(data.length));
     for (LocalTime time : data) {
       column.append(time);
     }
     return column;
   }
 
   public static TimeColumn create(String name, int initialSize) {
     TimeColumn column = new TimeColumn(name, new IntArrayList(initialSize));
     for (int i = 0; i < initialSize; i++) {
       column.appendMissing();
     }
     return column;
   }
 
   public static TimeColumn create(String name, Stream<LocalTime> stream) {
     TimeColumn column = create(name);
     stream.forEach(column::append);
     return column;
   }
 
   /** {@inheritDoc} */
   @Override
   public TimeColumn appendMissing() {
     appendInternal(TimeColumnType.missingValueIndicator());
     return this;
   }
 
   /** {@inheritDoc} */
   @Override
   public TimeColumn subset(int[] rows) {
     final TimeColumn c = this.emptyCopy();
     for (final int row : rows) {
       c.appendInternal(getIntInternal(row));
     }
     return c;
   }
 
   /** {@inheritDoc} */
   @Override
   public TimeColumn lag(int n) {
     int srcPos = n >= 0 ? 0 : -n;
     int[] dest = new int[size()];
     int destPos = Math.max(n, 0);
     int length = n >= 0 ? size() - n : size() + n;
 
     for (int i = 0; i < size(); i++) {
       dest[i] = TimeColumnType.missingValueIndicator();
     }
 
     System.arraycopy(data.toIntArray(), srcPos, dest, destPos, length);
 
     TimeColumn copy = emptyCopy(size());
     copy.data = new IntArrayList(dest);
     copy.setName(name() + " lag(" + n + ")");
     return copy;
   }
 
   /** {@inheritDoc} */
   @Override
   public boolean isMissing(int rowNumber) {
     return valueIsMissing(getIntInternal(rowNumber));
   }
 
   /** {@inheritDoc} */
   @Override
   public int size() {
     return data.size();
   }
 
   public TimeColumn appendInternal(int f) {
     data.add(f);
     return this;
   }
 
   /** {@inheritDoc} */
   @Override
   public TimeColumn append(LocalTime time) {
     int value;
     if (time == null) {
       value = TimeColumnType.missingValueIndicator();
     } else {
       value = PackedLocalTime.pack(time);
     }
     appendInternal(value);
     return this;
   }
 
   /** {@inheritDoc} */
   @Override
   public TimeColumn appendObj(Object obj) {
     if (obj == null) {
       return appendMissing();
     }
     if (obj instanceof LocalTime) {
       return append((LocalTime) obj);
     }
     if (obj instanceof Time) {
       Time time = (Time) obj;
       return append(time.toLocalTime());
     }
     throw new IllegalArgumentException(
         "Cannot append " + obj.getClass().getName() + " to TimeColumn");
   }
 
   /** {@inheritDoc} */
   @Override
   public TimeColumn removeMissing() {
     TimeColumn noMissing = emptyCopy();
     IntIterator iterator = intIterator();
     while (iterator.hasNext()) {
       int i = iterator.nextInt();
       if (!valueIsMissing(i)) {
         noMissing.appendInternal(i);
       }
     }
     return noMissing;
   }
 
   /** {@inheritDoc} */
   @Override
   public String getString(int row) {
     return printFormatter.format(getPackedTime(row));
   }
 
   /** {@inheritDoc} */
   @Override
   public String getUnformattedString(int row) {
     return PackedLocalTime.toShortTimeString(getPackedTime(row));
   }
 
   public void setPrintFormatter(DateTimeFormatter dateTimeFormatter, String missingValueString) {
     Preconditions.checkNotNull(dateTimeFormatter);
     Preconditions.checkNotNull(missingValueString);
     this.printFormatter = new TimeColumnFormatter(dateTimeFormatter, missingValueString);
   }
 
   public void setPrintFormatter(DateTimeFormatter dateTimeFormatter) {
     Preconditions.checkNotNull(dateTimeFormatter);
     this.printFormatter = new TimeColumnFormatter(dateTimeFormatter);
   }
 
   /** {@inheritDoc} */
   @Override
   public TimeColumn emptyCopy() {
     TimeColumn empty = create(name());
     empty.printFormatter = printFormatter;
     return empty;
   }
 
   /** {@inheritDoc} */
   @Override
   public TimeColumn emptyCopy(int rowSize) {
     TimeColumn column = TimeColumn.create(name(), rowSize);
     column.printFormatter = printFormatter;
     return column;
   }
 
   /** {@inheritDoc} */
   @Override
   public TimeColumn copy() {
     TimeColumn column = emptyCopy(size());
     column.data = data.clone();
     return column;
   }
 
   /** {@inheritDoc} */
   @Override
   public void clear() {
     data.clear();
   }
 
   /** Returns the entire contents of this column as a list */
   @Override
   public List<LocalTime> asList() {
     List<LocalTime> times = new ArrayList<>();
     for (LocalTime time : this) {
       times.add(time);
     }
     return times;
   }
 
   /** {@inheritDoc} */
   @Override
   public void sortAscending() {
     data.sort(IntComparators.NATURAL_COMPARATOR);
   }
 
   /** {@inheritDoc} */
   @Override
   public void sortDescending() {
     data.sort(IntComparators.OPPOSITE_COMPARATOR);
   }
 
   public LocalTime max() {
 
     if (isEmpty()) {
       return null;
     }
     int max = getIntInternal(0);
 
     for (int aData : data) {
       max = Math.max(max, aData);
     }
 
     if (max == TimeColumnType.missingValueIndicator()) {
       return null;
     }
     return PackedLocalTime.asLocalTime(max);
   }
 
   /** {@inheritDoc} */
   @Override
   public LocalTime min() {
 
     if (isEmpty()) {
       return null;
     }
 
     int min = Integer.MAX_VALUE;
 
     for (int aData : data) {
       if (aData != TimeColumnType.missingValueIndicator()) {
         min = Math.min(min, aData);
       }
     }
     if (min == Integer.MAX_VALUE) {
       return null;
     }
     return PackedLocalTime.asLocalTime(min);
   }
 
   /** {@inheritDoc} */
   @Override
   public Table summary() {
 
     Table table = Table.create("Column: " + name());
     StringColumn measure = StringColumn.create("Measure");
     StringColumn value = StringColumn.create("Value");
     table.addColumns(measure);
     table.addColumns(value);
 
     measure.append("Count");
     value.append(String.valueOf(size()));
 
     measure.append("Missing");
     value.append(String.valueOf(countMissing()));
 
     measure.append("Earliest");
     value.append(String.valueOf(min()));
 
     measure.append("Latest");
     value.append(String.valueOf(max()));
 
     return table;
   }
 
   /** Returns the count of missing values in this column */
   @Override
   public int countMissing() {
     int count = 0;
     for (int i = 0; i < size(); i++) {
       if (getIntInternal(i) == TimeColumnType.missingValueIndicator()) {
         count++;
       }
     }
     return count;
   }
 
   /** {@inheritDoc} */
   @Override
   public int countUnique() {
     IntOpenHashSet hashSet = new IntOpenHashSet(data);
     return hashSet.size();
   }
 
   /** {@inheritDoc} */
   @Override
   public TimeColumn unique() {
     IntSet ints = new IntOpenHashSet(data);
     TimeColumn column = emptyCopy(ints.size());
     column.data = IntArrayList.wrap(ints.toIntArray());
     column.setName(name() + " Unique values");
     return column;
   }
 
   /** {@inheritDoc} */
   @Override
   public boolean isEmpty() {
     return data.isEmpty();
   }
 
   /** {@inheritDoc} */
   @Override
   public TimeColumn appendCell(String object) {
     appendInternal(PackedLocalTime.pack(parser().parse(object)));
     return this;
   }
 
   /** {@inheritDoc} */
   @Override
   public TimeColumn appendCell(String object, AbstractColumnParser<?> parser) {
     return appendObj(parser.parse(object));
   }
 
   /** {@inheritDoc} */
   @Override
   public int getIntInternal(int index) {
     return data.getInt(index);
   }
 
   protected int getPackedTime(int index) {
     return getIntInternal(index);
   }
 
   /** {@inheritDoc} */
   @Override
   public LocalTime get(int index) {
     return PackedLocalTime.asLocalTime(getIntInternal(index));
   }
 
   /** {@inheritDoc} */
   @Override
   public IntComparator rowComparator() {
     return comparator;
   }
 
   public double getDouble(int i) {
     return getIntInternal(i);
   }
 
   public double[] asDoubleArray() {
     double[] doubles = new double[data.size()];
     for (int i = 0; i < size(); i++) {
       doubles[i] = data.getInt(i);
     }
     return doubles;
   }
 
   public DoubleColumn asDoubleColumn() {
     return DoubleColumn.create(name(), asDoubleArray());
   }
 
   /** {@inheritDoc} */
   @Override
   public String toString() {
     return "LocalTime column: " + name();
   }
 
   /** {@inheritDoc} */
   @Override
   public TimeColumn append(Column<LocalTime> column) {
     Preconditions.checkArgument(
         column.type() == this.type(),
         "Column '%s' has type %s, but column '%s' has type %s.",
         name(),
         type(),
         column.name(),
         column.type());
     TimeColumn timeCol = (TimeColumn) column;
     final int size = timeCol.size();
     for (int i = 0; i < size; i++) {
       appendInternal(timeCol.getIntInternal(i));
     }
     return this;
   }
 
   /** {@inheritDoc} */
   @Override
   public TimeColumn append(Column<LocalTime> column, int row) {
     Preconditions.checkArgument(
         column.type() == this.type(),
         "Column '%s' has type %s, but column '%s' has type %s.",
         name(),
         type(),
         column.name(),
         column.type());
     return appendInternal(((TimeColumn) column).getIntInternal(row));
   }
 
   /** {@inheritDoc} */
   @Override
   public TimeColumn set(int row, Column<LocalTime> column, int sourceRow) {
     Preconditions.checkArgument(
         column.type() == this.type(),
         "Column '%s' has type %s, but column '%s' has type %s.",
         name(),
         type(),
         column.name(),
         column.type());
     return set(row, ((TimeColumn) column).getIntInternal(sourceRow));
   }
 
 
/** Returns the n largest values ("top") in the column. */
 public List<LocalTime> top(int n){
    return top(n, null);    
 }

 

}