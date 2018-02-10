package client.library;

import java.util.ArrayList;


public abstract class RequestObject {
    // classe astratta con il semplice scopo di permettere alla Notification e alla Request di ereditare i suoi parametri e soprattutto
    // permette di trattare la Notification e la Request come se fossero due RequestObject
    String jsonrpc;
    String method;
    ArrayList<String> params;

}
