package org.openapitools.codegen.validations.oas;
 
 import io.swagger.v3.oas.models.parameters.HeaderParameter;
 import io.swagger.v3.oas.models.parameters.Parameter;
 import org.apache.commons.lang3.StringUtils;
 import org.openapitools.codegen.validation.GenericValidator;
 import org.openapitools.codegen.validation.ValidationRule;
 
 import java.util.ArrayList;
 import java.util.Locale;
 
 /**
  * A standalone instance for evaluating rules and recommendations related to OAS {@link Parameter}
  */
 class OpenApiParameterValidations extends GenericValidator<ParameterWrapper> {
     OpenApiParameterValidations(RuleConfiguration ruleConfiguration) {
         super(new ArrayList<>());
         if (ruleConfiguration.isEnableRecommendations()) {
             if (ruleConfiguration.isEnableApacheNginxUnderscoreRecommendation()) {
                 rules.add(ValidationRule.warn(
                         ValidationConstants.ApacheNginxUnderscoreDescription,
                         ValidationConstants.ApacheNginxUnderscoreFailureMessage,
                         OpenApiParameterValidations::apacheNginxHeaderCheck
                 ));
             }
         }
     }
 
 
/** Apache and Nginx use legacy CGI behavior by default in which the underscore header is ignored. */
 private static ValidationRule.Result apacheNginxHeaderCheck(ParameterWrapper parameterWrapper){
        if (parameterWrapper.getParameter() instanceof HeaderParameter) {
            HeaderParameter headerParameter = (HeaderParameter) parameterWrapper.getParameter();
            if (StringUtils.isNotBlank(headerParameter.getName())) {
                if (headerParameter.getName().contains("_")) {
                    return ValidationRule.Result.failure(ValidationConstants.ApacheNginxUnderscoreFailureMessage);
                }
            }
        }
        return ValidationRule.Result.success();
    }       
 }

 

}