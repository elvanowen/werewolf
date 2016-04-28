import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    static ArrayList<JSONObject> clientList;
    static String highestKPUId = "0-0";

    static void setPlayerID(int playerID){
        playerID = playerID;
    }

    static void setPlayerID(ArrayList<JSONObject> clientList){
        clientList = clientList;
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

    static void onPreparePromiseReceived(JSONObject message, String remoteAddress, int remotePort){
        String[] kpuId = highestKPUId.split("-");

        if (Integer.parseInt(((JSONArray) message.get("proposal_id")).get(0).toString()) > Integer.parseInt(kpuId[0]) || (Integer.parseInt(((JSONArray) message.get("proposal_id")).get(0).toString()) == Integer.parseInt(kpuId[0]) && Integer.parseInt(((JSONArray) message.get("proposal_id")).get(1).toString()) > Integer.parseInt(kpuId[1]))) {
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

                exec.schedule(new Runnable() {
                    @Override
                    public void run() {
                        Paxos.prepare();
                    }
                }, 3, TimeUnit.SECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void onAcceptPromiseReceived(JSONObject message, String remoteAddress, int remotePort){

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
