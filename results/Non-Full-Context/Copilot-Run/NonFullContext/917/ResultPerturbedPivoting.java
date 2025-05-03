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
 
 package tech.tablesaw.columns.numbers;
 
 import org.apache.commons.math3.random.EmpiricalDistribution;
 import org.apache.commons.math3.stat.StatUtils;
 import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
 import tech.tablesaw.api.DoubleColumn;
 import tech.tablesaw.api.NumericColumn;
 
 public interface NumberMapFunctions {
 
   /**
    * Returns a transformation of the data in this column such that the result has a mean of 0, and a
    * standard deviation of 1
    */
   default DoubleColumn normalize() {
     double[] result = StatUtils.normalize(asDoubleArray());
     return DoubleColumn.create(name() + " normalized", result);
   }
 
   String name();
 
   int size();
 
   boolean isEmpty();
 
   double[] asDoubleArray();
 
   /** Return the elements of this column as the ratios of their value and the sum of all elements */
   default DoubleColumn asRatio() {
     DoubleColumn pctColumn = DoubleColumn.create(name() + " percents", size());
     double total = sum();
     for (int i = 0; i < size(); i++) {
       if (total != 0) {
         pctColumn.set(i, getDouble(i) / total);
       }
     }
     return pctColumn;
   }
 
   double sum();
 
   /**
    * Return the elements of this column as the percentages of their value relative to the sum of all
    * elements
    */
   default DoubleColumn asPercent() {
     DoubleColumn pctColumn = DoubleColumn.create(name() + " percents", size());
     double total = sum();
     for (int i = 0; i < size(); i++) {
       if (total != 0) {
         pctColumn.set(i, (getDouble(i) / total) * 100);
       }
     }
     return pctColumn;
   }
 
   default DoubleColumn subtract(NumericColumn<?> column2) {
     int col1Size = size();
     int col2Size = column2.size();
     if (col1Size != col2Size)
       throw new IllegalArgumentException("The columns must have the same number of elements");
 
     DoubleColumn result = DoubleColumn.create(name() + " - " + column2.name(), col1Size);
     for (int r = 0; r < col1Size; r++) {
       result.set(r, subtract(getDouble(r), column2.getDouble(r)));
     }
     return result;
   }
 
   default DoubleColumn add(NumericColumn<?> column2) {
     int col1Size = size();
     int col2Size = column2.size();
     if (col1Size != col2Size)
       throw new IllegalArgumentException("The columns must have the same number of elements");
 
     DoubleColumn result = DoubleColumn.create(name() + " + " + column2.name(), col1Size);
     for (int r = 0; r < col1Size; r++) {
       result.set(r, add(getDouble(r), column2.getDouble(r)));
     }
     return result;
   }
 
   default DoubleColumn multiply(NumericColumn<?> column2) {
     int col1Size = size();
     int col2Size = column2.size();
     if (col1Size != col2Size)
       throw new IllegalArgumentException("The columns must have the same number of elements");
 
     DoubleColumn result = DoubleColumn.create(name() + " * " + column2.name(), col1Size);
 
     for (int r = 0; r < col1Size; r++) {
       result.set(r, multiply(getDouble(r), column2.getDouble(r)));
     }
     return result;
   }
 
   default DoubleColumn divide(NumericColumn<?> column2) {
     int col1Size = size();
     int col2Size = column2.size();
     if (col1Size != col2Size)
       throw new IllegalArgumentException("The columns must have the same number of elements");
 
     DoubleColumn result = DoubleColumn.create(name() + " / " + column2.name(), col1Size);
     for (int r = 0; r < col1Size; r++) {
       result.set(r, divide(getDouble(r), column2.getDouble(r)));
     }
     return result;
   }
 
   default DoubleColumn add(Number value) {
     double val = value.doubleValue();
     DoubleColumn result = DoubleColumn.create(name() + " + " + val, size());
     for (int i = 0; i < size(); i++) {
       result.set(i, add(getDouble(i), val));
     }
     return result;
   }
 
   default DoubleColumn subtract(Number value) {
     double val = value.doubleValue();
     DoubleColumn result = DoubleColumn.create(name() + " - " + val, size());
     for (int i = 0; i < size(); i++) {
       result.set(i, subtract(getDouble(i), val));
     }
     return result;
   }
 
   default DoubleColumn divide(Number value) {
     double val = value.doubleValue();
     DoubleColumn result = DoubleColumn.create(name() + " / " + val, size());
     for (int i = 0; i < size(); i++) {
       result.set(i, divide(getDouble(i), val));
     }
     return result;
   }
 
   default DoubleColumn multiply(Number value) {
     double val = value.doubleValue();
     DoubleColumn result = DoubleColumn.create(name() + " * " + val, size());
     for (int i = 0; i < size(); i++) {
       result.set(i, multiply(getDouble(i), val));
     }
     return result;
   }
 
   default double add(double val1, double val2) {
     if (DoubleColumnType.valueIsMissing(val1) || DoubleColumnType.valueIsMissing(val2)) {
       return DoubleColumnType.missingValueIndicator();
     }
     return val1 + val2;
   }
 
   default double multiply(double val1, double val2) {
     if (DoubleColumnType.valueIsMissing(val1) || DoubleColumnType.valueIsMissing(val2)) {
       return DoubleColumnType.missingValueIndicator();
     }
     return val1 * val2;
   }
 
   default double divide(double val1, double val2) {
     if (DoubleColumnType.valueIsMissing(val1) || DoubleColumnType.valueIsMissing(val2)) {
       return DoubleColumnType.missingValueIndicator();
     }
     return val1 / val2;
   }
 
   /** Returns the result of subtracting val2 from val1, after handling missing values */
   default double subtract(double val1, double val2) {
     if (DoubleColumnType.valueIsMissing(val1) || DoubleColumnType.valueIsMissing(val2)) {
       return DoubleColumnType.missingValueIndicator();
     }
     return val1 - val2;
   }
 
   /** Returns a NumberColumn with the exponential power of each value in this column */
   default DoubleColumn power(double power) {
     DoubleColumn newColumn = DoubleColumn.create(name() + "[pow]", size());
     for (int i = 0; i < size(); i++) {
       newColumn.set(i, Math.pow(getDouble(i), power));
     }
     return newColumn;
   }
 
   default DoubleColumn power(NumericColumn<?> powerColumn) {
     DoubleColumn result = DoubleColumn.create(name() + "[pow]", size());
     for (int i = 0; i < size(); i++) {
       result.set(i, Math.pow(getDouble(i), powerColumn.getDouble(i)));
     }
     return result;
   }
 
   /** Returns a NumberColumn with the reciprocal (1/n) for each value n in this column */
   default DoubleColumn reciprocal() {
     DoubleColumn result = DoubleColumn.create(name() + "[1/n]", size());
     for (int i = 0; i < size(); i++) {
       if (isMissing(i)) {
         result.setMissing(i);
       } else {
         result.set(i, 1 / getDouble(i));
       }
     }
     return result;
   }
 
   /** Returns a NumberColumn with the square of each value in this column */
   default DoubleColumn square() {
     DoubleColumn newColumn = power(2);
     newColumn.setName(name() + "[sq]");
     return newColumn;
   }
 
   default DoubleColumn sqrt() {
     DoubleColumn newColumn = DoubleColumn.create(name() + "[sqrt]", size());
     for (int i = 0; i < size(); i++) {
       newColumn.set(i, Math.sqrt(getDouble(i)));
     }
     return newColumn;
   }
 
   default DoubleColumn cubeRoot() {
     DoubleColumn newColumn = DoubleColumn.create(name() + "[cbrt]", size());
     for (int i = 0; i < size(); i++) {
       newColumn.set(i, Math.cbrt(getDouble(i)));
     }
     return newColumn;
   }
 
   default DoubleColumn cube() {
     DoubleColumn newColumn = power(3);
     newColumn.setName(name() + "[cb]");
     return newColumn;
   }
 
   default DoubleColumn remainder(DoubleColumn column2) {
     DoubleColumn result = DoubleColumn.create(name() + " % " + column2.name(), size());
     for (int r = 0; r < size(); r++) {
       double val1 = getDouble(r);
       double val2 = column2.getDouble(r);
       if (DoubleColumnType.valueIsMissing(val1) || DoubleColumnType.valueIsMissing(val2)) {
         result.setMissing(r);
       } else {
         result.set(r, getDouble(r) % column2.getDouble(r));
       }
     }
     return result;
   }
 
   default DoubleColumn remainder(double val2) {
     DoubleColumn result = DoubleColumn.create(name() + " % " + val2, size());
     for (int r = 0; r < size(); r++) {
       double val1 = getDouble(r);
       if (DoubleColumnType.valueIsMissing(val1) || DoubleColumnType.valueIsMissing(val2)) {
         result.setMissing(r);
       } else {
         result.set(r, getDouble(r) % val2);
       }
     }
     return result;
   }
 
   /** Returns the natural log of the values in this column as a NumberColumn. */
   default DoubleColumn logN() {
     DoubleColumn newColumn = DoubleColumn.create(name() + "[logN]", size());
 
     for (int i = 0; i < size(); i++) {
       newColumn.set(i, Math.log(getDouble(i)));
     }
     return newColumn;
   }
 
 
/** Returns the base 10 log of the values in this column as a Number column. */
 DoubleColumn log10(){
    DoubleColumn newColumn = DoubleColumn.create(name() + "[log10]", size());
    for (int i = 0; i < size(); i++) {
      newColumn.set(i, Math.log10(getDouble(i)));
    }
    return newColumn;
  }
  
    /** Returns the base 2 log of the values in this column as a Number column. */
    default DoubleColumn log2() {
      DoubleColumn newColumn = DoubleColumn.create(name() + "[log2]", size());
      for (int i = 0; i < size(); i++) {
        newColumn.set(i, Math.log(getDouble(i)) / Math.log(2));
      }
      return newColumn;
    }
  
    /** Returns the base e log of the values in this column as a Number column. */
    default DoubleColumn logE() {
      DoubleColumn newColumn = DoubleColumn.create(name() + "[logE]", size());
      for (int i = 0; i < size(); i++) {
        newColumn.set(i, Math.log(getDouble(i)) / Math.log(Math.E));
      }
      return newColumn;
    }
  
    /** Returns the natural log of the values in this column as a Number column. */
    default DoubleColumn log(double base) {
      DoubleColumn newColumn = DoubleColumn.create(name() + "[log" + base + "]", size());
      for (int i = 0; i < size(); i++) {
        newColumn.set(i, Math.log(getDouble(i)) / Math.log(base));
      }
      return newColumn;
    }
  
    /** Returns the natural log of the values in this column as a Number column. */
    default DoubleColumn log(NumericColumn<?> baseColumn) {
      DoubleColumn newColumn = DoubleColumn.create(name() + "[log]", size());
      for (int i = 0; i < size(); i++) {
        newColumn.set(i, Math.log(getDouble(i)) / Math.log(baseColumn.getDouble(i)));   
 }

 

}