package tech.tablesaw.io;
 
 import static tech.tablesaw.api.ColumnType.SKIP;
 
 import com.google.common.base.Strings;
 import com.google.common.collect.Lists;
 import com.google.common.collect.Streams;
 import com.univocity.parsers.common.AbstractParser;
 import java.io.Reader;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.NoSuchElementException;
 import java.util.Optional;
 import java.util.Random;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import tech.tablesaw.api.ColumnType;
 import tech.tablesaw.api.Table;
 import tech.tablesaw.columns.AbstractColumnParser;
 import tech.tablesaw.columns.Column;
 
 public abstract class FileReader {
 
   private static Logger logger = LoggerFactory.getLogger(FileReader.class);
   private static final int UNLIMITED_SAMPLE_SIZE = -1;
 
   /**
    * @deprecated Use {@link #getColumnTypes(Reader, ReadOptions, int, AbstractParser, String[])} }
    */
   @Deprecated
   public ColumnType[] getColumnTypes(
       Reader reader, ReadOptions options, int linesToSkip, AbstractParser<?> parser) {
     return getColumnTypes(reader, options, linesToSkip, parser, null);
   }
   /**
    * Returns an array containing the inferred columnTypes for the file being read, as calculated by
    * the ColumnType inference logic. These types may not be correct.
    */
   public ColumnType[] getColumnTypes(
       Reader reader,
       ReadOptions options,
       int linesToSkip,
       AbstractParser<?> parser,
       String[] columnNames) {
 
     if (parser.getContext() == null) parser.beginParsing(reader);
 
     for (int i = 0; i < linesToSkip; i++) {
       parser.parseNext();
     }
 
     ColumnTypeDetector detector = new ColumnTypeDetector(options.columnTypesToDetect());
 
     ColumnType[] columnTypes =
         detector.detectColumnTypes(
             new Iterator<String[]>() {
 
               String[] nextRow = parser.parseNext();
 
               @Override
               public boolean hasNext() {
                 return nextRow != null;
               }
 
               @Override
               public String[] next() {
                 if (!hasNext()) {
                   throw new NoSuchElementException();
                 }
                 String[] tmp = nextRow;
                 nextRow = parser.parseNext();
                 return tmp;
               }
             },
             options);
 
     // If there are columnTypes configured by the user use them
     for (int i = 0; i < columnTypes.length; i++) {
       boolean hasColumnName = columnNames != null && i < columnNames.length;
       Optional<ColumnType> configuredColumnType =
           options.columnTypeReadOptions().columnType(i, hasColumnName ? columnNames[i] : null);
       if (configuredColumnType.isPresent()) {
         columnTypes[i] = configuredColumnType.get();
       }
     }
 
     return columnTypes;
   }
 
   private String cleanName(String name) {
     return name.trim();
   }
 
   /** Returns the column names for each column in the source. */
   public String[] getColumnNames(
       ReadOptions options,
       ReadOptions.ColumnTypeReadOptions columnTypeReadOptions,
       AbstractParser<?> parser) {
 
     if (options.header()) {
 
       String[] headerNames = parser.parseNext();
 
       // work around issue where Univocity returns null if a column has no header.
       for (int i = 0; i < headerNames.length; i++) {
         if (headerNames[i] == null) {
           headerNames[i] = "C" + i;
         } else {
           headerNames[i] = headerNames[i].trim();
         }
       }
       if (options.allowDuplicateColumnNames()) {
         renameDuplicateColumnHeaders(headerNames);
       }
       return headerNames;
     } else {
       // Placeholder column names for when the file read has no header
       int columnCount =
           columnTypeReadOptions.columnTypes() != null
               ? columnTypeReadOptions.columnTypes().length
               : 0;
       String[] headerNames = new String[columnCount];
       for (int i = 0; i < columnCount; i++) {
         headerNames[i] = "C" + i;
       }
       return headerNames;
     }
   }
 
 
/** Rename any column that appears more than once. */
 private void renameDuplicateColumnHeaders(String[] headerNames){}

 

}