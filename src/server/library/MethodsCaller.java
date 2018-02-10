package server.library;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

public class MethodsCaller extends Thread {
    // Questa classe è fondamentale: essa viene usata per istanziare un oggetto che analizzerà il messaggio ottenuto dal client.
    // Come si vede ha due costruttori, questo per il semplice motivo che l'analisi e la gestione dei Batch è completamente
    // differente da quella dei RequestObject, quindi quando si vuole analizzare un batch viene chiamato il costruttore che
    // ha come primo parametro di ingresso un ArrayList di Map <String, Object> mentre quando si vuole analizzare un RequestObject
    // viene chiamato il costruttore che ha come primo parametro semplicemente la Map<String,Object>.
    // Il motivo dell'ArrayList è dovuto al fatto che un Batch è trattato come un vettore di RequestObject (che in questa
    // implementazione del protocollo JSONRPC vengono analizzati dal server come Map di <String,Object>).
    // Avremo quindi un attributo della classe chiamato batchMsg, esso servirà a far agire MethodsCaller per un batch oppure
    // per RequestObject.

    private ArrayList<Map<String, Object>> batch;
    private Map<String, Object> requestObject;
    private Server server;
    private Object objectWithMethodsToCall;
    private boolean batchMsg;

    MethodsCaller(Map<String, Object> msg, Server server, Object objectWithMethodsToCall) {
        this.requestObject = msg;
        this.server = server;
        this.objectWithMethodsToCall = objectWithMethodsToCall;
        this.batchMsg = false;
    }
    MethodsCaller (ArrayList<Map<String, Object>> msg, Server server, Object objectWithMethodsToCall) {
        this.batch = msg;
        this.server = server;
        this.objectWithMethodsToCall = objectWithMethodsToCall;
        this.batchMsg = true;
    }

    public void run() {

        if (!batchMsg) {

            this.callInternalMethod(requestObject);

        } else {

            // Se la richiesta è un batch allora dobbiamo analizzare richiesta per richiesta tutto l'arrayList batch
            Map <String,Object> singleRequest;
            for (int i = 0; i < batch.size(); i++) {
                singleRequest = batch.get(i);
                this.callInternalMethod(singleRequest);
            }
        }
    }

    private void callInternalMethod(Map<String, Object> msg) {
        // Questa funzione svolge le operazioni richieste dal RequestObject, invoca quindi il metodo richiesto dal client
        // passando i giusti parametri.

        String message;
        String id;
        try {
            message = msg.get("method").toString();
        } catch (NullPointerException e) {

            try {
                id = msg.get("id").toString();
                this.server.sendError("-32600","Invalid Request", null, id, msg.get("identity"));
            } catch (NullPointerException e1 ){
                this.server.sendError("-32600","Invalid Request", null, null, msg.get("identity"));
            }
            return;
        }


        // in questa porzione di codice viene effettuato il parsing della stringa contenuta in method (in realtà il campo
        // method della mappa msg contiene sia il nome del metodo che i vari parametri).
        // vengono quindi messi all'interno della stringa methodToBeInvoked il nome del metodo, e dentro l'ArrayList params
        // gli eventuali parametri.
        ArrayList<String> params = new ArrayList<>();
        StringBuilder methodToBeInvoked = new StringBuilder();
        StringBuilder tmp = new StringBuilder();
        boolean firstWord = true;
        for (int i = 0; i < message.length(); i++) {
            if (i == message.length() - 1) {
                if (firstWord) {
                    methodToBeInvoked.append(message.charAt(i));
                    break;
                } else {
                    tmp.append(message.charAt(i));
                    params.add(tmp.toString());
                    break;
                }
            } else if ((message.charAt(i) == ';' && !firstWord)) {
                params.add(tmp.toString());
                tmp = new StringBuilder();
            } else if (message.charAt(i) == ';' && firstWord) {
                firstWord = false;
                tmp = new StringBuilder();
            } else {
                if (firstWord) methodToBeInvoked.append(message.charAt(i));
                if (!firstWord) tmp.append(message.charAt(i));
            }
        }

        // Per invocare i metodi di una classe dall'esterno viene usata la java reflection, infatti essa ci permetterà di
        // invocare i metodi di una classe esterna e quindi di ottenere il risultato dal metodo stesso.
        Method method;

        // Essendo che il metodo getDeclaredMethod() necessita del metodo da invocare e di un' array di tipo Class con i tipi
        // dei parametri che il metodo vuole in ingresso, con questo ciclo mi creo l'array richiesto.
        Class[] paramsType = new Class[params.size()];
        for (int i = 0; i < params.size(); i++) {
            paramsType[i] = String.class;
        }

        // il metodo invoke() invece necessita sempre del metodo da invocare e di un Array di Object con tutti gli oggetti
        // da passare come parametri, quindi istanzio un array di Object in cui inserisco i parametri contenuti nell'
        // ArrayList params.
        Object[] objectsParams = params.toArray();
        String result = "";
        boolean success = false;
        try {
            // utilizzo la java reflection per invocare il metodo e ottenerne il risultato
            method = objectWithMethodsToCall.getClass().getDeclaredMethod(methodToBeInvoked.toString(), paramsType);
            try {
                result = (method.invoke(objectWithMethodsToCall, objectsParams)).toString();
            } catch (NullPointerException e){
                result = "nothing to return";
            }
            success = true;

        } catch (IllegalAccessException e) {

            // questa eccezione viene sollevata quando si vuole accedere ad un metodo che non è accessibile dall'oggetto
            // che lo sta chiamando, chiaramente questa eccezione viene notificata all'utente ma solo se quest'ultimo ha
            // inviato una Request e non una Notification
            if (msg.get("type").equals("request")) {
                this.server.sendError("-32001", "Illegal access", "access is not allowed in the method named "
                        +  methodToBeInvoked, msg.get("id").toString(), msg.get("identity"));
            }
        } catch (NoSuchMethodException e) {

            // questa eccezione viene sollevata quando il metodo non esiste oppure il numero di parametri è sbagliato
            if (msg.get("type").equals("request")) {
                this.server.sendError("-32601", "Method not found", "the method named " +
                        methodToBeInvoked + " does not exist.", msg.get("id").toString(), msg.get("identity"));
            }

        } catch (InvocationTargetException e) {

            // questa eccezione invece viene sollevata quando il metodo chiamato ha sollevato a sua volta un'eccezione, e in
            // questo particolare caso mostra all'utente perché l'eccezione è stata sollevata
            Throwable cause = e.getCause();
            if (msg.get("type").equals("request")) {
                this.server.sendError("-32000", "Server error", "the method named " + methodToBeInvoked +
                        " has thrown an exception because " + cause.getMessage() , msg.get("id").toString(), msg.get("identity"));
            }
        }

        // se l'invocazione del metodo non ha sollevato nessuna eccezione citata sopra, allora si invia il risultato del metodo
        // sempre solo se il client ha inviato una Request
        if (success && msg.get("type").equals("request")) {
            this.server.sendResult(result, msg.get("id").toString(), msg.get("identity"));
        }
    }
}
