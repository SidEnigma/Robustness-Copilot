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
 	
 
/** Returns all indexes that a bit set can have. */
 public static short[] allIndices(){
          short[] indices = new short[64];
          for (int i = 0; i < 64; i++) {
               indices[i] = (short)i;
          }
          return indices;
     }
     
     	/**
          * Creates a new set with all bits set to 0.
          * @return a new, empty, set.
          */
          public BitSet() {
     	}
     	
          /**
          * Creates a new set with all bits set to 1.
          * @return a new, full, set.
          */
     	public BitSet( long store ) {
     		this.store = store;
     	}
     	
     	/**
          * Creates a new set with all bits set to 0.
          * @return a new, empty, set.
          */
          public BitSet( int size ) {
     		this.store = 0l;
     	}
     	
     	/**
          * Creates a new set with all bits set to 1.
          * @return a new, full, set.
          */
     	public BitSet( int size, long store ) {
     		this.store = store;
     	}
     	
     	/**
          * Creates a new set with all bits set to 0.
          * @return a new, empty, set.
          */
          public BitSet( int size, long store, boolean[] bits ) {
     		this.store = store;
     	}
     	
     	/**
          * Creates a new set with all bits set to 1.
          * @return a new, full, set.
          */
     	public BitSet( int size, boolean[] bits ) {
     		this.store = 0l;
     		for (int i = 0; i < bits.length; i++) {
     			if (bits[i]) {
     				this.store |= 1l << i;
     			}
     		}
     	}
     	
     	/**
          * Creates a new set with all bits set to 0        
 }

 

}