package zmq.io;
 
 import zmq.Msg;
 
 public class Msgs
 {
     private Msgs()
     {
         // no possible instantiation
     }
 
 
/** If the message starts with a string, it's checked. */

public static boolean startsWith(Msg msg, String data, boolean includeLength) {
    if (msg == null || data == null) {
        return false;
    }

    byte[] msgData = msg.getData();
    byte[] dataBytes = data.getBytes();

    if (includeLength) {
        if (msgData.length < dataBytes.length + 4) {
            return false;
        }

        for (int i = 0; i < dataBytes.length; i++) {
            if (msgData[i + 4] != dataBytes[i]) {
                return false;
            }
        }
    } else {
        if (msgData.length < dataBytes.length) {
            return false;
        }

        for (int i = 0; i < dataBytes.length; i++) {
            if (msgData[i] != dataBytes[i]) {
                return false;
            }
        }
    }

    return true;
}

}