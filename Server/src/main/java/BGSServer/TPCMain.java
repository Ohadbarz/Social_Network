package BGSServer;

import bgu.spl.net.api.bidi.BidiMessageEncoderDecoderImpl;
import bgu.spl.net.api.bidi.BidiMessagingProtocolImpl;
import bgu.spl.net.api.bidi.Message;
import bgu.spl.net.srv.BaseServer;
import bgu.spl.net.srv.Server;

public class TPCMain {
    public static void main(String[] args) {
        try (BaseServer<Message> server =(BaseServer<Message>) Server.threadPerClient(7777,()-> new BidiMessagingProtocolImpl(), ()->new BidiMessageEncoderDecoderImpl());){
            server.serve();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
