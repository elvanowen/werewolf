import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by elvan_owen on 4/28/16.
 */
enum PAXOS_STATE {
    NONE,
    PREPARE,
    ACCEPT,
    DONE
}

enum PAXOS_ROLE {
    LEADER,
    ACCEPTOR
}

public class Paxos {
    PAXOS_STATE state = PAXOS_STATE.NONE;
    PAXOS_ROLE role;
    int proposalSequenceNumber = 0;
    int playerID;
    int kpuID;
    ArrayList<JSONObject> clientList;
    ArrayList<JSONObject> preparePromiseList = new ArrayList<>();
    ArrayList<JSONObject> acceptPromiseList = new ArrayList<>();
    String highestKPUId;
    OnLeaderChosenInterface onLeaderChosenCallback;

    public Paxos(PAXOS_ROLE role){
        this.role = role;

        setKpuID(-1);
        setHighestKpuID("-1", "-1");
    }

    void setPlayerID(int playerID){
        this.playerID = playerID;
    }

    void setClientList(ArrayList<JSONObject> clientList){
        this.clientList = clientList;
    }

    int getKpuID(){
        return kpuID;
    }

    void setKpuID(int kpuID){
        this.kpuID = kpuID;
    }

    void setHighestKpuID(String sequenceNumber, String playerID){
        this.highestKPUId = sequenceNumber + "/" + playerID;
    }

    String[] getHighestKPUId(){
        return this.highestKPUId.split("/");
    }

    void sendPrepareProposal(){
        state = PAXOS_STATE.PREPARE;

        if (role == PAXOS_ROLE.LEADER){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "prepare_proposal");

            JSONArray proposalId =  new JSONArray();
            proposalId.add(++proposalSequenceNumber);
            proposalId.add(playerID);

            jsonObject.put("proposal_id", proposalId);

            ArrayList<Integer> playerIds = new ArrayList<>();

            for (JSONObject client: clientList){
                playerIds.add(Integer.parseInt(client.get("player_id").toString()));
            }

            Collections.sort(playerIds);

            for (JSONObject client : clientList){
                int clientId = Integer.parseInt(client.get("player_id").toString());

                if (clientId != playerIds.get(0) && clientId != playerIds.get(1)) {
                    try {
                        new UDPClient(client.get("address").toString(), Integer.parseInt(client.get("port").toString())).send(jsonObject.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);

            exec.schedule(new Runnable() {
                @Override
                public void run() {
                    if (state == PAXOS_STATE.PREPARE && preparePromiseList.size() < (clientList.size() - 2)/2 + 1){
                        sendPrepareProposal();
                    }
                }
            }, 3, TimeUnit.SECONDS);
        }
    }

    void onPreparePromiseReceived(JSONObject message, String remoteAddress, int remotePort){
        if (role == PAXOS_ROLE.ACCEPTOR){
            String[] kpuId = getHighestKPUId();

            if (Integer.parseInt(((JSONArray) message.get("proposal_id")).get(0).toString()) > Integer.parseInt(kpuId[0]) || (Integer.parseInt(((JSONArray) message.get("proposal_id")).get(0).toString()) == Integer.parseInt(kpuId[0]) && Integer.parseInt(((JSONArray) message.get("proposal_id")).get(1).toString()) > Integer.parseInt(kpuId[1]))) {
                try {
                    JSONObject response = new JSONObject();
                    response.put("status", "ok");
                    response.put("description", "accepted");
                    response.put("previous_accepted", highestKPUId);

                    new UDPClient(remoteAddress, remotePort).send(response.toString());

                    setHighestKpuID(((JSONArray) message.get("proposal_id")).get(0).toString(), ((JSONArray) message.get("proposal_id")).get(1).toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    JSONObject response = new JSONObject();
                    response.put("status", "fail");
                    response.put("description", "rejected");

                    new UDPClient(remoteAddress, remotePort).send(response.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (role == PAXOS_ROLE.LEADER && state == PAXOS_STATE.PREPARE){
            preparePromiseList.add(message);

            if (preparePromiseList.size() > (clientList.size() - 2)/2){
                sendAcceptProposal();
            }
        }
    }

    void sendAcceptProposal(){
        state = PAXOS_STATE.ACCEPT;

        if (role == PAXOS_ROLE.LEADER) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "accept_proposal");

            JSONArray proposalId =  new JSONArray();
            proposalId.add(proposalSequenceNumber);
            proposalId.add(playerID);

            jsonObject.put("proposal_id", proposalId);
            jsonObject.put("kpu_id", getKpuID());

            ArrayList<Integer> playerIds = new ArrayList<Integer>();

            for (JSONObject client: clientList){
                playerIds.add(Integer.parseInt(client.get("player_id").toString()));
            }

            Collections.sort(playerIds);

            for (JSONObject client : clientList){
                int clientId = Integer.parseInt(client.get("player_id").toString());

                if (clientId != playerIds.get(0) && clientId != playerIds.get(1)) {
                    try {
                        new UDPClient(client.get("address").toString(), Integer.parseInt(client.get("port").toString())).send(jsonObject.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);

            exec.schedule(new Runnable() {
                @Override
                public void run() {
                    if (state == PAXOS_STATE.ACCEPT && acceptPromiseList.size() < (clientList.size() - 2)/2 + 1){
                        sendPrepareProposal();
                    }
                }
            }, 3, TimeUnit.SECONDS);
        }
    }

    void onAcceptPromiseReceived(JSONObject message, String remoteAddress, int remotePort){
        if (role == PAXOS_ROLE.ACCEPTOR){
            String[] kpuId = getHighestKPUId();

            if (Integer.parseInt(((JSONArray) message.get("proposal_id")).get(0).toString()) > Integer.parseInt(kpuId[0]) || (Integer.parseInt(((JSONArray) message.get("proposal_id")).get(0).toString()) == Integer.parseInt(kpuId[0]) && Integer.parseInt(((JSONArray) message.get("proposal_id")).get(1).toString()) > Integer.parseInt(kpuId[1]))) {
                try {
                    JSONObject response = new JSONObject();
                    response.put("status", "ok");
                    response.put("description", "accepted");

                    new UDPClient(remoteAddress, remotePort).send(response.toString());

                    setHighestKpuID(((JSONArray) message.get("proposal_id")).get(0).toString(), ((JSONArray) message.get("proposal_id")).get(1).toString());
                    setKpuID(Integer.parseInt(((JSONArray) message.get("proposal_id")).get(1).toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    JSONObject response = new JSONObject();
                    response.put("status", "fail");
                    response.put("description", "rejected");

                    new UDPClient(remoteAddress, remotePort).send(response.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (role == PAXOS_ROLE.LEADER && state == PAXOS_STATE.ACCEPT){
            acceptPromiseList.add(message);

            if (acceptPromiseList.size() > (clientList.size() - 2)/2){
                setKpuID(playerID);
                sendToLearner();
            }
        }
    }

    void sendToLearner(){
        state = PAXOS_STATE.DONE;

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("method", "prepare_proposal");
        jsonObject.put("kpu_id", getKpuID());
        jsonObject.put("Description", "Kpu is selected");

        for (JSONObject client : clientList){
            try {
                new UDPClient(client.get("address").toString(), Integer.parseInt(client.get("port").toString())).send(jsonObject.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        onLeaderChosenCallback.onLeaderChosen(getKpuID());
    }

    interface OnLeaderChosenInterface{
        public void onLeaderChosen(int kpuId);
    }

    public void onLeaderChosen(OnLeaderChosenInterface onLeaderChosenCallback){
        this.onLeaderChosenCallback = onLeaderChosenCallback;
    }
}
