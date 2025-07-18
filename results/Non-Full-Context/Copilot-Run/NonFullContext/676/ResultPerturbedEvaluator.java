package zmq.io;
 
 import zmq.Msg;
 
 public class Msgs
 {
     private Msgs()
     {
         // no possible instantiation
     }
 
 
/** Returns true if the message starts with the string provided as input, false otherwise */
 public static boolean startsWith(Msg msg, String data, boolean includeLength){
        if(includeLength){
            if(msg.size() < data.length()){
                return false;
            }
        }
        else{
            if(msg.size() < data.length() - 1){
                return false;
            }
        }
        for(int i = 0; i < data.length(); i++){
            if(msg.data()[i] != data.charAt(i)){
                return false;
            }
        }
        return true;        
 }

 

}