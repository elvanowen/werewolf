import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {
	/**
	 * Contoh kode program untuk node yang mengirimkan paket. Paket dikirim
	 * menggunakan UnreliableSender untuk mensimulasikan paket yang hilang.
	 */

    static TCPClient tcpClient;
    static UDPClient udpClient;

	public static void main(String args[]) throws Exception {
        tcpClient = new TCPClient();
//        udpClient = new UDPClient();
	}

    public void joinGame(){

    }

    public void leaveGame(){

    }

    public void readyUp(){

    }

    public void listClient(){

    }


}
