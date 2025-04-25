package zmq.io;
 
 import zmq.Msg;
 
 public class Msgs
 {
     private Msgs()
     {
         // no possible instantiation
     }
 
 
/** If the message starts with a string, it's checked. */
 public static boolean startsWith(Msg msg, String data, boolean includeLength){
        if(msg.size() < data.length()){
            return false;
        }
        if(includeLength){
            return msg.size() == data.length() && msg.toString().startsWith(data);
        }
        return msg.toString().startsWith(data);     
 }

 

}