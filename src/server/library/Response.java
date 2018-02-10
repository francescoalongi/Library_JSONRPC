package server.library;

class Response extends ResponseObject {

    String result;

    Response (String Result, String Jsonrpc, String Id) {

        this.result = Result;
        this.jsonrpc = Jsonrpc;
        this.id = Id;
    }

}
