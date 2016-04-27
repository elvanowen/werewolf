import java.util.Scanner;

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

        Scanner reader = new Scanner(System.in);  // Reading from System.in

        while (true) {
            System.out.print("Enter message: ");
            String message = reader.next(); // Scans the next token of the input as an int.

            System.out.println("Message : " + message);

            tcpClient.send(message);
        }
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
