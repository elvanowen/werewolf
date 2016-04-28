import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class WerewolfClient extends TCPClient{
    String lastSentMethod;
    static HashMap<String, OnMessageResponseInterface> callbackList = new HashMap<>();

    public WerewolfClient(String targetAddress, int targetPort) {
        super(targetAddress, targetPort);
    }

    public void registerListener(String method, OnMessageResponseInterface onMessageResponseInterface){
        callbackList.put(method, onMessageResponseInterface);
    }

    public void onMessageReceived(String message) {
        System.out.println("onReceivedMessage : " + message);

        JSONObject jsonObject;
        try {
            jsonObject = (JSONObject) new JSONParser().parse(message);

            if (jsonObject.get("method").toString() == null) {
                callbackList.get(this.lastSentMethod).onMessageReceived(jsonObject);
            } else {
                callbackList.get(jsonObject.get("method").toString()).onMessageReceived(jsonObject);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void send(JSONObject message){
        this.lastSentMethod = message.get("method").toString();
        super.send(message.toString());
    }
}

public class Client{
    static UDPServer udpServer;
    static WerewolfClient client;
    static ArrayList<JSONObject> clientList;
    static String username;
    static int playerID;

    static {
        promptServerAddress();
    }

    public static void main(String args[]) throws Exception {
        registerListener();
        while (true) promptCommand();
    }

    public static void registerListener(){
        client.registerListener("join", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                if (response.get("status") == "ok") {
                    playerID = Integer.parseInt(response.get("player_id").toString());
                }
            }

            @Override
            public void onMessageReceived(JSONObject message, String remoteAddress, int remotePort) {

            }
        });

        client.registerListener("ready", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                if (response.get("status") == "ok") {
                    String description = response.get("desription").toString();
                    System.out.println(description);
                }
            }

            @Override
            public void onMessageReceived(JSONObject message, String remoteAddress, int remotePort) {

            }
        });

        client.registerListener("leave", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                if (response.get("status") == "ok") {
                    System.exit(0);
                }
            }

            @Override
            public void onMessageReceived(JSONObject message, String remoteAddress, int remotePort) {

            }
        });

        client.registerListener("client_address", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject message) {

            }

            @Override
            public void onMessageReceived(JSONObject response, String remoteAddress, int remotePort) {
                if (response.get("status") == "ok") {
                    JSONArray clients = (JSONArray) response.get("clients");

                    clientList = new ArrayList<>();

                    for (int i = 0; i < clients.size(); i++) {
                        clientList.add((JSONObject) clients.get(i));
                    }
                }
            }
        });

        client.registerListener("prepare_proposal", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
            }

            @Override
            public void onMessageReceived(JSONObject message, String remoteAddress, int remotePort) {
                String[] kpuId = highestKPUId.split("-");

                if (Integer.parseInt(((JSONArray)message.get("proposal_id")).get(0).toString()) > Integer.parseInt(kpuId[0]) || (Integer.parseInt(((JSONArray) message.get("proposal_id")).get(0).toString()) == Integer.parseInt(kpuId[0]) && Integer.parseInt(((JSONArray) message.get("proposal_id")).get(1).toString()) > Integer.parseInt(kpuId[1]))){
                    try {
                        JSONObject response = new JSONObject();
                        response.put("status", "ok");
                        response.put("status", "accepted");
                        response.put("status", highestKPUId);

                        new UDPClient(remoteAddress, remotePort).send(response.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        JSONObject response = new JSONObject();
                        response.put("status", "fail");
                        response.put("status", "rejected");

                        new UDPClient(remoteAddress, remotePort).send(response.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        client.registerListener("accept_proposal", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject message) {

            }

            @Override
            public void onMessageReceived(JSONObject message, String remoteAddress, int remotePort) {
                String[] kpuId = highestKPUId.split("-");

                if (Integer.parseInt(((JSONArray)message.get("proposal_id")).get(0).toString()) > Integer.parseInt(kpuId[0]) || (Integer.parseInt(((JSONArray) message.get("proposal_id")).get(0).toString()) == Integer.parseInt(kpuId[0]) && Integer.parseInt(((JSONArray) message.get("proposal_id")).get(1).toString()) > Integer.parseInt(kpuId[1]))){
                    try {
                        JSONObject response = new JSONObject();
                        response.put("status", "ok");
                        response.put("status", "accepted");
                        response.put("status", highestKPUId);

                        new UDPClient(remoteAddress, remotePort).send(response.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        JSONObject response = new JSONObject();
                        response.put("status", "fail");
                        response.put("status", "rejected");

                        new UDPClient(remoteAddress, remotePort).send(response.toString());

                        ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);

                        exec.schedule(new Runnable(){
                            @Override
                            public void run(){
                                paxosPrepareProposal();
                            }
                        }, 3, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        client.registerListener("start", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                client.send(new JSONObject().put("status", "ok").toString());
            }

            @Override
            public void onMessageReceived(JSONObject message, String remoteAddress, int remotePort) {

            }
        });

        client.registerListener("change_phase", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                client.send(new JSONObject().put("status", "ok").toString());
            }

            @Override
            public void onMessageReceived(JSONObject message, String remoteAddress, int remotePort) {

            }
        });

        client.registerListener("game_over", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                client.send(new JSONObject().put("status", "ok").toString());
            }

            @Override
            public void onMessageReceived(JSONObject message, String remoteAddress, int remotePort) {

            }
        });
    }

    public static void promptServerAddress(){
        Scanner reader = new Scanner(System.in);  // Reading from System.in

        System.out.print("Enter server address [localhost]: ");
        String targetAddress = reader.nextLine();

        if (targetAddress.equals("")){
//            targetAddress = "localhost";
            targetAddress = "10.5.22.49";
        }

        System.out.print("Enter server port [8888]: ");
        String _targetPort = reader.nextLine();
        int targetPort;

        if (!_targetPort.equals("")){
            targetPort = Integer.parseInt(_targetPort);
        } else {
            targetPort = 8888;
        }

        client = new WerewolfClient(targetAddress, targetPort);
    }

    public static void promptCommand(){
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

    public static void promptUsername(){
        Scanner reader = new Scanner(System.in);  // Reading from System.in

        System.out.print("Enter username: ");
        username = reader.next(); // Scans the next token of the input as an int.
    }

    public static void joinGame(){
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("method", "join");
        jsonObject.put("username", username);

        client.send(jsonObject);
    }

    public static void leaveGame(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("method", "leave");

        client.send(jsonObject.toString());
    }

    public static void readyUp(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("method", "ready");

        client.send(jsonObject.toString());
    }

    public static void listClient(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("method", "client_address");

        client.send(jsonObject.toString());
    }

    public static void chooseLeader(){

    }
}