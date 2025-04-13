package tech.tablesaw.api;
 
 import static com.google.common.base.Preconditions.checkArgument;
 
 import com.google.common.base.Preconditions;
 import it.unimi.dsi.fastutil.ints.*;
 import java.nio.ByteBuffer;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Set;
 import java.util.stream.IntStream;
 import tech.tablesaw.columns.AbstractColumnParser;
 import tech.tablesaw.columns.Column;
 import tech.tablesaw.columns.numbers.DoubleColumnType;
 import tech.tablesaw.columns.numbers.IntColumnType;
 import tech.tablesaw.columns.numbers.NumberColumnFormatter;
 import tech.tablesaw.selection.BitmapBackedSelection;
 import tech.tablesaw.selection.Selection;
 
 public class IntColumn extends NumberColumn<IntColumn, Integer>
     implements CategoricalColumn<Integer> {
 
   protected final IntArrayList data;
 
   protected IntColumn(final String name, IntArrayList data) {
     super(IntColumnType.instance(), name, IntColumnType.DEFAULT_PARSER);
     setPrintFormatter(NumberColumnFormatter.ints());
     this.data = data;
   }
 
   public static IntColumn create(final String name) {
     return new IntColumn(name, new IntArrayList());
   }
 
   public static IntColumn create(final String name, final int... arr) {
     return new IntColumn(name, new IntArrayList(arr));
   }
 
   public static IntColumn create(final String name, final Integer[] arr) {
     IntColumn newColumn = IntColumn.create(name, 0);
     for (Integer integer : arr) {
       newColumn.append(integer);
     }
     return newColumn;
   }
 
   public static IntColumn create(String name, int initialSize) {
     IntColumn column = new IntColumn(name, new IntArrayList(initialSize));
     for (int i = 0; i < initialSize; i++) {
       column.appendMissing();
     }
     return column;
   }
 
   public static IntColumn create(String name, IntStream stream) {
     IntArrayList list = new IntArrayList();
     stream.forEach(list::add);
     return new IntColumn(name, list);
   }
 
   @Override
   public IntColumn createCol(String name, int initialSize) {
     return create(name, initialSize);
   }
 
   @Override
   public IntColumn createCol(String name) {
     return create(name);
   }
 
 
/** Returns a new numeric column initialized with the given name and size. */
 public static IntColumn indexColumn(final String columnName, final int size, final int startsWith){
    IntColumn column = new IntColumn(columnName, new IntArrayList(size));
    for(int i = 0; i < size; i++){
      column.set(i, startsWith + i);
    }
    return column;
  }
  
    @Override
    public IntColumn createEmptyColumn(String name) {
      return new IntColumn(name, new IntArrayList());
    }
  
    @Override
    public IntColumn createEmptyColumn(String name, int initialSize) {
      return new IntColumn(name, new IntArrayList(initialSize));
    }
  
    @Override
    public IntColumn createEmptyColumn(String name, int initialSize, int missingValue) {
      return new IntColumn(name, new IntArrayList(initialSize, missingValue));
    }
  
    @Override
    public IntColumn createEmptyColumn(String name, int initialSize, int missingValue, int missingValue2) {
      return new IntColumn(name, new IntArrayList(initialSize, missingValue, missingValue2));
    }
  
    @Override
    public IntColumn createEmptyColumn(String name, int initialSize, int missingValue, int missingValue2, int missingValue3) {
      return new IntColumn(name, new IntArrayList(initialSize, missingValue, missingValue2, missingValue3));
    }
  
    @Override
    public IntColumn createEmptyColumn(String name, int initialSize, int missingValue, int missingValue2, int missingValue3, int missingValue4) {
      return new IntColumn(name, new IntArrayList(initialSize, missingValue, missingValue2, missingValue3, missingValue4));
    }
  
    @Override
    public IntColumn createEmptyColumn(String name, int initialSize, int missingValue, int missingValue2, int missingValue3, int missingValue4, int missingValue5) {
      return new IntColumn(name, new IntArrayList(initialSize, missingValue, missingValue2, missingValue3, missingValue4, missingValue5));
    }
  
    @Override
    public IntColumn createEmptyColumn(String name, int initialSize,    
 }

 

}