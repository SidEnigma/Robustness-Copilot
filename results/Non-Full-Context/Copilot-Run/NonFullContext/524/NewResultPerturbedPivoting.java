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
 
 
/** Checks a list of new data files for duplicate names, renaming all duplicates to make sure they are unique. */

public static void checkForDuplicateFileNamesFinal(DatasetVersion version, List<DataFile> newFiles, DataFile fileToReplace) {
    Set<String> existingFileNames = new HashSet<>();
    List<DataFile> filesToRename = new ArrayList<>();

    // Add existing file names to the set
    for (FileMetadata fileMetadata : version.getFileMetadatas()) {
        existingFileNames.add(fileMetadata.getLabel());
    }

    // Check for duplicate names in the new files
    for (DataFile newFile : newFiles) {
        String fileName = newFile.getFileMetadata().getLabel();
        if (existingFileNames.contains(fileName)) {
            filesToRename.add(newFile);
        } else {
            existingFileNames.add(fileName);
        }
    }

    // Rename the duplicate files
    for (DataFile file : filesToRename) {
        String fileName = file.getFileMetadata().getLabel();
        String baseName = FileUtil.getBaseName(fileName);
        String extension = FileUtil.getFileExtension(fileName);
        int counter = 1;

        // Generate a unique file name
        String uniqueFileName = baseName + "_" + counter + "." + extension;
        while (existingFileNames.contains(uniqueFileName)) {
            counter++;
            uniqueFileName = baseName + "_" + counter + "." + extension;
        }

        // Update the file name and add it to the set
        file.getFileMetadata().setLabel(uniqueFileName);
        existingFileNames.add(uniqueFileName);
    }

    // Replace the file if necessary
    if (fileToReplace != null) {
        version.getFileMetadatas().remove(fileToReplace.getFileMetadata());
        version.getFileMetadatas().add(fileToReplace.getFileMetadata());
    }
}
 

}