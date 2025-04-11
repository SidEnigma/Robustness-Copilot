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

public void upsertFolderHierarchy(String fileGuid, String pathName, String externalSourceGuid, String externalSourceName, String userId, String methodName)
        throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
    final String guidParameterName = "fileGuid";
    final String pathParameterName = "pathName";

    invalidParameterHandler.validateUserId(userId, methodName);
    invalidParameterHandler.validateGUID(fileGuid, guidParameterName, methodName);
    invalidParameterHandler.validateName(pathName, pathParameterName, methodName);

    String[] folderNames = StringUtils.split(pathName, File.separator);

    if (folderNames != null && folderNames.length > 0) {
        List<String> folderGUIDs = new ArrayList<>();

        for (String folderName : folderNames) {
            if (StringUtils.isNotBlank(folderName)) {
                Optional<EntityDetail> optionalFolder = folderHandler.getEntityByUniquePropertyValue(FILE_FOLDER_TYPE_NAME, folderName, methodName);

                if (optionalFolder.isPresent()) {
                    EntityDetail folderEntity = optionalFolder.get();
                    folderGUIDs.add(folderEntity.getGUID());
                } else {
                    String folderGUID = dataEngineCommonHandler.createFolder(userId, folderName, externalSourceGuid, externalSourceName, methodName);
                    folderGUIDs.add(folderGUID);
                }
            }
        }

        if (!folderGUIDs.isEmpty()) {
            String folderHierarchyGUID = dataEngineCommonHandler.createFolderHierarchy(userId, folderGUIDs, methodName);
            dataEngineCommonHandler.linkElementToElement(userId, fileGuid, guidParameterName, DATA_FILE_TYPE_NAME,
                    folderHierarchyGUID, FOLDER_HIERARCHY_TYPE_NAME, methodName);
        }
    }
}
 

}