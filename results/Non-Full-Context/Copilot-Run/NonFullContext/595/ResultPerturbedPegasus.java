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
 
 package com.cronutils.mapper;
 
 import com.cronutils.Function;
 import com.cronutils.utils.Preconditions;
 import com.cronutils.utils.VisibleForTesting;
 
 import java.io.Serializable;
 
 @VisibleForTesting
 public class WeekDay implements Serializable {
 
     private static final long serialVersionUID = -1542525283511798919L;
     private final int mondayDoWValue;
     private final boolean firstDayZero;
 
     public WeekDay(final int mondayDoWValue, final boolean firstDayZero) {
         Preconditions.checkArgument(mondayDoWValue >= 0, "Monday Day of Week value must be greater or equal to zero");
         this.mondayDoWValue = mondayDoWValue;
         this.firstDayZero = firstDayZero;
     }
 
     public int getMondayDoWValue() {
         return mondayDoWValue;
     }
 
     public boolean isFirstDayZero() {
         return firstDayZero;
     }
 
 
/** Maps given WeekDay to representation hold this instance. */
 public int mapTo(final int dayOfWeek, final WeekDay targetWeekDayDefinition){
        Preconditions.checkArgument(dayOfWeek >= 0, "Day of Week value must be greater or equal to zero");
        Preconditions.checkArgument(targetWeekDayDefinition != null, "Target WeekDay definition must not be null");
    
        if (dayOfWeek == targetWeekDayDefinition.getMondayDoWValue()) {
            return 0;
        }
    
        if (dayOfWeek == targetWeekDayDefinition.getMondayDoWValue() + 1) {
            return 1;
        }
    
        if (dayOfWeek == targetWeekDayDefinition.getMondayDoWValue() + 2) {
            return 2;
        }
    
        if (dayOfWeek == targetWeekDayDefinition.getMondayDoWValue() + 3) {
            return 3;
        }
    
        if (dayOfWeek == targetWeekDayDefinition.getMondayDoWValue() + 4) {
            return 4;
        }
    
        if (dayOfWeek == targetWeekDayDefinition.getMondayDoWValue() + 5) {
            return 5;
        }
    
        if (dayOfWeek == targetWeekDayDefinition.getMondayDoWValue() + 6) {
            return 6;
        }
    
        if (dayOfWeek == targetWeekDayDefinition.getMondayDoWValue() + 7) {
            return 7;
        }
    
        if (dayOfWeek == targetWeekDayDefinition.getMondayDoWValue() + 8) {
            return 8;
        }
    
        if (dayOfWeek == targetWeekDayDefinition.getMondayDoWValue() + 9) {
            return 9;
        }
    
        if (dayOfWeek == targetWeekDayDefinition.getMondayDoWValue() + 10) {
            return 10;
        }
    
        if (dayOfWeek == targetWeekDayDefinition.getMondayDoWValue() + 11) {
            return 11;
        }
    
        if (dayOfWeek == targetWeekDayDefinition.getMondayDoWValue() + 12) {
            return 12;
        }
    
        if (dayOfWeek == targetWeekDayDefinition.getMondayDo        
 }

 

}