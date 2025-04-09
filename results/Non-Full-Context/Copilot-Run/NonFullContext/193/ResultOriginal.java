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
 
 
/** Reads a CSV file, converts it into a dataverse DataTable. */
 public TabularDataIngest read(BufferedInputStream stream, File dataFile) throws IOException{
                    
 }

 

}