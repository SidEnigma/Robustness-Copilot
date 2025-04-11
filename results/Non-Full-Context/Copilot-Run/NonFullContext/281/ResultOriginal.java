/* SPDX-License-Identifier: Apache 2.0 */
 /* Copyright Contributors to the ODPi Egeria project. */
 package org.odpi.openmetadata.accessservices.dataengine.server.handlers;
 
 import org.apache.commons.lang3.StringUtils;
 import org.odpi.openmetadata.accessservices.dataengine.model.DeleteSemantic;
 import org.odpi.openmetadata.accessservices.dataengine.model.FileFolder;
 import org.odpi.openmetadata.accessservices.dataengine.model.OwnerType;
 import org.odpi.openmetadata.commonservices.ffdc.InvalidParameterHandler;
 import org.odpi.openmetadata.commonservices.generichandlers.AssetHandler;
 import org.odpi.openmetadata.commonservices.repositoryhandler.RepositoryHandler;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.InvalidParameterException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.PropertyServerException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.UserNotAuthorizedException;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.Relationship;
 import org.odpi.openmetadata.repositoryservices.ffdc.exception.FunctionNotSupportedException;
 
 import java.io.File;
 import java.nio.file.Path;
 import java.nio.file.Paths;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Optional;
 
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.DATA_FILE_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.FILE_FOLDER_TYPE_GUID;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.FILE_FOLDER_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.FOLDER_HIERARCHY_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.GUID_PROPERTY_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.NESTED_FILE_TYPE_GUID;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.NESTED_FILE_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.SERVER_ASSET_USE_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.SOFTWARE_SERVER_CAPABILITY_TYPE_NAME;
 
 /**
  * FolderHierarchyHandler manages FileFolder objects from the property server. It runs server-side in the DataEngine OMAS
  * and creates FileFolder entities with wire relationships through the OMRSRepositoryConnector.
  */
 public class DataEngineFolderHierarchyHandler {
 
     private final InvalidParameterHandler invalidParameterHandler;
     private final RepositoryHandler repositoryHandler;
     private final DataEngineCommonHandler dataEngineCommonHandler;
     private final AssetHandler<FileFolder> folderHandler;
 
     /**
      * Construct the handler information needed to interact with the repository services
      *
      * @param invalidParameterHandler handler for managing parameter errors
      * @param repositoryHandler       manages calls to the repository services
      * @param dataEngineCommonHandler provides common Data Engine Omas utilities
      * @param folderHandler           provides utilities specific for manipulating FileFolders
      */
     public DataEngineFolderHierarchyHandler(InvalidParameterHandler invalidParameterHandler,
                                             RepositoryHandler repositoryHandler, DataEngineCommonHandler dataEngineCommonHandler,
                                             AssetHandler<FileFolder> folderHandler) {
 
         this.invalidParameterHandler = invalidParameterHandler;
         this.repositoryHandler = repositoryHandler;
         this.dataEngineCommonHandler = dataEngineCommonHandler;
         this.folderHandler = folderHandler;
     }
 
 
/** Construct the folder structure in which a data file is stored all the way to the SoftwareServerCapability. */
 public void upsertFolderHierarchy(String fileGuid, String pathName, String externalSourceGuid, String externalSourceName, String userId, String methodName) throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException{
     
      if (StringUtils.isEmpty(fileGuid)) {
            invalidParameterHandler.throwInvalidParameterException(methodName,
                                                                  null,
                                                                  "fileGuid",
                                                                  "The file guid is null or empty");
      }
     
      if (StringUtils.isEmpty(pathName)) {
            invalidParameterHandler.throwInvalidParameterException(methodName,
                                                                  null,
                                                                  "pathName",
                                                                  "The path name is null or empty");
      }
     
      if (StringUtils.isEmpty(externalSourceGuid)) {
            invalidParameterHandler.throwInvalidParameterException(methodName,
                                                                  null,
                                                                  "externalSourceGuid",
                                                                  "The external source guid is null or empty");
      }
     
      if (StringUtils.isEmpty(externalSourceName)) {
            invalidParameterHandler.throwInvalidParameterException(methodName,
                                                                  null,
                                                                  "externalSourceName",
                                                                  "The external source name is null or empty");
      }
     
      if (StringUtils.isEmpty(userId)) {
            invalidParameterHandler.throwInvalidParameterException(methodName,
                                                                  null,
                                                                  "userId",
                                                                  "The userId is null or empty");
      }
     
      if (StringUtils.isEmpty(methodName)) {
            invalidParameterHandler.throwInvalidParameterException(methodName,
                                                                  null,
                                                                  "methodName",
                                                                  "The method name is null or empty");
      }
     
      Path path = Paths.get(pathName);
     
      if (path.isAbsolute()) {
            invalidParameterHandler.throwInvalidParameterException(methodName,
                                                                  null,
                                                                  "pathName",
                                                                  "The path name is absolute");
      }
     
      String folderName = path.getFileName().toString();
     
      if (StringUtils.isEmpty(folderName))          
 }

 

}