package zmq.io;
 
 import zmq.Msg;
 
 public class Msgs
 {
     private Msgs()
     {
         // no possible instantiation
     }
 
 
/** Checks whether the message starts with the given string. */
 public static boolean startsWith(Msg msg, String data, boolean includeLength){
        if (includeLength) {
            if (msg.size() < data.length())
                return false;
        } else {
            if (msg.size() < data.length() + 1)
                return false;
        }
        byte[] buf = msg.data();
        for (int i = 0; i < data.length(); i++) {
            if (buf[i] != data.charAt(i))
                return false;
        }
        return true;        
 }

 

}