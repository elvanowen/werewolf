import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

class WerewolfTCPClient extends TCPClient{
    String lastSentMethod;
    static HashMap<String, OnMessageResponseInterface> callbackList = new HashMap<>();

    public WerewolfTCPClient(String targetAddress, int targetPort) {
        super(targetAddress, targetPort);
    }

    public void registerListener(String method, OnMessageResponseInterface onMessageResponseInterface){
        callbackList.put(method, onMessageResponseInterface);
    }

    public synchronized void onMessageReceived(String message) {
        JSONObject jsonObject;
        try {
            jsonObject = (JSONObject) new JSONParser().parse(message);

            if (jsonObject.get("method") == null) {
                callbackList.get(this.lastSentMethod).onMessageReceived(jsonObject);
            } else {
                callbackList.get(jsonObject.get("method").toString()).onMessageReceived(jsonObject);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void send(JSONObject message){
        if (message.get("method") != null) {
            this.lastSentMethod = message.get("method").toString();
        }

        super.send(message.toString());
    }
}

class WerewolfUDPServer extends UDPServer{
    static String lastSentMethod;
    static HashMap<String, OnMessageResponseInterface> callbackList = new HashMap<>();

    public WerewolfUDPServer(int port) {
        super(port);

        WerewolfUDPClient.udpServer = this;
    }

    @Override
    public synchronized void onMessageReceived(String message, InetAddress remoteAddress, int remotePort) {
        System.out.println("onMessageReceived : " + message + " -> " + remoteAddress + " : " + remotePort);
        JSONObject jsonObject;
        try {
            jsonObject = (JSONObject) new JSONParser().parse(message);

            if (jsonObject.get("method") == null) {
                if (jsonObject.get("proposal_id") != null && jsonObject.get("kpu_id") != null) {
//                    For acceptor receiving accept proposal since no signal is sent before leader proposing
                    callbackList.get("accept_proposal").onMessageReceived(jsonObject, remoteAddress, remotePort);
                } else if (jsonObject.get("proposal_id") != null || jsonObject.get("previous_accepted") != null) {
//                    For acceptor receiving prepare proposal since no signal is sent before leader proposing
                    callbackList.get("prepare_proposal").onMessageReceived(jsonObject, remoteAddress, remotePort);
                } else {
                    callbackList.get(WerewolfUDPServer.lastSentMethod).onMessageReceived(jsonObject, remoteAddress, remotePort);
                }
            } else {
                callbackList.get(jsonObject.get("method").toString()).onMessageReceived(jsonObject, remoteAddress, remotePort);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void registerListener(String method, OnMessageResponseInterface onMessageResponseInterface){
        callbackList.put(method, onMessageResponseInterface);
    }
}

class WerewolfUDPClient extends UDPClient{
    static WerewolfUDPServer udpServer;

    public WerewolfUDPClient(InetAddress targetAddress, int targetPort){
        super(targetAddress, targetPort, WerewolfUDPClient.udpServer);
    }

    public synchronized void send(JSONObject message) throws Exception{
        if (message.get("method") != null) {
            WerewolfUDPServer.lastSentMethod = message.get("method").toString();
        }

        super.send(message.toString());
    }
}

enum GAME_ROLE {
    CIVILIAN,
    WEREWOLF
}

public class Client{
    static WerewolfUDPServer udpServer;
    static WerewolfTCPClient tcpClient;
    static ArrayList<JSONObject> clientList;
    static String username;
    static int playerID;
    static Paxos paxos;
    static GAME_ROLE playerRole;
    static ArrayList<String> playerFriends;
    static String gameTime;

    static ArrayList<Vote> voteList;
    static int numOfPlayer;
    static int numOfWerewolf;
    static int numOfVote=0;

    static {
        promptLocalPort();
        promptServerAddress();
    }

    public static void main(String args[]) throws Exception {
        registerListener();
        promptCommand();
    }

    public static void registerListener(){
        tcpClient.registerListener("join", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                System.out.println("onMessageReceived join");

                if (response.get("status").toString().equalsIgnoreCase("ok")) {
                    playerID = Integer.parseInt(response.get("player_id").toString());
                }
            }

            @Override
            public void onMessageReceived(JSONObject message, InetAddress remoteAddress, int remotePort) {
            }
        });

        tcpClient.registerListener("ready", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                System.out.println("onMessageReceived ready");
            }

            @Override
            public void onMessageReceived(JSONObject message, InetAddress remoteAddress, int remotePort) {
            }
        });

        tcpClient.registerListener("leave", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                System.out.println("onMessageReceived leave");

                if (response.get("status").toString().equalsIgnoreCase("ok")) {
                    System.exit(0);
                }
            }

            @Override
            public void onMessageReceived(JSONObject message, InetAddress remoteAddress, int remotePort) {

            }
        });

        tcpClient.registerListener("client_address", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                System.out.println("onMessageReceived client_address : " + response);

                if (response.get("status").toString().equalsIgnoreCase("ok")) {
                    JSONArray clients = (JSONArray) response.get("clients");

                    clientList = new ArrayList<>();
                    ArrayList<String> usernames = new ArrayList<>();

                    for (int i = 0; i < clients.size(); i++) {
                        clientList.add((JSONObject) clients.get(i));
                    }

                    if (playerRole == GAME_ROLE.WEREWOLF){
                        System.out.println("----------------------------");
                        System.out.println("Your Werewolf friends are : ");
                        usernames = playerFriends;
                    } else {
                        System.out.println("----------------------------");
                        System.out.println("Game players are : ");

                        for (int i=0;i<clientList.size();i++){
                            usernames.add(clientList.get(i).get("username").toString());
                        }
                    }

                    for (int i=0;i<usernames.size();i++){
                        System.out.println("\t-\t" + usernames.get(i));
                    }

                    System.out.println("----------------------------");
                    System.out.println();

                    if (gameTime.equalsIgnoreCase("day")){
                        ArrayList<Integer> playerIds = new ArrayList<Integer>();

                        for (JSONObject client: clientList){
                            playerIds.add(Integer.parseInt(client.get("player_id").toString()));
                        }

                        Collections.sort(playerIds);

                        if (playerID == playerIds.get(0) || playerID == playerIds.get(1)) {
                            paxos = new Paxos(PAXOS_ROLE.LEADER);
                            paxos.setPlayerID(playerID);
                            paxos.setClientList(clientList);
                            paxos.setServerSocket(tcpClient);
                            paxos.setUDPServer(udpServer);
                            paxos.sendPrepareProposal();
                        } else {
                            paxos = new Paxos(PAXOS_ROLE.ACCEPTOR);
                            paxos.setPlayerID(playerID);
                            paxos.setClientList(clientList);
                            paxos.setServerSocket(tcpClient);
                            paxos.setUDPServer(udpServer);
                        }

                        paxos.onLeaderChosen(new Paxos.OnLeaderChosenInterface() {

                            @Override
                            public void onLeaderChosen(int kpuId) {
                                if (kpuId != playerID) {
                                    killCivilianVote(kpuId);
                                }
                            }
                        });
                    } else if (gameTime.equalsIgnoreCase("night")){
                        if (paxos.getAcceptedKpuID() != playerID) {
                            killWerewolfVote(paxos.getAcceptedKpuID());
                        }
                    }
                }
            }

            @Override
            public void onMessageReceived(JSONObject response, InetAddress remoteAddress, int remotePort) {
            }
        });

        tcpClient.registerListener("kpu_selected", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                System.out.println("onMessageReceived kpu_selected");

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("status", "ok");

                tcpClient.send(jsonObject);

                paxos.setAcceptedKpuID((Integer) response.get("kpu_id"));
                paxos.onLeaderChosenCallback.onLeaderChosen(paxos.getAcceptedKpuID());
            }

            @Override
            public void onMessageReceived(JSONObject message, InetAddress remoteAddress, int remotePort) {
            }
        });

        tcpClient.registerListener("start", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                System.out.println("onMessageReceived Start");

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("status", "ok");

                tcpClient.send(jsonObject);

                gameTime = response.get("time").toString();

                if (response.get("role").toString().equals("werewolf")) {
                    playerRole = GAME_ROLE.WEREWOLF;

                    JSONArray _playerFriends = ((JSONArray)response.get("friend"));
                    playerFriends = new ArrayList<>();

                    for (int i=0;i<_playerFriends.size();i++){
                        playerFriends.add(_playerFriends.get(i).toString());
                    }

                    System.out.println("playerFriends : " + playerFriends);
                } else {
                    playerRole = GAME_ROLE.CIVILIAN;
                }

                System.out.println("Player Role : " + playerRole);
                System.out.println("Listing client");

                listClient();
            }

            @Override
            public void onMessageReceived(JSONObject message, InetAddress remoteAddress, int remotePort) {

            }
        });

        tcpClient.registerListener("change_phase", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                System.out.println("onMessageReceived change_phase");

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("status", "ok");

                tcpClient.send(jsonObject);

                gameTime = response.get("time").toString();
                listClient();
            }

            @Override
            public void onMessageReceived(JSONObject message, InetAddress remoteAddress, int remotePort) {

            }
        });

        tcpClient.registerListener("game_over", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                System.out.println("onMessageReceived game_over");

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("status", "ok");

                tcpClient.send(jsonObject);
            }

            @Override
            public void onMessageReceived(JSONObject message, InetAddress remoteAddress, int remotePort) {

            }
        });

        tcpClient.registerListener("accepted_proposal", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
                System.out.println("onMessageReceived TCPClient prepare_proposal");

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("status", "ok");
                jsonObject.put("description", "");

                tcpClient.send(jsonObject);
            }

            @Override
            public void onMessageReceived(JSONObject message, InetAddress remoteAddress, int remotePort) {

            }
        });

        udpServer.registerListener("prepare_proposal", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
            }

            @Override
            public void onMessageReceived(JSONObject message, InetAddress remoteAddress, int remotePort) {
                System.out.println("onMessageReceived prepare_proposal");

                if (paxos != null) paxos.onPreparePromiseReceived(message, remoteAddress, remotePort);
            }
        });

        udpServer.registerListener("accept_proposal", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject message) {

            }

            @Override
            public void onMessageReceived(JSONObject message, InetAddress remoteAddress, int remotePort) {
                System.out.println("onMessageReceived accept_proposal");

                if (paxos != null) paxos.onAcceptPromiseReceived(message, remoteAddress, remotePort);
            }
        });

        udpServer.registerListener("vote_werewolf", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
            }

            @Override
            public void onMessageReceived(JSONObject message, InetAddress remoteAddress, int remotePort) {
                System.out.println("onMessageReceived vote_werewolf");

                infoWerewolfKilled(Integer.parseInt(message.get("player_id").toString()));

                try {
                    JSONObject response = new JSONObject();
                    response.put("status", "ok");
                    response.put("description", "");

                    new WerewolfUDPClient(remoteAddress, remotePort).send(response);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        udpServer.registerListener("vote_civilian", new OnMessageResponseInterface() {
            @Override
            public void onMessageReceived(JSONObject response) {
            }

            @Override
            public void onMessageReceived(JSONObject message, InetAddress remoteAddress, int remotePort) {
                System.out.println("onMessageReceived vote_civilian");

                infoCivilianKilled(Integer.parseInt(message.get("player_id").toString()));

                try {
                    JSONObject response = new JSONObject();
                    response.put("status", "ok");
                    response.put("description", "");

                    new WerewolfUDPClient(remoteAddress, remotePort).send(response);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void promptLocalPort(){
        Scanner reader = new Scanner(System.in);  // Reading from System.in

        System.out.print("Enter local port [7777]: ");
        String _targetPort = reader.nextLine();
        int targetPort;

        if (!_targetPort.equals("")){
            targetPort = Integer.parseInt(_targetPort);
        } else {
            targetPort = 7777;
        }

        udpServer = new WerewolfUDPServer(targetPort);
    }

    public static void promptServerAddress(){
        Scanner reader = new Scanner(System.in);  // Reading from System.in

        System.out.print("Enter server address [localhost]: ");
        String targetAddress = reader.nextLine();

        if (targetAddress.equals("")){
            targetAddress = "localhost";
//            targetAddress = "10.5.22.49";
        }

        System.out.print("Enter server port [8888]: ");
        String _targetPort = reader.nextLine();
        int targetPort;

        if (!_targetPort.equals("")){
            targetPort = Integer.parseInt(_targetPort);
        } else {
            targetPort = 8888;
        }

        tcpClient = new WerewolfTCPClient(targetAddress, targetPort);
    }

    public static void promptCommand(){
        Scanner reader = new Scanner(System.in);  // Reading from System.in

        System.out.print("Enter command: ");
        String command = reader.next(); // Scans the next token of the input as an int.

        if (command.equalsIgnoreCase("join")){
            promptUsername();
            joinGame();
            promptCommand();
        } else if (command.equalsIgnoreCase("ready")){
            readyUp();
        } else if (command.equalsIgnoreCase("leave")){
            leaveGame();
            promptCommand();
        } else if (command.equalsIgnoreCase("client_address")){
            listClient();
            promptCommand();
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

        InetAddress ip = null;
        try {
            ip = InetAddress.getLocalHost();

            jsonObject.put("udp_address", ip.getHostAddress());
            jsonObject.put("udp_port", udpServer.getListenPort());

            tcpClient.send(jsonObject);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static void leaveGame(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("method", "leave");

        tcpClient.send(jsonObject);
    }

    public static void readyUp(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("method", "ready");

        tcpClient.send(jsonObject);
    }

    public static void listClient(){
        System.out.println("listClient method");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("method", "client_address");

        tcpClient.send(jsonObject);
    }

    public static void killWerewolfVote(final int kpuID){
        new AsyncInput("Input player (civilian) id who to kill : ").onInputEntered(new OnInputEnteredInterface() {
            @Override
            public void onInputEntered(String input) {
                int playerIDVote = Integer.parseInt(input);

                JSONObject jsonObject = new JSONObject();

                jsonObject.put("method","vote_werewolf");
                jsonObject.put("player_id", playerIDVote);

                for (JSONObject client: clientList){
                    if (Integer.parseInt(client.get("player_id").toString()) == kpuID){
                        WerewolfUDPClient udpClient = null;
                        try {
                            udpClient = new WerewolfUDPClient(InetAddress.getByAddress(client.get("address").toString().getBytes()), Integer.parseInt(client.get("port").toString()));
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }

                        try {
                            udpClient.send(jsonObject);
                        } catch (Exception ex) {
                            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        });
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
            recap = recap + "[" + "[" + firstVote.getPlayerId() + ", " + firstVote.getVoteCount() + "]";
            for (Vote object: voteList) {
                if(object.getPlayerId() != firstVote.getPlayerId()){
                    recap = recap + ", [" + object.getPlayerId()  + ", " + object.getVoteCount() + "]";
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
                tcpClient.send(jsonObject);
                //   System.out.println(jsonObject.toString());
            } else { //tidak ada majority terpilih
                jsonObject.put("method","vote_result");
                jsonObject.put("vote_status", "-1");
                jsonObject.put("vote_result", recap);
                tcpClient.send(jsonObject);
            }
            numOfVote = 0; //reset numOfVote untuk voting baru
            voteList.clear(); //reset list of vote
        }
    }

    public static void killCivilianVote(final int kpuID){
        new AsyncInput("Input player id who to kill : ").onInputEntered(new OnInputEnteredInterface() {
            @Override
            public void onInputEntered(String input) {
                int playerIDVote = Integer.parseInt(input);

                System.out.println("PlayerIDVote : " + playerIDVote);

                JSONObject jsonObject = new JSONObject();

                jsonObject.put("method","vote_civilian");
                jsonObject.put("player_id", playerIDVote);

                for (JSONObject client: clientList){
                    if (Integer.parseInt(client.get("player_id").toString()) == kpuID){
                        WerewolfUDPClient udpClient = null;
                        try {
                            udpClient = new WerewolfUDPClient(InetAddress.getByAddress(client.get("address").toString().getBytes()), Integer.parseInt(client.get("port").toString()));
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }

                        try {
                            udpClient.send(jsonObject);
                        } catch (Exception ex) {
                            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        });
    }

    public static void infoCivilianKilled(int player_id){
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
            recap = recap + "[" + "[" + firstVote.getPlayerId() + ", " + firstVote.getVoteCount() + "]";
            for (Vote object: voteList) {
                if(object.getPlayerId() != firstVote.getPlayerId()){
                    recap = recap + ", [" + object.getPlayerId()  + ", " + object.getVoteCount() + "]";
                }
            }
            recap = recap +"]";

            JSONObject jsonObject = new JSONObject();

            //rekapitulasi selesai
            if (majority_selected){
                jsonObject.put("method","vote_result_civilian");
                jsonObject.put("vote_status", "1");
                jsonObject.put("player_killed", player_to_kill.getPlayerId());
                jsonObject.put("vote_result", recap);
                tcpClient.send(jsonObject);
                //   System.out.println(jsonObject.toString());
            } else { //tidak ada majority terpilih
                jsonObject.put("method","vote_result");
                jsonObject.put("vote_status", "-1");
                jsonObject.put("vote_result", recap);
                tcpClient.send(jsonObject);
            }
            numOfVote = 0; //reset numOfVote untuk voting baru
            voteList.clear(); //reset list of vote
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