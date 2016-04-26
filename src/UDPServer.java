import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by elvan_owen on 4/26/16.
 */

public class UDPServer implements Runnable {
    /**
     * Contoh kode program untuk node yang menerima paket. Idealnya dalam paxos
     * balasan juga dikirim melalui UnreliableSender.
     */

    int listenPort;
    DatagramSocket serverSocket;
    Thread thread;

    public UDPServer(){
        this(7777);
    }

    public UDPServer(int listerPort){
        try {
            serverSocket = new DatagramSocket(listenPort);
            thread = new Thread(this);

            thread.start();
        } catch (Exception e){
            System.out.println(e);
        }

    }

    public void onMessageReceived(String message){

    }

    public void run(){
        while (true){
            try {
                byte[] receiveData = new byte[1024];

                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);

                String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
                onMessageReceived(sentence);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
