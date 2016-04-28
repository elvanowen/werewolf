
import java.util.ArrayList;
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
        
        int numFailVoteWerewolf; // number of current failed vote for killing werewolf
        
        public Game(){
            this.gameStatus = "not playing";
        }
        
        public void start(){
            this.gameStatus = "playing";
            this.days = 0;
            this.time = "night";
            numFailVoteWerewolf = 0;
        }
        
        public void changePhase(){
            if(this.time.equals("day")){
                this.time = "night";
            }
            else{
                this.time = "day";
                this.days++;
            }
        }
        
        public void finish(){
            this.gameStatus = "not playing";
        }
    }
    
    private static int minClients = 1; // minimum clients to play
    private static TCPServer tcpServer;
    private static  HashMap<String, TCPServer.Client> clientList; //(username, TCPServer.Client)
    private int clientReady; // num of clients ready
    private static Game game;
    
    public Server(){
        this.clientList = new HashMap<String, TCPServer.Client>();
        game = new Game();
    }
    
    public static JSONObject setClientJoin(TCPServer.Client client, String username){
        JSONObject response = new JSONObject();
        
        if(!clientList.containsKey(username)){
            if(!Server.game.gameStatus.equals("playing")){
                client.username = username;
                clientList.put(username, client);
                response.put("status", "ok");
                response.put("player_id", clientList.size()-1);
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
    
    public static JSONObject getClient(TCPServer.Client client){
        JSONObject response = new JSONObject();
        response.put("played_id",client.playerId);
        response.put("is_alive",client.isAlive);
        response.put("address",client.address);
        response.put("port",client.port);
        response.put("username",client.username);
        response.put("role",client.role);
        
        return response;
    }
    
    public static JSONObject getClientList(){
        JSONObject response = new JSONObject();
        response.put("status", "ok");
        
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
        int numWerewolf = (int) ((double)clientList.size()/3);
        int numCivilian = clientList.size() - numWerewolf;
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
    public static  String[] getFriends(TCPServer.Client werewolf){
        ArrayList<String> friends = new ArrayList<String>();
        
        Iterator it = clientList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry clientEntry = (Map.Entry)it.next();
            TCPServer.Client client = (TCPServer.Client)clientEntry.getValue();
            if(client.role.equals("werewolf") && !client.username.equals(werewolf.username))
                friends.add(client.username);
//            it.remove(); // avoids a ConcurrentModificationException
        }
        
        String[] friendArr = new String[friends.size()];
        friendArr = friends.toArray(friendArr);

        return friendArr;
    }
    
    public static JSONObject startGame(TCPServer.Client client){
        //create request start game message for tcpClient
        JSONObject request = new JSONObject();
        request.put("method", "start");
        request.put("time", "night");
        request.put("role", client.role);
        if(client.role.equals("werewolf"))
            request.put("friend",(Object)getFriends(client));
        else
            request.put("friend","");
        request.put("description", "game is started");
        return request;
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
    
    public static void onMessageReceived(TCPServer.Client client, String message){
        JSONParser parser = new JSONParser();
        JSONObject response = new JSONObject();
        try {
            JSONObject obj = (JSONObject)parser.parse(message);
            String method = obj.get("method").toString(); // get method
            
            switch(method){
                case "join":{
                    String username = obj.get("username").toString();
                    response = setClientJoin(client, username);
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
                case "vote_result_werewolf":{
                    int vote_status = (int) obj.get("vote_status");
                    int player_killed = (int) obj.get("player_killed");
                    if(vote_status == 1){
                        response = infoWerewolfKilled(player_killed);
                    }
                    else{ //vote_status == -1
                        if(game.numFailVoteWerewolf == 0){
                            game.numFailVoteWerewolf++;
                            response = obj;
                            Server.tcpServer.broadcast(clientList,response.toString()); //broadcast to all clients
                            return;
                        }
                        else{
                            game.numFailVoteWerewolf = 0;
                            response = changePhase();
                        }
                    }
                    break;
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
