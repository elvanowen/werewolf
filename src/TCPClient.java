import java.lang.*;
import java.io.*;
import java.net.*;

class TCPClient implements Runnable{
    Socket socket;
    Thread thread;

    public TCPClient(){
        this("localhost", 8888);
    }

    public TCPClient(String targetAddress, int targetPort){
        try {
            socket = new Socket(targetAddress, targetPort);

            thread = new Thread(this);
            thread.start();
        } catch (Exception e){
            e.printStackTrace();
        }

        System.out.println("Just connected to " + socket.getRemoteSocketAddress());
    }

    public void send(String message){
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println(message);
            out.flush();

            System.out.println("Client Socket sent " + message);
        } catch (Exception e){
            System.out.println(e);
        }
    }

    public void onMessageReceived(String message){
        System.out.println("onReceivedMessage : " + message);
    }

    public void run(){
        while (true){
            try {
                DataInputStream is = new DataInputStream(socket.getInputStream());
                onMessageReceived(is.readLine());
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}