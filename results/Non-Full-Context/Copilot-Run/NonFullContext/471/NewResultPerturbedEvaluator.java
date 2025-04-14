/*
  * Copyright 2015 jmrozanec
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
 
 package com.cronutils.model.time.generator;
 
 import com.cronutils.mapper.ConstantsMapper;
 import com.cronutils.mapper.WeekDay;
 import com.cronutils.model.field.CronField;
 import com.cronutils.model.field.CronFieldName;
 import com.cronutils.model.field.expression.FieldExpression;
 import com.cronutils.model.field.expression.On;
 import com.cronutils.model.field.value.IntegerFieldValue;
 import com.cronutils.utils.Preconditions;
 
 import java.time.DayOfWeek;
 import java.time.LocalDate;
 
 class OnDayOfWeekValueGenerator extends OnDayOfCalendarValueGenerator {
 
     private static final On ON_SATURDAY = new On(new IntegerFieldValue(7));
     private final WeekDay mondayDoWValue;
 
     public OnDayOfWeekValueGenerator(final CronField cronField, final int year, final int month, final WeekDay mondayDoWValue) {
         super(cronField, year, month);
         Preconditions.checkArgument(CronFieldName.DAY_OF_WEEK.equals(cronField.getField()), "CronField does not belong to day of week");
         this.mondayDoWValue = mondayDoWValue;
     }
 
     @Override
     public int generateNextValue(final int reference) throws NoSuchValueException {
         final On on = ((On) cronField.getExpression());
         final int value = generateValue(on, year, month, reference);
         if (value <= reference) {
             throw new NoSuchValueException();
         }
         return value;
     }
 
     @Override
     public int generatePreviousValue(final int reference) throws NoSuchValueException {
         final On on = ((On) cronField.getExpression());
         final int value = generateValue(on, year, month, reference);
         if (value >= reference) {
             throw new NoSuchValueException();
         }
         return value;
     }
 
     @Override
     public boolean isMatch(final int value) {
         final On on = ((On) cronField.getExpression());
         try {
             return value == generateValue(on, year, month, value - 1);
         } catch (final NoSuchValueException ignored) {
             //we just skip, since we generate values until we get the exception
         }
         return false;
     }
 
     @Override
     protected boolean matchesFieldExpressionClass(final FieldExpression fieldExpression) {
         return fieldExpression instanceof On;
     }
 
     private int generateValue(final On on, final int year, final int month, final int reference) throws NoSuchValueException {
         switch (on.getSpecialChar().getValue()) {
             case HASH:
                 return generateHashValues(on, year, month);
             case L:
                 return on.getTime().getValue() == -1 ? /* L by itself simply means “7” or “SAT” */
                         generateNoneValues(ON_SATURDAY, year, month, reference) :
                         generateLValues(on, year, month);
             case NONE:
                 return generateNoneValues(on, year, month, reference);
             default:
                 throw new NoSuchValueException();
         }
     }
 
     private int generateHashValues(final On on, final int year, final int month) {
         final DayOfWeek dowForFirstDoM = LocalDate.of(year, month, 1).getDayOfWeek();//1-7
         final int requiredDoW = ConstantsMapper.weekDayMapping(mondayDoWValue, ConstantsMapper.JAVA8, on.getTime().getValue());//to normalize to jdk8-time value
         final int requiredNth = on.getNth().getValue();
         int baseDay = 1;//day 1 from given month
         final int diff = dowForFirstDoM.getValue() - requiredDoW;
         if (diff < 0) {
             baseDay = baseDay + Math.abs(diff);
         }
         if (diff > 0) {
             baseDay = baseDay + 7 - diff;
         }
         return (requiredNth - 1) * 7 + baseDay;
     }
 
     private int generateLValues(final On on, final int year, final int month) throws NoSuchValueException {
         final int lastDoM = LocalDate.of(year, month, 1).lengthOfMonth();
         final LocalDate lastDoMDateTime = LocalDate.of(year, month, lastDoM);
         final int dowForLastDoM = lastDoMDateTime.getDayOfWeek().getValue();//1-7
         final int requiredDoW = ConstantsMapper.weekDayMapping(mondayDoWValue, ConstantsMapper.JAVA8, on.getTime().getValue());//to normalize to jdk8-time value
         final int dowDiff = dowForLastDoM - requiredDoW;
 
         if (dowDiff == 0) {
             return lastDoMDateTime.getDayOfMonth();
         }
         if (dowDiff < 0) {
             return lastDoMDateTime.minusDays(dowForLastDoM + (long)(7 - requiredDoW)).getDayOfMonth();
         }
         if (dowDiff > 0) {
             return lastDoMDateTime.minusDays(dowDiff).getDayOfMonth();
         }
         throw new NoSuchValueException();
     }
 
 
/** The function takes the expression of the day of week and generates valid days. You need to pass -1 for the reference value for starting the generation of the sequence, allowing to handle special cases like when the day of the month is the initial matching value. */

private int generateNoneValues(final On on, final int year, final int month, final int reference) {
    // The function takes the expression of the day of week and generates valid days.
    // You need to pass -1 for the reference value for starting the generation of the sequence,
    // allowing to handle special cases like when the day of the month is the initial matching value.
    
    // Add your implementation logic here
    
    // Return the generated value
}
 

}