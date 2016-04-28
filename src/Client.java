import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    static WerewolfClient client;
    static ArrayList<JSONObject> clientList;
    static int proposalSequenceNumber = 0;
    static String highestKPUId = "0-0";
    static String username;
    static int playerID;
    static int kpuID;
    
    static ArrayList<Vote> voteList;
    static int numOfPlayer;
    static int numOfWerewolf;
    static int numOfVote=0;

    static {
        promptServerAddress();
    }

    public static void main(String args[]) throws Exception {
        registerListener();
        while (true) promptCommand();
        JSONObject json = 
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
        
        client.registerListener("vote_werewolf", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
            }

            @Override
            public void onMessageReceived(JSONObject message, String remoteAddress, int remotePort) {
                infoWerewolfKilled(Integer.parseInt(message.get("player_id").toString()));
                
                try {
                    JSONObject response = new JSONObject();
                    response.put("status", "fail");
                    response.put("status", "rejected");

                    new UDPClient(remoteAddress, remotePort).send(response.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
        client.send((JSONObject) new JSONObject().put("method", "leave"));
    }

    public static void readyUp(){
        client.send((JSONObject) new JSONObject().put("method", "ready"));
    }

    public static void listClient(){
        client.send((JSONObject) new JSONObject().put("method", "client_address"));
    }

    public static void paxosPrepareProposal() throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("method", "prepare_proposal");

        JSONArray proposalId =  new JSONArray();
        proposalId.add(proposalSequenceNumber);
        proposalId.add(playerID);

        jsonObject.put("proposal_id", proposalId);

        for (JSONObject client : clientList){
            new UDPClient(client.get("address").toString(), Integer.parseInt(client.get("port").toString())).send(jsonObject.toString());
        }
    }

    public static void paxosAcceptProposal() throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("method", "accept_proposal");

        JSONArray proposalId =  new JSONArray();
        proposalId.add(proposalSequenceNumber);
        proposalId.add(playerID);

        jsonObject.put("proposal_id", proposalId);

        for (JSONObject client : clientList){
            new UDPClient(client.get("address").toString(), Integer.parseInt(client.get("port").toString())).send(jsonObject.toString());
        }
    }
    
    
    public void killWerewolfVote(){
        //send vote to KPU for all non-KPU
        if (playerID != kpuID){
            //client dan werewolf
            
            Scanner reader = new Scanner(System.in);  // Reading from System.in
            int playerIDVote = reader.nextInt();
            
            JSONObject jsonObject = new JSONObject();
            
            jsonObject.put("method","vote_werewolf");
            jsonObject.put("player_id", playerIDVote);
            UDPClient udpClient = new UDPClient();
            
            try {
                udpClient.send(jsonObject.toString());
            } catch (Exception ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static void infoWerewolfKilled(int player_id){
        boolean added = false;
        numOfVote++;
        //cari apakah sudah ada di voteList
        for (Vote object: voteList) {
            if ((player_id==object.getPlayerId()) && !added) {
                //tambah count 1
                object.setVoteCount(object.getVoteCount() + 1);
                added=true;
            }
            if(added){
                break;
            }
        }
        if (!added){
            //buat vote baru
            Vote voteNew = new Vote(player_id,1);
            voteList.add(voteNew);
        }

        //semua telah vote, laporkan hasil voting ke server (KPU-->Server)
        if (numOfVote==numOfPlayer){
            //Menentukan apakah ada voting tertinggi
            Vote player_to_kill = voteList.get(0); // asumsi awal majority
            boolean majority_selected = true;
            for (Vote object: voteList) {
                if (object.getVoteCount() > player_to_kill.getVoteCount()) {
                    player_to_kill = object;
                    majority_selected = true; //ada majority baru
                } else if (object.getVoteCount() == player_to_kill.getVoteCount()){
                    majority_selected = false; //ada lebih dari 1 majority
                }
            }
            //Sudah ada kesimpulan apakah ada majority / tidak

            //rekapitulasi vote_result
            String recap = "";
            //Vote pertama
            Vote firstVote = voteList.get(0);
            recap = recap + "[" + "(" + firstVote.getPlayerId() + ", " + firstVote.getVoteCount() + ")"; 
            for (Vote object: voteList) {
                if(object.getPlayerId() != firstVote.getPlayerId()){
                    recap = recap + ", (" + object.getPlayerId()  + ", " + object.getVoteCount() + ")";
                }
            }
            recap = recap +"]";
            
            JSONObject jsonObject = new JSONObject();

            //rekapitulasi selesai  
            if (majority_selected){
                jsonObject.put("method","vote_result_werewolf");
                jsonObject.put("vote_status", "1");
                jsonObject.put("player_killed", player_to_kill.getPlayerId());
                jsonObject.put("vote_result", recap);
                //client.send(jsonObject.toString());
                   System.out.println(jsonObject.toString());
            } else { //tidak ada majority terpilih
                jsonObject.put("method","vote_result");
                jsonObject.put("vote_status", "-1");
                jsonObject.put("vote_result", recap);
                client.send(jsonObject.toString());                
            }
        }
    }
}


class Vote{
    private int playerId;
    private int voteCount;

    public Vote(int playerId, int voteCount) {
        this.playerId = playerId;
        this.voteCount = voteCount;
    }

    public int getPlayerId() {
        return playerId;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public void setVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }
}