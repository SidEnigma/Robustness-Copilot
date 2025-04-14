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
 
   /**
    * Execute all aggregate functions for the given slice setting values in the appropriate
    * destination column.
    */
   private void processAggregateFunctions(TableSlice slice) {
     for (String toColumn : query.getArgumentList().getAggregateFunctions().keySet()) {
       FunctionCall<AggregateFunctions> functionCall =
           query.getArgumentList().getAggregateFunctions().get(toColumn);
 
       AggregateFunctions aggregateFunction = functionCall.getFunction();
       Column<?> sourceColumn = query.getTable().column(functionCall.getSourceColumnName());
       validateColumn(aggregateFunction, sourceColumn);
 
       Column<?> destinationColumn = destination.column(functionCall.getDestinationColumnName());
       new WindowSlider(
               query.getWindowFrame(), aggregateFunction, slice, sourceColumn, destinationColumn)
           .execute();
     }
   }
 
 
/** Select the given slide and execute all numbering functions, setting the values in the correct destination columns. */
 private void processNumberingFunctions(TableSlice slice){
    for (String toColumn : query.getArgumentList().getNumberingFunctions().keySet()) {
      FunctionCall<NumberingFunctions> functionCall =
          query.getArgumentList().getNumberingFunctions().get(toColumn);
  
      NumberingFunctions numberingFunction = functionCall.getFunction();
      Column<?> sourceColumn = query.getTable().column(functionCall.getSourceColumnName());
      validateColumn(numberingFunction, sourceColumn);
  
      Column<?> destinationColumn = destination.column(functionCall.getDestinationColumnName());
      new WindowSlider(
              query.getWindowFrame(), numberingFunction, slice, sourceColumn, destinationColumn)
          .execute();
    }
  }
  
    private void validateColumn(AnalyticFunction function, Column<?> column) {
      if (function.getRequiredColumnType() != column.type()) {
        throw new IllegalArgumentException(
            "Column " + column.name() + " is of type " + column.type() + " but " + function.getName() + " requires "
                + function.getRequiredColumnType());
      }
    }
  
    private void orderBy(TableSlice slice) {
      if (rowComparator != null) {
        slice.sort(rowComparator);
      }
    }
  
    private void addColumns() {
      for (String columnName : query.getArgumentList().getColumns()) {
        destination.addColumn(query.getTable().column(columnName));
      }
    }
  
    private TableSlice partition() {
      TableSlice slice = query.getTable().slice(query.getWindowFrame());
      if (query.getArgumentList().getPartitionBy().isPresent()) {
        String partitionBy = query.getArgumentList().getPartitionBy().get();
        Column<?> partitionColumn = query.getTable().column(partitionBy);
        ImmutableList<Object> uniqueValues =
            partitionColumn.uniqueValues().stream().collect(ImmutableList.toImmutableList());
        for (Object value : uniqueValues) {
          TableSlice    
 }

 

}