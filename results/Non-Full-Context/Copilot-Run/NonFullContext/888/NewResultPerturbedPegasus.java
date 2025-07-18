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
 
 package com.cronutils.mapper;
 
 import com.cronutils.Function;
 import com.cronutils.model.Cron;
 import com.cronutils.model.CronType;
 import com.cronutils.model.SingleCron;
 import com.cronutils.model.definition.CronDefinition;
 import com.cronutils.model.definition.CronDefinitionBuilder;
 import com.cronutils.model.field.CronField;
 import com.cronutils.model.field.CronFieldName;
 import com.cronutils.model.field.constraint.FieldConstraints;
 import com.cronutils.model.field.constraint.FieldConstraintsBuilder;
 import com.cronutils.model.field.definition.DayOfWeekFieldDefinition;
 import com.cronutils.model.field.definition.FieldDefinition;
 import com.cronutils.model.field.expression.*;
 import com.cronutils.model.field.expression.visitor.FieldExpressionVisitorAdaptor;
 import com.cronutils.model.field.expression.visitor.ValueMappingFieldExpressionVisitor;
 import com.cronutils.model.field.value.FieldValue;
 import com.cronutils.model.field.value.IntegerFieldValue;
 import com.cronutils.model.field.value.SpecialChar;
 import com.cronutils.utils.Preconditions;
 import com.cronutils.utils.VisibleForTesting;
 
 import java.util.ArrayList;
 import java.util.EnumMap;
 import java.util.List;
 import java.util.Map;
 
 import static com.cronutils.model.field.expression.FieldExpression.always;
 import static com.cronutils.model.field.expression.FieldExpression.questionMark;
 
 public class CronMapper {
     private final Map<CronFieldName, Function<CronField, CronField>> mappings;
     private final Function<Cron, Cron> cronRules;
     private final CronDefinition to;
 
     /**
      * Constructor.
      *
      * @param from      - source CronDefinition;
      *                  if null a NullPointerException will be raised
      * @param to        - target CronDefinition;
      *                  if null a NullPointerException will be raised
      * @param cronRules - cron rules
      */
     public CronMapper(final CronDefinition from, final CronDefinition to, final Function<Cron, Cron> cronRules) {
         Preconditions.checkNotNull(from, "Source CronDefinition must not be null");
         this.to = Preconditions.checkNotNull(to, "Destination CronDefinition must not be null");
         this.cronRules = Preconditions.checkNotNull(cronRules, "CronRules must not be null");
         mappings = new EnumMap<>(CronFieldName.class);
         buildMappings(from, to);
     }
 
     /**
      * Maps given cron to target cron definition.
      *
      * @param cron - Instance to be mapped;
      *             if null a NullPointerException will be raised
      * @return new Cron instance, never null;
      */
     public Cron map(final Cron cron) {
         Preconditions.checkNotNull(cron, "Cron must not be null");
         final List<CronField> fields = new ArrayList<>();
         for (final CronFieldName name : CronFieldName.values()) {
             if (mappings.containsKey(name)) {
                 final CronField field = mappings.get(name).apply(cron.retrieve(name));
                 if (field != null) {
                     fields.add(field);
                 }
             }
         }
         return cronRules.apply(new SingleCron(to, fields)).validate();
     }
 
     /**
      * Creates a CronMapper that maps a cron4j expression to a quartz expression.
      * @return a CronMapper for mapping from cron4j to quartz
      */
     public static CronMapper fromCron4jToQuartz() {
         return new CronMapper(
                 CronDefinitionBuilder.instanceDefinitionFor(CronType.CRON4J),
                 CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ),
                 setQuestionMark()
         );
     }
 
     public static CronMapper fromQuartzToCron4j() {
         return new CronMapper(
                 CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ),
                 CronDefinitionBuilder.instanceDefinitionFor(CronType.CRON4J),
                 sameCron()
         );
     }
 
     public static CronMapper fromQuartzToUnix() {
         return new CronMapper(
                 CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ),
                 CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX),
                 sameCron()
         );
     }
 
     public static CronMapper fromUnixToQuartz() {
         return new CronMapper(
                 CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX),
                 CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ),
                 setQuestionMark()
         );
     }
 
     public static CronMapper fromQuartzToSpring() {
         return new CronMapper(
                 CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ),
                 CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING),
                 setQuestionMark()
         );
     }
 
     public static CronMapper fromSpringToQuartz() {
         return new CronMapper(
                 CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING),
                 CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ),
                 setQuestionMark()
         );
     }
 
     public static CronMapper sameCron(final CronDefinition cronDefinition) {
         return new CronMapper(cronDefinition, cronDefinition, sameCron());
     }
 
     private static Function<Cron, Cron> sameCron() {
         return cron -> cron;
     }
 
     private static Function<Cron, Cron> setQuestionMark() {
         return cron -> {
             final CronField dow = cron.retrieve(CronFieldName.DAY_OF_WEEK);
             final CronField dom = cron.retrieve(CronFieldName.DAY_OF_MONTH);
             if (dow == null && dom == null) {
                 return cron;
             }
             if (dow.getExpression() instanceof QuestionMark || dom.getExpression() instanceof QuestionMark) {
                 return cron;
             }
             final Map<CronFieldName, CronField> fields = new EnumMap<>(CronFieldName.class);
             fields.putAll(cron.retrieveFieldsAsMap());
             if (dow.getExpression() instanceof Always) {
                 fields.put(CronFieldName.DAY_OF_WEEK,
                         new CronField(CronFieldName.DAY_OF_WEEK, questionMark(), fields.get(CronFieldName.DAY_OF_WEEK).getConstraints()));
             } else {
                 if (dom.getExpression() instanceof Always) {
                     fields.put(CronFieldName.DAY_OF_MONTH,
                             new CronField(CronFieldName.DAY_OF_MONTH, questionMark(), fields.get(CronFieldName.DAY_OF_MONTH).getConstraints()));
                 } else {
                     cron.validate();
                 }
             }
             return new SingleCron(cron.getCronDefinition(), new ArrayList<>(fields.values()));
         };
     }
 
 
/** Functions that map the fields from source to target were built. */

private void buildMappings(final CronDefinition from, final CronDefinition to) {
    // Functions that map the fields from source to target were built.
    mappings.put(CronFieldName.SECOND, this::mapSecond);
    mappings.put(CronFieldName.MINUTE, this::mapMinute);
    mappings.put(CronFieldName.HOUR, this::mapHour);
    mappings.put(CronFieldName.DAY_OF_MONTH, this::mapDayOfMonth);
    mappings.put(CronFieldName.MONTH, this::mapMonth);
    mappings.put(CronFieldName.DAY_OF_WEEK, this::mapDayOfWeek);
    mappings.put(CronFieldName.YEAR, this::mapYear);
}
 

}