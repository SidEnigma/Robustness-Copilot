/* SPDX-License-Identifier: Apache 2.0 */
 /* Copyright Contributors to the ODPi Egeria project. */
 package org.odpi.openmetadata.accessservices.dataengine.server.handlers;
 
 import org.odpi.openmetadata.accessservices.dataengine.model.Attribute;
 import org.odpi.openmetadata.accessservices.dataengine.model.SchemaType;
 import org.odpi.openmetadata.commonservices.ffdc.InvalidParameterHandler;
 import org.odpi.openmetadata.commonservices.generichandlers.SchemaAttributeBuilder;
 import org.odpi.openmetadata.commonservices.generichandlers.SchemaAttributeHandler;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.InvalidParameterException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.PropertyServerException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.UserNotAuthorizedException;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetailDifferences;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;
 import org.springframework.util.CollectionUtils;
 
 import java.util.HashMap;
 import java.util.List;
 import java.util.Optional;
 
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.QUALIFIED_NAME_PROPERTY_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.SCHEMA_ATTRIBUTE_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.TABULAR_COLUMN_TYPE_GUID;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.TABULAR_COLUMN_TYPE_NAME;
 
 /**
  * DataEngineSchemaAttributeHandler manages schema attributes objects from the property server. It runs server-side in the
  * DataEngine OMAS and creates and retrieves schema type entities through the OMRSRepositoryConnector.
  */
 public class DataEngineSchemaAttributeHandler {
     public static final String SCHEMA_TYPE_GUID_PARAMETER_NAME = "schemaTypeGUID";
     private final String serviceName;
     private final String serverName;
     private final OMRSRepositoryHelper repositoryHelper;
     private final InvalidParameterHandler invalidParameterHandler;
     private final SchemaAttributeHandler<Attribute, SchemaType> schemaAttributeHandler;
     private final DataEngineRegistrationHandler dataEngineRegistrationHandler;
     private final DataEngineCommonHandler dataEngineCommonHandler;
 
     /**
      * Construct the handler information needed to interact with the repository services
      *
      * @param serviceName                   name of this service
      * @param serverName                    name of the local server
      * @param invalidParameterHandler       handler for managing parameter errors
      * @param repositoryHelper              provides utilities for manipulating the repository services objects
      * @param dataEngineRegistrationHandler provides calls for retrieving external data engine guid
      * @param dataEngineCommonHandler       provides utilities for manipulating entities
      * @param schemaAttributeHandler        handler for managing schema attributes in the metadata repositories
      */
     public DataEngineSchemaAttributeHandler(String serviceName, String serverName, InvalidParameterHandler invalidParameterHandler,
                                             OMRSRepositoryHelper repositoryHelper,
                                             SchemaAttributeHandler<Attribute, SchemaType> schemaAttributeHandler,
                                             DataEngineRegistrationHandler dataEngineRegistrationHandler,
                                             DataEngineCommonHandler dataEngineCommonHandler) {
         this.serviceName = serviceName;
         this.serverName = serverName;
         this.invalidParameterHandler = invalidParameterHandler;
         this.repositoryHelper = repositoryHelper;
         this.schemaAttributeHandler = schemaAttributeHandler;
         this.dataEngineRegistrationHandler = dataEngineRegistrationHandler;
         this.dataEngineCommonHandler = dataEngineCommonHandler;
     }
 
 
/** It uses the fully qualified name to retrieve the entity. It allows you to find out if the  SchemaAttribute object is already stored in the repository. */
 private Optional<EntityDetail> findSchemaAttributeEntity(String userId, String qualifiedName) throws UserNotAuthorizedException, PropertyServerException, InvalidParameterException{}

 

}