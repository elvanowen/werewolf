public class Server {
    private static TCPServer server;

    public static void main(String [] args) {
        try {
            server = new TCPServer();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
