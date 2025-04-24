/* SPDX-License-Identifier: Apache-2.0 */
 /* Copyright Contributors to the ODPi Egeria project. */
 package org.odpi.openmetadata.accessservices.assetlineage.handlers;
 
 import org.apache.commons.collections4.CollectionUtils;
 import org.odpi.openmetadata.accessservices.assetlineage.event.AssetLineageEventType;
 import org.odpi.openmetadata.accessservices.assetlineage.model.GraphContext;
 import org.odpi.openmetadata.accessservices.assetlineage.model.LineageEntity;
 import org.odpi.openmetadata.accessservices.assetlineage.model.RelationshipsContext;
 import org.odpi.openmetadata.commonservices.repositoryhandler.RepositoryHandler;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.InvalidParameterException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.OCFCheckedExceptionBase;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.PropertyServerException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.UserNotAuthorizedException;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.Classification;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstancePropertyValue;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.Relationship;
 
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Optional;
 import java.util.Set;
 
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.ANCHOR_GUID;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.ASSET_SCHEMA_TYPE;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.ATTRIBUTE_FOR_SCHEMA;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.CONNECTION_ENDPOINT;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.CONNECTION_TO_ASSET;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.DATA_CONTENT_FOR_DATA_SET;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.EVENT_SCHEMA_ATTRIBUTE;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.FOLDER_HIERARCHY;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.NESTED_FILE;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.NESTED_SCHEMA_ATTRIBUTE;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.PORT_IMPLEMENTATION;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.RELATIONAL_COLUMN;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.SCHEMA_ATTRIBUTE;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.SCHEMA_TYPE_OPTION;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.TABULAR_COLUMN;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.TABULAR_FILE_COLUMN;
 
 /**
  * The Asset Context Handler provides methods to build graph context for schema elements.
  */
 public class AssetContextHandler {
 
     private final RepositoryHandler repositoryHandler;
     private final HandlerHelper handlerHelper;
     private final List<String> supportedZones;
 
 
     /**
      * Construct the handler information needed to interact with the repository services
      *
      * @param repositoryHandler handler for calling the repository services
      * @param handlerHelper     helper handler
      * @param supportedZones    configurable list of zones that Asset Lineage is allowed to retrieve Assets from
      */
     public AssetContextHandler(RepositoryHandler repositoryHandler, HandlerHelper handlerHelper, List<String> supportedZones) {
         this.repositoryHandler = repositoryHandler;
         this.handlerHelper = handlerHelper;
         this.supportedZones = supportedZones;
     }
 
 
/** The asset context is built for a schema element. */

public Map<String, RelationshipsContext> buildSchemaElementContext(String userId, EntityDetail entityDetail) throws OCFCheckedExceptionBase {
    Map<String, RelationshipsContext> contextMap = new HashMap<>();

    // Check if the entity detail is null
    if (entityDetail == null) {
        throw new InvalidParameterException(AssetLineageErrorCode.NULL_ENTITY.getMessageDefinition(), this.getClass().getName(), "entityDetail");
    }

    // Get the entity guid
    String entityGuid = entityDetail.getGUID();

    // Get the entity type name
    String entityTypeName = entityDetail.getType().getTypeDefName();

    // Check if the entity type name is supported
    if (!handlerHelper.isSupportedEntityType(entityTypeName)) {
        throw new InvalidParameterException(AssetLineageErrorCode.UNSUPPORTED_ENTITY_TYPE.getMessageDefinition(entityTypeName), this.getClass().getName(), "entityDetail");
    }

    // Check if the entity is in a supported zone
    if (!handlerHelper.isEntityInSupportedZone(entityDetail, supportedZones)) {
        throw new UserNotAuthorizedException(AssetLineageErrorCode.UNAUTHORIZED_ENTITY.getMessageDefinition(entityGuid), this.getClass().getName(), "entityDetail");
    }

    // Create the relationships context
    RelationshipsContext relationshipsContext = new RelationshipsContext();

    // Set the entity detail in the relationships context
    relationshipsContext.setEntity(entityDetail);

    // Get the relationships for the entity
    List<Relationship> relationships = repositoryHandler.getRelationshipsByEntityGUID(userId, entityGuid, entityTypeName, null, null, null, null, null, 0);

    // Check if there are any relationships
    if (CollectionUtils.isNotEmpty(relationships)) {
        // Process each relationship
        for (Relationship relationship : relationships) {
            // Get the relationship type name
            String relationshipTypeName = relationship.getType().getTypeDefName();

            // Check if the relationship type name is supported
            if (!handlerHelper.isSupportedRelationshipType(relationshipTypeName)) {
                continue;
            }

            // Get the relationship end 1
            EntityDetail end1Entity = repositoryHandler.getEntityByGUID(userId, relationship.getEntityOneProxy().getGUID(), "relationship end 1");

            // Get the relationship end 2
            EntityDetail end2Entity = repositoryHandler.getEntityByGUID(userId, relationship.getEntityTwoProxy().getGUID(), "relationship end 2");

            // Check if the relationship end 1 is null or not in a supported zone
            if (end1Entity == null || !handlerHelper.isEntityInSupportedZone(end1Entity, supportedZones)) {
                continue;
            }

            // Check if the relationship end 2 is null or not in a supported zone
            if (end2Entity == null || !handlerHelper.isEntityInSupportedZone(end2Entity, supportedZones)) {
                continue;
            }

            // Create the lineage entity for the relationship
            LineageEntity lineageEntity = new LineageEntity();
            lineageEntity.setEntity(end2Entity);
            lineageEntity.setRelationship(relationship);

            // Add the lineage entity to the relationships context
            relationshipsContext.addLineageEntity(lineageEntity);
        }
    }

    // Add the relationships context to the context map
    contextMap.put(entityGuid, relationshipsContext);

    return contextMap;
}
 

}