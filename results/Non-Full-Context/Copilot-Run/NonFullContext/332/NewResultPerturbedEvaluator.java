/* SPDX-License-Identifier: Apache 2.0 */
 /* Copyright Contributors to the ODPi Egeria project. */
 package org.odpi.openmetadata.accessservices.dataengine.server.handlers;
 
 import org.apache.commons.lang3.StringUtils;
 import org.odpi.openmetadata.accessservices.dataengine.model.DeleteSemantic;
 import org.odpi.openmetadata.accessservices.dataengine.model.Topic;
 import org.odpi.openmetadata.commonservices.ffdc.InvalidParameterHandler;
 import org.odpi.openmetadata.commonservices.generichandlers.AssetHandler;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.InvalidParameterException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.PropertyServerException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.UserNotAuthorizedException;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail;
 import org.odpi.openmetadata.repositoryservices.ffdc.exception.FunctionNotSupportedException;
 
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Optional;
 
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.DISPLAY_NAME_PROPERTY_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.QUALIFIED_NAME_PROPERTY_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.TOPIC_TYPE_GUID;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.TOPIC_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.TOPIC_TYPE_PROPERTY_NAME;
 
 /**
  * DataEngineTopicHandler manages topic objects. It runs server-side in the
  * DataEngine OMAS and creates and retrieves collections entities through the OMRSRepositoryConnector.
  */
 public class DataEngineTopicHandler {
     private final InvalidParameterHandler invalidParameterHandler;
     private final AssetHandler<Topic> topicHandler;
     private final DataEngineCommonHandler dataEngineCommonHandler;
     private final DataEngineRegistrationHandler registrationHandler;
 
     public static final String TOPIC_GUID_PARAMETER_NAME = "topicGUID";
 
     /**
      * Construct the handler information needed to interact with the repository services
      *
      * @param invalidParameterHandler       handler for managing parameter errors
      * @param topicHandler                  provides utilities specific for manipulating topic entities
      * @param dataEngineCommonHandler       provides utilities for manipulating entities
      * @param dataEngineRegistrationHandler provides utilities for  software server capability entities
      */
     public DataEngineTopicHandler(InvalidParameterHandler invalidParameterHandler, AssetHandler<Topic> topicHandler,
                                   DataEngineRegistrationHandler dataEngineRegistrationHandler, DataEngineCommonHandler dataEngineCommonHandler) {
         this.invalidParameterHandler = invalidParameterHandler;
         this.topicHandler = topicHandler;
         this.registrationHandler = dataEngineRegistrationHandler;
         this.dataEngineCommonHandler = dataEngineCommonHandler;
     }
 
     /**
      * Create or update the topic with event types
      *
      * @param userId             the name of the calling user
      * @param topic              the values of the topic
      * @param externalSourceName the unique name of the external source
      *
      * @return unique identifier of the topic in the repository
      *
      * @throws InvalidParameterException  the bean properties are invalid
      * @throws UserNotAuthorizedException user not authorized to issue this request
      * @throws PropertyServerException    problem accessing the property server
      */
     public String upsertTopic(String userId, Topic topic, String externalSourceName) throws InvalidParameterException, PropertyServerException,
                                                                                             UserNotAuthorizedException {
         final String methodName = "upsertTopic";
         validateParameters(userId, methodName, topic.getQualifiedName(), topic.getDisplayName());
 
         String externalSourceGUID = registrationHandler.getExternalDataEngine(userId, externalSourceName);
         Optional<EntityDetail> originalTopicEntity = findTopicEntity(userId, topic.getQualifiedName());
 
         Map<String, Object> extendedProperties = new HashMap<>();
         if (StringUtils.isNotEmpty(topic.getTopicType())) {
             extendedProperties.put(TOPIC_TYPE_PROPERTY_NAME, topic.getTopicType());
         }
         int ownerTypeOrdinal = dataEngineCommonHandler.getOwnerTypeOrdinal(topic.getOwnerType());
         String topicGUID;
         if (originalTopicEntity.isEmpty()) {
             topicHandler.verifyExternalSourceIdentity(userId, externalSourceGUID, externalSourceName,
                     false, false, null, null);
             topicGUID = topicHandler.createAssetInRepository(userId, externalSourceGUID, externalSourceName, topic.getQualifiedName(),
                     topic.getDisplayName(), topic.getDescription(), topic.getZoneMembership(), topic.getOwner(), ownerTypeOrdinal,
                     topic.getOriginOrganizationGUID(), topic.getOriginBusinessCapabilityGUID(), topic.getOtherOriginValues(),
                     topic.getAdditionalProperties(), TOPIC_TYPE_GUID, TOPIC_TYPE_NAME, extendedProperties, methodName);
         } else {
             topicGUID = originalTopicEntity.get().getGUID();
             topicHandler.updateAsset(userId, externalSourceGUID, externalSourceName, topicGUID, TOPIC_GUID_PARAMETER_NAME, topic.getQualifiedName(),
                     topic.getDisplayName(), topic.getDescription(), topic.getAdditionalProperties(), TOPIC_TYPE_GUID, TOPIC_TYPE_NAME,
                     extendedProperties, methodName);
         }
 
         return topicGUID;
     }
 
 
/** Discover whether the Topic object is already present in the repository, using the fully qualified name for retrieving the entity. */

public Optional<EntityDetail> findTopicEntity(String userId, String qualifiedName) throws UserNotAuthorizedException, PropertyServerException, InvalidParameterException {
    final String methodName = "findTopicEntity";

    invalidParameterHandler.validateUserId(userId, methodName);
    invalidParameterHandler.validateName(qualifiedName, QUALIFIED_NAME_PROPERTY_NAME, methodName);

    return topicHandler.getEntityByUniquePropertyValue(TOPIC_TYPE_GUID, TOPIC_TYPE_NAME, QUALIFIED_NAME_PROPERTY_NAME, qualifiedName, methodName);
}
 

}