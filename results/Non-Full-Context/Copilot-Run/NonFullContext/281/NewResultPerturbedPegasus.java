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
 
 
/** The folder structure should have a data file in it. */

public void upsertFolderHierarchy(String fileGuid, String pathName, String externalSourceGuid, String externalSourceName, String userId, String methodName) throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
    // Validate input parameters
    invalidParameterHandler.validateUserId(userId, methodName);
    invalidParameterHandler.validateGUID(fileGuid, GUID_PROPERTY_NAME, methodName);

    // Get the file entity
    EntityDetail fileEntity = repositoryHandler.getEntityByGUID(userId, fileGuid, GUID_PROPERTY_NAME, DATA_FILE_TYPE_NAME, methodName);

    // Check if the file entity exists
    if (fileEntity == null) {
        throw new InvalidParameterException(DataEngineErrorCode.FILE_NOT_FOUND.getMessageDefinition(fileGuid),
                this.getClass().getName(), methodName, GUID_PROPERTY_NAME);
    }

    // Get the file folder entity
    EntityDetail fileFolderEntity = repositoryHandler.getEntityForRelationshipType(userId, fileGuid, DATA_FILE_TYPE_NAME,
            NESTED_FILE_TYPE_GUID, NESTED_FILE_TYPE_NAME, methodName);

    // Check if the file folder entity exists
    if (fileFolderEntity == null) {
        throw new InvalidParameterException(DataEngineErrorCode.FILE_FOLDER_NOT_FOUND.getMessageDefinition(fileGuid),
                this.getClass().getName(), methodName, GUID_PROPERTY_NAME);
    }

    // Get the folder hierarchy entity
    EntityDetail folderHierarchyEntity = repositoryHandler.getEntityForRelationshipType(userId, fileGuid, DATA_FILE_TYPE_NAME,
            FOLDER_HIERARCHY_TYPE_GUID, FOLDER_HIERARCHY_TYPE_NAME, methodName);

    // Check if the folder hierarchy entity exists
    if (folderHierarchyEntity == null) {
        throw new InvalidParameterException(DataEngineErrorCode.FOLDER_HIERARCHY_NOT_FOUND.getMessageDefinition(fileGuid),
                this.getClass().getName(), methodName, GUID_PROPERTY_NAME);
    }

    // Get the folder entity
    EntityDetail folderEntity = repositoryHandler.getEntityForRelationshipType(userId, fileGuid, DATA_FILE_TYPE_NAME,
            FILE_FOLDER_TYPE_GUID, FILE_FOLDER_TYPE_NAME, methodName);

    // Check if the folder entity exists
    if (folderEntity == null) {
        throw new InvalidParameterException(DataEngineErrorCode.FOLDER_NOT_FOUND.getMessageDefinition(fileGuid),
                this.getClass().getName(), methodName, GUID_PROPERTY_NAME);
    }

    // Get the software server capability entity
    EntityDetail softwareServerCapabilityEntity = repositoryHandler.getEntityForRelationshipType(userId, fileGuid, DATA_FILE_TYPE_NAME,
            SERVER_ASSET_USE_TYPE_GUID, SOFTWARE_SERVER_CAPABILITY_TYPE_NAME, methodName);

    // Check if the software server capability entity exists
    if (softwareServerCapabilityEntity == null) {
        throw new InvalidParameterException(DataEngineErrorCode.SOFTWARE_SERVER_CAPABILITY_NOT_FOUND.getMessageDefinition(fileGuid),
                this.getClass().getName(), methodName, GUID_PROPERTY_NAME);
    }

    // Get the owner type
    OwnerType ownerType = dataEngineCommonHandler.getOwnerType(userId, fileGuid, methodName);

    // Get the delete semantic
    DeleteSemantic deleteSemantic = dataEngineCommonHandler.getDeleteSemantic(userId, fileGuid, methodName);

    // Get the file folder object
    FileFolder fileFolder = folderHandler.getFileFolderBean(fileFolderEntity);

    // Set the path name
    fileFolder.setPathName(pathName);

    // Set the external source GUID and name
    fileFolder.setExternalSourceGUID(externalSourceGuid);
    fileFolder.setExternalSourceName(externalSourceName);

    // Set the owner type and delete semantic
    fileFolder.setOwnerType(ownerType);
    fileFolder.setDeleteSemantic(deleteSemantic);

    // Update the file folder entity
    folderHandler.updateBeanInRepository(userId, externalSourceGuid, externalSourceName, fileFolderEntity, fileFolder, methodName);

    // Update the folder hierarchy entity
    folderHandler.updateBeanInRepository(userId, externalSourceGuid, externalSourceName, folderHierarchyEntity, fileFolder, methodName);

    // Update the folder entity
    folderHandler.updateBeanInRepository(userId, externalSourceGuid, externalSourceName, folderEntity, fileFolder, methodName);

    // Update the software server capability entity
    folderHandler.updateBeanInRepository(userId, externalSourceGuid, externalSourceName, softwareServerCapabilityEntity, fileFolder, methodName);
}
 

}