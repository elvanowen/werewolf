import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class Client {
	/**
	 * Contoh kode program untuk node yang mengirimkan paket. Paket dikirim
	 * menggunakan UnreliableSender untuk mensimulasikan paket yang hilang.
	 */

    static TCPClient tcpClient;
    static UDPClient udpClient;
    static String lastSentMethod;

    static String username;
    static int playerID;

	public static void main(String args[]) throws Exception {
        tcpClient = new TCPClient();

        promptUsername();
        joinGame();

//        Random random = new Random();
//        int number = random.nextInt(5);
//        System.out.println("Number : " + number);
//
//        if (number % 2 == 1){
//            System.out.println("As UDPClient");
//            udpClient = new UDPClient();
//
//            Scanner reader = new Scanner(System.in);  // Reading from System.in
//
//            while (true) {
//                System.out.print("Enter message: ");
//                String message = reader.next(); // Scans the next token of the input as an int.
//
//                System.out.println("Message : " + message);
//
////            tcpClient.send(message);
//                udpClient.send(message);
//            }
//        } else {
//            System.out.println("As UDPServer");
//            UDPServer udpServer = new UDPServer();
//        }
	}

    public static void promptUsername(){
        Scanner reader = new Scanner(System.in);  // Reading from System.in

        System.out.print("Enter username: ");
        username = reader.next(); // Scans the next token of the input as an int.
    }

    public static void joinGame(){
        lastSentMethod = "join";

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("method", "join");
        jsonObject.put("username", username);

        tcpClient.send(jsonObject.toString());
    }

    public static void onResponseJoinGame(String message) throws ParseException {
        JSONParser parser = new JSONParser();

        JSONObject jsonObject = (JSONObject) parser.parse(message);

        if (jsonObject.get("status") == "ok") {
            playerID = Integer.parseInt(jsonObject.get("player_id").toString());
        }
    }

    public static void leaveGame(){
        lastSentMethod = "leave";

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("method", "leave");
        tcpClient.send(jsonObject.toString());
    }

    public static void onResponseLeaveGame(String message) throws ParseException {
        JSONParser parser = new JSONParser();

        JSONObject jsonObject = (JSONObject) parser.parse(message);

        if (jsonObject.get("status") == "ok") {
            playerID = Integer.parseInt(jsonObject.get("player_id").toString());
        }
    }

    public static void readyUp(){

    }

    public static void onResponseReadyUp(String message) throws ParseException {
        JSONParser parser = new JSONParser();

        JSONObject jsonObject = (JSONObject) parser.parse(message);

        if (jsonObject.get("status") == "ok") {
            playerID = Integer.parseInt(jsonObject.get("player_id").toString());
        }
    }

    public static void listClient(){

    }

    public static void onResponseListClient(String message) throws ParseException {
        JSONParser parser = new JSONParser();

        JSONObject jsonObject = (JSONObject) parser.parse(message);

        if (jsonObject.get("status") == "ok") {
            playerID = Integer.parseInt(jsonObject.get("player_id").toString());
        }
    }
}
