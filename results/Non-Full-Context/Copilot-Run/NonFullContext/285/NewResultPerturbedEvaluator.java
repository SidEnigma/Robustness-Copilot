package tech.tablesaw.api;
 
 import java.util.function.Function;
 import tech.tablesaw.filtering.And;
 import tech.tablesaw.filtering.DeferredBooleanColumn;
 import tech.tablesaw.filtering.DeferredColumn;
 import tech.tablesaw.filtering.DeferredDateColumn;
 import tech.tablesaw.filtering.DeferredDateTimeColumn;
 import tech.tablesaw.filtering.DeferredInstantColumn;
 import tech.tablesaw.filtering.DeferredNumberColumn;
 import tech.tablesaw.filtering.DeferredStringColumn;
 import tech.tablesaw.filtering.DeferredTextColumn;
 import tech.tablesaw.filtering.DeferredTimeColumn;
 import tech.tablesaw.filtering.Not;
 import tech.tablesaw.filtering.Or;
 import tech.tablesaw.selection.Selection;
 
 /** Utility methods to aid in the construction of complex queries on tables */
 public class QuerySupport {
 
   /** Returns a selection for all records for which the given function is {@code false} */
   public static Function<Table, Selection> not(Function<Table, Selection> deferredSelection) {
     return new Not(deferredSelection);
   }
 
   /**
    * Returns a selection for all records that match neither of the given functions. In other words,
    * if either (or both) of sel1 or sel2 is {@code true}, the record as a whole is {@code false}.
    */
   public static Function<Table, Selection> neither(
       Function<Table, Selection> sel1, Function<Table, Selection> sel2) {
     return new Not(either(sel1, sel2));
   }
 
 
/** For all records that don't match both the given functions, return a selection. */

public static Function<Table, Selection> notBoth(Function<Table, Selection> sel1, Function<Table, Selection> sel2) {
  return new Not(new And(sel1, sel2));
}
 

}