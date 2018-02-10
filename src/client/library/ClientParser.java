package client.library;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class ClientParser {
    
    public String batchToJSONString (Batch batch) throws JSONException{
        // In questa funzione un batch viene convertito in JSONArray contenente più JSONObject

        JSONArray toSend = new JSONArray();

        // Analizzo tutti i RequestObject contenuti all'interno del batch e per ognuno genero un JSONObject che sarà
        // inserito all'interno del JSONArray
        for (int i = 0; i < batch.contentOfBatch.size(); i++) {
            JSONObject obj = new JSONObject();

            if (batch.contentOfBatch.get(i) instanceof Request) {
                //creo una stringa JSON relativa a un oggetto di tipo Request che sarà poi mandata tramite zeroMQ


                obj.put("jsonrpc", batch.contentOfBatch.get(i).jsonrpc);
                obj.put("method", batch.contentOfBatch.get(i).method);
                obj.put("params", batch.contentOfBatch.get(i).params);
                obj.put("id", ((Request) batch.contentOfBatch.get(i)).id);

            } else if (batch.contentOfBatch.get(i) instanceof Notification) {
                //creo una stringa JSON relativa a un oggetto di tipo Notification che sarà poi mandata tramite zeroMQ

                obj.put("jsonrpc", "2.0");
                obj.put("method", batch.contentOfBatch.get(i).method);
                obj.put("params", batch.contentOfBatch.get(i).params);

            }
            toSend.put(obj);
        }

        // Dopo aver creato il JSONArray lo ritorno al livello superiore come stringa (con la formattazione precisa di
        // un bacth che viene specificata nel protocollo JSONRPC)
        return toSend.toString();
    }

    public String requestObjectToJSONString (RequestObject requestObject) throws JSONException{
        JSONObject obj = new JSONObject();

        if (requestObject instanceof Request) {
            //creo una stringa JSON relativa ad un oggetto di tipo Request che sarà poi mandata tramite zeroMQ

            obj.put("jsonrpc", requestObject.jsonrpc);
            obj.put("method", requestObject.method);
            obj.put("params", requestObject.params);
            obj.put("id", ((Request) requestObject).id);


        } else if (requestObject instanceof Notification) {
            //creo una stringa JSON relativa ad un oggetto di tipo Notification che sarà poi mandata tramite zeroMQ

            obj.put("jsonrpc", requestObject.jsonrpc);
            obj.put("method", requestObject.method);
            obj.put("params", requestObject.params);
        }

        // Dopo aver creato il JSONObject lo ritorno al livello superiore tramite una stringa
        return obj.toString();
    }


    public String analyzeResponse(JSONObject responseObject, String requestId) throws JSONException {
        // Questa funzione viene chiamata quando il client una volta ricevuto il ResponseObject dal server (sotto forma
        // di stringa formattata come un JSONObject) e deve analizzarlo per capire cosa il server gli ha restituito.
        String result = "";

        // Se il responseObject è null significa che la risposta è vuota
        if (responseObject != null) {
            // se la risposta ha un id allora è una request e bisogna confrontare se l'id di ciò che si ricevuto è uguale
            // all'id di ciò che è stato mandato, se sono diversi il server ha sbagliato qualcosa e quindi l'utente viene
            // notificato.
            if (responseObject.has("id")) {
                if (!(responseObject.get("id").toString().equals(requestId)) && responseObject.get("id") != JSONObject.NULL) {
                    result = "The id of your request is different from the id of the response";
                    return result;
                }
            }
            // se la risposta ha un campo "result" allora lo preleviamo e lo restituiamo all'utente, se il ResponseObject
            // per qualche motivo è arrivato senza questo campo result, allora o è un errore oppure sono stati persi dei
            // pacchetti e quindi il messaggio è inutilizzabile, se ha il campo "error" allora è un errore e quindi l'utente
            // viene notificato di conseguenza. Se non è nemmeno un errore allora l'utente viene notificato con il messaggio
            // "The response has arrived empty"
            if (responseObject.has("result")) {
                result = responseObject.get("result").toString();
            } else if (responseObject.has("error")) {
                result = "Error code: " + ((JSONObject)responseObject.get("error")).get("code") + " ~ " +
                        ((JSONObject)responseObject.get("error")).get("message");
                if (((JSONObject)(responseObject.get("error"))).has("data")) {
                    result += ": " + ((JSONObject)responseObject.get("error")).get("data");
                }
            } else {
                result = "The response has arrived empty";
            }
        } else {
            result = "The response has arrived empty";
        }
        return result;
    }
}
