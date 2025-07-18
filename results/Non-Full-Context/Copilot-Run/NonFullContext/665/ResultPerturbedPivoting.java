package tech.tablesaw.api;
 
 import static com.google.common.base.Preconditions.checkArgument;
 
 import com.google.common.base.Preconditions;
 import it.unimi.dsi.fastutil.doubles.*;
 import java.math.BigDecimal;
 import java.nio.ByteBuffer;
 import java.util.Collection;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Set;
 import java.util.function.DoubleConsumer;
 import java.util.function.DoublePredicate;
 import java.util.function.DoubleSupplier;
 import java.util.stream.DoubleStream;
 import tech.tablesaw.columns.AbstractColumnParser;
 import tech.tablesaw.columns.Column;
 import tech.tablesaw.columns.numbers.DoubleColumnType;
 import tech.tablesaw.columns.numbers.FloatColumnType;
 import tech.tablesaw.columns.numbers.NumberColumnFormatter;
 import tech.tablesaw.columns.numbers.NumberFillers;
 import tech.tablesaw.columns.numbers.fillers.DoubleRangeIterable;
 import tech.tablesaw.selection.BitmapBackedSelection;
 import tech.tablesaw.selection.Selection;
 
 public class DoubleColumn extends NumberColumn<DoubleColumn, Double>
     implements NumberFillers<DoubleColumn> {
 
   protected final DoubleArrayList data;
 
   protected DoubleColumn(String name, DoubleArrayList data) {
     super(DoubleColumnType.instance(), name, DoubleColumnType.DEFAULT_PARSER);
     setPrintFormatter(NumberColumnFormatter.floatingPointDefault());
     this.data = data;
   }
 
   public static boolean valueIsMissing(double value) {
     return DoubleColumnType.valueIsMissing(value);
   }
 
   @Override
   public String getString(int row) {
     final double value = getDouble(row);
     return String.valueOf(getPrintFormatter().format(value));
   }
 
   @Override
   public int size() {
     return data.size();
   }
 
   @Override
   public void clear() {
     data.clear();
   }
 
   @Override
   public DoubleColumn setMissing(int index) {
     set(index, DoubleColumnType.missingValueIndicator());
     return this;
   }
 
   protected DoubleColumn(String name) {
     super(DoubleColumnType.instance(), name, DoubleColumnType.DEFAULT_PARSER);
     setPrintFormatter(NumberColumnFormatter.floatingPointDefault());
     this.data = new DoubleArrayList(DEFAULT_ARRAY_SIZE);
   }
 
   public static DoubleColumn create(String name, double... arr) {
     return new DoubleColumn(name, new DoubleArrayList(arr));
   }
 
   public static DoubleColumn create(String name) {
     return new DoubleColumn(name);
   }
 
   public static DoubleColumn create(String name, float... arr) {
     final double[] doubles = new double[arr.length];
     for (int i = 0; i < arr.length; i++) {
       doubles[i] = arr[i];
     }
     return new DoubleColumn(name, new DoubleArrayList(doubles));
   }
 
   public static DoubleColumn create(String name, int... arr) {
     final double[] doubles = new double[arr.length];
     for (int i = 0; i < arr.length; i++) {
       doubles[i] = arr[i];
     }
     return new DoubleColumn(name, new DoubleArrayList(doubles));
   }
 
   public static DoubleColumn create(String name, long... arr) {
     final double[] doubles = new double[arr.length];
     for (int i = 0; i < arr.length; i++) {
       doubles[i] = arr[i];
     }
     return new DoubleColumn(name, new DoubleArrayList(doubles));
   }
 
   public static DoubleColumn create(String name, Collection<? extends Number> numberList) {
     DoubleColumn newColumn = new DoubleColumn(name, new DoubleArrayList(0));
     for (Number number : numberList) {
       newColumn.append(number);
     }
     return newColumn;
   }
 
   public static DoubleColumn create(String name, Number[] numbers) {
     DoubleColumn newColumn = new DoubleColumn(name, new DoubleArrayList(0));
     for (Number number : numbers) {
       newColumn.append(number);
     }
     return newColumn;
   }
 
   public static DoubleColumn create(String name, int initialSize) {
     DoubleColumn column = new DoubleColumn(name);
     for (int i = 0; i < initialSize; i++) {
       column.appendMissing();
     }
     return column;
   }
 
   public static DoubleColumn create(String name, DoubleStream stream) {
     DoubleArrayList list = new DoubleArrayList();
     stream.forEach(list::add);
     return new DoubleColumn(name, list);
   }
 
   @Override
   public DoubleColumn createCol(String name, int initialSize) {
     return create(name, initialSize);
   }
 
   @Override
   public DoubleColumn createCol(String name) {
     return create(name);
   }
 
   @Override
   public Double get(int index) {
     double result = getDouble(index);
     return isMissingValue(result) ? null : result;
   }
 
   @Override
   public DoubleColumn where(Selection selection) {
     return (DoubleColumn) super.where(selection);
   }
 
   public Selection isNotIn(final double... doubles) {
     final Selection results = new BitmapBackedSelection();
     results.addRange(0, size());
     results.andNot(isIn(doubles));
     return results;
   }
 
   public Selection isIn(final double... doubles) {
     final Selection results = new BitmapBackedSelection();
     final DoubleRBTreeSet doubleSet = new DoubleRBTreeSet(doubles);
     for (int i = 0; i < size(); i++) {
       if (doubleSet.contains(getDouble(i))) {
         results.add(i);
       }
     }
     return results;
   }
 
   @Override
   public DoubleColumn subset(int[] rows) {
     final DoubleColumn c = this.emptyCopy();
     for (final int row : rows) {
       c.append(getDouble(row));
     }
     return c;
   }
 
   @Override
   public DoubleColumn unique() {
     final DoubleSet doubles = new DoubleOpenHashSet();
     for (int i = 0; i < size(); i++) {
       doubles.add(getDouble(i));
     }
     final DoubleColumn column = DoubleColumn.create(name() + " Unique values");
     doubles.forEach((DoubleConsumer) column::append);
     return column;
   }
 
   @Override
   public DoubleColumn top(int n) {
     DoubleArrayList top = new DoubleArrayList();
     double[] values = data.toDoubleArray();
     DoubleArrays.parallelQuickSort(values, DoubleComparators.OPPOSITE_COMPARATOR);
     for (int i = 0; i < n && i < values.length; i++) {
       top.add(values[i]);
     }
     return new DoubleColumn(name() + "[Top " + n + "]", top);
   }
 
   @Override
   public DoubleColumn bottom(final int n) {
     DoubleArrayList bottom = new DoubleArrayList();
     double[] values = data.toDoubleArray();
     DoubleArrays.parallelQuickSort(values);
     for (int i = 0; i < n && i < values.length; i++) {
       bottom.add(values[i]);
     }
     return new DoubleColumn(name() + "[Bottoms " + n + "]", bottom);
   }
 
   @Override
   public DoubleColumn lag(int n) {
     final int srcPos = n >= 0 ? 0 : -n;
     final double[] dest = new double[size()];
     final int destPos = Math.max(n, 0);
     final int length = n >= 0 ? size() - n : size() + n;
 
     for (int i = 0; i < size(); i++) {
       dest[i] = FloatColumnType.missingValueIndicator();
     }
 
     double[] array = data.toDoubleArray();
 
     System.arraycopy(array, srcPos, dest, destPos, length);
     return new DoubleColumn(name() + " lag(" + n + ")", new DoubleArrayList(dest));
   }
 
   @Override
   public DoubleColumn removeMissing() {
     DoubleColumn result = copy();
     result.clear();
     DoubleListIterator iterator = data.iterator();
     while (iterator.hasNext()) {
       double v = iterator.nextDouble();
       if (!isMissingValue(v)) {
         result.append(v);
       }
     }
     return result;
   }
 
   /** Adds the given float to this column */
   public DoubleColumn append(final float f) {
     data.add(f);
     return this;
   }
 
   /** Adds the given double to this column */
   public DoubleColumn append(double d) {
     data.add(d);
     return this;
   }
 
   public DoubleColumn append(int i) {
     data.add(i);
     return this;
   }
 
   @Override
   public DoubleColumn append(Double val) {
     if (val == null) {
       appendMissing();
     } else {
       append(val.doubleValue());
     }
     return this;
   }
 
   public DoubleColumn append(Number val) {
     if (val == null) {
       appendMissing();
     } else {
       append(val.doubleValue());
     }
     return this;
   }
 
   @Override
   public DoubleColumn copy() {
     return new DoubleColumn(name(), data.clone());
   }
 
   @Override
   public Iterator<Double> iterator() {
     return data.iterator();
   }
 
   public double[] asDoubleArray() {
     return data.toDoubleArray();
   }
 
   @Override
   public Double[] asObjectArray() {
     final Double[] output = new Double[size()];
     for (int i = 0; i < size(); i++) {
       if (!isMissing(i)) {
         output[i] = getDouble(i);
       } else {
         output[i] = null;
       }
     }
     return output;
   }
 
   @Override
   public int compare(Double o1, Double o2) {
     return Double.compare(o1, o2);
   }
 
   @Override
   public DoubleColumn set(int i, Double val) {
     return val == null ? setMissing(i) : set(i, (double) val);
   }
 
   public DoubleColumn set(int i, double val) {
     data.set(i, val);
     return this;
   }
 
   /**
    * Updates this column where values matching the selection are replaced with the corresponding
    * value from the given column
    */
   public DoubleColumn set(DoublePredicate condition, NumericColumn<?> other) {
     for (int row = 0; row < size(); row++) {
       if (condition.test(getDouble(row))) {
         set(row, other.getDouble(row));
       }
     }
     return this;
   }
 
   @Override
   public Column<Double> set(int row, String stringValue, AbstractColumnParser<?> parser) {
     return set(row, parser.parseDouble(stringValue));
   }
 
   @Override
   public DoubleColumn append(final Column<Double> column) {
     Preconditions.checkArgument(
         column.type() == this.type(),
         "Column '%s' has type %s, but column '%s' has type %s.",
         name(),
         type(),
         column.name(),
         column.type());
     final DoubleColumn numberColumn = (DoubleColumn) column;
     final int size = numberColumn.size();
     for (int i = 0; i < size; i++) {
       append(numberColumn.getDouble(i));
     }
     return this;
   }
 
   @Override
   public DoubleColumn append(Column<Double> column, int row) {
     checkArgument(
         column.type() == this.type(),
         "Column '%s' has type %s, but column '%s' has type %s.",
         name(),
         type(),
         column.name(),
         column.type());
     DoubleColumn doubleColumn = (DoubleColumn) column;
     return append(doubleColumn.getDouble(row));
   }
 
   @Override
   public DoubleColumn set(int row, Column<Double> column, int sourceRow) {
     Preconditions.checkArgument(column.type() == this.type());
     DoubleColumn doubleColumn = (DoubleColumn) column;
     return set(row, doubleColumn.getDouble(sourceRow));
   }
 
   /**
    * Returns a new NumberColumn with only those rows satisfying the predicate
    *
    * @param test the predicate
    * @return a new NumberColumn with only those rows satisfying the predicate
    */
   public DoubleColumn filter(DoublePredicate test) {
     DoubleColumn result = DoubleColumn.create(name());
     for (int i = 0; i < size(); i++) {
       double d = getDouble(i);
       if (test.test(d)) {
         result.append(d);
       }
     }
     return result;
   }
 
   @Override
   public byte[] asBytes(int rowNumber) {
     return ByteBuffer.allocate(DoubleColumnType.instance().byteSize())
         .putDouble(getDouble(rowNumber))
         .array();
   }
 
   /** {@inheritDoc} */
   @Override
   public Set<Double> asSet() {
     return new HashSet<>(unique().asList());
   }
 
   @Override
   public int countUnique() {
     DoubleSet uniqueElements = new DoubleOpenHashSet();
     for (int i = 0; i < size(); i++) {
       uniqueElements.add(getDouble(i));
     }
     return uniqueElements.size();
   }
 
   @Override
   public double getDouble(int row) {
     return data.getDouble(row);
   }
 
   public boolean isMissingValue(double value) {
     return DoubleColumnType.valueIsMissing(value);
   }
 
   @Override
   public boolean isMissing(int rowNumber) {
     return isMissingValue(getDouble(rowNumber));
   }
 
   @Override
   public void sortAscending() {
     data.sort(DoubleComparators.NATURAL_COMPARATOR);
   }
 
   @Override
   public void sortDescending() {
     data.sort(DoubleComparators.OPPOSITE_COMPARATOR);
   }
 
   @Override
   public DoubleColumn appendMissing() {
     return append(DoubleColumnType.missingValueIndicator());
   }
 
   @Override
   public DoubleColumn appendObj(Object obj) {
     if (obj == null) {
       return appendMissing();
     }
     if (obj instanceof Double) {
       return append((double) obj);
     }
     if (obj instanceof BigDecimal) {
       return append(((BigDecimal) obj).doubleValue());
     }
     throw new IllegalArgumentException("Could not append " + obj.getClass());
   }
 
   @Override
   public DoubleColumn appendCell(final String value) {
     try {
       return append(parser().parseDouble(value));
     } catch (final NumberFormatException e) {
       throw new NumberFormatException(
           "Error adding value to column " + name() + ": " + e.getMessage());
     }
   }
 
   @Override
   public DoubleColumn appendCell(final String value, AbstractColumnParser<?> parser) {
     try {
       return append(parser.parseDouble(value));
     } catch (final NumberFormatException e) {
       throw new NumberFormatException(
           "Error adding value to column " + name() + ": " + e.getMessage());
     }
   }
 
   @Override
   public String getUnformattedString(final int row) {
     final double value = getDouble(row);
     if (DoubleColumnType.valueIsMissing(value)) {
       return "";
     }
     return String.valueOf(value);
   }
 
   // fillWith methods
 
   @Override
   public DoubleColumn fillWith(final DoubleIterator iterator) {
     for (int r = 0; r < size(); r++) {
       if (!iterator.hasNext()) {
         break;
       }
       set(r, iterator.nextDouble());
     }
     return this;
   }
 
   @Override
   public DoubleColumn fillWith(final DoubleRangeIterable iterable) {
     DoubleIterator iterator = iterable.iterator();
     for (int r = 0; r < size(); r++) {
       if (!iterator.hasNext()) {
         iterator = iterable.iterator();
         if (!iterator.hasNext()) {
           break;
         }
       }
       set(r, iterator.nextDouble());
     }
     return this;
   }
 
   @Override
   public DoubleColumn fillWith(final DoubleSupplier supplier) {
     for (int r = 0; r < size(); r++) {
       try {
         set(r, supplier.getAsDouble());
       } catch (final Exception e) {
         break;
       }
     }
     return this;
   }
 
   @Override
   public DoubleColumn fillWith(double d) {
     for (int r = 0; r < size(); r++) {
       set(r, d);
     }
     return this;
   }
 
   /**
    * Returns a new LongColumn containing a value for each value in this column, truncating if
    * necessary
    *
    * <p>A narrowing primitive conversion such as this one may lose information about the overall
    * magnitude of a numeric value and may also lose precision and range. Specifically, if the value
    * is too small (a negative value of large magnitude or negative infinity), the result is the
    * smallest representable value of type long.
    *
    * <p>Similarly, if the value is too large (a positive value of large magnitude or positive
    * infinity), the result is the largest representable value of type long.
    *
    * <p>Despite the fact that overflow, underflow, or other loss of information may occur, a
    * narrowing primitive conversion never results in a run-time exception.
    *
    * <p>A missing value in the receiver is converted to a missing value in the result
    */
   @Override
   public LongColumn asLongColumn() {
     LongColumn result = LongColumn.create(name());
     for (double d : data) {
       if (DoubleColumnType.valueIsMissing(d)) {
         result.appendMissing();
       } else {
         result.append((long) d);
       }
     }
     return result;
   }
 
 
/** Returns a new IntColumn containing a value for each value in this column, truncating if necessary. */
 public IntColumn asIntColumn(){
    IntColumn result = IntColumn.create(name());
    for (double d : data) {
      if (DoubleColumnType.valueIsMissing(d)) {
        result.appendMissing();
      } else {
        result.append((int) d);
      }
    }
    return result;
  }
  
    /**
      * Returns a new DoubleColumn containing a value for each value in this column, truncating if
      * necessary
      *
      * <p>A narrowing primitive conversion such as this one may lose information about the overall
      * magnitude of a numeric value and may also lose precision and range. Specifically, if the value
      * is too small (a negative value of large magnitude or negative infinity), the result is the
      * smallest representable value of type double.
      *
      * <p>Similarly, if the value is too large (a positive value of large magnitude or positive
      * infinity), the result is the largest representable value of type double.
      *
      * <p>Despite the fact that overflow, underflow, or other loss of information may occur, a
      * narrowing primitive conversion never results in a run-time exception.
      *
      * <p>A missing value in the receiver is converted to a missing value in the result
      */
    @Override
    public DoubleColumn asDoubleColumn() {
      DoubleColumn result = DoubleColumn.create(name());
      for (double d : data) {
        if (DoubleColumnType.valueIsMissing(d)) {
          result.appendMissing();
        } else {
          result.append(d);
        }
      }
      return result;
    }
  
    @Override
    public DoubleColumn asDoubleColumn(DoubleColumnType type) {
      DoubleColumn result = DoubleColumn.create(name());
      for (double d : data) {
        if (DoubleColumnType.valueIsMissing(d)) {
          result.appendMissing();
        } else {
          result.append(d);
        }
      }
      return result;
    }
  
    @Override
    public DoubleColumn asDoubleColumn(DoubleColumnType type, DoubleColumnType missingValueType) {    
 }

 

}