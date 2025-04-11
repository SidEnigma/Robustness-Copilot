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
 
 
/** Execute all aggregate functions for the given slice setting values in the appropriate  destination column. */
 private void processAggregateFunctions(TableSlice slice){
    for (AggregateFunction function : query.getAggregateFunctions()) {
      Column column = function.getColumn();
      Column destinationColumn = destination.column(column.name());
      switch (function.getType()) {
        case COUNT:
          destinationColumn.fill(slice.count());
          break;
        case SUM:
          destinationColumn.fill(slice.sum(column));
          break;
        case MIN:
          destinationColumn.fill(slice.min(column));
          break;
        case MAX:
          destinationColumn.fill(slice.max(column));
          break;
        case MEAN:
          destinationColumn.fill(slice.mean(column));
          break;
        case MEDIAN:
          destinationColumn.fill(slice.median(column));
          break;
        case STDDEV:
          destinationColumn.fill(slice.stddev(column));
          break;
        case VARIANCE:
          destinationColumn.fill(slice.variance(column));
          break;
        case COUNT_DISTINCT:
          destinationColumn.fill(slice.countDistinct(column));
          break;
        case SUM_DISTINCT:
          destinationColumn.fill(slice.sumDistinct(column));
          break;
        case MIN_DISTINCT:
          destinationColumn.fill(slice.minDistinct(column));
          break;
        case MAX_DISTINCT:
          destinationColumn.fill(slice.maxDistinct(column));
          break;
        case MEAN_DISTINCT:
          destinationColumn.fill(slice.meanDistinct(column));
          break;
        case MEDIAN_DISTINCT:
          destinationColumn.fill(slice.medianDistinct(column));
          break;
        case STDDEV_DISTINCT:
          destinationColumn.fill(slice.stddevDistinct(column));
          break;
        case VARIANCE_DISTINCT:
          destinationColumn.fill(slice.varianceDistinct(column));
          break;
        default:
          throw new IllegalArgumentException("Unsupported aggregate function: " +   
 }

 

}