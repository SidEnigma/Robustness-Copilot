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
 
 
/** Given a file path, loads the contents of the files into a map. */
 static Map<String, String> loadContents(Path rootDir) throws IOException{}

 

}