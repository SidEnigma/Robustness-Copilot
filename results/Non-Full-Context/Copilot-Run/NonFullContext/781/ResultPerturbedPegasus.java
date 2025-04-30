/* SPDX-License-Identifier: Apache 2.0 */
 /* Copyright Contributors to the ODPi Egeria project. */
 package org.odpi.openmetadata.accessservices.dataengine.server.handlers;
 
 import org.odpi.openmetadata.accessservices.dataengine.ffdc.DataEngineErrorCode;
 import org.odpi.openmetadata.accessservices.dataengine.model.DeleteSemantic;
 import org.odpi.openmetadata.accessservices.dataengine.model.Port;
 import org.odpi.openmetadata.accessservices.dataengine.model.PortAlias;
 import org.odpi.openmetadata.accessservices.dataengine.model.PortImplementation;
 import org.odpi.openmetadata.accessservices.dataengine.model.PortType;
 import org.odpi.openmetadata.accessservices.dataengine.server.mappers.CommonMapper;
 import org.odpi.openmetadata.commonservices.ffdc.InvalidParameterHandler;
 import org.odpi.openmetadata.commonservices.generichandlers.PortBuilder;
 import org.odpi.openmetadata.commonservices.generichandlers.PortHandler;
 import org.odpi.openmetadata.commonservices.repositoryhandler.RepositoryHandler;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.InvalidParameterException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.PropertyServerException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.UserNotAuthorizedException;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetailDifferences;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EnumPropertyValue;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceProperties;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;
 import org.odpi.openmetadata.repositoryservices.ffdc.exception.FunctionNotSupportedException;
 
 import java.util.Optional;
 
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.DISPLAY_NAME_PROPERTY_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.PORT_ALIAS_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.PORT_DELEGATION_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.PORT_IMPLEMENTATION_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.PORT_SCHEMA_RELATIONSHIP_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.PORT_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.PORT_TYPE_PROPERTY_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.QUALIFIED_NAME_PROPERTY_NAME;
 
 /**
  * PortHandler manages Port objects from the property server. It runs server-side in the DataEngine OMAS
  * and creates port entities with wire relationships through the OMRSRepositoryConnector.
  */
 public class DataEnginePortHandler {
     private final String serviceName;
     private final String serverName;
     private final OMRSRepositoryHelper repositoryHelper;
     private final InvalidParameterHandler invalidParameterHandler;
     private final DataEngineCommonHandler dataEngineCommonHandler;
     private final PortHandler<Port> portHandler;
     private final DataEngineRegistrationHandler registrationHandler;
 
     private static final String PROCESS_GUID_PARAMETER_NAME = "processGUID";
     private static final String PORT_GUID_PARAMETER_NAME = "portGUID";
     private static final String SCHEMA_TYPE_GUID_PARAMETER_NAME = "schemaTypeGUID";
 
     /**
      * Construct the handler information needed to interact with the repository services
      *
      * @param serviceName             name of this service
      * @param serverName              name of the local server
      * @param invalidParameterHandler handler for managing parameter errors
      * @param repositoryHandler       manages calls to the repository services
      * @param repositoryHelper        provides utilities for manipulating the repository services objects
      * @param dataEngineCommonHandler provides utilities for manipulating entities
      * @param portHandler             provides utilities for manipulating the repository services ports
      * @param registrationHandler     provides utilities for manipulating software server capability entities
      */
     public DataEnginePortHandler(String serviceName, String serverName, InvalidParameterHandler invalidParameterHandler,
                                  RepositoryHandler repositoryHandler, OMRSRepositoryHelper repositoryHelper,
                                  DataEngineCommonHandler dataEngineCommonHandler, PortHandler<Port> portHandler,
                                  DataEngineRegistrationHandler registrationHandler) {
         this.serviceName = serviceName;
         this.serverName = serverName;
         this.invalidParameterHandler = invalidParameterHandler;
         this.repositoryHelper = repositoryHelper;
         this.dataEngineCommonHandler = dataEngineCommonHandler;
         this.portHandler = portHandler;
         this.registrationHandler = registrationHandler;
     }
 
     /**
      * Create the port implementation attached to a process.
      *
      * @param userId             the name of the calling user
      * @param portImplementation the port implementation values
      * @param processGUID        the unique identifier of the process
      * @param externalSourceName the unique name of the external source
      *
      * @return unique identifier of the port implementation in the repository
      *
      * @throws InvalidParameterException  the bean properties are invalid
      * @throws UserNotAuthorizedException user not authorized to issue this request
      * @throws PropertyServerException    problem accessing the property server
      */
     public String createPortImplementation(String userId, PortImplementation portImplementation, String processGUID, String externalSourceName) throws
                                                                                                                                                 InvalidParameterException,
                                                                                                                                                 UserNotAuthorizedException,
                                                                                                                                                 PropertyServerException {
         return createPort(userId, portImplementation, PORT_IMPLEMENTATION_TYPE_NAME, processGUID, externalSourceName);
     }
 
 
/** Attach the port to the process. */
 public String createPortAlias(String userId, PortAlias portAlias, String processGUID, String externalSourceName) throws InvalidParameterException, UserNotAuthorizedException, PropertyServerException{
        return createPort(userId, portAlias, PORT_ALIAS_TYPE_NAME, processGUID, externalSourceName);        
 }

 

}