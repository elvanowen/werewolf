
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Server {
    
    //game
    private class Game{
        String gameStatus; // "not playing" , "playing"
        int days; // number of days played
        String time; // "day", "night"
        
        int kpuId; //leader ID
        int numAcceptedProposal; //number of acc. proposal received from clients, leader decided when this number achieved client majority
        int[] acceptedProposalResult; // vote result for leader election
        int[] proposersId; // client ID that become proposers
        
        int numFailVoteCivilian; // number of current failed vote for killing werewolf (on day time)
        
        String winner;// werewolf or civilian
        
        public Game(){
            this.gameStatus = "not playing";
        }
        
        public void resetLeader(){
            this.kpuId = -1;
            this.numAcceptedProposal = 0;
            this.acceptedProposalResult = new int[2];
            this.acceptedProposalResult[0] = 0;
            this.acceptedProposalResult[1] = 0;
            this.proposersId = new int[2];
            this.setProposersId();
        }
        
        public void setProposersId(){
            int[] clientsId = new int[clientList.size()];
            int i = 0;    
            Iterator it = clientList.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry clientEntry = (Map.Entry)it.next();
                TCPServer.Client client = (TCPServer.Client)clientEntry.getValue();
                clientsId[i++] = client.playerId;
            }
            
            Arrays.sort(clientsId);//sort
            this.proposersId[0]=clientsId[clientsId.length-2];
            this.proposersId[1]=clientsId[clientsId.length-1];
        }
        
        public void voteLeader(int id){
            for(int i=0; i<proposersId.length; i++){
                if(proposersId[i] == id){
                    acceptedProposalResult[i]++;
                    numAcceptedProposal++;
                    break;
                }
            }
            if(numAcceptedProposal == clientList.size())
                setKpuId(); //leader decided
        }
        
        public void setKpuId(){
            if(acceptedProposalResult[0] > acceptedProposalResult[1])
                this.kpuId = this.proposersId[0];
            else if(acceptedProposalResult[1] > acceptedProposalResult[0])
                this.kpuId = this.proposersId[1];
        }
        
        public void start(){
            this.gameStatus = "playing";
            this.days = 1;
            this.time = "day";
            numFailVoteCivilian = 0;
            resetLeader();
        }
        
        public void changePhase(){
            if(this.time.equals("day")){// change to night
                this.time = "night";
            }
            else{ // change to day
                this.time = "day";
                this.days++;
                resetLeader();
            }
        }
        
        public void setWinner(String winner){
            this.winner = winner;
        }
        
        public void finish(){
            this.gameStatus = "not playing";
        }
    }
    
    //attributes
    private static int minClients = 6; // minimum clients to play
    private static TCPServer tcpServer;
    private static  HashMap<String, TCPServer.Client> clientList; //(username, TCPServer.Client)
    private int clientReady; // num of clients ready
    private static Game game;
    
    private static int newPlayerId = 0;
    
    public Server(){
        this.clientList = new HashMap<String, TCPServer.Client>();
        game = new Game();
    }
    
    public static JSONObject setClientJoin(TCPServer.Client client, String username, String udp_address, int udp_port){
        JSONObject response = new JSONObject();
        
        if(!clientList.containsKey(username)){
            if(!Server.game.gameStatus.equals("playing")){
                client.setClientJoin(newPlayerId++, udp_address, udp_port, username);
                clientList.put(username, client);
                response.put("status", "ok");
                response.put("player_id", client.playerId);
            }
            else{ // is playing
                response.put("status", "fail");
                response.put("description", "please wait, game is currently running");
            }
        }
        else{
            response.put("status", "fail");
            response.put("description", "user exists");
        }
        return response;
    }
    
    //client leave the game
    public static JSONObject setClientLeave(String username){
        JSONObject response = new JSONObject();
        
        if(clientList.containsKey(username)){
            clientList.remove(username); // remove from client list
            response.put("status", "ok");
        }
        else{
            response.put("status", "fail");
            response.put("desription", "client has not joined");
        }
        
        return response;
    }
    
    public static JSONObject setClientReady(String username){
        JSONObject response = new JSONObject();
        
        if(clientList.containsKey(username)){
            clientList.get(username).status = "ready";
            response.put("status", "ok");
            response.put("desription", "waiting for other player to start");
        }
        else{
            response.put("status", "error");
            response.put("desription", "client has not joined");
        }
        
        return response;
    }
    
    //return client in JSON format
    public static JSONObject getClient(TCPServer.Client client){
        JSONObject response = new JSONObject();
        response.put("player_id",client.playerId);
        response.put("is_alive",client.isAlive);
        response.put("address",client.udpAddress);
        response.put("port",client.udpPort);
        response.put("username",client.username);
        
        //put role if client not alive
        if(client.isAlive == 0)
            response.put("role",client.role);
        
        return response;
    }
    
    public static JSONObject getClientList(){
        JSONObject response = new JSONObject();
        response.put("status", "ok");
        
        JSONArray clientArr = new JSONArray();
        Iterator it = clientList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry clientEntry = (Map.Entry)it.next();
            TCPServer.Client client = (TCPServer.Client)clientEntry.getValue();
            clientArr.add(getClient(client));
        }
        
        response.put("clients", clientArr);
        return response;
    }
    
    //check if all clients ready and >= minClients
    public static boolean isAllClientsReady(){
        if(clientList.size() < minClients){
            return false; // not enough clients
        }
        else{
            int numJoin = 0; // num of clients status = "join"
//            int numReady = 0; // num of clients status = "ready"

            Iterator it = clientList.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry clientEntry = (Map.Entry)it.next();
                TCPServer.Client client = (TCPServer.Client)clientEntry.getValue();
                if(client.status.equals("join"))
                    numJoin++;
//                it.remove(); // avoids a ConcurrentModificationException
            }
            
            return (numJoin == 0);
        }
    }
    
    public static void giveRoles(){
        //give roles (civilian or werewolf) to every tcpClient
        int numWerewolf = (int) (long) ((double)clientList.size()/3);
        int numCivilian = clientList.size() - numWerewolf;
        
//        int numWerewolf = 2; //dummy
//        int numCivilian =1;
        
        int[] roles = new int[2];
        roles[0] = numWerewolf;
        roles[1] = numCivilian;
        
        Iterator it = clientList.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry clientEntry = (Map.Entry)it.next();
            TCPServer.Client client = (TCPServer.Client)clientEntry.getValue();
            int idx = (int)Math.random(); // 0 for werewolf or 1 for civilian
            
            if(roles[idx] == 0){
                idx = (idx+1)%2;
            }
            
            client.role = (idx == 0) ? "werewolf" : "civilian";
            roles[idx]--;
            
//            it.remove();
        }
    }
    
    //return all werewolf friends
    public static  ArrayList<String> getFriends(TCPServer.Client werewolf){
        ArrayList<String> friends = new ArrayList<String>();
        
        Iterator it = clientList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry clientEntry = (Map.Entry)it.next();
            TCPServer.Client client = (TCPServer.Client)clientEntry.getValue();
            if(client.role.equals("werewolf") && !client.username.equals(werewolf.username))
                friends.add(client.username);
//            it.remove(); // avoids a ConcurrentModificationException
        }
        System.out.println("friendList: "+friends.toString());
