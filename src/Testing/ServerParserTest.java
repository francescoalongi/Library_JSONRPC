package Testing;


import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.library.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


class ServerParserTest {

    ServerParser serverParser;
    @BeforeEach
    void setUp() {
        this.serverParser = new ServerParser();
        System.out.println("----------------------------");
        System.out.print("Running ");
    }

    @AfterEach
    void tearDown() {
        System.out.println("Test finished with success!");
        System.out.println("-----------------------------\n");
    }

    @Test
    @DisplayName("Traduzione da ResponseObject a JSONString")
    void testResponseObjectToJSONString() {
        // questo test verificherà che questa funzione traduca rispettando perfettamente le specifiche JSONRPC un ResponseObject
        // ben formato. Si presuppone che il RequestObject ben formato per le evidenze dimostrate in ResponseFactoryTest

        System.out.println("testResponseObjectToJSONString");


        ResponseObject responseObjectR = null;
        ResponseObject responseObjectE = null;
        try {
            responseObjectR = ResponseFactory.createResponseObj(Type.RESPONSE, "2.0", "2", "19", -1, null, null);
            responseObjectE = ResponseFactory.createResponseObj(Type.ERROR, "2.0", "5", null, -32601, "Method not found", null);
        } catch (JSONException e) {
            // non verrà mai catchata perché in questo scenario i dati inseriti dall'utente sono tutti corretti
        }

        try {
            assertEquals(this.serverParser.responseObjectToJSONString(responseObjectR),"{\"result\":\"19\",\"id\":\"2\",\"jsonrpc\":\"2.0\"}" );
            assertEquals(this.serverParser.responseObjectToJSONString(responseObjectE),"{\"id\":\"5\",\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32601,\"message\":\"Method not found\"}}" );
        } catch (JSONException e) {
            // in questo scenario non verrà mai catchata
        }
    }


    @Test
    @DisplayName("Riscontri con gli errori più comuni di analisi di JSONString")
    void testAnalyzeRequestJSONObject() {
        // in questa funzione si faranno più casi di test
        // NOTA BENE: questa funzione ritorna una mappa, quindi gli assert verranno fatti sulle mappe
        // Bisogna tenere in considerazione che quando verrà chiamata qusta funzione i JSONObject che verranno passati
        // come parametri saranno necessariamente dei JSONObject validi. Questo perché questa funzione verrà chiamata
        // da un'altra funzione della classe Server (getCall()) la quale farà il controllo sui JSONObject (quini scarterà
        // quelli invalidi) prima di passarli a questa funzione

        System.out.println("testAnalyzeRequestJSONObject");

        // primo caso: il messaggio ricevuto dal client è una richiesta corretta di un metodo che non riceve parametri,
        // ci si aspetta quindi di ottenere da questa funzione una mappa cosi fatta:
        Map<String, Object> messageActual = new HashMap<>();
        Map<String, Object> messageExpected = new HashMap<>();
        messageExpected.put("method", "foobar");
        messageExpected.put("id", "1");
        messageExpected.put("type", "request");

        try {
            messageActual = serverParser.analyzeRequest(new JSONObject("{\"jsonrpc\":\"2.0\",\"method\":\"foobar\",\"id\":\"1\"}"));
        } catch (JSONException e) {
            // questa eccezione viene sollevata nel caso in cui o il costruttore del JSONObject fallisce (non è questo il caso)
            // oppure quando il metodo durante l'analisi trova che mancano alcuni (o anche solo uno) campi fondamentali
        }
        assertEquals( messageExpected, messageActual);
        //fine primo caso di test

        // prepariamo gli oggetti per il secondo caso di test
        messageActual.clear();
        messageExpected.clear();

        // secondo caso: il JSONObject non ha il campo metodo (se un RequestObject è senza metodo viene considerato inutile
        // e quindi scartato e il server notificato tramite un eccezione)
        try {
            messageActual = serverParser.analyzeRequest(new JSONObject("{\"jsonrpc\":\"2.0\",\"params\":\"bar\"}"));
        } catch (JSONException e) {
            assertEquals("JSONObject[\"method\"] not found.", e.getMessage());
        }
        // fine secondo caso.

        // prepariamo gli oggetti per il terzo caso di test
        messageActual.clear();
        messageExpected.clear();

        // terzo caso: il JSONObject ottenuto dal client ha versione jsonrpc differente da 2.0. La richiesta viene scartata
        // e il server notificato tramite un eccezione.
        try {
            messageActual = serverParser.analyzeRequest(new JSONObject("{\"jsonrpc\":\"2.1\",\"params\":\"bar\"}"));
        } catch (JSONException e) {
            assertEquals("Cannot parse RequestObject with a jsonrpc version different from 2.0.", e.getMessage());
        }
        // fine terzo caso

        // prepariamo gli oggetti per il quarto caso di test
        messageActual.clear();
        messageExpected.clear();

        // quarto caso: il server riceve un JSONObject che rappresenta una notifica corretta

        messageExpected.put("method", "foobar");
        messageExpected.put("id", null);
        messageExpected.put("type", "notification");
        try {
            messageActual = serverParser.analyzeRequest(new JSONObject("{\"jsonrpc\":\"2.0\",\"method\":\"foobar\"}"));
        } catch (JSONException e) {
            // questa eccezione viene sollevata nel caso in cui o il costruttore del JSONObject fallisce (non è questo il caso)
            // oppure quando il metodo durante l'analisi trova che mancano alcuni (o anche solo uno) campi fondamentali
        }

        assertEquals(messageExpected, messageActual);
        // fine quarto caso

    }

}