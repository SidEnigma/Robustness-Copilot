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
 	
 
/** All the indiced featuring a bit set will be returned. */
 public static short[] allIndices(){}

 

}