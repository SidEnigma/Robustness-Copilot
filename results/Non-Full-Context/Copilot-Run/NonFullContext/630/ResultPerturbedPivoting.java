/**
  * Copyright 2007 the dCache team
  */
 package org.dcache.services.info.base;
 
 import java.util.ArrayList;
 import java.util.List;
 
 
 /**
  * StatePath provides a representation of a value's location within the dCache State.  A path
  * consists of an ordered list of path elements, each element is a String.
  * <p>
  * In addition to the constructor, various methods exist to create derived paths: StatePaths that
  * are, in some sense, relative to an existing StatePath; for examples of these, see the
  * <tt>newChild()</tt> and
  * <tt>childPath()</tt> methods.
  * <p>
  * The constructor provides an easy method of creating a complex StatePath. It will parse the String
  * and split it at dot boundaries, forming the path.  Some paths may have elements that contain
  * dots.  To construct corresponding StatePath representations, use the <tt>newChild()</tt> method.
  *
  * @author Paul Millar <paul.millar@desy.de>
  */
 public class StatePath {
 
     private static final int NULL_ELEMENT_HASH = 0xDEADBEAF;
 
     protected final List<String> _elements;
     private int _myHashCode;
     private boolean _haveHashCode;
     private String _toString;
 
 
     /**
      * Parse a dot-separated path to build a StatePath
      *
      * @param path the path, as an ordered list of path elements, each element separated by a dot.
      * @return the corresponding StatePath.
      */
     public static StatePath parsePath(String path) {
         String elements[] = path.split("\\.");
         return new StatePath(elements);
     }
 
     /**
      * Create a new StatePath that duplicates an existing one.
      *
      * @param path the StatePath to copy.
      */
     protected StatePath(StatePath path) {
         _elements = new ArrayList<>(path._elements);
     }
 
     /**
      * Create a new StatePath when given a List of path elements.
      *
      * @param pathElements The List of path elements from which to construct the state path.
      * @return the corresponding StatePath
      */
     public static StatePath buildFromList(List<String> pathElements) {
         return new StatePath(pathElements, pathElements.size());
     }
 
 
     /**
      * Create a new StatePath based on a List of path elements.
      *
      * @param elements
      */
     private StatePath(List<String> elements, int elementCount) {
         _elements = new ArrayList<>(elementCount);
 
         for (String element : elements) {
             _elements.add(element.intern());
         }
     }
 
     /**
      * Build a new StatePath based on an array of elements.
      *
      * @param elements the path elements, in order.
      */
     protected StatePath(String[] elements) {
         _elements = new ArrayList<>(elements.length);
 
         for (String element : elements) {
             _elements.add(element.intern());
         }
     }
 
     /**
      * Provide a new StatePath with a single path element.  The result is the same as new
      * StatePath().newChild(name);
      *
      * @param name: the name of the path element.
      */
     public StatePath(String element) {
         _elements = new ArrayList<>(1);
         _elements.add(element != null ? element.intern() : null);
     }
 
 
     /**
      * Calculate the hash code and store it for later quick reference.
      */
     void calcHashCode() {
         int code = 0;
         int elementCount = 0;
 
         for (String element : _elements) {
             int stringHash = 0;
 
             if (element == null) {
                 stringHash = NULL_ELEMENT_HASH;
             } else {
                 // Since Java's String hashCode is so poor, spice it up a little.
                 byte bytes[] = element.getBytes();
 
                 int len = bytes.length > 10 ? 10 : bytes.length; // limit length
                 for (int i = 0; i < len; i++) {
                     stringHash ^= (bytes[i]) << (i * 5 + elementCount) % 24;
                 }
             }
 
             code ^= stringHash;
             elementCount++;
         }
 
         _myHashCode = code;
         _haveHashCode = true;
     }
 
 
 
/** Check if another path points to the same location. */
 public boolean equals(Object otherObject){
        if (otherObject == null) {
            return false;
        }
    
        if (otherObject instanceof StatePath) {
            StatePath otherPath = (StatePath) otherObject;
    
            if (otherPath._elements.size() != _elements.size()) {
                return false;
            }
    
            for (int i = 0; i < _elements.size(); i++) {
                if (!_elements.get(i).equals(otherPath._elements.get(i))) {
                    return false;
                }
            }
    
            return true;
        }
    
        return false;
    }
    
        /**
        * Calculate the hash code and store it for later quick reference.
        */
        public int hashCode() {
            if (!_haveHashCode) {
                calcHashCode();
            }
    
            return _myHashCode;
        }
    
        /**
        * Return the path elements as a List.
        *
        * @return the path elements.
        */
        public List<String> getElements() {
            return _elements;
        }
    
        /**
        * Return the path elements as an array.
        *
        * @return the path elements.
        */
        public String[] getElementsArray() {
            return _elements.toArray(new String[_elements.size()]);
        }
    
        /**
        * Return the path elements as a String.
        *
        * @return the path elements.
        */
        public String toString() {
            if (_toString == null) {
                StringBuilder sb = new StringBuilder();
    
                for (String element : _elements) {
                    if (sb.length() > 0) {
                        sb.append('.');
                    }
    
                    sb.append(element);
                }
    
                _toString = sb.toString();
            }
    
            return _toString;
        }
    
        /**
        * Return the path elements as a String.
        *
        * @return       
 }

 

}