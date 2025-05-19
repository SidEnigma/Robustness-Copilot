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

    public static SchemaGenerator createCrdSchemaGenerator() {
        // Implementation logic goes here
        
        // Create a new instance of SchemaGenerator
        SchemaGenerator schemaGenerator = new SchemaGenerator();

        // Set the tuning parameters for the schema generator
        TuningParameters tuningParameters = TuningParameters.getInstance();
        schemaGenerator.setTuningParameters(tuningParameters);

        // Set the quantity format for the schema generator
        Quantity.Format quantityFormat = Quantity.Format.valueOf(tuningParameters.getQuantityFormat());
        schemaGenerator.setQuantityFormat(quantityFormat);

        // Set the optional properties for the schema generator
        schemaGenerator.setOptionalProperties(Collections.singleton(Optional.class));

        // Return the created schema generator
        return schemaGenerator;
    }
 

}