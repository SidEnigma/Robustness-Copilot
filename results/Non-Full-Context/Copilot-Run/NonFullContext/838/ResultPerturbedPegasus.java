/* SPDX-License-Identifier: Apache 2.0 */
 /* Copyright Contributors to the ODPi Egeria project. */
 package org.odpi.openmetadata.accessservices.dataengine.server.handlers;
 
 
 import org.odpi.openmetadata.accessservices.dataengine.ffdc.DataEngineErrorCode;
 import org.odpi.openmetadata.accessservices.dataengine.model.DeleteSemantic;
 import org.odpi.openmetadata.accessservices.dataengine.model.ParentProcess;
 import org.odpi.openmetadata.accessservices.dataengine.model.Process;
 import org.odpi.openmetadata.accessservices.dataengine.model.ProcessContainmentType;
 import org.odpi.openmetadata.accessservices.dataengine.server.builders.ProcessPropertiesBuilder;
 import org.odpi.openmetadata.accessservices.dataengine.server.mappers.CommonMapper;
 import org.odpi.openmetadata.commonservices.ffdc.InvalidParameterHandler;
 import org.odpi.openmetadata.commonservices.generichandlers.AssetHandler;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.InvalidParameterException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.PropertyServerException;
 import org.odpi.openmetadata.frameworks.connectors.ffdc.UserNotAuthorizedException;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetailDifferences;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceProperties;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceStatus;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDef;
 import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;
 import org.odpi.openmetadata.repositoryservices.ffdc.exception.FunctionNotSupportedException;
 
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Optional;
 import java.util.Set;
 import java.util.stream.Collectors;
 
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.DISPLAY_NAME_PROPERTY_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.FORMULA_PROPERTY_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.IMPLEMENTATION_LANGUAGE_PROPERTY_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.PROCESS_HIERARCHY_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.PROCESS_PORT_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.PROCESS_TYPE_GUID;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.PROCESS_TYPE_NAME;
 import static org.odpi.openmetadata.commonservices.generichandlers.OpenMetadataAPIMapper.QUALIFIED_NAME_PROPERTY_NAME;
 
 /**
  * ProcessHandler manages Process objects from the property server.  It runs server-side in the DataEngine OMAS
  * and creates process entities and relationships through the OMRSRepositoryConnector.
  */
 public class DataEngineProcessHandler {
     private final String serviceName;
     private final String serverName;
     private final OMRSRepositoryHelper repositoryHelper;
     private final InvalidParameterHandler invalidParameterHandler;
     private final AssetHandler<Process> assetHandler;
     private final DataEngineCommonHandler dataEngineCommonHandler;
     private final DataEngineRegistrationHandler registrationHandler;
 
     public static final String PROCESS_GUID_PARAMETER_NAME = "processGUID";
 
     /**
      * Construct the handler information needed to interact with the repository services
      *
      * @param serviceName             name of this service
      * @param serverName              name of the local server
      * @param invalidParameterHandler handler for managing parameter errors
      * @param repositoryHelper        provides utilities for manipulating the repository services objects
      * @param assetHandler            provides utilities for manipulating the repository services assets
      * @param dataEngineCommonHandler provides utilities for manipulating entities
      * @param registrationHandler     provides utilities for manipulating software server capability entities
      **/
     public DataEngineProcessHandler(String serviceName, String serverName, InvalidParameterHandler invalidParameterHandler,
                                     OMRSRepositoryHelper repositoryHelper, AssetHandler<Process> assetHandler,
                                     DataEngineRegistrationHandler registrationHandler, DataEngineCommonHandler dataEngineCommonHandler) {
 
         this.serviceName = serviceName;
         this.serverName = serverName;
         this.invalidParameterHandler = invalidParameterHandler;
         this.repositoryHelper = repositoryHelper;
         this.assetHandler = assetHandler;
         this.registrationHandler = registrationHandler;
         this.dataEngineCommonHandler = dataEngineCommonHandler;
     }
 
     /**
      * Create the process
      *
      * @param userId             the name of the calling user
      * @param process            the values of the process
      * @param externalSourceName the unique name of the external source
      *
      * @return unique identifier of the process in the repository
      *
      * @throws InvalidParameterException  the bean properties are invalid
      * @throws UserNotAuthorizedException user not authorized to issue this request
      * @throws PropertyServerException    problem accessing the property server
      */
     public String createProcess(String userId, Process process, String externalSourceName) throws InvalidParameterException,
                                                                                                   UserNotAuthorizedException,
                                                                                                   PropertyServerException {
         final String methodName = "createProcess";
         validateProcessParameters(userId, process.getQualifiedName(), methodName);
 
         String externalSourceGUID = registrationHandler.getExternalDataEngine(userId, externalSourceName);
 
         return assetHandler.createAssetInRepository(userId, externalSourceGUID, externalSourceName, process.getQualifiedName(), process.getName(),
                 process.getDescription(), process.getZoneMembership(), process.getOwner(),
                 dataEngineCommonHandler.getOwnerTypeOrdinal(process.getOwnerType()), process.getOriginBusinessCapabilityGUID(),
                 process.getOriginBusinessCapabilityGUID(), process.getOtherOriginValues(), process.getAdditionalProperties(),
                 PROCESS_TYPE_GUID, PROCESS_TYPE_NAME, buildProcessExtendedProperties(process), InstanceStatus.DRAFT, methodName);
     }
 
     /**
      * Update the process
      *
      * @param userId                the name of the calling user
      * @param originalProcessEntity the created process entity
      * @param updatedProcess        the new values of the process
      * @param externalSourceName    the external data engine
      *
      * @throws InvalidParameterException  the bean properties are invalid
      * @throws UserNotAuthorizedException user not authorized to issue this request
      * @throws PropertyServerException    problem accessing the property server
      */
     public void updateProcess(String userId, EntityDetail originalProcessEntity, Process updatedProcess, String externalSourceName) throws
                                                                                                                                     InvalidParameterException,
                                                                                                                                     UserNotAuthorizedException,
                                                                                                                                     PropertyServerException {
 
         final String methodName = "updateProcess";
 
         validateProcessParameters(userId, updatedProcess.getQualifiedName(), methodName);
 
         String processGUID = originalProcessEntity.getGUID();
 
         ProcessPropertiesBuilder updatedProcessBuilder = getProcessPropertiesBuilder(updatedProcess);
 
         InstanceProperties updatedProcessProperties = updatedProcessBuilder.getInstanceProperties(methodName);
         EntityDetail updatedProcessEntity = dataEngineCommonHandler.buildEntityDetail(processGUID, updatedProcessProperties);
         EntityDetailDifferences entityDetailDifferences = repositoryHelper.getEntityDetailDifferences(originalProcessEntity,
                 updatedProcessEntity, true);
         if (!entityDetailDifferences.hasInstancePropertiesDifferences()) {
             return;
         }
 
         String externalSourceGUID = registrationHandler.getExternalDataEngine(userId, externalSourceName);
         assetHandler.updateAsset(userId, externalSourceGUID, externalSourceName, processGUID, PROCESS_GUID_PARAMETER_NAME,
                 updatedProcess.getQualifiedName(), updatedProcess.getName(), updatedProcess.getDescription(),
                 updatedProcess.getAdditionalProperties(), PROCESS_TYPE_GUID, PROCESS_TYPE_NAME,
                 buildProcessExtendedProperties(updatedProcess), methodName);
     }
 
 
/** If the Process object is already in the repository, you should find it. */
 public Optional<EntityDetail> findProcessEntity(String userId, String qualifiedName) throws UserNotAuthorizedException, PropertyServerException, InvalidParameterException{
        final String methodName = "findProcessEntity";
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateName(qualifiedName, methodName);
    
        return assetHandler.findAssetByGUID(userId, qualifiedName, PROCESS_TYPE_GUID, methodName);
    }
    
        /**
        * Find the process by its unique identifier.
        *
        * @param userId                the name of the calling user
        * @param processGUID           the unique identifier of the process
        * @param externalSourceName    the external data engine
        *
        * @return the process
        *
        * @throws InvalidParameterException  the bean properties are invalid
        * @throws UserNotAuthorizedException user not authorized to issue this request
        * @throws PropertyServerException    problem accessing the property server
        */
        public Process findProcessByGUID(String userId, String processGUID, String externalSourceName) throws InvalidParameterException,
                                                                                                            UserNotAuthorizedException,
                                                                                                            PropertyServerException {
    
            final String methodName = "findProcessByGUID";
            invalidParameterHandler.validateUserId(userId, methodName);
            invalidParameterHandler.validateGUID(processGUID, PROCESS_GUID_PARAMETER_NAME, methodName);
    
            String externalSourceGUID = registrationHandler.getExternalDataEngine(userId, externalSourceName);
    
            EntityDetail processEntity = assetHandler.getAssetByGUID(userId, processGUID, externalSourceGUID, methodName);
    
            return getProcessFromEntity(processEntity);
        }
    
        /**
        * Find the process by its unique identifier.
        *
        * @param userId                the name of the calling user
        * @param processGUID           the unique identifier of the process
        * @param externalSourceName    the external data engine
        *
        * @return the process
        *
        * @throws InvalidParameterException  the bean properties are invalid
        * @throws UserNotAuthorizedException user not authorized        
 }

 

}