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
        System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");

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
        System.out.println("on: " + message); 
        Server.onMessageReceived(client, message); // pass to server
    }
    
    public void broadcast(HashMap<String, TCPServer.Client>clientList ,String message){
        
    }
    
    public void send(TCPServer.Client client, String message){
        client.clientSocket.send(message);
    }
    
    // client
    public class Client{
        int playerId;
        int isAlive; // 0 or 1
        String address;
        int port;
        String username;
        String role;
        String status; // "join", "ready"
        ClientSocket clientSocket;
        
        //called when client create socket conn
        public Client(TCPServer tcpServer, Socket socket){
            clientSocket = new ClientSocket(tcpServer, socket,this);

            //set client address and port
            this.address = socket.getInetAddress().toString();
            this.port = socket.getPort();
        }
        
        //called when client join game
        void setClient(int playerId,String username, String role){
            this.playerId = playerId;
            this.isAlive = 1;
            this.username = username;
            this.role = role;
            this.status = "join"; // set initial client status
        }
    }
    
    private class ClientSocket implements Runnable {
        private Socket clientSocket = null;
        private Thread thread;
        private TCPServer server;
        private Client client; //reference to client
        
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