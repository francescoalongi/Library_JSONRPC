package server.library;

import org.json.JSONException;

public class ResponseFactory {

    // Questa funzione statica ci permette di creare ResponseObject a richiesta in qualsiasi parte del codice, ciò che fa è molto
    // semplice: dato un tipo di richiesta da generare (nel caso server ERROR o RESPONSE) esso, chiamando il costruttore
    // relativo, genera il corrispettivo oggetto
    public static ResponseObject createResponseObj (Type type, String jsonrpc, String id, String result, int code, String message, String data) throws JSONException {
        if (!(jsonrpc.equals("2.0"))) {
            throw new JSONException("the jsonrpc of the response must be equals to 2.0, you ask for a jsonrpc" +
                    " version equals to " + String.valueOf(jsonrpc));
        }

        if (type == Type.RESPONSE) {
            return new Response(result, jsonrpc, id);

        } else if (type == Type.ERROR) {
            return new Error(code,message,data,id,jsonrpc);

        } else{
            System.out.println("Errore nella creazione del Request Object");
            return null;
        }

    }
}
