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
 
 
/** It creates a binary encoded image of the message, compatible with zproto and optimized in memory allocation, to the socket for sending it. */

public ZMsg msgBinaryPicture(String picture, Object... args) {
    ZMsg msg = new ZMsg();
    msg.add(picture);

    for (Object arg : args) {
        msg.add(arg.toString());
    }

    return msg;
}
 

}