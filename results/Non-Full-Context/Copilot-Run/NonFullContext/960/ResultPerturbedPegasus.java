// Copyright (c) 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.weblogic.domain.model;
 
 import java.util.Collections;
 import java.util.Optional;
 
 import io.kubernetes.client.custom.Quantity;
 import oracle.kubernetes.json.SchemaGenerator;
 import oracle.kubernetes.operator.TuningParameters;
 
 public class CrdSchemaGenerator {
 
 
/** A custom-made generator is created for generating Kubernetes CRD schemas. */
 public static SchemaGenerator createCrdSchemaGenerator(){
     
  SchemaGenerator schemaGenerator = new SchemaGenerator();
 
  // Add the custom-made generator for generating Kubernetes CRD schemas.
  schemaGenerator.addCustomGenerator(new CrdSchemaGenerator());
 
  return schemaGenerator;           
 }

 

}