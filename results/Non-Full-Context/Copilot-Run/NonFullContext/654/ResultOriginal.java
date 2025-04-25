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
 	
 
/** Returns all the indices a bit set may have. */
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
          public BitSet( int size, int[] indices ) {
          }
     	
          /**
          * Creates a new set with all bits set to 1.
          * @return a new, full, set.
          */
     	public BitSet( int size, int[] indices, long store ) {
     		this.store = store;
     	}
     	
     	/**
          * Creates a new set with all bits set to 0.
          * @return a new, empty, set.
          */
          public BitSet( int size, EnumSet<BitSet.Bit> bits ) {
          }
     	
          /**
          * Creates a new set with all bits set to 1.
          * @return a new, full, set.
          */
     	public BitSet( int size,      
 }

 

}