package channel.library;

import javafx.util.Pair;
import org.omg.IOP.Encoding;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.concurrent.Semaphore;

import static org.zeromq.ZMQ.*;

public class ZMQChannel implements Channel {

    private ZMQ.Socket canale;
    private ZMQ.Context ctx;

    // essendo che tutti i thread lavorano sulla stessa risorsa (la stessa socket che viene istanziata solo una volta quando viene
    // istanziato il Server) per gestire il parallelismo uso dei semafori
    private final Semaphore used = new Semaphore(1,true);

    public ZMQChannel (String port, String type) {

        // Quando viene istanziato un oggetto di tipo ZMQChannel (gestito dall'interfaccia Channel) esso, nel nostro progetto,
        // può essere di due tipi: ROUTER o DEALER, il ROUTER corrisponde al SERVER mentre il DEALER corrisponde al CLIENT

        if (type.equals("SERVER")) {
            ZMQ.Context ctx = ZMQ.context(1);
            ZMQ.Socket socket = ctx.socket(ROUTER);
            socket.bind("tcp://*:" + port);
            this.ctx = ctx;
            this.canale = socket;
        } else if (type.equals("CLIENT")){
            ZMQ.Context ctx = ZMQ.context(1);
            ZMQ.Socket socket = ctx.socket(DEALER);
            this.ctx = ctx;
            this.canale = socket;
        }
    }

    public Pair receiveFromClient() {
        ZMsg inMsg = ZMsg.recvMsg(this.canale);
        ZFrame identity = inMsg.pop();
        ZFrame message = inMsg.pop();
        return new Pair(identity, message);

    }

    public void sendToClient(Object receiver, String message) throws InterruptedException {

        // questa istruzione mi garantisce che questo blocco di codice venga eseguito da un solo thread per volta
        used.acquire();

        ZFrame identity = (ZFrame) receiver;
        ZFrame content = new ZFrame(message.getBytes(ZMQ.CHARSET));
        identity.send(this.canale, ZFrame.REUSE + ZFrame.MORE);
        content.send(this.canale, ZFrame.REUSE);

        used.release();
    }

    public void sendToServer(String port, Object receiver, String message) throws IllegalArgumentException {

        // prima di effettuare la send sul ZMsg mi connetto al receiver, se il receiver non è un IPAddress valido
        // viene lanciata un'eccezione gestita al livello superiore
        this.connect(receiver.toString(), port);

        ZMsg outMsg = new ZMsg();
        outMsg.add(message);
        outMsg.send(this.canale);
        outMsg.destroy();

    }

    public void connect (String IPAddress, String port) throws IllegalArgumentException {

        this.canale.connect("tcp://" + IPAddress + ":" + port);

    }

    public String receiveFromServer() {
        // questa funzione setta anche un timeout sulla socket che riceve il messaggio (in questo caso di 5 secondi)
        // cosi da non lasciare il client aspettare all'infinito se il server non risponde entro 5 secondi

        int originalTimeout = this.canale.getReceiveTimeOut();
        this.canale.setReceiveTimeOut(5000);
        ZMsg resp = ZMsg.recvMsg(this.canale);
        this.canale.setReceiveTimeOut(originalTimeout);

        // se si riesce ad ottenere qualcosa dal server come risposta (quindi resp non è nullo) allora result conterrà
        // un messaggio, altrimenti sarà uguale a una stringa vuota
        ZFrame frame;
        String result = "";
        if (resp != null && !(resp.isEmpty())) {
            frame = resp.pop();
            resp.destroy();
            result = frame.toString();
        }
        return result;
    }

    public void close() {

        // la setLinger viene chiamata per settare la zmq.linger, di default è -1 ma se viene lasciata cosi allora
        // avremo un infinito loop che non farà chiudere il context se la socket è in uso.
        this.canale.setLinger(10);
        this.canale.close();
        this.ctx.close();

    }

}
