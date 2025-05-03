/*
    Copyright (C) 2005-2016, by the President and Fellows of Harvard College.
 
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
    Version 4.0.
  */
 package edu.harvard.iq.dataverse.ingest;
 
 import edu.harvard.iq.dataverse.DataFile;
 import edu.harvard.iq.dataverse.Dataset;
 import edu.harvard.iq.dataverse.DatasetVersion;
 import edu.harvard.iq.dataverse.FileMetadata;
 import edu.harvard.iq.dataverse.util.FileUtil;
 
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Set;
 import java.util.logging.Logger;
 import javax.json.Json;
 import javax.json.JsonArrayBuilder;
 import javax.json.JsonObjectBuilder;
 import org.dataverse.unf.UNFUtil;
 import org.dataverse.unf.UnfException;
 
 /**
  * Various helper methods used by IngestServiceBean.
  *
  * @author bmckinney
  */
 public class IngestUtil {
 
     private static final Logger logger = Logger.getLogger(IngestUtil.class.getCanonicalName());
 
     /**
      * Checks a list of new data files for duplicate names, renaming any
      * duplicates to ensure that they are unique.
      *
      * @param version the dataset version
      * @param newFiles the list of new data files to add to it
      * @param fileToReplace
      */
     public static void checkForDuplicateFileNamesFinal(DatasetVersion version, List<DataFile> newFiles, DataFile fileToReplace) {
 
         // Step 1: create list of existing path names from all FileMetadata in the DatasetVersion
         // unique path name: directoryLabel + file separator + fileLabel
         Set<String> pathNamesExisting = existingPathNamesAsSet(version, ((fileToReplace == null) ? null : fileToReplace.getFileMetadata()));
         // Step 2: check each new DataFile against the list of path names, if a duplicate create a new unique file name
         for (Iterator<DataFile> dfIt = newFiles.iterator(); dfIt.hasNext();) {
 
             FileMetadata fm = dfIt.next().getFileMetadata();
 
             fm.setLabel(duplicateFilenameCheck(fm, pathNamesExisting));
         }
     }
 
     /**
      * Checks if the unique file path of the supplied fileMetadata is already on
      * the list of the existing files; and if so, keeps generating a new name
      * until it is unique. Returns the final file name. (i.e., it only modifies
      * the filename, and not the folder name, in order to achieve uniqueness)
      *
      * @param fileMetadata supplied FileMetadata
      * @param existingFileNames a set of the already existing pathnames
      * @return a [possibly] new unique filename
      */
     public static String duplicateFilenameCheck(FileMetadata fileMetadata, Set<String> existingFileNames) {
         if (existingFileNames == null) {
             existingFileNames = existingPathNamesAsSet(fileMetadata.getDatasetVersion());
         }
 
         String fileName = fileMetadata.getLabel();
         String directoryName = fileMetadata.getDirectoryLabel();
         String pathName = makePathName(directoryName, fileName);
 
         while (existingFileNames.contains(pathName)) {
             fileName = IngestUtil.generateNewFileName(fileName);
             pathName = IngestUtil.makePathName(directoryName, fileName);
         }
 
         existingFileNames.add(pathName);
         return fileName;
     }
 
     /**
      * Given an existing file that may or may not have a directoryLabel, take
      * the incoming label and/or directory label and combine it with what's in
      * the existing file, overwriting and filling in as necessary.
      */
     public static String getPathAndFileNameToCheck(String incomingLabel, String incomingDirectoryLabel, String existingLabel, String existingDirectoryLabel) {
         String labelToReturn = existingLabel;
         String directoryLabelToReturn = existingDirectoryLabel;
         if (incomingLabel != null) {
             labelToReturn = incomingLabel;
         }
         if (incomingDirectoryLabel != null) {
             directoryLabelToReturn = incomingDirectoryLabel;
         }
         if (directoryLabelToReturn != null) {
             return directoryLabelToReturn + "/" + labelToReturn;
         } else {
             return labelToReturn;
         }
     }
 
 
/** Check existing files to see if a duplicate directoryLabel/label combination would be produced if a new label or directoryLabel is proposed for a file. */
 public static boolean conflictsWithExistingFilenames(String pathPlusFilename, List<FileMetadata> fileMetadatas){}

 

}