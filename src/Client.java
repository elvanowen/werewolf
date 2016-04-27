import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashMap;
import java.util.Scanner;

public class Client extends TCPClient{
	/**
	 * Contoh kode program untuk node yang mengirimkan paket. Paket dikirim
	 * menggunakan UnreliableSender untuk mensimulasikan paket yang hilang.
	 */

    static String lastSentMethod;
    static Client client;

    static String username;
    static int playerID;
    static HashMap<String, OnMessageResponseInterface> callbackList = new HashMap<String, OnMessageResponseInterface>();

    public Client(String targetAddress, int targetPort) {
        super(targetAddress, targetPort);
    }

    static {
        promptServerAddress();
    }

	public static void main(String args[]) throws Exception {
        client.registerListener();
        while (true) client.promptCommand();
	}

    public void onMessageReceived(String message) {
        System.out.println("onReceivedMessage : " + message);

        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) new JSONParser().parse(message);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (jsonObject.get("method").toString() == null) {
            callbackList.get(Client.lastSentMethod).onMessageReceived(jsonObject);
        } else {
            callbackList.get(jsonObject.get("method").toString()).onMessageReceived(jsonObject);
        }
    }

    public void registerListener(){
        callbackList.put("join", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                if (response.get("status") == "ok") {
                    playerID = Integer.parseInt(response.get("player_id").toString());
                }
            }
        });

        callbackList.put("ready", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                if (response.get("status") == "ok") {
                    String description = response.get("desription").toString();
                    System.out.println(description);
                }
            }
        });

        callbackList.put("leave", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                if (response.get("status") == "ok") {
                    System.exit(0);
                }
            }
        });

        callbackList.put("client_address", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                if (response.get("status") == "ok") {
                    JSONArray clients = (JSONArray) response.get("clients");

                    for (int i = 0; i < clients.size(); i++) {
                        JSONObject client = (JSONObject) clients.get(i);
                    }
                }
            }
        });

        callbackList.put("start", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                send(new JSONObject().put("status", "ok").toString());
            }
        });

        callbackList.put("change_phase", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                send(new JSONObject().put("status", "ok").toString());
            }
        });

        callbackList.put("game_over", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                send(new JSONObject().put("status", "ok").toString());
            }
        });
    }

    public static void promptServerAddress(){
        Scanner reader = new Scanner(System.in);  // Reading from System.in

        System.out.print("Enter server address [localhost]: ");
        String targetAddress = reader.nextLine();

        if (targetAddress.equals("")){
            targetAddress = "localhost";
        }

        System.out.print("Enter server port [8888]: ");
        String _targetPort = reader.nextLine();
        int targetPort;

        if (!_targetPort.equals("")){
            targetPort = Integer.parseInt(_targetPort);
        } else {
            targetPort = 8888;
        }

        client = new Client(targetAddress, targetPort);
    }

    public void promptCommand(){
        Scanner reader = new Scanner(System.in);  // Reading from System.in

        System.out.print("Enter command: ");
        String command = reader.next(); // Scans the next token of the input as an int.

        if (command.equalsIgnoreCase("join")){
            promptUsername();
            joinGame();
        } else if (command.equalsIgnoreCase("ready")){
            readyUp();
        } else if (command.equalsIgnoreCase("leave")){
            leaveGame();
        } else if (command.equalsIgnoreCase("client_address")){
            listClient();
        } else {
            System.out.println("Command not recognized");
            promptCommand();
        }
    }

    public void promptUsername(){
        Scanner reader = new Scanner(System.in);  // Reading from System.in

        System.out.print("Enter username: ");
        username = reader.next(); // Scans the next token of the input as an int.
    }

    public void joinGame(){
        lastSentMethod = "join";

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("method", "join");
        jsonObject.put("username", username);

        client.send(jsonObject.toString());
    }

    public void leaveGame(){
        lastSentMethod = "leave";

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("method", "leave");
        client.send(jsonObject.toString());
    }

    public void readyUp(){
        lastSentMethod = "readyUp";

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("method", "ready");
        client.send(jsonObject.toString());
    }

    public void listClient(){
        lastSentMethod = "client_address";

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("method", "client_address");
        send(jsonObject.toString());
    }
}
