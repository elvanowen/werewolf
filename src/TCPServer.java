import java.net.*;
import java.io.*;
import java.util.*;

public class TCPServer extends Thread {
    private ServerSocket serverSocket;
    private ArrayList<Client> clientList = new ArrayList<Client>();

    public TCPServer(){
        this(8888);
    }

    public TCPServer(int port){
        try {
            serverSocket = new ServerSocket(port);

            Thread thread = new Thread(this);
            thread.start();
//            thread.join();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void run() {
        System.out.println("Waiting for tcpClient on port " + serverSocket.getLocalPort() + "...");

        while(true) {
            try {
                Socket _clientSocket = serverSocket.accept();

                Client client = new Client(this, _clientSocket);
                clientList.add(client); // potential players
                
            } catch(SocketTimeoutException s) {
                System.out.println("Socket timed out!");
                break;
            } catch(IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }
    
    public void onMessageReceived(Client client, String message){
        System.out.println("received: " + message); 
        Server.onMessageReceived(client, message); // pass to server
    }
    
    public void broadcast(HashMap<String, TCPServer.Client>clientList ,String message){
        Iterator it = clientList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry clientEntry = (Map.Entry)it.next();
            TCPServer.Client client = (TCPServer.Client)clientEntry.getValue();
            send(client, message);
        }
    }
    
    public void send(TCPServer.Client client, String message){
        client.clientSocket.send(message);
    }
    
    // tcpClient
    public class Client{
        int playerId;
        int isAlive; // 0 or 1
        String udpAddress;
        int udpPort;
        String username;
        String role;
        String status; // "join", "ready"
        ClientSocket clientSocket;
        
        //called when tcpClient create socket conn
        public Client(TCPServer tcpServer, Socket socket){
            clientSocket = new ClientSocket(tcpServer, socket,this);

        }
        
        //called when client join game
        void setClientJoin(int playerId, String udpAddress, int udpPort, String username){
            this.playerId = playerId;
            this.isAlive = 1;// set to alive
            this.udpAddress = udpAddress;
            this.udpPort = udpPort;
            this.username = username;
            this.status = "join"; // set initial client status
        }
    }
    
    private class ClientSocket implements Runnable {
        private Socket clientSocket = null;
        private Thread thread;
        private TCPServer server;
        private Client client; //reference to tcpClient
        
        public ClientSocket(TCPServer server, Socket clientSocket, Client client){
            this.server = server;
            this.clientSocket = clientSocket;
            this.client = client;
            
            System.out.println("Just connected to " + this.clientSocket.getRemoteSocketAddress());

            try {
                thread = new Thread(this);
                thread.start();
//                thread.join();
            } catch (Exception e){
                System.out.println(e);
            }

        }

        public void run(){
            while (true){
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));

                    String inLine = null;
                    while (((inLine = br.readLine()) != null) && (!(inLine.equals("")))) {
                          this.server.onMessageReceived(this.client,inLine);
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }

        public void send(String message){
            PrintStream output;

            try {
                output = new PrintStream(clientSocket.getOutputStream());

                output.println(message);
                output.flush();

                System.out.println("Server Socket sent " + message);
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
}