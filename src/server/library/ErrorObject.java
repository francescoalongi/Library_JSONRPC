package server.library;

public class ErrorObject {

    protected int code;
    protected String message;
    protected String data;

    ErrorObject (int Code, String Message, String Data) {

        this.code = Code;
        this.message = Message;
        this.data = Data;
    }


}
