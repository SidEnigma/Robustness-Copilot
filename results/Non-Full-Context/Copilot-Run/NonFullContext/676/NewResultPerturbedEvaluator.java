package zmq.io;
 
 import zmq.Msg;
 
 public class Msgs
 {
     private Msgs()
     {
         // no possible instantiation
     }
 
 
/** Returns true if the message starts with the string provided as input, false otherwise */

public static boolean startsWith(Msg msg, String data, boolean includeLength) {
    if (includeLength) {
        return msg.size() >= data.length() && msg.getString(0, data.length()).equals(data);
    } else {
        return msg.getString(0).startsWith(data);
    }
}

}