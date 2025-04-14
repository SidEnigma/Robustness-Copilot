/*
  * Copyright (c) 2016 Network New Technologies Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.networknt.schema;
 
 import com.fasterxml.jackson.databind.JsonNode;
 import com.fasterxml.jackson.databind.node.JsonNodeType;
 import com.fasterxml.jackson.databind.node.TextNode;
 import com.networknt.schema.utils.JsonNodeUtil;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.Set;
 
 public class TypeValidator extends BaseJsonValidator implements JsonValidator {
     private static final String TYPE = "type";
     private static final String ENUM = "enum";
     private static final String REF = "$ref";
 
     private static final Logger logger = LoggerFactory.getLogger(TypeValidator.class);
 
     private JsonType schemaType;
     private JsonSchema parentSchema;
     private UnionTypeValidator unionTypeValidator;
     private final ValidationContext validationContext;
 
     public TypeValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) {
         super(schemaPath, schemaNode, parentSchema, ValidatorTypeCode.TYPE, validationContext);
         schemaType = TypeFactory.getSchemaNodeType(schemaNode);
         this.parentSchema = parentSchema;
         this.validationContext = validationContext;
         if (schemaType == JsonType.UNION) {
             unionTypeValidator = new UnionTypeValidator(schemaPath, schemaNode, parentSchema, validationContext);
         }
 
         parseErrorCode(getValidatorType().getErrorCodeKey());
     }
 
     public JsonType getSchemaType() {
         return schemaType;
     }
 
     public boolean equalsToSchemaType(JsonNode node) {
         JsonType nodeType = TypeFactory.getValueNodeType(node, validationContext.getConfig());
         // in the case that node type is not the same as schema type, try to convert node to the
         // same type of schema. In REST API, query parameters, path parameters and headers are all
         // string type and we must convert, otherwise, all schema validations will fail.
         if (nodeType != schemaType) {
             if (schemaType == JsonType.ANY) {
                 return true;
             }
 
             if (schemaType == JsonType.NUMBER && nodeType == JsonType.INTEGER) {
                 return true;
             }
 
             ValidatorState state = (ValidatorState) CollectorContext.getInstance().get(ValidatorState.VALIDATOR_STATE_KEY);
             if(JsonType.NULL.equals(nodeType) ){
                 if ((state.isComplexValidator() && JsonNodeUtil.isNodeNullable(parentSchema.getParentSchema().getSchemaNode(), validationContext.getConfig())) ||
                         JsonNodeUtil.isNodeNullable(this.getParentSchema().getSchemaNode())) {
                     return true;
                 }
             }
 
             // Skip the type validation when the schema is an enum object schema. Since the current type
             // of node itself can be used for type validation.
             if (isEnumObjectSchema(parentSchema)) {
                 return true;
             }
             if (validationContext.getConfig().isTypeLoose()) {
                 // if typeLoose is true, everything can be a size 1 array
                 if (schemaType == JsonType.ARRAY) {
                     return true;
                 }
                 if (nodeType == JsonType.STRING) {
                     if (schemaType == JsonType.INTEGER) {
                         if (isInteger(node.textValue())) {
                             return true;
                         }
                     } else if (schemaType == JsonType.BOOLEAN) {
                         if (isBoolean(node.textValue())) {
                             return true;
                         }
                     } else if (schemaType == JsonType.NUMBER) {
                         if (isNumeric(node.textValue())) {
                             return true;
                         }
                     }
                 }
             }
 
             return false;
         }
         return true;
     }
 
     public Set<ValidationMessage> validate(JsonNode node, JsonNode rootNode, String at) {
         debug(logger, node, rootNode, at);
 
         if (schemaType == JsonType.UNION) {
             return unionTypeValidator.validate(node, rootNode, at);
         }
 
         //if (!equalsToSchemaType(node)) {
         if(!JsonNodeUtil.equalsToSchemaType(node,schemaType, parentSchema, validationContext.getConfig())){
             JsonType nodeType = TypeFactory.getValueNodeType(node, validationContext.getConfig());
             return Collections.singleton(buildValidationMessage(at, nodeType.toString(), schemaType.toString()));
         }
         return Collections.emptySet();
     }
 
     public static boolean isInteger(String str) {
         if (str == null || str.equals("")) {
             return false;
         }
 
         // all code below could be replaced with
         //return str.matrch("[-+]?(?:0|[1-9]\\d*)")
         int i = 0;
         if (str.charAt(0) == '-' || str.charAt(0) == '+') {
             if (str.length() == 1) {
                 return false;
             }
             i = 1;
         }
         for (; i < str.length(); i++) {
             char c = str.charAt(i);
             if (c < '0' || c > '9') {
                 return false;
             }
         }
         return true;
     }
 
     public static boolean isBoolean(String s) {
         return "true".equals(s) || "false".equals(s);
     }
 
     public static boolean isNumeric(String str) {
         if (str == null || str.equals("")) {
             return false;
         }
 
         // all code below could be replaced with
         //return str.matrch("[-+]?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?")
         int i = 0;
         int len = str.length();
 
         if (str.charAt(i) == MINUS || str.charAt(i) == PLUS) {
             if (str.length() == 1) {
                 return false;
             }
             i = 1;
         }
 
         char c = str.charAt(i++);
 
         if (c == CHAR_0) {
             // TODO: if leading zeros are supported (counter to JSON spec) handle it here
             if (i < len) {
                 c = str.charAt(i++);
                 if (c != DOT && c != CHAR_E && c != CHAR_e) {
                     return false;
                 }
             }
         } else if (CHAR_1 <= c && c <= CHAR_9) {
             while (i < len && CHAR_0 <= c && c <= CHAR_9) {
                 c = str.charAt(i++);
             }
         } else {
             return false;
         }
 
         if (c == DOT) {
             if (i >= len) {
                 return false;
             }
             c = str.charAt(i++);
             while (i < len && CHAR_0 <= c && c <= CHAR_9) {
                 c = str.charAt(i++);
             }
         }
 
         if (c == CHAR_E || c == CHAR_e) {
             if (i >= len) {
                 return false;
             }
             c = str.charAt(i++);
             if (c == PLUS || c == MINUS) {
                 if (i >= len) {
                     return false;
                 }
                 c = str.charAt(i++);
             }
             while (i < len && CHAR_0 <= c && c <= CHAR_9) {
                 c = str.charAt(i++);
             }
         }
 
         return i >= len && (CHAR_0 <= c && c <= CHAR_9);
     }
 
     private static final char CHAR_0 = '0';
     private static final char CHAR_1 = '1';
     private static final char CHAR_9 = '9';
     private static final char MINUS = '-';
     private static final char PLUS = '+';
     private static final char DOT = '.';
     private static final char CHAR_E = 'E';
     private static final char CHAR_e = 'e';
 
 
/** Using the typeLoose flag's status, check if the JsonNode's value is a number. */
 public static boolean isNumber(JsonNode node, SchemaValidatorsConfig config){
        if (node.isNumber()) {
            return true;
        }
        if (node.isTextual()) {
            return isNumeric(node.textValue());
        }
        if (node.isBoolean()) {
            return false;
        }
        if (node.isNull()) {
            return false;
        }
        if (node.isArray()) {
            return false;
        }
        if (node.isObject()) {
            return false;
        }
        if (node.isMissingNode()) {
            return false;
        }
        if (node.isPointer()) {
            return false;
        }
        if (node.isValueNode()) {
            return false;
        }
        if (node.isContainerNode()) {
            return false;
        }
        if (node.isMissing()) {
            return false;
        }
        if (node.isPojo()) {
            return false;
        }
        if (node.isBigInteger()) {
            return true;
        }
        if (node.isBigDecimal()) {
            return true;
        }
        if (node.isInt()) {
            return true;
        }
        if (node.isLong()) {
            return true;
        }
        if (node.isDouble()) {
            return true;
        }
        if (node.isFloat()) {
            return true;
        }
        if (node.isShort()) {
            return true;
        }
        if (node.isByte()) {
            return true;
        }
        if (node.isIntValue()) {
            return true;
        }
        if (node.isLongValue()) {
            return true;
        }
        if (node.isDoubleValue()) {
            return true;
        }
        if (node.isFloatValue()) {
            return true;
        }
        if (node.isShortValue()) {
            return true;
        }
        if (node.isByteValue()) {
            return true;
        }
        if (node.isBigInteger       
 }

 

}