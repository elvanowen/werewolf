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

    InetAddress targetAddress;
    int targetPort;
    UDPServer udpServer;

//    public UDPClient(){
//        this("localhost", 7777);
//    }

    public UDPClient(InetAddress targetAddress, int targetPort, UDPServer udpServer){
        try {
            this.targetAddress = targetAddress;
            this.targetPort = targetPort;
            this.udpServer = udpServer;
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void send(String message) throws Exception{
        udpServer.send(message, this.targetAddress, this.targetPort);
    }
}
