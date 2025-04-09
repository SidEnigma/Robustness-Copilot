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
 
 
/** Reads a CSV file, converts it to DataTable dataverse. */

public TabularDataIngest read(BufferedInputStream stream, File dataFile) throws IOException {
    init();
    
    try (CSVParser parser = new CSVParser(new InputStreamReader(stream), inFormat);
         PrintWriter logWriter = new PrintWriter(new FileWriter(dataFile.getAbsolutePath() + ".log"))) {
        
        DataTable dataTable = new DataTable();
        List<DataVariable> dataVariables = new ArrayList<>();
        List<String> variableNames = new ArrayList<>();
        List<String> variableLabels = new ArrayList<>();
        List<String> variableTypes = new ArrayList<>();
        List<String> variableFormatCategories = new ArrayList<>();
        List<String> variableFormatTypes = new ArrayList<>();
        List<String> variableFormatOptions = new ArrayList<>();
        List<String> variableUnf = new ArrayList<>();
        List<String> variableIntervalDiscrete = new ArrayList<>();
        List<String> variableIntervalContinuous = new ArrayList<>();
        List<String> variableIntervalType = new ArrayList<>();
        List<String> variableIntervalMissingValue = new ArrayList<>();
        List<String> variableIntervalMissingValueNotes = new ArrayList<>();
        List<String> variableIntervalLabel = new ArrayList<>();
        List<String> variableIntervalLabelNotes = new ArrayList<>();
        List<String> variableIntervalUniverse = new ArrayList<>();
        List<String> variableIntervalUniverseNotes = new ArrayList<>();
        List<String> variableIntervalCategories = new ArrayList<>();
        List<String> variableIntervalCategoriesNotes = new ArrayList<>();
        List<String> variableIntervalCategoriesValue = new ArrayList<>();
        List<String> variableIntervalCategoriesLabel = new ArrayList<>();
        List<String> variableIntervalCategoriesMissing = new ArrayList<>();
        List<String> variableIntervalCategoriesMissingNotes = new ArrayList<>();
        List<String> variableIntervalCategoriesUniverse = new ArrayList<>();
        List<String> variableIntervalCategoriesUniverseNotes = new ArrayList<>();
        
        List<CSVRecord> records = parser.getRecords();
        int numRecords = records.size();
        
        // Process header row
        CSVRecord header = records.get(0);
        int numColumns = header.size();
        for (int i = 0; i < numColumns; i++) {
            String variableName = header.get(i);
            variableNames.add(variableName);
            variableLabels.add(variableName);
            variableTypes.add("character");
            variableFormatCategories.add("");
            variableFormatTypes.add("");
            variableFormatOptions.add("");
            variableUnf.add("");
            variableIntervalDiscrete.add("");
            variableIntervalContinuous.add("");
            variableIntervalType.add("");
            variableIntervalMissingValue.add("");
            variableIntervalMissingValueNotes.add("");
            variableIntervalLabel.add("");
            variableIntervalLabelNotes.add("");
            variableIntervalUniverse.add("");
            variableIntervalUniverseNotes.add("");
            variableIntervalCategories.add("");
            variableIntervalCategoriesNotes.add("");
            variableIntervalCategoriesValue.add("");
            variableIntervalCategoriesLabel.add("");
            variableIntervalCategoriesMissing.add("");
            variableIntervalCategoriesMissingNotes.add("");
            variableIntervalCategoriesUniverse.add("");
            variableIntervalCategoriesUniverseNotes.add("");
            
            DataVariable dataVariable = new DataVariable();
            dataVariable.setName(variableName);
            dataVariable.setLabel(variableName);
            dataVariable.setType("character");
            dataVariable.setFormatCategory("");
            dataVariable.setFormatType("");
            dataVariable.setFormatOptions("");
            dataVariable.setUnf("");
            dataVariable.setIntervalDiscrete("");
            dataVariable.setIntervalContinuous("");
            dataVariable.setIntervalType("");
            dataVariable.setIntervalMissingValue("");
            dataVariable.setIntervalMissingValueNotes("");
            dataVariable.setIntervalLabel("");
            dataVariable.setIntervalLabelNotes("");
            dataVariable.setIntervalUniverse("");
            dataVariable.setIntervalUniverseNotes("");
            dataVariable.setIntervalCategories("");
            dataVariable.setIntervalCategoriesNotes("");
            dataVariable.setIntervalCategoriesValue("");
            dataVariable.setIntervalCategoriesLabel("");
            dataVariable.setIntervalCategoriesMissing("");
            dataVariable.setIntervalCategoriesMissingNotes("");
            dataVariable.setIntervalCategoriesUniverse("");
            dataVariable.setIntervalCategoriesUniverseNotes("");
            
            dataVariables.add(dataVariable);
        }
        
        dataTable.setDataVariables(dataVariables);
        
        // Process data rows
        for (int i = 1; i < numRecords; i++) {
            CSVRecord record = records.get(i);
            for (int j = 0; j < numColumns; j++) {
                String value = record.get(j);
                DataVariable dataVariable = dataVariables.get(j);
                dataVariable.addValue(value);
            }
        }
        
        dataTable.setVariableNames(variableNames);
        dataTable.setVariableLabels(variableLabels);
        dataTable.setVariableTypes(variableTypes);
        dataTable.setVariableFormatCategories(variableFormatCategories);
        dataTable.setVariableFormatTypes(variableFormatTypes);
        dataTable.setVariableFormatOptions(variableFormatOptions);
        dataTable.setVariableUnf(variableUnf);
        dataTable.setVariableIntervalDiscrete(variableIntervalDiscrete);
        dataTable.setVariableIntervalContinuous(variableIntervalContinuous);
        dataTable.setVariableIntervalType(variableIntervalType);
        dataTable.setVariableIntervalMissingValue(variableIntervalMissingValue);
        dataTable.setVariableIntervalMissingValueNotes(variableIntervalMissingValueNotes);
        dataTable.setVariableIntervalLabel(variableIntervalLabel);
        dataTable.setVariableIntervalLabelNotes(variableIntervalLabelNotes);
        dataTable.setVariableIntervalUniverse(variableIntervalUniverse);
        dataTable.setVariableIntervalUniverseNotes(variableIntervalUniverseNotes);
        dataTable.setVariableIntervalCategories(variableIntervalCategories);
        dataTable.setVariableIntervalCategoriesNotes(variableIntervalCategoriesNotes);
        dataTable.setVariableIntervalCategoriesValue(variableIntervalCategoriesValue);
        dataTable.setVariableIntervalCategoriesLabel(variableIntervalCategoriesLabel);
        dataTable.setVariableIntervalCategoriesMissing(variableIntervalCategoriesMissing);
        dataTable.setVariableIntervalCategoriesMissingNotes(variableIntervalCategoriesMissingNotes);
        dataTable.setVariableIntervalCategoriesUniverse(variableIntervalCategoriesUniverse);
        dataTable.setVariableIntervalCategoriesUniverseNotes(variableIntervalCategoriesUniverseNotes);
        
        return new TabularDataIngest(dataTable, logWriter.toString());
    } catch (IOException e) {
        logger.warning("Error reading CSV file: " + e.getMessage());
        throw e;
    }
}
 

}