package server.library;

public abstract class ResponseObject{

    // classe astratta che ha il solo scopo di far ereditare agli Error e alle Response i suoi attributi, essa è molto
    // utile nel codice perchè un Error o una Response venga trattata con un ResponseObject generico
    protected String jsonrpc;
    protected String id;

}
