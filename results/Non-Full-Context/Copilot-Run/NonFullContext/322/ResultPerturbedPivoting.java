package org.zeromq;
 
 import java.io.DataInputStream;
 import java.io.DataOutputStream;
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.io.StringWriter;
 import java.util.ArrayDeque;
 import java.util.Collection;
 import java.util.Deque;
 import java.util.Iterator;
 
 import org.zeromq.ZMQ.Socket;
 
 import zmq.util.Draft;
 import zmq.util.function.Consumer;
 
 /**
  * The ZMsg class provides methods to send and receive multipart messages
  * across 0MQ sockets. This class provides a list-like container interface,
  * with methods to work with the overall container.  ZMsg messages are
  * composed of zero or more ZFrame objects.
  *
  * <pre>
  * // Send a simple single-frame string message on a ZMQSocket "output" socket object
  * ZMsg.newStringMsg("Hello").send(output);
  *
  * // Add several frames into one message
  * ZMsg msg = new ZMsg();
  * for (int i = 0 ; i &lt; 10 ; i++) {
  *     msg.addString("Frame" + i);
  * }
  * msg.send(output);
  *
  * // Receive message from ZMQSocket "input" socket object and iterate over frames
  * ZMsg receivedMessage = ZMsg.recvMsg(input);
  * for (ZFrame f : receivedMessage) {
  *     // Do something with frame f (of type ZFrame)
  * }
  * </pre>
  *
  * Based on <a href="http://github.com/zeromq/czmq/blob/master/src/zmsg.c">zmsg.c</a> in czmq
  *
  */
 
 public class ZMsg implements Iterable<ZFrame>, Deque<ZFrame>
 {
     /**
      * Hold internal list of ZFrame objects
      */
     private final ArrayDeque<ZFrame> frames = new ArrayDeque<>();
 
     /**
      * Class Constructor
      */
     public ZMsg()
     {
     }
 
     /**
      * Destructor.
      * Explicitly destroys all ZFrames contains in the ZMsg
      */
     public void destroy()
     {
         for (ZFrame f : frames) {
             f.destroy();
         }
         frames.clear();
     }
 
     /**
      * @return total number of bytes contained in all ZFrames in this ZMsg
      */
     public long contentSize()
     {
         long size = 0;
         for (ZFrame f : frames) {
             size += f.size();
         }
         return size;
     }
 
     /**
      * Add a String as a new ZFrame to the end of list
      * @param str
      *              String to add to list
      */
     public ZMsg addString(String str)
     {
         frames.add(new ZFrame(str));
         return this;
     }
 
     /**
      * Creates copy of this ZMsg.
      * Also duplicates all frame content.
      * @return
      *          The duplicated ZMsg object, else null if this ZMsg contains an empty frame set
      */
     public ZMsg duplicate()
     {
         if (frames.isEmpty()) {
             return null;
         }
         else {
             ZMsg msg = new ZMsg();
             for (ZFrame f : frames) {
                 msg.add(f.duplicate());
             }
             return msg;
         }
     }
 
 
/** Push the frame plus the blank frame towards the front of the message, before the 1st image. */
 public ZMsg wrap(ZFrame frame){
        frames.addFirst(frame);
        frames.addFirst(new ZFrame());
        return this;        
 }

 

}