package client.library;

import java.util.ArrayList;

class Notification extends RequestObject {
    public Notification(String jsonrpVersion, String methodToBeInvoked, ArrayList<String> parameters){
        this.jsonrpc = jsonrpVersion;
        this.method = methodToBeInvoked;
        this.params = parameters;
    }
}

