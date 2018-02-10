package server.library;

import channel.library.*;
import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public class Server {

    private Channel channel;
    private ServerParser serverParser;

    public Server(String port) {
        // quando viene istanziato un server, esso istanzierà una socket su cui si metterà in ascolto
        this.channel = new ZMQChannel(port, "SERVER");
        this.serverParser = new ServerParser();
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void start(Object objectWithMethodsToCall) {
        // L'oggetto che viene passato a questa funzione è l'oggetto su cui si vuole che il server chiami i metodi, quindi
        // una volta istanziato il server con il suo costruttore non è ancora stato definito su quale oggetto chiamare i
        // metodi fin quando non viene chiamata questa funzione

        ArrayList<Map<String, Object>> msg;
        while (true) {

            // con getCall() si ricevono i messaggi inviati dal client e, parsati in modo opportuno, vengono messi tutti
            // in un ArrayList di Map<String, Object>, questo perchè il server non sa se ciò che sta per ricevere è un batch
            // (quindi un ArrayList di mappe) oppure un RequestObject (quindi una singola mappa), per unificare la ricezione
            // dei messaggi in un unica funzione è stato preferito restituire sempre un ArrayList di mappe e se la dimensione
            // dell'array è uguale a 1 allora il server ha ricevuto un RequestObject, altrimenti un Batch.
            // msg conterrà il tipo/i tipi del/dei RequestObject identificato/i da "type"
            // metodo/i e parametri identificati da "method"
            // identità del client identificato da "identity"
            // id del/dei messaggio/i (id in JSON-RPC) identificato/i da "id"

            msg = this.getCall();

            if (msg != null) {
                if (msg.size() == 1) {
                    //si tratta di un RequestObject
                    MethodsCaller thread = new MethodsCaller(msg.get(0), this, objectWithMethodsToCall);
                    thread.start();

                } else {
                    // è un batch
                    MethodsCaller thread = new MethodsCaller(msg, this, objectWithMethodsToCall);
                    thread.start();
                }
            }
        }
    }

    private ArrayList<Map<String, Object>> getCall () {
        // con questo metodo il server rimane in ascolto per la prossima richiesta da ricevere da cui ne estrapolerà
        // i dati per poi lavorarli con un thread (istanziato dalla classe MethodsCaller)

        // il server riceve le richieste dal client e le analizza come pair (<identità, messaggio>) l'identità contiene
        // l'indirizzo IP a cui il server manderà il ResponseObject
        Pair receivedFromClient = this.channel.receiveFromClient();

        // qui il server analizza il messaggio contenuto nella value del Pair ottenuto poc'anzi e inizialmente si suppone
        // che il messaggio non sia un Batch settando questo boolean a false se dopo analisi risulta essere un batch questa
        // variabile verrà settata a true
        boolean batch = false;
        Map <String, Object> contentOfRequestObject;
        ArrayList<Map<String,Object>> contentOfBatch;
        ArrayList<Map<String,Object>> contentOfMsg = new ArrayList<>();

        JSONObject requestToAnalyze = new JSONObject();
        JSONArray batchToAnalyze = new JSONArray();
        try {
            // verifichiamo se è un RequestObject valido, se lo è il costruttore del JSONObject non avrà problemi a generare
            // il relativo JSONObject
            requestToAnalyze = new JSONObject(receivedFromClient.getValue().toString());

        } catch (JSONException e) {

            try  {
                // se è un RequestObject invalido è probabile che sia un batch valido
                batchToAnalyze = new JSONArray(receivedFromClient.getValue().toString());
                batch = true;

            } catch (JSONException e1) {
                // se non è ne un RequestObject valido ne un Batch valido mandiamo un messaggio di errore
                this.sendError("-32600", "Invalid Request", "The JSON sent is not a valid Request object or Batch.", null, receivedFromClient.getKey());
                return null;
            }
        }

        // A questo punto sappiamo se è un batch oppure un RequestObject, procediamo quindi all'analisi differenziando i due casi
        if (!batch) {
            try {
                contentOfRequestObject = serverParser.analyzeRequest(requestToAnalyze);
            } catch (JSONException e) {
                this.sendError("-32700", "Parse error", "an error occurred on the server while parsing the JSON text.", null, receivedFromClient.getKey());
                return null;
            }

            if (contentOfRequestObject != null) {
                contentOfRequestObject.put("identity", receivedFromClient.getKey());
            }
            contentOfMsg.add(contentOfRequestObject);

        } else {
            try {
                contentOfBatch = serverParser.analyzeRequest(batchToAnalyze);
            } catch (JSONException e) {
                this.sendError("-32700", "Parse error", "An error occurred on the server while parsing the JSON text.", null, receivedFromClient.getKey());
                return null;
            }
            for (int i = 0; i < contentOfBatch.size(); i++) {
                contentOfBatch.get(i).put("identity", receivedFromClient.getKey());
            }
            contentOfMsg = contentOfBatch;
        }

        return contentOfMsg;
    }

    void sendResult(String result, String id, Object identity) {
        // Quando l'analisi e l'invocazione del metodo sono state completate con successo, allora il server manda il risultato del
        // metodo invocato

        ResponseObject response = null;
        try {
            response = ResponseFactory.createResponseObj(Type.RESPONSE, "2.0", id, result, -1, null, null);
        } catch (JSONException e) {
            System.out.println("Error during the creation of the ResponseObject because " + e.getMessage());
            return;
        }
        this.sendResponseObject(response, identity);

    }

    void sendError(String code, String message, String data, String id, Object identity) {
        // Se invece l'analisi oppure l'invocazione del metodo non sono andate a buon fine il server manda un errore con le
        // info necessarie
        int _code = Integer.parseInt(code);
        ResponseObject error = null;
        try {
            error = ResponseFactory.createResponseObj(Type.ERROR, "2.0", id, null, _code, message, data);
        } catch (JSONException e) {
            System.out.println("Error during the creation of the ResponseObject because " + e.getMessage());
            return;
        }
        this.sendResponseObject(error, identity);

    }

    private void sendResponseObject (ResponseObject responseObject, Object identity) {
        // Questa funzione prende il ResponseObject, lo parsa in una stringa e lo invia al destinatario

        String msgToSend;
        try {
            msgToSend = serverParser.responseObjectToJSONString(responseObject);
        } catch (JSONException e) {
            System.out.println("Error during parsing from ResponseObject to JSONString");
            return;
        }

        try {
            this.channel.sendToClient(identity, msgToSend);
        } catch (InterruptedException e) {
            System.out.println("Error in handling multithread");
        }
    }

    public void close() {
        this.channel.close();
    }
}
