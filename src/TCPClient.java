import java.lang.*;
import java.io.*;
import java.net.*;

class TCPClient implements Runnable{
    Socket socket;
    private Thread thread;

    public TCPClient(){
        this("localhost", 8888);
    }

    public TCPClient(String targetAddress, int targetPort){
        try {
            socket = new Socket(targetAddress, targetPort);
        } catch (Exception e){
            System.out.print(e.getMessage());
        }
    }

    public void send(String message){
        try {
            OutputStream outToServer = socket.getOutputStream();
            DataOutputStream out = new DataOutputStream(outToServer);
            out.writeUTF(message);
        } catch (Exception e){
            System.out.println(e);
        }
    }

    public void onMessageReceived(String message){

    }

    public void run(){
        while (true){
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                System.out.print("Received string: '");

                while (!in.ready()) {}
                onMessageReceived(in.readLine());
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}