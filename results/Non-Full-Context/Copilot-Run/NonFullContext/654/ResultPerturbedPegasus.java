package edu.harvard.iq.dataverse.util;
 
 import java.io.Serializable;
 import java.util.EnumSet;
 
 /**
  * A set, backed by a single {@code long}.
  * @author michael
  */
 public class BitSet implements Serializable {
 
 	private long store = 0l;
 	
 	/**
      * Creates a new set with all bits set to 0.
      * @return a new, empty, set.
      */
     public static BitSet emptySet() {
 		return new BitSet();
 	}
 	
     /**
      * Creates a new set with all bits set to 1.
      * @return a new, full, set.
      */
 	public static BitSet fullSet() {
 		return new BitSet( ~0 );
 	}
 	
 
/** A bit set may have all the indices. */
 public static short[] allIndices(){
          short[] indices = new short[Short.MAX_VALUE];
          for (short i = 0; i < indices.length; i++) {
               indices[i] = i;
          }
          return indices;
     }
     
     	/**
          * Creates a new set with all bits set to 1.
          * @return a new, full, set.
          */
     	public static BitSet fullSet(short[] indices) {
     		BitSet set = new BitSet();
     		for (short i : indices) {
     			set.set(i);
     		}
     		return set;
     	}
     	
     	/**
          * Creates a new set with all bits set to 1.
          * @return a new, full, set.
          */
     	public static BitSet fullSet(short[] indices, int start, int end) {
     		BitSet set = new BitSet();
     		for (short i = start; i < end; i++) {
     			set.set(i);
     		}
     		return set;
     	}
     	
     	/**
          * Creates a new set with all bits set to 1.
          * @return a new, full, set.
          */
     	public static BitSet fullSet(short[] indices, int start, int end, int step) {
     		BitSet set = new BitSet();
     		for (short i = start; i < end; i += step) {
     			set.set(i);
     		}
     		return set;
     	}
     	
     	/**
          * Creates a new set with all bits set to 1.
          * @return a new, full, set.
          */
     	public static BitSet fullSet(short[] indices, int start, int end, int step, int offset) {
     		BitSet set = new BitSet();
     		for (short i = start; i < end; i += step) {
     			set.set(i + offset);          
 }

 

}