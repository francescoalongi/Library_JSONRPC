package Testing;


import client.library.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientParserTest {

    private ClientParser clientParser;

    @BeforeEach
    void setUp() {
        this.clientParser = new ClientParser();
        System.out.println("----------------------------");
        System.out.println("Running test...");

    }

    @AfterEach
    void tearDown() {

        System.out.println("Test finished with success!");
        System.out.println("-----------------------------\n");
    }


    @Test
    @DisplayName("Un batch viene parsato esattamente come le specifiche JSONRPC chiedono")
    void testBatchToJSONString() {
        // in questo test viene mostrato come un ArrayList<RequestObject> (se correttamente strutturato) viene parsato esattamente
        // come le specifiche di JSONRPC chiedono, il passo fondamentale sta nel fatto che un RequestObject non può essere costruito
        // in un modo errato (il test di questa cosa verrà fatta nella classe RequestFactoryTest) quindi se si ha la certezza che tutti
        // i RequestObject sono corretti all'interno dell'ArrayList allora questa funzione dovrà semplicemente tradurre i RequestObject
        // in una JSONString, e questo test mostra la sua correttezza.

        System.out.println("testBatchToJSONString");

        // inizio costruzione del batch
        ArrayList<String> paramsOfReq1 = new ArrayList<>();
        paramsOfReq1.add("1");
        paramsOfReq1.add("2");
        paramsOfReq1.add("4");
        RequestObject req1 = null;
        try {
            req1 = RequestFactory.createRequestObj(Type.REQUEST, "2.0", "sum", paramsOfReq1, "1");
        } catch (JSONException e) {
            // questa eccezione verrà sollevata solo se la jsonrpc version non è uguale a 2.0, in questo caso quindi mai
        }
        ArrayList<String> paramsOfNot1 = new ArrayList<>();
        paramsOfNot1.add("7");
        RequestObject not1 = null;

        try {
            not1 = RequestFactory.createRequestObj(Type.NOTIFICATION, "2.0", "notify_hello", paramsOfNot1,null);
        } catch (JSONException e) {
            // questa eccezione verrà sollevata solo se la jsonrpc version non è uguale a 2.0, in questo caso quindi mai
        }

        ArrayList<String> paramsOfReq2 = new ArrayList<>();
        paramsOfReq2.add("42");
        paramsOfReq2.add("23");
        RequestObject req2 = null;
        try {
            req2 = RequestFactory.createRequestObj(Type.REQUEST, "2.0", "subtract", paramsOfReq2, "2");
        } catch (JSONException e) {
            // questa eccezione verrà sollevata solo se la jsonrpc version non è uguale a 2.0, in questo caso quindi mai
        }

        ArrayList<RequestObject> arrayBatch = new ArrayList<>();
        arrayBatch.add(req1);
        arrayBatch.add(not1);
        arrayBatch.add(req2);

        Batch batch = new Batch(arrayBatch);
        // fine costruzione del batch

        try {
            assertEquals(clientParser.batchToJSONString(batch),"[{\"method\":\"sum\",\"id\":\"1\",\"jsonrpc\":\"2.0\",\"params\":[\"1\",\"2\",\"4\"]}," +
                    "{\"method\":\"notify_hello\",\"jsonrpc\":\"2.0\",\"params\":[\"7\"]}," +
                    "{\"method\":\"subtract\",\"id\":\"2\",\"jsonrpc\":\"2.0\",\"params\":[\"42\",\"23\"]}]");

        } catch (JSONException e ) {
            // in questo caso non verrà mai chiamata
        }
    }

    @Test
    @DisplayName("Il RequestObject viene parsato esattamente come le specifiche JSONRPC chiedono")
    void testRequestObjectToJSONString() {
        // Questo test mostra invece che un RequestObject se correttamente costruito (ma, come già detto, non potrebbe essere altrimenti in
        // in quanto la RequestFactory creerà dei RequestObject coerenti con le specifiche JSONRPC) viene facilmente parsato in una JSONString
        System.out.println("testRequestObjectToJSONString");

        // inizio creazione oggetti di test
        ArrayList<String> paramsForR = new ArrayList<>();
        paramsForR.add("25");
        paramsForR.add("13");
        ArrayList<String> paramsForN = new ArrayList<>();
        paramsForN.add("1");
        paramsForN.add("2");
        paramsForN.add("4");
        RequestObject requestObjectR = null;
        RequestObject requestObjectN = null;
        // fine creazione oggetti di test

        try {
            requestObjectR = RequestFactory.createRequestObj(Type.REQUEST, "2.0", "subtract", paramsForR, "0");
            requestObjectN = RequestFactory.createRequestObj(Type.NOTIFICATION, "2.0", "notify_sum", paramsForN, null);
        } catch (JSONException e) {
            // questa eccezione verrà sollevata solo se la jsonrpc version non è uguale a 2.0, in questo caso quindi mai
        }
        try {
            assertEquals(clientParser.requestObjectToJSONString(requestObjectR),"{\"method\":\"subtract\",\"id\":\"0\",\"jsonrpc\":\"2.0\",\"params\":[\"25\",\"13\"]}");
            assertEquals(clientParser.requestObjectToJSONString(requestObjectN), "{\"method\":\"notify_sum\",\"jsonrpc\":\"2.0\",\"params\":[\"1\",\"2\",\"4\"]}" );
        } catch ( JSONException | IllegalArgumentException e) {
            // in questo caso non viene mai catchata un eccezione
        }
    }

    @Test
    @DisplayName("Riscontri nell'analisi di response Object diversi")
    void testAnalyzeResponse() {
        System.out.println("testAnalyzeResponse");

        // arriva un JSONString identificante un errore senza il campo data
        try {
            assertEquals(clientParser.analyzeResponse(new JSONObject("{\"jsonrpc\": \"2.0\", \"error\": {\"code\": -32600, \"message\": \"Invalid Request\"}, \"id\": null}"),"3"),
                    "Error code: -32600 ~ Invalid Request");
        } catch (JSONException e){
            // mai chiamata
        }

        // arriva un JSONString identificante un errore con il campo data
        try{
            assertEquals(clientParser.analyzeResponse(new JSONObject("{\"jsonrpc\": \"2.0\", \"error\": {\"code\": -32600, \"message\": \"Invalid Request\",\"data\":\"some data\"}, \"id\": null}"),"3"),
                    "Error code: -32600 ~ Invalid Request: some data");
        }catch (JSONException e) {
            // mai chiamata
        }

        // arriva un JSONString identificante una risposta che viene correttamente analizzato
        try {
            assertEquals(clientParser.analyzeResponse(new JSONObject("{\"jsonrpc\": \"2.0\", \"result\": \"7\", \"id\": \"1\"}"), "1"), "7");
        } catch (JSONException e) {
            // mai chiamata
        }

        // arriva un JSONString identificante una risposta che restituisce più risultati
        try {
            assertEquals(clientParser.analyzeResponse(new JSONObject("{\"jsonrpc\": \"2.0\", \"result\": [\"hello\", 5], \"id\": \"9\"}"), "9"), "[\"hello\",5]");
        } catch (JSONException e) {
            // mai chiamata
        }

        // arriva un JSON identificante una risposta con un id diverso dall'id della risposta
        try {
            assertEquals(clientParser.analyzeResponse(new JSONObject("{\"jsonrpc\": \"2.0\", \"result\": [\"hello\", 5], \"id\": \"9\"}"), "8"), "The id of your request is different from the id of the response");
        } catch (JSONException e) {
            // mai chiamata
        }
    }

}