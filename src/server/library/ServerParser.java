package server.library;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ServerParser {

    public String responseObjectToJSONString (ResponseObject responseObject) throws JSONException {
        // Questa funzione traduce il ResponseObject nel corrispettivo JSONObject

        JSONObject obj = new JSONObject();

        if (responseObject instanceof Error) {
            //creo una stringa JSON relativa all'oggetto di tipo Error che sarà poi mandata tramite zeroMQ

            JSONObject error = new JSONObject();                        //corrisponde alla stringa JSON di ErrorObject

            error.put("code", ((Error) responseObject).error.code);
            error.put("message", ((Error) responseObject).error.message);
            error.put("data", ((Error) responseObject).error.data);
            // se non è stato possibile ottenere l'id della richiesta, è necessario che l'id sia JSONObject.NULL
            if (responseObject.id == null)
                obj.put("id", JSONObject.NULL);
            else
                obj.put("id", responseObject.id);

            obj.put("jsonrpc", responseObject.jsonrpc);
            obj.put("error", error);


        } else if (responseObject instanceof Response) {
            //creo una stringa JSON relativa a un oggetto di tipo Response che sarà poi mandata tramite zeroMQ

            obj.put("jsonrpc", responseObject.jsonrpc);
            obj.put("id", responseObject.id);
            obj.put("result", ((Response) responseObject).result);
        }
        return obj.toString();
    }

    // Di questa funzione è stato fatto un overload in quando può prendere in ingresso un JSONArray oppure un JSONObject,
    // JSONArray se si tratta di un Batch altrimenti JSONObject se si tratta di un RequestObject
    public Map<String, Object> analyzeRequest(JSONObject json) throws JSONException {
    //Si tratta di un RequestObject

        if (json.has("jsorpc")) {
            String jsonrpc = (json.get("jsonrpc").toString());
            if (!(jsonrpc.equals("2.0"))) {
                throw new JSONException("Cannot parse RequestObject with a jsonrpc version different from 2.0.");
            }
        }
        Map<String, Object> content = new HashMap <> ();
        String id = null;
        if (json.has("id")) {
            id = (json.get("id")).toString();
            content.put("type", "request");
        } else {
            content.put("type", "notification");
        }

        boolean withParams = false;
        ArrayList<String> paramsParsed = new ArrayList<>();                 // conterrà tutti i parametri contenuti

        if (json.has("params")) {
            withParams = true;
            String params = (json.get("params").toString());


            //vengono analizzati i parametri e aggiunti in un ArrayList di stringhe
            StringBuilder tmp = new StringBuilder();
            boolean added = true;
            for (int i = 0; i < params.length(); i++) {
                if ((params.charAt(i) >= 32 && params.charAt(i) <= 33) || (params.charAt(i) >= 35 && params.charAt(i) <= 38) ||
                        (params.charAt(i) >= 40 && params.charAt(i) <= 43) || (params.charAt(i) >= 45 && params.charAt(i) <= 46)
                        || (params.charAt(i) >= 48 && params.charAt(i) <= 90) || (params.charAt(i) >= 94 && params.charAt(i) <= 122)) {
                    tmp.append(params.charAt(i));
                    added = false;
                } else if (added)
                    continue;
                else {
                    paramsParsed.add(tmp.toString());
                    added = true;
                    tmp = new StringBuilder();
                }
            }
        }

        String method = null;
        if (json.has("method")) {
            method = (json.get("method").toString());
        }

        // la stringa resultOfAnalysis conterrà il nome del metodo che il client voleva invocare sul server seguito dai parametri
        // seguirà la seguente formattazione: method;param1;param2;param3;...;...
        if (method != null) {
            StringBuilder resultOfAnalysis;
            resultOfAnalysis = new StringBuilder(method);
            if (withParams) {
                for (int i = 0; i < paramsParsed.size(); i++) {
                    resultOfAnalysis.append(";").append(paramsParsed.get(i));
                }
            }
            content.put("method", resultOfAnalysis.toString());
        } else {
            content.put("method", method);
        }
        content.put("id", id);


        // content conterrà i dati che serviranno per inviare la risposta al client corretto
        return content;
    }


    //overload della funzione precedente
    public ArrayList<Map<String, Object>> analyzeRequest(JSONArray json) throws JSONException {
        // si tratta di un Batch
        // questa funzione deve ritornare un vector di map, per ogni RequestObject contenuto nel jsonarray si deve
        // chiamare la funzione che prende in ingresso un JSONObject

        ArrayList<Map<String, Object>> contentOfBatch = new ArrayList<> ();
        for (int i = 0; i < json.length(); i++) {
                contentOfBatch.add(analyzeRequest(json.getJSONObject(i)));
        }


        return contentOfBatch;
    }

}
