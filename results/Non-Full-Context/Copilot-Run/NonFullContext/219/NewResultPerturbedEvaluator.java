package tech.tablesaw.analytic;
 
 import com.google.common.collect.ImmutableList;
 import java.util.Optional;
 import tech.tablesaw.analytic.ArgumentList.FunctionCall;
 import tech.tablesaw.api.Row;
 import tech.tablesaw.api.Table;
 import tech.tablesaw.columns.Column;
 import tech.tablesaw.sorting.Sort;
 import tech.tablesaw.sorting.SortUtils;
 import tech.tablesaw.sorting.comparators.IntComparatorChain;
 import tech.tablesaw.table.TableSlice;
 
 /**
  * Executes analytic queries.
  *
  * <p>Makes no changes to the underlying table. The order of the rows in "result" Table will match
  * the order of the rows in underlying source table.
  */
 final class AnalyticQueryEngine {
   private final AnalyticQuery query;
   private final Table destination;
   private final IntComparatorChain rowComparator;
 
   private AnalyticQueryEngine(AnalyticQuery query) {
     this.query = query;
     this.destination = Table.create("Analytic ~ " + query.getTable().name());
     Optional<Sort> sort = query.getSort();
     this.rowComparator = sort.isPresent() ? SortUtils.getChain(query.getTable(), sort.get()) : null;
   }
 
   public static AnalyticQueryEngine create(AnalyticQuery query) {
     return new AnalyticQueryEngine(query);
   }
 
   /**
    * Execute the given analytic Query.
    *
    * @return a table with the result of the query. Rows in the result table match the order of rows
    *     in the source table.
    */
   public Table execute() {
     addColumns();
     partition().forEach(this::processSlice);
     return destination;
   }
 
   private void processSlice(TableSlice slice) {
     orderBy(slice);
     processAggregateFunctions(slice);
     processNumberingFunctions(slice);
   }
 
 
/** From the given slice perform all aggregate functions by setting values in the appropriate destination column in the destination column */

private void processAggregateFunctions(TableSlice slice) {
  ImmutableList<ArgumentList.FunctionCall> aggregateFunctions = query.getAggregateFunctions();
  
  for (ArgumentList.FunctionCall functionCall : aggregateFunctions) {
    Column<?> destinationColumn = destination.column(functionCall.getColumnName());
    Column<?> sourceColumn = slice.column(functionCall.getColumnName());
    
    switch (functionCall.getFunction()) {
      case SUM:
        destinationColumn.append(sourceColumn.sum());
        break;
      case MIN:
        destinationColumn.append(sourceColumn.min());
        break;
      case MAX:
        destinationColumn.append(sourceColumn.max());
        break;
      case MEAN:
        destinationColumn.append(sourceColumn.mean());
        break;
      case MEDIAN:
        destinationColumn.append(sourceColumn.median());
        break;
      // Add more cases for other aggregate functions if needed
      
      default:
        throw new IllegalArgumentException("Unsupported aggregate function: " + functionCall.getFunction());
    }
  }
}
 

}