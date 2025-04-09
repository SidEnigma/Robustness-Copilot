/* SPDX-License-Identifier: Apache 2.0 */
 /* Copyright Contributors to the ODPi Egeria project. */
 package org.odpi.openmetadata.accessservices.dataengine.server.handlers;
 
 import org.apache.commons.collections4.CollectionUtils;
 import org.odpi.openmetadata.accessservices.dataengine.ffdc.DataEngineErrorCode;
 import org.odpi.openmetadata.accessservices.dataengine.model.Attribute;
 import org.odpi.openmetadata.accessservices.dataengine.model.DataItemSortOrder;
 import org.odpi.openmetadata.accessservices.dataengine.model.DeleteSemantic;
 import org.odpi.openmetadata.accessservices.dataengine.model.OwnerType;
 import org.odpi.openmetadata.accessservices.dataengine.server.mappers.CommonMapper;
 import org.odpi.openmetadata.commonservices.ffdc.InvalidParameterHandler;
 import org.odpi.openmetadata.commonservices.repositoryhandler.RepositoryHandler;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.InvalidParameterException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.PropertyServerException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.UserNotAuthorizedException;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceHeader;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceProperties;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceStatus;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.Relationship;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.RelationshipDifferences;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDef;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;
 import org.odpi.openmetadata.repositoryservices.ffdc.OMRSErrorCode;
 import org.odpi.openmetadata.repositoryservices.ffdc.exception.EntityNotDeletedException;
 import org.odpi.openmetadata.repositoryservices.ffdc.exception.FunctionNotSupportedException;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.util.HashSet;
 import java.util.List;
 import java.util.Optional;
 import java.util.Set;
 import java.util.stream.Collectors;
 
 /**
  * DataEngineCommonHandler manages objects from the property server. It runs server-side in the DataEngine OMAS
  * and creates port entities with wire relationships through the OMRSRepositoryConnector.
  */
 public class DataEngineCommonHandler {
     private final String serviceName;
     private final String serverName;
     private final RepositoryHandler repositoryHandler;
     private final OMRSRepositoryHelper repositoryHelper;
     private final InvalidParameterHandler invalidParameterHandler;
     private final DataEngineRegistrationHandler dataEngineRegistrationHandler;
 
     private static final Logger log = LoggerFactory.getLogger(DataEngineCommonHandler.class);
 
     /**
      * Construct the handler information needed to interact with the repository services
      *
      * @param serviceName                   name of this service
      * @param serverName                    name of the local server
      * @param invalidParameterHandler       handler for managing parameter errors
      * @param repositoryHandler             manages calls to the repository services
      * @param repositoryHelper              provides utilities for manipulating the repository services objects
      * @param dataEngineRegistrationHandler provides calls for retrieving external data engine guid
      */
     public DataEngineCommonHandler(String serviceName, String serverName, InvalidParameterHandler invalidParameterHandler,
                                    RepositoryHandler repositoryHandler, OMRSRepositoryHelper repositoryHelper,
                                    DataEngineRegistrationHandler dataEngineRegistrationHandler) {
         this.serviceName = serviceName;
         this.serverName = serverName;
         this.invalidParameterHandler = invalidParameterHandler;
         this.repositoryHelper = repositoryHelper;
         this.repositoryHandler = repositoryHandler;
         this.dataEngineRegistrationHandler = dataEngineRegistrationHandler;
     }
 
     /**
      * Create a new entity from an external source with the specified instance status
      *
      * @param userId             the name of the calling user
      * @param instanceProperties the properties of the entity
      * @param instanceStatus     initial status (needs to be valid for type)
      * @param entityTypeName     name of the entity's type
      * @param externalSourceName the unique name of the external source
      *
      * @return unique identifier of the process in the repository
      *
      * @throws InvalidParameterException  the bean properties are invalid
      * @throws UserNotAuthorizedException user not authorized to issue this request
      * @throws PropertyServerException    problem accessing the property server
      */
     protected String createExternalEntity(String userId, InstanceProperties instanceProperties, InstanceStatus instanceStatus, String entityTypeName,
                                           String externalSourceName) throws InvalidParameterException,
                                                                             UserNotAuthorizedException,
                                                                             PropertyServerException {
         final String methodName = "createExternalEntity";
 
         String externalSourceGUID = dataEngineRegistrationHandler.getExternalDataEngine(userId, externalSourceName);
 
         TypeDef entityTypeDef = repositoryHelper.getTypeDefByName(userId, entityTypeName);
 
         return repositoryHandler.createEntity(userId, entityTypeDef.getGUID(), entityTypeDef.getName(), externalSourceGUID,
                 externalSourceName, instanceProperties, instanceStatus, methodName);
     }
 
     /**
      * Update an existing entity
      *
      * @param userId             the name of the calling user
      * @param entityGUID         unique identifier of entity to update
      * @param instanceProperties the properties of the entity
      * @param entityTypeName     name of the entity's type
      * @param externalSourceName the external data engine
      *
      * @throws UserNotAuthorizedException user not authorized to issue this request
      * @throws PropertyServerException    problem accessing the property server
      * @throws InvalidParameterException  the bean properties are invalid
      */
     protected void updateEntity(String userId, String entityGUID, InstanceProperties instanceProperties, String entityTypeName,
                                 String externalSourceName) throws UserNotAuthorizedException, PropertyServerException, InvalidParameterException {
         final String methodName = "updateEntity";
 
         TypeDef entityTypeDef = repositoryHelper.getTypeDefByName(userId, entityTypeName);
 
         String externalSourceGUID = dataEngineRegistrationHandler.getExternalDataEngine(userId, externalSourceName);
 
         repositoryHandler.updateEntity(userId, externalSourceGUID, externalSourceName, entityGUID, entityTypeDef.getGUID(),
                 entityTypeName, instanceProperties, null, methodName);
     }
 
     /**
      * Build an EntityDetail object  based on the instance properties on an entity bean
      *
      * @param entityGUID         unique identifier of entity to update
      * @param instanceProperties the properties of the entity
      *
      * @return an EntityDetail object containing the entity properties
      */
     protected EntityDetail buildEntityDetail(String entityGUID, InstanceProperties instanceProperties) {
         EntityDetail entityDetail = new EntityDetail();
 
         entityDetail.setGUID(entityGUID);
         entityDetail.setProperties(instanceProperties);
 
         return entityDetail;
     }
 
     /**
      * Build an Relationship  object  based on the instance properties of a relationship
      *
      * @param entityGUID         unique identifier of entity to update
      * @param instanceProperties the properties of the relationship
      *
      * @return an Relationship object containing the entity properties
      */
     protected Relationship buildRelationship(String entityGUID, InstanceProperties instanceProperties) {
         Relationship relationship = new Relationship();
 
         relationship.setGUID(entityGUID);
         relationship.setProperties(instanceProperties);
 
         return relationship;
     }
 
     /**
      * Find out if the entity is already stored in the repository. It uses the fully qualified name to retrieve the entity
      *
      * @param userId         the name of the calling user
      * @param qualifiedName  the qualifiedName name of the entity to be searched
      * @param entityTypeName the type name of the entity
      *
      * @return optional with entity details if found, empty optional if not found
      *
      * @throws InvalidParameterException  the bean properties are invalid
      * @throws UserNotAuthorizedException user not authorized to issue this request
      * @throws PropertyServerException    problem accessing the property server
      */
     public Optional<EntityDetail> findEntity(String userId, String qualifiedName, String entityTypeName) throws UserNotAuthorizedException,
                                                                                                                 PropertyServerException,
                                                                                                                 InvalidParameterException {
         final String methodName = "findEntity";
 
         invalidParameterHandler.validateUserId(userId, methodName);
         invalidParameterHandler.validateName(qualifiedName, CommonMapper.QUALIFIED_NAME_PROPERTY_NAME, methodName);
 
         qualifiedName = repositoryHelper.getExactMatchRegex(qualifiedName);
 
         InstanceProperties properties = repositoryHelper.addStringPropertyToInstance(serviceName, null, CommonMapper.QUALIFIED_NAME_PROPERTY_NAME,
                 qualifiedName, methodName);
 
         TypeDef entityTypeDef = repositoryHelper.getTypeDefByName(userId, entityTypeName);
 
         Optional<EntityDetail> retrievedEntity = Optional.ofNullable(repositoryHandler.getUniqueEntityByName(userId, qualifiedName,
                 CommonMapper.QUALIFIED_NAME_PROPERTY_NAME, properties, entityTypeDef.getGUID(), entityTypeDef.getName(), methodName));
 
         log.trace("Searching for entity with qualifiedName: {}. Result is {}", qualifiedName,
                 retrievedEntity.map(InstanceHeader::getGUID).orElse(null));
 
         return retrievedEntity;
     }
 
     /**
      * Fetch the entity using the identifier and the type name. It uses the unique identifier to retrieve the entity
      *
      * @param userId           the user identifier
      * @param entityDetailGUID the entity unique identifier
      * @param entityTypeName   the entity type name
      *
      * @return optional with entity details if found, empty optional if not found
      *
      * @throws InvalidParameterException  one of the parameters is null or invalid.
      * @throws UserNotAuthorizedException user not authorized to issue this request.
      * @throws PropertyServerException    problem retrieving the entity.
      */
     public Optional<EntityDetail> getEntityDetails(String userId, String entityDetailGUID, String entityTypeName) throws InvalidParameterException,
                                                                                                                          PropertyServerException,
                                                                                                                          UserNotAuthorizedException {
         String methodName = "getEntityDetails";
         invalidParameterHandler.validateUserId(userId, methodName);
         invalidParameterHandler.validateGUID(entityDetailGUID, CommonMapper.GUID_PROPERTY_NAME, methodName);
 
         return Optional.ofNullable(repositoryHandler.getEntityByGUID(userId, entityDetailGUID, CommonMapper.GUID_PROPERTY_NAME, entityTypeName,
                 methodName));
     }
 
 
/** Updates or creates an external relation between two entities. Check before creating the relationship that it is not present. If the relationship is present, the instanceProperties of the relationship to be updated must be checked. */
 protected void upsertExternalRelationship(String userId, String firstGUID, String secondGUID, String relationshipTypeName, String firstEntityTypeName, String externalSourceName, InstanceProperties relationshipProperties) throws InvalidParameterException, UserNotAuthorizedException, PropertyServerException{}

 

}