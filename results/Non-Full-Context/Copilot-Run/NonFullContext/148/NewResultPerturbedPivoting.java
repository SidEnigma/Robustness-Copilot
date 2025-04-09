/* SPDX-License-Identifier: Apache 2.0 */
 /* Copyright Contributors to the ODPi Egeria project. */
 package org.odpi.openmetadata.accessservices.dataengine.server.handlers;
 
 import org.apache.commons.collections4.CollectionUtils;
 import org.codehaus.plexus.util.StringUtils;
 import org.odpi.openmetadata.accessservices.dataengine.ffdc.DataEngineErrorCode;
 import org.odpi.openmetadata.accessservices.dataengine.model.Database;
 import org.odpi.openmetadata.accessservices.dataengine.model.DatabaseSchema;
 import org.odpi.openmetadata.accessservices.dataengine.model.DeleteSemantic;
 import org.odpi.openmetadata.accessservices.dataengine.model.RelationalColumn;
 import org.odpi.openmetadata.accessservices.dataengine.model.RelationalTable;
 import org.odpi.openmetadata.accessservices.dataengine.model.SchemaType;
 import org.odpi.openmetadata.commonservices.ffdc.InvalidParameterHandler;
 import org.odpi.openmetadata.commonservices.generichandlers.AssetHandler;
 import org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper;
 import org.odpi.openmetadata.commonservices.generichandlers.RelationalDataHandler;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.InvalidParameterException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.PropertyServerException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.UserNotAuthorizedException;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail;
 import org.odpi.openmetadata.repositoryservices.ffdc.exception.FunctionNotSupportedException;
 
 import java.util.List;
 import java.util.Optional;
 
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.DATABASE_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.DEPLOYED_DATABASE_SCHEMA_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.DISPLAY_NAME_PROPERTY_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.GUID_PROPERTY_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.INCOMPLETE_CLASSIFICATION_TYPE_GUID;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.INCOMPLETE_CLASSIFICATION_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.QUALIFIED_NAME_PROPERTY_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.RELATIONAL_COLUMN_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.RELATIONAL_TABLE_TYPE_GUID;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.RELATIONAL_TABLE_TYPE_NAME;
 
 /**
  * DataEngineRelationalDataHandler manages Databases and RelationalTables objects from the property server.  It runs server-side in the DataEngine
  * OMAS and creates entities and relationships through the OMRSRepositoryConnector.
  */
 public class DataEngineRelationalDataHandler {
 
     private static final String DATABASE_SCHEMA_GUID = "databaseSchemaGUID";
     private static final String DATABASE_GUID = "databaseGUID";
     private final String serviceName;
     private final String serverName;
     private final InvalidParameterHandler invalidParameterHandler;
     private final RelationalDataHandler<Database, DatabaseSchema, RelationalTable, RelationalTable, RelationalColumn, SchemaType>
             relationalDataHandler;
     private final AssetHandler<DatabaseSchema> databaseSchemaAssetHandler;
     private final DataEngineCommonHandler dataEngineCommonHandler;
     private final DataEngineRegistrationHandler registrationHandler;
     private final DataEngineConnectionAndEndpointHandler dataEngineConnectionAndEndpointHandler;
 
     /**
      * Construct the handler information needed to interact with the repository services
      *
      * @param serviceName                            name of this service
      * @param serverName                             name of the local server
      * @param invalidParameterHandler                handler for managing parameter errors
      * @param relationalDataHandler                  provides utilities for manipulating the repository services assets
      * @param dataEngineCommonHandler                provides utilities for manipulating entities
      * @param registrationHandler                    creates software server capability entities
      * @param dataEngineConnectionAndEndpointHandler provides utilities specific for manipulating Connections and Endpoints
      **/
     public DataEngineRelationalDataHandler(String serviceName, String serverName, InvalidParameterHandler invalidParameterHandler,
                                            RelationalDataHandler<Database, DatabaseSchema, RelationalTable, RelationalTable, RelationalColumn,
                                                    SchemaType> relationalDataHandler, AssetHandler<DatabaseSchema> databaseSchemaAssetHandler,
                                            DataEngineRegistrationHandler registrationHandler, DataEngineCommonHandler dataEngineCommonHandler,
                                            DataEngineConnectionAndEndpointHandler dataEngineConnectionAndEndpointHandler) {
 
         this.serviceName = serviceName;
         this.serverName = serverName;
         this.invalidParameterHandler = invalidParameterHandler;
         this.relationalDataHandler = relationalDataHandler;
         this.databaseSchemaAssetHandler = databaseSchemaAssetHandler;
         this.registrationHandler = registrationHandler;
         this.dataEngineCommonHandler = dataEngineCommonHandler;
         this.dataEngineConnectionAndEndpointHandler = dataEngineConnectionAndEndpointHandler;
     }
 
     /**
      * Create or update the database and the inside entities, if any (a database schema and relational tables)
      *
      * @param userId             the name of the calling user
      * @param database           the values of the database
      * @param externalSourceName the unique name of the external source
      * @param incomplete         determines if the entities inside the database are incomplete, if any (database schema
      *                           and relational tables)
      *
      * @return unique identifier of the database in the repository
      *
      * @throws InvalidParameterException  the bean properties are invalid
      * @throws UserNotAuthorizedException user not authorized to issue this request
      * @throws PropertyServerException    problem accessing the property server
      */
     public String upsertDatabase(String userId, Database database, boolean incomplete, String externalSourceName) throws InvalidParameterException,
                                                                                                                          UserNotAuthorizedException,
                                                                                                                          PropertyServerException {
         final String methodName = "upsertDatabase";
         validateParameters(userId, methodName, database.getQualifiedName(), database.getDisplayName());
 
         String externalSourceGUID = registrationHandler.getExternalDataEngine(userId, externalSourceName);
         Optional<EntityDetail> originalDatabaseEntity = findDatabaseEntity(userId, database.getQualifiedName());
 
         int ownerTypeOrdinal = dataEngineCommonHandler.getOwnerTypeOrdinal(database.getOwnerType());
         String databaseGUID;
         if (originalDatabaseEntity.isEmpty()) {
             databaseGUID = relationalDataHandler.createDatabase(userId, externalSourceGUID, externalSourceName, database.getQualifiedName(),
                     database.getDisplayName(), database.getDescription(), database.getOwner(), ownerTypeOrdinal, database.getZoneMembership(),
                     database.getOriginOrganizationGUID(), database.getOriginBusinessCapabilityGUID(), database.getOtherOriginValues(),
                     database.getPathName(), database.getCreateTime(), database.getModifiedTime(), database.getEncodingType(),
                     database.getEncodingLanguage(), database.getEncodingDescription(), database.getEncodingProperties(), database.getDatabaseType(),
                     database.getDatabaseVersion(), database.getDatabaseInstance(), database.getDatabaseImportedFrom(),
                     database.getAdditionalProperties(), DATABASE_TYPE_NAME, null,
                     null, methodName);
         } else {
             databaseGUID = originalDatabaseEntity.get().getGUID();
             relationalDataHandler.updateDatabase(userId, externalSourceGUID, externalSourceName, databaseGUID, database.getQualifiedName(),
                     database.getDisplayName(), database.getDescription(), database.getOwner(), ownerTypeOrdinal, database.getZoneMembership(),
                     database.getOriginOrganizationGUID(), database.getOriginBusinessCapabilityGUID(), database.getOtherOriginValues(),
                     database.getCreateTime(), database.getModifiedTime(), database.getEncodingType(), database.getEncodingLanguage(),
                     database.getEncodingDescription(), database.getEncodingProperties(), database.getDatabaseType(), database.getDatabaseVersion(),
                     database.getDatabaseInstance(), database.getDatabaseImportedFrom(), database.getAdditionalProperties(),
                     DATABASE_TYPE_NAME, null, null, methodName);
         }
 
         dataEngineConnectionAndEndpointHandler.upsertConnectionAndEndpoint(database.getQualifiedName(),
             databaseGUID, DATABASE_TYPE_NAME, database.getProtocol(), database.getNetworkAddress(),
             externalSourceGUID, externalSourceName, userId);
 
         DatabaseSchema databaseSchema = database.getDatabaseSchema();
         if(databaseSchema != null) {
             upsertDatabaseSchema(userId, databaseGUID, databaseSchema, incomplete, externalSourceName);
             List<RelationalTable> tables = database.getTables();
             if(CollectionUtils.isNotEmpty(tables)) {
                 for (RelationalTable table: tables) {
                     String databaseSchemaQualifiedName = databaseSchema.getQualifiedName();
                     upsertRelationalTable(userId, databaseSchemaQualifiedName, table, externalSourceName, incomplete);
                 }
             }
         }
 
         return databaseGUID;
     }
 
     /**
      * Find out if the Database object is already stored in the repository. It uses the fully qualified name to retrieve the entity
      *
      * @param userId        the name of the calling user
      * @param qualifiedName the qualifiedName name of the database to be searched
      *
      * @return optional with entity details if found, empty optional if not found
      *
      * @throws InvalidParameterException  the bean properties are invalid
      * @throws UserNotAuthorizedException user not authorized to issue this request
      * @throws PropertyServerException    problem accessing the property server
      */
     private Optional<EntityDetail> findDatabaseEntity(String userId, String qualifiedName) throws InvalidParameterException,
                                                                                                   PropertyServerException,
                                                                                                   UserNotAuthorizedException {
         return dataEngineCommonHandler.findEntity(userId, qualifiedName, DATABASE_TYPE_NAME);
     }
 
 
/** Verify that the DatabaseSchema object is already stored in the repository. */

private Optional<EntityDetail> findDatabaseSchemaEntity(String userId, String qualifiedName) throws InvalidParameterException,
        PropertyServerException, UserNotAuthorizedException {
    // Implementation logic goes here
}
 

}