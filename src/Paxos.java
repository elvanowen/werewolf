import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;

/**
 * Created by elvan_owen on 4/28/16.
 */
enum PAXOS_STATE {
    PREPARE,
    ACCEPT,
    DONE
}

public class Paxos {
    static PAXOS_STATE state;
    static int proposalSequenceNumber = 0;
    static int playerID;
    static String highestKPUId = "0-0";

    static void setPlayerID(int playerID){
        playerID = playerID;
    }

    static void setPlayerID(ArrayList<JSONObject> clientList){
        playerID = playerID;
    }

    static void prepare(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("method", "prepare_proposal");

        JSONArray proposalId =  new JSONArray();
        proposalId.add(++proposalSequenceNumber);
        proposalId.add(playerID);

        jsonObject.put("proposal_id", proposalId);

        for (JSONObject client : clientList){
            try {
                new UDPClient(client.get("address").toString(), Integer.parseInt(client.get("port").toString())).send(jsonObject.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void accept(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("method", "accept_proposal");

        JSONArray proposalId =  new JSONArray();
        proposalId.add(proposalSequenceNumber);
        proposalId.add(playerID);

        jsonObject.put("proposal_id", proposalId);

        for (JSONObject client : clientList){
            try {
                new UDPClient(client.get("address").toString(), Integer.parseInt(client.get("port").toString())).send(jsonObject.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void write(){

    }
}
