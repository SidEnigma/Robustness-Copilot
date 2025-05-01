package net.glowstone.util;
 
 import lombok.Getter;
 
 public final class VariableValueArray implements Cloneable {
 
     @Getter
     private final long[] backing;
     @Getter
     private final int capacity;
     @Getter
     private final int bitsPerValue;
     private final long valueMask;
 
     /**
      * Creates an instance.
      *
      * @param bitsPerValue the number of bits into which each value must fit
      * @param capacity the number of entries
      */
     public VariableValueArray(int bitsPerValue, int capacity) {
         if (capacity < 0) {
             throw new IllegalArgumentException(String
                     .format("capacity (%s) must not be negative", capacity));
         }
         if (bitsPerValue < 1) {
             throw new IllegalArgumentException(String
                     .format("bitsPerValue (%s) must not be less than 1", bitsPerValue));
         }
         if (bitsPerValue > 64) {
             throw new IllegalArgumentException(String
                     .format("bitsPerValue (%s) must not be greater than 64", bitsPerValue));
         }
         backing = new long[(int) Math.ceil((bitsPerValue * capacity) / 64.0)];
         this.bitsPerValue = bitsPerValue;
         valueMask = (1L << bitsPerValue) - 1L;
         this.capacity = capacity;
     }
 
 
/** Calculates the number of bits needed to store the given value. */
 public static int calculateNeededBits(int number){
        int bits = 0;
        while (number > 0){
            number >>= 1;
            bits++;
        }
        return bits;
    }
    
        /**
        * Gets the value at the given index.
        *
        * @param index the index
        * @return the value
        */
        public long get(int index) {
            if (index < 0 || index >= capacity) {
                throw new IndexOutOfBoundsException(String.format("index (%s) must be between 0 and %s", index, capacity));
            }
            int wordIndex = index / 64;
            int bitIndex = index % 64;
            return (backing[wordIndex] >>> bitIndex) & valueMask;
        }
    
        /**
        * Sets the value at the given index.
        *
        * @param index the index
        * @param value the value
        */
        public void set(int index, long value) {
            if (index < 0 || index >= capacity) {
                throw new IndexOutOfBoundsException(String.format("index (%s) must be between 0 and %s", index, capacity));
            }
            int wordIndex = index / 64;
            int bitIndex = index % 64;
            backing[wordIndex] = (backing[wordIndex] & ~(valueMask << bitIndex)) | (value << bitIndex);
        }
    
        /**
        * Gets the value at the given index.
        *
        * @param index the index
        * @return the value
        */
        public int getInt(int index) {
            return (int) get(index);
        }
    
        /**
        * Sets the value at the given index.
        *
        * @param index the index
        * @param value the value
        */
        public void setInt(int index, int value) {
            set(index, value);
        }
    
        /**
        * Gets the value at the given index.
        *
        * @param index the index
        * @return the value
        */
        public long getLong(int index) {
            return get(     
 }

 

}