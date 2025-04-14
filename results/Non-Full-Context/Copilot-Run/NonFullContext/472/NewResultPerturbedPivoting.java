package org.openapitools.codegen.utils;
 
 import org.openapitools.codegen.CodegenConfig;
 import org.openapitools.codegen.CodegenModel;
 import org.openapitools.codegen.CodegenProperty;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 /**
  * This class holds data to add to `oneOf` members. Let's consider this example:
  *
  * Foo:
  *   properties:
  *     x:
  *       oneOf:
  *         - $ref: "#/components/schemas/One
  *         - $ref: "#/components/schemas/Two
  *     y:
  *       type: string
  * One:
  *   properties:
  *     z:
  *       type: string
  * Two:
  *   properties:
  *     a:
  *       type: string
  *
  * In codegens that use this mechanism, `Foo` will become an interface and `One` will
  * become its implementing class. This class carries all data necessary to properly modify
  * the implementing class model. Specifically:
  *
  * * Interfaces that the implementing classes have to implement (in the example above, `One` and `Two` will implement `Foo`)
  * * Properties that need to be added to implementing classes (as `Foo` is interface, the `y` property will get pushed
  *   to implementing classes `One` and `Two`)
  * * Imports that need to be added to implementing classes (e.g. if type of property `y` needs a specific import, it
  *   needs to be added to `One` and `Two` because of the above point)
  */
 public class OneOfImplementorAdditionalData {
     private String implementorName;
     private List<String> additionalInterfaces = new ArrayList<String>();
     private List<CodegenProperty> additionalProps = new ArrayList<CodegenProperty>();
     private List<Map<String, String>> additionalImports = new ArrayList<Map<String, String>>();
     private final Logger LOGGER = LoggerFactory.getLogger(OneOfImplementorAdditionalData.class);
 
     public OneOfImplementorAdditionalData(String implementorName) {
         this.implementorName = implementorName;
     }
 
     public String getImplementorName() {
         return implementorName;
     }
 
 
/** Add data from a given CodegenModel that the oneOf implementer needs to implement. */

public void addFromInterfaceModel(CodegenModel cm, List<Map<String, String>> modelsImports) {
    // Add additional interfaces
    additionalInterfaces.addAll(cm.getInterfaces());

    // Add additional properties
    additionalProps.addAll(cm.getAllVars());

    // Add additional imports
    for (CodegenProperty prop : cm.getAllVars()) {
        Map<String, String> importMap = new HashMap<>();
        importMap.put("import", prop.getDatatypeWithEnum());
        additionalImports.add(importMap);
    }

    // Add model imports
    for (Map<String, String> importMap : cm.getImports()) {
        modelsImports.add(importMap);
    }

    LOGGER.info("Added additional data from CodegenModel: {}", cm.getName());
}
 

}