import client.library.*;

import java.util.ArrayList;

public class ClientMain {

    public static void main (String[] args) {
        Client client = new Client("5555");
        ArrayList<String> params = new ArrayList<>();
        params.add("1");
        params.add("3");
        System.out.println("The result of the method sum is: " + client.callMethod("request", "0", "sum", params, "localhost"));
        System.out.println(client.callMethod("notification", "1", "notify_im_up", null, "localhost"));
        System.out.println("The result of the method foo is: " + client.callMethod("request", "2", "foo", null, "localhost"));

        ArrayList<String> types = new ArrayList<>();
        ArrayList<String> methods = new ArrayList<>();
        ArrayList<ArrayList<String>> parameters = new ArrayList<>();
        ArrayList<String> ids = new ArrayList<>();

        types.add("request");
        types.add("notification");
        types.add("request");
        types.add("request");
        methods.add("methodThatDoesntExist");
        methods.add("notify_im_up");
        methods.add("sum");
        methods.add("methodWithException");
        parameters.add(null);
        parameters.add(null);
        parameters.add(params);
        parameters.add(null);
        ids.add("3");
        ids.add("4");
        ids.add("5");
        ids.add("6");
        ArrayList<String> results = client.callMultipleMethod(types, methods, parameters, ids, "localhost");
        for (int i = 0; i < results.size(); i++) {
            System.out.println("The result of the method " + methods.get(i) + " is: " + results.get(i));
        }


    }

}
