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

public int mapTo(final int dayOfWeek, final WeekDay targetWeekDayDefinition) {
    int mappedDayOfWeek = dayOfWeek;

    if (targetWeekDayDefinition.isFirstDayZero()) {
        mappedDayOfWeek -= 1;
    }

    mappedDayOfWeek += targetWeekDayDefinition.getMondayDoWValue();

    if (mappedDayOfWeek < 0) {
        mappedDayOfWeek += 7;
    } else if (mappedDayOfWeek >= 7) {
        mappedDayOfWeek -= 7;
    }

    return mappedDayOfWeek;
}
 

}