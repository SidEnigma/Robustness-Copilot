/*
  * Copyright 2019 OpenAPI-Generator Contributors (https://openapi-generator.tech)
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     https://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.openapitools.codegen.validation;
 
 import java.util.List;
 
 /**
  * A generic implementation of a validator instance which simply applies rules to an input instance.
  *
  * @param <TInput> The type of object being evaluated.
  */
 public class GenericValidator<TInput> implements Validator<TInput> {
     protected List<ValidationRule> rules;
 
     /**
      * Constructs a new instance of {@link GenericValidator}.
      *
      * @param rules The rules to be evaluated during validation.
      */
     public GenericValidator(List<ValidationRule> rules) {
         this.rules = rules;
     }
 
 
/** Validates input, resulting in a instance of {@link ValidationResult} which provides details on all validations performed (success, error, warning). */

public ValidationResult validate(TInput input) {
    ValidationResult validationResult = new ValidationResult();

    for (ValidationRule rule : rules) {
        ValidationResult.RuleResult ruleResult = rule.validate(input);
        validationResult.addRuleResult(ruleResult);
    }

    return validationResult;
}
 

}