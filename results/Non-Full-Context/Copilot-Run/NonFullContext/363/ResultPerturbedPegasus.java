/*
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package tech.tablesaw.columns.dates;
 
 import static tech.tablesaw.api.DateColumn.valueIsMissing;
 
 import com.google.common.base.Preconditions;
 import com.google.common.base.Strings;
 import java.time.LocalDate;
 import java.time.LocalTime;
 import java.time.temporal.ChronoUnit;
 import java.time.temporal.TemporalUnit;
 import java.time.temporal.UnsupportedTemporalTypeException;
 import tech.tablesaw.api.DateColumn;
 import tech.tablesaw.api.DateTimeColumn;
 import tech.tablesaw.api.IntColumn;
 import tech.tablesaw.api.StringColumn;
 import tech.tablesaw.api.TimeColumn;
 import tech.tablesaw.columns.Column;
 import tech.tablesaw.columns.datetimes.PackedLocalDateTime;
 import tech.tablesaw.columns.numbers.NumberColumnFormatter;
 
 /** An interface for mapping operations unique to Date columns */
 public interface DateMapFunctions extends Column<LocalDate> {
 
   static String dateColumnName(Column<LocalDate> column1, int value, TemporalUnit unit) {
     return column1.name() + ": " + value + " " + unit.toString() + "(s)";
   }
 
   default IntColumn daysUntil(DateColumn column2) {
     return timeUntil(column2, ChronoUnit.DAYS);
   }
 
   default IntColumn weeksUntil(DateColumn column2) {
     return timeUntil(column2, ChronoUnit.WEEKS);
   }
 
   default IntColumn monthsUntil(DateColumn column2) {
     return timeUntil(column2, ChronoUnit.MONTHS);
   }
 
   default IntColumn yearsUntil(DateColumn column2) {
     return timeUntil(column2, ChronoUnit.YEARS);
   }
 
   default IntColumn dayOfMonth() {
     IntColumn newColumn = IntColumn.create(this.name() + " day of month");
     for (int r = 0; r < this.size(); r++) {
       int c1 = this.getIntInternal(r);
       if (valueIsMissing(c1)) {
         newColumn.appendMissing();
       } else {
         newColumn.append(PackedLocalDate.getDayOfMonth(c1));
       }
     }
     return newColumn;
   }
 
   default IntColumn dayOfYear() {
     IntColumn newColumn = IntColumn.create(this.name() + " day of year");
     for (int r = 0; r < this.size(); r++) {
       int c1 = this.getIntInternal(r);
       if (valueIsMissing(c1)) {
         newColumn.appendMissing();
       } else {
         newColumn.append((short) PackedLocalDate.getDayOfYear(c1));
       }
     }
     return newColumn;
   }
 
   default IntColumn monthValue() {
     IntColumn newColumn = IntColumn.create(this.name() + " month");
 
     for (int r = 0; r < this.size(); r++) {
       int c1 = this.getIntInternal(r);
       if (DateColumn.valueIsMissing(c1)) {
         newColumn.appendMissing();
       } else {
         newColumn.append(PackedLocalDate.getMonthValue(c1));
       }
     }
     return newColumn;
   }
 
   default StringColumn month() {
     StringColumn newColumn = StringColumn.create(this.name() + " month");
 
     for (int r = 0; r < this.size(); r++) {
       int c1 = this.getIntInternal(r);
       if (DateColumn.valueIsMissing(c1)) {
         newColumn.appendMissing();
       } else {
         newColumn.append(PackedLocalDate.getMonth(c1).name());
       }
     }
     return newColumn;
   }
 
   default IntColumn year() {
     IntColumn newColumn = IntColumn.create(this.name() + " year");
     for (int r = 0; r < this.size(); r++) {
       int c1 = this.getIntInternal(r);
       if (DateColumn.valueIsMissing(c1)) {
         newColumn.appendMissing();
       } else {
         newColumn.append(PackedLocalDate.getYear(c1));
       }
     }
     return newColumn;
   }
 
 
/** The year and quarter from this column will be used to sort the lexicographically in temporal order. */
 StringColumn yearQuarter(){
    StringColumn newColumn = StringColumn.create(this.name() + " year quarter");
    for (int r = 0; r < this.size(); r++) {
      int c1 = this.getIntInternal(r);
      if (DateColumn.valueIsMissing(c1)) {
        newColumn.appendMissing();
      } else {
        newColumn.append(PackedLocalDate.getYearQuarter(c1));
      }
    }
    return newColumn;
  }
  
    default IntColumn quarter() {
      IntColumn newColumn = IntColumn.create(this.name() + " quarter");
      for (int r = 0; r < this.size(); r++) {
        int c1 = this.getIntInternal(r);
        if (DateColumn.valueIsMissing(c1)) {
          newColumn.appendMissing();
        } else {
          newColumn.append(PackedLocalDate.getQuarter(c1));
        }
      }
      return newColumn;
    }
  
    default IntColumn dayOfWeek() {
      IntColumn newColumn = IntColumn.create(this.name() + " day of week");
      for (int r = 0; r < this.size(); r++) {
        int c1 = this.getIntInternal(r);
        if (DateColumn.valueIsMissing(c1)) {
          newColumn.appendMissing();
        } else {
          newColumn.append(PackedLocalDate.getDayOfWeek(c1));
        }
      }
      return newColumn;
    }
  
    default IntColumn dayOfWeekInMonth() {
      IntColumn newColumn = IntColumn.create(this.name() + " day of week in month");
      for (int r = 0; r < this.size(); r++) {
        int c1 = this.getIntInternal(r);
        if (DateColumn.valueIsMissing(c1)) {
          newColumn.appendMissing();
        } else {
          newColumn.append(PackedLocalDate.getDayOfWeekInMonth(c1));
        }
      }
      return newColumn;   
 }

 

}