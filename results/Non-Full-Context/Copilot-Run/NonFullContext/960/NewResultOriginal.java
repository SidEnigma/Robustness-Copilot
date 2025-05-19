// Copyright (c) 2021, Oracle and/or its affiliates.
 // Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 
 package oracle.kubernetes.weblogic.domain.model;
 
 import java.util.Collections;
 import java.util.Optional;
 
 import io.kubernetes.client.custom.Quantity;
 import oracle.kubernetes.json.SchemaGenerator;
 import oracle.kubernetes.operator.TuningParameters;
 
 public class CrdSchemaGenerator {
 
 
/** Creates a schema generator, suitably customized for generating Kubernetes CRD schemas. */

    public static SchemaGenerator createCrdSchemaGenerator() {
        // Add your implementation logic here
        
        // Create a new instance of SchemaGenerator
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        
        // Customize the schema generator for generating Kubernetes CRD schemas
        
        // Set the tuning parameters for the schema generator
        TuningParameters tuningParameters = TuningParameters.getInstance();
        schemaGenerator.setTuningParameters(tuningParameters);
        
        // Set the quantity converter for the schema generator
        Quantity.QuantityConverter quantityConverter = Quantity.getQuantityConverter();
        schemaGenerator.setQuantityConverter(quantityConverter);
        
        // Set the optional converter for the schema generator
        Optional.OptionalConverter optionalConverter = Optional.getOptionalConverter();
        schemaGenerator.setOptionalConverter(optionalConverter);
        
        // Set the empty list converter for the schema generator
        Collections.EmptyListConverter emptyListConverter = Collections.getEmptyListConverter();
        schemaGenerator.setEmptyListConverter(emptyListConverter);
        
        // Return the customized schema generator
        return schemaGenerator;
    }
 

}