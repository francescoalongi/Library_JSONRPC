package Testing;

import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import client.library.*;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestFactoryTest {
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
    @DisplayName("Richiesta creazione RequestObject con jsonrpc diverso da 2.0")
    void testCreateRequestObj() {
        // In questo test si vede come un Request Object deve essere creato secondo le specifiche di JSONRPC, infatti se viene immessa
        // una versione di jsonRPC diversa da 2.0 viene sollevata un eccezione che notifica all'utente l'errore

        System.out.println("testCreateRequestObject");

        ArrayList<String> params = new ArrayList<>();
        params.add("25");
        params.add("13");

        try {
            RequestObject requestObject = RequestFactory.createRequestObj(Type.REQUEST, "2.5", "subtract",params, "0");
        } catch ( JSONException | IllegalArgumentException e) {
            assertEquals("the jsonrpc of the request must be equals to 2.0, you ask for a jsonrpc version equals to 2.5", e.getMessage());
        }
    }

}