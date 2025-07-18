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
 
     /**
      * Builds the context for a schema element without the asset context.
      *
      * @param userId       the unique identifier for the user
      * @param entityDetail the entity for which the context is build
      *
      * @return the context of the schema element
      *
      * @throws OCFCheckedExceptionBase checked exception for reporting errors found when using OCF connectors
      */
     public Map<String, RelationshipsContext> buildSchemaElementContext(String userId, EntityDetail entityDetail) throws OCFCheckedExceptionBase {
         final String methodName = "buildSchemaElementContext";
         handlerHelper.validateAsset(entityDetail, methodName, supportedZones);
 
         Map<String, RelationshipsContext> context = new HashMap<>();
         final String typeDefName = entityDetail.getType().getTypeDefName();
         Set<GraphContext> columnContext = new HashSet<>();
         switch (typeDefName) {
             case TABULAR_COLUMN:
                 if (!isInternalTabularColumn(userId, entityDetail)) {
                     columnContext = buildTabularColumnContext(userId, entityDetail);
                 }
                 break;
             case TABULAR_FILE_COLUMN:
                 columnContext = buildTabularColumnContext(userId, entityDetail);
                 break;
             case RELATIONAL_COLUMN:
                 columnContext = buildRelationalColumnContext(userId, entityDetail);
                 break;
             case EVENT_SCHEMA_ATTRIBUTE:
                 columnContext = buildEventSchemaAttributeContext(userId, entityDetail);
                 break;
             default:
                return context;
         }
 
         context.put(AssetLineageEventType.COLUMN_CONTEXT_EVENT.getEventTypeName(), new RelationshipsContext(entityDetail.getGUID(), columnContext));
         return context;
     }
 
     /**
      * Builds the asset context for a schema element.
      *
      * @param userId        the unique identifier for the user
      * @param lineageEntity the entity for which the context is build
      *
      * @return the asset context of the schema element
      *
      * @throws OCFCheckedExceptionBase checked exception for reporting errors found when using OCF connectors
      */
     public Map<String, RelationshipsContext> buildAssetContext(String userId, LineageEntity lineageEntity)
             throws OCFCheckedExceptionBase {
 
         Map<String, RelationshipsContext> context = new HashMap<>();
 
         EntityDetail asset = handlerHelper.getEntityDetails(userId, lineageEntity.getGuid(), lineageEntity.getTypeDefName());
         context.put(AssetLineageEventType.ASSET_CONTEXT_EVENT.getEventTypeName(), buildAssetContext(userId, asset));
         return context;
     }
 
     /**
      * Builds the asset context for a schema element.
      *
      * @param userId       the unique identifier for the user
      * @param entityDetail the entity for which the context is build
      *
      * @return the asset context of the schema element
      *
      * @throws OCFCheckedExceptionBase checked exception for reporting errors found when using OCF connectors
      */
     public RelationshipsContext buildAssetContext(String userId, EntityDetail entityDetail) throws OCFCheckedExceptionBase {
         final String methodName = "buildAssetContext";
         handlerHelper.validateAsset(entityDetail, methodName, supportedZones);
         RelationshipsContext context = new RelationshipsContext();
 
         if (handlerHelper.isDataStore(userId, entityDetail)) {
             context = buildDataFileContext(userId, entityDetail);
         }
 
         if (handlerHelper.isTable(userId, entityDetail)) {
             context = buildRelationalTableContext(userId, entityDetail);
         }
 
         if (handlerHelper.isTopic(userId, entityDetail)) {
             context = buildTopicContext(userId, entityDetail);
         }
 
         return context;
     }
 
     /**
      * Builds the column context for a schema element
      *
      * @param lineageEntity column as lineage entity
      *
      * @return column context of the schema element
      *
      * @throws OCFCheckedExceptionBase checked exception for reporting errors found when using OCF connectors
      */
     public Map<String, RelationshipsContext> buildColumnContext(String userId, LineageEntity lineageEntity)
             throws OCFCheckedExceptionBase {
         if (!handlerHelper.isSchemaAttribute(userId, lineageEntity.getTypeDefName())) {
             return new HashMap<>();
         }
         EntityDetail entityDetail = handlerHelper.getEntityDetails(userId, lineageEntity.getGuid(), SCHEMA_ATTRIBUTE);
 
         return buildSchemaElementContext(userId, entityDetail);
     }
 
     /**
      * Returns the asset entity context in lineage format
      *
      * @param userId      the unique identifier for the user
      * @param guid        the guid of the entity for which the context is build
      * @param typeDefName the type def name of the entity for which the context is build
      *
      * @return the asset entity context in lineage format
      *
      * @throws OCFCheckedExceptionBase checked exception for reporting errors found when using OCF connectors
      */
     public Optional<LineageEntity> buildAssetEntityContext(String userId, String guid, String typeDefName) throws OCFCheckedExceptionBase {
         EntityDetail entityDetail = handlerHelper.getEntityDetails(userId, guid, typeDefName);
         if (!handlerHelper.isTableOrDataStore(userId, entityDetail)) {
             return Optional.empty();
         }
 
         return Optional.of(handlerHelper.getLineageEntity(entityDetail));
     }
 
     /**
      * Validates that an entity is internal to DataEngine OMAS
      *
      * @param userId        the unique identifier for the user
      * @param tabularColumn the column to validate
      *
      * @return true if it's internal, false otherwise
      *
      * @throws InvalidParameterException  one of the parameters is null or invalid
      * @throws PropertyServerException    problem accessing property server
      * @throws UserNotAuthorizedException security access problem
      */
     private boolean isInternalTabularColumn(String userId, EntityDetail tabularColumn) throws OCFCheckedExceptionBase {
         String methodName = "isInternalTabularColumn";
 
         Optional<Relationship> relationship = handlerHelper.getUniqueRelationshipByType(userId, tabularColumn.getGUID(), ATTRIBUTE_FOR_SCHEMA,
                 tabularColumn.getType().getTypeDefName());
         if (relationship.isEmpty()) {
             return false;
         }
 
         EntityDetail schemaType = handlerHelper.getEntityAtTheEnd(userId, tabularColumn.getGUID(), relationship.get());
         Optional<Classification> anchorGUIDClassification = getAnchorsClassification(schemaType);
         if (anchorGUIDClassification.isEmpty()) {
             return false;
         }
         Optional<String> anchorGUID = getAnchorGUID(anchorGUIDClassification.get());
         if (anchorGUID.isEmpty()) {
             return false;
         }
 
         return repositoryHandler.isEntityATypeOf(userId, anchorGUID.get(), ANCHOR_GUID, PORT_IMPLEMENTATION, methodName);
     }
 
     /**
      * Retrieves the anchorGUID property form a classification
      *
      * @param classification the classification
      *
      * @return the anchorGUID property or an empty optional
      */
     private Optional<String> getAnchorGUID(Classification classification) {
         InstancePropertyValue anchorGUIDProperty = classification.getProperties().getPropertyValue(ANCHOR_GUID);
         if (anchorGUIDProperty == null) {
             return Optional.empty();
         }
         return Optional.of(anchorGUIDProperty.valueAsString());
     }
 
     /**
      * Retrieves the Anchors classification from an entity
      *
      * @param entityDetail the entity to check for the classification
      *
      * @return the Anchors classification or an empty Optional if missing
      */
     private Optional<Classification> getAnchorsClassification(EntityDetail entityDetail) {
         List<Classification> classifications = entityDetail.getClassifications();
         if (CollectionUtils.isEmpty(classifications)) {
             return Optional.empty();
         }
         for (Classification classification : classifications) {
             if ("Anchors".equalsIgnoreCase(classification.getName()))
                 return Optional.of(classification);
         }
         return Optional.empty();
     }
 
 
/** The context for a column in the table is built. */
 private RelationshipsContext buildRelationalTableContext(String userId, EntityDetail entityDetail) throws OCFCheckedExceptionBase{}

 

}