//        String[] friendArr = new String[friends.size()];
//        friendArr = friends.toArray(friendArr);

        return friends;
    }
    
    public static JSONObject startGame(TCPServer.Client client){
        //create request start game message for client
        JSONObject request = new JSONObject();
        request.put("method", "start");
        request.put("time", game.time);
        request.put("role", client.role);
        if(client.role.equals("werewolf")){
            System.out.println("friends: "+getFriends(client).toString());
            request.put("friend", getFriends(client));
        }
        else
            request.put("friend","");
        request.put("description", "game is started");
        return request;
    }
    
    public static JSONObject clientAcceptedProposal(int kpuId){
        JSONObject response = new JSONObject();
        game.voteLeader(kpuId);
        
        response.put("status", "ok");
        response.put("description", "");
        return response;
    }
    
    public static JSONObject kpuSelected(){
        JSONObject response = new JSONObject();
        response.put("method", "kpu_selected");
        response.put("kpu_id", game.kpuId);
        
        return response;
    }
    
    public static JSONObject voteCommand(){
        JSONObject response = new JSONObject();
        response.put("method", "vote_now");
        response.put("phase",game.time);
        
        return response;
    }
    
    public static JSONObject infoCivilianKilled(int playerId){
        JSONObject response = new JSONObject();
        
        //find client with player_id = playerId
        Iterator it = clientList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry clientEntry = (Map.Entry)it.next();
            TCPServer.Client client = (TCPServer.Client)clientEntry.getValue();
            if(client.playerId == playerId){
                client.isAlive = 0;
            }
        }
        
        response.put("status", "ok");
        response.put("description", "");
        return response;
    }
    
    public static JSONObject infoWerewolfKilled(int playerId){
        JSONObject response = new JSONObject();
        
        //find client with player_id = playerId
        Iterator it = clientList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry clientEntry = (Map.Entry)it.next();
            TCPServer.Client client = (TCPServer.Client)clientEntry.getValue();
            if(client.playerId == playerId){
                client.isAlive = 0;
            }
        }
        
        response.put("status", "ok");
        response.put("description", "");
        return response;
    }
    
    public static JSONObject changePhase(){
        JSONObject response = new JSONObject();
        game.changePhase();//change game phase
        
        response.put("method", "change_phase");
        response.put("time", game.time);
        response.put("days", game.days);
        response.put("description", "");
        
        return response;
    }
    
    public static boolean isGameOver(){
        int numCivilian = 0;
        int numWerewolf = 0;
        Iterator it = clientList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry clientEntry = (Map.Entry)it.next();
            TCPServer.Client client = (TCPServer.Client)clientEntry.getValue();
            if(client.isAlive == 1){
                if(client.role.equals("civilian"))
                    numCivilian++;
                else if(client.role.equals("werewolf"))
                    numWerewolf++;
            }
        }
        
        //check if game is over
        if(numWerewolf == 0){
            game.setWinner("civilian");
            return true;
        }
        else if(numWerewolf == numCivilian){
            game.setWinner("werewolf");
            return true;
        }
        else
            return false;
    }
    
    public static JSONObject gameOver(){
        JSONObject response = new JSONObject();
        game.finish();
        
        response.put("method", "game_over");
        response.put("winner", game.winner);
        response.put("description", "");
        return response;
    }
    
    public static void onMessageReceived(TCPServer.Client client, String message){
        JSONParser parser = new JSONParser();
        JSONObject response = new JSONObject();
        try {
            JSONObject obj = (JSONObject)parser.parse(message);
            
            //check if receive status
            if(obj.containsKey("status")){
                return; //ignore
            }
            
            String method = obj.get("method").toString(); // get method
            
            switch(method){
                case "join":{
                    String username = obj.get("username").toString();
                    String udp_address = obj.get("udp_address").toString();
                    int udp_port = (int) (long) obj.get("udp_port");
                    response = setClientJoin(client, username, udp_address, udp_port);
                    break;
                }
                case "leave":{
                    String username = obj.get("username").toString();
                    response = setClientLeave(username);
                    break;
                }
                case "ready":{
                    response = setClientReady(client.username);
                    printResponse(response.toString());
                    Server.tcpServer.send(client,response.toString());
                    
                    //check if game can start
                    if(isAllClientsReady()){
                        //give role to every tcpClient
                        giveRoles();
                        
                        game.start();// set game start
                        //send start game to all clients
                        Iterator it = clientList.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry clientEntry = (Map.Entry)it.next();
                            TCPServer.Client clientDest = (TCPServer.Client)clientEntry.getValue();
                            
                            JSONObject request = startGame(clientDest);
                            Server.tcpServer.send(clientDest,request.toString());
                            printRequest(request.toString());
                            
                        }
                        
                    }
                    return;
                }
                case "client_address":{
                    response = getClientList();
                    break;
                }
                case "accepted_proposal":{
                    int kpu_id = (int) (long) obj.get("kpu_id");
                    response = clientAcceptedProposal(kpu_id);
                    Server.tcpServer.send(client,response.toString());
                    
                    //check if leader has been decided
                    if(game.kpuId != -1){
                        response = kpuSelected();
                        Server.tcpServer.broadcast(clientList,response.toString()); // broadcast leader to every client
                        
                        response = voteCommand();
                        Server.tcpServer.broadcast(clientList,response.toString()); // broadcast vote command to every client
                    }
                    
                    return;
                }
                case "vote_result_civilian":{
                    int vote_status = (int) (long) obj.get("vote_status");
                    if(vote_status == 1){
                        int player_killed = (int) (long) obj.get("player_killed");
                        if(game.numFailVoteCivilian > 0)
                            game.numFailVoteCivilian =0; //reset failed vote counter
                        
                        response = infoCivilianKilled(player_killed);
                        Server.tcpServer.send(client,response.toString());
                        
                    }
                    else{ //vote_status == -1
                        if(game.numFailVoteCivilian == 0){
                            game.numFailVoteCivilian++;
                            response = voteCommand();
                            Server.tcpServer.broadcast(clientList,response.toString()); //broadcast vote command (re-vote) to all clients
                            return;
                        }
                        else{
                            game.numFailVoteCivilian = 0;
                        }
                    }
                    
                    //check game over
                    if(isGameOver()){
                        response = gameOver();
                        Server.tcpServer.broadcast(clientList,response.toString()); //broadcast to all clients
                        return;
                    }
                    
                    response = changePhase(); //change to night phase
                    Server.tcpServer.broadcast(clientList,response.toString()); //broadcast to all clients

                    response = voteCommand();
                    Server.tcpServer.broadcast(clientList,response.toString()); //broadcast vote command to all clients
                    
                    return;
                }
                case "vote_result_werewolf":{
                    int vote_status = (int) (long) obj.get("vote_status");
                    if(vote_status == 1){
                        int player_killed = (int) (long) obj.get("player_killed");
                        response = infoWerewolfKilled(player_killed);
                        Server.tcpServer.send(client,response.toString());
                    }
                    else{ //vote_status == -1
                        response = voteCommand();
                        Server.tcpServer.broadcast(clientList,response.toString()); //broadcast vote command (re-vote) to all clients
                        
                        return;
                    }
                    
                    //check game over
                    if(isGameOver()){
                        response = gameOver();
                        Server.tcpServer.broadcast(clientList,response.toString()); //broadcast to all clients
                        return;
                    }
                    
                    response = changePhase(); //change to day phase
                    Server.tcpServer.broadcast(clientList,response.toString()); //broadcast to all clients
                    
                    return;
                }
                default:{ // wrong req
                    response.put("status", "error");
                    response.put("description", "wrong request");
                    break;
                }
            }
        } catch (ParseException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //send response to client
        printResponse(response.toString());
        Server.tcpServer.send(client,response.toString());
    }
    
    public static void printResponse(String message){
        System.out.println("response: " + message);
    }
    
    public static void printRequest(String message){
        System.out.println("request: " + message);
    }
    
    public static void main(String [] args) {
        Server server = new Server();
        
        try {
            tcpServer = new TCPServer();
        } catch (Exception e) {
            System.out.println(e);
        }
        
        
    }
}
