import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by elvan_owen on 4/26/16.
 */

public abstract class UDPServer implements Runnable {
    /**
     * Contoh kode program untuk node yang menerima paket. Idealnya dalam paxos
     * balasan juga dikirim melalui UnreliableSender.
     */

    int listenPort;
    DatagramSocket serverSocket;
    Thread thread;

//    public UDPServer(){
//        this(7777);
//    }

    public UDPServer(int listenPort){
        this.listenPort = listenPort;

        try {
            serverSocket = new DatagramSocket(listenPort);
            thread = new Thread(this);

            thread.start();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public int getListenPort(){
        return this.listenPort;
    }

    public abstract void onMessageReceived(String message, InetAddress remoteAddress, int remotePort);

    public void run(){
        System.out.println("UDP Server Listening on port " + listenPort);
        while (true){
            try {
                byte[] receiveData = new byte[1024];

                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);

                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());

                System.out.println("UDPServer Receiving message from " + receivePacket.getAddress() + " : " + receivePacket.getPort());

                onMessageReceived(message, receivePacket.getAddress(), receivePacket.getPort());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void send(String message, InetAddress remoteAddress, int remotePort){
        byte[] sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteAddress, remotePort);

        try {
            new UnreliableSender(this.serverSocket).send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
