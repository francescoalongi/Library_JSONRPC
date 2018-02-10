package server.library;

class Error extends ResponseObject {

    ErrorObject error;

    Error (int Code, String Message, String Data, String Id, String Jsonrpc) {

        error = new ErrorObject(Code, Message, Data);
        this.id = Id;
        this.jsonrpc = Jsonrpc;

    }

}
