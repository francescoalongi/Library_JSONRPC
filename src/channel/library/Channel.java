package channel.library;

import javafx.util.Pair;

import java.io.UnsupportedEncodingException;

public interface Channel {

    void sendToClient(Object receiver, String message) throws InterruptedException;
    void sendToServer(String port, Object receiver, String message);
    Pair receiveFromClient();
    String receiveFromServer();
    void connect(String IPAddress, String port);
    void close();
    //void startDeal();
}
