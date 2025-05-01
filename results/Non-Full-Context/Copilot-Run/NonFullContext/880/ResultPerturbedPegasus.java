// Copyright (c) 2018, 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.operator.helpers;
 
 import java.io.IOException;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.nio.file.FileSystem;
 import java.nio.file.FileSystems;
 import java.nio.file.Files;
 import java.nio.file.Path;
 import java.nio.file.Paths;
 import java.util.Collections;
 import java.util.Map;
 import java.util.function.Function;
 import java.util.stream.Collectors;
 import java.util.stream.Stream;
 
 import oracle.kubernetes.operator.logging.LoggingFacade;
 import oracle.kubernetes.operator.logging.LoggingFactory;
 import oracle.kubernetes.operator.logging.MessageKeys;
 
 /**
  * This class can load a group of files under a specified classpath directory into a map. It handles
  * both files on the file system and in a JAR.
  */
 class FileGroupReader {
   private static final LoggingFacade LOGGER = LoggingFactory.getLogger("Operator", "Operator");
 
   private final String pathToGroup;
 
   @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"}) // keep non-final for unit test
   private static Function<URI, Path> uriToPath = Paths::get;
   
   /**
    * Creates a reader for a specific file location.
    *
    * @param pathToGroup the top-level directory containing the files, relative to the classpath.
    */
   FileGroupReader(String pathToGroup) {
     this.pathToGroup = pathToGroup;
   }
 
 
/** Load the contents of the files into a map. */
 static Map<String, String> loadContents(Path rootDir) throws IOException{
    return Files.walk(rootDir)
      .filter(Files::isRegularFile)
      .map(FileGroupReader::getFileName)
      .collect(Collectors.toMap(Function.identity(), FileGroupReader::readFile));
  }
  
    private static String getFileName(Path path) {
      return path.getFileName().toString();
    }
  
    private static String readFile(Path path) throws IOException {
      return new String(Files.readAllBytes(path));
    }
  
    /**
      * Load the contents of the files into a map.
      *
      * @return the contents of the files in the group.
      */
    Map<String, String> loadContents() throws IOException {
      return loadContents(getRootDir());
    }
  
    private Path getRootDir() throws IOException {
      Path rootDir = getPathToGroup();
      if (Files.isDirectory(rootDir)) {
        return rootDir;
      }
      try {
        URI uri = new URI(pathToGroup);
        if (uri.getScheme().equals("jar")) {
          return getPathFromJar(uri);
        }
      } catch (URISyntaxException e) {
        LOGGER.log(MessageKeys.OPERATOR_ERROR_LOADING_FILE_GROUP, pathToGroup, e.getMessage());
        throw new IOException(e);
      }
      LOGGER.log(MessageKeys.OPERATOR_ERROR_LOADING_FILE_GROUP, pathToGroup, "not a directory or a JAR");
      throw new IOException("not a directory or a JAR");
    }
  
    private Path getPathToGroup() throws IOException {
      Path rootDir = getPathFromClasspath(pathToGroup);
      if (Files.isDirectory(rootDir)) {
        return rootDir;
      }
      LOGGER.log(MessageKeys.OPERATOR_ERROR_LOADING_FILE_GROUP, pathToGroup, "not a directory");
      throw new IOException("not a directory");
    }
  
    private Path getPathFromClasspath   
 }

 

}