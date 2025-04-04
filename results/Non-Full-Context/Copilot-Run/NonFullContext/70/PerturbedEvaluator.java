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
 
     /**
      * Given a new proposed label or directoryLabel for a file, check against
      * existing files if a duplicate directoryLabel/label combination would be
      * created.
      */
     public static boolean conflictsWithExistingFilenames(String pathPlusFilename, List<FileMetadata> fileMetadatas) {
         List<String> filePathsAndNames = getPathsAndFileNames(fileMetadatas);
         return filePathsAndNames.contains(pathPlusFilename);
     }
 
     /**
      * Given a DatasetVersion, and the newFiles about to be added to the 
      * version iterate across all the files (including their
      * paths) and return any duplicates.
      *
      * @param datasetVersion
      * @param newFiles
      * @return A Collection of Strings in the form of path/to/file.txt
      */
     public static Collection<String> findDuplicateFilenames(DatasetVersion datasetVersion, List<DataFile> newFiles) {
         List<FileMetadata> toTest = new ArrayList();
         datasetVersion.getFileMetadatas().forEach((fm) -> {
             toTest.add(fm);
         });
         newFiles.forEach((df) -> {
             toTest.add(df.getFileMetadata());
         });
         List<String> allFileNamesWithPaths = getPathsAndFileNames(toTest);
         return findDuplicates(allFileNamesWithPaths);
     }
 
     // https://stackoverflow.com/questions/7414667/identify-duplicates-in-a-list
     private static <T> Set<T> findDuplicates(Collection<T> collection) {
         Set<T> duplicates = new HashSet<>();
         Set<T> uniques = new HashSet<>();
         for (T t : collection) {
             if (!uniques.add(t)) {
                 duplicates.add(t);
             }
         }
         return duplicates;
     }
 
     /**
      * @return A List of Strings in the form of path/to/file.txt
      */
     public static List<String> getPathsAndFileNames(List<FileMetadata> fileMetadatas) {
         List<String> allFileNamesWithPaths = new ArrayList<>();
         for (FileMetadata fileMetadata : fileMetadatas) {
             String directoryLabel = fileMetadata.getDirectoryLabel();
             String path = "";
             if (directoryLabel != null) {
                 path = directoryLabel + "/";
             }
             String pathAndfileName = path + fileMetadata.getLabel();
             allFileNamesWithPaths.add(pathAndfileName);
         }
         return allFileNamesWithPaths;
     }
 
     // This method is called on a single file, when we need to modify the name 
     // of an already ingested/persisted datafile. For ex., when we have converted
     // a file to tabular data, and want to update the extension accordingly. 
     public static void modifyExistingFilename(DatasetVersion version, FileMetadata fileMetadata, String newFilename) {
         // Step 1: create list of existing path names from all FileMetadata in the DatasetVersion
         // unique path name: directoryLabel + file separator + fileLabel
         fileMetadata.setLabel(newFilename);
         Set<String> pathNamesExisting = existingPathNamesAsSet(version, fileMetadata);
         fileMetadata.setLabel(duplicateFilenameCheck(fileMetadata, pathNamesExisting));
 
     }
 
     // unique path name: directoryLabel + file separator + fileLabel
     public static String makePathName(String directoryName, String fileName) {
         String pathName;
         if (directoryName != null && !directoryName.isEmpty()) {
             pathName = directoryName + File.separator + fileName;
         } else {
             pathName = fileName;
         }
         return pathName;
     }
 
 
/** Adding -[number] to the base name generates a new filename */
 public static String generateNewFileName(final String fileName){}

 

}