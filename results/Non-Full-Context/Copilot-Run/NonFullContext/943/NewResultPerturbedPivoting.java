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
 
     /**
      * Creates a binary encoded 'picture' message to the socket (or actor), so it can be sent.
      * The arguments are encoded in a binary format that is compatible with zproto, and
      * is designed to reduce memory allocations.
      *
      * @param picture The picture argument is a string that defines the
      *                type of each argument. Supports these argument types:
      *                <table>
      *                <caption> </caption>
      *                <tr><th style="text-align:left">pattern</th><th style="text-align:left">java type</th><th style="text-align:left">zproto type</th></tr>
      *                <tr><td>1</td><td>int</td><td>type = "number" size = "1"</td></tr>
      *                <tr><td>2</td><td>int</td><td>type = "number" size = "2"</td></tr>
      *                <tr><td>4</td><td>long</td><td>type = "number" size = "3"</td></tr>
      *                <tr><td>8</td><td>long</td><td>type = "number" size = "4"</td></tr>
      *                <tr><td>s</td><td>String, 0-255 chars</td><td>type = "string"</td></tr>
      *                <tr><td>S</td><td>String, 0-2^32-1 chars</td><td>type = "longstr"</td></tr>
      *                <tr><td>b</td><td>byte[], 0-2^32-1 bytes</td><td>type = "chunk"</td></tr>
      *                <tr><td>c</td><td>byte[], 0-2^32-1 bytes</td><td>type = "chunk"</td></tr>
      *                <tr><td>f</td><td>ZFrame</td><td>type = "frame"</td></tr>
      *                <tr><td>m</td><td>ZMsg</td><td>type = "msg" <b>Has to be the last element of the picture</b></td></tr>
      *                </table>
      * @param args    Arguments according to the picture
      * @return true when it has been queued on the socket and ØMQ has assumed responsibility for the message.
      * This does not indicate that the message has been transmitted to the network.
      * @apiNote Does not change or take ownership of any arguments.
      */
     @Draft
     public ZMsg msgBinaryPicture(String picture, Object... args)
     {
         if (!BINARY_FORMAT.matcher(picture).matches()) {
             throw new ZMQException(picture + " is not in expected binary format " + BINARY_FORMAT.pattern(),
                     ZError.EPROTO);
         }
         ZMsg msg = new ZMsg();
 
         //  Pass 1: calculate total size of data frame
         int frameSize = 0;
         for (int index = 0; index < picture.length(); index++) {
             char pattern = picture.charAt(index);
             switch (pattern) {
             case '1': {
                 frameSize += 1;
                 break;
             }
             case '2': {
                 frameSize += 2;
                 break;
             }
             case '4': {
                 frameSize += 4;
                 break;
             }
             case '8': {
                 frameSize += 8;
                 break;
             }
             case 's': {
                 String string = (String) args[index];
                 frameSize += 1 + (string != null ? string.getBytes(ZMQ.CHARSET).length : 0);
                 break;
             }
             case 'S': {
                 String string = (String) args[index];
                 frameSize += 4 + (string != null ? string.getBytes(ZMQ.CHARSET).length : 0);
                 break;
             }
             case 'b':
             case 'c': {
                 byte[] block = (byte[]) args[index];
                 frameSize += 4 + block.length;
                 break;
             }
             case 'f': {
                 ZFrame frame = (ZFrame) args[index];
                 msg.add(frame);
                 break;
             }
             case 'm': {
                 ZMsg other = (ZMsg) args[index];
                 if (other == null) {
                     msg.add(new ZFrame((byte[]) null));
                 }
                 else {
                     msg.addAll(other);
                 }
                 break;
             }
             default:
                 assert (false) : "invalid picture element '" + pattern + "'";
             }
         }
 
         //  Pass 2: encode data into data frame
         ZFrame frame = new ZFrame(new byte[frameSize]);
         ZNeedle needle = new ZNeedle(frame);
         for (int index = 0; index < picture.length(); index++) {
             char pattern = picture.charAt(index);
             switch (pattern) {
             case '1': {
                 needle.putNumber1((int) args[index]);
                 break;
             }
             case '2': {
                 needle.putNumber2((int) args[index]);
                 break;
             }
             case '4': {
                 needle.putNumber4((int) args[index]);
                 break;
             }
             case '8': {
                 needle.putNumber8((long) args[index]);
                 break;
             }
             case 's': {
                 needle.putString((String) args[index]);
                 break;
             }
             case 'S': {
                 needle.putLongString((String) args[index]);
                 break;
             }
             case 'b':
             case 'c': {
                 byte[] block = (byte[]) args[index];
                 needle.putNumber4(block.length);
                 needle.putBlock(block, block.length);
                 break;
             }
             case 'f':
             case 'm':
                 break;
             default:
                 assert (false) : "invalid picture element '" + pattern + "'";
             }
         }
         msg.addFirst(frame);
         return msg;
     }
 
     @Draft
     public boolean sendBinaryPicture(Socket socket, String picture, Object... args)
     {
         return msgBinaryPicture(picture, args).send(socket);
     }
 
 
/** Receive a binary encoded 'image' message from the socket (or actor). */

public Object[] recvBinaryPicture(Socket socket, final String picture) {
    ZMsg msg = ZMsg.recvMsg(socket);
    ZFrame frame = msg.pop();
    ZNeedle needle = new ZNeedle(frame.getData());
    List<Object> argsList = new ArrayList<>();

    for (int index = 0; index < picture.length(); index++) {
        char pattern = picture.charAt(index);
        switch (pattern) {
            case '1': {
                argsList.add(needle.getNumber1());
                break;
            }
            case '2': {
                argsList.add(needle.getNumber2());
                break;
            }
            case '4': {
                argsList.add(needle.getNumber4());
                break;
            }
            case '8': {
                argsList.add(needle.getNumber8());
                break;
            }
            case 's': {
                argsList.add(needle.getString());
                break;
            }
            case 'S': {
                argsList.add(needle.getLongString());
                break;
            }
            case 'b':
            case 'c': {
                int length = needle.getNumber4();
                byte[] block = new byte[length];
                needle.getBlock(block, length);
                argsList.add(block);
                break;
            }
            case 'f': {
                ZFrame zFrame = msg.pop();
                argsList.add(zFrame);
                break;
            }
            case 'm': {
                ZMsg other = new ZMsg();
                while (msg.size() > 0) {
                    ZFrame zFrame = msg.pop();
                    other.add(zFrame);
                }
                argsList.add(other);
                break;
            }
            default:
                assert (false) : "invalid picture element '" + pattern + "'";
        }
    }

    msg.destroy();
    frame.destroy();

    return argsList.toArray();
}
 

}