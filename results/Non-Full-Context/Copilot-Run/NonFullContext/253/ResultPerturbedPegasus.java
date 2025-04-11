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
 
 package tech.tablesaw.columns.datetimes;
 
 import static tech.tablesaw.columns.datetimes.PackedLocalDateTime.daysUntil;
 import static tech.tablesaw.columns.datetimes.PackedLocalDateTime.getDayOfMonth;
 import static tech.tablesaw.columns.datetimes.PackedLocalDateTime.getDayOfWeek;
 import static tech.tablesaw.columns.datetimes.PackedLocalDateTime.getDayOfYear;
 import static tech.tablesaw.columns.datetimes.PackedLocalDateTime.getHour;
 import static tech.tablesaw.columns.datetimes.PackedLocalDateTime.getMinute;
 import static tech.tablesaw.columns.datetimes.PackedLocalDateTime.getMinuteOfDay;
 import static tech.tablesaw.columns.datetimes.PackedLocalDateTime.getMonthValue;
 import static tech.tablesaw.columns.datetimes.PackedLocalDateTime.getQuarter;
 import static tech.tablesaw.columns.datetimes.PackedLocalDateTime.getSecondOfDay;
 import static tech.tablesaw.columns.datetimes.PackedLocalDateTime.getWeekOfYear;
 import static tech.tablesaw.columns.datetimes.PackedLocalDateTime.getYear;
 import static tech.tablesaw.columns.datetimes.PackedLocalDateTime.hoursUntil;
 import static tech.tablesaw.columns.datetimes.PackedLocalDateTime.minutesUntil;
 import static tech.tablesaw.columns.datetimes.PackedLocalDateTime.monthsUntil;
 import static tech.tablesaw.columns.datetimes.PackedLocalDateTime.pack;
 import static tech.tablesaw.columns.datetimes.PackedLocalDateTime.weeksUntil;
 import static tech.tablesaw.columns.datetimes.PackedLocalDateTime.yearsUntil;
 
 import com.google.common.base.Strings;
 import java.time.LocalDateTime;
 import java.time.Month;
 import java.time.temporal.ChronoUnit;
 import java.time.temporal.UnsupportedTemporalTypeException;
 import tech.tablesaw.api.DateColumn;
 import tech.tablesaw.api.DateTimeColumn;
 import tech.tablesaw.api.IntColumn;
 import tech.tablesaw.api.LongColumn;
 import tech.tablesaw.api.StringColumn;
 import tech.tablesaw.api.TimeColumn;
 import tech.tablesaw.columns.dates.PackedLocalDate;
 import tech.tablesaw.columns.numbers.NumberColumnFormatter;
 import tech.tablesaw.columns.strings.StringColumnType;
 import tech.tablesaw.columns.temporal.TemporalMapFunctions;
 import tech.tablesaw.columns.times.TimeColumnType;
 
 public interface DateTimeMapFunctions extends TemporalMapFunctions<LocalDateTime> {
 
   default IntColumn hour() {
     IntColumn newColumn = IntColumn.create(name() + "[" + "hour" + "]");
     for (int r = 0; r < size(); r++) {
       if (!isMissing(r)) {
         long c1 = getLongInternal(r);
         newColumn.append(getHour(c1));
       } else {
         newColumn.appendMissing();
       }
     }
     return newColumn;
   }
 
   default IntColumn minuteOfDay() {
     IntColumn newColumn = IntColumn.create(name() + "[" + "minute-of-day" + "]");
     for (int r = 0; r < size(); r++) {
       if (!isMissing(r)) {
         long c1 = getLongInternal(r);
         newColumn.append((short) getMinuteOfDay(c1));
       } else {
         newColumn.appendMissing();
       }
     }
     return newColumn;
   }
 
   default IntColumn secondOfDay() {
     IntColumn newColumn = IntColumn.create(name() + "[" + "second-of-day" + "]");
     for (int r = 0; r < size(); r++) {
       if (!isMissing(r)) {
         long c1 = getLongInternal(r);
         newColumn.append(getSecondOfDay(c1));
       } else {
         newColumn.appendMissing();
       }
     }
     return newColumn;
   }
 
   @Override
   default DateTimeColumn lead(int n) {
     DateTimeColumn column = lag(-n);
     column.setName(name() + " lead(" + n + ")");
     return column;
   }
 
   @Override
   DateTimeColumn lag(int n);
 
   /** Returns a TimeColumn containing the time portion of each dateTime in this DateTimeColumn */
   default TimeColumn time() {
     TimeColumn newColumn = TimeColumn.create(this.name() + " time");
     for (int r = 0; r < this.size(); r++) {
       long c1 = getLongInternal(r);
       if (DateTimeColumn.valueIsMissing(c1)) {
         newColumn.appendInternal(TimeColumnType.missingValueIndicator());
       } else {
         newColumn.appendInternal(PackedLocalDateTime.time(c1));
       }
     }
     return newColumn;
   }
 
   default IntColumn monthValue() {
     IntColumn newColumn = IntColumn.create(this.name() + " month");
     for (int r = 0; r < this.size(); r++) {
       if (isMissing(r)) {
         newColumn.appendMissing();
       } else {
         long c1 = getLongInternal(r);
         newColumn.append((short) getMonthValue(c1));
       }
     }
     return newColumn;
   }
 
   /** Returns a StringColumn containing the name of the month for each date/time in this column */
   default StringColumn month() {
     StringColumn newColumn = StringColumn.create(this.name() + " month");
     for (int r = 0; r < this.size(); r++) {
       long c1 = this.getLongInternal(r);
       if (DateTimeColumn.valueIsMissing(c1)) {
         newColumn.append(StringColumnType.missingValueIndicator());
       } else {
         newColumn.append(Month.of(getMonthValue(c1)).name());
       }
     }
     return newColumn;
   }
 
   /**
    * Returns a StringColumn with the year and quarter from this column concatenated into a String
    * that will sort lexicographically in temporal order.
    *
    * <p>This simplifies the production of plots and tables that aggregate values into standard
    * temporal units (e.g., you want monthly data but your source data is more than a year long and
    * you don't want months from different years aggregated together).
    */
   default StringColumn yearQuarter() {
     StringColumn newColumn = StringColumn.create(this.name() + " year & quarter");
     for (int r = 0; r < this.size(); r++) {
       long c1 = this.getLongInternal(r);
       if (DateTimeColumn.valueIsMissing(c1)) {
         newColumn.append(StringColumnType.missingValueIndicator());
       } else {
         String yq = getYear(c1) + "-" + getQuarter(c1);
         newColumn.append(yq);
       }
     }
     return newColumn;
   }
 
   @Override
   DateTimeColumn plus(long amountToAdd, ChronoUnit unit);
 
   @Override
   default DateTimeColumn plusYears(long amountToAdd) {
     return plus(amountToAdd, ChronoUnit.YEARS);
   }
 
   @Override
   default DateTimeColumn plusMonths(long amountToAdd) {
     return plus(amountToAdd, ChronoUnit.MONTHS);
   }
 
   @Override
   default DateTimeColumn plusWeeks(long amountToAdd) {
     return plus(amountToAdd, ChronoUnit.WEEKS);
   }
 
   @Override
   default DateTimeColumn plusDays(long amountToAdd) {
     return plus(amountToAdd, ChronoUnit.DAYS);
   }
 
   @Override
   default DateTimeColumn plusHours(long amountToAdd) {
     return plus(amountToAdd, ChronoUnit.HOURS);
   }
 
   @Override
   default DateTimeColumn plusMinutes(long amountToAdd) {
     return plus(amountToAdd, ChronoUnit.MINUTES);
   }
 
   @Override
   default DateTimeColumn plusSeconds(long amountToAdd) {
     return plus(amountToAdd, ChronoUnit.SECONDS);
   }
 
   @Override
   default DateTimeColumn plusMillis(long amountToAdd) {
     return plus(amountToAdd, ChronoUnit.MILLIS);
   }
 
   @Override
   default DateTimeColumn plusMicros(long amountToAdd) {
     return plus(amountToAdd, ChronoUnit.MICROS);
   }
 
   /**
    * Returns a StringColumn with the year and month from this column concatenated into a String that
    * will sort lexicographically in temporal order.
    *
    * <p>This simplifies the production of plots and tables that aggregate values into standard
    * temporal units (e.g., you want monthly data but your source data is more than a year long and
    * you don't want months from different years aggregated together).
    */
   default StringColumn yearMonth() {
     StringColumn newColumn = StringColumn.create(this.name() + " year & month");
     for (int r = 0; r < this.size(); r++) {
       long c1 = this.getLongInternal(r);
       if (DateTimeColumn.valueIsMissing(c1)) {
         newColumn.append(StringColumnType.missingValueIndicator());
       } else {
         String ym = String.valueOf(getYear(c1));
         ym = ym + "-" + Strings.padStart(String.valueOf(getMonthValue(c1)), 2, '0');
         newColumn.append(ym);
       }
     }
     return newColumn;
   }
 
   /**
    * Returns a StringColumn with the year and day-of-year derived from this column concatenated into
    * a String that will sort lexicographically in temporal order.
    *
    * <p>This simplifies the production of plots and tables that aggregate values into standard
    * temporal units (e.g., you want monthly data but your source data is more than a year long and
    * you don't want months from different years aggregated together).
    */
   default StringColumn yearDay() {
     StringColumn newColumn = StringColumn.create(this.name() + " year & month");
     for (int r = 0; r < this.size(); r++) {
       long c1 = this.getLongInternal(r);
       if (DateTimeColumn.valueIsMissing(c1)) {
         newColumn.append(StringColumnType.missingValueIndicator());
       } else {
         String ym = String.valueOf(getYear(c1));
         ym = ym + "-" + Strings.padStart(String.valueOf(getDayOfYear(c1)), 3, '0');
         newColumn.append(ym);
       }
     }
     return newColumn;
   }
 
 
/** The year and week-of-year derived from this column will be returned as a String. */
 StringColumn hourMinute(){
    StringColumn newColumn = StringColumn.create(this.name() + " hour & minute");
    for (int r = 0; r < this.size(); r++) {
      long c1 = this.getLongInternal(r);
      if (DateTimeColumn.valueIsMissing(c1)) {
        newColumn.append(StringColumnType.missingValueIndicator());
      } else {
        String ym = Strings.padStart(String.valueOf(getHour(c1)), 2, '0');
        ym = ym + ":" + Strings.padStart(String.valueOf(getMinute(c1)), 2, '0');
        newColumn.append(ym);
      }
    }
    return newColumn;
  }
  
    /**
      * Returns a StringColumn with the year and day-of-year derived from this column concatenated into
      * a String that will sort lexicographically in temporal order.
      *
      * <p>This simplifies the production of plots and tables that aggregate values into standard
      * temporal units (e.g., you want monthly data but your source data is more than a year long and
      * you don't want months from different years aggregated together).
      */
    default StringColumn yearDayOfYear() {
      StringColumn newColumn = StringColumn.create(this.name() + " year & day-of-year");
      for (int r = 0; r < this.size(); r++) {
        long c1 = this.getLongInternal(r);
        if (DateTimeColumn.valueIsMissing(c1)) {
          newColumn.append(StringColumnType.missingValueIndicator());
        } else {
          String ym = String.valueOf(getYear(c1));
          ym = ym + "-" + Strings.padStart(String.valueOf(getDayOfYear(c1)), 3, '0');
          newColumn.append(ym);
        }
      }
      return newColumn;
    }
  
    /**
      * Returns a StringColumn with the year and day-of-year derived from this column concatenated into
      *   
 }

 

}