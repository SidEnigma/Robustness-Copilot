/* SPDX-License-Identifier: Apache 2.0 */
 /* Copyright Contributors to the ODPi Egeria project. */
 package org.odpi.openmetadata.accessservices.dataengine.server.handlers;
 
 import org.apache.commons.collections4.CollectionUtils;
 import org.odpi.openmetadata.accessservices.dataengine.ffdc.DataEngineErrorCode;
 import org.odpi.openmetadata.accessservices.dataengine.model.DeleteSemantic;
 import org.odpi.openmetadata.accessservices.dataengine.model.SchemaType;
 import org.odpi.openmetadata.commonservices.ffdc.InvalidParameterHandler;
 import org.odpi.openmetadata.commonservices.generichandlers.SchemaTypeBuilder;
 import org.odpi.openmetadata.commonservices.generichandlers.SchemaTypeHandler;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.InvalidParameterException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.PropertyServerException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.UserNotAuthorizedException;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetailDifferences;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceHeader;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;
 import org.odpi.openmetadata.repositoryservices.ffdc.exception.FunctionNotSupportedException;
 
 import java.util.HashSet;
 import java.util.Optional;
 import java.util.Set;
 import java.util.stream.Collectors;
 
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.ASSET_TO_SCHEMA_TYPE_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.DISPLAY_NAME_PROPERTY_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.GUID_PROPERTY_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.LINEAGE_MAPPING_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.QUALIFIED_NAME_PROPERTY_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.REFERENCEABLE_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.SCHEMA_TYPE_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.TABULAR_COLUMN_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.TABULAR_SCHEMA_TYPE_TYPE_GUID;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.TABULAR_SCHEMA_TYPE_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.TYPE_TO_ATTRIBUTE_RELATIONSHIP_TYPE_NAME;
 
 /**
  * DataEngineSchemaTypeHandler manages schema types objects from the property server. It runs server-side in the
  * DataEngine OMAS and creates and retrieves schema type entities through the OMRSRepositoryConnector.
  */
 public class DataEngineSchemaTypeHandler {
     public static final String SCHEMA_TYPE_GUID_PARAMETER_NAME = "schemaTypeGUID";
     private final String serviceName;
     private final String serverName;
     private final OMRSRepositoryHelper repositoryHelper;
     private final InvalidParameterHandler invalidParameterHandler;
     private final SchemaTypeHandler<SchemaType> schemaTypeHandler;
     private final DataEngineRegistrationHandler dataEngineRegistrationHandler;
     private final DataEngineCommonHandler dataEngineCommonHandler;
     private final DataEngineSchemaAttributeHandler dataEngineSchemaAttributeHandler;
 
     /**
      * Construct the handler information needed to interact with the repository services
      *
      * @param serviceName                   name of this service
      * @param serverName                    name of the local server
      * @param invalidParameterHandler       handler for managing parameter errors
      * @param repositoryHelper              provides utilities for manipulating the repository services objects
      * @param schemaTypeHandler             handler for managing schema elements in the metadata repositories
      * @param dataEngineRegistrationHandler provides calls for retrieving external data engine guid
      * @param dataEngineCommonHandler       provides utilities for manipulating entities
      */
     public DataEngineSchemaTypeHandler(String serviceName, String serverName, InvalidParameterHandler invalidParameterHandler,
                                        OMRSRepositoryHelper repositoryHelper, SchemaTypeHandler<SchemaType> schemaTypeHandler,
                                        DataEngineRegistrationHandler dataEngineRegistrationHandler,
                                        DataEngineCommonHandler dataEngineCommonHandler,
                                        DataEngineSchemaAttributeHandler dataEngineSchemaAttributeHandler) {
         this.serviceName = serviceName;
         this.serverName = serverName;
         this.invalidParameterHandler = invalidParameterHandler;
         this.repositoryHelper = repositoryHelper;
         this.schemaTypeHandler = schemaTypeHandler;
         this.dataEngineRegistrationHandler = dataEngineRegistrationHandler;
         this.dataEngineCommonHandler = dataEngineCommonHandler;
         this.dataEngineSchemaAttributeHandler = dataEngineSchemaAttributeHandler;
     }
 
 
/** If the existing entity doesn't exist or is not updated, create a new entity with the corresponding attributes and relationships. */
 public String upsertSchemaType(String userId, SchemaType schemaType, String externalSourceName) throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException{
        final String methodName = "upsertSchemaType";
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateObject(schemaType, methodName);
    
        String schemaTypeGUID = null;
        EntityDetail schemaTypeEntity = null;
        EntityDetailDifferences schemaTypeDifferences = null;
    
        try {
            schemaTypeGUID = schemaType.getGUID();
            if (schemaTypeGUID == null) {
                schemaTypeGUID = repositoryHelper.getGUIDFromName(userId,
                                                                SchemaTypeBuilder.getSchemaTypeTypeName(),
                                                                schemaType.getName(),
                                                                externalSourceName,
                                                                methodName);
            }
    
            if (schemaTypeGUID == null) {
                schemaTypeEntity = createSchemaTypeEntity(userId, schemaType, externalSourceName);
                schemaTypeGUID = schemaTypeEntity.getGUID();
            } else {
                schemaTypeEntity = repositoryHelper.getEntityForUpdate(userId,
                                                                        schemaTypeGUID,
                                                                        SchemaTypeBuilder.getSchemaTypeTypeName(),
                                                                        methodName);
                schemaTypeDifferences = getSchemaTypeDifferences(userId, schemaType, schemaTypeEntity, externalSourceName);
                if (schemaTypeDifferences != null) {
                    schemaTypeEntity = repositoryHelper.updateEntity(userId,
                                                                    schemaTypeEntity,
                                                                    schemaTypeDifferences,
                                                                    methodName);
                }
            }
        } catch (FunctionNotSupportedException e) {
            throw new PropertyServerException(DataEngineErrorCode.FUNCTION_NOT_SUPPORTED,
                                            this.serviceName,
                                            methodName,
                                            e.getMessage());
        }
    
        return schemaTypeGUID;      
 }

 

}