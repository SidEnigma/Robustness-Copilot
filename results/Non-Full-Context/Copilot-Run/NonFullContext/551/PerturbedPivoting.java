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
 
 
/** Generates the context of a schema element without the context of the resource. */
 public Map<String, RelationshipsContext> buildSchemaElementContext(String userId, EntityDetail entityDetail) throws OCFCheckedExceptionBase{}

 

}