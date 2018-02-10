package Testing;

import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import server.library.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResponseFactoryTest {
    @BeforeEach
    void setUp() {
        System.out.println("----------------------------");
        System.out.println("Running test...");
    }

    @AfterEach
    void tearDown() {
        System.out.println("Test finished with success!");
        System.out.println("-----------------------------\n");
    }

    @Test
    @DisplayName("Richiesta creazione ResponseObject con jsonrpc diverso da 2.0")
    void testCreateResponseObj() {
        // In questo test si vede come un Response Object deve essere creato secondo le specifiche di JSONRPC, infatti se viene immessa
        // una versione di jsonRPC diversa da 2.0 viene sollevata un eccezione che notifica al server l'errore e inoltre, essendo stata
        // stabilita un enumerazione Type che contiene solo RESPONSE e ERROR, è impossibile creare un tipo di oggetto diverso. Quindi se
        // viene creato un ResponseObject esso rispetta perfettamente le specifiche di JSONRPC

        System.out.println("testCreateRequestObject");

        ArrayList<String> params = new ArrayList<>();
        params.add("25");
        params.add("13");

        try {
            ResponseObject responseObject = ResponseFactory.createResponseObj(Type.RESPONSE, "2.5", "0","anything", -1, null, null);
        } catch ( JSONException | IllegalArgumentException e) {
            assertEquals("the jsonrpc of the response must be equals to 2.0, you ask for a jsonrpc version equals to 2.5", e.getMessage());
        }
    }


}