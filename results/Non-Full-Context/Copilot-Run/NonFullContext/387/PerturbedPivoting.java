/* SPDX-License-Identifier: Apache-2.0 */
 /* Copyright Contributors to the ODPi Egeria project. */
 package org.odpi.openmetadata.accessservices.assetlineage.listeners;
 
 import com.fasterxml.jackson.core.JsonProcessingException;
 import org.apache.commons.collections4.CollectionUtils;
 import org.odpi.openmetadata.accessservices.assetlineage.auditlog.AssetLineageAuditCode;
 import org.odpi.openmetadata.accessservices.assetlineage.event.AssetLineageEventType;
 import org.odpi.openmetadata.accessservices.assetlineage.outtopic.AssetLineagePublisher;
 import org.odpi.openmetadata.accessservices.assetlineage.util.Converter;
 import org.odpi.openmetadata.frameworks.auditlog.AuditLog;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.ConnectorCheckedException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.OCFCheckedExceptionBase;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.PropertyServerException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.UserNotAuthorizedException;
 import org.odpi.openmetadata.repositoryservices.connectors.omrstopic.OMRSTopicListener;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.Relationship;
 import org.odpi.openmetadata.repositoryservices.events.OMRSEventOriginator;
 import org.odpi.openmetadata.repositoryservices.events.OMRSInstanceEvent;
 import org.odpi.openmetadata.repositoryservices.events.OMRSInstanceEventType;
 import org.odpi.openmetadata.repositoryservices.events.OMRSRegistryEvent;
 import org.odpi.openmetadata.repositoryservices.events.OMRSTypeDefEvent;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.util.Collections;
 import java.util.List;
 import java.util.Set;
 import java.util.stream.Collectors;
 
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.LINEAGE_MAPPING;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.PROCESS;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.PROCESS_HIERARCHY;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.SEMANTIC_ASSIGNMENT;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.TERM_CATEGORIZATION;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.VALUE_FOR_ACTIVE;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.immutableValidLineageDeleteEntityEvents;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.immutableValidLineageEntityEvents;
 import static org.odpi.openmetadata.accessservices.assetlineage.util.AssetLineageConstants.immutableValidLineageRelationshipTypes;
 
 /**
  * AssetLineageOMRSTopicListener received details of each OMRS event from the cohorts that the local server
  * is connected to.  It passes Lineage Entity events to the publisher.
  */
 public class AssetLineageOMRSTopicListener implements OMRSTopicListener {
 
     private static final Logger log = LoggerFactory.getLogger(AssetLineageOMRSTopicListener.class);
     private static final String PROCESSING_RELATIONSHIP_DEBUG_MESSAGE =
             "Asset Lineage OMAS is processing a {} event concerning relationship {} of type {}";
     private static final String PROCESSING_ENTITY_DETAIL_DEBUG_MESSAGE =
             "Asset Lineage OMAS is processing a {} event concerning entity {} of type {}";
 
     private final AssetLineagePublisher
             publisher;
     private final AuditLog auditLog;
     private final Converter converter;
     private final Set<String> lineageClassificationTypes;
     private final String serverName;
 
     /**
      * The constructor is given the connection to the out topic for Asset Lineage OMAS
      * along with classes for testing and manipulating instances.
      *
      * @param converter  The converter used for creating entities in Open Lineage format
      * @param serverName name of this server instance
      * @param publisher  instance of the asset-lineage topic publisherT
      */
     public AssetLineageOMRSTopicListener(Converter converter,
                                          String serverName,
                                          AssetLineagePublisher publisher,
                                          Set<String> lineageClassificationTypes,
                                          AuditLog auditLog) {
         this.publisher = publisher;
         this.lineageClassificationTypes = lineageClassificationTypes;
         this.auditLog = auditLog;
         this.serverName = serverName;
         this.converter = converter;
     }
 
     /**
      * Returns the Asset Lineage Publisher
      *
      * @return Asset Lineage Publisher
      */
     public AssetLineagePublisher getPublisher() {
         return publisher;
     }
 
     /**
      * Method to pass a Registry event received on topic.
      *
      * @param event inbound event
      */
     @Override
     public void processRegistryEvent(OMRSRegistryEvent event) {
         log.trace("Ignoring registry event: {}", event);
     }
 
     /**
      * Method to pass a Registry event received on topic.
      *
      * @param event inbound event
      */
     @Override
     public void processTypeDefEvent(OMRSTypeDefEvent event) {
         log.trace("Ignoring type event: {}", event);
     }
 
     /**
      * Unpack and deliver an instance event to the InstanceEventProcessor
      *
      * @param instanceEvent event to unpack
      */
     @Override
     public void processInstanceEvent(OMRSInstanceEvent instanceEvent) {
         if (instanceEvent == null) {
             return;
         }
 
         OMRSEventOriginator instanceEventOriginator = instanceEvent.getEventOriginator();
         if (instanceEventOriginator == null) {
             return;
         }
 
         OMRSInstanceEventType instanceEventType = instanceEvent.getInstanceEventType();
         EntityDetail entityDetail = instanceEvent.getEntity();
         Relationship relationship = instanceEvent.getRelationship();
 
         try {
             switch (instanceEventType) {
                 case UPDATED_ENTITY_EVENT:
                     EntityDetail originalEntity = instanceEvent.getOriginalEntity();
                     processUpdatedEntity(entityDetail, originalEntity);
                     break;
                 case DELETED_ENTITY_EVENT:
                     processDeletedEntity(entityDetail);
                     break;
                 case CLASSIFIED_ENTITY_EVENT:
                     processClassifiedEntityEvent(entityDetail);
                     break;
                 case RECLASSIFIED_ENTITY_EVENT:
                     processReclassifiedEntityEvent(entityDetail);
                     break;
                 case DECLASSIFIED_ENTITY_EVENT:
                     processDeclassifiedEntityEvent(entityDetail);
                     break;
                 case NEW_RELATIONSHIP_EVENT:
                     processNewRelationshipEvent(relationship);
                     break;
                 case UPDATED_RELATIONSHIP_EVENT:
                     processUpdatedRelationshipEvent(relationship);
                     break;
                 case DELETED_RELATIONSHIP_EVENT:
                     processDeletedRelationshipEvent(relationship);
                     break;
                 default:
                     break;
             }
         } catch (OCFCheckedExceptionBase e) {
             log.error("The following exception occurred: \n" + e + "\n \nWhile processing OMRSTopic event: \n" + instanceEvent,
                     e);
             logExceptionToAudit(instanceEvent, e);
         } catch (Exception e) {
             log.error("An exception occurred while processing OMRSTopic event: \n " + instanceEvent, e);
             logExceptionToAudit(instanceEvent, e);
         }
     }
 
     /**
      * Determine whether an updated entity is an lineage entity and publish the update entity for lineage
      *
      * @param entityDetail   entity object that has just been updated.
      * @param originalEntity original entity
      *
      * @throws OCFCheckedExceptionBase checked exception for reporting errors found when using OCF connectors
      * @throws JsonProcessingException exception parsing the event json
      */
     private void processUpdatedEntity(EntityDetail entityDetail, EntityDetail originalEntity) throws OCFCheckedExceptionBase,
                                                                                                      JsonProcessingException {
         if (!immutableValidLineageEntityEvents.contains(entityDetail.getType().getTypeDefName())) {
             return;
         }
 
         log.debug(PROCESSING_ENTITY_DETAIL_DEBUG_MESSAGE, AssetLineageEventType.UPDATE_ENTITY_EVENT.getEventTypeName(),
                 entityDetail.getGUID(), entityDetail.getType().getTypeDefName());
 
         if (isProcessStatusChangedToActive(entityDetail, originalEntity)) {
             if (publisher.publishProcessContext(entityDetail).isEmpty()) {
                 publishEntityEvent(entityDetail, AssetLineageEventType.UPDATE_ENTITY_EVENT);
             }
 
             log.info("Asset Lineage OMAS published the context for process with guid {}", entityDetail.getGUID());
         } else {
             publishEntityEvent(entityDetail, AssetLineageEventType.UPDATE_ENTITY_EVENT);
         }
     }
 
     /**
      * Process delete event for lineage entities.
      *
      * @param entityDetail entity object that has been deleted
      *
      * @throws UserNotAuthorizedException the user is not authorized to make this request.
      * @throws PropertyServerException    the service name is not known - indicating a logic error
      * @throws ConnectorCheckedException  unable to send the event due to connectivity issue
      * @throws JsonProcessingException    exception parsing the event json
      */
     private void processDeletedEntity(EntityDetail entityDetail) throws OCFCheckedExceptionBase, JsonProcessingException {
         if (!immutableValidLineageDeleteEntityEvents.contains(entityDetail.getType().getTypeDefName())) {
             return;
         }
 
         log.debug(PROCESSING_ENTITY_DETAIL_DEBUG_MESSAGE, AssetLineageEventType.DELETE_ENTITY_EVENT.getEventTypeName(),
                 entityDetail.getGUID(), entityDetail.getType().getTypeDefName());
         publishEntityEvent(entityDetail, AssetLineageEventType.DELETE_ENTITY_EVENT);
     }
 
     /**
      * Process classified event for lineage entities.
      *
      * @param entityDetail the entity object that has been deleted
      *
      * @throws OCFCheckedExceptionBase unable to send the event due to connectivity issue
      * @throws JsonProcessingException exception parsing the event json
      */
     private void processClassifiedEntityEvent(EntityDetail entityDetail) throws OCFCheckedExceptionBase, JsonProcessingException {
         if (!immutableValidLineageEntityEvents.contains(entityDetail.getType().getTypeDefName()))
             return;
 
         if (!anyLineageClassificationsLeft(entityDetail))
             return;
 
         if (!publisher.isEntityEligibleForPublishing(entityDetail)) {
             return;
         }
 
         log.debug(PROCESSING_ENTITY_DETAIL_DEBUG_MESSAGE, AssetLineageEventType.CLASSIFICATION_CONTEXT_EVENT.getEventTypeName(),
                 entityDetail.getGUID(), entityDetail.getType().getTypeDefName());
         publisher.publishClassificationContext(entityDetail, AssetLineageEventType.CLASSIFICATION_CONTEXT_EVENT);
     }
 
     /**
      * Process Re-Classify event from lineage entity.
      * <p>
      * The event is processed only if it contains lineage classifications
      *
      * @param entityDetail the entity object that contains a classification that has been updated
      *
      * @throws OCFCheckedExceptionBase unable to send the event due to connectivity issue
      * @throws JsonProcessingException exception parsing the event json
      */
     private void processReclassifiedEntityEvent(EntityDetail entityDetail) throws OCFCheckedExceptionBase, JsonProcessingException {
         if (!immutableValidLineageEntityEvents.contains(entityDetail.getType().getTypeDefName()))
             return;
 
         if (!anyLineageClassificationsLeft(entityDetail))
             return;
 
         if (!publisher.isEntityEligibleForPublishing(entityDetail)) {
             return;
         }
 
         log.debug(PROCESSING_ENTITY_DETAIL_DEBUG_MESSAGE, AssetLineageEventType.RECLASSIFIED_ENTITY_EVENT.getEventTypeName(),
                 entityDetail.getGUID(), entityDetail.getType().getTypeDefName());
         publisher.publishClassificationContext(entityDetail, AssetLineageEventType.RECLASSIFIED_ENTITY_EVENT);
     }
 
 
/** Process declassified entity event for lineage entity The entity context is published if there is no lineage classification left. */
 private void processDeclassifiedEntityEvent(EntityDetail entityDetail) throws OCFCheckedExceptionBase, JsonProcessingException{}

 

}