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
 
 
/** Create the folder structure where the data file is stored until SoftwareServerCapability. Uniqueness of the relationship NestedFile is maintained. */

public void upsertFolderHierarchy(String fileGuid, String pathName, String externalSourceGuid, String externalSourceName, String userId, String methodName)
        throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
    final String guidParameterName = "fileGuid";
    final String pathParameterName = "pathName";

    invalidParameterHandler.validateUserId(userId, methodName);
    invalidParameterHandler.validateGUID(fileGuid, guidParameterName, methodName);
    invalidParameterHandler.validateName(pathName, pathParameterName, methodName);

    String[] folderNames = StringUtils.split(pathName, File.separator);

    if (folderNames != null && folderNames.length > 0) {
        List<String> existingFolderGuids = new ArrayList<>();
        String parentFolderGuid = null;

        for (String folderName : folderNames) {
            Optional<EntityDetail> existingFolder = folderHandler.findEntity(userId, folderName, FILE_FOLDER_TYPE_NAME, methodName);

            if (existingFolder.isPresent()) {
                existingFolderGuids.add(existingFolder.get().getGUID());
                parentFolderGuid = existingFolder.get().getGUID();
            } else {
                FileFolder newFolder = dataEngineCommonHandler.createFileFolder(userId, folderName, externalSourceGuid, externalSourceName, methodName);
                String newFolderGuid = newFolder.getGUID();

                if (parentFolderGuid != null) {
                    Relationship nestedFileRelationship = dataEngineCommonHandler.createNestedFileRelationship(userId, parentFolderGuid, newFolderGuid, methodName);
                    repositoryHandler.createRelationship(userId, FILE_FOLDER_TYPE_NAME, parentFolderGuid, newFolderGuid, nestedFileRelationship, methodName);
                }

                existingFolderGuids.add(newFolderGuid);
                parentFolderGuid = newFolderGuid;
            }
        }

        dataEngineCommonHandler.createFolderHierarchyRelationships(userId, fileGuid, existingFolderGuids, methodName);
    }
}
 

}