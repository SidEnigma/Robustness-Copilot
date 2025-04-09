package dmg.cells.nucleus;
 
 import static com.google.common.base.Preconditions.checkArgument;
 import static com.google.common.base.Preconditions.checkState;
 import static dmg.cells.nucleus.SerializationHandler.Serializer;
 
 import com.google.common.base.Strings;
 import java.io.DataInput;
 import java.io.DataOutput;
 import java.io.IOException;
 import java.io.ObjectInputStream;
 import java.io.Serializable;
 import java.util.Objects;
 
 /**
  * Do not subclass - otherwise raw encoding in LocationMgrTunnel will break.
  *
  * @author Patrick Fuhrmann
  * @version 0.1, 15 Feb 1998
  */
 public final class CellMessage implements Cloneable, Serializable {
 
     private static final long serialVersionUID = -5559658187264201731L;
 
     /**
      * Maximum TTL adjustment in milliseconds.
      */
     private static final int TTL_BUFFER_MAXIMUM = 10000;
 
     /**
      * Maximum TTL adjustment as a fraction of TTL.
      */
     private static final float TTL_BUFFER_FRACTION = 0.10f;
 
     private CellPath _source, _destination;
     private Object _message;
     private long _creationTime;
     private long _ttl = Long.MAX_VALUE;
     private int _mode;
     /**
      * Unique Mesage ID
      */
     private UOID _umid, _lastUmid;
     private byte[] _messageStream;
     private boolean _isPersistent;
     private Object _session;
     /**
      * Indicates deserialized message format
      */
     private static final int ORIGINAL_MODE = 0;
     /**
      * Indicates serialized message format
      */
     private static final int STREAM_MODE = 1;
     private transient long _receivedAt;
 
     public CellMessage(CellAddressCore address, Serializable msg) {
         this(new CellPath(address));
         _message = msg;
     }
 
     public CellMessage(CellAddressCore address) {
         this(new CellPath(address));
     }
 
     public CellMessage(CellPath path, Serializable msg) {
         this(path.clone());
         _message = msg;
     }
 
     public CellMessage(CellPath path) {
         _source = new CellPath();
         _destination = path;
         _creationTime = System.currentTimeMillis();
         _receivedAt = _creationTime;
         _mode = ORIGINAL_MODE;
         _umid = new UOID();
         _lastUmid = _umid;
         _session = CDC.getSession();
     }
 
     public CellMessage() {
         this(new CellPath());
     }
 
     @Override
     public String toString() {
         StringBuilder sb = new StringBuilder();
         sb.append("<CM: S=").append(_source).
               append(";D=").append(_destination);
         if (_mode == ORIGINAL_MODE) {
             sb.append(";C=").
                   append(_message.getClass().getName());
             sb.append(";M=").
                   append(_message.toString());
         } else {
             sb.append(";C=Stream");
         }
 
         sb.append(";O=").append(_umid).append(";LO=").append(_lastUmid);
         if (_session != null) {
             sb.append(";SID=").append(_session);
         }
         if (_ttl < Long.MAX_VALUE) {
             sb.append(";TTL=").append(_ttl);
         }
         sb.append('>');
         return sb.toString();
     }
 
     @Override
     public int hashCode() {
         return _umid.hashCode();
     }
 
     @Override
     public boolean equals(Object obj) {
 
         if (obj == this) {
             return true;
         }
 
         if (obj == null || obj.getClass() != this.getClass()) {
             return false;
         }
 
         return ((CellMessage) obj)._umid.equals(_umid);
     }
 
     public boolean isReply() {
         return _isPersistent;
     }
 
     public UOID getUOID() {
         return _umid;
     }
 
     public UOID getLastUOID() {
         return _lastUmid;
     }
 
     public void setUOID(UOID umid) {
         _umid = umid;
     }
 
     public void setLastUOID(UOID lastUOID) {
         _lastUmid = lastUOID;
     }
 
     public Serializable getSession() {
         return (Serializable) _session;
     }
 
     public void setSession(Serializable session) {
         _session = session;
     }
 
     public void setTtl(long ttl) {
         _ttl = ttl;
         _receivedAt = System.currentTimeMillis();
     }
 
     public long getTtl() {
         return _ttl;
     }
 
     public CellAddressCore getSourceAddress() {
         return _source.getSourceAddress();
     }
 
     public CellPath getDestinationPath() {
         return _destination;
     }
 
     public CellPath getSourcePath() {
         return _source;
     }
 
     public Serializable getMessageObject() {
         return (Serializable) _message;
     }
 
     public void setMessageObject(Serializable obj) {
         checkState(_mode == ORIGINAL_MODE);
         _message = obj;
     }
 
     public void revertDirection() {
         checkState(!_source.getSourceAddress().isDomainAddress(),
               "Cannot return envelope to a domain address.");
         _destination = _source.revert();
         _source = new CellPath();
         _lastUmid = _umid;
         _umid = new UOID();
         _isPersistent = true;
     }
 
     public boolean isFinalDestination() {
         return _destination.isFinalDestination();
     }
 
     public boolean isFirstDestination() {
         return _destination.isFirstDestination();
     }
 
     public boolean nextDestination() {
         return _destination.next();
     }
 
     //
     // package methods
     //
     boolean isStreamMode() {
         return _mode == STREAM_MODE;
     }
 
 
/** The method only uses the message stream as a reference. */
 public CellMessage clone(){
        CellMessage msg = new CellMessage();
        msg._source = _source.clone();
        msg._destination = _destination.clone();
        msg._message = _message;
        msg._creationTime = _creationTime;
        msg._ttl = _ttl;
        msg._mode = _mode;
        msg._umid = _umid;
        msg._lastUmid = _lastUmid;
        msg._messageStream = _messageStream;
        msg._isPersistent = _isPersistent;
        msg._session = _session;
        msg._receivedAt = _receivedAt;
        return msg;
    }
    
        public void setMessageStream(byte[] stream) {
            checkState(_mode == ORIGINAL_MODE);
            _messageStream = stream;
        }
    
        public byte[] getMessageStream() {
            return _messageStream;
        }
    
        public void setIsPersistent(boolean isPersistent) {
            _isPersistent = isPersistent;
        }
    
        public boolean isPersistent() {
            return _isPersistent;
        }
    
        public void setMode(int mode) {
            _mode = mode;
        }
    
        public int getMode() {
            return _mode;
        }
    
        public void setCreationTime(long time) {
            _creationTime = time;
        }
    
        public long getCreationTime() {
            return _creationTime;
        }
    
        public void setReceivedAt(long time) {
            _receivedAt = time;
        }
    
        public long getReceivedAt() {
            return _receivedAt;
        }
    
        public void setSource(CellPath source) {
            _source = source;
        }
    
        public void setDestination(CellPath destination) {
            _destination = destination;
        }
    
        public void setMessage(Serializable message) {
            _message = message;
        }
    
        public CellAddressCore getDestinationAddress() {
            return _destination.getDestinationAddress       
 }

 

}