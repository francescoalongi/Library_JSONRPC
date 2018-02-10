package client.library;

import channel.library.Channel;
import channel.library.ZMQChannel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class Client {

    private Channel channel;
    private String port;
    private ClientParser clientParser;

    public Client (String port){
        channel = new ZMQChannel(port, "CLIENT");
        this.port = port;
        this.clientParser = new ClientParser();

    }

    public String callMethod(String type, String idRequest, String method, ArrayList<String> params, Object receiver) {
        // questo metodo è quello che verrà chiamato a livello applicazione, ciò che fa è semplicemente chiamare la RequestFactory
        // passandogli il tipo giusto di richiesta che si vuole mandare e passa il controllo alla sendRequestObject, che restituirà
        // il risultato del metodo invocato

        String result;
        switch (type) {
            case "request": {
                RequestObject requestObject;
                try {
                    requestObject = RequestFactory.createRequestObj(Type.REQUEST, "2.0", method, params, idRequest);
                } catch (JSONException e) {
                    System.out.println("Error during the creation of the RequestObject because " + e.getMessage());
                    return null;
                }
                result = this.sendRequestObject(requestObject, receiver);
                break;
            }
            case "notification": {
                RequestObject requestObject;
                try {
                    requestObject = RequestFactory.createRequestObj(Type.NOTIFICATION, "2.0", method, params, idRequest);
                } catch (JSONException e) {
                    System.out.println("Error during the creation of the RequestObject because " + e.getMessage());
                    return null;
                }
                result = this.sendRequestObject(requestObject, receiver);
                break;
            }
            default:
                result = "Type of request is not recognized by the protocol JSONRPC.";
                break;
        }
        return result;
    }



    private String sendRequestObject (RequestObject requestObject, Object receiver){
        // questa funzione invia un RequestObject parsandolo prima in un JSONObject grazie alla funzione
        // requestObjectToJSONString, se quest'ultima funzione solleva un eccezione significa che la RequestObject,
        // per qualche motivo, è stata generata male e quindi non rispetta gli standard di JSONRPC

        String result = "";
        String msgToSend;
        try {
            msgToSend = clientParser.requestObjectToJSONString(requestObject);
        } catch (JSONException e) {
            result = "Client parsing error: there were some errors during parsing.";
            return result;
        }

        // in questo try catch viene invocato sendToServer, se questa funzione solleva un eccezione significa che
        // o la porta inserita in fase di istaziazione del client è errata oppure l'IPAddress a cui si vuole inviare
        // il messaggio non esiste o non è valido
        try {
            this.channel.sendToServer(this.port, receiver, msgToSend);
        } catch (IllegalArgumentException e) {
            result = "The connection has failed because either the IP address " + receiver.toString() + " or the port " +
                    this.port + " is wrong";
            return result;
        }

        // se il RequestObject che si vuole mandare è in particolare una Request allora il client si aspetta una risposta,
        // altrimenti notifica l'utente che la notifica è stata inviata correttamente
        if (requestObject instanceof Request) {

            // Come spiegato nella classe ZMQChannel al metodo receiveFromServer(), se tmpResult è uguale a "" allora
            // non è stato possibile stabilire una connessione con il server quindi notificherà al client che il server
            // non è raggiungibile
            String tmpResult = this.channel.receiveFromServer();
            if (!tmpResult.equals("")) {
                try {
                    JSONObject responseObject = new JSONObject(tmpResult);
                    result = clientParser.analyzeResponse(responseObject, ((Request) requestObject).id);

                } catch (JSONException e) {
                    result = "The response obtained for the request number " + ((Request) requestObject).id + " has encountered parsing error.";
                    return result;
                }
            } else result = "Server unreacheable.";

        } else if (requestObject instanceof Notification) {
            result = "The notification has been sended to the server.";
        }
        return result;
    }

    public ArrayList<String> callMultipleMethod(ArrayList<String> typesInString, ArrayList<String> methods, ArrayList<ArrayList<String>> paramsOfEachMethod, ArrayList<String> ids, Object receiver) {
        // Questo metodo viene chiamato quando si vogliono chiamare più metodi con la stessa richiesta (quindi un batch) che
        // non è altro che un vettore di RequestObject

        ArrayList<RequestObject> requests = new ArrayList<>();
        ArrayList<String> result = new ArrayList<>();

        // se le dimensioni dei vari vettori presi in ingresso sono tutte diverse c'è stato un errore dell'immissione dei vari parametri
        // e quindi l'invio del batch non può continuare
        if (typesInString.size() == methods.size() && methods.size() == paramsOfEachMethod.size() && paramsOfEachMethod.size() == ids.size()){

            ArrayList<Type> types = new ArrayList<>();
            for (int i = 0; i < typesInString.size(); i++) {
                switch (typesInString.get(i)) {
                    case "request":
                        types.add(Type.REQUEST);
                        break;
                    case "notification":
                        types.add(Type.NOTIFICATION);
                        break;
                    default:
                        System.out.println("Type of the request number " + (i + 1) + " contained in the batch is not valid, it is not possible to perform the operation.");
                        return result;
                }
            }

            for (int i = 0; i < methods.size(); i++) {
                try {
                    requests.add(RequestFactory.createRequestObj(types.get(i), "2.0", methods.get(i), paramsOfEachMethod.get(i), ids.get(i)));
                } catch (JSONException e) {
                    System.out.println("Error during the creation of the RequestObject because " + e.getMessage());
                    return null;
                }
            }

            Batch batch = new Batch(requests);
            result = this.sendBatch(batch, receiver);
        } else {
            System.out.println("The number of type, methods, parameters and ids must be the same.");
        }
        return result;
    }

    private ArrayList<String> sendBatch (Batch batch, Object receiver) {
        // in questa funzione ci serviamo del clientParser per tradurre il Batch in String in maniera tale da poterlo
        // inviare al server

        ArrayList<String> result = new ArrayList<>();
        String msgToSend;
        // se la batchToJSONString ha sollevato un eccezione allora significa che per qualche motivo l'utente ha fatto
        // produrre un tipo di batch non riconosciuto dal protocollo JSONRPC
        try {
            msgToSend = clientParser.batchToJSONString(batch);
        } catch (JSONException e) {
            System.out.println("Client parsing error: the message you want to send does not respect JSONRPC protocol.");
            return result;
        }

        // se la sendToServer fallisce significa che o l'IPAddress inserito non è valido o non esiste oppure la port inserita
        // in fase di istanziazione del Client non è valida
        try {
            this.channel.sendToServer(this.port, receiver, msgToSend);
        } catch (IllegalArgumentException  e) {
            System.out.println("The connection has failed because either the IP address " + receiver.toString() + " or the port " +
                    this.port + " is wrong");
            return result;
        }


        String resultOfSingularRequest;
        // A questo punto, se si manda un Batch con x RequestObject di cui y sono Request e z sono Notification, ci si
        // aspetta di ricevere y Response (o Error). Questo è quello che succede in questa parte di codice, se la posizione
        // i-esima dell'ArrayList contenente le RequestObject era una Request allora ci mettiamo in attesa di ricevere la
        // corrispondente Response (o Error) altrimenti andiamo avanti ad aspettare (se necessario) la risposta del RequestObject
        // immediatamente successivo a quello precedente
        for (int i = 0; i < batch.contentOfBatch.size(); i++) {
            if (batch.contentOfBatch.get(i) instanceof Request) {
                String tmpResult = this.channel.receiveFromServer();
                // Come spiegato nella classe ZMQChannel al metodo receiveFromServer(), se tmpResult è uguale a "" allora
                // non è stato possibile stabilire una connessione con il server quindi notificherà al client che il server
                // non è raggiungibile
                if (!(tmpResult.equals(""))) {
                    try {
                        JSONObject JSONStringOfSingularRequest = new JSONObject(tmpResult);
                        resultOfSingularRequest = clientParser.analyzeResponse(JSONStringOfSingularRequest, ((Request) batch.contentOfBatch.get(i)).id);

                    } catch (JSONException e) {
                        resultOfSingularRequest = "The response obtained for the request number " + ((Request) batch.contentOfBatch.get(i)).id + " has encountered parsing error";
                    }
                } else {
                    resultOfSingularRequest = "Server unreachable";
                }
                result.add(resultOfSingularRequest);

            } else result.add("The notification has been sended to the server.");
        }
        return result;
    }

    public void close(){
        this.channel.close();
    }
}
