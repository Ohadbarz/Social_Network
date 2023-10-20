package BGSServer;

import bgu.spl.net.api.bidi.BidiMessageEncoderDecoderImpl;
import bgu.spl.net.api.bidi.BidiMessagingProtocolImpl;
import bgu.spl.net.api.bidi.ServerDataBase;
import bgu.spl.net.srv.Server;

public class ReactorMain {
    public static void main(String[] args) {
        ServerDataBase.getInstance();
        Server.reactor(3,7777, ()->new BidiMessagingProtocolImpl(),()-> new BidiMessageEncoderDecoderImpl()).serve();
    }
}
