import java.net.*;
import java.io.*;
import java.util.*;

public class TCPServer implements Runnable {
    ServerSocket serverSocket;
    ArrayList<ClientSocket> clientSocketList = new ArrayList<ClientSocket>();
    Thread thread;

    public TCPServer(){
        this(8888);
    }

    public TCPServer(int port){
        try {
            serverSocket = new ServerSocket(port);

            thread = new Thread(this);
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

                ClientSocket clientSocket = new ClientSocket(this, _clientSocket);
                System.out.println("Before : " + clientSocketList.size());
                clientSocketList.add(clientSocket);
                System.out.println("After : " + clientSocketList.size());
            } catch(SocketTimeoutException s) {
                System.out.println("Socket timed out!");
                break;
            } catch(IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void onMessageReceived(String message){
        System.out.println("onReceivedMessage : " + message);

        broadcast(message + " toooo!!");
//        send(clientSocketList, message + " toooo!!");


    }

    public void broadcast(String message){
        for (ClientSocket clientSocket: clientSocketList) {
            System.out.println("Broadcasting !!");
            System.out.println(clientSocket);

            clientSocket.send(message);
        }
    }

    public void send(ArrayList<ClientSocket> clientSocketList, String message){
        for (ClientSocket clientSocket: clientSocketList) {
            clientSocket.send(message);
        }
    }

    private class ClientSocket implements Runnable {
        private Socket clientSocket = null;
        private Thread thread;
        private TCPServer server;

        public ClientSocket(TCPServer server, Socket clientSocket){
            this.server = server;
            this.clientSocket = clientSocket;

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
                        this.server.onMessageReceived(inLine);
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