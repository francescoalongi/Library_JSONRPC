package client.library;

import java.util.ArrayList;

class Request extends RequestObject {

    protected String id;

    public Request(String jsonrpVersion, String methodToBeInvoked, ArrayList<String> parameters, String Id){
        this.id=Id;
        this.jsonrpc=jsonrpVersion;
        this.method=methodToBeInvoked;
        this.params=new ArrayList<>();
        // se il metodo che si vuole chiamare non ha parametri allora il client nel campo "parameters" passa null
        if (parameters != null) {
            this.params.addAll(parameters);
        }
    }

}
