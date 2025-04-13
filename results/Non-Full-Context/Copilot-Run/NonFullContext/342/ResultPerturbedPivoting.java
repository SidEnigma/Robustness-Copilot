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
    public IntColumn createCol(String name, IntStream stream) {
      return create(name, stream);
    }
  
    @Override
    public IntColumn createCol(String name, Iterator<Integer> iterator) {
      return create(name, iterator);
    }
  
    @Override
    public IntColumn createCol(String name, Selection selection) {
      return create(name, selection);
    }
  
    @Override
    public IntColumn createCol(String name, BitmapBackedSelection selection) {
      return create(name, selection);
    }
  
    @Override
    public IntColumn createCol(String name, IntColumn column) {
      return create(name, column);
    }
  
    @Override
    public IntColumn createCol(String name, IntColumn column, Selection selection) {
      return create(name, column, selection);
    }
  
    @Override
    public IntColumn createCol(String name, IntColumn column, BitmapBackedSelection selection) {
      return create(name, column, selection);
    }
  
    @Override
    public IntColumn createCol(String name, IntColumn column, int start, int end) {
      return create(name, column, start, end);
    }
  
    @Override
    public IntColumn createCol(String name, IntColumn column, int start, int end, Selection selection) {
      return create(name, column, start, end, selection);
    }
  
    @Override
    public IntColumn createCol(String name, IntColumn column, int start, int end, BitmapBackedSelection selection) {
      return create(name, column, start, end, selection);
    }
  
    @Override
    public IntColumn createCol(String name, IntColumn column, int start, int end, int step) {
      return create   
 }

 

}