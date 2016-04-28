import org.json.simple.parser.ParseException;

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
        this.listenPort = listerPort;

        try {
            serverSocket = new DatagramSocket(listenPort);
            thread = new Thread(this);

            thread.start();
        } catch (Exception e){
            System.out.println(e);
        }
    }

    public void onMessageReceived(String message) throws ParseException {
        System.out.println("onMessageReceived : " + message);
    }

    public void run(){
        System.out.println("UDP Server Listening on port " + listenPort);
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