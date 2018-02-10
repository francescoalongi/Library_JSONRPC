import server.library.*;

public class ServerMain {
    public static void main (String[] args) {
        Server server = new Server("5555");
        ClassWithMethods classWithMethods = new ClassWithMethods();
        server.start(classWithMethods);
    }
}
