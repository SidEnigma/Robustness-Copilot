/*
  * Copyright 2014 jmrozanec
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.cronutils.model.definition;
 
 import com.cronutils.model.CronType;
 import com.cronutils.model.field.CronFieldName;
 import com.cronutils.model.field.definition.*;
 
 import java.util.*;
 
 /**
  * Builder that allows to define and create CronDefinition instances.
  */
 public class CronDefinitionBuilder {
     private final Map<CronFieldName, FieldDefinition> fields = new EnumMap<>(CronFieldName.class);
     private final Set<CronConstraint> cronConstraints = new HashSet<>();
     private boolean matchDayOfWeekAndDayOfMonth;
 
     /**
      * Constructor.
      */
     private CronDefinitionBuilder() {/*NOP*/}
 
     /**
      * Creates a builder instance.
      *
      * @return new CronDefinitionBuilder instance
      */
     public static CronDefinitionBuilder defineCron() {
         return new CronDefinitionBuilder();
     }
 
     /**
      * Adds definition for seconds field.
      *
      * @return new FieldDefinitionBuilder instance
      */
     public FieldDefinitionBuilder withSeconds() {
         return new FieldDefinitionBuilder(this, CronFieldName.SECOND);
     }
 
     /**
      * Adds definition for minutes field.
      *
      * @return new FieldDefinitionBuilder instance
      */
     public FieldDefinitionBuilder withMinutes() {
         return new FieldDefinitionBuilder(this, CronFieldName.MINUTE);
     }
 
     /**
      * Adds definition for hours field.
      *
      * @return new FieldDefinitionBuilder instance
      */
     public FieldDefinitionBuilder withHours() {
         return new FieldDefinitionBuilder(this, CronFieldName.HOUR);
     }
 
     /**
      * Adds definition for day of month field.
      *
      * @return new FieldSpecialCharsDefinitionBuilder instance
      */
     public FieldSpecialCharsDefinitionBuilder withDayOfMonth() {
         return new FieldSpecialCharsDefinitionBuilder(this, CronFieldName.DAY_OF_MONTH);
     }
 
     /**
      * Adds definition for month field.
      *
      * @return new FieldDefinitionBuilder instance
      */
     public FieldDefinitionBuilder withMonth() {
         return new FieldDefinitionBuilder(this, CronFieldName.MONTH);
     }
 
     /**
      * Adds definition for day of week field.
      *
      * @return new FieldSpecialCharsDefinitionBuilder instance
      */
     public FieldDayOfWeekDefinitionBuilder withDayOfWeek() {
         return new FieldDayOfWeekDefinitionBuilder(this, CronFieldName.DAY_OF_WEEK);
     }
 
     /**
      * Adds definition for year field.
      *
      * @return new FieldDefinitionBuilder instance
      */
     public FieldDefinitionBuilder withYear() {
         return new FieldDefinitionBuilder(this, CronFieldName.YEAR);
     }
 
     /**
      * Adds definition for day of year field.
      *
      * @return new FieldDefinitionBuilder instance
      */
     public FieldQuestionMarkDefinitionBuilder withDayOfYear() {
         return new FieldQuestionMarkDefinitionBuilder(this, CronFieldName.DAY_OF_YEAR);
     }
 
     /**
      * Sets matchDayOfWeekAndDayOfMonth value to true.
      *
      * @return this CronDefinitionBuilder instance
      */
     public CronDefinitionBuilder matchDayOfWeekAndDayOfMonth() {
         matchDayOfWeekAndDayOfMonth = true;
         return this;
     }
 
     /**
      * Adds a cron validation.
      * @param validation - constraint validation
      * @return this CronDefinitionBuilder instance
      */
     public CronDefinitionBuilder withCronValidation(final CronConstraint validation) {
         cronConstraints.add(validation);
         return this;
     }
 
     /**
      * Registers a certain FieldDefinition.
      *
      * @param definition - FieldDefinition  instance, never null
      */
     public void register(final FieldDefinition definition) {
         //ensure that we can't register a mandatory definition if there are already optional ones
         boolean hasOptionalField = false;
         for (final FieldDefinition fieldDefinition : fields.values()) {
             if (fieldDefinition.isOptional()) {
                 hasOptionalField = true;
                 break;
             }
         }
         if (!definition.isOptional() && hasOptionalField) {
             throw new IllegalArgumentException("Can't register mandatory definition after a optional definition.");
         }
         fields.put(definition.getFieldName(), definition);
     }
 
 
/** From the provided field definitions, create a new instance of CronDefinition */
 public CronDefinition instance(){}

 

}