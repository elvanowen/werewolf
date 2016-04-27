import java.lang.*;
import java.io.*;
import java.net.*;

public abstract class TCPClient implements Runnable{
    static Socket socket;
    static Thread thread;

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

    public static void send(String message){
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println(message);
            out.flush();

            System.out.println("Client Socket sent " + message);
        } catch (Exception e){
            System.out.println(e);
        }
    }

    public abstract void onMessageReceived(String message);

    @Override
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