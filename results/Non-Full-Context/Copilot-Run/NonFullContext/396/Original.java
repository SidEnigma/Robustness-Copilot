package org.zeromq.proto;
 
 import java.util.regex.Pattern;
 
 import org.zeromq.ZFrame;
 import org.zeromq.ZMQ;
 import org.zeromq.ZMQ.Socket;
 import org.zeromq.ZMQException;
 import org.zeromq.ZMsg;
 
 import zmq.ZError;
 import zmq.util.Draft;
 
 /**
  * De/serialization of data within a message.
  *
  * This is a DRAFT class, and may change without notice.
  */
 @Draft
 public class ZPicture
 {
     private static final Pattern FORMAT        = Pattern.compile("[i1248sbcfz]*m?");
     private static final Pattern BINARY_FORMAT = Pattern.compile("[1248sSbcf]*m?");
 
 
/** Creates a binary encoded 'picture' message to the socket (or actor), so it can be sent. */
 public ZMsg msgBinaryPicture(String picture, Object... args){}

 

}