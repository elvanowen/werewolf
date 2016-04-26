import java.net.*;
import java.io.*;
import java.util.*;

public class TCPServer extends Thread {
    private ServerSocket serverSocket;
    private ArrayList<ClientSocket> clientSocketList;

    public TCPServer(){
        this(8888);
    }

    public TCPServer(int port){
        try {
            serverSocket = new ServerSocket(port);
//            serverSocket.setSoTimeout(10000);

            this.start();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void run() {
        while(true) {
            try {
                System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");

                Socket _clientSocket = serverSocket.accept();

                ClientSocket clientSocket = new ClientSocket(this, _clientSocket);
                clientSocketList.add(clientSocket);
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

    }

    public void broadcast(String message){
        for (ClientSocket clientSocket: clientSocketList) {
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

            thread = new Thread(this);
            thread.start();
        }

        public void run(){
            while (true){
                try {
                    DataInputStream in = new DataInputStream(this.clientSocket.getInputStream());
                    this.server.onMessageReceived(in.readUTF());
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        public void send(String message){
            try {
                DataOutputStream out = new DataOutputStream(this.clientSocket.getOutputStream());
                out.writeUTF(message);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

        }
    }
}