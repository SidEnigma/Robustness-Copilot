/*
  Copyright (C) 2005-2013, by the President and Fellows of Harvard College.
 
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
 
  http://www.apache.org/licenses/LICENSE-2.0
 
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 
  Dataverse Network - A web application to share, preserve and analyze research data.
  Developed at the Institute for Quantitative Social Science, Harvard University.
  Version 3.0.
  */
 package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.csv;
 
 import java.io.FileReader;
 import java.io.InputStreamReader;
 
 import edu.harvard.iq.dataverse.DataTable;
 import edu.harvard.iq.dataverse.datavariable.DataVariable;
 
 import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;
 import edu.harvard.iq.dataverse.ingest.tabulardata.spi.TabularDataFileReaderSpi;
 import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
 import edu.harvard.iq.dataverse.util.BundleUtil;
 import java.io.BufferedInputStream;
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.math.BigDecimal;
 import java.math.MathContext;
 import java.math.RoundingMode;
 import java.text.ParseException;
 import java.text.ParsePosition;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Date;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.logging.Logger;
 import org.apache.commons.csv.CSVFormat;
 import org.apache.commons.lang3.StringUtils;
 import org.apache.commons.csv.CSVParser;
 import org.apache.commons.csv.CSVPrinter;
 import org.apache.commons.csv.CSVRecord;
 
 /**
  * Dataverse 4.0 implementation of <code>TabularDataFileReader</code> for the
  * plain CSV file with a variable name header.
  *
  *
  * @author Oscar Smith
  *
  * This implementation uses the Apache CSV Parser
  */
 public class CSVFileReader extends TabularDataFileReader {
 
     private static final Logger logger = Logger.getLogger(CSVFileReader.class.getPackage().getName());
     private static final int DIGITS_OF_PRECISION_DOUBLE = 15;
     private static final String FORMAT_IEEE754 = "%+#." + DIGITS_OF_PRECISION_DOUBLE + "e";
     private MathContext doubleMathContext;
     private CSVFormat inFormat;
     //private final Set<Character> firstNumCharSet = new HashSet<>();
 
     // DATE FORMATS
     private static SimpleDateFormat[] DATE_FORMATS = new SimpleDateFormat[]{
         new SimpleDateFormat("yyyy-MM-dd"), //new SimpleDateFormat("yyyy/MM/dd"),
     //new SimpleDateFormat("MM/dd/yyyy"),
     //new SimpleDateFormat("MM-dd-yyyy"),
     };
 
     // TIME FORMATS
     private static SimpleDateFormat[] TIME_FORMATS = new SimpleDateFormat[]{
         // Date-time up to seconds with timezone, e.g. 2013-04-08 13:14:23 -0500
         new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"),
         // Date-time up to seconds and no timezone, e.g. 2013-04-08 13:14:23
         new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
     };
 
     public CSVFileReader(TabularDataFileReaderSpi originator, char delim) {
         super(originator);
         if (delim == ','){
             inFormat = CSVFormat.EXCEL;
         } else if (delim == '\t'){
             inFormat = CSVFormat.TDF;
         }
     }
 
     private void init() throws IOException {
         doubleMathContext = new MathContext(DIGITS_OF_PRECISION_DOUBLE, RoundingMode.HALF_EVEN);
         //firstNumCharSet.addAll(Arrays.asList(new Character[]{'+', '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'}));
     }
 
 
/** A dataverse DataTable is created when acsv file is read and converted. */

public TabularDataIngest read(BufferedInputStream stream, File dataFile) throws IOException {
    // Initialize the reader
    init();

    // Create a new DataTable
    DataTable dataTable = new DataTable();

    // Create a list to store the data variables
    List<DataVariable> dataVariables = new ArrayList<>();

    // Create a set to store the unique variable names
    Set<String> variableNames = new HashSet<>();

    // Create a map to store the variable index and its corresponding data type
    Map<Integer, String> variableIndexToTypeMap = new HashMap<>();

    // Create a map to store the variable index and its corresponding missing values
    Map<Integer, Set<String>> variableIndexToMissingValuesMap = new HashMap<>();

    // Create a map to store the variable index and its corresponding categories
    Map<Integer, Set<String>> variableIndexToCategoriesMap = new HashMap<>();

    // Create a map to store the variable index and its corresponding label
    Map<Integer, String> variableIndexToLabelMap = new HashMap<>();

    // Create a map to store the variable index and its corresponding format
    Map<Integer, String> variableIndexToFormatMap = new HashMap<>();

    // Create a map to store the variable index and its corresponding weight variable index
    Map<Integer, Integer> variableIndexToWeightVariableIndexMap = new HashMap<>();

    // Create a map to store the variable index and its corresponding index variable index
    Map<Integer, Integer> variableIndexToIndexVariableIndexMap = new HashMap<>();

    // Create a map to store the variable index and its corresponding UNF
    Map<Integer, String> variableIndexToUnfMap = new HashMap<>();

    // Create a map to store the variable index and its corresponding summary statistics
    Map<Integer, Map<String, BigDecimal>> variableIndexToSummaryStatisticsMap = new HashMap<>();

    // Create a map to store the variable index and its corresponding invalid range
    Map<Integer, Map<String, BigDecimal>> variableIndexToInvalidRangeMap = new HashMap<>();

    // Create a map to store the variable index and its corresponding UNF precision
    Map<Integer, Integer> variableIndexToUnfPrecisionMap = new HashMap<>();

    try (CSVParser parser = new CSVParser(new InputStreamReader(stream), inFormat)) {
        // Get the CSV records
        List<CSVRecord> records = parser.getRecords();

        // Check if the CSV file is empty
        if (records.isEmpty()) {
            throw new IOException("CSV file is empty");
        }

        // Get the header record
        CSVRecord headerRecord = records.get(0);

        // Iterate over the header record to create the data variables
        for (int i = 0; i < headerRecord.size(); i++) {
            String variableName = headerRecord.get(i);

            // Check if the variable name is empty or already exists
            if (StringUtils.isBlank(variableName) || variableNames.contains(variableName)) {
                throw new IOException("Invalid variable name: " + variableName);
            }

            // Create a new data variable
            DataVariable dataVariable = new DataVariable();

            // Set the variable name
            dataVariable.setName(variableName);

            // Add the data variable to the list
            dataVariables.add(dataVariable);

            // Add the variable name to the set
            variableNames.add(variableName);

            // Add the variable index and its corresponding data type to the map
            variableIndexToTypeMap.put(i, "numeric");

            // Add the variable index and its corresponding missing values to the map
            variableIndexToMissingValuesMap.put(i, new HashSet<>());

            // Add the variable index and its corresponding categories to the map
            variableIndexToCategoriesMap.put(i, new HashSet<>());

            // Add the variable index and its corresponding label to the map
            variableIndexToLabelMap.put(i, "");

            // Add the variable index and its corresponding format to the map
            variableIndexToFormatMap.put(i, "");

            // Add the variable index and its corresponding weight variable index to the map
            variableIndexToWeightVariableIndexMap.put(i, -1);

            // Add the variable index and its corresponding index variable index to the map
            variableIndexToIndexVariableIndexMap.put(i, -1);

            // Add the variable index and its corresponding UNF to the map
            variableIndexToUnfMap.put(i, "");

            // Add the variable index and its corresponding summary statistics to the map
            variableIndexToSummaryStatisticsMap.put(i, new HashMap<>());

            // Add the variable index and its corresponding invalid range to the map
            variableIndexToInvalidRangeMap.put(i, new HashMap<>());

            // Add the variable index and its corresponding UNF precision to the map
            variableIndexToUnfPrecisionMap.put(i, 0);
        }

        // Iterate over the records to populate the data variables
        for (int i = 1; i < records.size(); i++) {
            CSVRecord record = records.get(i);

            // Iterate over the record fields
            for (int j = 0; j < record.size(); j++) {
                String value = record.get(j);

                // Get the data variable for the current field
                DataVariable dataVariable = dataVariables.get(j);

                // Get the variable index
                int variableIndex = j;

                // Get the variable type
                String variableType = variableIndexToTypeMap.get(variableIndex);

                // Get the variable missing values
                Set<String> missingValues = variableIndexToMissingValuesMap.get(variableIndex);

                // Get the variable categories
                Set<String> categories = variableIndexToCategoriesMap.get(variableIndex);

                // Get the variable label
                String label = variableIndexToLabelMap.get(variableIndex);

                // Get the variable format
                String format = variableIndexToFormatMap.get(variableIndex);

                // Get the variable weight variable index
                int weightVariableIndex = variableIndexToWeightVariableIndexMap.get(variableIndex);

                // Get the variable index variable index
                int indexVariableIndex = variableIndexToIndexVariableIndexMap.get(variableIndex);

                // Get the variable UNF
                String unf = variableIndexToUnfMap.get(variableIndex);

                // Get the variable summary statistics
                Map<String, BigDecimal> summaryStatistics = variableIndexToSummaryStatisticsMap.get(variableIndex);

                // Get the variable invalid range
                Map<String, BigDecimal> invalidRange = variableIndexToInvalidRangeMap.get(variableIndex);

                // Get the variable UNF precision
                int unfPrecision = variableIndexToUnfPrecisionMap.get(variableIndex);

                // Check if the value is missing
                if (StringUtils.isBlank(value)) {
                    missingValues.add(value);
                } else {
                    // Parse the value based on the variable type
                    if (variableType.equals("numeric")) {
                        try {
                            BigDecimal numericValue = new BigDecimal(value, doubleMathContext);
                            dataVariable.addNumericValue(numericValue);
                        } catch (NumberFormatException ex) {
                            throw new IOException("Invalid numeric value: " + value);
                        }
                    } else if (variableType.equals("character")) {
                        dataVariable.addCharacterValue(value);
                    } else if (variableType.equals("factor")) {
                        categories.add(value);
                        dataVariable.addFactorValue(value);
                    } else if (variableType.equals("date")) {
                        Date dateValue = parseDate(value);
                        dataVariable.addDateValue(dateValue);
                    } else if (variableType.equals("time")) {
                        Date timeValue = parseTime(value);
                        dataVariable.addTimeValue(timeValue);
                    } else if (variableType.equals("datetime")) {
                        Date datetimeValue = parseDatetime(value);
                        dataVariable.addDatetimeValue(datetimeValue);
                    }
                }
            }
        }
    } catch (IOException ex) {
        throw ex;
    } catch (Exception ex) {
        throw new IOException("Error reading CSV file", ex);
    }

    // Set the data variables in the DataTable
    dataTable.setDataVariables(dataVariables);

    // Create a new TabularDataIngest
    TabularDataIngest tabularDataIngest = new TabularDataIngest();

    // Set the DataTable in the TabularDataIngest
    tabularDataIngest.setDataTable(dataTable);

    return tabularDataIngest;
}
 

}