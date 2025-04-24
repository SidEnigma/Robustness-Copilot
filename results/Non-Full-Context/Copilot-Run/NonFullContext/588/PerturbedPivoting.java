package org.openapitools.codegen.validations.oas;
 
 import io.swagger.v3.oas.models.media.ComposedSchema;
 import io.swagger.v3.oas.models.media.Schema;
 
 import org.openapitools.codegen.utils.ModelUtils;
 import org.openapitools.codegen.utils.SemVer;
 import org.openapitools.codegen.validation.GenericValidator;
 import org.openapitools.codegen.validation.ValidationRule;
 
 import java.util.ArrayList;
 import java.util.Locale;
 import java.util.Set;
 import java.util.HashSet;
 import java.util.Arrays;
 
 /**
  * A standalone instance for evaluating rules and recommendations related to OAS {@link Schema}
  */
 class OpenApiSchemaValidations extends GenericValidator<SchemaWrapper> {
     OpenApiSchemaValidations(RuleConfiguration ruleConfiguration) {
         super(new ArrayList<>());
         if (ruleConfiguration.isEnableRecommendations()) {
             if (ruleConfiguration.isEnableOneOfWithPropertiesRecommendation()) {
                 rules.add(ValidationRule.warn(
                         "Schema defines properties alongside oneOf.",
                         "Schemas defining properties and oneOf are not clearly defined in the OpenAPI Specification. While our tooling supports this, it may cause issues with other tools.",
                         OpenApiSchemaValidations::checkOneOfWithProperties
                 ));
             }
             if (ruleConfiguration.isEnableSchemaTypeRecommendation()) {
                 rules.add(ValidationRule.warn(
                         "Schema uses the 'null' type but OAS document is version 3.0.",
                         "The 'null' type is not supported in OpenAPI 3.0.x. It is supported in OpenAPI 3.1 and above. While our tooling supports this, it may cause issues with other tools.",
                         OpenApiSchemaValidations::checkNullType
                 ));
             }
             if (ruleConfiguration.isEnableNullableAttributeRecommendation()) {
                 rules.add(ValidationRule.warn(
                         "Schema uses the 'nullable' attribute.",
                         "The 'nullable' attribute is deprecated in OpenAPI 3.1, and may no longer be supported in future releases. Consider migrating to the 'null' type.",
                         OpenApiSchemaValidations::checkNullableAttribute
                 ));
             }
             if (ruleConfiguration.isEnableInvalidTypeRecommendation()) {
                 rules.add(ValidationRule.warn(
                         "Schema uses an invalid value for the 'type' attribute.",
                         "The 'type' attribute must be one of 'null', 'boolean', 'object', 'array', 'number', 'string', or 'integer'.",
                         OpenApiSchemaValidations::checkInvalidType
                 ));
             }
         }
     }
 
 
/** JSON Schema defines oneOf as a validation property that can be applied to any schema. */
 private static ValidationRule.Result checkOneOfWithProperties(SchemaWrapper schemaWrapper){}

 

}