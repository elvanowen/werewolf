import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by elvan_owen on 4/26/16.
 */

public class UDPClient {
    /**
     * Contoh kode program untuk node yang mengirimkan paket. Paket dikirim
     * menggunakan UnreliableSender untuk mensimulasikan paket yang hilang.
     */

    InetAddress IPAddress;
    int targetPort;

    DatagramSocket datagramSocket;
    UnreliableSender unreliableSender;

    public UDPClient(){
        this("localhost", 7777);
    }

    public UDPClient(String targetAddress, int targetPort){
        try {
            this.IPAddress = InetAddress.getByName(targetAddress);
            this.targetPort = targetPort;

            datagramSocket = new DatagramSocket();
            unreliableSender = new UnreliableSender(datagramSocket);
        } catch (Exception e){
            System.out.print(e.getMessage());
        }
    }

    public void send(String message) throws Exception{
        byte[] sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, targetPort);
        unreliableSender.send(sendPacket);
    }
}
