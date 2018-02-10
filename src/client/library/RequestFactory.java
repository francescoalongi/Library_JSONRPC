package client.library;

import org.json.JSONException;

import java.util.ArrayList;

public class RequestFactory {

    // Questa funzione statica ci permette di creare RequestObject a richiesta in qualsiasi parte del codice, ciò che fa è molto
    // semplice: dato un tipo di richiesta da generare (nel caso client NOTIFICATION o REQUEST) esso, chiamando il costruttore
    // relativo, genera il corrispettivo oggetto
    public static RequestObject createRequestObj(Type type, String jsonrpc, String methodToBeInvoked, ArrayList<String> parameters, String Id) throws JSONException{
        if (!(jsonrpc.equals("2.0"))) {
            throw new JSONException("the jsonrpc of the request must be equals to 2.0, you ask for a" +
                    " jsonrpc version equals to " + jsonrpc);
        }

        if(type == Type.NOTIFICATION){
            return new Notification(jsonrpc,methodToBeInvoked,parameters);
        } else if(type == Type.REQUEST){
            return new Request(jsonrpc,methodToBeInvoked,parameters,Id);
        } else {
            throw new JSONException("the type you entered is not recognized in JSONRPC protocol.");
        }
    }

}

